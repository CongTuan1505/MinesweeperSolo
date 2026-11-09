package domin;

import java.util.*;
import java.util.function.LongSupplier;

/** Client-side, bounded session history; created afresh for each login. Used on the Swing thread. */
public final class ChatSession {
    public static final int HISTORY_LIMIT = 100;
    public static final long SEND_TIMEOUT_NANOS = 5_000_000_000L;
    private final LongSupplier clock;
    private final String owner;
    private String activePeer;
    private final Map<String,Conversation> conversations=new LinkedHashMap<>();
    private static final class Conversation {
        final Deque<Message> messages=new ArrayDeque<>();
        int unread;
        String draft="",pending="",error="";
        long pendingSince;
    }
    public static final class Message {
        public final String sender,text;
        public final long timestamp;
        Message(String sender,String text,long timestamp){this.sender=sender;this.text=text;this.timestamp=timestamp;}
    }
    public ChatSession(String owner){this(owner,System::nanoTime);}
    ChatSession(String owner,LongSupplier clock){this.owner=Objects.requireNonNull(owner);this.clock=clock;}
    private Conversation conversation(String peer){
        if(peer==null || peer.isBlank() || owner.equals(peer))throw new IllegalArgumentException("Người nhận không hợp lệ");
        return conversations.computeIfAbsent(peer,key -> new Conversation());
    }
    public String activePeer(){return activePeer;}
    public void open(String peer){conversation(peer);activePeer=peer;markRead(peer);}
    public void back(){activePeer=null;}
    public Set<String> peers(){return new LinkedHashSet<>(conversations.keySet());}
    public List<Message> messages(String peer){return new ArrayList<>(conversation(peer).messages);}
    public int unread(String peer){Conversation c=conversations.get(peer);return c==null?0:c.unread;}
    public void markRead(String peer){conversation(peer).unread=0;}
    public String draft(String peer){return conversation(peer).draft;}
    public void setDraft(String peer,String text){conversation(peer).draft=text;}
    public boolean pending(String peer){return !conversation(peer).pending.isEmpty();}
    public void sending(String peer,String text){
        Conversation c=conversation(peer);
        if(!c.pending.isEmpty())throw new IllegalStateException("Đang chờ gửi tin");
        c.pending=text;c.pendingSince=clock.getAsLong();c.error="";
    }
    public void failed(String peer){failed(peer,"Tin nhắn chưa được gửi.");}
    public void failed(String peer,String message){
        Conversation c=conversation(peer);c.pending="";c.error=message;
    }
    public String error(String peer){return conversation(peer).error;}
    public Set<String> expirePending(){
        Set<String> expired=new LinkedHashSet<>();
        long now=clock.getAsLong();
        for(Map.Entry<String,Conversation> entry:conversations.entrySet()){
            Conversation c=entry.getValue();
            if(!c.pending.isEmpty() && now-c.pendingSince>=SEND_TIMEOUT_NANOS){
                c.pending="";c.error="Gửi chưa được xác nhận. Kiểm tra kết nối trước khi thử lại.";
                expired.add(entry.getKey());
            }
        }
        return expired;
    }
    /** Legacy servers answer CHAT_SEND with a generic unknown-command error. */
    public Set<String> handleServerError(String message){
        Set<String> failed=new LinkedHashSet<>();
        if(!"Lệnh không hợp lệ".equals(message))return failed;
        for(Map.Entry<String,Conversation> entry:conversations.entrySet()){
            if(!entry.getValue().pending.isEmpty()){
                failed(entry.getKey(),"Server chưa hỗ trợ chat. Hãy khởi động lại server.");
                failed.add(entry.getKey());
            }
        }
        return failed;
    }
    /** Reject messages unrelated to the logged-in account, even across screen/login changes. */
    public String receive(String from,String to,String text,long timestamp,boolean visible){
        if(!owner.equals(from)&&!owner.equals(to))throw new IllegalArgumentException("Tin nhắn không thuộc tài khoản");
        String peer=owner.equals(from)?to:from;
        Conversation c=conversation(peer);
        c.messages.addLast(new Message(from,text,timestamp));
        while(c.messages.size()>HISTORY_LIMIT)c.messages.removeFirst();
        if(owner.equals(from)){
            // A delayed echo for an earlier message must not clear a newer send.
            if(c.pending.isEmpty() || c.pending.equals(text)){
                c.pending="";c.error="";
                if(c.draft.strip().equals(text))c.draft="";
            }
        }else if(!visible)c.unread=Math.min(HISTORY_LIMIT,c.unread+1);
        else c.unread=0;
        return peer;
    }
}
