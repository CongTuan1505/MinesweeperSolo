package domin.ui;



import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * AdminScreen — Màn hình quản trị hệ thống dành riêng cho tài khoản Quản trị viên (Admin).
 * Giao diện Dark Modern Tactical, đồng bộ với Theme và HomeScreen.
 *
 * Tính năng chính:
 * 1. Bảng số liệu thống kê hệ thống (Trận đấu đang diễn ra, Người chơi online, Tổng tài khoản, Tổng trận đấu)
 * 2. DANH SÁCH CÁC CẶP NGƯỜI CHƠI ĐANG THI ĐẤU (Live Arena):
 *    - Hiển thị thông tin từng cặp (Người chơi 1, Người chơi 2, Thời gian, Tiến độ %, Số cờ, Trạng thái)
 *    - Click vào bất kỳ cặp đấu nào để mở màn hình thi đấu trực tiếp (Spectator Mode)
 * 3. Quản lý danh sách người chơi (Thống kê điểm số, tỉ lệ thắng, vai trò)
 * 4. Lịch sử tất cả các trận đấu trên server
 * 5. Chuyển đổi nhanh sang chế độ Sảnh chơi (Player Mode) hoặc Đăng xuất
 */
public class AdminScreen extends JPanel {

    public interface AdminCallback {
        void send(String message);
        void onSpectate(String matchId, String p1, String p2);
        void onSwitchToPlayerHome();
        void onLogout();
    }

    public static class MatchItem {
        public String p1;
        public String p2;
        public String matchId;
        public int elapsedSec;
        public int p1Percent;
        public int p2Percent;
        public int p1Flags;
        public int p2Flags;
        public String p1Status;
        public String p2Status;

        public MatchItem(String p1, String p2, String matchId, int elapsedSec,
                         int p1Percent, int p2Percent, int p1Flags, int p2Flags,
                         String p1Status, String p2Status) {
            this.p1 = p1;
            this.p2 = p2;
            this.matchId = matchId;
            this.elapsedSec = elapsedSec;
            this.p1Percent = p1Percent;
            this.p2Percent = p2Percent;
            this.p1Flags = p1Flags;
            this.p2Flags = p2Flags;
            this.p1Status = p1Status;
            this.p2Status = p2Status;
        }
    }

    private final String adminName;
    private final AdminCallback callback;

    // Stat cards labels
    private JLabel activeMatchesValue;
    private JLabel onlinePlayersValue;
    private JLabel totalUsersValue;
    private JLabel totalMatchesValue;

    // Matches tab
    private JPanel matchesListPanel;
    private final List<MatchItem> activeMatches = new ArrayList<>();
    private JLabel matchCountHeader;

    // Users tab
    private DefaultTableModel usersTableModel;
    private JTable usersTable;

    // History tab
    private DefaultTableModel historyTableModel;
    private JTable historyTable;

    // Navigation
    private final CardLayout tabCardLayout = new CardLayout();
    private final JPanel tabContentPanel = new JPanel(tabCardLayout);
    private JButton tabMatchesBtn;
    private JButton tabUsersBtn;
    private JButton tabHistoryBtn;

    // Auto-refresh timer
    private Timer refreshTimer;

    public AdminScreen(String adminName, final AdminCallback callback) {
        this.adminName = adminName;
        this.callback = callback;

        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_DARK);

