package domin.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Podium and detailed standings share the server's ordered, mode-specific data. */
final class RankingPanel extends JPanel {
    final BoardSelector selector = new BoardSelector();
    final JPanel rows = new JPanel();
    private final JPanel podium = new JPanel(new GridLayout(1,3,16,0));
    private final JLabel summary = label("Đang tải bảng xếp hạng...",11,Theme.TEXT_SECONDARY);
    private final String username;
    private BufferedImage avatar;
    private final List<Entry> entries = new ArrayList<>();
    private static final Color GOLD = new Color(250,200,94), SILVER = new Color(174,197,222), BRONZE = new Color(226,151,109);

    private static final class Entry {
        final String name;
        final int score,wins,losses,draws,rank;
        Entry(String[] v,int rank) {
            name=v[0];score=Integer.parseInt(v[1]);wins=Integer.parseInt(v[2]);
            losses=Integer.parseInt(v[3]);draws=Integer.parseInt(v[4]);this.rank=rank;
        }
        int rate() { int total=wins+losses+draws;return total==0?0:wins*100/total; }
    }

    RankingPanel(String username, Consumer<String> request) {
        this.username=username;
        setOpaque(false);setLayout(new BorderLayout(0,18));
        setBorder(new EmptyBorder(24,32,20,32));
        JPanel header=new JPanel(new BorderLayout(20,0));header.setOpaque(false);
        JPanel titles=new JPanel();titles.setOpaque(false);titles.setLayout(new BoxLayout(titles,BoxLayout.Y_AXIS));
        JLabel eyebrow=label("ĐẤU TRƯỜNG DÒ MÌN",10,Theme.ACCENT_CYAN);
        JLabel title=label("BẢNG XẾP HẠNG CAO THỦ",24,Theme.TEXT_PRIMARY);
        title.setFont(Theme.TITLE_FONT.deriveFont(Font.BOLD,24f));
        titles.add(eyebrow);titles.add(Box.createVerticalStrut(4));titles.add(title);
        titles.add(Box.createVerticalStrut(6));titles.add(summary);
        header.add(titles,BorderLayout.WEST);
        selector.setPreferredSize(new Dimension(224,48));
        JPanel picker=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,10));picker.setOpaque(false);picker.add(selector);
        selector.addActionListener(e -> request.accept(((domin.BoardType)selector.getSelectedItem()).id));
        header.add(picker,BorderLayout.EAST);add(header,BorderLayout.NORTH);

        JPanel content=new JPanel(new BorderLayout(0,20));content.setOpaque(false);
        podium.setOpaque(false);podium.setPreferredSize(new Dimension(0,204));
        content.add(podium,BorderLayout.NORTH);
        JPanel table=surface(new Color(38,53,78));table.setLayout(new BorderLayout(0,12));
        table.setBorder(new EmptyBorder(16,16,12,16));
        JPanel tableHeader=new JPanel(new BorderLayout(0,14));tableHeader.setOpaque(false);
        JPanel section=new JPanel(new BorderLayout());section.setOpaque(false);
        section.add(label("BẢNG THÀNH TÍCH",12,Theme.TEXT_PRIMARY),BorderLayout.WEST);
        section.add(label("ĐIỂM  ·  THẮNG  ·  THUA  ·  HÒA  ·  TỈ LỆ",9,Theme.TEXT_MUTED),BorderLayout.EAST);
        tableHeader.add(section,BorderLayout.NORTH);
        String[] headings={"HẠNG","NGƯỜI CHƠI","ĐIỂM SỐ","THẮNG","THUA","HÒA","TỈ LỆ THẮNG"};
        Component[] cols=new Component[headings.length];
        for(int i=0;i<headings.length;i++) cols[i]=center(headings[i],10,Theme.TEXT_MUTED);
        JPanel columnNames=columns(cols);columnNames.setBorder(new EmptyBorder(0,12,0,12));
        tableHeader.add(columnNames,BorderLayout.CENTER);table.add(tableHeader,BorderLayout.NORTH);
        rows.setOpaque(false);rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));
        JScrollPane scroll=new JScrollPane(rows);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(5,0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors(){thumbColor=new Color(43,64,91);trackColor=new Color(12,19,34);}
            protected JButton createDecreaseButton(int o){JButton b=new JButton();b.setPreferredSize(new Dimension());return b;}
            protected JButton createIncreaseButton(int o){return createDecreaseButton(o);}
        });
        table.add(scroll,BorderLayout.CENTER);content.add(table,BorderLayout.CENTER);add(content,BorderLayout.CENTER);
        render();
    }

    void setAvatar(BufferedImage image) { avatar=image;podium.repaint();rows.repaint(); }
    void setData(String raw) {
        entries.clear();
        if(raw!=null) for(String record:raw.split(";")) {
            String[] v=record.split("~");
            if(v.length<5 || "admin".equalsIgnoreCase(v[0]))continue;
            try { entries.add(new Entry(v,entries.size()+1)); }catch(NumberFormatException ignored){}
        }
        render();
    }
    private void render() {
        podium.removeAll();rows.removeAll();
        for(int rank:new int[]{2,1,3}) {
            Entry entry=entries.size()>=rank?entries.get(rank-1):null;
            JPanel wrap=new JPanel(new BorderLayout());wrap.setOpaque(false);
            wrap.setBorder(new EmptyBorder(rank==1?0:14,0,0,0));
            wrap.add(podiumCard(entry,rank));podium.add(wrap);
        }
        Entry own=null;
        for(Entry entry:entries) {
            if(entry.name.equals(username))own=entry;
            rows.add(row(entry));rows.add(Box.createVerticalStrut(7));
        }
        summary.setText(entries.size()+" người chơi"
                +(own==null?"  ·  Thành tích riêng theo chế độ":"  ·  Vị trí của bạn: #"+own.rank+"  ·  "+own.score+" điểm"));
        if(entries.isEmpty()) {
            JLabel empty=center("Chưa có dữ liệu xếp hạng cho chế độ này",13,Theme.TEXT_SECONDARY);
            empty.setBorder(new EmptyBorder(24,0,24,0));rows.add(empty);
        }
        podium.revalidate();podium.repaint();rows.revalidate();rows.repaint();
    }
    private JPanel podiumCard(Entry entry,int rank) {
        Color accent=medal(rank);
        JPanel card=new JPanel() {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g=quality(graphics);int w=getWidth(),h=getHeight();
                g.setPaint(new GradientPaint(0,0,new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),30),
                        w,h,new Color(13,21,38)));
                g.fillRoundRect(1,1,w-2,h-2,18,18);
                g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),rank==1?150:65));
                g.drawRoundRect(1,1,w-3,h-3,18,18);
                g.setColor(accent);g.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD,10f));
                g.drawString(rank==1?"DẪN ĐẦU":"HẠNG "+rank,18,24);
                if(entry!=null && entry.name.equals(username)) {
                    g.setColor(Theme.ACCENT_CYAN);g.drawString("BẠN",w-43,24);
                }
                int size=rank==1?54:46,y=rank==1?38:32;
                drawAvatar(g,entry==null?"?":entry.name,w/2-size/2,y,size,accent,
                        entry!=null&&entry.name.equals(username)?avatar:null);
                g.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD,16f));g.setColor(Theme.TEXT_PRIMARY);
                centered(g,entry==null?"Đang chờ cao thủ":fit(g,entry.name,w-32),w/2,y+size+25);
                g.setFont(Theme.STAT_FONT.deriveFont(Font.BOLD,rank==1?28f:24f));g.setColor(accent);
                centered(g,entry==null?"—":entry.score+" ĐIỂM",w/2,y+size+60);
                g.setFont(Theme.SMALL_FONT.deriveFont(11f));g.setColor(Theme.TEXT_SECONDARY);
                centered(g,entry==null?"Vị trí chưa có người chơi":entry.wins+" thắng  ·  "+entry.rate()+"% tỉ lệ thắng",w/2,h-17);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.getAccessibleContext().setAccessibleName("Hạng "+rank+(entry==null?", chưa có người chơi":", "+entry.name+", "+entry.score+" điểm"));
        return card;
    }
    private JPanel row(Entry e) {
        boolean me=e.name.equals(username);
        JPanel row=new JPanel(new BorderLayout()) {
            boolean hovered;
            {addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent ev){hovered=true;repaint();}
                public void mouseExited(MouseEvent ev){hovered=false;repaint();}
            });}
            protected void paintComponent(Graphics graphics) {
                Graphics2D g=quality(graphics);
                g.setColor(me?new Color(20,43,65):hovered?new Color(23,34,54):new Color(16,25,43));
                g.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                if(me) {g.setColor(new Color(34,211,238,130));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);}
                g.dispose();
            }
        };
        row.setOpaque(false);row.setBorder(new EmptyBorder(8,12,8,12));
        row.setPreferredSize(new Dimension(0,60));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,60));
        JLabel rank=center(String.format("%02d",e.rank),16,e.rank<=3?medal(e.rank):Theme.TEXT_SECONDARY);
        JPanel player=new JPanel(new BorderLayout(10,0));player.setOpaque(false);
        JComponent portrait=new JComponent(){
            protected void paintComponent(Graphics graphics){
                Graphics2D g=quality(graphics);drawAvatar(g,e.name,0,2,32,me?Theme.ACCENT_CYAN:Theme.PRIMARY,me?avatar:null);g.dispose();
            }
        };
        portrait.setPreferredSize(new Dimension(34,36));player.add(portrait,BorderLayout.WEST);
        JPanel names=new JPanel(new GridLayout(2,1));names.setOpaque(false);
        names.add(label(e.name,13,me?Theme.ACCENT_CYAN:Theme.TEXT_PRIMARY));
        names.add(label(me?"BẠN":"NGƯỜI CHƠI",8,Theme.TEXT_MUTED));player.add(names);
        JPanel rate=new JPanel(new BorderLayout(0,4));rate.setOpaque(false);
        rate.add(center(e.rate()+"%",11,Theme.TEXT_SECONDARY),BorderLayout.NORTH);
        JProgressBar progress=new JProgressBar(0,100);progress.setValue(e.rate());
        progress.setBorderPainted(false);progress.setForeground(Theme.ACCENT_CYAN);progress.setBackground(new Color(30,43,63));
        progress.setUI(new javax.swing.plaf.basic.BasicProgressBarUI());
        progress.setPreferredSize(new Dimension(80,4));
        JPanel track=new JPanel(new BorderLayout());track.setOpaque(false);track.setBorder(new EmptyBorder(0,18,3,18));track.add(progress);
        rate.add(track,BorderLayout.SOUTH);
        row.add(columns(new Component[]{rank,player,center(""+e.score,17,GOLD),
                center(""+e.wins,13,Theme.SUCCESS),center(""+e.losses,13,new Color(248,113,113)),
                center(""+e.draws,13,Theme.WARNING),rate}));
        row.setToolTipText(e.wins+" thắng · "+e.losses+" thua · "+e.draws+" hòa");
        return row;
    }
    private static JPanel columns(Component[] components) {
        double[] weights={.65,2.0,1,.8,.8,.8,1.2};
        JPanel p=new JPanel(new GridBagLayout());p.setOpaque(false);
        for(int i=0;i<components.length;i++) {
            GridBagConstraints c=new GridBagConstraints();c.gridx=i;c.gridy=0;c.weightx=weights[i];c.weighty=1;
            c.fill=GridBagConstraints.BOTH;c.insets=new Insets(0,5,0,5);
            // Equal zero preferred widths keep columns aligned across independently laid out rows.
            JPanel slot=new JPanel(new BorderLayout());slot.setOpaque(false);slot.add(components[i]);
            slot.setPreferredSize(new Dimension(0,36));slot.setMinimumSize(new Dimension(0,0));p.add(slot,c);
        }
        return p;
    }
    static JPanel surface(Color edge) {
        JPanel panel=new JPanel(){
            protected void paintComponent(Graphics graphics){
                Graphics2D g=quality(graphics);g.setColor(new Color(12,19,35,235));
                g.fillRoundRect(0,0,getWidth(),getHeight(),18,18);g.setColor(edge);
                g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);g.dispose();
            }
        };panel.setOpaque(false);return panel;
    }
    static JLabel label(String text,int size,Color color){
        JLabel label=new JLabel(text);label.setFont(Theme.BODY_FONT.deriveFont((float)size));label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);return label;
    }
    private static JLabel center(String text,int size,Color color){
        JLabel l=label(text,size,color);l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD));return l;
    }
    static Graphics2D quality(Graphics graphics){
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);return g;
    }
    static Color medal(int rank){return rank==1?GOLD:rank==2?SILVER:rank==3?BRONZE:Theme.ACCENT_CYAN;}
    private static void centered(Graphics2D g,String text,int x,int y){g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    private static String fit(Graphics2D g,String text,int width){
        if(g.getFontMetrics().stringWidth(text)<=width)return text;
        while(text.length()>1&&g.getFontMetrics().stringWidth(text+"…")>width)text=text.substring(0,text.length()-1);
        return text+"…";
    }
    static void drawAvatar(Graphics2D g,String name,int x,int y,int size,Color color,BufferedImage image){
        g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),22));g.fillOval(x-4,y-4,size+8,size+8);
        if(image!=null){Shape clip=g.getClip();g.clip(new Ellipse2D.Double(x,y,size,size));g.drawImage(image,x,y,size,size,null);g.setClip(clip);}
        else {
            g.setPaint(new GradientPaint(x,y,new Color(color.getRed(),color.getGreen(),color.getBlue(),150),x+size,y+size,new Color(28,42,67)));
            g.fillOval(x,y,size,size);g.setColor(Theme.TEXT_PRIMARY);g.setFont(Theme.TITLE_FONT.deriveFont(Font.BOLD,size*.4f));
            String initial=name==null||name.isEmpty()?"?":name.substring(0,1).toUpperCase();
            centered(g,initial,x+size/2,y+size/2+g.getFontMetrics().getAscent()/3);
        }
        g.setColor(color);g.setStroke(new BasicStroke(1.4f));g.drawOval(x,y,size,size);
    }
}
