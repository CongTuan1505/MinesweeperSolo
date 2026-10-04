package domin;

/**
 * Định nghĩa các hằng số và data class dùng chung cho cả Server và Client.
 *
 * Tương tự lớp Model trong QuizDoiKhang:
 *  - Hằng số cấu hình bàn mìn (kích thước, số mìn, thời gian)
 *  - Hằng số trạng thái người chơi
 */
public final class Model {

    // ---- Cấu hình bàn mìn ----
    public static final int ROWS = 9;
    public static final int COLS = 9;
    public static final int MINES = 10;
    public static final int SAFE_TOTAL = ROWS * COLS - MINES;
    public static final int TIME_LIMIT_SEC = 300;

    // ---- Trạng thái người chơi (dùng phía Server) ----
    public static final String ST_PLAYING    = "Đang chơi";
    public static final String ST_DONE       = "Đã hoàn thành";
    public static final String ST_MINE       = "Đạp trúng mìn";
    public static final String ST_SURRENDER  = "Bỏ cuộc";
    public static final String ST_DISCONNECT = "Mất kết nối";

    // ---- Cổng mặc định ----
    public static final int PORT = 5555;

    private Model() {}
}