        // ==============================================================
        //  1. TOP NAVBAR: Admin Title, Stats Quick Info, Actions
        // ==============================================================
        JPanel topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        // ==============================================================
        //  2. MAIN CENTER: Stats KPI + Tabs Header + Content
        // ==============================================================
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 14));
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(16, 24, 20, 24));

        // Stats summary cards
        JPanel statsPanel = createStatsCardsPanel();
        centerWrapper.add(statsPanel, BorderLayout.NORTH);

        // Tab buttons bar & tab view
        JPanel contentCard = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(Theme.BORDER_CARD);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        contentCard.setOpaque(false);
        contentCard.setBorder(new EmptyBorder(14, 16, 16, 16));

        JPanel tabsHeader = createTabsHeader();
        contentCard.add(tabsHeader, BorderLayout.NORTH);

        // Build individual tab panels
        JPanel matchesTab = createMatchesTab();
        JPanel usersTab = createUsersTab();
        JPanel historyTab = createHistoryTab();

        tabContentPanel.setOpaque(false);
        tabContentPanel.add(matchesTab, "TAB_MATCHES");
        tabContentPanel.add(usersTab, "TAB_USERS");
        tabContentPanel.add(historyTab, "TAB_HISTORY");

        contentCard.add(tabContentPanel, BorderLayout.CENTER);
        centerWrapper.add(contentCard, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);

        // Setup timer to request fresh data every 3 seconds
        refreshTimer = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestData();
            }
        });
        refreshTimer.start();
        requestData();
    }

    public void stopTimer() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }

    public void requestData() {
        if (callback != null) {
            callback.send("ADMIN_GET_STATS");
            callback.send("ADMIN_GET_MATCHES");
            callback.send("ADMIN_GET_USERS");
            callback.send("ADMIN_GET_HISTORY");
        }
    }

    // ==============================================================
    //  UI Building Methods
    // ==============================================================

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_NAVBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER_CARD);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 24, 12, 24));

        // Left Branding
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandBox.setOpaque(false);

        // Admin Shield / Emblem Icon
        JLabel shieldIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Outer hexagon / shield
                int w = getWidth(), h = getHeight();
                Polygon p = new Polygon();
                p.addPoint(w / 2, 2);
                p.addPoint(w - 2, h / 4);
                p.addPoint(w - 2, h * 3 / 4);
                p.addPoint(w / 2, h - 2);
                p.addPoint(2, h * 3 / 4);
                p.addPoint(2, h / 4);

                g2.setColor(new Color(0x38, 0xBD, 0xF8, 40));
                g2.fill(p);
                g2.setColor(Theme.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(p);

                // Star / Key in center
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String star = "★";
                g2.drawString(star, (w - fm.stringWidth(star)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        shieldIcon.setPreferredSize(new Dimension(36, 36));

        JPanel titlesBox = new JPanel();
        titlesBox.setLayout(new BoxLayout(titlesBox, BoxLayout.Y_AXIS));
        titlesBox.setOpaque(false);

        JLabel appTitle = new JLabel("HỆ THỐNG QUẢN TRỊ ADMIN — MINESWEEPER BATTLE");
        appTitle.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 16));
        appTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel subTitle = new JLabel("Trung tâm giám sát thi đấu & Quản lý người chơi thời gian thực");
        subTitle.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 11));
        subTitle.setForeground(Theme.TEXT_MUTED);

        titlesBox.add(appTitle);
        titlesBox.add(Box.createVerticalStrut(2));
        titlesBox.add(subTitle);

        brandBox.add(shieldIcon);
        brandBox.add(titlesBox);

        // Right Actions (Refresh, Player Mode, Logout)
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        // Admin User Chip
        JPanel userChip = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        userChip.setOpaque(false);
        userChip.setBorder(new EmptyBorder(2, 10, 2, 12));
        JLabel adminTag = new JLabel("ADMIN: " + adminName);
        adminTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 12));
        adminTag.setForeground(Theme.ACCENT_CYAN);
        userChip.add(adminTag);

        // Refresh Button
        JButton refreshBtn = createStyledButton("Làm Mới", new Color(0x1E, 0x29, 0x3B), Theme.TEXT_PRIMARY);
        refreshBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestData();
            }
        });

        // Switch to Player Mode Button
        JButton playerModeBtn = createStyledButton("Vào Sảnh Chơi", Theme.PRIMARY, Color.WHITE);
        playerModeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (callback != null) {
                    callback.onSwitchToPlayerHome();
                }
            }
        });

        // Logout Button
        JButton logoutBtn = createStyledButton("Đăng Xuất", new Color(0x3B, 0x1A, 0x20), new Color(0xF8, 0x71, 0x71));
        logoutBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (callback != null) {
                    callback.onLogout();
                }
            }
        });

        rightActions.add(userChip);
        rightActions.add(refreshBtn);
        rightActions.add(playerModeBtn);
        rightActions.add(logoutBtn);

        bar.add(brandBox, BorderLayout.WEST);
        bar.add(rightActions, BorderLayout.EAST);
        return bar;
    }

    private JPanel createStatsCardsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 16, 0));
        p.setOpaque(false);

        activeMatchesValue = new JLabel("0");
        onlinePlayersValue = new JLabel("0");
        totalUsersValue = new JLabel("0");
        totalMatchesValue = new JLabel("0");

        p.add(createKpiCard("Trận Đang Đấu", activeMatchesValue, "Cặp thi đấu trực tiếp", Theme.ACCENT_CYAN));
        p.add(createKpiCard("Người Chơi Online", onlinePlayersValue, "Đang kết nối server", Theme.SUCCESS));
        p.add(createKpiCard("Tổng Người Dùng", totalUsersValue, "Tài khoản đã đăng ký", Theme.PRIMARY));
        p.add(createKpiCard("Tổng Trận Đấu", totalMatchesValue, "Lịch sử tất cả ván", Theme.WARNING));

        return p;
    }

    private JPanel createKpiCard(final String title, final JLabel valueLabel, final String desc, final Color accent) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(Theme.BORDER_CARD);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));

                // Left highlight accent line
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Float(1, 4, 4, getHeight() - 8, 4, 4));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 18, 12, 16));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 11));
        titleLbl.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 26));
        valueLabel.setForeground(accent);

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 10));
        descLbl.setForeground(new Color(0x64, 0x74, 0x8B));

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(descLbl);

        return card;
    }

    private JPanel createTabsHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel tabsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tabsRow.setOpaque(false);

        tabMatchesBtn = createTabButton("⚔ CÁC CẶP ĐẤU ĐANG THI ĐẤU", true);
        tabUsersBtn = createTabButton("👥 QUẢN LÝ NGƯỜI CHƠI", false);
        tabHistoryBtn = createTabButton("📜 LỊCH SỬ TẤT CẢ TRẬN ĐẤU", false);

        tabMatchesBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchTab("TAB_MATCHES", tabMatchesBtn);
            }
        });
        tabUsersBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchTab("TAB_USERS", tabUsersBtn);
            }
        });
        tabHistoryBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchTab("TAB_HISTORY", tabHistoryBtn);
            }
        });

        tabsRow.add(tabMatchesBtn);
        tabsRow.add(tabUsersBtn);
        tabsRow.add(tabHistoryBtn);

        matchCountHeader = new JLabel("0 cặp đang thi đấu");
        matchCountHeader.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 12));
        matchCountHeader.setForeground(Theme.ACCENT_CYAN);

        header.add(tabsRow, BorderLayout.WEST);
        header.add(matchCountHeader, BorderLayout.EAST);
        return header;
    }

    private void switchTab(String tabName, JButton activeBtn) {
        tabCardLayout.show(tabContentPanel, tabName);
        resetTabButton(tabMatchesBtn, false);
        resetTabButton(tabUsersBtn, false);
        resetTabButton(tabHistoryBtn, false);
        resetTabButton(activeBtn, true);
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(Theme.SUBTITLE_FONT.getFamily(), Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        resetTabButton(btn, active);
        return btn;
    }

    private void resetTabButton(final JButton btn, final boolean active) {
        btn.setOpaque(false);
        if (active) {
            btn.setForeground(Theme.TEXT_PRIMARY);
        } else {
            btn.setForeground(Theme.TEXT_MUTED);
        }
        for (MouseListener ml : btn.getMouseListeners()) {
            if (ml.getClass().getName().contains("TabHover")) {
                btn.removeMouseListener(ml);
            }
        }
        btn.addMouseListener(new MouseAdapter() {
            class TabHover {}
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!active) btn.setForeground(Theme.TEXT_PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!active) btn.setForeground(Theme.TEXT_MUTED);
            }
        });
    }

    // ==============================================================
    //  TAB 1: Danh sách các cặp đang thi đấu (Match List)
    // ==============================================================

    private JPanel createMatchesTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        matchesListPanel = new JPanel();
        matchesListPanel.setLayout(new BoxLayout(matchesListPanel, BoxLayout.Y_AXIS));
        matchesListPanel.setOpaque(false);

        renderMatchesList();

        JScrollPane scroll = new JScrollPane(matchesListPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private void renderMatchesList() {
        matchesListPanel.removeAll();

        if (activeMatches.isEmpty()) {
            // Elegant Empty State
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setOpaque(false);
            empty.setBorder(new EmptyBorder(60, 20, 60, 20));

            JLabel radarIcon = new JLabel("◎", SwingConstants.CENTER);
            radarIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 52));
            radarIcon.setForeground(new Color(0x38, 0xBD, 0xF8, 120));
            radarIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel title = new JLabel("HIỆN TẠI CHƯA CÓ CẶP ĐẤU NÀO ĐANG THI ĐẤU");
            title.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 15));
            title.setForeground(Theme.TEXT_SECONDARY);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel sub = new JLabel("Hệ thống đang quét liên tục. Khi các người chơi bắt đầu ghép trận hoặc thách đấu, các cặp đấu sẽ tự động xuất hiện tại đây!");
            sub.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 12));
            sub.setForeground(Theme.TEXT_MUTED);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);

            empty.add(radarIcon);
            empty.add(Box.createVerticalStrut(16));
            empty.add(title);
            empty.add(Box.createVerticalStrut(6));
            empty.add(sub);

            matchesListPanel.add(empty);
        } else {
            for (final MatchItem m : activeMatches) {
                JPanel card = createMatchCard(m);
                matchesListPanel.add(card);
                matchesListPanel.add(Box.createVerticalStrut(12));
            }
        }

        matchesListPanel.revalidate();
        matchesListPanel.repaint();
    }

    private JPanel createMatchCard(final MatchItem m) {
        JPanel card = new JPanel(new BorderLayout(14, 0)) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) {
                        if (callback != null) {
                            callback.onSpectate(m.matchId, m.p1, m.p2);
                        }
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? Theme.BG_CARD_HOVER : new Color(0x0E, 0x15, 0x27));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(hover ? Theme.ACCENT_CYAN : Theme.BORDER_CARD);
                g2.setStroke(new BasicStroke(hover ? 1.5f : 1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(12, 18, 12, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        // Left Player Info
        JPanel leftPlayer = new JPanel();
        leftPlayer.setLayout(new BoxLayout(leftPlayer, BoxLayout.Y_AXIS));
        leftPlayer.setOpaque(false);
        leftPlayer.setPreferredSize(new Dimension(280, 0));

        JLabel p1Name = new JLabel(m.p1);
        p1Name.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 15));
        p1Name.setForeground(Theme.ACCENT_CYAN);

        JLabel p1Stats = new JLabel("Tiến độ: " + m.p1Percent + "%  •  Cờ: " + m.p1Flags + "  •  " + m.p1Status);
        p1Stats.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 11));
        p1Stats.setForeground(Theme.TEXT_SECONDARY);

        leftPlayer.add(p1Name);
        leftPlayer.add(Box.createVerticalStrut(4));
        leftPlayer.add(p1Stats);
        leftPlayer.add(Box.createVerticalStrut(4));
        leftPlayer.add(createMiniProgressBar(m.p1Percent, Theme.PRIMARY, Theme.ACCENT_CYAN));

        // Center: VS Badge + Elapsed Time + LIVE indicator
        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);

        JLabel liveBadge = new JLabel("● TRỰC TIẾP");
        liveBadge.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        liveBadge.setForeground(new Color(0xEF, 0x44, 0x44));
        liveBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel vsLabel = new JLabel("VS");
        vsLabel.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 16));
        vsLabel.setForeground(Theme.TEXT_PRIMARY);
        vsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        int min = m.elapsedSec / 60;
        int sec = m.elapsedSec % 60;
        JLabel timeLabel = new JLabel(String.format("%02d:%02d", min, sec));
        timeLabel.setFont(new Font(Theme.STAT_FONT.getFamily(), Font.BOLD, 13));
        timeLabel.setForeground(Theme.ACCENT_CYAN);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerBox.add(liveBadge);
        centerBox.add(Box.createVerticalStrut(2));
        centerBox.add(vsLabel);
        centerBox.add(Box.createVerticalStrut(2));
        centerBox.add(timeLabel);

        // Right Player Info
        JPanel rightPlayer = new JPanel();
        rightPlayer.setLayout(new BoxLayout(rightPlayer, BoxLayout.Y_AXIS));
        rightPlayer.setOpaque(false);
        rightPlayer.setPreferredSize(new Dimension(280, 0));

        JLabel p2Name = new JLabel(m.p2);
        p2Name.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 15));
        p2Name.setForeground(new Color(0xFB, 0x92, 0x3C));
        p2Name.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel p2Stats = new JLabel("Tiến độ: " + m.p2Percent + "%  •  Cờ: " + m.p2Flags + "  •  " + m.p2Status);
        p2Stats.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 11));
        p2Stats.setForeground(Theme.TEXT_SECONDARY);
        p2Stats.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightPlayer.add(p2Name);
        rightPlayer.add(Box.createVerticalStrut(4));
        rightPlayer.add(p2Stats);
        rightPlayer.add(Box.createVerticalStrut(4));
        JPanel barWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        barWrap.setOpaque(false);
        barWrap.add(createMiniProgressBar(m.p2Percent, new Color(0xEA, 0x58, 0x0C), new Color(0xEF, 0x44, 0x44)));
        rightPlayer.add(barWrap);

        // Action Button: Spectate
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        actionBox.setOpaque(false);
        JButton spectateBtn = createStyledButton("👁 THEO DÕI TRẬN ĐẤU", Theme.PRIMARY, Color.WHITE);
        spectateBtn.setFont(new Font(Theme.SUBTITLE_FONT.getFamily(), Font.BOLD, 12));
        spectateBtn.setPreferredSize(new Dimension(170, 36));
        spectateBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (callback != null) {
                    callback.onSpectate(m.matchId, m.p1, m.p2);
                }
            }
        });
        actionBox.add(spectateBtn);

        card.add(leftPlayer, BorderLayout.WEST);
        card.add(centerBox, BorderLayout.CENTER);

        JPanel rightWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        rightWrapper.setOpaque(false);
        rightWrapper.add(rightPlayer);
        rightWrapper.add(actionBox);

        card.add(rightWrapper, BorderLayout.EAST);
        return card;
    }

    private JComponent createMiniProgressBar(final int percent, final Color c1, final Color c2) {
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // BG track
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 4, 4));
                // Fill
                int fillW = (int) (getWidth() * (Math.max(0, Math.min(100, percent)) / 100.0));
                if (fillW > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, c1, fillW, 0, c2);
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Float(0, 0, fillW, getHeight(), 4, 4));
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(180, 6));
        bar.setMaximumSize(new Dimension(180, 6));
        return bar;
    }

    // ==============================================================
    //  TAB 2: Quản lý người chơi
    // ==============================================================

    private JPanel createUsersTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);

        // --- Toolbar on top: Search & Actions ---
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);

        // Search box
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setForeground(Theme.TEXT_MUTED);
        final JTextField searchField = new JTextField(15);
        searchField.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 12));
        searchField.setBackground(new Color(0x1F, 0x2A, 0x40));
        searchField.setForeground(Theme.TEXT_PRIMARY);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x33, 0x41, 0x55), 1),
                new EmptyBorder(6, 8, 6, 8)
        ));
        searchField.setToolTipText("Nhập tên tài khoản cần tìm...");
        searchBox.add(searchIcon);
        searchBox.add(searchField);

        // Action buttons
        final JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionGroup.setOpaque(false);

        final JButton resetPassBtn = createStyledButton("🔑 Đổi Mật Khẩu", new Color(0x25, 0x63, 0xEB), Color.WHITE);
        final JButton resetScoreBtn = createStyledButton("🔄 Reset Điểm", new Color(0xD9, 0x77, 0x06), Color.WHITE);
        final JButton setRoleBtn = createStyledButton("👑 Đổi Quyền", new Color(0x7C, 0x3A, 0xED), Color.WHITE);
        final JButton deleteUserBtn = createStyledButton("🗑 Xóa Tài Khoản", new Color(0xDC, 0x26, 0x26), Color.WHITE);

        resetPassBtn.setEnabled(false);
        resetScoreBtn.setEnabled(false);
        setRoleBtn.setEnabled(false);
        deleteUserBtn.setEnabled(false);

        actionGroup.add(resetPassBtn);
        actionGroup.add(resetScoreBtn);
        actionGroup.add(setRoleBtn);
        actionGroup.add(deleteUserBtn);

        toolbar.add(searchBox, BorderLayout.WEST);
        toolbar.add(actionGroup, BorderLayout.EAST);
        p.add(toolbar, BorderLayout.NORTH);

        // --- Users Table ---
        String[] cols = {"Tên Người Dùng", "Vai Trò", "Điểm Số", "Thắng", "Thua", "Hòa", "Tỉ Lệ Thắng", "Ngày Tạo"};
        usersTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        usersTable = new JTable(usersTableModel);
        styleTable(usersTable);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Enable buttons on row selection
        usersTable.getSelectionModel().addListSelectionListener(e -> {
            int row = usersTable.getSelectedRow();
            boolean hasSel = (row >= 0);
            resetPassBtn.setEnabled(hasSel);
            resetScoreBtn.setEnabled(hasSel);
            setRoleBtn.setEnabled(hasSel);
            deleteUserBtn.setEnabled(hasSel);
        });

        // Search filter listener
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String txt = searchField.getText().trim().toLowerCase();
                javax.swing.table.TableRowSorter<DefaultTableModel> sorter = new javax.swing.table.TableRowSorter<>(usersTableModel);
                usersTable.setRowSorter(sorter);
                if (txt.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(txt), 0));
                }
            }
        });

        // Action: Reset Password
        resetPassBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) return;
            String username = (String) usersTable.getValueAt(row, 0);
            String newPass = JOptionPane.showInputDialog(this,
                    "Nhập mật khẩu mới cho người chơi [" + username + "]:",
                    "Đổi Mật Khẩu Người Chơi", JOptionPane.QUESTION_MESSAGE);
            if (newPass != null && !newPass.trim().isEmpty()) {
                if (callback != null) {
                    callback.send("ADMIN_RESET_PASSWORD|" + username + "|" + newPass.trim());
                }
            }
        });

        // Action: Reset Score
        resetScoreBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) return;
            String username = (String) usersTable.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn đặt lại điểm số và số trận thắng/thua của [" + username + "] về 0?",
                    "Xác Nhận Reset Điểm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION && callback != null) {
                callback.send("ADMIN_RESET_SCORE|" + username);
            }
        });

        // Action: Change Role
        setRoleBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) return;
            String username = (String) usersTable.getValueAt(row, 0);
            String currentRole = (String) usersTable.getValueAt(row, 1);
            String[] roles = {"USER", "ADMIN"};
            String newRole = (String) JOptionPane.showInputDialog(this,
                    "Chọn vai trò mới cho [" + username + "]:",
                    "Phân Quyền Người Dùng", JOptionPane.QUESTION_MESSAGE, null, roles,
                    "ADMIN".equalsIgnoreCase(currentRole) ? "ADMIN" : "USER");
            if (newRole != null && callback != null) {
                callback.send("ADMIN_SET_ROLE|" + username + "|" + newRole);
            }
        });

        // Action: Delete User
        deleteUserBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) return;
            String username = (String) usersTable.getValueAt(row, 0);
            if ("admin".equalsIgnoreCase(username)) {
                JOptionPane.showMessageDialog(this, "Không thể xóa tài khoản Quản trị viên gốc!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "CẢNH BÁO: Bạn có chắc chắn muốn XÓA vĩnh viễn tài khoản [" + username + "]?",
                    "Xác Nhận Xóa Tài Khoản", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION && callback != null) {
                callback.send("ADMIN_DELETE_USER|" + username);
            }
        });

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ==============================================================
    //  TAB 3: Lịch sử thi đấu
    // ==============================================================

    private JPanel createHistoryTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        String[] cols = {"Mã Trận", "Người Chơi 1", "Người Chơi 2", "Kết Quả", "Lý Do", "Thời Lượng", "Thời Gian Đấu"};
        historyTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        styleTable(historyTable);

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private void styleTable(JTable table) {
        table.setOpaque(false);
        table.setBackground(new Color(0x0E, 0x15, 0x28));
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 12));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0x1F, 0x2A, 0x48));
        table.setSelectionForeground(Theme.ACCENT_CYAN);

        JTableHeader th = table.getTableHeader();
        th.setOpaque(false);
        th.setBackground(new Color(0x15, 0x1E, 0x34));
        th.setForeground(Theme.TEXT_MUTED);
        th.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 11));
        th.setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        center.setOpaque(false);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }
    }

    // ==============================================================
    //  Public Updaters called from Client.java on network message
    // ==============================================================

    public void setStats(int onlineCount, int matchesCount, int totalUsers, int totalMatches) {
        if (activeMatchesValue != null) activeMatchesValue.setText(String.valueOf(matchesCount));
        if (onlinePlayersValue != null) onlinePlayersValue.setText(String.valueOf(onlineCount));
        if (totalUsersValue != null) totalUsersValue.setText(String.valueOf(totalUsers));
        if (totalMatchesValue != null) totalMatchesValue.setText(String.valueOf(totalMatches));
    }

    public void setMatches(String raw) {
        activeMatches.clear();
        if (raw != null && !raw.trim().isEmpty()) {
            for (String item : raw.split(";")) {
                String[] parts = item.split("~");
                if (parts.length >= 10) {
                    try {
                        activeMatches.add(new MatchItem(
                                parts[0], parts[1], parts[2],
                                Integer.parseInt(parts[3]),
                                Integer.parseInt(parts[4]),
                                Integer.parseInt(parts[5]),
                                Integer.parseInt(parts[6]),
                                Integer.parseInt(parts[7]),
                                parts[8], parts[9]
                        ));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (matchCountHeader != null) {
            matchCountHeader.setText(activeMatches.size() + " cặp đang thi đấu");
        }
        renderMatchesList();
    }

    public void setUsers(String raw) {
        if (usersTableModel == null) return;
        usersTableModel.setRowCount(0);
        if (raw == null || raw.trim().isEmpty()) return;

        for (String item : raw.split(";")) {
            String[] parts = item.split("~");
            if (parts.length >= 7) {
                try {
                    String user = parts[0];
                    String role = parts[1];
                    int score = Integer.parseInt(parts[2]);
                    int wins = Integer.parseInt(parts[3]);
                    int losses = Integer.parseInt(parts[4]);
                    int draws = Integer.parseInt(parts[5]);
                    String date = parts[6];

                    int total = wins + losses + draws;
                    String winRate = total > 0 ? String.format("%.1f%%", (wins * 100.0 / total)) : "0.0%";

                    usersTableModel.addRow(new Object[]{
                            user, role, score, wins, losses, draws, winRate, date
                    });
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public void setHistory(String raw) {
        if (historyTableModel == null) return;
        historyTableModel.setRowCount(0);
        if (raw == null || raw.trim().isEmpty()) return;

        for (String item : raw.split(";")) {
            String[] parts = item.split("~");
            if (parts.length >= 7) {
                try {
                    String id = "#" + parts[0];
                    String p1 = parts[1];
                    String p2 = parts[2];
                    String res = parts[3];
                    String reason = parts[4];
                    int dur = Integer.parseInt(parts[5]);
                    String time = parts[6];

                    String resultText;
                    if ("P1_WIN".equals(res)) resultText = p1 + " thắng";
                    else if ("P2_WIN".equals(res)) resultText = p2 + " thắng";
                    else resultText = "Hòa";

                    String durationText = String.format("%02d:%02d", dur / 60, dur % 60);

                    historyTableModel.addRow(new Object[]{
                            id, p1, p2, resultText, reason, durationText, time
                    });
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // Helper button
    private JButton createStyledButton(final String text, final Color bg, final Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }
}
