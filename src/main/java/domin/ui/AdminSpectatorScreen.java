package domin.ui;



import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * AdminSpectatorScreen — Màn hình thi đấu trực tiếp (Spectator Mode) dành cho Quản trị viên.
 *
 * Hiển thị song song hai bàn cờ của 2 người chơi trong cặp đấu:
 * - Bàn cờ Người chơi 1 (bên trái) với các ô đã mở và cờ đã cắm của P1
 * - Bàn cờ Người chơi 2 (bên phải) với các ô đã mở và cờ đã cắm của P2
 * - Cột trung tâm: Đồng hồ trực tiếp, Bảng so sánh tiến độ, Nhật ký hành động trận đấu (Combat Log)
 * - Tính năng Radar Admin (God Mode): Bật/Tắt hiển thị vị trí mìn ngầm để quan sát trực quan
 * - Khi trận kết thúc: Banner thông báo kết quả chi tiết kèm nút quay lại
 */
public class AdminSpectatorScreen extends JPanel {

    public interface SpectatorCallback {
        void send(String message);
        void onExitSpectate();
    }

    private final String matchId;
    private final String p1Name;
    private final String p2Name;
    private final int rows;
    private final int cols;
    private final int totalMines;
    private final SpectatorCallback callback;

    // Board states
    private final Set<String> mineLocations = new HashSet<>();
    private final SpectatorCell[][] p1Cells;
    private final SpectatorCell[][] p2Cells;

    // HUD Elements
    private JLabel timerLabel;
    private JLabel p1StatusLabel;
    private JLabel p2StatusLabel;
    private JProgressBar p1Bar;
    private JProgressBar p2Bar;
    private DefaultListModel<String> combatLogModel;
    private JList<String> combatLogList;

    // God Mode Toggle
    private boolean godMode = true;

    // Match over state
    private boolean isFinished = false;
    private JPanel gameOverOverlay;
    private JLabel gameOverResultLabel;
    private JLabel gameOverReasonLabel;

    public AdminSpectatorScreen(String matchId, String p1Name, String p2Name,
                                int rows, int cols, int totalMines, int initialElapsed,
                                final SpectatorCallback callback) {
        this.matchId = matchId;
        this.p1Name = p1Name;
        this.p2Name = p2Name;
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.callback = callback;

        this.p1Cells = new SpectatorCell[rows][cols];
        this.p2Cells = new SpectatorCell[rows][cols];

        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_DARK);

        // ==============================================================
        //  1. TOP BAR: Back Button, Match Title, Live Clock, God Mode Toggle
        // ==============================================================
        JPanel topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        // ==============================================================
        //  2. CENTER: Left Board (P1) | Center HUD & Combat Log | Right Board (P2)
        // ==============================================================
        JPanel arenaPanel = new JPanel(new GridBagLayout());
        arenaPanel.setOpaque(false);
        arenaPanel.setBorder(new EmptyBorder(10, 16, 12, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // --- Left Arena: Player 1 ---
        gbc.gridx = 0;
        gbc.weightx = 0.42;
        JPanel leftPanel = createPlayerArenaPanel(p1Name, Theme.ACCENT_CYAN, Theme.PRIMARY, p1Cells, 1);
        arenaPanel.add(leftPanel, gbc);

        // --- Center HUD: Timer, Progress comparison, Live Feed ---
        gbc.gridx = 1;
        gbc.weightx = 0.22;
        JPanel centerHUD = createCenterHUDPanel();
        arenaPanel.add(centerHUD, gbc);

        // --- Right Arena: Player 2 ---
        gbc.gridx = 2;
        gbc.weightx = 0.42;
        JPanel rightPanel = createPlayerArenaPanel(p2Name, new Color(0xFB, 0x92, 0x3C), new Color(0xEF, 0x44, 0x44), p2Cells, 2);
        arenaPanel.add(rightPanel, gbc);

        // Layers for Game Over announcement
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));

        gameOverOverlay = createGameOverOverlay();
        gameOverOverlay.setVisible(false);

