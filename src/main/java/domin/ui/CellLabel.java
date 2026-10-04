package domin.ui;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 * Ô trên bàn dò mìn (tách ra từ inner class GameFrame.CellLabel).
 * Mỗi ô biết tọa độ (r, c) và callback khi click trái/phải.
 */
public class CellLabel extends JLabel {

    /** Callback khi người chơi click vào ô. */
    public interface CellClickListener {
        void onLeftClick(int r, int c);
        void onRightClick(int r, int c);
    }

    private final int row;
    private final int col;

    public CellLabel(int r, int c, final CellClickListener listener) {
        super("", SwingConstants.CENTER);
        this.row = r;
        this.col = c;
        setOpaque(true);
        setBackground(Theme.COVERED);
        setFont(Theme.CELL_FONT);
        setPreferredSize(new Dimension(Theme.CELL_SIZE, Theme.CELL_SIZE));
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (listener == null) return;
                if (javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                    listener.onLeftClick(row, col);
                } else if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                    listener.onRightClick(row, col);
                }
            }
        });
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
}
