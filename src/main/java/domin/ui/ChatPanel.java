package domin.ui;

import domin.ChatMessage;
import domin.ChatSession;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.function.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.*;

/** Private conversation occupies the same sidebar as the online list. */
final class ChatPanel extends JPanel {
    private final String username;
    private final ChatSession session;
    private final Consumer<String> send;
    private final Function<String,String> presence;
    private final JLabel person=new JLabel(),status=new JLabel(),counter=new JLabel("0/500");
    private final JTextArea error=new JTextArea(" ",2,0);
    private final Timer sendTimer=new Timer(500,e -> checkSendTimeouts());
    private final ChatTranscript transcript;
    private final JScrollPane messageScroll;
    private final JTextArea input=new JTextArea(2,16){
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            if(!getText().isEmpty())return;
            Graphics2D g=RankingPanel.quality(graphics);
            g.setFont(getFont().deriveFont(12f));g.setColor(Theme.TEXT_MUTED);
            g.drawString(isEnabled()?"Nhập tin nhắn...":"Người chơi đang ngoại tuyến",getInsets().left,getInsets().top+g.getFontMetrics().getAscent());
            g.dispose();
        }
    };
    private final JButton sendButton=button("Gửi",true);
    private boolean updating;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm");

    ChatPanel(String username,ChatSession session,Consumer<String> send,Function<String,String> presence,Runnable back){
        this.username=username;this.session=session;this.send=send;this.presence=presence;
        setOpaque(false);setLayout(new BorderLayout(0,12));
        JPanel heading=new JPanel(new BorderLayout(0,13));heading.setOpaque(false);
        JPanel navigation=new JPanel(new BorderLayout());navigation.setOpaque(false);
        JButton backButton=button("Quay lại",false);
        backButton.setIcon(new Icon(){
            public int getIconWidth(){return 12;}
            public int getIconHeight(){return 12;}
            public void paintIcon(Component c,Graphics graphics,int x,int y){
                Graphics2D g=RankingPanel.quality(graphics);g.setColor(Theme.ACCENT_CYAN);g.setStroke(new BasicStroke(1.5f));
                g.drawLine(x+10,y+6,x+2,y+6);g.drawLine(x+2,y+6,x+6,y+2);g.drawLine(x+2,y+6,x+6,y+10);g.dispose();
            }
        });
        backButton.setToolTipText("Quay lại danh sách người chơi");backButton.addActionListener(e -> back.run());
        navigation.add(backButton,BorderLayout.WEST);
        JLabel label=new JLabel("TRÒ CHUYỆN RIÊNG");
        label.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD,9f));label.setForeground(Theme.TEXT_MUTED);
        navigation.add(label,BorderLayout.EAST);heading.add(navigation,BorderLayout.NORTH);
        JPanel identity=RankingPanel.surface(new Color(34,58,80));
        identity.setLayout(new BorderLayout(11,0));identity.setBorder(new EmptyBorder(13,12,13,12));
        JComponent avatar=new JComponent(){
            protected void paintComponent(Graphics graphics){
                Graphics2D g=RankingPanel.quality(graphics);
                RankingPanel.drawAvatar(g,session.activePeer(),3,3,36,Theme.ACCENT_CYAN,null);g.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(43,43));identity.add(avatar,BorderLayout.WEST);
        JPanel names=new JPanel(new GridLayout(2,1,0,5));names.setOpaque(false);
        person.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD,15f));person.setForeground(Theme.TEXT_PRIMARY);
        status.setFont(Theme.SMALL_FONT.deriveFont(10f));
        status.setIcon(new Icon(){
            public int getIconWidth(){return 9;}public int getIconHeight(){return 9;}
            public void paintIcon(Component c,Graphics graphics,int x,int y){
                Graphics2D g=RankingPanel.quality(graphics);g.setColor(status.getForeground());g.fillOval(x+1,y+2,6,6);g.dispose();
            }
        });
        names.add(person);names.add(status);identity.add(names,BorderLayout.CENTER);
        heading.add(identity,BorderLayout.CENTER);add(heading,BorderLayout.NORTH);

        transcript=new ChatTranscript(username);
        messageScroll=new JScrollPane(transcript);
        messageScroll.setOpaque(false);messageScroll.getViewport().setOpaque(false);
        messageScroll.setBorder(null);
        messageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleScroll(messageScroll);
        add(messageScroll,BorderLayout.CENTER);
        JPanel composer=new JPanel(new BorderLayout(0,7));composer.setOpaque(false);
        input.setFont(Theme.BODY_FONT);input.setForeground(Theme.TEXT_PRIMARY);
        input.setOpaque(false);input.setCaretColor(Theme.ACCENT_CYAN);
        input.setLineWrap(true);input.setWrapStyleWord(true);input.setBorder(new EmptyBorder(9,10,9,10));
        input.setToolTipText("Enter để gửi · Shift+Enter để xuống dòng");
        input.getAccessibleContext().setAccessibleName("Nhập tin nhắn");
        ((AbstractDocument)input.getDocument()).setDocumentFilter(new DocumentFilter(){
            public void insertString(FilterBypass fb,int offset,String text,AttributeSet attrs)throws BadLocationException{
                replace(fb,offset,0,text,attrs);
            }
            public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs)throws BadLocationException{
                if(fb.getDocument().getLength()-length+(text==null?0:text.length())<=ChatMessage.MAX_LENGTH)
                    fb.replace(offset,length,text,attrs);
            }
        });
        input.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){changed();}
            public void removeUpdate(DocumentEvent e){changed();}
            public void changedUpdate(DocumentEvent e){changed();}
            private void changed(){
                if(!updating&&session.activePeer()!=null)session.setDraft(session.activePeer(),input.getText());
                updateSendState();
            }
        });
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"send-chat");
        input.getActionMap().put("send-chat",new AbstractAction(){
            public void actionPerformed(ActionEvent e){if(sendButton.isEnabled())sendButton.doClick(0);}
        });
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.SHIFT_DOWN_MASK),"chat-newline");
        input.getActionMap().put("chat-newline",new AbstractAction(){
            public void actionPerformed(ActionEvent e){input.replaceSelection("\n");}
        });
        JScrollPane inputScroll=new JScrollPane(input);
        inputScroll.setOpaque(false);inputScroll.getViewport().setOpaque(false);
        inputScroll.setBorder(null);inputScroll.setPreferredSize(new Dimension(0,62));
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleScroll(inputScroll);
        JPanel inputCard=new JPanel(new BorderLayout(0,5)){
            protected void paintComponent(Graphics graphics){
                Graphics2D g=RankingPanel.quality(graphics);
                g.setColor(new Color(16,28,46));g.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g.setColor(input.isFocusOwner()?new Color(34,156,185):new Color(40,61,86));
                g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);g.dispose();
            }
        };
        inputCard.setOpaque(false);inputCard.setBorder(new EmptyBorder(3,3,8,8));
        input.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){inputCard.repaint();}
            public void focusLost(FocusEvent e){inputCard.repaint();}
        });
        inputCard.add(inputScroll,BorderLayout.CENTER);
        JPanel controls=new JPanel(new BorderLayout());controls.setOpaque(false);
        controls.setBorder(new EmptyBorder(0,10,0,0));
        counter.setFont(Theme.SMALL_FONT.deriveFont(9f));counter.setForeground(Theme.TEXT_MUTED);
        controls.add(counter,BorderLayout.WEST);
        sendButton.setIcon(new Icon(){
            public int getIconWidth(){return 15;}public int getIconHeight(){return 15;}
            public void paintIcon(Component c,Graphics graphics,int x,int y){
                Graphics2D g=RankingPanel.quality(graphics);
                g.setColor(c.isEnabled()?Theme.TEXT_PRIMARY:Theme.TEXT_MUTED);
                g.setStroke(new BasicStroke(1.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.drawPolygon(new int[]{x+1,x+13,x+9,x+6},new int[]{y+2,y+1,y+13,y+7},4);
                g.drawLine(x+6,y+7,x+13,y+1);g.dispose();
            }
        });
        sendButton.addActionListener(e -> submit());controls.add(sendButton,BorderLayout.EAST);
        inputCard.add(controls,BorderLayout.SOUTH);composer.add(inputCard,BorderLayout.NORTH);
        error.setFont(Theme.SMALL_FONT.deriveFont(10f));error.setForeground(new Color(248,171,116));
        error.setOpaque(false);error.setEditable(false);error.setFocusable(false);
        error.setLineWrap(true);error.setWrapStyleWord(true);error.setBorder(null);error.setVisible(false);
        composer.add(error,BorderLayout.SOUTH);add(composer,BorderLayout.SOUTH);
    }
    private static void styleScroll(JScrollPane scroll){
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4,0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            protected void configureScrollBarColors(){thumbColor=new Color(38,62,88);trackColor=new Color(12,18,34);}
            protected JButton createDecreaseButton(int o){JButton b=new JButton();b.setPreferredSize(new Dimension());return b;}
            protected JButton createIncreaseButton(int o){return createDecreaseButton(o);}
        });
    }
    private static JButton button(String text,boolean primary){
        JButton b=new JButton(text){
            protected void paintComponent(Graphics graphics){
                Graphics2D g=RankingPanel.quality(graphics);
                if(primary){
                    g.setPaint(new GradientPaint(0,0,isEnabled()?Theme.PRIMARY:new Color(28,43,65),
                            getWidth(),getHeight(),isEnabled()?new Color(46,89,192):new Color(22,35,53)));
                }else g.setColor(getModel().isRollover()?new Color(26,46,65):new Color(17,31,48));
                g.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                if(hasFocus()){g.setColor(Theme.ACCENT_CYAN);g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,10,10);}
                g.dispose();super.paintComponent(graphics);
            }
        };
        b.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD,11f));
        b.setForeground(primary?Theme.TEXT_PRIMARY:Theme.ACCENT_CYAN);
        b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8,primary?14:9,8,primary?14:9));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }
    private void submit(){
        String peer=session.activePeer();if(peer==null||session.pending(peer)||"OFFLINE".equals(presence.apply(peer)))return;
        String text=input.getText().strip();if(text.isEmpty())return;
        try {
            String payload=ChatMessage.encode(text);
            session.sending(peer,text);setErrorText(" ");updateSendState();
            send.accept("CHAT_SEND|"+peer+"|"+payload);
        }catch(IllegalArgumentException|IllegalStateException e){
            session.failed(peer,e.getMessage());showError(e.getMessage());
        }
    }
    private void checkSendTimeouts(){
        java.util.Set<String> expired=session.expirePending();
        String peer=session.activePeer();
        if(peer!=null && expired.contains(peer))showError(session.error(peer));
        else updateSendState();
    }
    @Override public void addNotify(){super.addNotify();sendTimer.start();}
    @Override public void removeNotify(){sendTimer.stop();super.removeNotify();}
    private void setErrorText(String message){
        boolean visible=message!=null&&!message.isBlank();
        error.setText(visible?message:" ");error.setToolTipText(message);error.setVisible(visible);revalidate();
    }
    void showError(String message){setErrorText(message);updateSendState();}
    private void updateSendState(){
        String peer=session.activePeer();
        boolean online=peer!=null&&!"OFFLINE".equals(presence.apply(peer));
        boolean pending=peer!=null&&session.pending(peer);
        input.setEnabled(online);
        sendButton.setEnabled(online&&!pending&&!input.getText().isBlank());
        sendButton.setText(pending?"Đang gửi...":"Gửi");
        counter.setText(input.getText().length()+"/"+ChatMessage.MAX_LENGTH);
    }
    void refresh(boolean read){
        session.expirePending();
        String peer=session.activePeer();if(peer==null)return;
        setErrorText(session.error(peer));
        if(read)session.markRead(peer);
        person.setText(peer);person.setToolTipText(peer);
        String state=presence.apply(peer);
        status.setText("OFFLINE".equals(state)?"Ngoại tuyến":"IN_MATCH".equals(state)?"Đang trong trận":"Đang online");
        status.setForeground("OFFLINE".equals(state)?Theme.TEXT_MUTED:"IN_MATCH".equals(state)?Theme.WARNING:Theme.SUCCESS);
        updating=true;
        if(!input.getText().equals(session.draft(peer)))input.setText(session.draft(peer));
        updating=false;updateSendState();
        JScrollBar bar=messageScroll.getVerticalScrollBar();
        boolean nearBottom=bar.getValue()+bar.getVisibleAmount()>=bar.getMaximum()-32;
        boolean changed=transcript.setMessages(peer,session.messages(peer));
        java.util.List<ChatSession.Message> messages=session.messages(peer);
        boolean ownLast=!messages.isEmpty()&&messages.get(messages.size()-1).sender.equals(username);
        if(changed&&(nearBottom||ownLast))SwingUtilities.invokeLater(() -> {
            if(peer.equals(session.activePeer()))bar.setValue(bar.getMaximum());
        });
        repaint();
    }
}
