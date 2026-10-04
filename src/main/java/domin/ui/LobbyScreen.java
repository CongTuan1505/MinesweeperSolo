package domin.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Sảnh chờ: bấm "Tìm trận" để vào hàng đợi (tương tự LobbyScreen trong QuizDoiKhang).
 */
public class LobbyScreen extends JPanel {

    /** Callback cho các hành động lobby. */
    public interface LobbyCallback {
        void onFindMatch();
        void onCancelMatch();
    }

    private final JLabel statusLabel = new JLabel("Sẵn sàng thi đấu", SwingConstants.CENTER);
    private final JButton findBtn = new JButton("Tìm trận");
    private final JButton cancelBtn = new JButton("Hủy tìm");

    public LobbyScreen(String username, final LobbyCallback callback) {
        setLayout(new GridLayout(0, 1, 8, 12));
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel hello = new JLabel("Xin chào, " + username, SwingConstants.CENTER);
        add(hello);
        add(statusLabel);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 8));
        buttons.add(findBtn);
        buttons.add(cancelBtn);
        cancelBtn.setEnabled(false);
        add(buttons);

        findBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.onFindMatch();
                findBtn.setEnabled(false);
                cancelBtn.setEnabled(true);
                statusLabel.setText("Đang tìm đối thủ...");
            }
        });
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.onCancelMatch();
            }
        });
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /** Reset trạng thái nút khi hủy tìm trận. */
    public void resetButtons() {
        findBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
    }
}
