import java.io.*; import java.net.*; import java.util.*; import java.nio.charset.StandardCharsets;

public class TestE2E {
    static int port;
    static class C {
        Socket s; PrintWriter out; BlockingQ q = new BlockingQ();
        C() throws Exception {
            s = new Socket("localhost", port);
            final BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            Thread t = new Thread(() -> { try { String l; while ((l = in.readLine()) != null) q.add(l); } catch (IOException e) {} }); t.setDaemon(true); t.start();
        }
        void send(String m) { out.println(m); }
        // chờ tin nhắn bắt đầu bằng prefix (bỏ qua tin khác), tối đa 3 giây
        String expect(String prefix) throws Exception { return q.take(prefix, 3000); }
        String expectContains(String prefix, String fragment) throws Exception { return q.takeContains(prefix, fragment, 3000); }
    }
    static class BlockingQ {
        final LinkedList<String> l = new LinkedList<>();
        synchronized void add(String s) { l.add(s); notifyAll(); }
        synchronized void discard(String prefix) { l.removeIf(s -> s.startsWith(prefix)); }
        synchronized String take(String prefix, long ms) throws Exception {
            long end = System.currentTimeMillis() + ms;
            while (true) {
                for (Iterator<String> it = l.iterator(); it.hasNext();) { String s = it.next(); if (s.startsWith(prefix)) { it.remove(); return s; } }
                long left = end - System.currentTimeMillis(); if (left <= 0) return null; wait(left);
            }
        }
        synchronized String takeContains(String prefix, String fragment, long ms) throws Exception {
            long end = System.currentTimeMillis() + ms;
            while (true) {
                for (Iterator<String> it = l.iterator(); it.hasNext();) {
                    String s = it.next();
                    if (s.startsWith(prefix) && s.contains(fragment)) { it.remove(); return s; }
                }
                long left = end - System.currentTimeMillis(); if (left <= 0) return null; wait(left);
            }
        }
    }
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "PASS  " : "FAIL  ") + what); if (!ok) fails++; }

    public static void main(String[] x) throws Exception {
        if (x.length != 1) throw new IllegalArgumentException("Cần cổng server thử riêng, ví dụ: TestE2E 5557");
        port = Integer.parseInt(x[0]);
        if (port == 5555) throw new IllegalArgumentException("Không chạy E2E trên server chơi thật ở cổng 5555");
        String suffix = Long.toString(System.nanoTime(), 36);
        String ann = "ann_" + suffix;
        String binh = "binh_" + suffix;
        C a = new C(), b = new C(), spare = new C();
        a.send("REGISTER|" + ann + "|123"); check("REGISTER_OK".equals(a.expect("REGISTER")), "Đăng ký thành công");
        a.send("REGISTER|" + ann + "|123"); check(a.expect("REGISTER").startsWith("REGISTER_FAIL"), "Đăng ký trùng tên bị từ chối");
        b.send("REGISTER|" + binh + "|123"); check("REGISTER_OK".equals(b.expect("REGISTER")), "Đăng ký người thứ hai");
        a.send("LOGIN|" + ann + "|sai"); check(a.expect("LOGIN").startsWith("LOGIN_FAIL"), "Sai mật khẩu bị từ chối");
        a.send("LOGIN|" + ann + "|123"); check(("LOGIN_OK|" + ann + "|USER").equals(a.expect("LOGIN")), "Đăng nhập đúng");
        spare.send("LOGIN|" + ann + "|123"); check(spare.expect("LOGIN").contains("nơi khác"), "Không cho đăng nhập trùng tài khoản");
        b.send("LOGIN|" + binh + "|123"); check(("LOGIN_OK|" + binh + "|USER").equals(b.expect("LOGIN")), "Đăng nhập người thứ hai");
        a.send("GET_PROFILE"); check(a.expect("PROFILE|") != null, "Hồ sơ người chơi");
        a.send("GET_ONLINE"); check(a.expect("ONLINE_LIST|").contains(binh), "Danh sách người chơi online");
        a.send("GET_LEADERBOARD"); check(a.expect("LEADERBOARD|") != null, "Bảng xếp hạng");
        a.send("INVALID_COMMAND"); check(a.expect("ERROR|") != null, "Lệnh không hợp lệ bị từ chối");
        String avatar = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+XGd0AAAAASUVORK5CYII=";
        a.send("UPDATE_AVATAR|" + avatar);
        check(a.expectContains("PROFILE|", avatar) != null, "Cập nhật và đọc lại ảnh đại diện");
        check(a.expectContains("NOTIFY|", "ảnh đại diện") != null, "Thông báo cập nhật ảnh đại diện");
        a.send("OPEN|0|0"); Thread.sleep(200);
        check(a.q.take("CELLS", 300) == null, "Chưa vào trận thì lệnh OPEN bị bỏ qua");

        a.send("FIND_MATCH"); check("WAITING".equals(a.expect("WAITING")), "Người đầu tiên nhận WAITING");
        a.send("CANCEL_MATCH"); check("CANCELED".equals(a.expect("CANCELED")), "Hủy tìm trận");
        a.send("FIND_MATCH"); check("WAITING".equals(a.expect("WAITING")), "Có thể tìm lại sau khi hủy");
        b.send("FIND_MATCH");
        String ma = a.expect("MATCH_START"), mb = b.expect("MATCH_START");
        check(("MATCH_START|" + binh + "|9|9|10|300").equals(ma) && ("MATCH_START|" + ann + "|9|9|10|300").equals(mb), "Ghép trận: cả hai nhận MATCH_START đúng đối thủ");

        // Vào trận: mở nhiều ô đến khi có CELLS, kiểm tra B không nhận CELLS của A
        a.send("OPEN|4|4");
        String cells = a.expect("CELLS");
        String resp = a.expect("PROGRESS");
        if (cells == null) { // ô (4,4) có thể là mìn -> ván có thể đã kết thúc, chấp nhận
            System.out.println("INFO  ô (4,4) là mìn ở ván này: " + a.q.take("GAME_OVER", 500));
        } else {
            check(true, "A mở ô -> nhận CELLS: " + cells.substring(0, Math.min(30, cells.length())));
            check(b.q.take("CELLS", 300) == null, "B không nhận vị trí ô A đã mở");
            String pb = b.expect("PROGRESS");
            check(pb != null && pb.split("\\|").length == 7, "B nhận PROGRESS (chỉ số liệu tổng hợp)");
            if (cells.substring(6).contains(";")) {
                check(b.expect("NOTIFY|") != null, "Thông báo realtime khi mở vùng");
            }
            Set<String> open = new HashSet<>();
            for (String it : cells.substring(6).split(";")) { String[] v = it.split(","); open.add(v[0] + "," + v[1]); }
            String fr = "0", fc = "0";
            outer: for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) if (!open.contains(r + "," + c)) { fr = "" + r; fc = "" + c; break outer; }
            a.send("FLAG|" + fr + "|" + fc); check(("FLAG|" + fr + "|" + fc + "|1").equals(a.expect("FLAG")), "Đặt cờ qua mạng");
            check(b.expect("TIME") != null, "Đồng hồ realtime tới client");
            // B bỏ cuộc
            b.send("SURRENDER");
            String ga = a.expect("GAME_OVER"), gb = b.expect("GAME_OVER");
            check(ga != null && ga.startsWith("GAME_OVER|WIN|SURRENDER"), "A thắng khi B bỏ cuộc: " + ga);
            check(gb != null && gb.startsWith("GAME_OVER|LOSE|SURRENDER"), "B thua khi bỏ cuộc");
        }

        // Ván 2: A tìm trận lại, B đóng kết nối giữa ván -> A thắng
        a.send("FIND_MATCH"); b.send("FIND_MATCH");
        check(a.expect("MATCH_START") != null && b.expect("MATCH_START") != null, "Ghép trận lần 2 (sau khi hết ván 1)");
        b.s.close();
        String g = a.expect("GAME_OVER");
        check(g != null && g.startsWith("GAME_OVER|WIN|DISCONNECT"), "Đối thủ rớt mạng giữa ván -> thắng: " + g);

        // Thách đấu, quản trị và theo dõi trận trên server thật.
        String cuong = "cuong_" + suffix;
        C c = new C(), admin = new C();
        c.send("REGISTER|" + cuong + "|123");
        check("REGISTER_OK".equals(c.expect("REGISTER")), "Đăng ký người chơi thứ ba");
        c.send("LOGIN|" + cuong + "|123");
        check(("LOGIN_OK|" + cuong + "|USER").equals(c.expect("LOGIN")), "Đăng nhập người chơi thứ ba");
        admin.send("LOGIN|admin|123456");
        check("LOGIN_OK|admin|ADMIN".equals(admin.expect("LOGIN")), "Đăng nhập quản trị viên");
        a.send("CHALLENGE|" + cuong);
        check(c.expectContains("CHALLENGE_RECEIVED|", ann) != null, "Người nhận thấy lời thách đấu");
        c.send("CHALLENGE_REJECT|" + ann);
        check(a.expectContains("NOTIFY|", "từ chối") != null, "Từ chối thách đấu báo cho người gửi");
        a.send("CHALLENGE|" + cuong);
        check(c.expectContains("CHALLENGE_RECEIVED|", ann) != null, "Có thể thách đấu lại");
        c.send("CHALLENGE_ACCEPT|" + ann);
        check(a.expect("MATCH_START|") != null && c.expect("MATCH_START|") != null, "Chấp nhận thách đấu tạo trận");
        admin.send("ADMIN_GET_MATCHES");
        String active = admin.expect("ADMIN_MATCH_LIST|");
        check(active != null && active.contains(ann) && active.contains(cuong), "Quản trị xem trận đang chơi");
        String matchId = active.split("\\|")[1].split("~")[2];
        admin.send("SPECTATE_START|" + matchId);
        String spectateInit = admin.expect("SPECTATE_INIT|");
        check(spectateInit != null, "Quản trị bắt đầu theo dõi trận");
        for (String command : new String[]{"ADMIN_GET_USERS", "ADMIN_GET_STATS", "ADMIN_GET_MATCHES", "ADMIN_GET_HISTORY"}) {
            c.send(command);
            check(c.expect("ERROR|") != null, "Người thường bị chặn: " + command);
        }
        c.send("SPECTATE_START|" + matchId);
        check(c.expect("ERROR|") != null, "Người thường bị chặn theo dõi trận đang chơi");
        c.send("ADMIN_RESET_SCORE|" + ann);
        check(c.expect("ERROR|") != null, "Người thường bị chặn thay đổi dữ liệu quản trị");
        Set<String> mines = new HashSet<>(Arrays.asList(spectateInit.split("\\|", -1)[8].split(";")));
        int safeR = -1, safeC = -1;
        outerSafe: for (int r = 0; r < 9; r++) for (int col = 0; col < 9; col++) {
            if (mines.contains(r + "," + col)) continue;
            int adjacent = 0;
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++)
                if (mines.contains((r + dr) + "," + (col + dc))) adjacent++;
            if (adjacent > 0) { safeR = r; safeC = col; break outerSafe; }
        }
        a.send("OPEN|" + safeR + "|" + safeC);
        check(a.expect("CELLS|" + safeR + "," + safeC + ",") != null, "Mở ô an toàn trong trận thách đấu");
        check(admin.expectContains("SPECTATE_OPEN|", ann) != null, "Người theo dõi thấy ô được mở");
        String[] flagCell = mines.iterator().next().split(",");
        String flagCommand = "FLAG|" + flagCell[0] + "|" + flagCell[1];
        a.send(flagCommand);
        check((flagCommand + "|1").equals(a.expect("FLAG|")), "Đặt cờ trong trận thách đấu");
        check(admin.expectContains("SPECTATE_FLAG|", ann) != null, "Người theo dõi nhận sự kiện đặt cờ");
        a.send(flagCommand); a.expect("FLAG|");
        a.send(flagCommand); a.expect("FLAG|");
        c.send("SURRENDER");
        check(a.expectContains("GAME_OVER|", "WIN|SURRENDER") != null, "Thắng trận thách đấu khi đối thủ bỏ cuộc");
        check(c.expectContains("GAME_OVER|", "LOSE|SURRENDER") != null, "Thua trận thách đấu khi bỏ cuộc");
        check(admin.expect("SPECTATE_OVER|") != null, "Người theo dõi nhận kết quả trận");

        admin.send("ADMIN_GET_STATS");
        check(admin.expect("ADMIN_STATS|") != null, "Quản trị xem thống kê");
        admin.send("ADMIN_GET_USERS");
        check(admin.expectContains("ADMIN_USER_LIST|", ann) != null, "Quản trị xem danh sách người dùng");
        admin.send("ADMIN_GET_HISTORY");
        check(admin.expectContains("ADMIN_HISTORY_LIST|", ann) != null, "Quản trị xem lịch sử trận");
        a.send("GET_HISTORY");
        check(a.expectContains("HISTORY|", cuong) != null, "Lịch sử người chơi có trận mới");
        a.q.discard("PROFILE|");
        a.send("GET_PROFILE");
        String profileAfter = a.expect("PROFILE|");
        check(profileAfter != null && Integer.parseInt(profileAfter.split("\\|")[1]) >= 6,
                "Điểm sau các trận được lưu vào database");
        a.send("LOGOUT");
        check("LOGOUT_OK".equals(a.expect("LOGOUT_OK")), "Đăng xuất");
        spare.send("LOGIN|" + ann + "|123");
        check(("LOGIN_OK|" + ann + "|USER").equals(spare.expect("LOGIN")), "Có thể đăng nhập lại sau khi đăng xuất");

        spare.send("ADMIN_GET_USERS");
        check(spare.expect("ERROR|") != null, "Người thường bị chặn xem dữ liệu quản trị");

        c.send("LOGOUT"); check("LOGOUT_OK".equals(c.expect("LOGOUT_OK")), "Người thứ ba đăng xuất");
        admin.send("ADMIN_RESET_SCORE|" + cuong);
        check(admin.expectContains("NOTIFY|", "reset điểm") != null, "Quản trị đặt lại điểm");
        admin.expect("ADMIN_USER_LIST|");
        admin.send("ADMIN_SET_ROLE|" + cuong + "|ADMIN");
        check(admin.expectContains("NOTIFY|", "vai trò") != null, "Quản trị đổi vai trò");
        check(admin.expectContains("ADMIN_USER_LIST|", cuong + "~ADMIN") != null, "Vai trò mới được lưu");
        admin.send("ADMIN_RESET_PASSWORD|" + cuong + "|new123");
        check(admin.expectContains("NOTIFY|", "mật khẩu") != null, "Quản trị đổi mật khẩu");
        admin.expect("ADMIN_USER_LIST|");
        c.send("LOGIN|" + cuong + "|123");
        check(c.expect("LOGIN_FAIL|") != null, "Mật khẩu cũ không còn dùng được");
        c.send("LOGIN|" + cuong + "|new123");
        check(("LOGIN_OK|" + cuong + "|ADMIN").equals(c.expect("LOGIN_OK|")), "Mật khẩu và vai trò mới có hiệu lực");
        c.send("LOGOUT"); c.expect("LOGOUT_OK");
        admin.send("ADMIN_DELETE_USER|" + cuong);
        check(admin.expectContains("NOTIFY|", "xóa tài khoản") != null, "Quản trị xóa người dùng");
        admin.expect("ADMIN_USER_LIST|");
        c.send("LOGIN|" + cuong + "|new123");
        check(c.expect("LOGIN_FAIL|") != null, "Tài khoản đã xóa không đăng nhập được");

        a.s.close(); c.s.close(); admin.s.close(); spare.s.close();
        System.out.println(fails == 0 ? "\nTẤT CẢ ĐỀU ĐẠT" : "\nCÓ " + fails + " CA LỖI");
        System.exit(fails);
    }
}
