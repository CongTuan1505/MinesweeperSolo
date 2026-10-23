package domin.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Trang chủ game Dò Mìn Đối Kháng (Minesweeper Battle Online).
 * Đồng bộ phong cách thiết kế và bảng màu thanh lịch với LoginScreen.
 */
public class HomeScreen extends JPanel {

    // ================================================================
    //  Callback — giao tiếp với Client (gửi lệnh qua Wire)
    // ================================================================

    public interface HomeCallback {
        void send(String message);
    }

    // ================================================================
    //  Hằng số
    // ================================================================

    private static final int SIDEBAR_WIDTH = 290;

    // ================================================================
    //  Dữ liệu
    // ================================================================

    private final String username;
    private final HomeCallback callback;
    private int score, wins, losses, draws;
    private final List<String[]> onlinePlayers = new ArrayList<>();
    private final List<String[]> challengeList = new ArrayList<>();
    private BufferedImage logoImage;
    private BufferedImage customAvatar;
    private JPanel avatarPanel;

    // ================================================================
    //  Tham chiếu UI cần cập nhật
    // ================================================================

    private JLabel navScoreLabel;
    private JLabel profileScoreLabel;
    private JLabel statsTotalLabel, statsWinsLabel, statsLossesLabel, statsDrawsLabel;
    private JLabel winRateLabel;
    private JPanel onlineListPanel;
    private JLabel onlineCountLabel;
    private JPanel leaderboardPanel;
    private JPanel historyPanel;
    private JPanel challengePanel;
    private JButton findMatchBtn, cancelMatchBtn;
    private JLabel gameStatusLabel;
    private JLabel statusBarLabel;

    // ================================================================
    //  Constructor
    // ================================================================

    public HomeScreen(String username, HomeCallback callback) {
        this.username = username;
        this.callback = callback;
        loadLogo();

        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);

        add(buildNavbar(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 18, 16, 18));
        content.add(buildLeftSidebar(), BorderLayout.WEST);
        content.add(buildMainContent(), BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(Theme.BG_DARK);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll.getVerticalScrollBar());
        add(scroll, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ================================================================
    //  LOAD LOGO
    // ================================================================

    private void loadLogo() {
        try (InputStream is = getClass().getResourceAsStream("/images/logotest.png")) {
            if (is != null) {
                logoImage = ImageIO.read(is);
            } else {
                System.err.println("Không tìm thấy /images/logotest.png");
            }
        } catch (Exception e) {
            System.err.println("Lỗi nạp logo: " + e.getMessage());
        }
    }

    private Image scaleImage(BufferedImage img, int w, int h) {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();
        return scaled;
    }

    // ================================================================
    //  NAVBAR
    // ================================================================

    private JPanel buildNavbar() {
        JPanel nav = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_NAVBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER_CARD);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        nav.setPreferredSize(new Dimension(0, 56));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(0, 18, 0, 18));

        // Bên trái: Hình logotest thay vì text
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        left.setOpaque(false);
        if (logoImage != null) {
            int targetH = 40;
            int targetW = (int) Math.round(logoImage.getWidth() * ((double) targetH / logoImage.getHeight()));
            JLabel logoLbl = new JLabel(new ImageIcon(scaleImage(logoImage, targetW, targetH)));
            left.add(logoLbl);
        } else {
            JLabel titleLbl = new JLabel("MINESWEEPER BATTLE");
            titleLbl.setFont(Theme.TITLE_FONT);
            titleLbl.setForeground(Theme.TEXT_PRIMARY);
            left.add(titleLbl);
        }

        // Bên phải: Điểm + Tên người dùng + Nút Đăng xuất
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        right.setOpaque(false);

        navScoreLabel = new JLabel("0 điểm");
        navScoreLabel.setFont(Theme.SUBTITLE_FONT);
        navScoreLabel.setForeground(Theme.ACCENT_GOLD);

        JLabel sep = new JLabel("|");
        sep.setForeground(Theme.TEXT_MUTED);

