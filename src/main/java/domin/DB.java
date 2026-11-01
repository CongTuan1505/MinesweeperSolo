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
                if (rs.next()) return true;
            }
            // Hỗ trợ linh hoạt cho tài khoản admin: chấp nhận cả 123456 và admin123
            if ("admin".equalsIgnoreCase(username) && ("123456".equals(password) || "admin123".equals(password))) {
                try (PreparedStatement ps2 = con.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
                    ps2.setString(1, username);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            // Cập nhật lại hash mật khẩu theo mật khẩu người dùng vừa nhập
                            try (PreparedStatement psUp = con.prepareStatement("UPDATE users SET password_hash = ? WHERE username = ?")) {
                                psUp.setString(1, hash(password));
                                psUp.setString(2, username);
                                psUp.executeUpdate();
                            }
                            return true;
                        } else {
                            // Nếu trong DB chưa có tài khoản admin thì tự động khởi tạo luôn
                            try (PreparedStatement psIns = con.prepareStatement(
                                    "INSERT INTO users(username, password_hash, role, score, wins, losses, draws) VALUES('admin', ?, 'ADMIN', 100, 30, 0, 0)")) {
                                psIns.setString(1, hash(password));
                                psIns.executeUpdate();
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
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
    static String getAvatar(String username) {
        ensureAvatarColumn();
        String sql = "SELECT avatar FROM users WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String av = rs.getString("avatar");
                    if (av != null && !av.isEmpty() && !av.equals("default_avatar.png") && av.length() > 50) {
                        return av;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static volatile boolean avatarColumnChecked = false;
    public static synchronized void ensureAvatarColumn() {
        if (avatarColumnChecked) return;
        try (Connection con = getConnection()) {
            boolean exists = false;
            java.sql.DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "users", "avatar")) {
                if (rs.next()) {
                    exists = true;
                }
            }
            try (java.sql.Statement st = con.createStatement()) {
                if (exists) {
                    st.executeUpdate("ALTER TABLE users MODIFY COLUMN avatar MEDIUMTEXT NULL");
                } else {
                    st.executeUpdate("ALTER TABLE users ADD COLUMN avatar MEDIUMTEXT NULL");
                }
            }
            avatarColumnChecked = true;
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra/nâng cấp cột avatar: " + e.getMessage());
        }
    }

    private static volatile boolean roleColumnChecked = false;
    public static synchronized void ensureRoleColumn() {
        if (roleColumnChecked) return;
        try (Connection con = getConnection();
             java.sql.Statement st = con.createStatement()) {
            boolean exists = false;
            try (ResultSet rs = st.executeQuery("SHOW COLUMNS FROM users LIKE 'role'")) {
                if (rs.next()) {
                    exists = true;
                }
            }
            if (!exists) {
                try {
                    st.executeUpdate("ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'");
                    System.out.println("Đã thêm cột role vào bảng users.");
                } catch (SQLException ignore) {}
            }
            // Đảm bảo tài khoản admin tồn tại và có quyền ADMIN, điểm = 0
            st.executeUpdate("UPDATE users SET role = 'ADMIN', score = 0, wins = 0, losses = 0, draws = 0 WHERE LOWER(username) = 'admin'");
            
            // Nếu chưa có tài khoản admin nào thì tự tạo tài khoản admin / 123456 (điểm = 0)
            try (ResultSet rsCheck = st.executeQuery("SELECT 1 FROM users WHERE LOWER(username) = 'admin'")) {
                if (!rsCheck.next()) {
                    String hashPass = hash("123456");
                    st.executeUpdate("INSERT INTO users(username, password_hash, role, score, wins, losses, draws) "
                            + "VALUES('admin', '" + hashPass + "', 'ADMIN', 0, 0, 0, 0)");
                    System.out.println("Đã khởi tạo tài khoản quản trị viên mặc định: admin / 123456");
                }
            }
            roleColumnChecked = true;
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra/nâng cấp cột role: " + e.getMessage());
        }
    }

    /** Kiểm tra xem tài khoản có quyền Quản trị viên hay không. */
    public static boolean isAdmin(String username) {
        if (username == null) return false;
        if ("admin".equalsIgnoreCase(username.trim())) return true;
        ensureRoleColumn();
        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "ADMIN".equalsIgnoreCase(role);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static int getTotalUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getTotalMatches() {
        String sql = "SELECT COUNT(*) FROM matches";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    public static String getAllUsersForAdmin(int limit) {
        ensureRoleColumn();
        String sql = "SELECT username, role, score, wins, losses, draws, "
                   + "DATE_FORMAT(created_at, '%Y-%m-%d') as reg_date FROM users "
                   + "ORDER BY CASE WHEN LOWER(role) = 'admin' THEN 0 ELSE 1 END, score DESC, wins DESC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(';');
                    sb.append(rs.getString("username")).append('~')
                      .append(rs.getString("role")).append('~')
                      .append(rs.getInt("score")).append('~')
                      .append(rs.getInt("wins")).append('~')
                      .append(rs.getInt("losses")).append('~')
                      .append(rs.getInt("draws")).append('~')
                      .append(rs.getString("reg_date") != null ? rs.getString("reg_date") : "");
                    first = false;
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    public static boolean adminResetPassword(String username, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hash(newPassword));
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean adminDeleteUser(String username) throws SQLException {
        if ("admin".equalsIgnoreCase(username)) return false;
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean adminResetScore(String username) throws SQLException {
        String sql = "UPDATE users SET score = 0, wins = 0, losses = 0, draws = 0 WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean adminSetRole(String username, String role) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.toUpperCase());
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        }
    }

    public static String getAllMatchesForAdmin(int limit) {
        String sql = "SELECT id, player1, player2, result, reason, duration_sec, "
                   + "DATE_FORMAT(played_at, '%Y-%m-%d %H:%i') as played_time "
                   + "FROM matches ORDER BY played_at DESC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(';');
                    sb.append(rs.getInt("id")).append('~')
                      .append(rs.getString("player1")).append('~')
                      .append(rs.getString("player2")).append('~')
                      .append(rs.getString("result")).append('~')
                      .append(rs.getString("reason")).append('~')
                      .append(rs.getInt("duration_sec")).append('~')
                      .append(rs.getString("played_time") != null ? rs.getString("played_time") : "");
                    first = false;
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println("Testing DB connection, ensureAvatarColumn() and ensureRoleColumn()...");
        ensureAvatarColumn();
        ensureRoleColumn();
        System.out.println("Testing isAdmin('admin'): " + isAdmin("admin"));
        System.out.println("Total users: " + getTotalUsers() + ", Total matches: " + getTotalMatches());
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
