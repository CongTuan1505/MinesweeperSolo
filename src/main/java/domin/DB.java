package domin;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * Lớp truy cập cơ sở dữ liệu (tương tự DB trong QuizDoiKhang).
 *
 * Gộp 3 lớp cũ: Database (kết nối), UserDAO (bảng users), MatchDAO (bảng matches).
 * Cấu hình đọc từ db.properties trên classpath.
 */
public final class DB {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties p = new Properties();
        try (InputStream in = DB.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
            } else {
                System.err.println("Không tìm thấy db.properties trên classpath, dùng cấu hình mặc định.");
            }
        } catch (IOException e) {
            System.err.println("Không đọc được db.properties: " + e.getMessage());
        }
        URL = p.getProperty("db.url",
                "jdbc:mysql://localhost:3306/domin_db?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC&characterEncoding=UTF-8");
        USER = p.getProperty("db.user", "root");
        PASSWORD = p.getProperty("db.password", "15052005");
    }

    // ---- Kết nối ----

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // ---- Bảo mật mật khẩu ----

    /** Băm mật khẩu bằng SHA-256, trả về chuỗi hex 64 ký tự. */
    static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- User DAO ----

    /** @return true nếu đăng ký thành công, false nếu tên đã tồn tại. */
    static boolean register(String username, String password) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // duplicate entry
                return false;
            }
            throw e;
        }
    }

    static boolean login(String username, String password) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? AND password_hash = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Cộng điểm và thống kê thắng/thua/hòa. result = WIN | LOSE | DRAW. */
    static void updateStats(String username, String result) throws SQLException {
        String column;
        int points;
        switch (result) {
            case "WIN":
                column = "wins";
                points = 3;
                break;
            case "DRAW":
                column = "draws";
                points = 1;
                break;
            default:
                column = "losses";
                points = 0;
        }
        String sql = "UPDATE users SET score = score + ?, " + column + " = " + column
                + " + 1 WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    // ---- Match DAO ----

    /**
     * @param result P1_WIN | P2_WIN | DRAW
     * @param reason COMPLETE | MINE | SURRENDER | DISCONNECT | TIMEOUT
     */
    static void saveMatch(String player1, String player2, String result,
                          String reason, int durationSec) throws SQLException {
        String sql = "INSERT INTO matches(player1, player2, result, reason, duration_sec) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, player1);
            ps.setString(2, player2);
            ps.setString(3, result);
            ps.setString(4, reason);
            ps.setInt(5, durationSec);
            ps.executeUpdate();
        }
    }

    // ---- Query methods cho HomeScreen ----

    /**
     * Lấy thông tin người chơi: [score, wins, losses, draws].
     */
    static int[] getProfile(String username) throws SQLException {
        String sql = "SELECT score, wins, losses, draws FROM users WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{
                        rs.getInt("score"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("draws")
                    };
                }
            }
        }
        return new int[]{0, 0, 0, 0};
    }

    /**
     * Lấy điểm của một người chơi.
     */
    static int getScore(String username) throws SQLException {
        String sql = "SELECT score FROM users WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("score");
                }
            }
        }
        return 0;
    }

    /**
     * Cập nhật ảnh đại diện người chơi (Base64).
     */
    static void updateAvatar(String username, String avatarData) throws SQLException {
        ensureAvatarColumn();
        String sql = "UPDATE users SET avatar = ? WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, avatarData);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    /**
     * Lấy ảnh đại diện (Base64) của người chơi.
     */
    static String getAvatar(String username) throws SQLException {
        ensureAvatarColumn();
        String sql = "SELECT avatar FROM users WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String av = rs.getString("avatar");
                    return av != null ? av : "";
                }
            }
        }
        return "";
    }

    private static volatile boolean avatarColumnChecked = false;
    private static void ensureAvatarColumn() {
        if (avatarColumnChecked) return;
        try (Connection con = getConnection()) {
            java.sql.DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "users", "avatar")) {
                if (!rs.next()) {
                    try (java.sql.Statement st = con.createStatement()) {
                        st.executeUpdate("ALTER TABLE users ADD COLUMN avatar MEDIUMTEXT NULL");
                    }
                }
            }
            avatarColumnChecked = true;
        } catch (SQLException ignored) {
            avatarColumnChecked = true;
        }
    }

    /**
     * Lấy bảng xếp hạng top N người chơi.
     * Quy tắc sắp xếp: score DESC, wins DESC, losses ASC.
     * Trả về chuỗi: name~score~wins~losses~draws;...
     */
    static String getLeaderboard(int limit) throws SQLException {
        String sql = "SELECT username, score, wins, losses, draws FROM users "
                   + "ORDER BY score DESC, wins DESC, losses ASC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(';');
                    sb.append(rs.getString("username")).append('~')
                      .append(rs.getInt("score")).append('~')
                      .append(rs.getInt("wins")).append('~')
                      .append(rs.getInt("losses")).append('~')
                      .append(rs.getInt("draws"));
                    first = false;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Lấy danh sách trận đấu gần đây của một người chơi.
     * Trả về chuỗi: opponent~result~points~duration~date;...
     */
    static String getRecentMatches(String username, int limit) throws SQLException {
        String sql = "SELECT player1, player2, result, reason, duration_sec, "
                   + "DATE_FORMAT(played_at, '%Y-%m-%d %H:%i') as played_time "
                   + "FROM matches WHERE player1 = ? OR player2 = ? "
                   + "ORDER BY played_at DESC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    String p1 = rs.getString("player1");
                    String p2 = rs.getString("player2");
                    String matchRes = rs.getString("result");
                    int duration = rs.getInt("duration_sec");
                    String date = rs.getString("played_time");
                    if (date == null) date = "";

                    boolean isP1 = username.equals(p1);
                    String opp = isP1 ? p2 : p1;
                    String res;
                    int points;
                    if ("DRAW".equals(matchRes)) {
                        res = "DRAW";
                        points = 1;
                    } else if (("P1_WIN".equals(matchRes) && isP1) || ("P2_WIN".equals(matchRes) && !isP1)) {
                        res = "WIN";
                        points = 3;
                    } else {
                        res = "LOSE";
                        points = 0;
                    }

                    if (!first) sb.append(';');
                    sb.append(opp).append('~')
                      .append(res).append('~')
                      .append(points).append('~')
                      .append(duration).append('~')
                      .append(date);
                    first = false;
                }
            }
        }
        return sb.toString();
    }

    private DB() {}
}
