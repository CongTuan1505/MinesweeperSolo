package domin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Server dò mìn đối kháng (tương tự Server trong QuizDoiKhang).
 *
 * Gộp các lớp cũ: GameServer, ClientHandler, Board, GameRoom, PlayerConnection.
 * Cấu trúc inner class rõ ràng, dễ theo dõi luồng xử lý:
 *
 *   Server          → lắng nghe socket, quản lý online, ghép trận
 *     ├─ Peer       → đại diện 1 client đã kết nối (thay ClientHandler)
 *     ├─ Board      → bàn mìn (chỉ tồn tại ở server)
 *     ├─ Player     → trạng thái mỗi người trong phòng (thay PlayerState)
 *     └─ Match      → phòng đấu 2 người (thay GameRoom)
 *
 * Chạy: java domin.Server [port]
 */
public class Server {

    private final ExecutorService workers = Executors.newFixedThreadPool(100);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final LinkedList<Peer> queue = new LinkedList<>();
    private final Set<String> online = new HashSet<>();
    private final Map<String, Peer> activePeers = new ConcurrentHashMap<>();
    private final Map<String, Match> activeMatches = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger matchCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    void broadcast(String message, Peer exclude) {
        for (Peer p : activePeers.values()) {
            if (p != exclude && p.isConnected()) {
                p.send(message);
            }
        }
    }

    // ====================================================================
    //  Peer — đại diện 1 client (tương tự Peer trong QuizDoiKhang)
    // ====================================================================
    class Peer {
        final Wire wire;
        volatile String name;
        volatile Match match;
        volatile Match spectatingMatch;
        volatile boolean isAdmin = false;

        Peer(Socket socket) throws IOException {
            this.wire = new Wire(socket);
        }

        void send(String message) {
            wire.send(message);
        }

        boolean isConnected() {
            return wire.isConnected();
        }
    }

    // ====================================================================
    //  Board — bàn mìn thật, CHỈ tồn tại ở Server
    // ====================================================================
    static class Board {
        private final boolean[][] mine = new boolean[Model.ROWS][Model.COLS];
        private final int[][] adjacent = new int[Model.ROWS][Model.COLS];

