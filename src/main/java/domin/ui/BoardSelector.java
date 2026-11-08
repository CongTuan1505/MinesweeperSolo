package domin.ui;

import domin.BoardType;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

/** Shared board picker with a dark popup, board previews and keyboard navigation. */
public final class BoardSelector extends JComboBox<BoardType> {
    private boolean hovered;
    private static final Color SURFACE = new Color(16, 25, 46);
    private static final Color EDGE = new Color(43, 60, 89);

    public BoardSelector() {
        super(BoardType.values());
        setOpaque(false);
        setFocusable(true);
        setForeground(Theme.TEXT_PRIMARY);
        setBackground(SURFACE);
        setBorder(new EmptyBorder(3, 3, 3, 3));
        setPreferredSize(new Dimension(224, 48));
        setMinimumSize(new Dimension(160, 44));
        setMaximumRowCount(3);
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Chọn bàn đấu · Thành tích riêng cho từng loại bàn");
        getAccessibleContext().setAccessibleName("Chọn loại bàn đấu");
        setKeySelectionManager((key, model) -> {
            for (int i=0;i<model.getSize();i++)
                if (Character.toUpperCase(key)==level((BoardType)model.getElementAt(i)).charAt(0)) return i;
            return -1;
        });
        setRenderer((list, value, index, selected, focus) ->
                new BoardRow(value, index >= 0, selected, isEnabled()));
        setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = graphics(g);
                        g2.setColor(isEnabled() ? Theme.ACCENT_CYAN : Theme.TEXT_MUTED);
                        int x = getWidth() / 2, y = getHeight() / 2;
                        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(x - 4, y - 2, x, y + 2);
                        g2.drawLine(x, y + 2, x + 4, y - 2);
                        g2.dispose();
                    }
                };
                arrow.setOpaque(false);
                arrow.setBorder(new EmptyBorder(0, 0, 0, 0));
                arrow.setContentAreaFilled(false);
                arrow.setFocusPainted(false);
                arrow.setPreferredSize(new Dimension(28, 30));
                return arrow;
            }
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean focus) {}
            @Override protected javax.swing.plaf.basic.ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox);
                popup.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(EDGE),
                        new EmptyBorder(5, 5, 5, 5)));
                popup.setBackground(SURFACE);
                popup.getList().setBackground(SURFACE);
                popup.getList().setFixedCellHeight(52);
                return popup;
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = graphics(g);
        boolean active = isEnabled() && (hovered || hasFocus() || isPopupVisible());
        g2.setPaint(new GradientPaint(0, 0, active ? new Color(25, 42, 72) : new Color(20, 31, 54),
                getWidth(), getHeight(), SURFACE));
        g2.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);
        g2.setColor(active ? Theme.ACCENT_CYAN : EDGE);
        g2.setStroke(new BasicStroke(active ? 1.5f : 1f));
        g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }

    private static Graphics2D graphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }
    private static Color accent(BoardType type) {
        return type == BoardType.SMALL ? Theme.ACCENT_CYAN :
                type == BoardType.MEDIUM ? new Color(167, 139, 250) : new Color(251, 171, 91);
    }
    private static String level(BoardType type) {
        return type == BoardType.SMALL ? "CƠ BẢN" : type == BoardType.MEDIUM ? "NÂNG CAO" : "CHUYÊN GIA";
    }
    private static void grid(Graphics2D g, int x, int y, int width, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 18));
        g.fillRoundRect(x-6, y-6, width+12, width+12, 10, 10);
        g.setColor(color);
        for (int r=0;r<3;r++) for(int c=0;c<3;c++)
            g.fillRoundRect(x+c*(width/3), y+r*(width/3), width/3-2, width/3-2, 2, 2);
    }

    private static final class BoardRow extends JPanel {
        private final BoardType type;
        private final boolean popup, selected, enabled;
        BoardRow(BoardType type, boolean popup, boolean selected, boolean enabled) {
            this.type = type == null ? BoardType.SMALL : type;
            this.popup = popup; this.selected = selected; this.enabled = enabled;
            setOpaque(false);
            setPreferredSize(new Dimension(224, 48));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = graphics(g);
            if (!enabled) g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));
            Color color = accent(type);
            if (popup && selected) {
                g2.setColor(new Color(33, 49, 78));
                g2.fillRoundRect(0, 1, getWidth(), getHeight()-2, 10, 10);
                g2.setColor(color);
                g2.fillRoundRect(0, 13, 3, getHeight()-26, 3, 3);
            }
            int cy = getHeight()/2;
            grid(g2, 15, cy-12, 27, color);
            g2.setFont(Theme.BODY_FONT.deriveFont(Font.BOLD, 15f));
            g2.setColor(Theme.TEXT_PRIMARY);
            g2.drawString(level(type), 54, cy+5);
            g2.dispose();
        }
    }

    /** Modal picker uses the same visual language and returns null on cancel/close. */
    public static BoardType chooseBoard(Component parent, String opponent, BoardType initial) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Chọn bàn thách đấu", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setBackground(new Color(11, 18, 34));
        body.setBorder(new EmptyBorder(24, 24, 22, 24));
        JPanel heading = new JPanel(new GridLayout(2,1,0,6));
        heading.setOpaque(false);
        JLabel title = new JLabel("CHỌN CHIẾN TRƯỜNG");
        title.setFont(Theme.TITLE_FONT.deriveFont(20f)); title.setForeground(Theme.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Thách đấu " + opponent + " · Thành tích độc lập theo bàn");
        subtitle.setFont(Theme.SMALL_FONT); subtitle.setForeground(Theme.TEXT_SECONDARY);
        heading.add(title); heading.add(subtitle);
        body.add(heading,BorderLayout.NORTH);
        JPanel options = new JPanel(new GridLayout(1,3,10,0));
        options.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        BoardType[] selected = {initial};
        for (BoardType type : BoardType.values()) {
            JToggleButton button = new JToggleButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = graphics(g);
                    Color color = accent(type);
                    boolean active = isSelected() || getModel().isRollover();
                    g2.setColor(active ? new Color(26, 41, 67) : SURFACE);
                    g2.fillRoundRect(1,1,getWidth()-2,getHeight()-2,16,16);
                    g2.setColor(isSelected() ? color : hasFocus() ? Theme.ACCENT_CYAN : EDGE);
                    g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
                    g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,16,16);
                    grid(g2,getWidth()/2-13,22,27,color);
                    g2.setFont(Theme.TITLE_FONT.deriveFont(15f));
                    g2.setColor(Theme.TEXT_PRIMARY);
                    String label = level(type);
                    g2.drawString(label,(getWidth()-g2.getFontMetrics().stringWidth(label))/2,88);
                    g2.dispose();
                }
            };
            button.setPreferredSize(new Dimension(150,112));
            button.setContentAreaFilled(false); button.setBorderPainted(false);
            button.setFocusPainted(false); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.getAccessibleContext().setAccessibleName(level(type));
            button.setSelected(type==initial);
            button.addActionListener(e -> selected[0]=type);
            group.add(button); options.add(button);
        }
        body.add(options,BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        actions.setOpaque(false);
        BoardType[] result = {null};
        JButton cancel = action("Để sau",false);
        cancel.addActionListener(e -> dialog.dispose());
        JButton confirm = action("GỬI THÁCH ĐẤU",true);
        confirm.addActionListener(e -> { result[0]=selected[0]; dialog.dispose(); });
        actions.add(cancel); actions.add(confirm); body.add(actions,BorderLayout.SOUTH);
        dialog.setContentPane(body);
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack(); dialog.setResizable(false); dialog.setLocationRelativeTo(parent); dialog.setVisible(true);
        return result[0];
    }

    private static JButton action(String text, boolean primary) {
        JButton button = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = graphics(g);
                g2.setColor(primary ? (getModel().isRollover() ? Theme.PRIMARY_HOVER : Theme.PRIMARY) : SURFACE);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                if (hasFocus()) { g2.setColor(Theme.ACCENT_CYAN); g2.drawRoundRect(1,1,getWidth()-3,getHeight()-3,12,12); }
                g2.dispose(); super.paintComponent(g);
            }
        };
        button.setForeground(Theme.TEXT_PRIMARY); button.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD,12f));
        button.setContentAreaFilled(false); button.setBorderPainted(false); button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12,18,12,18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