        JLabel userLbl = new JLabel(username);
        userLbl.setFont(Theme.SUBTITLE_FONT);
        userLbl.setForeground(Theme.TEXT_PRIMARY);

        JButton logoutBtn = createStyledButton("Đăng xuất", Theme.BTN_DANGER_BG);
        logoutBtn.setPreferredSize(new Dimension(95, 32));
        logoutBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int ch = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (ch == JOptionPane.YES_OPTION) {
                    callback.send("LOGOUT");
                    System.exit(0);
                }
            }
        });

        right.add(navScoreLabel);
        right.add(sep);
        right.add(userLbl);
        right.add(logoutBtn);

        nav.add(left, BorderLayout.WEST);
        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    // ================================================================
    //  LEFT SIDEBAR
    // ================================================================

    private JPanel buildLeftSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));

        JPanel profile = buildProfileCard();
        profile.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(profile);
        sidebar.add(Box.createVerticalStrut(16));

        JPanel challenge = buildChallengeCard();
        challenge.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(challenge);
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    // ================================================================
    //  PROFILE CARD
    // ================================================================

    private JPanel buildProfileCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.setMaximumSize(new Dimension(SIDEBAR_WIDTH, 420));

        // Tiêu đề
        JLabel title = createCardTitle("THÔNG TIN CÁ NHÂN");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        // Avatar tròn phong cách LoginScreen
        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 72;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                if (customAvatar != null) {
                    Shape oldClip = g2.getClip();
                    g2.setClip(new Ellipse2D.Double(x, y, size, size));
                    g2.drawImage(customAvatar, x, y, size, size, null);
                    g2.setClip(oldClip);
                } else {
                    // Nền gradient xám than thanh lịch
                    GradientPaint gp = new GradientPaint(x, y, new Color(55, 60, 72),
                            x + size, y + size, new Color(38, 42, 52));
                    g2.setPaint(gp);
                    g2.fillOval(x, y, size, size);

                    // Chữ cái đầu
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
                    String initial = (username != null && !username.isEmpty())
                            ? username.substring(0, 1).toUpperCase() : "U";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(initial, x + (size - fm.stringWidth(initial)) / 2,
                            y + (size - fm.getHeight()) / 2 + fm.getAscent());
                }

                // Viền mỏng
                g2.setColor(new Color(210, 214, 222));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x, y, size, size);
                g2.dispose();
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(80, 80));
        avatarPanel.setMaximumSize(new Dimension(80, 80));
        avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarPanel.setToolTipText("Click để đổi ảnh đại diện");
        avatarPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chooseAndUploadAvatar();
            }
        });
        card.add(avatarPanel);

        // Nút đổi ảnh nhỏ bên dưới avatar
        JButton changeAvatarBtn = createStyledButton("Đổi ảnh", new Color(110, 118, 135));
        changeAvatarBtn.setPreferredSize(new Dimension(84, 22));
        changeAvatarBtn.setFont(Theme.SMALL_FONT);
        changeAvatarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        changeAvatarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseAndUploadAvatar();
            }
        });
        card.add(changeAvatarBtn);
        card.add(Box.createVerticalStrut(10));

        // Tên tài khoản
        JLabel nameLbl = new JLabel(username);
        nameLbl.setFont(new Font(Theme.SUBTITLE_FONT.getFamily(), Font.BOLD, 17));
        nameLbl.setForeground(Theme.TEXT_PRIMARY);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(4));

        // Điểm số
        profileScoreLabel = new JLabel("0 điểm");
        profileScoreLabel.setFont(Theme.STAT_FONT);
        profileScoreLabel.setForeground(Theme.ACCENT_GOLD);
        profileScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(profileScoreLabel);
        card.add(Box.createVerticalStrut(14));

        // Đường phân cách
        card.add(createDivider());
        card.add(Box.createVerticalStrut(12));

        // Bảng thống kê
        JPanel statsBox = new JPanel(new GridLayout(0, 1, 0, 6));
        statsBox.setOpaque(false);
        statsBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsBox.setMaximumSize(new Dimension(SIDEBAR_WIDTH - 36, 110));

        statsTotalLabel = addStatRow(statsBox, "Tổng trận đấu", Theme.TEXT_PRIMARY);
        statsWinsLabel = addStatRow(statsBox, "Thắng", Theme.WIN_GREEN);
        statsLossesLabel = addStatRow(statsBox, "Thua", Theme.LOSE_RED);
        statsDrawsLabel = addStatRow(statsBox, "Hòa", Theme.DRAW_GOLD);
        card.add(statsBox);
        card.add(Box.createVerticalStrut(10));

        // Tỷ lệ thắng
        winRateLabel = new JLabel("Tỷ lệ thắng: 0%");
        winRateLabel.setFont(Theme.BODY_FONT);
        winRateLabel.setForeground(Theme.TEXT_SECONDARY);
        winRateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(winRateLabel);

        return card;
    }

    private JLabel addStatRow(JPanel parent, String label, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel left = new JLabel(label);
        left.setFont(Theme.BODY_FONT);
        left.setForeground(Theme.TEXT_SECONDARY);
        JLabel right = new JLabel("0");
        right.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
        right.setForeground(valueColor);
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        parent.add(row);
        return right;
    }

    // ================================================================
    //  CHALLENGE CARD
    // ================================================================

    private JPanel buildChallengeCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setMaximumSize(new Dimension(SIDEBAR_WIDTH, 260));

        JLabel title = createCardTitle("LỜI MỜI THÁCH ĐẤU");
        card.add(title, BorderLayout.NORTH);

        challengePanel = new JPanel();
        challengePanel.setLayout(new BoxLayout(challengePanel, BoxLayout.Y_AXIS));
        challengePanel.setOpaque(false);
        challengePanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        challengePanel.add(createMutedLabel("Chưa có lời mời nào"));

        JScrollPane sp = new JScrollPane(challengePanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.setPreferredSize(new Dimension(0, 170));
        styleScrollBar(sp.getVerticalScrollBar());

        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel createChallengeRow(final String fromUser, int challengerScore) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_CARD),
                new EmptyBorder(8, 0, 8, 0)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        JLabel nameLbl = new JLabel(fromUser);
        nameLbl.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD));
        nameLbl.setForeground(Theme.TEXT_PRIMARY);
        JLabel scoreLbl = new JLabel(challengerScore + " điểm");
        scoreLbl.setFont(Theme.SMALL_FONT);
        scoreLbl.setForeground(Theme.ACCENT_GOLD);
        info.add(nameLbl);
        info.add(scoreLbl);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        btns.setOpaque(false);
        JButton acceptBtn = createStyledButton("Nhận", Theme.BTN_SUCCESS_BG);
        acceptBtn.setPreferredSize(new Dimension(60, 28));
        acceptBtn.setFont(Theme.SMALL_FONT);
        acceptBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.send("CHALLENGE_ACCEPT|" + fromUser);
            }
        });

        JButton rejectBtn = createStyledButton("Từ chối", Theme.BTN_DANGER_BG);
        rejectBtn.setPreferredSize(new Dimension(65, 28));
        rejectBtn.setFont(Theme.SMALL_FONT);
        rejectBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.send("CHALLENGE_REJECT|" + fromUser);
                removeChallenge(fromUser);
            }
        });

        btns.add(acceptBtn);
        btns.add(rejectBtn);

        row.add(info, BorderLayout.CENTER);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    // ================================================================
    //  MAIN CONTENT
    // ================================================================

    private JPanel buildMainContent() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);

        JPanel gameArea = buildGameAreaCard();
        gameArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        gameArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        main.add(gameArea);
        main.add(Box.createVerticalStrut(16));

        JPanel dual = new JPanel(new GridLayout(1, 2, 16, 0));
        dual.setOpaque(false);
        dual.setAlignmentX(Component.LEFT_ALIGNMENT);
        dual.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        dual.add(buildOnlinePlayersCard());
        dual.add(buildLeaderboardCard());
        main.add(dual);
        main.add(Box.createVerticalStrut(16));

        JPanel history = buildHistoryCard();
        history.setAlignmentX(Component.LEFT_ALIGNMENT);
        history.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        main.add(history);

        return main;
    }

    // ================================================================
    //  GAME AREA CARD
    // ================================================================

    private JPanel buildGameAreaCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Tiêu đề
        JLabel title = createCardTitle("SẴN SÀNG THÁCH ĐẤU?");
        title.setFont(Theme.TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        // Nút bấm
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        btnPanel.setOpaque(false);

        findMatchBtn = createStyledButton("Tìm đối thủ ngẫu nhiên", Theme.BTN_PRIMARY_BG);
        findMatchBtn.setPreferredSize(new Dimension(210, 38));
        findMatchBtn.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD));
        findMatchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.send("FIND_MATCH");
                findMatchBtn.setEnabled(false);
                cancelMatchBtn.setEnabled(true);
                gameStatusLabel.setText("Đang tìm đối thủ...");
            }
        });

        cancelMatchBtn = createStyledButton("Hủy tìm", Theme.BTN_DANGER_BG);
        cancelMatchBtn.setPreferredSize(new Dimension(95, 38));
        cancelMatchBtn.setEnabled(false);
        cancelMatchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.send("CANCEL_MATCH");
            }
        });

        JButton rulesBtn = createStyledButton("Luật chơi", new Color(105, 115, 130));
        rulesBtn.setPreferredSize(new Dimension(95, 38));
        rulesBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { showRules(); }
        });

        btnPanel.add(findMatchBtn);
        btnPanel.add(cancelMatchBtn);
        btnPanel.add(rulesBtn);
        card.add(btnPanel, BorderLayout.CENTER);

        // Trạng thái
        gameStatusLabel = new JLabel(" ");
        gameStatusLabel.setFont(Theme.BODY_FONT);
        gameStatusLabel.setForeground(Theme.WIN_GREEN);
        gameStatusLabel.setBorder(new EmptyBorder(0, 2, 0, 0));
        card.add(gameStatusLabel, BorderLayout.SOUTH);

        return card;
    }

    // ================================================================
    //  ONLINE PLAYERS CARD
    // ================================================================

    private JPanel buildOnlinePlayersCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = createCardTitle("NGƯỜI CHƠI ONLINE");
        onlineCountLabel = new JLabel("0 người");
        onlineCountLabel.setFont(Theme.SMALL_FONT);
        onlineCountLabel.setForeground(Theme.TEXT_MUTED);
        header.add(title, BorderLayout.WEST);
        header.add(onlineCountLabel, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        onlineListPanel = new JPanel();
        onlineListPanel.setLayout(new BoxLayout(onlineListPanel, BoxLayout.Y_AXIS));
        onlineListPanel.setOpaque(false);
        onlineListPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        onlineListPanel.add(createMutedLabel("Đang tải..."));

        JScrollPane sp = new JScrollPane(onlineListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        styleScrollBar(sp.getVerticalScrollBar());
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JPanel createPlayerRow(final String name, int playerScore, String status) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_CARD),
                new EmptyBorder(7, 4, 7, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // Trái: chấm tròn màu + tên + điểm
        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        info.setOpaque(false);

        final Color dotColor = "ONLINE".equals(status) ? Theme.STATUS_ONLINE : Theme.STATUS_IN_MATCH;
        JPanel dotPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dotColor);
                g2.fillOval(1, (getHeight() - 8) / 2, 8, 8);
                g2.dispose();
            }
        };
        dotPanel.setPreferredSize(new Dimension(10, 20));
        dotPanel.setOpaque(false);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(Theme.BODY_FONT);
        nameLbl.setForeground(Theme.TEXT_PRIMARY);

        JLabel scoreLbl = new JLabel(playerScore + " điểm");
        scoreLbl.setFont(Theme.SMALL_FONT);
        scoreLbl.setForeground(Theme.ACCENT_GOLD);

        info.add(dotPanel);
        info.add(nameLbl);
        info.add(scoreLbl);
        row.add(info, BorderLayout.CENTER);

        // Phải: Nút Thách đấu hoặc nhãn Trong trận
        if (name.equals(username)) {
            JLabel meLbl = new JLabel("Bạn");
            meLbl.setFont(Theme.SMALL_FONT);
            meLbl.setForeground(Theme.TEXT_MUTED);
            row.add(meLbl, BorderLayout.EAST);
        } else if ("ONLINE".equals(status)) {
            JButton btn = createStyledButton("Thách đấu", Theme.BTN_PRIMARY_BG);
            btn.setPreferredSize(new Dimension(86, 26));
            btn.setFont(Theme.SMALL_FONT);
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    callback.send("CHALLENGE|" + name);
                }
            });
            row.add(btn, BorderLayout.EAST);
        } else {
            JLabel stLbl = new JLabel("Trong trận");
            stLbl.setFont(Theme.SMALL_FONT);
            stLbl.setForeground(Theme.STATUS_IN_MATCH);
            row.add(stLbl, BorderLayout.EAST);
        }

        return row;
    }

    // ================================================================
    //  LEADERBOARD CARD
    // ================================================================

    private JPanel buildLeaderboardCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = createCardTitle("BẢNG XẾP HẠNG");
        card.add(title, BorderLayout.NORTH);

        leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BoxLayout(leaderboardPanel, BoxLayout.Y_AXIS));
        leaderboardPanel.setOpaque(false);
        leaderboardPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        leaderboardPanel.add(createMutedLabel("Đang tải..."));

        JScrollPane sp = new JScrollPane(leaderboardPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        styleScrollBar(sp.getVerticalScrollBar());
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JPanel createLeaderboardHeader() {
        JPanel row = new JPanel(new GridLayout(1, 6, 4, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        String[] cols = {"#", "Tên", "Điểm", "Thắng", "Thua", "Hòa"};
        for (String col : cols) {
            JLabel lbl = new JLabel(col, SwingConstants.CENTER);
            lbl.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD));
            lbl.setForeground(Theme.TEXT_MUTED);
            row.add(lbl);
        }
        return row;
    }

    private JPanel createLeaderboardRow(int rank, String name, int lbScore,
                                        int lbWins, int lbLosses, int lbDraws) {
        JPanel row = new JPanel(new GridLayout(1, 6, 4, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        // Top 3 có màu biểu tượng thứ hạng
        Color rankColor = rank == 1 ? Theme.ACCENT_GOLD
                : rank == 2 ? new Color(110, 115, 125)
                : rank == 3 ? new Color(175, 100, 40) : Theme.TEXT_SECONDARY;

        JLabel rankLbl = new JLabel(String.valueOf(rank), SwingConstants.CENTER);
        rankLbl.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD));
        rankLbl.setForeground(rankColor);

        boolean isMe = name.equals(username);
        Color nameColor = isMe ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;

        JLabel nameLbl = new JLabel(name, SwingConstants.CENTER);
        nameLbl.setFont(isMe ? Theme.BODY_FONT.deriveFont(Font.BOLD) : Theme.BODY_FONT);
        nameLbl.setForeground(nameColor);

        JLabel scoreLbl = new JLabel(String.valueOf(lbScore), SwingConstants.CENTER);
        scoreLbl.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD));
        scoreLbl.setForeground(Theme.ACCENT_GOLD);

        JLabel wLbl = new JLabel(String.valueOf(lbWins), SwingConstants.CENTER);
        wLbl.setFont(Theme.BODY_FONT);
        wLbl.setForeground(Theme.WIN_GREEN);

        JLabel lLbl = new JLabel(String.valueOf(lbLosses), SwingConstants.CENTER);
        lLbl.setFont(Theme.BODY_FONT);
        lLbl.setForeground(Theme.LOSE_RED);

        JLabel dLbl = new JLabel(String.valueOf(lbDraws), SwingConstants.CENTER);
        dLbl.setFont(Theme.BODY_FONT);
        dLbl.setForeground(Theme.DRAW_GOLD);

        row.add(rankLbl);
        row.add(nameLbl);
        row.add(scoreLbl);
        row.add(wLbl);
        row.add(lLbl);
        row.add(dLbl);
        return row;
    }

    // ================================================================
    //  HISTORY CARD
    // ================================================================

    private JPanel buildHistoryCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = createCardTitle("LỊCH SỬ TRẬN ĐẤU GẦN ĐÂY");
        card.add(title, BorderLayout.NORTH);

        historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setOpaque(false);
        historyPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        historyPanel.add(createMutedLabel("Đang tải..."));

        JScrollPane sp = new JScrollPane(historyPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        styleScrollBar(sp.getVerticalScrollBar());
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JPanel createHistoryRow(String opponent, String result, int points,
                                    int duration, String date) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_CARD),
                new EmptyBorder(7, 4, 7, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel vsLbl = new JLabel("vs " + opponent);
        vsLbl.setFont(Theme.BODY_FONT);
        vsLbl.setForeground(Theme.TEXT_PRIMARY);

        String resultText;
        Color resultColor;
        if ("WIN".equals(result)) {
            resultText = "Thắng";
            resultColor = Theme.WIN_GREEN;
        } else if ("LOSE".equals(result)) {
            resultText = "Thua";
            resultColor = Theme.LOSE_RED;
        } else {
            resultText = "Hòa";
            resultColor = Theme.DRAW_GOLD;
        }
        JLabel resLbl = new JLabel(resultText);
        resLbl.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD));
        resLbl.setForeground(resultColor);

        JLabel detailLbl = new JLabel("+" + points + " điểm  |  " + formatDuration(duration)
                + "  |  " + date);
        detailLbl.setFont(Theme.SMALL_FONT);
        detailLbl.setForeground(Theme.TEXT_MUTED);
        detailLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPart.setOpaque(false);
        leftPart.add(vsLbl);
        leftPart.add(resLbl);

        row.add(leftPart, BorderLayout.CENTER);
        row.add(detailLbl, BorderLayout.EAST);
        return row;
    }

    // ================================================================
    //  STATUS BAR
    // ================================================================

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG_NAVBAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER_CARD);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        bar.setPreferredSize(new Dimension(0, 30));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(5, 18, 5, 18));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.STATUS_ONLINE);
                g2.fillOval(0, (getHeight() - 7) / 2, 7, 7);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(8, 18));
        dot.setOpaque(false);

        JLabel connLbl = new JLabel("Đã kết nối");
        connLbl.setFont(Theme.SMALL_FONT);
        connLbl.setForeground(Theme.TEXT_SECONDARY);
        left.add(dot);
        left.add(connLbl);

        statusBarLabel = new JLabel("Trang chủ");
        statusBarLabel.setFont(Theme.SMALL_FONT);
        statusBarLabel.setForeground(Theme.TEXT_MUTED);
        statusBarLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        bar.add(left, BorderLayout.WEST);
        bar.add(statusBarLabel, BorderLayout.EAST);
        return bar;
    }

    // ================================================================
    //  UI HELPERS
    // ================================================================

    private JPanel createCardPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Theme.BORDER_CARD);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private JButton createStyledButton(String text, final Color bgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                if (!isEnabled()) {
                    bg = new Color(220, 224, 230);
                } else if (getModel().isPressed()) {
                    bg = bgColor.darker();
                } else if (getModel().isRollover()) {
                    bg = bgColor.brighter();
                } else {
                    bg = bgColor;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(Theme.BODY_FONT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createCardTitle(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(Theme.SUBTITLE_FONT);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        return lbl;
    }

    private JPanel createDivider() {
        JPanel div = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.BORDER_CARD);
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        div.setOpaque(false);
        div.setPreferredSize(new Dimension(0, 2));
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        div.setAlignmentX(Component.LEFT_ALIGNMENT);
        return div;
    }

    private JLabel createMutedLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.BODY_FONT);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(14, 4, 14, 4));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void styleScrollBar(JScrollBar sb) {
        sb.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(205, 210, 220);
                this.trackColor = new Color(245, 246, 250);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return zeroBtn(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    private String formatDuration(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    // ================================================================
    //  PUBLIC UPDATE METHODS
    // ================================================================

    public void setProfile(int score, int wins, int losses, int draws) {
        this.score = score;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        int total = wins + losses + draws;
        int winRate = total > 0 ? (wins * 100 / total) : 0;

        profileScoreLabel.setText(score + " điểm");
        navScoreLabel.setText(score + " điểm");
        statsTotalLabel.setText(String.valueOf(total));
        statsWinsLabel.setText(String.valueOf(wins));
        statsLossesLabel.setText(String.valueOf(losses));
        statsDrawsLabel.setText(String.valueOf(draws));
        winRateLabel.setText("Tỷ lệ thắng: " + winRate + "%");
    }

    public void setOnlinePlayers(String data) {
        onlinePlayers.clear();
        if (data != null && !data.isEmpty()) {
            for (String entry : data.split(";")) {
                String[] parts = entry.split("~");
                if (parts.length >= 3) {
                    if (parts[0].equals(username)) continue;
                    onlinePlayers.add(parts);
                }
            }
        }
        refreshOnlineList();
        updateOnlineCount();
    }

    public void addOnlinePlayer(String name, int playerScore) {
        if (name.equals(username)) return;
        for (String[] p : onlinePlayers) {
            if (p[0].equals(name)) return;
        }
        onlinePlayers.add(new String[]{name, String.valueOf(playerScore), "ONLINE"});
        refreshOnlineList();
        updateOnlineCount();
    }

    public void removeOnlinePlayer(String name) {
        Iterator<String[]> it = onlinePlayers.iterator();
        while (it.hasNext()) {
            if (it.next()[0].equals(name)) {
                it.remove();
                break;
            }
        }
        refreshOnlineList();
        updateOnlineCount();
        removeChallenge(name);
    }

    public void updatePlayerStatus(String name, String status) {
        for (String[] p : onlinePlayers) {
            if (p[0].equals(name)) {
                p[2] = status;
                break;
            }
        }
        refreshOnlineList();
    }

    public void setLeaderboard(String data) {
        leaderboardPanel.removeAll();
        if (data == null || data.isEmpty()) {
            leaderboardPanel.add(createMutedLabel("Chưa có dữ liệu"));
        } else {
            leaderboardPanel.add(createLeaderboardHeader());
            leaderboardPanel.add(createDivider());
            int rank = 1;
            for (String entry : data.split(";")) {
                String[] parts = entry.split("~");
                if (parts.length >= 5) {
                    try {
                        leaderboardPanel.add(createLeaderboardRow(rank++, parts[0],
                                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        leaderboardPanel.revalidate();
        leaderboardPanel.repaint();
    }

    public void setHistory(String data) {
        historyPanel.removeAll();
        if (data == null || data.isEmpty()) {
            historyPanel.add(createMutedLabel("Chưa có lịch sử trận đấu"));
        } else {
            for (String entry : data.split(";")) {
                String[] parts = entry.split("~");
                if (parts.length >= 5) {
                    try {
                        historyPanel.add(createHistoryRow(parts[0], parts[1],
                                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4]));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    public void addChallenge(String fromUser, int challengerScore) {
        for (String[] c : challengeList) {
            if (c[0].equals(fromUser)) return;
        }
        challengeList.add(new String[]{fromUser, String.valueOf(challengerScore)});
        refreshChallengePanel();
    }

    public void removeChallenge(String fromUser) {
        Iterator<String[]> it = challengeList.iterator();
        while (it.hasNext()) {
            if (it.next()[0].equals(fromUser)) {
                it.remove();
                break;
            }
        }
        refreshChallengePanel();
    }

    public void setFindingMatch(boolean finding) {
        findMatchBtn.setEnabled(!finding);
        cancelMatchBtn.setEnabled(finding);
        gameStatusLabel.setText(finding ? "Đang tìm đối thủ..." : " ");
    }

    /** Cập nhật ảnh đại diện từ Base64 do Server gửi về. */
    public void setAvatarBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) {
                this.customAvatar = img;
                if (avatarPanel != null) {
                    avatarPanel.repaint();
                }
            }
        } catch (Exception ignored) {}
    }

    /** Mở hộp thoại chọn ảnh đại diện từ máy tính và gửi lên Server. */
    private void chooseAndUploadAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh đại diện mới");
        chooser.setFileFilter(new FileNameExtensionFilter("Tệp hình ảnh (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
        int res = chooser.showOpenDialog(getTopLevelAncestor());
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage src = ImageIO.read(file);
                if (src == null) {
                    JOptionPane.showMessageDialog(getTopLevelAncestor(), "Không thể đọc định dạng hình ảnh này!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Cắt vuông tâm ảnh và scale 100x100
                int minDim = Math.min(src.getWidth(), src.getHeight());
                int sx = (src.getWidth() - minDim) / 2;
                int sy = (src.getHeight() - minDim) / 2;
                BufferedImage cropped = src.getSubimage(sx, sy, minDim, minDim);

                BufferedImage scaled = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = scaled.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(cropped, 0, 0, 100, 100, null);
                g2.dispose();

                this.customAvatar = scaled;
                if (avatarPanel != null) avatarPanel.repaint();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(scaled, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                callback.send("UPDATE_AVATAR|" + base64);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(getTopLevelAncestor(), "Lỗi khi xử lý hình ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void resetButtons() {
        findMatchBtn.setEnabled(true);
        cancelMatchBtn.setEnabled(false);
        gameStatusLabel.setText(" ");
    }

    public void setStatus(String text) {
        gameStatusLabel.setText(text);
    }

    public void showNotification(String message) {
        statusBarLabel.setText(message);
    }

    // ================================================================
    //  DIALOGS
    // ================================================================

    private void showRules() {
        String rules = "LUẬT CHƠI DÒ MÌN ĐỐI KHÁNG ONLINE\n\n"
                + "1. Hai người chơi cùng thi đấu trên một bàn mìn 9×9 với 10 quả mìn.\n\n"
                + "2. Click chuột trái để mở ô, click chuột phải để cắm/bỏ cờ.\n\n"
                + "3. Người hoàn thành mở hết các ô an toàn trước sẽ giành chiến thắng.\n\n"
                + "4. Đạp trúng ô có mìn sẽ bị xử thua ngay lập tức.\n\n"
                + "5. Hết thời gian giới hạn (5 phút) mà chưa hoàn thành thì trận đấu hòa.\n\n"
                + "---------------------------------------------------------\n"
                + "CƠ CHẾ TÍNH ĐIỂM:\n"
                + "• Thắng: +3 điểm\n"
                + "• Hòa:   +1 điểm\n"
                + "• Thua:  +0 điểm\n";
        JOptionPane.showMessageDialog(getTopLevelAncestor(), rules,
                "Luật chơi", JOptionPane.INFORMATION_MESSAGE);
    }

    // ================================================================
    //  INTERNAL REFRESH
    // ================================================================

    private void refreshOnlineList() {
        onlineListPanel.removeAll();
        if (onlinePlayers.isEmpty()) {
            onlineListPanel.add(createMutedLabel("Không có người chơi nào khác online"));
        } else {
            for (String[] p : onlinePlayers) {
                int s = 0;
                try { s = Integer.parseInt(p[1]); } catch (NumberFormatException ignored) {}
                onlineListPanel.add(createPlayerRow(p[0], s, p.length > 2 ? p[2] : "ONLINE"));
            }
        }
        onlineListPanel.revalidate();
        onlineListPanel.repaint();
    }

    private void refreshChallengePanel() {
        challengePanel.removeAll();
        if (challengeList.isEmpty()) {
            challengePanel.add(createMutedLabel("Chưa có lời mời nào"));
        } else {
            for (String[] c : challengeList) {
                int s = 0;
                try { s = Integer.parseInt(c[1]); } catch (NumberFormatException ignored) {}
                challengePanel.add(createChallengeRow(c[0], s));
            }
        }
        challengePanel.revalidate();
        challengePanel.repaint();
    }

    private void updateOnlineCount() {
        int count = onlinePlayers.size();
        onlineCountLabel.setText(count + " người");
        statusBarLabel.setText(count + " người chơi khác đang online");
    }
}