        Board(long seed) {
            Random rnd = new Random(seed);
            int placed = 0;
            while (placed < Model.MINES) {
                int r = rnd.nextInt(Model.ROWS);
                int c = rnd.nextInt(Model.COLS);
                if (!mine[r][c]) {
                    mine[r][c] = true;
                    placed++;
                }
            }
            for (int r = 0; r < Model.ROWS; r++) {
                for (int c = 0; c < Model.COLS; c++) {
                    int count = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int nr = r + dr;
                            int nc = c + dc;
                            if ((dr != 0 || dc != 0) && inRange(nr, nc) && mine[nr][nc]) {
                                count++;
                            }
                        }
                    }
                    adjacent[r][c] = count;
                }
            }
        }

        boolean inRange(int r, int c) {
            return r >= 0 && r < Model.ROWS && c >= 0 && c < Model.COLS;
        }

        boolean isMine(int r, int c) { return mine[r][c]; }

        int adjacent(int r, int c) { return adjacent[r][c]; }
    }

    // ====================================================================
    //  Player — trạng thái riêng từng người chơi trong phòng
    // ====================================================================
    static class Player {
        final Peer peer;
        final boolean[][] revealed = new boolean[Model.ROWS][Model.COLS];
        /** 0 = không có, 1 = cờ, 2 = dấu hỏi */
        final int[][] flag = new int[Model.ROWS][Model.COLS];
        int openedSafe = 0;
        int flags = 0;
        String status = Model.ST_PLAYING;

        Player(Peer peer) { this.peer = peer; }

        int percent() { return openedSafe * 100 / Model.SAFE_TOTAL; }
    }

    // ====================================================================
    //  Match — phòng đấu giữa 2 người (tương tự Match trong QuizDoiKhang)
    // ====================================================================
    class Match {
        final String matchId;
        final Board board;
        final Player[] players = new Player[2];
        final Set<Peer> spectators = ConcurrentHashMap.newKeySet();
        ScheduledFuture<?> ticker;
        long startMillis;
        boolean finished = false;

        Match(Peer a, Peer b) {
            this(a, b, System.nanoTime());
        }

        /** Dùng seed cố định khi cần kiểm thử. */
        Match(Peer a, Peer b, long seed) {
            this.matchId = "M" + matchCounter.incrementAndGet();
            this.players[0] = new Player(a);
            this.players[1] = new Player(b);
            this.board = new Board(seed);
        }

        void sendToSpectators(String message) {
            for (Peer spec : spectators) {
                if (spec.isConnected()) {
                    spec.send(message);
                }
            }
        }

        // ---- Bắt đầu ván ----
        synchronized void start() {
            activeMatches.put(matchId, this);
            for (int i = 0; i < 2; i++) {
                Player me = players[i];
                Player opp = players[1 - i];
                me.peer.match = this;
                me.peer.send("MATCH_START|" + opp.peer.name + "|" + Model.ROWS + "|"
                        + Model.COLS + "|" + Model.MINES + "|" + Model.TIME_LIMIT_SEC);
            }
            startMillis = System.currentTimeMillis();
            broadcast("PLAYER_STATUS|" + players[0].peer.name + "|IN_MATCH", null);
            broadcast("PLAYER_STATUS|" + players[1].peer.name + "|IN_MATCH", null);
            broadcastAdminMatches();
            ticker = scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try { tick(); } catch (Throwable t) { t.printStackTrace(); }
                }
            }, 1, 1, TimeUnit.SECONDS);
            sendProgress();
        }

        // ---- Thao tác mở ô ----
        synchronized void handleOpen(Peer peer, int r, int c) {
            if (finished) return;
            int i = indexOf(peer);
            if (i < 0) return;
            Player me = players[i];
            Player opp = players[1 - i];
            if (!board.inRange(r, c) || !Model.ST_PLAYING.equals(me.status)) return;
            if (me.revealed[r][c] || me.flag[r][c] == 1) return;

            if (board.isMine(r, c)) {
                me.status = Model.ST_MINE;
                opp.peer.send("NOTIFY|Đối thủ đã đạp trúng mìn");
                sendToSpectators("SPECTATE_NOTIFY|" + me.peer.name + " đã đạp trúng mìn!");
                sendProgress();
                finish(1 - i, "MINE");
                return;
            }

            List<int[]> opened = floodOpen(me, r, c);
            StringBuilder sb = new StringBuilder("CELLS|");
            for (int k = 0; k < opened.size(); k++) {
                int[] cell = opened.get(k);
                if (k > 0) sb.append(';');
                sb.append(cell[0]).append(',').append(cell[1]).append(',').append(cell[2]);
            }
            me.peer.send(sb.toString());
            sendToSpectators("SPECTATE_OPEN|" + me.peer.name + "|" + sb.substring(6));

            if (opened.size() > 1) {
                opp.peer.send("NOTIFY|Đối thủ vừa mở thêm một vùng");
                sendToSpectators("SPECTATE_NOTIFY|" + me.peer.name + " vừa mở thêm một vùng " + opened.size() + " ô");
            }

            if (me.openedSafe == Model.SAFE_TOTAL) {
                me.status = Model.ST_DONE;
                opp.peer.send("NOTIFY|Đối thủ đã hoàn thành");
                sendToSpectators("SPECTATE_NOTIFY|" + me.peer.name + " đã hoàn thành toàn bộ bàn mìn!");
                sendProgress();
                finish(i, "COMPLETE");
                return;
            }
            sendProgress();
        }

        // ---- Thao tác đặt cờ ----
        synchronized void handleFlag(Peer peer, int r, int c) {
            if (finished) return;
            int i = indexOf(peer);
            if (i < 0) return;
            Player me = players[i];
            if (!board.inRange(r, c) || !Model.ST_PLAYING.equals(me.status) || me.revealed[r][c]) return;

            int old = me.flag[r][c];
            int now = (old + 1) % 3;
            me.flag[r][c] = now;
            if (old == 1) me.flags--;
            if (now == 1) me.flags++;
            me.peer.send("FLAG|" + r + "|" + c + "|" + now);
            sendToSpectators("SPECTATE_FLAG|" + me.peer.name + "|" + r + "|" + c + "|" + now);
            sendProgress();
        }

        // ---- Bỏ cuộc ----
        synchronized void handleSurrender(Peer peer) {
            if (finished) return;
            int i = indexOf(peer);
            if (i < 0) return;
            players[i].status = Model.ST_SURRENDER;
            players[1 - i].peer.send("NOTIFY|Đối thủ đã bỏ cuộc");
            sendToSpectators("SPECTATE_NOTIFY|" + players[i].peer.name + " đã đầu hàng / bỏ cuộc");
            sendProgress();
            finish(1 - i, "SURRENDER");
        }

        // ---- Mất kết nối ----
        synchronized void handleDisconnect(Peer peer) {
            if (finished) return;
            int i = indexOf(peer);
            if (i < 0) return;
            players[i].status = Model.ST_DISCONNECT;
            players[1 - i].peer.send("NOTIFY|Đối thủ bị mất kết nối");
            sendToSpectators("SPECTATE_NOTIFY|" + players[i].peer.name + " đã bị mất kết nối");
            sendProgress();
            finish(1 - i, "DISCONNECT");
        }

        // ---- Đồng hồ ----
        private synchronized void tick() {
            if (finished) return;
            int elapsed = elapsedSec();
            if (elapsed >= Model.TIME_LIMIT_SEC) {
                players[0].peer.send("NOTIFY|Hết giờ!");
                players[1].peer.send("NOTIFY|Hết giờ!");
                sendToSpectators("SPECTATE_NOTIFY|Hết thời gian thi đấu!");
                finish(-1, "TIMEOUT");
                return;
            }
            players[0].peer.send("TIME|" + elapsed);
            players[1].peer.send("TIME|" + elapsed);
            sendToSpectators("SPECTATE_TIME|" + elapsed);
        }

        int elapsedSec() {
            return (int) ((System.currentTimeMillis() - startMillis) / 1000);
        }

        // ---- Tiện ích nội bộ ----
        private int indexOf(Peer peer) {
            if (players[0].peer == peer) return 0;
            if (players[1].peer == peer) return 1;
            return -1;
        }

        private List<int[]> floodOpen(Player me, int r, int c) {
            List<int[]> result = new ArrayList<>();
            Deque<int[]> q = new ArrayDeque<>();
            me.revealed[r][c] = true;
            me.flag[r][c] = 0;
            me.openedSafe++;
            q.add(new int[]{r, c});
            while (!q.isEmpty()) {
                int[] cur = q.poll();
                int adj = board.adjacent(cur[0], cur[1]);
                result.add(new int[]{cur[0], cur[1], adj});
                if (adj != 0) continue;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = cur[0] + dr;
                        int nc = cur[1] + dc;
                        if (!board.inRange(nr, nc) || me.revealed[nr][nc] || me.flag[nr][nc] == 1
                                || board.isMine(nr, nc)) continue;
                        me.revealed[nr][nc] = true;
                        me.flag[nr][nc] = 0;
                        me.openedSafe++;
                        q.add(new int[]{nr, nc});
                    }
                }
            }
            return result;
        }

        private void sendProgress() {
            for (int i = 0; i < 2; i++) {
                Player me = players[i];
                Player opp = players[1 - i];
                me.peer.send("PROGRESS|" + me.percent() + "|" + opp.percent() + "|" + me.flags + "|"
                        + opp.flags + "|" + me.status + "|" + opp.status);
            }
            sendToSpectators("SPECTATE_PROGRESS|" + players[0].percent() + "|" + players[1].percent()
                    + "|" + players[0].flags + "|" + players[1].flags
                    + "|" + players[0].status + "|" + players[1].status);
        }

        private String minesMessage() {
            StringBuilder sb = new StringBuilder("MINES|");
            boolean first = true;
            for (int r = 0; r < Model.ROWS; r++) {
                for (int c = 0; c < Model.COLS; c++) {
                    if (board.isMine(r, c)) {
                        if (!first) sb.append(';');
                        sb.append(r).append(',').append(c);
                        first = false;
                    }
                }
            }
            return sb.toString();
        }

        /**
         * Kết thúc ván.
         * @param winner 0 hoặc 1 = người thắng, -1 = hòa
         * @param reason COMPLETE | MINE | SURRENDER | DISCONNECT | TIMEOUT
         */
        private void finish(int winner, String reason) {
            finished = true;
            activeMatches.remove(matchId);
            if (ticker != null) ticker.cancel(false);
            final int elapsed = elapsedSec();
            final String[] results = new String[2];

            for (int i = 0; i < 2; i++) {
                Player me = players[i];
                Player opp = players[1 - i];
                String res = winner < 0 ? "DRAW" : (winner == i ? "WIN" : "LOSE");
                results[i] = res;
                int points = res.equals("WIN") ? 3 : (res.equals("DRAW") ? 1 : 0);

                me.peer.send(minesMessage());
                me.peer.send("GAME_OVER|" + res + "|" + reason + "|" + elapsed + "|"
                        + me.openedSafe + "|" + opp.openedSafe + "|" + me.flags + "|"
                        + opp.flags + "|" + points);
                me.peer.match = null;
            }

            String winnerName = winner < 0 ? "DRAW" : players[winner].peer.name;
            sendToSpectators(minesMessage());
            sendToSpectators("SPECTATE_OVER|" + winnerName + "|" + reason + "|" + elapsed + "|"
                    + players[0].openedSafe + "|" + players[1].openedSafe + "|"
                    + players[0].flags + "|" + players[1].flags);
            for (Peer spec : spectators) {
                spec.spectatingMatch = null;
            }
            spectators.clear();

            broadcast("PLAYER_STATUS|" + players[0].peer.name + "|ONLINE", null);
            broadcast("PLAYER_STATUS|" + players[1].peer.name + "|ONLINE", null);
            broadcastAdminMatches();

            final String p1 = players[0].peer.name;
            final String p2 = players[1].peer.name;
            final String matchResult = winner == 0 ? "P1_WIN" : (winner == 1 ? "P2_WIN" : "DRAW");
            final String matchReason = reason;

            // Ghi database ở luồng khác để không làm chậm phòng đấu
            scheduler.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        DB.saveMatch(p1, p2, matchResult, matchReason, elapsed);
                        DB.updateStats(p1, results[0]);
                        DB.updateStats(p2, results[1]);

                        // Cập nhật ngay profile mới nhất cho 2 người chơi
                        sendProfile(activePeers.get(p1));
                        sendProfile(activePeers.get(p2));
                        broadcastOnlineList();
                    } catch (Throwable e) {
                        System.err.println("Không lưu được kết quả trận vào database: " + e);
                    }
                }
            });
        }
    }

    // ====================================================================
    //  Quản lý online / Ghép trận
    // ====================================================================

    synchronized boolean markOnline(String username) { return online.add(username); }

    synchronized void markOffline(String username) { online.remove(username); }

    void findMatch(Peer peer) {
        Peer opponent;
        synchronized (queue) {
            if (queue.contains(peer)) return;
            opponent = queue.poll();
            while (opponent != null && !opponent.isConnected()) {
                opponent = queue.poll();
            }
            if (opponent == null) {
                queue.add(peer);
                peer.send("WAITING");
                return;
            }
        }
        new Match(opponent, peer).start();
    }

    void removeFromQueue(Peer peer) {
        synchronized (queue) { queue.remove(peer); }
    }

    // ====================================================================
    //  Xử lý lệnh từ Client (tương tự handle() trong QuizDoiKhang)
    // ====================================================================

    void serve(Peer peer) {
        try {
            String line;
            while ((line = peer.wire.readLine()) != null) {
                handle(peer, line.trim());
            }
        } catch (IOException e) {
            // client ngắt kết nối
        } finally {
            cleanup(peer);
        }
    }

    private void handle(Peer peer, String line) {
        if (line.isEmpty()) return;
        String[] p = Wire.parse(line);
        try {
            switch (p[0]) {
                case "REGISTER": {
                    if (p.length != 3 || !p[1].matches("[A-Za-z0-9_]{3,20}") || p[2].length() < 3) {
                        peer.send("REGISTER_FAIL|Tên 3-20 ký tự (chữ, số, _), mật khẩu tối thiểu 3 ký tự");
                    } else if (DB.register(p[1], p[2])) {
                        peer.send("REGISTER_OK");
                    } else {
                        peer.send("REGISTER_FAIL|Tên đăng nhập đã tồn tại");
                    }
                    break;
                }
                case "LOGIN": {
                    if (p.length != 3) {
                        peer.send("LOGIN_FAIL|Thiếu thông tin đăng nhập");
                    } else if (!DB.login(p[1], p[2])) {
                        peer.send("LOGIN_FAIL|Sai tên đăng nhập hoặc mật khẩu");
                    } else if (!markOnline(p[1])) {
                        peer.send("LOGIN_FAIL|Tài khoản đang đăng nhập ở nơi khác");
                    } else {
                        peer.name = p[1];
                        peer.isAdmin = DB.isAdmin(peer.name);
                        activePeers.put(peer.name, peer);
                        peer.send("LOGIN_OK|" + peer.name + "|" + (peer.isAdmin ? "ADMIN" : "USER"));
                        int sc = 0;
                        try { sc = DB.getScore(peer.name); } catch (Exception ignored) {}
                        broadcast("PLAYER_ONLINE|" + peer.name + "|" + sc, peer);
                    }
                    break;
                }
                case "FIND_MATCH": {
                    if (peer.name != null && peer.match == null) {
                        findMatch(peer);
                    }
                    break;
                }
                case "CANCEL_MATCH": {
                    removeFromQueue(peer);
                    peer.send("CANCELED");
                    break;
                }
                case "OPEN": {
                    Match m = peer.match;
                    if (m != null) m.handleOpen(peer, Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    break;
                }
                case "FLAG": {
                    Match m = peer.match;
                    if (m != null) m.handleFlag(peer, Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    break;
                }
                case "SURRENDER": {
                    Match m = peer.match;
                    if (m != null) m.handleSurrender(peer);
                    break;
                }
                case "GET_PROFILE": {
                    if (peer.name != null) {
                        sendProfile(peer);
                    }
                    break;
                }
                case "UPDATE_AVATAR": {
                    if (peer.name != null && p.length >= 2) {
                        DB.updateAvatar(peer.name, p[1]);
                        sendProfile(peer);
                        peer.send("NOTIFY|Cập nhật ảnh đại diện thành công!");
                    }
                    break;
                }
                case "GET_ONLINE": {
                    StringBuilder sb = new StringBuilder("ONLINE_LIST|");
                    boolean first = true;
                    for (Peer pPeer : activePeers.values()) {
                        if (pPeer.name == null) continue;
                        if (!first) sb.append(';');
                        int sc = 0;
                        try { sc = DB.getScore(pPeer.name); } catch (Exception ignored) {}
                        String st = (pPeer.match != null) ? "IN_MATCH" : "ONLINE";
                        sb.append(pPeer.name).append('~').append(sc).append('~').append(st);
                        first = false;
                    }
                    peer.send(sb.toString());
                    break;
                }
                case "GET_LEADERBOARD": {
                    String lb = DB.getLeaderboard(10);
                    peer.send("LEADERBOARD|" + lb);
                    break;
                }
                case "GET_HISTORY": {
                    if (peer.name != null) {
                        String his = DB.getRecentMatches(peer.name, 10);
                        peer.send("HISTORY|" + his);
                    }
                    break;
                }
                case "CHALLENGE": {
                    if (p.length >= 2 && peer.name != null) {
                        String targetName = p[1];
                        if (targetName.equals(peer.name)) {
                            peer.send("NOTIFY|Bạn không thể tự thách đấu chính mình");
                        } else {
                            Peer target = activePeers.get(targetName);
                            if (target == null || !target.isConnected()) {
                                peer.send("NOTIFY|Người chơi " + targetName + " hiện không online");
                            } else if (target.match != null) {
                                peer.send("NOTIFY|Người chơi " + targetName + " đang trong trận đấu khác");
                            } else {
                                int challengerScore = 0;
                                try { challengerScore = DB.getScore(peer.name); } catch (Exception ignored) {}
                                target.send("CHALLENGE_RECEIVED|" + peer.name + "|" + challengerScore);
                                peer.send("NOTIFY|Đã gửi lời mời thách đấu tới " + targetName);
                            }
                        }
                    }
                    break;
                }
                case "CHALLENGE_ACCEPT": {
                    if (p.length >= 2 && peer.name != null) {
                        String fromName = p[1];
                        Peer challenger = activePeers.get(fromName);
                        if (challenger == null || !challenger.isConnected()) {
                            peer.send("NOTIFY|Người thách đấu không còn online");
                        } else if (challenger.match != null) {
                            peer.send("NOTIFY|Người thách đấu đang trong trận khác");
                        } else if (peer.match != null) {
                            peer.send("NOTIFY|Bạn đang trong trận đấu khác");
                        } else {
                            removeFromQueue(challenger);
                            removeFromQueue(peer);
                            new Match(challenger, peer).start();
                        }
                    }
                    break;
                }
                case "CHALLENGE_REJECT": {
                    if (p.length >= 2 && peer.name != null) {
                        String fromName = p[1];
                        Peer challenger = activePeers.get(fromName);
                        if (challenger != null && challenger.isConnected()) {
                            challenger.send("NOTIFY|" + peer.name + " đã từ chối lời thách đấu");
                        }
                    }
                    break;
                }
                case "LOGOUT": {
                    if (peer.spectatingMatch != null) {
                        peer.spectatingMatch.spectators.remove(peer);
                        peer.spectatingMatch = null;
                    }
                    removeFromQueue(peer);
                    Match m = peer.match;
                    if (m != null) m.handleDisconnect(peer);
                    String oldName = peer.name;
                    if (oldName != null) {
                        activePeers.remove(oldName);
                        markOffline(oldName);
                        peer.name = null;
                        peer.isAdmin = false;
                        broadcast("PLAYER_OFFLINE|" + oldName, peer);
                    }
                    peer.send("LOGOUT_OK");
                    break;
                }
                case "ADMIN_GET_MATCHES": {
                    peer.send(buildAdminMatchListMessage());
                    break;
                }
                case "ADMIN_GET_STATS": {
                    int onlineCount = activePeers.size();
                    int matchesCount = activeMatches.size();
                    int totalUsers = DB.getTotalUsers();
                    int totalMatches = DB.getTotalMatches();
                    peer.send("ADMIN_STATS|" + onlineCount + "|" + matchesCount + "|" + totalUsers + "|" + totalMatches);
                    break;
                }
                case "ADMIN_GET_USERS": {
                    String users = DB.getAllUsersForAdmin(50);
                    peer.send("ADMIN_USER_LIST|" + users);
                    break;
                }
                case "ADMIN_RESET_PASSWORD": {
                    if (peer.isAdmin && p.length >= 3) {
                        String targetUser = p[1];
                        String newPass = p[2];
                        boolean ok = DB.adminResetPassword(targetUser, newPass);
                        peer.send(ok ? "NOTIFY|Đã đổi mật khẩu cho người dùng " + targetUser : "ERROR|Không đổi được mật khẩu");
                        peer.send("ADMIN_USER_LIST|" + DB.getAllUsersForAdmin(50));
                    }
                    break;
                }
                case "ADMIN_DELETE_USER": {
                    if (peer.isAdmin && p.length >= 2) {
                        String targetUser = p[1];
                        boolean ok = DB.adminDeleteUser(targetUser);
                        peer.send(ok ? "NOTIFY|Đã xóa tài khoản " + targetUser : "ERROR|Không thể xóa tài khoản này");
                        peer.send("ADMIN_USER_LIST|" + DB.getAllUsersForAdmin(50));
                    }
                    break;
                }
                case "ADMIN_RESET_SCORE": {
                    if (peer.isAdmin && p.length >= 2) {
                        String targetUser = p[1];
                        boolean ok = DB.adminResetScore(targetUser);
                        peer.send(ok ? "NOTIFY|Đã reset điểm cho " + targetUser : "ERROR|Không thể reset điểm");
                        peer.send("ADMIN_USER_LIST|" + DB.getAllUsersForAdmin(50));
                    }
                    break;
                }
                case "ADMIN_SET_ROLE": {
                    if (peer.isAdmin && p.length >= 3) {
                        String targetUser = p[1];
                        String role = p[2];
                        boolean ok = DB.adminSetRole(targetUser, role);
                        peer.send(ok ? "NOTIFY|Đã cập nhật vai trò " + role + " cho " + targetUser : "ERROR|Không thể đổi vai trò");
                        peer.send("ADMIN_USER_LIST|" + DB.getAllUsersForAdmin(50));
                    }
                    break;
                }
                case "ADMIN_GET_HISTORY": {
                    String history = DB.getAllMatchesForAdmin(50);
                    peer.send("ADMIN_HISTORY_LIST|" + history);
                    break;
                }
                case "SPECTATE_START": {
                    String targetId = p.length > 1 ? p[1] : "";
                    Match targetMatch = null;
                    if (activeMatches.containsKey(targetId)) {
                        targetMatch = activeMatches.get(targetId);
                    } else if (p.length >= 3) {
                        String p1 = p[1];
                        String p2 = p[2];
                        for (Match m : activeMatches.values()) {
                            if ((m.players[0].peer.name.equalsIgnoreCase(p1) && m.players[1].peer.name.equalsIgnoreCase(p2)) ||
                                (m.players[0].peer.name.equalsIgnoreCase(p2) && m.players[1].peer.name.equalsIgnoreCase(p1))) {
                                targetMatch = m;
                                break;
                            }
                        }
                    }
                    if (targetMatch == null || targetMatch.finished) {
                        peer.send("ERROR|Trận đấu đã kết thúc hoặc không còn tồn tại");
                    } else {
                        if (peer.spectatingMatch != null) {
                            peer.spectatingMatch.spectators.remove(peer);
                        }
                        peer.spectatingMatch = targetMatch;
                        targetMatch.spectators.add(peer);
                        sendSpectateInit(peer, targetMatch);
                    }
                    break;
                }
                case "SPECTATE_STOP": {
                    if (peer.spectatingMatch != null) {
                        peer.spectatingMatch.spectators.remove(peer);
                        peer.spectatingMatch = null;
                    }
                    peer.send("SPECTATE_STOPPED");
                    break;
                }
                default:
                    peer.send("ERROR|Lệnh không hợp lệ");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            peer.send("ERROR|Lệnh không hợp lệ");
        } catch (SQLException e) {
            System.err.println("Lỗi database: " + e);
            peer.send("ERROR|Lỗi cơ sở dữ liệu, kiểm tra MySQL đã chạy chưa");
        }
    }

    private void cleanup(Peer peer) {
        if (peer.spectatingMatch != null) {
            peer.spectatingMatch.spectators.remove(peer);
            peer.spectatingMatch = null;
        }
        removeFromQueue(peer);
        Match m = peer.match;
        if (m != null) m.handleDisconnect(peer);
        String name = peer.name;
        if (name != null) {
            activePeers.remove(name);
            markOffline(name);
            peer.name = null;
            peer.isAdmin = false;
            broadcast("PLAYER_OFFLINE|" + name, peer);
        }
        peer.wire.close();
    }

    // ====================================================================
    //  Admin & Spectate Utilities
    // ====================================================================

    void broadcastAdminMatches() {
        String msg = buildAdminMatchListMessage();
        for (Peer p : activePeers.values()) {
            if (p.isAdmin && p.isConnected()) {
                p.send(msg);
            }
        }
    }

    String buildAdminMatchListMessage() {
        StringBuilder sb = new StringBuilder("ADMIN_MATCH_LIST|");
        boolean first = true;
        for (Match m : activeMatches.values()) {
            if (m.finished) continue;
            if (!first) sb.append(';');
            sb.append(m.players[0].peer.name).append('~')
              .append(m.players[1].peer.name).append('~')
              .append(m.matchId).append('~')
              .append(m.elapsedSec()).append('~')
              .append(m.players[0].percent()).append('~')
              .append(m.players[1].percent()).append('~')
              .append(m.players[0].flags).append('~')
              .append(m.players[1].flags).append('~')
              .append(m.players[0].status).append('~')
              .append(m.players[1].status);
            first = false;
        }
        return sb.toString();
    }

    void sendSpectateInit(Peer peer, Match m) {
        StringBuilder sb = new StringBuilder("SPECTATE_INIT|");
        sb.append(m.matchId).append('|')
          .append(m.players[0].peer.name).append('|')
          .append(m.players[1].peer.name).append('|')
          .append(Model.ROWS).append('|')
          .append(Model.COLS).append('|')
          .append(Model.MINES).append('|')
          .append(m.elapsedSec()).append('|');

        // mineLocations: r,c;r,c;...
        boolean firstMine = true;
        for (int r = 0; r < Model.ROWS; r++) {
            for (int c = 0; c < Model.COLS; c++) {
                if (m.board.isMine(r, c)) {
                    if (!firstMine) sb.append(';');
                    sb.append(r).append(',').append(c);
                    firstMine = false;
                }
            }
        }
        sb.append('|');

        // p1Revealed: r,c,adj;...
        boolean firstP1Rev = true;
        for (int r = 0; r < Model.ROWS; r++) {
            for (int c = 0; c < Model.COLS; c++) {
                if (m.players[0].revealed[r][c]) {
                    if (!firstP1Rev) sb.append(';');
                    sb.append(r).append(',').append(c).append(',').append(m.board.adjacent(r, c));
                    firstP1Rev = false;
                }
            }
        }
        sb.append('|');

        // p1Flags: r,c,flag;...
        boolean firstP1Flag = true;
        for (int r = 0; r < Model.ROWS; r++) {
            for (int c = 0; c < Model.COLS; c++) {
                if (m.players[0].flag[r][c] != 0) {
                    if (!firstP1Flag) sb.append(';');
                    sb.append(r).append(',').append(c).append(',').append(m.players[0].flag[r][c]);
                    firstP1Flag = false;
                }
            }
        }
        sb.append('|');

        // p2Revealed: r,c,adj;...
        boolean firstP2Rev = true;
        for (int r = 0; r < Model.ROWS; r++) {
            for (int c = 0; c < Model.COLS; c++) {
                if (m.players[1].revealed[r][c]) {
                    if (!firstP2Rev) sb.append(';');
                    sb.append(r).append(',').append(c).append(',').append(m.board.adjacent(r, c));
                    firstP2Rev = false;
                }
            }
        }
        sb.append('|');

        // p2Flags: r,c,flag;...
        boolean firstP2Flag = true;
        for (int r = 0; r < Model.ROWS; r++) {
            for (int c = 0; c < Model.COLS; c++) {
                if (m.players[1].flag[r][c] != 0) {
                    if (!firstP2Flag) sb.append(';');
                    sb.append(r).append(',').append(c).append(',').append(m.players[1].flag[r][c]);
                    firstP2Flag = false;
                }
            }
        }
        sb.append('|');

        // stats: p1Percent, p2Percent, p1Flags, p2Flags, p1Status, p2Status
        sb.append(m.players[0].percent()).append('|')
          .append(m.players[1].percent()).append('|')
          .append(m.players[0].flags).append('|')
          .append(m.players[1].flags).append('|')
          .append(m.players[0].status).append('|')
          .append(m.players[1].status);

        peer.send(sb.toString());
    }

    // ====================================================================
    //  In IP LAN
    // ====================================================================
    private static void printLanAddresses() {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) {
                        System.out.println("  Địa chỉ IP khả dụng: " + addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    void sendProfile(Peer peer) {
        if (peer != null && peer.isConnected() && peer.name != null) {
            try {
                int[] prof = DB.getProfile(peer.name);
                String av = DB.getAvatar(peer.name);
                peer.send("PROFILE|" + prof[0] + "|" + prof[1] + "|" + prof[2] + "|" + prof[3] + "|" + av);
            } catch (Exception ignored) {}
        }
    }

    void broadcastOnlineList() {
        StringBuilder sb = new StringBuilder("ONLINE_LIST|");
        boolean first = true;
        for (Peer pPeer : activePeers.values()) {
            if (pPeer.name == null) continue;
            if (!first) sb.append(';');
            int sc = 0;
            try { sc = DB.getScore(pPeer.name); } catch (Exception ignored) {}
            String st = (pPeer.match != null) ? "IN_MATCH" : "ONLINE";
            sb.append(pPeer.name).append('~').append(sc).append('~').append(st);
            first = false;
        }
        broadcast(sb.toString(), null);
    }

    // ====================================================================
    //  main — Chạy: java domin.Server [port]
    // ====================================================================
    public static void main(String[] args) throws IOException {
        DB.ensureAvatarColumn();
        DB.ensureRoleColumn();
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Model.PORT;
        Server app = new Server();
        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.println("Server dò mìn đang chạy ở cổng " + port + " ...");
            printLanAddresses();
            System.out.println("Người chơi ở máy khác kết nối bằng một trong các địa chỉ IP trên "
                    + "(cùng mạng LAN/Wi-Fi) và cổng " + port + ".");
            while (true) {
                Socket s = socket.accept();
                System.out.println("Client mới: " + s.getRemoteSocketAddress());
                final Peer peer = app.new Peer(s);
                app.workers.execute(new Runnable() {
                    @Override
                    public void run() { app.serve(peer); }
                });
            }
        }
    }
}