        layeredPane.add(gameOverOverlay, Integer.valueOf(2));
        layeredPane.add(arenaPanel, Integer.valueOf(1));

        add(layeredPane, BorderLayout.CENTER);

        updateTimer(initialElapsed);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0)) {
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
        bar.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Back Button
        JButton backBtn = new JButton("← QUAY LẠI QUẢN TRỊ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0x2A, 0x38, 0x54));
                } else {
                    g2.setColor(new Color(0x1A, 0x24, 0x3A));
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(Theme.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        backBtn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        backBtn.setForeground(Theme.TEXT_PRIMARY);
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (callback != null) {
                    callback.onExitSpectate();
                }
            }
        });

        // Center Title & LIVE Badge
        JPanel centerBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        centerBox.setOpaque(false);

        JLabel liveBadge = new JLabel("● TRỰC TIẾP") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xEF, 0x44, 0x44, 40));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(0xEF, 0x44, 0x44));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        liveBadge.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        liveBadge.setForeground(new Color(0xF8, 0x71, 0x71));
        liveBadge.setBorder(new EmptyBorder(3, 8, 3, 8));

        JLabel title = new JLabel("GIÁM SÁT TRẬN ĐẤU: " + p1Name + "  VS  " + p2Name);
        title.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 15));
        title.setForeground(Theme.TEXT_PRIMARY);

        centerBox.add(liveBadge);
        centerBox.add(title);

        // Right: God Mode Toggle
        final JCheckBox godModeBox = new JCheckBox("Hiện vị trí mìn (Radar Quản trị)", godMode);
        godModeBox.setOpaque(false);
        godModeBox.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 12));
        godModeBox.setForeground(Theme.ACCENT_CYAN);
        godModeBox.setFocusPainted(false);
        godModeBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        godModeBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                godMode = godModeBox.isSelected();
                repaintBoards();
            }
        });

        bar.add(backBtn, BorderLayout.WEST);
        bar.add(centerBox, BorderLayout.CENTER);
        bar.add(godModeBox, BorderLayout.EAST);
        return bar;
    }

    private JPanel createPlayerArenaPanel(String playerName, Color accentColor, Color barColor,
                                          SpectatorCell[][] cellsGrid, int playerIndex) {
        JPanel arena = new JPanel(new BorderLayout(0, 10));
        arena.setOpaque(false);

        // Player Header Card
        JPanel headerCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Theme.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        headerCard.setOpaque(false);
        headerCard.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel tag = new JLabel(playerIndex == 1 ? "● NGƯỜI CHƠI 1" : "● NGƯỜI CHƠI 2");
        tag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        tag.setForeground(accentColor);

        JLabel nameLbl = new JLabel(playerName);
        nameLbl.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 16));
        nameLbl.setForeground(Theme.TEXT_PRIMARY);

        JPanel infoLeft = new JPanel();
        infoLeft.setLayout(new BoxLayout(infoLeft, BoxLayout.Y_AXIS));
        infoLeft.setOpaque(false);
        infoLeft.add(tag);
        infoLeft.add(Box.createVerticalStrut(2));
        infoLeft.add(nameLbl);

        JLabel statusLabel = new JLabel("Đang thi đấu  •  0%");
        statusLabel.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 12));
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        if (playerIndex == 1) {
            this.p1StatusLabel = statusLabel;
        } else {
            this.p2StatusLabel = statusLabel;
        }

        headerCard.add(infoLeft, BorderLayout.WEST);
        headerCard.add(statusLabel, BorderLayout.EAST);

        // Board Wrapper
        JPanel boardWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        boardWrapper.setOpaque(false);

        JPanel boardBorder = new JPanel(new GridLayout(rows, cols, 2, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0A, 0x0E, 0x1C));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(Theme.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        boardBorder.setOpaque(false);
        boardBorder.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cellsGrid[r][c] = new SpectatorCell(r, c);
                boardBorder.add(cellsGrid[r][c]);
            }
        }
        boardWrapper.add(boardBorder);

        arena.add(headerCard, BorderLayout.NORTH);
        arena.add(boardWrapper, BorderLayout.CENTER);
        return arena;
    }

    private JPanel createCenterHUDPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 6, 0, 6));

        // Timer Box
        JPanel timerCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Theme.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        timerCard.setOpaque(false);
        timerCard.setLayout(new BoxLayout(timerCard, BoxLayout.Y_AXIS));
        timerCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel timeTag = new JLabel("THỜI GIAN THI ĐẤU", SwingConstants.CENTER);
        timeTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        timeTag.setForeground(Theme.TEXT_MUTED);
        timeTag.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerLabel = new JLabel("00:00 / 05:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font(Theme.STAT_FONT.getFamily(), Font.BOLD, 22));
        timerLabel.setForeground(Theme.ACCENT_CYAN);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        timerCard.add(timeTag);
        timerCard.add(Box.createVerticalStrut(4));
        timerCard.add(timerLabel);

        // Progress comparison Card
        JPanel progCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Theme.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        progCard.setOpaque(false);
        progCard.setLayout(new BoxLayout(progCard, BoxLayout.Y_AXIS));
        progCard.setBorder(new EmptyBorder(10, 14, 12, 14));

        JLabel progTag = new JLabel("TIẾN ĐỘ MỞ Ô AN TOÀN", SwingConstants.CENTER);
        progTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        progTag.setForeground(Theme.TEXT_MUTED);
        progTag.setAlignmentX(Component.CENTER_ALIGNMENT);

        p1Bar = new JProgressBar(0, 100);
        p1Bar.setValue(0);
        p1Bar.setForeground(Theme.ACCENT_CYAN);
        p1Bar.setBackground(new Color(0x1F, 0x2A, 0x40));
        p1Bar.setPreferredSize(new Dimension(0, 8));

        p2Bar = new JProgressBar(0, 100);
        p2Bar.setValue(0);
        p2Bar.setForeground(new Color(0xFB, 0x92, 0x3C));
        p2Bar.setBackground(new Color(0x1F, 0x2A, 0x40));
        p2Bar.setPreferredSize(new Dimension(0, 8));

        progCard.add(progTag);
        progCard.add(Box.createVerticalStrut(8));
        progCard.add(p1Bar);
        progCard.add(Box.createVerticalStrut(6));
        progCard.add(p2Bar);

        // Combat Log Feed Card
        JPanel logCard = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Theme.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        logCard.setOpaque(false);
        logCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel logTag = new JLabel("NHẬT KÝ CHIẾN ĐẤU (LIVE)");
        logTag.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.BOLD, 10));
        logTag.setForeground(Theme.TEXT_MUTED);

        combatLogModel = new DefaultListModel<>();
        combatLogModel.addElement("Bắt đầu giám sát trận đấu.");
        combatLogList = new JList<>(combatLogModel);
        combatLogList.setOpaque(false);
        combatLogList.setBackground(new Color(0, 0, 0, 0));
        combatLogList.setForeground(Theme.TEXT_SECONDARY);
        combatLogList.setFont(new Font(Theme.SMALL_FONT.getFamily(), Font.PLAIN, 11));
        combatLogList.setSelectionBackground(new Color(0x1F, 0x2A, 0x40));
        combatLogList.setSelectionForeground(Theme.TEXT_PRIMARY);

        JScrollPane logScroll = new JScrollPane(combatLogList);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(BorderFactory.createEmptyBorder());

        logCard.add(logTag, BorderLayout.NORTH);
        logCard.add(logScroll, BorderLayout.CENTER);

        JPanel topCluster = new JPanel();
        topCluster.setLayout(new BoxLayout(topCluster, BoxLayout.Y_AXIS));
        topCluster.setOpaque(false);
        topCluster.add(timerCard);
        topCluster.add(Box.createVerticalStrut(10));
        topCluster.add(progCard);

        center.add(topCluster, BorderLayout.NORTH);
        center.add(logCard, BorderLayout.CENTER);
        return center;
    }

    private JPanel createGameOverOverlay() {
        JPanel overlay = new JPanel(new GridBagLayout());
        overlay.setOpaque(false);

        JPanel modal = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0C, 0x12, 0x24, 245));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(Theme.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));
                g2.dispose();
            }
        };
        modal.setOpaque(false);
        modal.setLayout(new BoxLayout(modal, BoxLayout.Y_AXIS));
        modal.setBorder(new EmptyBorder(24, 36, 24, 36));

        JLabel title = new JLabel("TRẬN ĐẤU ĐÃ KẾT THÚC!", SwingConstants.CENTER);
        title.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 22));
        title.setForeground(Theme.WARNING);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        gameOverResultLabel = new JLabel("Chiến thắng: ...", SwingConstants.CENTER);
        gameOverResultLabel.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 18));
        gameOverResultLabel.setForeground(Theme.TEXT_PRIMARY);
        gameOverResultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        gameOverReasonLabel = new JLabel("Lý do: ...", SwingConstants.CENTER);
        gameOverReasonLabel.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 13));
        gameOverReasonLabel.setForeground(Theme.TEXT_MUTED);
        gameOverReasonLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton exitBtn = new JButton("← QUAY LẠI QUẢN TRỊ VIÊN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        exitBtn.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setFocusPainted(false);
        exitBtn.setBorderPainted(false);
        exitBtn.setContentAreaFilled(false);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitBtn.setBorder(new EmptyBorder(10, 24, 10, 24));
        exitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (callback != null) {
                    callback.onExitSpectate();
                }
            }
        });

        modal.add(title);
        modal.add(Box.createVerticalStrut(10));
        modal.add(gameOverResultLabel);
        modal.add(Box.createVerticalStrut(4));
        modal.add(gameOverReasonLabel);
        modal.add(Box.createVerticalStrut(18));
        modal.add(exitBtn);

        overlay.add(modal);
        return overlay;
    }

    // ==============================================================
    //  State Updating Methods called from Client.java
    // ==============================================================

    public void initMines(String rawMines) {
        mineLocations.clear();
        if (rawMines != null && !rawMines.trim().isEmpty()) {
            for (String item : rawMines.split(";")) {
                if (!item.isEmpty()) {
                    mineLocations.add(item);
                }
            }
        }
        repaintBoards();
    }

    public void initPlayerRevealed(int playerIdx, String rawCells) {
        SpectatorCell[][] grid = (playerIdx == 1) ? p1Cells : p2Cells;
        if (rawCells != null && !rawCells.trim().isEmpty()) {
            for (String item : rawCells.split(";")) {
                String[] v = item.split(",");
                if (v.length >= 3) {
                    try {
                        int r = Integer.parseInt(v[0]);
                        int c = Integer.parseInt(v[1]);
                        int num = Integer.parseInt(v[2]);
                        if (r >= 0 && r < rows && c >= 0 && c < cols) {
                            grid[r][c].setOpened(num);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public void initPlayerFlags(int playerIdx, String rawFlags) {
        SpectatorCell[][] grid = (playerIdx == 1) ? p1Cells : p2Cells;
        if (rawFlags != null && !rawFlags.trim().isEmpty()) {
            for (String item : rawFlags.split(";")) {
                String[] v = item.split(",");
                if (v.length >= 3) {
                    try {
                        int r = Integer.parseInt(v[0]);
                        int c = Integer.parseInt(v[1]);
                        int flag = Integer.parseInt(v[2]);
                        if (r >= 0 && r < rows && c >= 0 && c < cols) {
                            grid[r][c].setFlag(flag);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public void handleOpen(String playerName, String cellsData) {
        SpectatorCell[][] grid = (p1Name.equalsIgnoreCase(playerName)) ? p1Cells : p2Cells;
        int openedCount = 0;
        if (cellsData != null && !cellsData.isEmpty()) {
            for (String item : cellsData.split(";")) {
                String[] v = item.split(",");
                if (v.length >= 3) {
                    try {
                        int r = Integer.parseInt(v[0]);
                        int c = Integer.parseInt(v[1]);
                        int num = Integer.parseInt(v[2]);
                        if (r >= 0 && r < rows && c >= 0 && c < cols) {
                            grid[r][c].setOpened(num);
                            openedCount++;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (openedCount > 1) {
            addCombatLog(playerName + " vừa mở một cụm " + openedCount + " ô an toàn!");
        } else if (openedCount == 1) {
            addCombatLog(playerName + " đã mở ô an toàn.");
        }
    }

    public void handleFlag(String playerName, int r, int c, int flagType) {
        SpectatorCell[][] grid = (p1Name.equalsIgnoreCase(playerName)) ? p1Cells : p2Cells;
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            grid[r][c].setFlag(flagType);
        }
        if (flagType == 1) {
            addCombatLog(playerName + " đã cắm cờ tại (" + (r + 1) + ", " + (c + 1) + ")");
        } else if (flagType == 2) {
            addCombatLog(playerName + " đặt dấu ? tại (" + (r + 1) + ", " + (c + 1) + ")");
        } else {
            addCombatLog(playerName + " đã gỡ cờ tại (" + (r + 1) + ", " + (c + 1) + ")");
        }
    }

    public void updateProgress(int p1Percent, int p2Percent, int p1Flags, int p2Flags, String p1Status, String p2Status) {
        if (p1Bar != null) p1Bar.setValue(p1Percent);
        if (p2Bar != null) p2Bar.setValue(p2Percent);
        if (p1StatusLabel != null) {
            p1StatusLabel.setText(p1Status + "  •  " + p1Percent + "% (Cờ: " + p1Flags + ")");
        }
        if (p2StatusLabel != null) {
            p2StatusLabel.setText(p2Status + "  •  " + p2Percent + "% (Cờ: " + p2Flags + ")");
        }
    }

    public void updateTimer(int elapsedSec) {
        if (timerLabel != null) {
            int curMin = elapsedSec / 60;
            int curSec = elapsedSec % 60;
            timerLabel.setText(String.format("%02d:%02d / 05:00", curMin, curSec));
        }
    }

    public void addCombatLog(String message) {
        if (combatLogModel != null) {
            combatLogModel.addElement(message);
            if (combatLogList != null) {
                combatLogList.ensureIndexIsVisible(combatLogModel.size() - 1);
            }
        }
    }

    public void handleGameOver(String winner, String reason, int elapsed, int p1Safe, int p2Safe, int p1Flags, int p2Flags) {
        isFinished = true;
        // Reveal all mines on both boards
        for (String m : mineLocations) {
            String[] v = m.split(",");
            if (v.length == 2) {
                int r = Integer.parseInt(v[0]);
                int c = Integer.parseInt(v[1]);
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    p1Cells[r][c].setRevealedMine();
                    p2Cells[r][c].setRevealedMine();
                }
            }
        }

        String resultText;
        if ("DRAW".equalsIgnoreCase(winner)) {
            resultText = "KẾT QUẢ: TRẬN ĐẤU HÒA";
        } else {
            resultText = "CHIẾN THẮNG: " + winner.toUpperCase();
        }

        String reasonDetail;
        switch (reason) {
            case "COMPLETE": reasonDetail = "Hoàn thành toàn bộ bàn mìn trước đối thủ"; break;
            case "MINE": reasonDetail = "Đối thủ đã đạp trúng mìn"; break;
            case "SURRENDER": reasonDetail = "Đối thủ đã đầu hàng / bỏ cuộc"; break;
            case "DISCONNECT": reasonDetail = "Đối thủ bị mất kết nối"; break;
            case "TIMEOUT": reasonDetail = "Hết thời gian thi đấu 5 phút"; break;
            default: reasonDetail = reason; break;
        }

        if (gameOverResultLabel != null) gameOverResultLabel.setText(resultText);
        if (gameOverReasonLabel != null) gameOverReasonLabel.setText("Lý do: " + reasonDetail + " (Thời gian: " + elapsed + "s)");
        if (gameOverOverlay != null) gameOverOverlay.setVisible(true);

        addCombatLog("Trận đấu kết thúc! " + resultText);
        repaint();
    }

    private void repaintBoards() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                p1Cells[r][c].repaint();
                p2Cells[r][c].repaint();
            }
        }
    }

    // ==============================================================
    //  Spectator Board Cell Component
    // ==============================================================
    private class SpectatorCell extends JPanel {
        private final int r;
        private final int c;
        private boolean opened = false;
        private int number = 0;
        private int flag = 0; // 0: None, 1: Flag, 2: Question
        private boolean isMineRevealed = false;

        public SpectatorCell(int r, int c) {
            this.r = r;
            this.c = c;
            setOpaque(false);
            setPreferredSize(new Dimension(36, 36));
        }

        public void setOpened(int num) {
            this.opened = true;
            this.number = num;
            this.flag = 0;
            repaint();
        }

        public void setFlag(int f) {
            if (!opened) {
                this.flag = f;
                repaint();
            }
        }

        public void setRevealedMine() {
            this.isMineRevealed = true;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            boolean hasMine = mineLocations.contains(r + "," + c);

            if (opened) {
                // Opened state
                g2.setColor(new Color(0x1B, 0x24, 0x38));
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 4, 4));
                g2.setColor(new Color(0x28, 0x36, 0x50));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 4, 4));

                if (number > 0) {
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                    g2.setColor(getNumberColor(number));
                    FontMetrics fm = g2.getFontMetrics();
                    String str = String.valueOf(number);
                    g2.drawString(str, (w - fm.stringWidth(str)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
                }
            } else {
                // Covered state
                g2.setColor(new Color(0x14, 0x1B, 0x2E));
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 4, 4));
                g2.setColor(new Color(0x22, 0x2E, 0x48));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 4, 4));

                if (flag == 1) {
                    // Flag icon (Red triangle flag)
                    g2.setColor(new Color(0xEF, 0x44, 0x44));
                    Polygon p = new Polygon();
                    p.addPoint(w / 2 - 3, h / 2 - 8);
                    p.addPoint(w / 2 + 7, h / 2 - 4);
                    p.addPoint(w / 2 - 3, h / 2);
                    g2.fill(p);

                    g2.setColor(Color.WHITE);
                    g2.drawLine(w / 2 - 3, h / 2 - 8, w / 2 - 3, h / 2 + 8);
                    g2.drawLine(w / 2 - 6, h / 2 + 8, w / 2 + 2, h / 2 + 8);
                } else if (flag == 2) {
                    // Question mark
                    g2.setColor(Theme.WARNING);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("?", (w - fm.stringWidth("?")) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
                } else if (isMineRevealed || (godMode && hasMine)) {
                    // God-mode Admin Mine Radar or Game Over mine reveal
                    if (isMineRevealed) {
                        g2.setColor(new Color(0xEF, 0x44, 0x44, 180));
                    } else {
                        g2.setColor(new Color(0xEF, 0x44, 0x44, 75)); // Translucent radar hint
                    }
                    g2.fillOval(w / 2 - 6, h / 2 - 6, 12, 12);
                    g2.setColor(new Color(0xF8, 0x71, 0x71));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawOval(w / 2 - 6, h / 2 - 6, 12, 12);
                    // Center dot
                    g2.fillOval(w / 2 - 2, h / 2 - 2, 4, 4);
                }
            }
            g2.dispose();
        }

        private Color getNumberColor(int num) {
            switch (num) {
                case 1: return new Color(0x38, 0xBD, 0xF8); // Cyan
                case 2: return new Color(0x34, 0xD3, 0x99); // Emerald green
                case 3: return new Color(0xF8, 0x71, 0x71); // Light red
                case 4: return new Color(0x81, 0x8C, 0xF8); // Indigo
                case 5: return new Color(0xFB, 0x92, 0x3C); // Amber / orange
                case 6: return new Color(0x2D, 0xD4, 0xBF); // Teal
                case 7: return new Color(0xF4, 0x72, 0xB6); // Pink
                case 8: return Color.WHITE;
                default: return Theme.TEXT_PRIMARY;
            }
        }
    }
}
