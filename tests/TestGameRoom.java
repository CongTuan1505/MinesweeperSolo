package domin;

import java.util.*;
import java.util.concurrent.*;

public class TestGameRoom {
    static class Fake extends Server.Peer {
        final String fakeName;
        final List<String> log = Collections.synchronizedList(new ArrayList<String>());

        Fake(Server server, String n) throws Exception {
            // Tạo peer giả (không cần socket thật)
            server.super(createDummySocket());
            fakeName = n;
            name = n;
        }

        private static java.net.Socket createDummySocket() throws Exception {
            // Tạo socket dummy dùng ServerSocket nội bộ
            java.net.ServerSocket ss = new java.net.ServerSocket(0);
            java.net.Socket s = new java.net.Socket("localhost", ss.getLocalPort());
            ss.accept().close();
            ss.close();
            return s;
        }

        @Override
        public void send(String m) { log.add(m); }

        String last(String prefix) {
            synchronized (log) {
                for (int i = log.size()-1; i>=0; i--)
                    if (log.get(i).startsWith(prefix)) return log.get(i);
            }
            return null;
        }

        int count(String prefix) {
            int n = 0;
            synchronized (log) {
                for (String s: log) if (s.startsWith(prefix)) n++;
            }
            return n;
        }
    }

    static int fails = 0;
    static void check(boolean ok, String what) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) fails++;
    }

    public static void main(String[] a) throws Exception {
        Server server = new Server();
        ScheduledExecutorService sch = Executors.newScheduledThreadPool(2);
        long seed = 42;
        Server.Board b = new Server.Board(seed);
        int mr=-1, mc=-1, sr=-1, sc=-1;
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) {
            if (b.isMine(r,c) && mr<0) { mr=r; mc=c; }
            if (!b.isMine(r,c) && b.adjacent(r,c)>0 && sr<0) { sr=r; sc=c; }
        }
        int mines=0; for (int r=0;r<9;r++) for (int c=0;c<9;c++) if (b.isMine(r,c)) mines++;
        check(mines == 10, "Bàn có đúng 10 mìn");

        // --- Ca 1: mở ô an toàn, cờ 3 trạng thái, hai người cùng bàn ---
        Fake A = new Fake(server, "An"), B = new Fake(server, "Binh");
        Server.Match room = server.new Match(A, B, seed);
        room.start();
        check(A.last("MATCH_START").equals("MATCH_START|Binh|9|9|10|300"), "MATCH_START gửi đúng tên đối thủ + cấu hình");
        room.handleOpen(A, sr, sc);
        String cells = A.last("CELLS");
        check(cells != null && cells.startsWith("CELLS|" + sr + "," + sc + "," + b.adjacent(sr,sc)), "Mở ô số: server trả về đúng số mìn lân cận");
        check(B.count("CELLS") == 0, "Đối thủ KHÔNG nhận vị trí ô người kia mở");
        String prog = B.last("PROGRESS");
        check(prog != null && !prog.contains(sr + "," + sc), "PROGRESS gửi cho đối thủ chỉ có số liệu tổng hợp");
        room.handleFlag(A, 0, 0); room.handleFlag(A, 0, 0); room.handleFlag(A, 0, 0);
        check(A.last("FLAG").equals("FLAG|0|0|0"), "Cờ 3 trạng thái: cờ -> dấu hỏi -> bỏ");
        room.handleFlag(A, mr, mc);
        room.handleOpen(A, mr, mc);
        check(A.count("GAME_OVER") == 0, "Ô đang cắm cờ thì không mở được (không nổ mìn oan)");
        check(A.last("PROGRESS").split("\\|")[3].equals("1"), "Đếm đúng số cờ đã đặt");

        // --- Ca 2: trúng mìn -> người trúng thua, người kia thắng ---
        Fake C = new Fake(server, "Cuong"), D = new Fake(server, "Dung");
        Server.Match r2 = server.new Match(C, D, seed); r2.start();
        r2.handleOpen(C, mr, mc);
        check(C.last("GAME_OVER").startsWith("GAME_OVER|LOSE|MINE"), "Trúng mìn: người trúng THUA");
        check(D.last("GAME_OVER").startsWith("GAME_OVER|WIN|MINE"), "Trúng mìn: đối thủ THẮNG");
        check(C.last("MINES") != null && C.last("MINES").split(";").length == 10, "Hết ván: server mới gửi vị trí 10 mìn");
        check(C.match == null && D.match == null, "Hết ván: người chơi được thoát khỏi phòng");
        r2.handleOpen(D, sr, sc);
        check(D.count("CELLS") == 0, "Sau khi hết ván, thao tác bị bỏ qua");

        // --- Ca 3: bỏ cuộc ---
        Fake E = new Fake(server, "Em"), F = new Fake(server, "Phuc");
        Server.Match r3 = server.new Match(E, F, seed); r3.start();
        r3.handleSurrender(E);
        check(E.last("GAME_OVER").startsWith("GAME_OVER|LOSE|SURRENDER"), "Bỏ cuộc: người bỏ THUA");
        check(F.last("GAME_OVER").startsWith("GAME_OVER|WIN|SURRENDER"), "Bỏ cuộc: đối thủ THẮNG");
        check(F.last("NOTIFY").contains("bỏ cuộc"), "Đối thủ nhận thông báo realtime 'bỏ cuộc'");

        // --- Mất kết nối ---
        Fake G = new Fake(server, "Gia"), H = new Fake(server, "Hai");
        Server.Match r3b = server.new Match(G, H, seed); r3b.start();
        r3b.handleDisconnect(G);
        check(H.last("GAME_OVER").startsWith("GAME_OVER|WIN|DISCONNECT"), "Mất kết nối: đối thủ THẮNG");
        check(H.last("PROGRESS").split("\\|")[6].equals("Mất kết nối"), "Trạng thái đối thủ hiện 'Mất kết nối'");

        // --- Ca 4: hoàn thành trước -> thắng ---
        Fake I = new Fake(server, "Ich"), K = new Fake(server, "Kha");
        Server.Match r4 = server.new Match(I, K, seed); r4.start();
        for (int r=0;r<9;r++) for (int c=0;c<9;c++) if (!b.isMine(r,c)) r4.handleOpen(I, r, c);
        check(I.last("GAME_OVER").startsWith("GAME_OVER|WIN|COMPLETE"), "Hoàn thành 100% trước: THẮNG");
        check(K.last("GAME_OVER").startsWith("GAME_OVER|LOSE|COMPLETE"), "Người chậm hơn: THUA");
        check(I.last("GAME_OVER").split("\\|")[4].equals("71"), "Đếm đủ 71 ô an toàn (81 - 10 mìn)");
        check(I.last("GAME_OVER").endsWith("|3") && K.last("GAME_OVER").endsWith("|0"), "Điểm: thắng +3, thua +0");
        check(K.last("NOTIFY").contains("hoàn thành"), "Đối thủ nhận thông báo 'đã hoàn thành'");

        // --- Flood fill: tổng ô mở qua CELLS phải bằng openedSafe ---
        int total = 0; synchronized (I.log) { for (String s: I.log) if (s.startsWith("CELLS|")) total += s.substring(6).split(";").length; }
        check(total == 71, "Flood fill không mở trùng ô, tổng đúng 71");

        // --- Ca 5: hòa khi hết giờ ---
        Fake L = new Fake(server, "Lan"), M = new Fake(server, "Minh");
        Server.Match r5 = server.new Match(L, M, seed); r5.start();
        Thread.sleep(2300);
        check(L.count("TIME|") >= 2, "Đồng hồ gửi TIME mỗi giây (nhận " + L.count("TIME|") + " lần / 2.3s)");
        java.lang.reflect.Field f = Server.Match.class.getDeclaredField("startMillis");
        f.setAccessible(true); f.setLong(r5, System.currentTimeMillis() - 301000L);
        Thread.sleep(1300);
        check(L.last("GAME_OVER") != null && L.last("GAME_OVER").startsWith("GAME_OVER|DRAW|TIMEOUT"), "Hết giờ chưa ai xong: HÒA");
        check(L.last("GAME_OVER").endsWith("|1") && M.last("GAME_OVER").endsWith("|1"), "Hòa: mỗi người +1 điểm");

        sch.shutdownNow();
        System.out.println(fails == 0 ? "\nTẤT CẢ ĐỀU ĐẠT" : "\nCÓ " + fails + " CA LỖI");
        System.exit(fails);
    }
}
