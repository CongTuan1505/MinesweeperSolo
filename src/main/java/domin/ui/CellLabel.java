package domin.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Ô trên bàn dò mìn — Modern Tactical Gaming UI.
 * Vector-drawn cho mọi trạng thái (Covered, Opened, Flagged, Question, Mine).
 */
public class CellLabel extends JLabel {

    /** Callback khi người chơi click vào ô. */
    public interface CellClickListener {
        void onLeftClick(int r, int c);
        void onRightClick(int r, int c);
    }

    public enum CellState {
        COVERED, OPENED, FLAGGED, QUESTION, MINE
    }

    public static final int CELL_SIZE = 38;

    private final int row;
    private final int col;
    private CellState state = CellState.COVERED;
    private int number = 0;
    private boolean hovered = false;
    private boolean pressed = false;

    public CellLabel(int r, int c, final CellClickListener listener) {
        super("", SwingConstants.CENTER);
        this.row = r;
        this.col = c;
        setOpaque(false);
        setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        setMinimumSize(new Dimension(CELL_SIZE, CELL_SIZE));
        setMaximumSize(new Dimension(CELL_SIZE, CELL_SIZE));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
                if (listener == null) return;
                if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                    listener.onLeftClick(row, col);
                } else if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    listener.onRightClick(row, col);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                repaint();
            }
        });
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public void setOpened(int num) {
        this.state = CellState.OPENED;
        this.number = num;
        repaint();
    }

    public void setFlag(int flagState) {
        if (flagState == 1) {
            this.state = CellState.FLAGGED;
        } else if (flagState == 2) {
            this.state = CellState.QUESTION;
        } else {
            this.state = CellState.COVERED;
        }
        repaint();
    }

    public void setMine() {
        this.state = CellState.MINE;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        switch (state) {
            case COVERED: {
                if (pressed) {
                    g2.setColor(new Color(0x0E, 0x15, 0x24));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 4, 4));
                } else if (hovered) {
                    g2.setColor(new Color(0x22, 0x30, 0x4C));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 4, 4));
                    g2.setColor(new Color(0x38, 0xBD, 0xF8, 140));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 4, 4));
                } else {
                    g2.setColor(new Color(0x18, 0x22, 0x38));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 4, 4));
                    // Bevel highlight top-left
                    g2.setColor(new Color(0x28, 0x38, 0x54));
                    g2.drawLine(1, 1, w - 2, 1);
                    g2.drawLine(1, 1, 1, h - 2);
                    // Dark bevel bottom-right
                    g2.setColor(new Color(0x0E, 0x15, 0x24));
                    g2.drawLine(1, h - 2, w - 2, h - 2);
                    g2.drawLine(w - 2, 1, w - 2, h - 2);
                }
                break;
            }
            case FLAGGED: {
                // Covered background
                g2.setColor(new Color(0x18, 0x22, 0x38));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 4, 4));
                g2.setColor(new Color(0x28, 0x38, 0x54));
                g2.drawLine(1, 1, w - 2, 1);
                g2.drawLine(1, 1, 1, h - 2);

                int cx = w / 2;
                int cy = h / 2;

                // Flag pole & base
                g2.setColor(new Color(0x94, 0xA3, 0xB8));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 3, cy - 9, cx - 3, cy + 9);
                g2.drawLine(cx - 7, cy + 9, cx + 2, cy + 9);

                // Triangular red flag banner
                g2.setColor(new Color(0xEF, 0x44, 0x44));
                Polygon flagPoly = new Polygon();
                flagPoly.addPoint(cx - 3, cy - 9);
                flagPoly.addPoint(cx + 8, cy - 4);
                flagPoly.addPoint(cx - 3, cy + 1);
                g2.fillPolygon(flagPoly);

                // Flag highlight
                g2.setColor(new Color(0xFA, 0x71, 0x71));
                g2.drawLine(cx - 3, cy - 8, cx + 6, cy - 4);
                break;
            }
            case QUESTION: {
                g2.setColor(new Color(0x18, 0x22, 0x38));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 4, 4));
                g2.setColor(new Color(0xFB, 0xBF, 0x24));
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("?", cx(w, fm, "?"), cy(h, fm));
                break;
            }
            case OPENED: {
                // Flat recessed tile
                g2.setColor(new Color(0x0C, 0x12, 0x20));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(0x15, 0x1E, 0x2E));
                g2.drawRect(0, 0, w - 1, h - 1);

                if (number > 0) {
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
                    g2.setColor(getNeonNumberColor(number));
                    String numStr = String.valueOf(number);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(numStr, cx(w, fm, numStr), cy(h, fm));
                }
                break;
            }
            case MINE: {
                // Danger explosion background
                GradientPaint gp = new GradientPaint(0, 0, new Color(0xDC, 0x26, 0x26),
                        w, h, new Color(0x99, 0x1B, 0x1B));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 4, 4));

                // Mine sphere
                int mcx = w / 2;
                int mcy = h / 2;
                g2.setColor(new Color(0x0F, 0x17, 0x2A));
                g2.fillOval(mcx - 8, mcy - 8, 16, 16);

                // Spikes
                g2.setColor(new Color(0x0F, 0x17, 0x2A));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(mcx - 11, mcy, mcx + 11, mcy);
                g2.drawLine(mcx, mcy - 11, mcx, mcy + 11);
                g2.drawLine(mcx - 8, mcy - 8, mcx + 8, mcy + 8);
                g2.drawLine(mcx - 8, mcy + 8, mcx + 8, mcy - 8);

                // Highlight shine
                g2.setColor(Color.WHITE);
                g2.fillOval(mcx - 4, mcy - 4, 4, 4);
                break;
            }
        }

        g2.dispose();
    }

    private int cx(int w, FontMetrics fm, String s) {
        return (w - fm.stringWidth(s)) / 2;
    }

    private int cy(int h, FontMetrics fm) {
        return (h + fm.getAscent() - fm.getDescent()) / 2;
    }

    public static Color getNeonNumberColor(int n) {
        switch (n) {
            case 1: return new Color(0x38, 0xBD, 0xF8); // Neon Cyan
            case 2: return new Color(0x34, 0xD3, 0x99); // Neon Emerald
            case 3: return new Color(0xF8, 0x71, 0x71); // Neon Coral Red
            case 4: return new Color(0xC0, 0x84, 0xFC); // Neon Purple
            case 5: return new Color(0xFB, 0xBF, 0x24); // Neon Gold
            case 6: return new Color(0x2D, 0xD4, 0xBF); // Neon Teal
            case 7: return new Color(0xF4, 0x72, 0xB6); // Neon Pink
            case 8: return Color.WHITE;
            default: return Color.WHITE;
        }
    }
}
