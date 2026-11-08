package domin.ui;

import domin.SoundEffects;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Text plus vector speaker icon avoids platform-dependent emoji fonts. */
public final class SoundToggleButton extends JButton {
    public SoundToggleButton() {
        setIcon(new Icon() {
            public int getIconWidth() { return 19; }
            public int getIconHeight() { return 18; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SoundEffects.shared().isEnabled() ? Theme.ACCENT_CYAN : Theme.TEXT_MUTED);
                g2.fillRect(x+1,y+6,4,6);
                g2.fillPolygon(new int[]{x+4,x+10,x+10},new int[]{y+6,y+2,y+16},3);
                g2.setStroke(new BasicStroke(1.5f));
                if (SoundEffects.shared().isEnabled()) {
                    g2.drawArc(x+7,y+4,8,10,-65,130);
                    g2.drawArc(x+8,y+1,11,16,-65,130);
                } else {
                    g2.drawLine(x+13,y+6,x+18,y+12); g2.drawLine(x+18,y+6,x+13,y+12);
                }
                g2.dispose();
            }
        });
        setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD,11f));
        setForeground(Theme.TEXT_SECONDARY);
        setBorder(new EmptyBorder(8,10,8,10));
        setContentAreaFilled(false); setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh();
        addActionListener(e -> {
            SoundEffects sounds = SoundEffects.shared();
            sounds.setEnabled(!sounds.isEnabled());
            refresh();
            if (sounds.isEnabled()) sounds.play(SoundEffects.Cue.CLICK);
        });
    }
    private void refresh() {
        boolean enabled = SoundEffects.shared().isEnabled();
        setText(enabled ? "Âm thanh: Bật" : "Âm thanh: Tắt");
        setToolTipText(enabled ? "Tắt âm thanh trò chơi" : "Bật âm thanh trò chơi");
        getAccessibleContext().setAccessibleName(getText());
        repaint();
    }
    @Override public void addNotify() { super.addNotify(); refresh(); }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getModel().isRollover() ? new Color(29,43,67) : new Color(16,25,44));
        g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
        g2.setColor(hasFocus() ? Theme.ACCENT_CYAN : Theme.BORDER_CARD);
        g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
        g2.dispose(); super.paintComponent(g);
    }
}
