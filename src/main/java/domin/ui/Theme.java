package domin.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Theme — Bảng màu, font chữ, hằng số giao diện (tương tự Theme trong QuizDoiKhang).
 * Tập trung toàn bộ style ở đây để dễ thay đổi giao diện.
 */
public final class Theme {

    // ---- Bảng màu ----
    public static final Color COVERED       = new Color(189, 189, 189);
    public static final Color OPENED        = new Color(236, 236, 236);
    public static final Color MINE_BG       = new Color(231, 76, 60);
    public static final Color FLAG_RED      = new Color(200, 0, 0);
    public static final Color FLAG_GRAY     = new Color(60, 60, 60);
    public static final Color SURRENDER_RED = new Color(163, 45, 45);
    public static final Color BORDER_OPENED = new Color(160, 160, 160);

    /** Màu số mìn xung quanh (index 0 = 0 mìn, ... index 8 = 8 mìn). */
    public static final Color[] NUMBER_COLORS = {
        Color.BLACK, new Color(0, 0, 255), new Color(0, 128, 0), new Color(255, 0, 0),
        new Color(0, 0, 128), new Color(128, 0, 0), new Color(0, 128, 128),
        Color.BLACK, Color.GRAY
    };

    // ---- Typography ----
    private static final String FONT_FAMILY = resolveFontFamily();

    public static final Font NAME_FONT   = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font TIMER_FONT  = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font CELL_FONT   = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private static String resolveFontFamily() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        for (String preferred : new String[]{"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Arial"}) {
            for (String f : fontNames) {
                if (f.equalsIgnoreCase(preferred)) return f;
            }
        }
        return Font.SANS_SERIF;
    }

    // ---- Gaming & HomeScreen colors (đồng bộ tông màu với LoginScreen) ----
    public static final Color BG_DARK         = new Color(240, 242, 246);
    public static final Color BG_NAVBAR       = new Color(255, 255, 255);
    public static final Color BG_CARD         = new Color(255, 255, 255);
    public static final Color BORDER_CARD     = new Color(222, 226, 235);
    public static final Color TEXT_PRIMARY    = new Color(30, 32, 38);
    public static final Color TEXT_SECONDARY  = new Color(80, 85, 100);
    public static final Color TEXT_MUTED      = new Color(140, 145, 158);
    public static final Color ACCENT_CYAN     = new Color(55, 60, 72);
    public static final Color ACCENT_GOLD     = new Color(217, 130, 0);
    public static final Color ACCENT_GREEN    = new Color(40, 160, 80);
    public static final Color WIN_GREEN       = new Color(40, 160, 80);
    public static final Color LOSE_RED        = new Color(210, 60, 60);
    public static final Color DRAW_GOLD       = new Color(200, 130, 20);
    public static final Color STATUS_ONLINE   = new Color(40, 167, 69);
    public static final Color STATUS_IN_MATCH = new Color(245, 130, 32);
    public static final Color BTN_PRIMARY_BG  = new Color(55, 60, 72);
    public static final Color BTN_DANGER_BG   = new Color(210, 60, 60);
    public static final Color BTN_SUCCESS_BG  = new Color(40, 160, 80);

    // ---- HomeScreen Typography ----
    public static final Font TITLE_FONT    = new Font(FONT_FAMILY, Font.BOLD, 18);
    public static final Font SUBTITLE_FONT = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font STAT_FONT     = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font BODY_FONT     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font SMALL_FONT    = new Font(FONT_FAMILY, Font.PLAIN, 11);

    // ---- Kích thước ô ----
    public static final int CELL_SIZE = 34;

    private Theme() {}
}
