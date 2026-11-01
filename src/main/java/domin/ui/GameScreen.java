package domin.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Màn hình thi đấu dò mìn 1v1 — Modern Dark Tactical Gaming UI.
 * Đồng bộ hoàn toàn màu sắc, typography và hiệu ứng với HomeScreen.
 */
public class GameScreen extends JPanel implements CellLabel.CellClickListener {

    /** Callback cho các hành động game. */
    public interface GameCallback {
        void onOpenCell(int r, int c);
        void onFlagCell(int r, int c);
        void onSurrender();
    }

    private final GameCallback callback;
    private final int rows;
    private final int cols;
    private final int timeLimit;

    private final CellLabel[][] cells;
    private final boolean[][] revealed;
    private final JLabel timerLabel = new JLabel("00:00 / 00:00", SwingConstants.CENTER);
    private final JLabel notifyLabel = new JLabel("Trận đấu bắt đầu!", SwingConstants.CENTER);
    private final PlayerProgressBar myBar;
    private final PlayerProgressBar oppBar;
    private boolean over = false;

    public GameScreen(String myName, String oppName,
                      int rows, int cols, int mines, int timeLimit,
                      GameCallback callback) {
        this.callback = callback;
        this.rows = rows;
        this.cols = cols;
        this.timeLimit = timeLimit;
        this.cells = new CellLabel[rows][cols];
        this.revealed = new boolean[rows][cols];

        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        // ================================================================
        //  1. TOP HUD: 1v1 Battle Header (Player vs Opponent + Timer)
        // ================================================================
        JPanel topHUD = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0C, 0x12, 0x24, 230));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        topHUD.setOpaque(false);
        topHUD.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Left Player (You)
        JPanel meBox = new JPanel();
        meBox.setLayout(new BoxLayout(meBox, BoxLayout.Y_AXIS));
        meBox.setOpaque(false);
        JLabel meTag = new JLabel("● BẠN");
        meTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        meTag.setForeground(Theme.ACCENT_CYAN);
        JLabel meNameLbl = new JLabel(myName);
        meNameLbl.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 14));
        meNameLbl.setForeground(Theme.TEXT_PRIMARY);
        meBox.add(meTag);
        meBox.add(Box.createVerticalStrut(2));
        meBox.add(meNameLbl);

        // Center: Match Mode & Glowing Timer
        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);
        JLabel matchMode = new JLabel("CHIẾN TRƯỜNG 1v1  •  9×9");
        matchMode.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 9));
        matchMode.setForeground(new Color(0x64, 0x74, 0x8B));
        matchMode.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerLabel.setFont(new Font(Theme.STAT_FONT.getFamily(), Font.BOLD, 18));
        timerLabel.setForeground(Theme.ACCENT_CYAN);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerBox.add(matchMode);
        centerBox.add(Box.createVerticalStrut(2));
        centerBox.add(timerLabel);

        // Right Opponent
        JPanel oppBox = new JPanel();
        oppBox.setLayout(new BoxLayout(oppBox, BoxLayout.Y_AXIS));
        oppBox.setOpaque(false);
        JLabel oppTag = new JLabel("ĐỐI THỦ ●");
        oppTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        oppTag.setForeground(new Color(0xF8, 0x71, 0x71));
        oppTag.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel oppNameLbl = new JLabel(oppName);
        oppNameLbl.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 14));
        oppNameLbl.setForeground(Theme.TEXT_PRIMARY);
        oppNameLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        oppBox.add(oppTag);
        oppBox.add(Box.createVerticalStrut(2));
        oppBox.add(oppNameLbl);

        topHUD.add(meBox, BorderLayout.WEST);
        topHUD.add(centerBox, BorderLayout.CENTER);
        topHUD.add(oppBox, BorderLayout.EAST);
        updateTimer(0);

        // ================================================================
        //  2. CENTER: The Minesweeper Board (Tactical Grid)
        // ================================================================
        JPanel boardWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        boardWrap.setOpaque(false);

        JPanel boardFrame = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0A, 0x0F, 0x20, 240));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(0x1E, 0x29, 0x42));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        boardFrame.setOpaque(false);
        boardFrame.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel board = new JPanel(new GridLayout(rows, cols, 2, 2));
        board.setOpaque(false);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new CellLabel(r, c, this);
                board.add(cells[r][c]);
            }
        }
        boardFrame.add(board, BorderLayout.CENTER);
        boardWrap.add(boardFrame);

        // ================================================================
        //  3. BOTTOM HUD: Dual Progress Bars + Notifications + Surrender
        // ================================================================
        JPanel bottomHUD = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0C, 0x12, 0x24, 230));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        bottomHUD.setLayout(new BoxLayout(bottomHUD, BoxLayout.Y_AXIS));
        bottomHUD.setOpaque(false);
        bottomHUD.setBorder(new EmptyBorder(10, 14, 10, 14));

        myBar = new PlayerProgressBar("BẠN (" + myName + ")", Theme.PRIMARY, Theme.ACCENT_CYAN);
        oppBar = new PlayerProgressBar("ĐỐI THỦ (" + oppName + ")", new Color(0xEA, 0x58, 0x0C), new Color(0xEF, 0x44, 0x44));

        notifyLabel.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        notifyLabel.setForeground(Theme.WARNING);
        notifyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton surrenderBtn = new JButton("BỎ CUỘC") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(Theme.DANGER);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    setForeground(Color.WHITE);
                } else {
                    g2.setColor(new Color(0x2A, 0x12, 0x1A, 200));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.setColor(new Color(0xEF, 0x44, 0x44, 130));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                    setForeground(new Color(0xF8, 0x71, 0x71));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        surrenderBtn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 11));
        surrenderBtn.setFocusPainted(false);
        surrenderBtn.setBorderPainted(false);
        surrenderBtn.setContentAreaFilled(false);
        surrenderBtn.setOpaque(false);
        surrenderBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        surrenderBtn.setPreferredSize(new Dimension(130, 30));
        surrenderBtn.setMaximumSize(new Dimension(130, 30));
        surrenderBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        surrenderBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { askSurrender(); }
        });

        bottomHUD.add(myBar);
        bottomHUD.add(Box.createVerticalStrut(6));
        bottomHUD.add(oppBar);
        bottomHUD.add(Box.createVerticalStrut(8));
        bottomHUD.add(notifyLabel);
        bottomHUD.add(Box.createVerticalStrut(8));
        bottomHUD.add(surrenderBtn);

        add(topHUD, BorderLayout.NORTH);
        add(boardWrap, BorderLayout.CENTER);
        add(bottomHUD, BorderLayout.SOUTH);
    }

    // ================================================================
    //  Custom Gaming Progress Bar Component
    // ================================================================
    private class PlayerProgressBar extends JPanel {
        private int percent = 0;
        private final String title;
        private final Color startColor;
        private final Color endColor;
        private String details = "0%  •  Cờ: 0  •  Đang chơi";

        PlayerProgressBar(String title, Color startColor, Color endColor) {
            this.title = title;
            this.startColor = startColor;
            this.endColor = endColor;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 32));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        }

        void update(int p, String flags, String status) {
            this.percent = Math.max(0, Math.min(100, p));
            this.details = percent + "%  •  Cờ: " + flags + "  •  " + status;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            // Title and Details row
            g2.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 10));
            g2.setColor(startColor);
            g2.drawString(title, 2, 11);

            g2.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 10));
            g2.setColor(Theme.TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(details, w - fm.stringWidth(details) - 2, 11);

            // Track background
            int barY = 18;
            int barH = 7;
            g2.setColor(new Color(0x10, 0x17, 0x2A));
            g2.fill(new RoundRectangle2D.Float(0, barY, w, barH, 4, 4));
            g2.setColor(new Color(0x1F, 0x2A, 0x40));
            g2.draw(new RoundRectangle2D.Float(0, barY, w - 1, barH, 4, 4));

            // Fill
            if (percent > 0) {
                int fillW = Math.max(6, (int) ((w - 2) * (percent / 100.0)));
                GradientPaint gp = new GradientPaint(0, 0, startColor, fillW, 0, endColor);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(1, barY + 1, fillW, barH - 2, 3, 3));
            }

            g2.dispose();
        }
    }

    // ================================================================
    //  Background Painting
    // ================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        GradientPaint gp = new GradientPaint(0, 0, new Color(0x07, 0x0A, 0x14),
                0, getHeight(), new Color(0x05, 0x07, 0x12));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    // ================================================================
    //  CellClickListener
    // ================================================================
    @Override
    public void onLeftClick(int r, int c) {
        if (!over) callback.onOpenCell(r, c);
    }

    @Override
    public void onRightClick(int r, int c) {
        if (!over) callback.onFlagCell(r, c);
    }

    // ================================================================
    //  Cập nhật trạng thái trận đấu
    // ================================================================
    public void showOpened(int r, int c, int number) {
        revealed[r][c] = true;
        cells[r][c].setOpened(number);
    }

    public void showFlag(int r, int c, int state) {
        cells[r][c].setFlag(state);
    }

    public void showMine(int r, int c) {
        cells[r][c].setMine();
    }

    public void updateTimer(int seconds) {
        timerLabel.setText(format(seconds) + " / " + format(timeLimit));
    }

    public void updateProgress(int myPercent, int oppPercent, String myFlags,
                               String oppFlags, String myStatus, String oppStatus) {
        myBar.update(myPercent, myFlags, myStatus);
        oppBar.update(oppPercent, oppFlags, oppStatus);
    }

    public void setNotify(String text) {
        notifyLabel.setText(text);
    }

    public void setGameOver() {
        over = true;
    }

    public boolean isOver() {
        return over;
    }

    // ================================================================
    //  Tiện ích
    // ================================================================
    private void askSurrender() {
        if (over) return;
        Component parent = getTopLevelAncestor();
        int choice = JOptionPane.showConfirmDialog(parent,
                "Bạn có chắc muốn bỏ cuộc? Bạn sẽ bị xử thua.", "Bỏ cuộc",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            callback.onSurrender();
        }
    }

    public static String format(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
