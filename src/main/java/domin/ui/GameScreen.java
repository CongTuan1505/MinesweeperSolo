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
        default void onEmote(String id) {}
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
    private final String myName;
    private final ProfileCard myProfile;
    private final ProfileCard oppProfile;
    private long lastEmote;
    public static final String[] EMOTE_IDS = {"HELLO", "GOOD", "WOW", "THINK", "GG"};
    public static final String[] EMOTE_TEXT = {"Ch\u00e0o b\u1ea1n!", "Hay qu\u00e1!", "B\u1ea5t ng\u1edd!", "\u0110ang suy ngh\u0129...", "Tr\u1eadn \u0111\u1ea5u hay!"};

    public GameScreen(String myName, String oppName,
                      int rows, int cols, int mines, int timeLimit,
                      GameCallback callback) {
        this.callback = callback;
        this.myName = myName;
        myProfile = new ProfileCard(myName, "B\u1ea0N", Theme.ACCENT_CYAN);
        oppProfile = new ProfileCard(oppName, "\u0110\u1ed0I TH\u1ee6", Theme.DANGER);
        this.rows = rows;
        this.cols = cols;
        this.timeLimit = timeLimit;
        this.cells = new CellLabel[rows][cols];
        this.revealed = new boolean[rows][cols];

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(18, 24, 18, 24));

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
        topHUD.setBorder(new EmptyBorder(14, 18, 14, 18));

        // Center: Match Mode & Glowing Timer
        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);
        JLabel matchMode = new JLabel("CHIẾN TRƯỜNG 1v1  •  " + rows + "×" + cols + "  •  " + mines + " MÌN");
        matchMode.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 9));
        matchMode.setForeground(new Color(0x64, 0x74, 0x8B));
        matchMode.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerLabel.setFont(new Font(Theme.STAT_FONT.getFamily(), Font.BOLD, 28));
        timerLabel.setForeground(Theme.ACCENT_CYAN);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerBox.add(Box.createVerticalGlue());
        centerBox.add(matchMode);
        centerBox.add(Box.createVerticalStrut(2));
        centerBox.add(timerLabel);
        JLabel versus = new JLabel("LIVE  /  1 VS 1");
        versus.setForeground(Theme.TEXT_SECONDARY);
        versus.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        versus.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerBox.add(Box.createVerticalStrut(6));
        centerBox.add(versus);
        centerBox.add(Box.createVerticalGlue());

        topHUD.add(myProfile, BorderLayout.WEST);
        topHUD.add(centerBox, BorderLayout.CENTER);
        topHUD.add(oppProfile, BorderLayout.EAST);
        updateTimer(0);

        // ================================================================
        //  2. CENTER: The Minesweeper Board (Tactical Grid)
        // ================================================================
        JPanel boardWrap = new JPanel(new GridBagLayout());
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
        boardFrame.setBorder(new EmptyBorder(14, 14, 14, 14));

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
        bottomHUD.setBorder(new EmptyBorder(14, 18, 14, 18));

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

        JPanel reactions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        reactions.setOpaque(false);
        for (int i = 0; i < EMOTE_IDS.length; i++) {
            final String id = EMOTE_IDS[i];
            JButton button = new ReactionButton(i);
            button.getAccessibleContext().setAccessibleName(EMOTE_TEXT[i]);
            button.setToolTipText(EMOTE_TEXT[i]);
            button.setFocusPainted(false);
            button.setForeground(Theme.TEXT_PRIMARY);
            button.setBackground(new Color(0x18, 0x22, 0x38));
            button.addActionListener(e -> {
                long now = System.currentTimeMillis();
                if (!over && now - lastEmote >= 2000) {
                    lastEmote = now;
                    callback.onEmote(id);
                }
            });
            reactions.add(button);
        }
        JPanel progress = new JPanel(new GridLayout(1, 2, 24, 0));
        progress.setOpaque(false);
        progress.add(myBar);
        progress.add(oppBar);
        bottomHUD.add(progress);
        bottomHUD.add(Box.createVerticalStrut(10));
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        toolbar.add(notifyLabel, BorderLayout.WEST);
        toolbar.add(reactions, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        controls.add(new SoundToggleButton());
        controls.add(surrenderBtn);
        toolbar.add(controls, BorderLayout.EAST);
        bottomHUD.add(toolbar);

        add(topHUD, BorderLayout.NORTH);
        JScrollPane boardScroll = new JScrollPane(boardWrap);
        boardScroll.setBorder(BorderFactory.createEmptyBorder());
        boardScroll.setOpaque(false);
        boardScroll.getViewport().setOpaque(false);
        boardScroll.getHorizontalScrollBar().setUnitIncrement(CellLabel.CELL_SIZE);
        boardScroll.getVerticalScrollBar().setUnitIncrement(CellLabel.CELL_SIZE);
        boardScroll.setPreferredSize(new Dimension(
                Math.max(930, Math.min(1260, cols * (CellLabel.CELL_SIZE + 2) + 48)),
                Math.min(680, rows * (CellLabel.CELL_SIZE + 2) + 60)));
        add(boardScroll, BorderLayout.CENTER);
        add(bottomHUD, BorderLayout.SOUTH);
    }

    public void updateProfile(String player, int score, int wins, int losses, int draws,
                              int rank, String avatar) {
        ProfileCard card = myProfile.player.equals(player) ? myProfile :
                oppProfile.player.equals(player) ? oppProfile : null;
        if (card != null) card.update(score, wins, losses, draws, rank, avatar);
    }

    public void showEmote(String player, String id) {
        if (over) return;
        ProfileCard card = myName.equals(player) ? myProfile :
                oppProfile.player.equals(player) ? oppProfile : null;
        if (card == null) return;
        for (int i = 0; i < EMOTE_IDS.length; i++) {
            if (EMOTE_IDS[i].equals(id)) {
                card.reaction.setText(EMOTE_TEXT[i]);
                card.reaction.setIcon(new EmoteIcon(i, 20));
                card.reactionTimer.restart();
                break;
            }
        }
    }

    private static class ReactionButton extends JButton {
        ReactionButton(int kind) {
            setIcon(new EmoteIcon(kind, 26));
            setPreferredSize(new Dimension(46, 42));
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? new Color(35, 75, 94) :
                    getModel().isRollover() ? new Color(30, 51, 73) : new Color(21, 31, 49));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setColor(getModel().isRollover() || isFocusOwner() ? Theme.ACCENT_CYAN : new Color(44, 59, 82));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Vector reactions render consistently without relying on emoji fonts. */
    private static class EmoteIcon implements Icon {
        private final int kind, size;
        EmoteIcon(int kind, int size) { this.kind = kind; this.size = size; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D v = (Graphics2D) g.create();
            v.translate(x, y);
            v.scale(size / 32.0, size / 32.0);
            v.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            v.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            v.setPaint(new GradientPaint(0, 2, new Color(255, 222, 113), 0, 30, new Color(245, 167, 48)));
            v.fillOval(2, 2, 28, 28);
            v.setColor(new Color(94, 56, 27));
            if (kind == 4) {
                v.setColor(new Color(225, 75, 97));
                for (int eye : new int[]{10, 22}) {
                    v.fillOval(eye-4, 9, 5, 5); v.fillOval(eye-1, 9, 5, 5);
                    v.fillPolygon(new int[]{eye-4, eye+4, eye}, new int[]{12,12,18}, 3);
                }
            } else {
                v.fillOval(9, 11, 3, kind == 2 ? 5 : 3);
                v.fillOval(20, 11, 3, kind == 2 ? 5 : 3);
            }
            v.setColor(new Color(94, 56, 27));
            if (kind == 2) v.drawOval(13, 20, 6, 7);
            else if (kind == 3) {
                v.drawLine(12, 23, 20, 21); v.drawLine(8, 7, 13, 8);
                v.setColor(new Color(255, 215, 117)); v.fillRoundRect(20, 21, 8, 10, 5, 5);
                v.setColor(new Color(94, 56, 27)); v.drawLine(22, 24, 25, 22);
            } else {
                v.drawArc(10, 15, 13, 10, 185, 170);
                if (kind == 1) { v.setColor(Color.WHITE); v.fillRoundRect(12, 21, 9, 3, 2, 2); }
                if (kind == 0) {
                    v.setColor(new Color(255, 225, 139)); v.fillOval(24, 20, 7, 10);
                    v.setColor(new Color(94, 56, 27)); v.drawLine(27, 21, 27, 26);
                }
            }
            v.dispose();
        }
    }

    private static class ProfileCard extends JPanel {
        final String player;
        final JLabel avatar = new JLabel("", SwingConstants.CENTER);
        final JLabel stats = new JLabel("\u0110ang t\u1ea3i h\u1ed3 s\u01a1...");
        final JLabel record = new JLabel(" ");
        final JLabel reaction = new JLabel(" ");
        final Timer reactionTimer;
        final Color accent;

        ProfileCard(String player, String role, Color accent) {
            super(new BorderLayout(10, 0));
            this.player = player;
            this.accent = accent;
            setBorder(new EmptyBorder(10, 12, 10, 12));
            setOpaque(false);
            setPreferredSize(new Dimension(280, 116));
            avatar.setText(player.isEmpty() ? "?" : player.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
            avatar.setOpaque(false);
            avatar.setBorder(BorderFactory.createLineBorder(accent, 2, true));
            avatar.setBackground(new Color(0x18, 0x22, 0x38));
            avatar.setForeground(accent);
            avatar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            avatar.setPreferredSize(new Dimension(56, 56));
            JPanel picture = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 14));
            picture.setOpaque(false);
            picture.add(avatar);
            add(picture, BorderLayout.WEST);
            JPanel info = new JPanel(new GridLayout(5, 1, 0, 2));
            info.setOpaque(false);
            JLabel tag = new JLabel(role);
            tag.setForeground(accent);
            JLabel name = new JLabel(player);
            name.setForeground(Theme.TEXT_PRIMARY);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            stats.setForeground(Theme.TEXT_SECONDARY);
            record.setForeground(Theme.TEXT_SECONDARY);
            reaction.setForeground(accent);
            info.add(tag); info.add(name); info.add(stats); info.add(record); info.add(reaction);
            add(info, BorderLayout.CENTER);
            reactionTimer = new Timer(4000, e -> { reaction.setText(" "); reaction.setIcon(null); });
            reactionTimer.setRepeats(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24),
                    getWidth(), getHeight(), new Color(18, 26, 43)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 65));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }

        void update(int score, int wins, int losses, int draws, int rank, String image) {
            stats.setText(score + " \u0111i\u1ec3m  \u2022  " + (rank > 0 ? "H\u1ea1ng #" + rank : "Ch\u01b0a x\u1ebfp h\u1ea1ng"));
            record.setText(wins + " th\u1eafng \u00b7 " + losses + " thua \u00b7 " + draws + " h\u00f2a");
            if (image == null || image.isEmpty()) return;
            try {
                java.awt.image.BufferedImage decoded = javax.imageio.ImageIO.read(
                        new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(image)));
                if (decoded != null) {
                    avatar.setIcon(new ImageIcon(decoded.getScaledInstance(56, 56, Image.SCALE_SMOOTH)));
                    avatar.setText("");
                }
            } catch (java.io.IOException | IllegalArgumentException ignored) {}
        }
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
            setPreferredSize(new Dimension(0, 48));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
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
            g2.drawString(details, 2, 27);

            // Track background
            int barY = 36;
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
        myProfile.reactionTimer.stop();
        oppProfile.reactionTimer.stop();
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
