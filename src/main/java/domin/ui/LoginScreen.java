package domin.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Màn hình đăng nhập / đăng ký — Giao diện hiện đại, cao cấp.
 *
 * Thiết kế: nền gradient tối, card bo góc, text field tùy chỉnh,
 * nút bấm gradient + hover animation, tab chuyển Login/Register.
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

    // ---- Bảng màu ----
    private static final Color BG_TOP       = new Color(15, 23, 42);
    private static final Color BG_BOTTOM    = new Color(30, 41, 59);
    private static final Color CARD_BG      = new Color(30, 41, 59, 230);
    private static final Color CARD_BORDER  = new Color(51, 65, 85);
    private static final Color FIELD_BG     = new Color(15, 23, 42);
    private static final Color FIELD_BORDER = new Color(71, 85, 105);
    private static final Color FIELD_FOCUS  = new Color(56, 189, 248);
    private static final Color TEXT_PRIMARY  = new Color(241, 245, 249);
    private static final Color TEXT_SECONDARY= new Color(148, 163, 184);
    private static final Color ACCENT_1     = new Color(56, 189, 248);   // sky-400
    private static final Color ACCENT_2     = new Color(99, 102, 241);   // indigo-500
    private static final Color BTN_HOVER_1  = new Color(14, 165, 233);   // sky-500
    private static final Color BTN_HOVER_2  = new Color(79, 70, 229);    // indigo-600
    private static final Color TAB_ACTIVE   = ACCENT_1;
    private static final Color TAB_INACTIVE = new Color(71, 85, 105);
    private static final Color ERROR_COLOR  = new Color(248, 113, 113);
    private static final Color SUCCESS_COLOR= new Color(74, 222, 128);

    // ---- Font ----
    private static final Font TITLE_FONT   = resolveFont(Font.BOLD, 28);
    private static final Font SUBTITLE_FONT= resolveFont(Font.PLAIN, 13);
    private static final Font LABEL_FONT   = resolveFont(Font.BOLD, 12);
    private static final Font FIELD_FONT   = resolveFont(Font.PLAIN, 14);
    private static final Font BTN_FONT     = resolveFont(Font.BOLD, 14);
    private static final Font TAB_FONT     = resolveFont(Font.BOLD, 13);
    private static final Font STATUS_FONT  = resolveFont(Font.PLAIN, 12);

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
    private final JTextField userField;
    private final JPasswordField passField;
    private final JPasswordField confirmPassField;
    private final JLabel confirmPassLabel;
    private final Component confirmPassStrut1;
    private final Component confirmPassStrut2;
    private final JLabel statusLabel;
    private final SendCallback sendCallback;

    // ---- Tab buttons ----
    private final JLabel loginTab;
    private final JLabel registerTab;

    public LoginScreen(SendCallback sendCallback) {
        this.sendCallback = sendCallback;
        setLayout(new GridBagLayout());  // center the card
        setOpaque(false);

        // ---- Card panel ----
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(CARD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Card border
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 40, 36, 40));
        card.setPreferredSize(new Dimension(400, 520));

        // ---- Logo / Title ----
        JLabel logo = new JLabel("\uD83D\uDCA3");  // 💣
        logo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logo);
        card.add(Box.createVerticalStrut(8));

        JLabel title = new JLabel("DÒ MÌN ĐỐI KHÁNG");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Đăng nhập để bắt đầu thi đấu");
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));

        // ---- Tab bar (Login / Register) ----
        JPanel tabBar = new JPanel(new GridLayout(1, 2, 0, 0));
        tabBar.setOpaque(false);
        tabBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tabBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginTab = createTab("ĐĂNG NHẬP", true);
        registerTab = createTab("ĐĂNG KÝ", false);
        tabBar.add(loginTab);
        tabBar.add(registerTab);

        loginTab.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchMode(true, subtitle); }
            @Override public void mouseEntered(MouseEvent e) { if (!isLoginMode) loginTab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
        });
        registerTab.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchMode(false, subtitle); }
            @Override public void mouseEntered(MouseEvent e) { if (isLoginMode) registerTab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
        });

        card.add(tabBar);
        card.add(Box.createVerticalStrut(24));

        // ---- Username field ----
        JLabel userLabel = new JLabel("TÊN ĐĂNG NHẬP");
        userLabel.setFont(LABEL_FONT);
        userLabel.setForeground(TEXT_SECONDARY);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(userLabel);
        card.add(Box.createVerticalStrut(6));

        userField = createStyledTextField();
        card.add(userField);
        card.add(Box.createVerticalStrut(16));

        // ---- Password field ----
        JLabel passLabel = new JLabel("MẬT KHẨU");
        passLabel.setFont(LABEL_FONT);
        passLabel.setForeground(TEXT_SECONDARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));

        passField = createStyledPasswordField();
        card.add(passField);
        card.add(Box.createVerticalStrut(16));

        // ---- Confirm password field (chỉ hiện khi đăng ký) ----
        confirmPassLabel = new JLabel("XÁC NHẬN MẬT KHẨU");
        confirmPassLabel.setFont(LABEL_FONT);
        confirmPassLabel.setForeground(TEXT_SECONDARY);
        confirmPassLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmPassLabel.setVisible(false);
        card.add(confirmPassLabel);
        confirmPassStrut1 = Box.createVerticalStrut(6);
        confirmPassStrut1.setVisible(false);
        card.add(confirmPassStrut1);

        confirmPassField = createStyledPasswordField();
        confirmPassField.setVisible(false);
        card.add(confirmPassField);
        confirmPassStrut2 = Box.createVerticalStrut(24);
        confirmPassStrut2.setVisible(false);
        card.add(confirmPassStrut2);

        card.add(Box.createVerticalStrut(8));

        // ---- Submit button ----
        JPanel submitBtn = createGradientButton("ĐĂNG NHẬP");
        submitBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                submit(isLoginMode ? "LOGIN" : "REGISTER");
            }
        });
        card.add(submitBtn);
        card.add(Box.createVerticalStrut(16));

        // ---- Status label ----
        statusLabel = new JLabel(" ");
        statusLabel.setFont(STATUS_FONT);
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(statusLabel);

        // ---- Enter key to submit ----
        Action submitAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit(isLoginMode ? "LOGIN" : "REGISTER");
            }
        };
        userField.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { passField.requestFocusInWindow(); }
        });
        passField.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { submit(isLoginMode ? "LOGIN" : "REGISTER"); }
        });

        add(card);
    }

    // ---- Paint gradient background ----
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        GradientPaint gp = new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM);
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Subtle decorative circles
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f));
        g2.setColor(ACCENT_1);
        g2.fillOval(-80, -80, 300, 300);
        g2.setColor(ACCENT_2);
        g2.fillOval(getWidth() - 200, getHeight() - 200, 350, 350);
        g2.dispose();
    }

    // ================ Helper methods ================

    private JLabel createTab(String text, boolean active) {
        JLabel tab = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw bottom indicator line
                if (getForeground().equals(TAB_ACTIVE)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(TAB_ACTIVE);
                    g2.fillRect(8, getHeight() - 3, getWidth() - 16, 3);
                    g2.dispose();
                }
            }
        };
        tab.setFont(TAB_FONT);
        tab.setForeground(active ? TAB_ACTIVE : TAB_INACTIVE);
        tab.setOpaque(false);
        tab.setPreferredSize(new Dimension(100, 40));
        return tab;
    }

    private void switchMode(boolean loginMode, JLabel subtitle) {
        if (this.isLoginMode == loginMode) return;
        this.isLoginMode = loginMode;
        loginTab.setForeground(loginMode ? TAB_ACTIVE : TAB_INACTIVE);
        registerTab.setForeground(loginMode ? TAB_INACTIVE : TAB_ACTIVE);
        loginTab.repaint();
        registerTab.repaint();
        subtitle.setText(loginMode ? "Đăng nhập để bắt đầu thi đấu" : "Tạo tài khoản mới để thi đấu");
        statusLabel.setText(" ");

        // Show/hide confirm password
        boolean showConfirm = !loginMode;
        confirmPassLabel.setVisible(showConfirm);
        confirmPassStrut1.setVisible(showConfirm);
        confirmPassField.setVisible(showConfirm);
        confirmPassStrut2.setVisible(showConfirm);
        if (!showConfirm) confirmPassField.setText("");

        // Revalidate card to adjust layout
        revalidate();
        repaint();

        // Update button text
        Container parent = this;
        updateButtonText(parent, loginMode ? "ĐĂNG NHẬP" : "ĐĂNG KÝ");
    }

    private void updateButtonText(Container container, String text) {
        for (Component c : container.getComponents()) {
            if (c instanceof Container) {
                updateButtonText((Container) c, text);
            }
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                if ("ĐĂNG NHẬP".equals(label.getText()) || "ĐĂNG KÝ".equals(label.getText())) {
                    label.setText(text);
                }
            }
        }
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hasFocus() ? FIELD_FOCUS : FIELD_BORDER);
                g2.setStroke(new BasicStroke(hasFocus() ? 2f : 1.5f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setFont(FIELD_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_1);
        field.setBorder(new EmptyBorder(10, 14, 10, 14));
        field.setPreferredSize(new Dimension(0, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hasFocus() ? FIELD_FOCUS : FIELD_BORDER);
                g2.setStroke(new BasicStroke(hasFocus() ? 2f : 1.5f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setFont(FIELD_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_1);
        field.setBorder(new EmptyBorder(10, 14, 10, 14));
        field.setPreferredSize(new Dimension(0, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private JPanel createGradientButton(String text) {
        final boolean[] hovered = {false};
        JPanel btn = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = hovered[0] ? BTN_HOVER_1 : ACCENT_1;
                Color c2 = hovered[0] ? BTN_HOVER_2 : ACCENT_2;
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), 0, c2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle glow on hover
                if (hovered[0]) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, getWidth(), getHeight() / 2);
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setLayout(new BorderLayout());
        btn.setPreferredSize(new Dimension(0, 46));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(BTN_FONT);
        label.setForeground(Color.WHITE);
        btn.add(label, BorderLayout.CENTER);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered[0] = true; btn.repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered[0] = false; btn.repaint(); }
        });

        return btn;
    }

    private void submit(String command) {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }
        if (pass.contains("|") || user.contains("|")) {
            setStatus("Không dùng ký tự '|'");
            return;
        }
        // Kiểm tra xác nhận mật khẩu khi đăng ký
        if (!isLoginMode) {
            String confirmPass = new String(confirmPassField.getPassword());
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
        statusLabel.setText(text);
        // Color based on content
        if (text.contains("thành công")) {
            statusLabel.setForeground(SUCCESS_COLOR);
        } else if (text.trim().isEmpty()) {
            statusLabel.setForeground(TEXT_SECONDARY);
        } else {
            statusLabel.setForeground(ERROR_COLOR);
        }
    }
}
