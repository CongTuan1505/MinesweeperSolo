package domin.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Theme — Modern dark gaming color palette, fonts, and layout constants.
 * Inspired by competitive multiplayer dashboards.
 */
public final class Theme {

    // ==============================================================
    //  GameScreen cell colors (unchanged – game logic depends on these)
    // ==============================================================
    public static final Color COVERED       = new Color(189, 189, 189);
    public static final Color OPENED        = new Color(236, 236, 236);
    public static final Color MINE_BG       = new Color(231, 76, 60);
    public static final Color FLAG_RED      = new Color(200, 0, 0);
    public static final Color FLAG_GRAY     = new Color(60, 60, 60);
    public static final Color SURRENDER_RED = new Color(163, 45, 45);
    public static final Color BORDER_OPENED = new Color(160, 160, 160);

    /** Mine-neighbour number colors (index 0–8). */
    public static final Color[] NUMBER_COLORS = {
        Color.BLACK, new Color(0, 0, 255), new Color(0, 128, 0), new Color(255, 0, 0),
        new Color(0, 0, 128), new Color(128, 0, 0), new Color(0, 128, 128),
        Color.BLACK, Color.GRAY
    };

    // ==============================================================
    //  Typography
    // ==============================================================
    private static final String FONT_FAMILY = resolveFontFamily();

    public static final Font NAME_FONT  = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font TIMER_FONT = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font CELL_FONT  = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private static String resolveFontFamily() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        for (String preferred : new String[]{"Inter", "Segoe UI", "Roboto", "Helvetica Neue", "Arial"}) {
            for (String f : fontNames) {
                if (f.equalsIgnoreCase(preferred)) return f;
            }
        }
        return Font.SANS_SERIF;
    }

    // ==============================================================
    //  Dark Gaming Palette
    // ==============================================================
    public static final Color BG_DARK         = new Color(0x0B, 0x10, 0x20);       // #0B1020
    public static final Color BG_SECONDARY    = new Color(0x11, 0x18, 0x27);       // #111827
    public static final Color BG_NAVBAR       = new Color(0x0D, 0x13, 0x25);       // slightly lighter navbar
    public static final Color BG_CARD         = new Color(0x15, 0x1D, 0x2E);       // #151D2E
    public static final Color BG_CARD_HOVER   = new Color(0x1C, 0x26, 0x3A);       // #1C263A
    public static final Color BORDER_CARD     = new Color(0x26, 0x32, 0x47);       // #263247

    // Primary action color
    public static final Color PRIMARY         = new Color(0x4F, 0x7C, 0xFF);       // #4F7CFF
    public static final Color PRIMARY_HOVER   = new Color(0x63, 0x8D, 0xFF);       // #638DFF
    public static final Color PRIMARY_GLOW    = new Color(0x4F, 0x7C, 0xFF, 60);   // glow

    // Accent
    public static final Color ACCENT_CYAN     = new Color(0x22, 0xD3, 0xEE);       // #22D3EE

    // Semantic
    public static final Color SUCCESS         = new Color(0x22, 0xC5, 0x5E);       // #22C55E
    public static final Color WARNING         = new Color(0xF5, 0x9E, 0x0B);       // #F59E0B
    public static final Color DANGER          = new Color(0xEF, 0x44, 0x44);       // #EF4444

    // Map legacy names
    public static final Color ACCENT_GOLD     = WARNING;
    public static final Color ACCENT_GREEN    = SUCCESS;
    public static final Color WIN_GREEN       = SUCCESS;
    public static final Color LOSE_RED        = DANGER;
    public static final Color DRAW_GOLD       = WARNING;
    public static final Color STATUS_ONLINE   = SUCCESS;
    public static final Color STATUS_IN_MATCH = WARNING;
    public static final Color BTN_PRIMARY_BG  = PRIMARY;
    public static final Color BTN_DANGER_BG   = DANGER;
    public static final Color BTN_SUCCESS_BG  = SUCCESS;

    // Text
    public static final Color TEXT_PRIMARY    = new Color(0xF8, 0xFA, 0xFC);       // #F8FAFC
    public static final Color TEXT_SECONDARY  = new Color(0x94, 0xA3, 0xB8);       // #94A3B8
    public static final Color TEXT_MUTED      = new Color(0x64, 0x74, 0x8B);       // #64748B

    // ==============================================================
    //  HomeScreen Typography
    // ==============================================================
    public static final Font TITLE_FONT    = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font SUBTITLE_FONT = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font STAT_FONT     = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font BODY_FONT     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font SMALL_FONT    = new Font(FONT_FAMILY, Font.PLAIN, 11);

    // ==============================================================
    //  Cell size
    // ==============================================================
    public static final int CELL_SIZE = 34;

    private Theme() {}
}
