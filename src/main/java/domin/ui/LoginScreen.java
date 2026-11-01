package domin.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Màn hình đăng nhập / đăng ký — Dark Modern Gaming Theme
 * Đồng bộ phong cách Futuristic / Dark Tactical với HomeScreen.
 */
public class LoginScreen extends JPanel {

    /** Callback khi đăng nhập thành công. */
    public interface LoginCallback {
        void onLoginSuccess(String username);
    }

    /** Callback gửi lệnh lên server. */
    public interface SendCallback {
        void send(String message);
    }

    // ---- Dark Gaming Palette (Đồng bộ với HomeScreen & Theme.java) ----
    private static final Color BG_DARK        = new Color(0x07, 0x0A, 0x14); // #070A14
    private static final Color BG_LEFT        = new Color(0x06, 0x09, 0x14);
    private static final Color BG_RIGHT       = new Color(0x0A, 0x0E, 0x1C);
    private static final Color DIVIDER_COLOR  = new Color(0x1F, 0x2A, 0x40);
    private static final Color FIELD_BG       = new Color(0x0E, 0x15, 0x28);
    private static final Color FIELD_BORDER   = new Color(0x1F, 0x2A, 0x42);
    private static final Color FIELD_FOCUS    = new Color(0x38, 0xBD, 0xF8); // Cyan focus
    private static final Color TEXT_PRIMARY   = new Color(0xF8, 0xFA, 0xFC); // Trắng sáng
    private static final Color TEXT_SECONDARY = new Color(0x94, 0xA3, 0xB8); // Xám chữ
    private static final Color TEXT_DIM       = new Color(0x64, 0x74, 0x8B); // Mờ
    private static final Color ACCENT_CYAN    = new Color(0x22, 0xD3, 0xEE); // #22D3EE
    private static final Color PRIMARY_BLUE   = new Color(0x25, 0x63, 0xEB); // #2563EB
    private static final Color LINK_COLOR     = new Color(0x38, 0xBD, 0xF8);
    private static final Color ERROR_COLOR    = new Color(0xF8, 0x71, 0x71);
    private static final Color SUCCESS_COLOR  = new Color(0x34, 0xD3, 0x99);

    // ---- Typography ----
    private static final String FONT_FAMILY = resolveFontFamily();
    private static final Font HEADING_FONT   = new Font(FONT_FAMILY, Font.BOLD, 26);
    private static final Font SUBTITLE_FONT  = new Font(FONT_FAMILY, Font.PLAIN, 12);
    private static final Font LABEL_FONT     = new Font(FONT_FAMILY, Font.BOLD, 11);
    private static final Font FIELD_FONT     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    private static final Font BTN_FONT       = new Font(FONT_FAMILY, Font.BOLD, 14);
    private static final Font LINK_FONT      = new Font(FONT_FAMILY, Font.PLAIN, 12);
    private static final Font LINK_BOLD_FONT = new Font(FONT_FAMILY, Font.BOLD, 12);
    private static final Font STATUS_FONT    = new Font(FONT_FAMILY, Font.PLAIN, 12);
    private static final Font HINT_FONT      = new Font(FONT_FAMILY, Font.PLAIN, 11);

    // ---- Kích thước chuẩn cho input & button ----
    private static final int FIELD_WIDTH  = 380;
    private static final int FIELD_HEIGHT = 44;
    private static final int BTN_HEIGHT   = 48;

