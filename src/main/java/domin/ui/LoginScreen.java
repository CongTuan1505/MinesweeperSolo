package domin.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Màn hình đăng nhập / đăng ký — Layout 2 cột theo thiết kế mới.
 *
 * Bên trái : Logo Minesweeper + tên game.
 * Bên phải : Form Đăng Nhập / Đăng Ký với nút xanh lá, icon trường nhập.
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

    // ---- Bảng màu xám trắng ----
    private static final Color BG_DARK       = new Color(240, 240, 242);
    private static final Color BG_LEFT       = new Color(235, 236, 240);
    private static final Color BG_RIGHT      = new Color(250, 250, 252);
    private static final Color DIVIDER_COLOR = new Color(210, 212, 218, 120);
    private static final Color FIELD_BG      = new Color(255, 255, 255);
    private static final Color FIELD_BORDER  = new Color(200, 203, 212);
    private static final Color FIELD_FOCUS   = new Color(90, 95, 110);
    private static final Color TEXT_PRIMARY   = new Color(30, 32, 38);
    private static final Color TEXT_SECONDARY = new Color(80, 85, 100);
    private static final Color TEXT_DIM       = new Color(140, 145, 158);
    private static final Color ACCENT_BTN    = new Color(55, 60, 72);      // Nút bấm xám đậm
    private static final Color ACCENT_BTN_H  = new Color(70, 75, 88);      // Hover
    private static final Color ACCENT_BTN_TEXT = new Color(255, 255, 255);  // Text trên nút
    private static final Color LINK_COLOR    = new Color(70, 80, 110);
    private static final Color ERROR_COLOR   = new Color(210, 60, 60);
    private static final Color SUCCESS_COLOR = new Color(40, 160, 80);
    private static final Color ICON_COLOR    = new Color(150, 155, 168);
    private static final Color CHECKBOX_BG   = new Color(55, 60, 72);

    // ---- Font ----
    private static final Font HEADING_FONT   = resolveFont(Font.BOLD, 28);
    private static final Font SUBTITLE_FONT  = resolveFont(Font.PLAIN, 13);
    private static final Font LABEL_FONT     = resolveFont(Font.BOLD, 12);
    private static final Font FIELD_FONT     = resolveFont(Font.PLAIN, 14);
    private static final Font BTN_FONT       = resolveFont(Font.BOLD, 15);
    private static final Font LINK_FONT      = resolveFont(Font.PLAIN, 13);
    private static final Font LINK_BOLD_FONT = resolveFont(Font.BOLD, 13);
    private static final Font STATUS_FONT    = resolveFont(Font.PLAIN, 12);
    private static final Font TIP_FONT       = resolveFont(Font.PLAIN, 11);
    private static final Font LOGO_FONT      = resolveFont(Font.BOLD, 20);
    private static final Font HINT_FONT      = resolveFont(Font.PLAIN, 11);

    // ---- Kích thước chuẩn cho input & button ----
    private static final int FIELD_WIDTH  = 380;
    private static final int FIELD_HEIGHT = 46;
    private static final int BTN_HEIGHT   = 50;

    private static Font resolveFont(int style, int size) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        for (String pref : new String[]{"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Arial"}) {
            for (String f : available) {
                if (f.equalsIgnoreCase(pref)) return new Font(f, style, size);
            }
        }
        return new Font(Font.SANS_SERIF, style, size);
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

        // ================ LEFT PANEL (Logo) ================
        JPanel leftPanel = createLeftPanel();
        leftPanel.setPreferredSize(new Dimension(380, 0));

        // ================ DIVIDER ================
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DIVIDER_COLOR);
                int x = getWidth() / 2;
                g2.drawLine(x, 40, x, getHeight() - 40);
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

        // ---- Assemble full-width layout ----
        JPanel leftWithDivider = new JPanel(new BorderLayout(0, 0));
        leftWithDivider.setOpaque(false);
        leftWithDivider.add(leftPanel, BorderLayout.CENTER);
        leftWithDivider.add(divider, BorderLayout.EAST);

        add(leftWithDivider, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    // ---- Left panel ----
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_LEFT, 0, getHeight(), new Color(225, 228, 235));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Decorative subtle circle
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
                g2.setColor(new Color(100, 110, 130));
                g2.fillOval(-60, getHeight() / 2 - 120, 240, 240);
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
            int maxW = 320;
            int maxH = 200;
            double scale = Math.min((double) maxW / mineImage.getWidth(), (double) maxH / mineImage.getHeight());
            int targetW = Math.max(1, (int) Math.round(mineImage.getWidth() * scale));
            int targetH = Math.max(1, (int) Math.round(mineImage.getHeight() * scale));
            ImageIcon icon = new ImageIcon(scaleImage(mineImage, targetW, targetH));
            JLabel imgLabel = new JLabel(icon);
            imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(imgLabel);
        }

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
        form.setBorder(new EmptyBorder(28, 36, 20, 36));
        form.setPreferredSize(new Dimension(480, 560));

        // Heading
        JLabel heading = new JLabel("Chào mừng trở lại!");
        heading.setFont(HEADING_FONT);
        heading.setForeground(TEXT_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(6));

        // Subtitle
        JLabel sub = new JLabel("Đăng nhập để tiếp tục hành trình phá kỷ lục của bạn.");
        sub.setFont(SUBTITLE_FONT);
        sub.setForeground(TEXT_DIM);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sub);
        form.add(Box.createVerticalStrut(28));

        // Username
        JLabel userLabel = new JLabel("Tên đăng nhập");
        userLabel.setFont(LABEL_FONT);
        userLabel.setForeground(TEXT_SECONDARY);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(userLabel);
        form.add(Box.createVerticalStrut(6));

        loginUserField = createIconTextField("\uD83D\uDC64", "Nhập tên đăng nhập");
        form.add(loginUserField);
        form.add(Box.createVerticalStrut(18));

        // Password
        JLabel passLabel = new JLabel("Mật khẩu");
        passLabel.setFont(LABEL_FONT);
        passLabel.setForeground(TEXT_SECONDARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passLabel);
        form.add(Box.createVerticalStrut(6));

        loginPassField = createIconPasswordField("\uD83D\uDD12", "Nhập mật khẩu");
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
        JCheckBox rememberCheck = new JCheckBox("Nhớ đăng nhập") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw custom checkbox
                int boxSize = 16;
                int y = (getHeight() - boxSize) / 2;
                if (isSelected()) {
                    g2.setColor(CHECKBOX_BG);
                    g2.fill(new RoundRectangle2D.Float(0, y, boxSize, boxSize, 4, 4));
                    // Checkmark
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(4, y + 8, 7, y + 12);
                    g2.drawLine(7, y + 12, 13, y + 5);
                } else {
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

        JLabel forgotLink = new JLabel("Quên mật khẩu?");
        forgotLink.setFont(LINK_FONT);
        forgotLink.setForeground(LINK_COLOR);
        forgotLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { forgotLink.setForeground(TEXT_PRIMARY); }
            @Override public void mouseExited(MouseEvent e) { forgotLink.setForeground(LINK_COLOR); }
        });
        rememberRow.add(forgotLink, BorderLayout.EAST);

        form.add(rememberRow);
        form.add(Box.createVerticalStrut(24));

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
        JPanel loginBtn = createActionButton("Đăng nhập", "→");
        loginBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { submit("LOGIN"); }
        });
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(16));

        // Switch to register link
        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setPreferredSize(new Dimension(FIELD_WIDTH, 24));
        switchRow.setMaximumSize(new Dimension(FIELD_WIDTH, 24));
        switchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel switchText = new JLabel("Chưa có tài khoản?");
        switchText.setFont(LINK_FONT);
        switchText.setForeground(TEXT_DIM);
        JLabel switchLink = new JLabel("Đăng ký ngay");
        switchLink.setFont(LINK_BOLD_FONT);
        switchLink.setForeground(LINK_COLOR);
        switchLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchMode(false); }
            @Override public void mouseEntered(MouseEvent e) { switchLink.setForeground(TEXT_PRIMARY); }
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
        form.setBorder(new EmptyBorder(28, 36, 20, 36));
        form.setPreferredSize(new Dimension(480, 560));

        // Heading
        JLabel heading = new JLabel("Bắt đầu cuộc chơi.");
        heading.setFont(HEADING_FONT);
        heading.setForeground(TEXT_PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(6));

        // Subtitle
        JLabel sub = new JLabel("Tạo tài khoản để tham gia thi đấu dò mìn.");
        sub.setFont(SUBTITLE_FONT);
        sub.setForeground(TEXT_DIM);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sub);
        form.add(Box.createVerticalStrut(22));

        // Tên đăng nhập
        JLabel nameLabel = new JLabel("Tên đăng nhập");
        nameLabel.setFont(LABEL_FONT);
        nameLabel.setForeground(TEXT_SECONDARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(nameLabel);
        form.add(Box.createVerticalStrut(6));

        regNameField = createIconTextField("\uD83D\uDC64", "Nhập tên đăng nhập");
        form.add(regNameField);
        form.add(Box.createVerticalStrut(14));

        // Mật khẩu
        JLabel passLabel = new JLabel("Mật khẩu");
        passLabel.setFont(LABEL_FONT);
        passLabel.setForeground(TEXT_SECONDARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passLabel);
        form.add(Box.createVerticalStrut(6));

        regPassField = createIconPasswordField("\uD83D\uDD12", "Tạo mật khẩu");
        form.add(regPassField);
        form.add(Box.createVerticalStrut(4));

        // Hint
        JLabel passHint = new JLabel("Ít nhất 8 ký tự, gồm chữ và số.");
        passHint.setFont(HINT_FONT);
        passHint.setForeground(TEXT_DIM);
        passHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passHint);
        form.add(Box.createVerticalStrut(12));

        // Xác nhận mật khẩu
        JLabel confirmLabel = new JLabel("Xác nhận mật khẩu");
        confirmLabel.setFont(LABEL_FONT);
        confirmLabel.setForeground(TEXT_SECONDARY);
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(confirmLabel);
        form.add(Box.createVerticalStrut(6));

        regConfirmField = createIconPasswordField("\uD83D\uDD12", "Nhập lại mật khẩu");
        form.add(regConfirmField);
        form.add(Box.createVerticalStrut(16));

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
        JPanel regBtn = createActionButton("Đăng ký", "→");
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
        JLabel switchLink = new JLabel("Đăng nhập");
        switchLink.setFont(LINK_BOLD_FONT);
        switchLink.setForeground(LINK_COLOR);
        switchLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        switchLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchMode(true); }
            @Override public void mouseEntered(MouseEvent e) { switchLink.setForeground(TEXT_PRIMARY); }
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

    // ================ HELPER METHODS ================

    /** Text field có icon bên trái. */
    private JTextField createIconTextField(String icon, String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);

                // Icon
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g3.setColor(ICON_COLOR);
                g3.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
                g3.drawString(icon, 12, getHeight() / 2 + 5);
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
                g2.setColor(hasFocus() ? FIELD_FOCUS : FIELD_BORDER);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setFont(FIELD_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(FIELD_FOCUS);
        field.setBorder(new EmptyBorder(10, 36, 10, 14));
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

    /** Password field có icon bên trái và toggle hiện/ẩn. */
    private JPasswordField createIconPasswordField(String icon, String placeholder) {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);

                // Icon
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g3.setColor(ICON_COLOR);
                g3.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
                g3.drawString(icon, 12, getHeight() / 2 + 5);
                g3.dispose();

                // Eye toggle icon on right
                Graphics2D g4 = (Graphics2D) g.create();
                g4.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g4.setColor(ICON_COLOR);
                g4.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                String eyeIcon = getEchoChar() == (char) 0 ? "👁" : "👁‍🗨";
                g4.drawString(eyeIcon, getWidth() - 32, getHeight() / 2 + 5);
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
                g2.setColor(hasFocus() ? FIELD_FOCUS : FIELD_BORDER);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setFont(FIELD_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(FIELD_FOCUS);
        field.setBorder(new EmptyBorder(10, 36, 10, 40));
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Toggle password visibility on click in the eye area
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() > field.getWidth() - 40) {
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
                if (e.getX() > field.getWidth() - 40) {
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

    /** Nút xám đậm. */
    private JPanel createActionButton(String text, String arrow) {
        final boolean[] hovered = {false};
        JPanel btn = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color c = hovered[0] ? ACCENT_BTN_H : ACCENT_BTN;
                g2.setColor(c);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

                // Subtle shine on top half when hovered
                if (hovered[0]) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                    g2.setColor(Color.WHITE);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() / 2, 12, 12));
                }
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
        label.setForeground(ACCENT_BTN_TEXT);
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

    // ---- Paint gradient background ----
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        GradientPaint gp = new GradientPaint(0, 0, BG_DARK, 0, getHeight(), new Color(230, 232, 238));
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
            } else {
                System.err.println("Resource not found: /images/logotest.png");
            }
        } catch (Exception e) {
            System.err.println("Could not load logotest.png: " + e.getMessage());
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
            setStatus("Nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }
        if (pass.contains("|") || user.contains("|")) {
            setStatus("Không dùng ký tự '|'");
            return;
        }
        // Kiểm tra xác nhận mật khẩu khi đăng ký
        if ("REGISTER".equals(command)) {
            String confirmPass = new String(regConfirmField.getPassword());
            if (confirmPass.isEmpty()) {
                setStatus("Nhập xác nhận mật khẩu");
                return;
            }
            if (!pass.equals(confirmPass)) {
                setStatus("Mật khẩu xác nhận không khớp");
                return;
            }
        }
        sendCallback.send(command + "|" + user + "|" + pass);
    }

    /** Hiện thông báo trên label trạng thái. */
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
        // Update both status labels
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
