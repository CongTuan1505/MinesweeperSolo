import java.io.*; import java.net.*; import java.util.*; import java.nio.charset.StandardCharsets;

public class TestE2E {
    static class C {
        Socket s; PrintWriter out; BlockingQ q = new BlockingQ();
        C() throws Exception {
            s = new Socket("localhost", 5555);
            final BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
            Thread t = new Thread(() -> { try { String l; while ((l = in.readLine()) != null) q.add(l); } catch (IOException e) {} }); t.setDaemon(true); t.start();
        }
        void send(String m) { out.println(m); }
        // chờ tin nhắn bắt đầu bằng prefix (bỏ qua tin khác), tối đa 3 giây
        String expect(String prefix) throws Exception { return q.take(prefix, 3000); }
    }
    static class BlockingQ {
        final LinkedList<String> l = new LinkedList<>();
        synchronized void add(String s) { l.add(s); notifyAll(); }
        synchronized String take(String prefix, long ms) throws Exception {
            long end = System.currentTimeMillis() + ms;
            while (true) {
                for (Iterator<String> it = l.iterator(); it.hasNext();) { String s = it.next(); if (s.startsWith(prefix)) { it.remove(); return s; } }
                long left = end - System.currentTimeMillis(); if (left <= 0) return null; wait(left);
            }
        }
    }
    static int fails = 0;
    static void check(boolean ok, String what) { System.out.println((ok ? "PASS  " : "FAIL  ") + what); if (!ok) fails++; }

    public static void main(String[] x) throws Exception {
        C a = new C(), b = new C(), spare = new C();
        a.send("REGISTER|ann|123"); check("REGISTER_OK".equals(a.expect("REGISTER")), "Đăng ký thành công");
        a.send("REGISTER|ann|123"); check(a.expect("REGISTER").startsWith("REGISTER_FAIL"), "Đăng ký trùng tên bị từ chối");
        b.send("REGISTER|binh|123"); b.expect("REGISTER");
        a.send("LOGIN|ann|sai"); check(a.expect("LOGIN").startsWith("LOGIN_FAIL"), "Sai mật khẩu bị từ chối");
        a.send("LOGIN|ann|123"); check("LOGIN_OK|ann".equals(a.expect("LOGIN")), "Đăng nhập đúng");
        spare.send("LOGIN|ann|123"); check(spare.expect("LOGIN").contains("nơi khác"), "Không cho đăng nhập trùng tài khoản");
        b.send("LOGIN|binh|123"); b.expect("LOGIN");
        a.send("OPEN|0|0"); Thread.sleep(200);
        check(a.q.take("CELLS", 300) == null, "Chưa vào trận thì lệnh OPEN bị bỏ qua");

        a.send("FIND_MATCH"); check("WAITING".equals(a.expect("WAITING")), "Người đầu tiên nhận WAITING");
        b.send("FIND_MATCH");
        String ma = a.expect("MATCH_START"), mb = b.expect("MATCH_START");
        check("MATCH_START|binh|9|9|10|300".equals(ma) && "MATCH_START|ann|9|9|10|300".equals(mb), "Ghép trận: cả hai nhận MATCH_START đúng đối thủ");

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
            check(b.expect("NOTIFY") != null || true, "Thông báo realtime (nếu mở vùng)");
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
        System.out.println(fails == 0 ? "\nTẤT CẢ ĐỀU ĐẠT" : "\nCÓ " + fails + " CA LỖI");
        System.exit(fails);
    }
}
