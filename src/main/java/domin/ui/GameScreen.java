package domin.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import java.awt.Component;

/**
 * Màn hình thi đấu dò mìn (tương tự MatchScreen trong QuizDoiKhang).
 *
 * Bố cục:
 *  [1] Bàn dò mìn          - click trái mở ô, click phải đặt cờ
 *  [2] Đồng hồ             - hiển thị thời gian do Server gửi mỗi giây
 *  [3] Tiến độ của bạn     - % ô an toàn đã mở, số cờ, trạng thái
 *  [4] Tiến độ đối thủ     - chỉ số liệu tổng hợp, không lộ vị trí ô
 *  [5] Thông báo realtime  - sự kiện của đối thủ
 *  [6] Nút "Bỏ cuộc"
 */
public class GameScreen extends JPanel implements CellLabel.CellClickListener {

    /** Callback cho các hành động game. */
    public interface GameCallback {
        void onOpenCell(int r, int c);
        void onFlagCell(int r, int c);
        void onSurrender();
    }

    private final GameCallback callback;
    private final int rows;
    private final int cols;
    private final int timeLimit;

    private final CellLabel[][] cells;
    private final boolean[][] revealed;
    private final JLabel timerLabel = new JLabel("00:00", SwingConstants.CENTER);
    private final JLabel notifyLabel = new JLabel("Trận đấu bắt đầu!");
    private final JProgressBar myBar = new JProgressBar(0, 100);
    private final JProgressBar oppBar = new JProgressBar(0, 100);
    private boolean over = false;

    public GameScreen(String myName, String oppName,
                      int rows, int cols, int mines, int timeLimit,
                      GameCallback callback) {
        this.callback = callback;
        this.rows = rows;
        this.cols = cols;
        this.timeLimit = timeLimit;
        this.cells = new CellLabel[rows][cols];
        this.revealed = new boolean[rows][cols];

        setLayout(new BorderLayout());

        // Phần trên: tên hai người và đồng hồ
        JLabel meLabel = new JLabel("Bạn: " + myName);
        JLabel oppLabel = new JLabel("Đối thủ: " + oppName, SwingConstants.RIGHT);
        meLabel.setFont(Theme.NAME_FONT);
        oppLabel.setFont(Theme.NAME_FONT);
        timerLabel.setFont(Theme.TIMER_FONT);
        JPanel top = new JPanel(new GridLayout(1, 3));
        top.setBorder(new EmptyBorder(10, 14, 6, 14));
        top.add(meLabel);
        top.add(timerLabel);
        top.add(oppLabel);
        updateTimer(0);

        // Bàn dò mìn
        JPanel board = new JPanel(new GridLayout(rows, cols, 1, 1));
        board.setBorder(new EmptyBorder(4, 14, 4, 14));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new CellLabel(r, c, this);
                board.add(cells[r][c]);
            }
        }
        JPanel boardWrap = new JPanel();
        boardWrap.add(board);

        // Tiến độ + Thông báo + Bỏ cuộc
        myBar.setStringPainted(true);
        oppBar.setStringPainted(true);
        myBar.setString("Bạn: 0%");
        oppBar.setString("Đối thủ: 0%");
        notifyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        notifyLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JButton surrenderBtn = new JButton("Bỏ cuộc");
        surrenderBtn.setForeground(Theme.SURRENDER_RED);
        surrenderBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { askSurrender(); }
        });

        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 6));
        bottom.setBorder(new EmptyBorder(6, 14, 12, 14));
        bottom.add(myBar);
        bottom.add(oppBar);
        bottom.add(notifyLabel);
        bottom.add(surrenderBtn);

        add(top, BorderLayout.NORTH);
        add(boardWrap, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ---- CellClickListener ----

    @Override
    public void onLeftClick(int r, int c) {
        if (!over) callback.onOpenCell(r, c);
    }

    @Override
    public void onRightClick(int r, int c) {
        if (!over) callback.onFlagCell(r, c);
    }

    // ---- Cập nhật giao diện ----

    public void showOpened(int r, int c, int number) {
        revealed[r][c] = true;
        CellLabel cell = cells[r][c];
        cell.setBackground(Theme.OPENED);
        cell.setBorder(BorderFactory.createLineBorder(Theme.BORDER_OPENED));
        cell.setText(number == 0 ? "" : String.valueOf(number));
        cell.setForeground(Theme.NUMBER_COLORS[Math.min(number, 8)]);
    }

    public void showFlag(int r, int c, int state) {
        CellLabel cell = cells[r][c];
        if (state == 1) {
            cell.setText("F");
            cell.setForeground(Theme.FLAG_RED);
        } else if (state == 2) {
            cell.setText("?");
            cell.setForeground(Theme.FLAG_GRAY);
        } else {
            cell.setText("");
        }
    }

    public void showMine(int r, int c) {
        cells[r][c].setBackground(Theme.MINE_BG);
        cells[r][c].setForeground(Color.WHITE);
        cells[r][c].setText("*");
    }

    public void updateTimer(int seconds) {
        timerLabel.setText(format(seconds) + " / " + format(timeLimit));
    }

    public void updateProgress(int myPercent, int oppPercent, String myFlags,
                               String oppFlags, String myStatus, String oppStatus) {
        myBar.setValue(myPercent);
        oppBar.setValue(oppPercent);
        myBar.setString("Bạn: " + myPercent + "% · cờ " + myFlags + " · " + myStatus);
        oppBar.setString("Đối thủ: " + oppPercent + "% · cờ " + oppFlags + " · " + oppStatus);
    }

    public void setNotify(String text) {
        notifyLabel.setText(text);
    }

    public void setGameOver() {
        over = true;
    }

    public boolean isOver() {
        return over;
    }

    // ---- Tiện ích ----

    private void askSurrender() {
        if (over) return;
        Component parent = getTopLevelAncestor();
        int choice = JOptionPane.showConfirmDialog(parent,
                "Bạn có chắc muốn bỏ cuộc? Bạn sẽ bị xử thua.", "Bỏ cuộc",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            callback.onSurrender();
        }
    }

    public static String format(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
