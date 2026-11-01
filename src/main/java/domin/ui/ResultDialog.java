package domin.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * ResultDialog — Bảng hiển thị kết quả trận đấu 1v1 hiện đại theo phong cách Modern Dark Tactical Gaming.
 * Thay thế hoàn toàn JOptionPane mặc định thô sơ bằng giao diện HUD ấn tượng.
 */
public class ResultDialog extends JDialog {

    public ResultDialog(Frame parent,
                        final String result,
                        String reason,
                        int seconds,
                        int mySafeOpened,
                        int oppSafeOpened,
                        int myFlags,
                        int oppFlags,
                        int points,
                        String myName,
                        String oppName) {
        super(parent, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(480, 570);
        setLocationRelativeTo(parent);

        final boolean isWin = "WIN".equalsIgnoreCase(result);
        final boolean isLose = "LOSE".equalsIgnoreCase(result);
        final boolean isDraw = !isWin && !isLose;

        // Màu chủ đạo theo kết quả
        final Color themeColor = isWin ? Theme.SUCCESS : (isLose ? Theme.DANGER : Theme.ACCENT_CYAN);
        final Color glowColor = isWin ? new Color(0x10, 0xB9, 0x81, 60) :
                                (isLose ? new Color(0xEF, 0x44, 0x44, 60) : new Color(0x22, 0xD3, 0xEE, 60));

        // Khung nền chính Glassmorphism
        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Nền gradient tối sang trọng
                GradientPaint bg = new GradientPaint(
                        0, 0, new Color(0x0C, 0x13, 0x24, 250),
                        0, h, new Color(0x07, 0x0C, 0x18, 252)
                );
                g2.setPaint(bg);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 22, 22));

