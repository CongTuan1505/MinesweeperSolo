package domin.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Three original diagrams, rendered at twice display resolution and exported as PNG assets. */
public final class GuideArtwork extends JPanel {
    private static final BufferedImage[] IMAGES=new BufferedImage[3];
    private final int kind;
    public GuideArtwork(int kind) {
        this.kind=kind;setOpaque(false);setPreferredSize(new Dimension(320,174));
        setMinimumSize(new Dimension(100,120));
        getAccessibleContext().setAccessibleName(new String[]{
            "Minh họa mở ô an toàn và đánh dấu mìn","Minh họa chuột trái mở ô, chuột phải cắm cờ","Minh họa thắng cộng 3, hòa cộng 1, thua cộng 0 điểm"}[kind]);
    }
    protected void paintComponent(Graphics graphics){
        Graphics2D g=RankingPanel.quality(graphics);
        if(IMAGES[kind]==null) {
            try(InputStream in=GuideArtwork.class.getResourceAsStream("/images/guide/"+kind+".png")){
                if(in!=null)IMAGES[kind]=ImageIO.read(in);
            }catch(IOException ignored){}
            if(IMAGES[kind]==null)IMAGES[kind]=create(kind);
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int w=Math.min(getWidth(),(int)(getHeight()*1.9)),h=w*200/380;
        g.drawImage(IMAGES[kind],(getWidth()-w)/2,(getHeight()-h)/2,w,h,null);g.dispose();
    }
    private static BufferedImage create(int kind){
        BufferedImage image=new BufferedImage(760,400,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=RankingPanel.quality(image.getGraphics());g.scale(2,2);
        g.setPaint(new GradientPaint(0,0,new Color(21,40,65),380,200,new Color(12,20,37)));
        g.fillRoundRect(0,0,380,200,18,18);
        g.setColor(new Color(35,58,82));g.drawRoundRect(1,1,378,198,18,18);
        if(kind==0)board(g);else if(kind==1)controls(g);else scoring(g);
        g.dispose();return image;
    }
    private static void text(Graphics2D g,String text,int x,int y,int size,Color color){
        g.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD,(float)size));g.setColor(color);g.drawString(text,x,y);
    }
    private static void flag(Graphics2D g,int x,int y,Color color){
        g.setColor(Theme.TEXT_SECONDARY);g.setStroke(new BasicStroke(2));g.drawLine(x,y,x,y+20);
        g.setColor(color);g.fillPolygon(new int[]{x,x+14,x},new int[]{y,y+6,y+12},3);
        g.fillRoundRect(x-5,y+20,13,3,2,2);
    }
    private static void board(Graphics2D g){
        int[][] values={{0,0,1,9,9},{0,1,2,9,9},{0,1,9,9,9},{0,1,1,2,9}};
        for(int r=0;r<4;r++)for(int c=0;c<5;c++){
            int x=24+c*34,y=30+r*34,v=values[r][c];
            g.setColor(v==9?new Color(37,58,85):new Color(21,38,57));g.fillRoundRect(x,y,30,30,5,5);
            g.setColor(new Color(58,83,108));g.drawRoundRect(x,y,30,30,5,5);
            if(v>0&&v<9)text(g,""+v,x+11,y+21,17,v==1?Theme.ACCENT_CYAN:new Color(167,139,250));
            if(r==1&&c==3)flag(g,x+10,y+4,new Color(251,171,91));
        }
        text(g,"MỞ Ô AN TOÀN",218,65,13,Theme.ACCENT_CYAN);
        text(g,"Tránh mìn.",218,91,12,Theme.TEXT_SECONDARY);
        text(g,"Nhanh hơn đối thủ.",218,111,11,Theme.TEXT_SECONDARY);
        g.setColor(new Color(32,197,151));g.setStroke(new BasicStroke(3));g.drawLine(223,139,231,147);g.drawLine(231,147,247,128);
        text(g,"HOÀN THÀNH",258,146,11,Theme.SUCCESS);
    }
    private static void mouse(Graphics2D g,int x,boolean right){
        g.setColor(new Color(27,45,69));g.fillRoundRect(x,29,68,100,32,32);
        Shape old=g.getClip();g.clip(new RoundRectangle2D.Double(x,29,68,100,32,32));
        g.setColor(right?new Color(94,64,70):new Color(25,92,106));g.fillRect(right?x+34:x,29,34,43);g.setClip(old);
        g.setColor(right?new Color(251,171,91):Theme.ACCENT_CYAN);g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x,29,68,100,32,32);g.drawLine(x+34,29,x+34,72);g.drawLine(x,72,x+68,72);
        g.fillRoundRect(x+31,43,6,16,4,4);
    }
    private static void controls(Graphics2D g){
        mouse(g,61,false);mouse(g,252,true);
        text(g,"CHUỘT TRÁI",47,154,12,Theme.ACCENT_CYAN);text(g,"Mở ô",73,176,12,Theme.TEXT_SECONDARY);
        text(g,"CHUỘT PHẢI",237,154,12,new Color(251,171,91));text(g,"Cắm / gỡ cờ",243,176,11,Theme.TEXT_SECONDARY);
        g.setColor(new Color(47,66,89));g.drawLine(190,28,190,176);
    }
    private static void scoring(Graphics2D g){
        String[] names={"THẮNG","HÒA","THUA"},points={"+3","+1","+0"};
        Color[] colors={Theme.SUCCESS,Theme.ACCENT_CYAN,new Color(248,113,113)};
        for(int i=0;i<3;i++){
            int x=18+i*121,y=i==0?25:41,h=i==0?153:137;
            g.setColor(new Color(colors[i].getRed(),colors[i].getGreen(),colors[i].getBlue(),20));
            g.fillRoundRect(x,y,104,h,12,12);g.setColor(colors[i]);g.drawRoundRect(x,y,104,h,12,12);
            text(g,names[i],x+26,y+30,12,colors[i]);text(g,points[i],x+24,y+82,34,colors[i]);
            text(g,"ĐIỂM",x+32,y+111,10,Theme.TEXT_SECONDARY);
        }
    }
    public static void main(String[] args)throws Exception {
        File directory=new File(args[0]);if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create "+directory);
        for(int i=0;i<3;i++)ImageIO.write(create(i),"png",new File(directory,i+".png"));
    }
}
