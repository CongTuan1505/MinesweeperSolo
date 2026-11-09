package domin;

import domin.ui.HomeScreen;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.*;

public class TestChat {
    static int checks;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    static Object field(Object o,String name)throws Exception{
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    static void command(Server s,Server.Peer p,String command)throws Exception{
        Method m=Server.class.getDeclaredMethod("handle",Server.Peer.class,String.class);
        m.setAccessible(true);m.invoke(s,p,command);
    }
    static void rejected(Runnable task,String message){
        try{task.run();throw new AssertionError(message);}catch(IllegalArgumentException expected){checks++;}
    }
    static String transcriptText(Object transcript)throws Exception{
        Method m=transcript.getClass().getDeclaredMethod("getText");m.setAccessible(true);return (String)m.invoke(transcript);
    }
    static JButton button(Container container,String text){
        for(Component c:container.getComponents()){
            if(c instanceof JButton && ((JButton)c).getText().equals(text))return (JButton)c;
            if(c instanceof Container){JButton found=button((Container)c,text);if(found!=null)return found;}
        }
        return null;
    }
    static void codec(){
        String text="Xin chào! | ~ ; <html> & 😊\nBạn chơi chế độ nào?";
        check(ChatMessage.decode(ChatMessage.encode(text)).equals(text),"Vietnamese, emoji, delimiters and newlines round trip");
        rejected(() -> ChatMessage.decode("%%%"),"Malformed Base64 rejected");
        rejected(() -> ChatMessage.decode(Base64.getEncoder().encodeToString(new byte[]{(byte)0xc3,0x28})),"Invalid UTF-8 rejected");
        rejected(() -> ChatMessage.encode(" \n "),"Blank rejected");
        rejected(() -> ChatMessage.encode("x".repeat(501)),"Oversized rejected");
        rejected(() -> ChatMessage.encode("bad\u0000text"),"Control characters rejected");
        check(ChatMessage.decode(ChatMessage.encode("x".repeat(500))).length()==500,"500-character boundary");
    }
    static void session(){
        ChatSession state=new ChatSession("alice");
        state.receive("bob","alice","Xin chào",1,false);
        check(state.unread("bob")==1,"Incoming unopened chat is unread");
        state.open("bob");check(state.unread("bob")==0,"Opening marks read");
        state.setDraft("bob","Chào bạn");state.sending("bob","Chào bạn");state.back();
        check(state.draft("bob").equals("Chào bạn")&&state.pending("bob"),"Back preserves pending draft");
        state.receive("alice","bob","Chào bạn",2,false);
        check(!state.pending("bob")&&state.draft("bob").isEmpty(),"Server echo confirms and clears matching draft");
        state.setDraft("bob","old");state.sending("bob","old");state.setDraft("bob","new");
        state.receive("alice","bob","old",3,false);
        check(state.draft("bob").equals("new"),"Echo cannot erase newer draft");
        state.sending("bob","new");state.failed("bob");
        check(!state.pending("bob")&&state.draft("bob").equals("new"),"Failure keeps draft for retry");
        for(int i=0;i<120;i++)state.receive("bob","alice","message "+i,i,false);
        check(state.messages("bob").size()==100&&state.unread("bob")==100,"History and unread are bounded");
        check(state.messages("bob").get(0).text.equals("message 20"),"Newest history retained");
        rejected(() -> state.receive("bob","eve","secret",1,false),"Other account's message rejected");
        check(new ChatSession("alice").messages("bob").isEmpty(),"New login has no previous session history");
    }
    static void pendingRecovery(){
        java.util.concurrent.atomic.AtomicLong clock=new java.util.concurrent.atomic.AtomicLong();
        ChatSession state=new ChatSession("alice",clock::get);
        state.setDraft("bob","hello");state.sending("bob","hello");
        check(state.handleServerError("Lỗi cơ sở dữ liệu").isEmpty()&&state.pending("bob"),
                "Unrelated server errors do not cancel chat");
        check(state.handleServerError("Lệnh không hợp lệ").contains("bob")&&!state.pending("bob"),
                "Legacy server's exact response immediately releases pending send");
        check(state.draft("bob").equals("hello")&&state.error("bob").contains("khởi động lại server"),
                "Legacy error retains draft and explains restart");
        state.sending("bob","hello");
        clock.set(ChatSession.SEND_TIMEOUT_NANOS-1);
        check(state.expirePending().isEmpty()&&state.pending("bob"),"No timeout before deadline");
        clock.incrementAndGet();
        check(state.expirePending().contains("bob")&&!state.pending("bob"),"Pending send expires at five seconds");
        check(state.draft("bob").equals("hello"),"Timeout preserves original draft");
        state.setDraft("bob","new message");state.sending("bob","new message");
        state.receive("alice","bob","hello",1,false);
        check(state.pending("bob")&&state.draft("bob").equals("new message"),
                "Late acknowledgement cannot release a newer pending message");
        state.receive("alice","bob","new message",2,false);
        check(!state.pending("bob")&&state.draft("bob").isEmpty(),"Matching acknowledgement resolves new send");
    }

    @SuppressWarnings("unchecked")
    static void server()throws Exception{
        Server server=new Server();
        Map<String,Server.Peer> peers=(Map<String,Server.Peer>)field(server,"activePeers");
        TestGameRoom.Fake alice=new TestGameRoom.Fake(server,"alice"),bob=new TestGameRoom.Fake(server,"bob"),
                eve=new TestGameRoom.Fake(server,"eve"),guest=new TestGameRoom.Fake(server,"guest");
        peers.put("alice",alice);peers.put("bob",bob);peers.put("eve",eve);
        guest.name=null;
        String content="Xin chào | đối thủ 😊";
        String request="CHAT_SEND|bob|"+ChatMessage.encode(content);
        command(server,alice,request);
        check(bob.count("CHAT_MESSAGE")==1&&alice.count("CHAT_MESSAGE")==1,"Recipient delivery and sender acknowledgement");
        check(eve.count("CHAT_MESSAGE")==0,"No broadcast to third player");
        String[] packet=Wire.parse(bob.last("CHAT_MESSAGE"));
        check(packet.length==5&&packet[1].equals("alice")&&packet[2].equals("bob"),"Authenticated sender and correct recipient");
        check(ChatMessage.decode(packet[4]).equals(content)&&Long.parseLong(packet[3])>0,"Wire text and timestamp");
        command(server,alice,request);
        check(alice.last("CHAT_ERROR")!=null&&bob.count("CHAT_MESSAGE")==1,"Flooding rejected without duplicate delivery");
        alice.lastChatNanos=0;
        command(server,alice,request+"|eve");
        check(bob.count("CHAT_MESSAGE")==1,"Spoofed extra sender field rejected");
        command(server,guest,request);check(guest.last("CHAT_ERROR")!=null,"Unauthenticated sender rejected");
        command(server,alice,"CHAT_SEND|alice|"+ChatMessage.encode("self"));
        check(bob.count("CHAT_MESSAGE")==1,"Self-chat rejected");
        command(server,alice,"CHAT_SEND|bob|%%%");
        check(bob.count("CHAT_MESSAGE")==1,"Invalid payload not delivered");
        bob.match=server.new Match(bob,eve,42,BoardType.MEDIUM);
        command(server,alice,request);
        check(bob.count("CHAT_MESSAGE")==2,"Chat can arrive while recipient plays");
        bob.match=null;command(server,bob,"LOGOUT");
        alice.lastChatNanos=0;command(server,alice,request);
        check(ChatMessage.decode(Wire.parse(alice.last("CHAT_ERROR"))[2]).contains("ngoại tuyến"),"Offline delivery reports failure");
    }
    @SuppressWarnings("unchecked")
    static void sockets()throws Exception{
        Server server=new Server();
        Map<String,Server.Peer> peers=(Map<String,Server.Peer>)field(server,"activePeers");
        try(ServerSocket listener=new ServerSocket(0);
            Socket a=new Socket("127.0.0.1",listener.getLocalPort());
            Socket acceptedA=listener.accept();
            Socket b=new Socket("127.0.0.1",listener.getLocalPort());
            Socket acceptedB=listener.accept()){
            a.setSoTimeout(2000);b.setSoTimeout(2000);
            Server.Peer pa=server.new Peer(acceptedA),pb=server.new Peer(acceptedB);
            pa.name="alice";pb.name="bob";peers.put("alice",pa);peers.put("bob",pb);
            Thread reader=new Thread(() -> server.serve(pa));reader.setDaemon(true);reader.start();
            Wire sender=new Wire(a),recipient=new Wire(b);
            sender.send("CHAT_SEND|bob|"+ChatMessage.encode("Tin nhắn TCP thật | tiếng Việt"));
            String received=recipient.readLine(),echo=sender.readLine();
            check(received.equals(echo),"Real TCP routes and acknowledges identical packet");
            check(ChatMessage.decode(Wire.parse(received)[4]).contains("tiếng Việt"),"Real UTF-8 protocol");
            a.shutdownOutput();reader.join(2000);
            check(!reader.isAlive(),"Socket disconnect cleanup finishes");
        }
    }
    static void ui()throws Exception{
        SwingUtilities.invokeAndWait(() -> {
            try{
                java.util.concurrent.atomic.AtomicLong clock=new java.util.concurrent.atomic.AtomicLong();
                ChatSession state=new ChatSession("alice",clock::get);
                java.util.List<String> sent=new ArrayList<>();
                HomeScreen home=new HomeScreen("alice",sent::add,state);
                home.setOnlinePlayers("bob~9~ONLINE;eve~3~IN_MATCH");
                JPanel list=(JPanel)field(home,"onlineListPanel");
                JPanel bob=(JPanel)list.getComponent(0);
                for(MouseListener listener:bob.getMouseListeners())listener.mouseClicked(
                        new MouseEvent(bob,MouseEvent.MOUSE_CLICKED,0,0,3,3,1,false));
                check(button(home,"Thách đấu")!=null&&button(home,"Trò chuyện")!=null,"Selected player has challenge and chat buttons");
                button(home,"Trò chuyện").doClick(0);
                check(home.isChatVisible("bob"),"Chat replaces list in same sidebar");
                Object panel=field(home,"chatPanel");
                JTextArea input=(JTextArea)field(panel,"input");
                JButton send=(JButton)field(panel,"sendButton");
                input.setText("Chào bob | 😊");send.doClick(0);
                check(sent.get(sent.size()-1).startsWith("CHAT_SEND|bob|"),"Send action targets selected player");
                check(state.pending("bob")&&!input.getText().isEmpty(),"Draft retained until acknowledgement");
                state.receive("alice","bob","Chào bob | 😊",1,true);home.chatUpdated("bob");
                check(input.getText().isEmpty(),"Acknowledgement clears sent draft");
                state.receive("bob","alice","<html>literal & private",2,true);home.chatUpdated("bob");
                Object transcript=field(panel,"transcript");
                check(transcriptText(transcript).contains("<html>literal & private"),"Message markup rendered as literal text");
                input.setText("Bản nháp");
                button((Container)panel,"Quay lại").doClick(0);
                check(!home.isChatVisible("bob")&&state.draft("bob").equals("Bản nháp"),"Back restores list and saves draft");
                state.receive("bob","alice","Tin mới",3,false);home.chatUpdated("bob");
                check(state.unread("bob")==1,"Unread survives list refresh");
                home.openChat("bob");
                check(state.unread("bob")==0&&input.getText().equals("Bản nháp"),"Reopen restores draft and marks read");
                Method switchTab=HomeScreen.class.getDeclaredMethod("switchTab",String.class);switchTab.setAccessible(true);
                switchTab.invoke(home,"TAB_LEADERBOARD");
                state.receive("bob","alice","Tin trong tab khác",4,false);home.chatUpdated("bob");
                check(state.unread("bob")==1,"Hidden tab does not mark messages read");
                switchTab.invoke(home,"TAB_LOBBY");check(state.unread("bob")==0,"Returning to visible conversation marks read");
                home.removeOnlinePlayer("bob");
                check(!send.isEnabled()&&!input.isEnabled(),"Offline peer cannot be sent new messages");
                button((Container)panel,"Quay lại").doClick(0);
                check(TestBoardModes.labels(list).contains("TRÒ CHUYỆN GẦN ĐÂY"),"Offline history remains accessible");
                java.util.List<String> rebuiltSent=new ArrayList<>();
                HomeScreen rebuilt=new HomeScreen("alice",rebuiltSent::add,state);
                rebuilt.setOnlinePlayers("bob~9~ONLINE");rebuilt.openChat("bob");
                check(state.draft("bob").equals("Bản nháp"),"Session survives home screen recreation after a game");
                check(transcriptText(field(field(rebuilt,"chatPanel"),"transcript")).contains("Tin mới"),"Conversation survives screen recreation");
                Object rebuiltPanel=field(rebuilt,"chatPanel");
                JButton retry=(JButton)field(rebuiltPanel,"sendButton");
                retry.doClick(0);
                check(state.pending("bob"),"Lost acknowledgement begins as pending");
                clock.addAndGet(ChatSession.SEND_TIMEOUT_NANOS);
                javax.swing.Timer timer=(javax.swing.Timer)field(rebuiltPanel,"sendTimer");
                for(ActionListener listener:timer.getActionListeners())listener.actionPerformed(
                        new ActionEvent(timer,ActionEvent.ACTION_PERFORMED,"tick"));
                check(!state.pending("bob")&&retry.isEnabled()&&retry.getText().equals("Gửi"),
                        "Timer unblocks the visible send button");
                check(((JTextArea)field(rebuiltPanel,"input")).getText().equals("Bản nháp"),
                        "Visible draft survives lost acknowledgement");
                check(((JTextArea)field(rebuiltPanel,"error")).getText().contains("chưa được xác nhận"),
                        "Timeout is explained inside chat panel");
                check(rebuiltSent.size()==1,"Timeout never auto-resends a possibly delivered message");

            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    public static void main(String[] args)throws Exception{
        try{codec();session();pendingRecovery();server();sockets();ui();System.out.println("PASS: "+checks+" private chat, TCP, session and UI checks.");System.exit(0);}
        catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