                // Viền sáng Neon theo kết quả trận đấu
                g2.setColor(glowColor);
                g2.setStroke(new BasicStroke(4f));
                g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 22, 22));

                g2.setColor(themeColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 22, 22));

                g2.dispose();
            }
        };
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(26, 28, 24, 28));

        // 1. Icon huy hiệu chiến trường vẽ vector
        JPanel emblemPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int r = 38;

                // Vầng hào quang ngoài
                g2.setColor(glowColor);
                g2.fill(new Ellipse2D.Float(cx - r - 8, cy - r - 8, (r + 8) * 2, (r + 8) * 2));

                // Vòng tròn trung tâm
                GradientPaint circleGrad = new GradientPaint(
                        cx - r, cy - r, new Color(0x13, 0x20, 0x38),
                        cx + r, cy + r, new Color(0x09, 0x10, 0x20)
                );
                g2.setPaint(circleGrad);
                g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

                g2.setColor(themeColor);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

                // Biểu tượng bên trong
                if (isWin) {
                    // Cúp vàng / Ngôi sao chiến thắng
                    g2.setColor(new Color(0xFB, 0xBF, 0x24));
                    // Vẽ hình chiếc cúp vector
                    int topY = cy - 14;
                    int cupW = 28;
                    int cupH = 18;
                    g2.fillRoundRect(cx - cupW / 2, topY, cupW, cupH, 6, 6);
                    g2.fillRect(cx - 3, topY + cupH, 6, 9);
                    g2.fillRoundRect(cx - 14, topY + cupH + 8, 28, 5, 3, 3);
                    // Quai cúp
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawArc(cx - 19, topY, 10, 14, 90, 180);
                    g2.drawArc(cx + 9, topY, 10, 14, 270, 180);
                } else if (isLose) {
                    // Quả bom / Đầu lâu chiến bại
                    g2.setColor(new Color(0xEF, 0x44, 0x44));
                    g2.fillOval(cx - 14, cy - 10, 28, 28);
                    // Ngòi nổ & tia lửa
                    g2.setColor(new Color(0xF5, 0x9E, 0x0B));
                    g2.fillRect(cx - 3, cy - 17, 6, 7);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(cx, cy - 17, cx + 8, cy - 24);
                    // Mắt chéo X X
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(cx - 9, cy - 4, cx - 3, cy + 2);
                    g2.drawLine(cx - 3, cy - 4, cx - 9, cy + 2);
                    g2.drawLine(cx + 3, cy - 4, cx + 9, cy + 2);
                    g2.drawLine(cx + 9, cy - 4, cx + 3, cy + 2);
                } else {
                    // Khiên hòa trận
                    g2.setColor(Theme.ACCENT_CYAN);
                    int[] xPoints = {cx, cx + 16, cx + 16, cx, cx - 16, cx - 16};
                    int[] yPoints = {cy - 16, cy - 10, cy + 8, cy + 18, cy + 8, cy - 10};
                    g2.fillPolygon(xPoints, yPoints, 6);
                    g2.setColor(new Color(0x0C, 0x13, 0x24));
                    g2.drawPolygon(xPoints, yPoints, 6);
                }

                g2.dispose();
            }
        };
        emblemPanel.setPreferredSize(new Dimension(100, 95));
        emblemPanel.setMaximumSize(new Dimension(100, 95));
        emblemPanel.setOpaque(false);
        emblemPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(emblemPanel);

        // 2. Tiêu đề kết quả
        String titleText = isWin ? "CHIẾN THẮNG!" : (isLose ? "THẤT BẠI!" : "HÒA TRẬN!");
        JLabel titleLbl = new JLabel(titleText, SwingConstants.CENTER);
        titleLbl.setFont(new Font(Theme.TITLE_FONT.getFamily(), Font.BOLD, 26));
        titleLbl.setForeground(themeColor);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(titleLbl);

        root.add(Box.createVerticalStrut(6));

        // 3. Diễn giải lý do
        String reasonText = describeReason(result, reason);
        JLabel reasonLbl = new JLabel(reasonText, SwingConstants.CENTER);
        reasonLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 13));
        reasonLbl.setForeground(Theme.TEXT_SECONDARY);
        reasonLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(reasonLbl);

        root.add(Box.createVerticalStrut(14));

        // 4. Pill điểm thưởng ELO
        JPanel pointsPill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(themeColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
            }
        };
        pointsPill.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pointsPill.setOpaque(false);
        pointsPill.setMaximumSize(new Dimension(240, 32));
        pointsPill.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel pointsLbl = new JLabel("ĐIỂM NHẬN ĐƯỢC: +" + points + " ĐIỂM");
        pointsLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
        pointsLbl.setForeground(themeColor);
        pointsPill.add(pointsLbl);
        root.add(pointsPill);

        root.add(Box.createVerticalStrut(18));

        // 5. Thẻ thống kê so sánh trận đấu (Bạn vs Đối thủ)
        JPanel statsCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x0B, 0x10, 0x20, 220));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(new Color(0x1F, 0x2A, 0x40));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
        statsCard.setLayout(new BoxLayout(statsCard, BoxLayout.Y_AXIS));
        statsCard.setOpaque(false);
        statsCard.setBorder(new EmptyBorder(14, 18, 14, 18));
        statsCard.setMaximumSize(new Dimension(420, 160));
        statsCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Header cột: BẠN vs ĐỐI THỦ
        String p1 = (myName != null && !myName.isEmpty()) ? myName : "BẠN";
        String p2 = (oppName != null && !oppName.isEmpty()) ? oppName : "ĐỐI THỦ";
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel myHeader = new JLabel("BẠN (" + p1 + ")");
        myHeader.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        myHeader.setForeground(Theme.ACCENT_CYAN);
        JLabel vsLabel = new JLabel("VS", SwingConstants.CENTER);
        vsLabel.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 11));
        vsLabel.setForeground(Theme.TEXT_MUTED);
        JLabel oppHeader = new JLabel("(" + p2 + ") ĐỐI THỦ", SwingConstants.RIGHT);
        oppHeader.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        oppHeader.setForeground(new Color(0xF8, 0x71, 0x71));

        headerRow.add(myHeader, BorderLayout.WEST);
        headerRow.add(vsLabel, BorderLayout.CENTER);
        headerRow.add(oppHeader, BorderLayout.EAST);
        statsCard.add(headerRow);

        statsCard.add(Box.createVerticalStrut(10));
        statsCard.add(createDivider());
        statsCard.add(Box.createVerticalStrut(8));

        // Dòng 1: Thời gian thi đấu
        statsCard.add(createStatRow("Thời gian thi đấu", GameScreen.format(seconds)));

        statsCard.add(Box.createVerticalStrut(6));

        // Dòng 2: Số ô an toàn đã mở
        statsCard.add(createComparisonRow("Ô an toàn đã mở", mySafeOpened, oppSafeOpened));

        statsCard.add(Box.createVerticalStrut(6));

        // Dòng 3: Số cờ chiến thuật đã cắm
        statsCard.add(createComparisonRow("Số cờ đã cắm", myFlags, oppFlags));

        root.add(statsCard);

        root.add(Box.createVerticalStrut(22));

        // 6. Nút "QUAY VỀ SẢNH CHỜ" phong cách game hiện đại
        final JButton returnBtn = new JButton("QUAY VỀ SẢNH CHỜ") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = hovered ?
                        new GradientPaint(0, 0, Theme.PRIMARY_HOVER, w, h, Theme.ACCENT_CYAN) :
                        new GradientPaint(0, 0, Theme.PRIMARY, w, h, new Color(0x38, 0xBD, 0xF8));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));

                g2.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        returnBtn.setPreferredSize(new Dimension(420, 44));
        returnBtn.setMaximumSize(new Dimension(420, 44));
        returnBtn.setContentAreaFilled(false);
        returnBtn.setBorderPainted(false);
        returnBtn.setFocusPainted(false);
        returnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        final ActionListener closeAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        returnBtn.addActionListener(closeAction);
        root.add(returnBtn);

        // Đóng bằng phím Enter hoặc Escape
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    dispose();
                }
            }
        };
        returnBtn.addKeyListener(keyAdapter);
        root.addKeyListener(keyAdapter);
        addKeyListener(keyAdapter);

        setContentPane(root);
    }

    private static JPanel createDivider() {
        JPanel div = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0x1E, 0x29, 0x3E));
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setPreferredSize(new Dimension(100, 1));
        div.setOpaque(false);
        return div;
    }

    private static JPanel createStatRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 12));
        lbl.setForeground(Theme.TEXT_SECONDARY);

        JLabel val = new JLabel(value, SwingConstants.RIGHT);
        val.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        val.setForeground(Theme.TEXT_PRIMARY);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private static JPanel createComparisonRow(String label, int myVal, int oppVal) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 12));
        lbl.setForeground(Theme.TEXT_SECONDARY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);

        JLabel myLbl = new JLabel(String.valueOf(myVal));
        myLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        myLbl.setForeground(myVal >= oppVal ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);

        JLabel slash = new JLabel("/");
        slash.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.PLAIN, 11));
        slash.setForeground(Theme.TEXT_MUTED);

        JLabel oppLbl = new JLabel(String.valueOf(oppVal));
        oppLbl.setFont(new Font(Theme.BODY_FONT.getFamily(), Font.BOLD, 12));
        oppLbl.setForeground(oppVal > myVal ? new Color(0xF8, 0x71, 0x71) : Theme.TEXT_PRIMARY);

        right.add(myLbl);
        right.add(slash);
        right.add(oppLbl);

        row.add(lbl, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private static String describeReason(String result, String reason) {
        boolean win = "WIN".equalsIgnoreCase(result);
        if (reason == null) return "";
        switch (reason) {
            case "COMPLETE":
                return win ? "Bạn hoàn thành bàn dò mìn nhanh hơn đối thủ!"
                           : "Đối thủ đã hoàn thành bàn dò mìn trước bạn.";
            case "MINE":
                return win ? "Đối thủ đã bất cẩn đạp trúng mìn!" : "Bạn đã không may đạp trúng mìn!";
            case "SURRENDER":
                return win ? "Đối thủ đã đầu hàng bỏ cuộc!" : "Bạn đã đầu hàng bỏ cuộc.";
            case "DISCONNECT":
                return "Đối thủ bị mất kết nối tới máy chủ.";
            case "TIMEOUT":
                return "Hết thời gian thi đấu mà cả hai chưa hoàn thành.";
            default:
                return reason;
        }
    }
}