    private static String resolveFontFamily() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        for (String pref : new String[]{"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Arial"}) {
            for (String f : available) {
                if (f.equalsIgnoreCase(pref)) return f;
            }
        }
        return Font.SANS_SERIF;
    }

    // ---- State ----
    private boolean isLoginMode = true;
    private final SendCallback sendCallback;

    // ---- Login fields ----
    private JTextField loginUserField;
    private JPasswordField loginPassField;
    private JLabel loginStatusLabel;

    // ---- Register fields ----
    private JTextField regNameField;
    private JPasswordField regPassField;
    private JPasswordField regConfirmField;
    private JLabel regStatusLabel;

    // ---- Panels ----
    private final JPanel rightPanel;
    private final CardLayout cardLayout;

    // ---- Images ----
    private BufferedImage mineImage;

    public LoginScreen(SendCallback sendCallback) {
        this.sendCallback = sendCallback;
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        loadImages();

        // ================ LEFT PANEL (Tactical Logo Showcase) ================
        JPanel leftPanel = createLeftPanel();
        leftPanel.setPreferredSize(new Dimension(380, 0));

        // ================ DIVIDER ================
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x07, 0x0A, 0x14),
                        0, getHeight() / 2, DIVIDER_COLOR);
                g2.setPaint(gp);
                int x = getWidth() / 2;
                g2.fillRect(x, 24, 1, getHeight() - 48);
                g2.dispose();
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(1, 0));

        // ================ RIGHT PANEL (Form with CardLayout) ================
        cardLayout = new CardLayout();
        rightPanel = new JPanel(cardLayout);
        rightPanel.setOpaque(false);

        JPanel loginForm = createLoginForm();
        JPanel registerForm = createRegisterForm();
        rightPanel.add(loginForm, "LOGIN");
        rightPanel.add(registerForm, "REGISTER");

        cardLayout.show(rightPanel, "LOGIN");

        // ---- Assemble layout ----
        JPanel leftWithDivider = new JPanel(new BorderLayout(0, 0));
        leftWithDivider.setOpaque(false);
        leftWithDivider.add(leftPanel, BorderLayout.CENTER);
        leftWithDivider.add(divider, BorderLayout.EAST);

        add(leftWithDivider, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    // ---- Left panel: Tactical Showcase ----
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Deep dark cyber background
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x0A, 0x0F, 0x22),
                        0, getHeight(), new Color(0x06, 0x09, 0x14));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int cx = getWidth() / 2;
                int cy = getHeight() / 2 - 50;

                // Glowing outer radar disc
                g2.setColor(new Color(0x25, 0x63, 0xEB, 25));
                g2.fillOval(cx - 130, cy - 130, 260, 260);

                // Cyan radar circle
                g2.setColor(new Color(0x22, 0xD3, 0xEE, 45));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(cx - 110, cy - 110, 220, 220);

                // Dashed tactical inner ring
                float[] dash = {6f, 6f};
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g2.setColor(new Color(0x1F, 0x2A, 0x48));
                g2.drawOval(cx - 80, cy - 80, 160, 160);

                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Minesweeper logo image
        if (mineImage != null) {
            int maxW = 260;
            int maxH = 170;
            double scale = Math.min((double) maxW / mineImage.getWidth(), (double) maxH / mineImage.getHeight());
            int targetW = Math.max(1, (int) Math.round(mineImage.getWidth() * scale));
            int targetH = Math.max(1, (int) Math.round(mineImage.getHeight() * scale));
            ImageIcon icon = new ImageIcon(scaleImage(mineImage, targetW, targetH));
            JLabel imgLabel = new JLabel(icon);
            imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(imgLabel);
            content.add(Box.createVerticalStrut(14));
        }

        JLabel titleBadge = new JLabel("MINESWEEPER BATTLE");
        titleBadge.setFont(new Font(FONT_FAMILY, Font.BOLD, 17));
        titleBadge.setForeground(ACCENT_CYAN);
        titleBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleBadge);
        content.add(Box.createVerticalStrut(4));

        JLabel subBadge = new JLabel("ĐẤU TRƯỜNG DÒ MÌN 1v1 TRỰC TUYẾN");
        subBadge.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
        subBadge.setForeground(TEXT_SECONDARY);
        subBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(subBadge);

        panel.add(content);
        return panel;
    }

    // ================ LOGIN FORM ================
    private JPanel createLoginForm() {
        JPanel wrapper = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_RIGHT);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setLayout(new GridBagLayout());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(24, 36, 20, 36));
        form.setPreferredSize(new Dimension(480, 530));

        // Heading
        JLabel heading = new JLabel("Chào mừng trở lại!");
        heading.setFont(HEADING_FONT);
        heading.setForeground(TEXT_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(4));

        // Subtitle
        JLabel sub = new JLabel("Đăng nhập để tiếp tục hành trình phá kỷ lục của bạn.");
        sub.setFont(SUBTITLE_FONT);
        sub.setForeground(TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sub);
        form.add(Box.createVerticalStrut(24));

        // Username
        JLabel userLabel = new JLabel("TÊN ĐĂNG NHẬP");
        userLabel.setFont(LABEL_FONT);
        userLabel.setForeground(ACCENT_CYAN);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(userLabel);
        form.add(Box.createVerticalStrut(6));

        loginUserField = createIconTextField(1, "Nhập tên đăng nhập");
        form.add(loginUserField);
        form.add(Box.createVerticalStrut(16));

        // Password
        JLabel passLabel = new JLabel("MẬT KHẨU");
        passLabel.setFont(LABEL_FONT);
        passLabel.setForeground(ACCENT_CYAN);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passLabel);
        form.add(Box.createVerticalStrut(6));

        loginPassField = createIconPasswordField("Nhập mật khẩu");
        form.add(loginPassField);
        form.add(Box.createVerticalStrut(14));

        // Remember me + Forgot password row
        JPanel rememberRow = new JPanel(new BorderLayout());
        rememberRow.setOpaque(false);
        rememberRow.setPreferredSize(new Dimension(FIELD_WIDTH, 24));
        rememberRow.setMaximumSize(new Dimension(FIELD_WIDTH, 24));
        rememberRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkPanel.setOpaque(false);
        final JCheckBox rememberCheck = new JCheckBox("Nhớ đăng nhập") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int boxSize = 16;
                int y = (getHeight() - boxSize) / 2;
                if (isSelected()) {
                    g2.setColor(PRIMARY_BLUE);
                    g2.fill(new RoundRectangle2D.Float(0, y, boxSize, boxSize, 4, 4));
                    // Checkmark
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(4, y + 8, 7, y + 12);
                    g2.drawLine(7, y + 12, 13, y + 5);
                } else {
                    g2.setColor(FIELD_BG);
                    g2.fill(new RoundRectangle2D.Float(0, y, boxSize, boxSize, 4, 4));
                    g2.setColor(FIELD_BORDER);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, y + 0.5f, boxSize - 1, boxSize - 1, 4, 4));
                }

                // Text
                g2.setFont(getFont());
                g2.setColor(TEXT_SECONDARY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), boxSize + 8, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        rememberCheck.setFont(LINK_FONT);
        rememberCheck.setForeground(TEXT_SECONDARY);
        rememberCheck.setOpaque(false);
        rememberCheck.setFocusPainted(false);
        rememberCheck.setBorderPainted(false);
        rememberCheck.setSelected(true);
        rememberCheck.setPreferredSize(new Dimension(160, 24));
        checkPanel.add(rememberCheck);
        rememberRow.add(checkPanel, BorderLayout.WEST);

        final JLabel forgotLink = new JLabel("Quên mật khẩu?");
        forgotLink.setFont(LINK_FONT);
        forgotLink.setForeground(LINK_COLOR);
        forgotLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { forgotLink.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e) { forgotLink.setForeground(LINK_COLOR); }
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(getTopLevelAncestor(),
                        "Tính năng khôi phục mật khẩu đang phát triển.\nVui lòng liên hệ quản trị viên hoặc tạo tài khoản mới.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        rememberRow.add(forgotLink, BorderLayout.EAST);

        form.add(rememberRow);
        form.add(Box.createVerticalStrut(20));

        // Status label (inline)
        loginStatusLabel = new JLabel(" ");
        loginStatusLabel.setFont(STATUS_FONT);
        loginStatusLabel.setForeground(ERROR_COLOR);
        loginStatusLabel.setPreferredSize(new Dimension(FIELD_WIDTH, 18));
        loginStatusLabel.setMaximumSize(new Dimension(FIELD_WIDTH, 18));
        loginStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(loginStatusLabel);
        form.add(Box.createVerticalStrut(8));

        // Submit button
        JPanel loginBtn = createActionButton("ĐĂNG NHẬP", "→");
        loginBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { submit("LOGIN"); }
        });
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(18));

        // Switch to register link
        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setPreferredSize(new Dimension(FIELD_WIDTH, 24));
        switchRow.setMaximumSize(new Dimension(FIELD_WIDTH, 24));
        switchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel switchText = new JLabel("Chưa có tài khoản?");
        switchText.setFont(LINK_FONT);
        switchText.setForeground(TEXT_DIM);
        final JLabel switchLink = new JLabel("Đăng ký ngay");
        switchLink.setFont(LINK_BOLD_FONT);
        switchLink.setForeground(LINK_COLOR);
        switchLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchMode(false); }
            @Override public void mouseEntered(MouseEvent e) { switchLink.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e) { switchLink.setForeground(LINK_COLOR); }
        });
        switchRow.add(switchText);
        switchRow.add(switchLink);
        form.add(switchRow);

        // ---- Enter key to submit ----
        loginUserField.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { loginPassField.requestFocusInWindow(); }
        });
        loginPassField.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { submit("LOGIN"); }
        });

        wrapper.add(form);
        return wrapper;
    }

    // ================ REGISTER FORM ================
    private JPanel createRegisterForm() {
        JPanel wrapper = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_RIGHT);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setLayout(new GridBagLayout());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(20, 36, 16, 36));
        form.setPreferredSize(new Dimension(480, 530));

        // Heading
        JLabel heading = new JLabel("Khởi tạo tài khoản");
        heading.setFont(HEADING_FONT);
        heading.setForeground(TEXT_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(4));

        // Subtitle
        JLabel sub = new JLabel("Tạo tài khoản để tham gia bảng xếp hạng dò mìn.");
        sub.setFont(SUBTITLE_FONT);
        sub.setForeground(TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sub);
        form.add(Box.createVerticalStrut(18));

        // Tên đăng nhập
        JLabel nameLabel = new JLabel("TÊN ĐĂNG NHẬP");
        nameLabel.setFont(LABEL_FONT);
        nameLabel.setForeground(ACCENT_CYAN);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(nameLabel);
        form.add(Box.createVerticalStrut(5));

        regNameField = createIconTextField(1, "Nhập tên đăng nhập (3-20 ký tự)");
        form.add(regNameField);
        form.add(Box.createVerticalStrut(12));

        // Mật khẩu
        JLabel passLabel = new JLabel("MẬT KHẨU");
        passLabel.setFont(LABEL_FONT);
        passLabel.setForeground(ACCENT_CYAN);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passLabel);
        form.add(Box.createVerticalStrut(5));

        regPassField = createIconPasswordField("Tạo mật khẩu (tối thiểu 3 ký tự)");
        form.add(regPassField);
        form.add(Box.createVerticalStrut(12));

        // Xác nhận mật khẩu
        JLabel confirmLabel = new JLabel("XÁC NHẬN MẬT KHẨU");
        confirmLabel.setFont(LABEL_FONT);
        confirmLabel.setForeground(ACCENT_CYAN);
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(confirmLabel);
        form.add(Box.createVerticalStrut(5));

        regConfirmField = createIconPasswordField("Nhập lại mật khẩu");
        form.add(regConfirmField);
        form.add(Box.createVerticalStrut(12));

        // Status label (inline)
        regStatusLabel = new JLabel(" ");
        regStatusLabel.setFont(STATUS_FONT);
        regStatusLabel.setForeground(ERROR_COLOR);
        regStatusLabel.setPreferredSize(new Dimension(FIELD_WIDTH, 18));
        regStatusLabel.setMaximumSize(new Dimension(FIELD_WIDTH, 18));
        regStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(regStatusLabel);
        form.add(Box.createVerticalStrut(8));

        // Submit button
        JPanel regBtn = createActionButton("ĐĂNG KÝ NGAY", "→");
        regBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { submit("REGISTER"); }
        });
        form.add(regBtn);
        form.add(Box.createVerticalStrut(16));

        // Switch to login link
        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setPreferredSize(new Dimension(FIELD_WIDTH, 24));
        switchRow.setMaximumSize(new Dimension(FIELD_WIDTH, 24));
        switchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel switchText = new JLabel("Đã có tài khoản?");
        switchText.setFont(LINK_FONT);
        switchText.setForeground(TEXT_DIM);
        final JLabel switchLink = new JLabel("Đăng nhập");
        switchLink.setFont(LINK_BOLD_FONT);
        switchLink.setForeground(LINK_COLOR);
        switchLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchMode(true); }
            @Override public void mouseEntered(MouseEvent e) { switchLink.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e) { switchLink.setForeground(LINK_COLOR); }
        });
        switchRow.add(switchText);
        switchRow.add(switchLink);
        form.add(switchRow);

        // ---- Enter key to submit ----
        regNameField.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { regPassField.requestFocusInWindow(); }
        });
        regPassField.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { regConfirmField.requestFocusInWindow(); }
        });
        regConfirmField.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { submit("REGISTER"); }
        });

        wrapper.add(form);
        return wrapper;
    }

    // ================ HELPER COMPONENTS ================

    /** Text field với vector icon bên trái. */
    private JTextField createIconTextField(final int iconType, final String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);

                // Vector Icon on Left
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g3.setColor(hasFocus() ? ACCENT_CYAN : new Color(0x64, 0x74, 0x8B));
                int icX = 14;
                int icY = getHeight() / 2;
                if (iconType == 1) { // User icon
                    g3.fillOval(icX + 3, icY - 9, 8, 8);
                    g3.fillArc(icX, icY + 1, 14, 12, 0, 180);
                }
                g3.dispose();

                // Placeholder
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g4 = (Graphics2D) g.create();
                    g4.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g4.setColor(TEXT_DIM);
                    g4.setFont(getFont());
                    Insets insets = getInsets();
                    g4.drawString(placeholder, insets.left, getHeight() / 2 + g4.getFontMetrics().getAscent() / 2 - 2);
                    g4.dispose();
                }
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hasFocus()) {
                    g2.setColor(FIELD_FOCUS);
                    g2.setStroke(new BasicStroke(1.5f));
                } else {
                    g2.setColor(FIELD_BORDER);
                    g2.setStroke(new BasicStroke(1f));
                }
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setFont(FIELD_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_CYAN);
        field.setBorder(new EmptyBorder(10, 38, 10, 14));
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.repaint(); }
            @Override public void focusLost(FocusEvent e) { field.repaint(); }
        });
        return field;
    }

    /** Password field với vector lock icon và eye toggle. */
    private JPasswordField createIconPasswordField(final String placeholder) {
        final JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);

                // Lock icon on Left
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g3.setColor(hasFocus() ? ACCENT_CYAN : new Color(0x64, 0x74, 0x8B));
                int icX = 14;
                int icY = getHeight() / 2;
                g3.setStroke(new BasicStroke(1.8f));
                g3.drawArc(icX + 3, icY - 9, 8, 8, 0, 180);
                g3.fillRoundRect(icX + 1, icY - 3, 12, 10, 3, 3);
                g3.setColor(FIELD_BG);
                g3.fillOval(icX + 5, icY, 4, 4);
                g3.dispose();

                // Vector Eye toggle icon on Right
                Graphics2D g4 = (Graphics2D) g.create();
                g4.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean visible = getEchoChar() == (char) 0;
                g4.setColor(visible ? ACCENT_CYAN : new Color(0x64, 0x74, 0x8B));
                int eyeX = getWidth() - 28;
                int eyeY = getHeight() / 2;
                g4.setStroke(new BasicStroke(1.4f));
                g4.drawArc(eyeX - 8, eyeY - 6, 16, 12, 30, 120);
                g4.drawArc(eyeX - 8, eyeY - 6, 16, 12, 210, 120);
                g4.fillOval(eyeX - 3, eyeY - 3, 6, 6);
                if (!visible) {
                    g4.drawLine(eyeX - 7, eyeY + 5, eyeX + 7, eyeY - 5);
                }
                g4.dispose();

                // Placeholder
                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D g5 = (Graphics2D) g.create();
                    g5.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g5.setColor(TEXT_DIM);
                    g5.setFont(getFont());
                    Insets insets = getInsets();
                    g5.drawString(placeholder, insets.left, getHeight() / 2 + g5.getFontMetrics().getAscent() / 2 - 2);
                    g5.dispose();
                }
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hasFocus()) {
                    g2.setColor(FIELD_FOCUS);
                    g2.setStroke(new BasicStroke(1.5f));
                } else {
                    g2.setColor(FIELD_BORDER);
                    g2.setStroke(new BasicStroke(1f));
                }
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setFont(FIELD_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_CYAN);
        field.setBorder(new EmptyBorder(10, 38, 10, 42));
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Toggle password visibility on click
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() > field.getWidth() - 42) {
                    if (field.getEchoChar() == (char) 0) {
                        field.setEchoChar('•');
                    } else {
                        field.setEchoChar((char) 0);
                    }
                    field.repaint();
                }
            }
        });
        field.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (e.getX() > field.getWidth() - 42) {
                    field.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    field.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                }
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.repaint(); }
            @Override public void focusLost(FocusEvent e) { field.repaint(); }
        });
        return field;
    }

    /** Nút hành động gradient neon gaming. */
    private JPanel createActionButton(final String text, final String arrow) {
        final boolean[] hovered = {false};
        JPanel btn = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp;
                if (hovered[0]) {
                    gp = new GradientPaint(0, 0, new Color(0x3B, 0x82, 0xF6),
                            getWidth(), 0, new Color(0x06, 0xB6, 0xD4));
                } else {
                    gp = new GradientPaint(0, 0, new Color(0x25, 0x63, 0xEB),
                            getWidth(), 0, new Color(0x02, 0x84, 0xC7));
                }
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                if (hovered[0]) {
                    g2.setColor(new Color(0x67, 0xE8, 0xF9, 140));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));
                }

                // Subtle top gloss
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() / 2, 10, 10));
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setLayout(new BorderLayout());
        btn.setPreferredSize(new Dimension(FIELD_WIDTH, BTN_HEIGHT));
        btn.setMinimumSize(new Dimension(FIELD_WIDTH, BTN_HEIGHT));
        btn.setMaximumSize(new Dimension(FIELD_WIDTH, BTN_HEIGHT));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(text + "  " + arrow, SwingConstants.CENTER);
        label.setFont(BTN_FONT);
        label.setForeground(Color.WHITE);
        btn.add(label, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered[0] = true; btn.repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered[0] = false; btn.repaint(); }
        });

        return btn;
    }

    // ---- Switch between Login/Register ----
    private void switchMode(boolean loginMode) {
        if (this.isLoginMode == loginMode) return;
        this.isLoginMode = loginMode;
        cardLayout.show(rightPanel, loginMode ? "LOGIN" : "REGISTER");
    }

    // ---- Paint background ----
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        GradientPaint gp = new GradientPaint(0, 0, BG_DARK, 0, getHeight(), new Color(0x05, 0x07, 0x10));
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    // ---- Load images ----
    private void loadImages() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/images/logotest.png");
            if (is != null) {
                mineImage = ImageIO.read(is);
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

    // ---- Submit ----
    private void submit(String command) {
        String user, pass;
        if ("LOGIN".equals(command)) {
            user = loginUserField.getText().trim();
            pass = new String(loginPassField.getPassword());
        } else {
            user = regNameField.getText().trim();
            pass = new String(regPassField.getPassword());
        }

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }
        if (pass.contains("|") || user.contains("|")) {
            setStatus("Không được dùng ký tự '|'");
            return;
        }
        if ("REGISTER".equals(command)) {
            String confirmPass = new String(regConfirmField.getPassword());
            if (confirmPass.isEmpty()) {
                setStatus("Vui lòng xác nhận lại mật khẩu");
                return;
            }
            if (!pass.equals(confirmPass)) {
                setStatus("Mật khẩu xác nhận không khớp");
                return;
            }
        }
        sendCallback.send(command + "|" + user + "|" + pass);
    }

    /** Cập nhật thông báo trạng thái. */
    public void setStatus(String text) {
        if (text == null) text = " ";
        Color color;
        if (text.contains("thành công")) {
            color = SUCCESS_COLOR;
        } else if (text.trim().isEmpty()) {
            color = TEXT_SECONDARY;
        } else {
            color = ERROR_COLOR;
        }
        if (loginStatusLabel != null) {
            loginStatusLabel.setText(text);
            loginStatusLabel.setForeground(color);
        }
        if (regStatusLabel != null) {
            regStatusLabel.setText(text);
            regStatusLabel.setForeground(color);
        }
    }
}
