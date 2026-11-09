package domin.ui;

import domin.ChatSession;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Selectable plain-text bubbles that wrap to the sidebar width. */
final class ChatTranscript extends JPanel implements Scrollable {
    private final String username;
    private final List<ChatSession.Message> rendered=new ArrayList<>();
    private String peer;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm");

    ChatTranscript(String username){
        this.username=username;setOpaque(false);setLayout(null);
        getAccessibleContext().setAccessibleName("Nội dung trò chuyện riêng");
    }
    boolean setMessages(String peer,List<ChatSession.Message> messages){
        if(peer.equals(this.peer)&&rendered.equals(messages))return false;
        this.peer=peer;rendered.clear();rendered.addAll(messages);removeAll();
        LocalDate previous=null;
        for(ChatSession.Message message:messages){
            LocalDate date=Instant.ofEpochMilli(message.timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
            if(!date.equals(previous)){
                JLabel day=new JLabel(date.equals(LocalDate.now())?"HÔM NAY":date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),SwingConstants.CENTER);
                day.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD,9f));day.setForeground(Theme.TEXT_MUTED);
                add(day);previous=date;
            }
            add(new Bubble(message,message.sender.equals(username)));
        }
        getAccessibleContext().setAccessibleDescription(getText());revalidate();repaint();return true;
    }
    public String getText(){
        StringBuilder text=new StringBuilder();
        for(ChatSession.Message message:rendered)text.append(message.sender).append(": ").append(message.text).append('\n');
        return text.toString();
    }
    private int contentHeight(int width){
        if(rendered.isEmpty())return 260;
        int height=8;
        for(Component c:getComponents())height+=c instanceof Bubble?((Bubble)c).heightAt(width):32;
        return height+8;
    }
    @Override public Dimension getPreferredSize(){
        int width=getParent()==null?260:Math.max(1,getParent().getWidth());
        return new Dimension(width,contentHeight(width));
    }
    @Override public void doLayout(){
        int y=8;
        for(Component c:getComponents()){
            int height=c instanceof Bubble?((Bubble)c).heightAt(getWidth()):32;
            c.setBounds(0,y,getWidth(),height);c.doLayout();y+=height;
        }
    }
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);
        if(!rendered.isEmpty())return;
        Graphics2D g=RankingPanel.quality(graphics);
        int x=getWidth()/2,y=70;
        g.setColor(new Color(34,211,238,12));g.fillOval(x-38,y-38,76,76);
        g.setColor(new Color(47,113,143));g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(x-21,y-17,42,29,10,10);g.drawLine(x-11,y+12,x-17,y+20);g.drawLine(x-17,y+20,x-3,y+12);
        g.setColor(Theme.ACCENT_CYAN);g.fillOval(x-11,y-5,4,4);g.fillOval(x-2,y-5,4,4);g.fillOval(x+7,y-5,4,4);
        centered(g,"BẮT ĐẦU TRÒ CHUYỆN",x,y+64,11,Theme.TEXT_SECONDARY);
        centered(g,"Gửi một lời chào cho "+(peer==null?"người chơi":peer),x,y+88,10,Theme.TEXT_MUTED);
        g.dispose();
    }
    private static void centered(Graphics2D g,String text,int x,int y,int size,Color color){
        g.setFont(Theme.BODY_FONT.deriveFont((float)size));g.setColor(color);
        g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);
    }
    private static final class Bubble extends JPanel {
        final JTextArea body;
        final JLabel time;
        final boolean own;
        final JPanel background;
        int bubbleWidth,bubbleHeight;
        Bubble(ChatSession.Message message,boolean own){
            this.own=own;setOpaque(false);setLayout(null);
            body=new JTextArea(message.text);
            body.setEditable(false);body.setLineWrap(true);body.setWrapStyleWord(true);
            body.setOpaque(false);body.setFont(Theme.BODY_FONT.deriveFont(13f));body.setForeground(Theme.TEXT_PRIMARY);
            body.setBorder(null);body.setCaretColor(Theme.ACCENT_CYAN);
            body.getAccessibleContext().setAccessibleName("Tin nhắn của "+message.sender);
            String stamp=TIME.format(Instant.ofEpochMilli(message.timestamp).atZone(ZoneId.systemDefault()));
            time=new JLabel((own?"Bạn · ":"")+stamp,SwingConstants.RIGHT);
            time.setFont(Theme.SMALL_FONT.deriveFont(9f));time.setForeground(own?new Color(167,204,244):Theme.TEXT_MUTED);
            background=new JPanel(new BorderLayout(0,5)){
                protected void paintComponent(Graphics graphics){
                    Graphics2D g=RankingPanel.quality(graphics);
                    g.setPaint(new GradientPaint(0,0,own?new Color(30,84,139):new Color(22,35,54),
                            getWidth(),getHeight(),own?new Color(28,58,109):new Color(18,29,46)));
                    g.fillRoundRect(0,0,getWidth(),getHeight(),15,15);
                    g.setColor(own?new Color(53,115,166):new Color(36,53,75));
                    g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,15,15);g.dispose();
                }
            };
            background.setOpaque(false);background.setBorder(new EmptyBorder(10,12,8,12));
            background.add(body,BorderLayout.CENTER);background.add(time,BorderLayout.SOUTH);
            background.setToolTipText(message.sender+" · "+stamp);add(background);
        }
        int heightAt(int width){
            int max=Math.max(74,(int)((width-12)*.87));
            FontMetrics fm=body.getFontMetrics(body.getFont());int natural=74;
            for(String line:body.getText().split("\\R",-1))natural=Math.max(natural,fm.stringWidth(line)+24);
            bubbleWidth=Math.min(max,natural);
            body.setSize(Math.max(1,bubbleWidth-24),Short.MAX_VALUE);
            bubbleHeight=body.getPreferredSize().height+18+5+time.getPreferredSize().height;
            return bubbleHeight+11;
        }
        @Override public void doLayout(){
            heightAt(getWidth());
            background.setBounds(own?getWidth()-bubbleWidth-5:5,0,bubbleWidth,bubbleHeight);
            background.doLayout();
        }
    }
    public Dimension getPreferredScrollableViewportSize(){return new Dimension(260,350);}
    public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 20;}
    public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(20,r.height-30);}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getPreferredSize().height<getParent().getHeight();}
}
