package domin;

import domin.ui.HomeScreen;
import domin.ui.GameScreen;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import javax.swing.*;

/** Run with TestGameRoom compiled alongside this class. No external DB is used. */
public class TestBoardModes {
    static int checks;
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    static void handle(Server server, Server.Peer peer, String command) throws Exception {
        Method m = Server.class.getDeclaredMethod("handle", Server.Peer.class, String.class);
        m.setAccessible(true); m.invoke(server, peer, command);
    }

    // Recording JDBC boundary verifies each mode uses the correct columns and transactions.
    static final List<String> sqlLog = Collections.synchronizedList(new ArrayList<>());
    static int commits, rollbacks;
    static boolean failUpdate;
    static Object defaults(Class<?> c) {
        if (!c.isPrimitive()) return null;
        if (c == boolean.class) return false;
        if (c == int.class) return 0;
        if (c == long.class) return 0L;
        return null;
    }
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    static Connection connection() {
        return proxy(Connection.class, (p,m,a) -> {
            switch(m.getName()) {
                case "prepareStatement": {
                    String sql = (String)a[0]; sqlLog.add(sql);
                    return proxy(PreparedStatement.class, (q,n,b) -> {
                        if (n.getName().equals("executeUpdate")) {
                            if (failUpdate && sql.startsWith("UPDATE")) throw new SQLException("Injected failure");
                            return 1;
                        }
                        if (n.getName().equals("executeQuery"))
                            return proxy(ResultSet.class, (r,k,c) -> defaults(k.getReturnType()));
                        return defaults(n.getReturnType());
                    });
                }
                case "getMetaData":
                    return proxy(DatabaseMetaData.class, (q,n,b) -> {
                        if (n.getName().equals("getColumns"))
                            return proxy(ResultSet.class, (r,k,c) -> defaults(k.getReturnType()));
                        return defaults(n.getReturnType());
                    });
                case "createStatement":
                    return proxy(Statement.class, (q,n,b) -> {
                        if (n.getName().equals("executeQuery"))
                            return proxy(ResultSet.class, (r,k,c) -> defaults(k.getReturnType()));
                        return defaults(n.getReturnType());
                    });
                case "commit": commits++; break;
                case "rollback": rollbacks++; break;
            }
            return defaults(m.getReturnType());
        });
    }
    static void verifyDB() throws Exception {
        DriverManager.registerDriver(new Driver() {
            public boolean acceptsURL(String u) { return u.startsWith("jdbc:mysql:"); }
            public Connection connect(String u, Properties p) { return acceptsURL(u) ? connection() : null; }
            public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; }
            public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; }
            public Logger getParentLogger() { return Logger.getGlobal(); }
        });
        for (BoardType type : BoardType.values()) {
            sqlLog.clear();
            DB.getProfile("test", type);
            DB.getLeaderboard(10, type);
            DB.getUserRank("test", type);
            DB.recordResult("a", "b", "P1_WIN", "COMPLETE", 50, type);
            for (String sql : sqlLog) {
                if (sql.startsWith("INSERT")) continue;
                for (String col : new String[]{"score","wins","losses","draws"}) {
                    if (type != BoardType.SMALL)
                        check(!sql.matches("(?s).*\\b" + col + "\\b.*"), "No 9x9 column in " + type + ": " + sql);
                }
            }
            check(sqlLog.stream().anyMatch(q -> q.contains("score" + type.statsSuffix + " = score" + type.statsSuffix)),
                    "Stats writes use selected board " + type);
            check(sqlLog.get(0).contains("score" + type.statsSuffix), "Profile uses selected board");
        }
        check(commits == 3, "All successful results commit");
        failUpdate = true;
        try {
            DB.recordResult("a", "b", "DRAW", "TIMEOUT", 900, BoardType.LARGE);
            throw new AssertionError("Must propagate write failure");
        } catch (SQLException expected) {
            check(rollbacks == 1 && commits == 3, "Failure rolls back history and stats");
        }
        failUpdate = false;
    }

    static String labels(java.awt.Component component) {
        StringBuilder result = new StringBuilder();
        if (component instanceof JLabel) result.append(((JLabel) component).getText()).append("\n");
        if (component instanceof java.awt.Container)
            for (java.awt.Component child : ((java.awt.Container) component).getComponents())
                result.append(labels(child));
        return result.toString();
    }

    public static void main(String[] args) throws Exception {
        try {
            verifyDB();
            Server server = new Server();
            for (BoardType type : BoardType.values()) {
                Server.Board board = new Server.Board(42, type);
                int mines = 0;
                for (int r=0;r<type.rows;r++) for (int c=0;c<type.cols;c++) {
                    if (board.isMine(r,c)) mines++;
                    int adjacent = 0;
                    for (int dr=-1;dr<=1;dr++) for (int dc=-1;dc<=1;dc++)
                        if ((dr!=0 || dc!=0) && board.inRange(r+dr,c+dc) && board.isMine(r+dr,c+dc)) adjacent++;
                    check(board.adjacent(r,c)==adjacent, "Adjacent count " + type);
                }
                check(mines==type.mines, "Mine count " + type);
                check(!board.inRange(type.rows,0) && !board.inRange(0,type.cols), "Bounds " + type);
                TestGameRoom.Fake a = new TestGameRoom.Fake(server,"a"+type.id);
                TestGameRoom.Fake b = new TestGameRoom.Fake(server,"b"+type.id);
                Server.Match room = server.new Match(a,b,42,type);
                room.start();
                check(a.last("MATCH_START").endsWith("|"+type.rows+"|"+type.cols+"|"+type.mines+"|"+type.timeLimit),
                    "Match config " + type);
                server.sendSpectateInit(a,room);
                String[] init=a.last("SPECTATE_INIT").split("\\|",-1);
                check(Integer.parseInt(init[4])==type.rows && init[8].split(";").length==type.mines, "Spectator config");
                // Every safe cell can be opened, including cells beyond the old 9x9 bounds.
                for(int r=type.rows-1;r>=0;r--) for(int c=type.cols-1;c>=0;c--)
                    if (!board.isMine(r,c)) room.handleOpen(a,r,c);
                check(a.last("GAME_OVER").startsWith("GAME_OVER|WIN|COMPLETE"),"Complete "+type);
                check(room.players[0].openedSafe==type.safeTotal() && room.players[0].percent()==100,"Safe total "+type);
            }
            TestGameRoom.Fake a = new TestGameRoom.Fake(server,"queueA");
            TestGameRoom.Fake b = new TestGameRoom.Fake(server,"queueB");
            TestGameRoom.Fake c = new TestGameRoom.Fake(server,"queueC");
            server.findMatch(a,BoardType.SMALL); server.findMatch(b,BoardType.LARGE);
            check(a.match==null && b.match==null,"Different boards never match");
            server.findMatch(c,BoardType.LARGE);
            check(b.match==c.match && b.match.type==BoardType.LARGE && a.match==null,"Same board matches");
            server.removeFromQueue(a);
            TestGameRoom.Fake invited = new TestGameRoom.Fake(server,"invited");
            handle(server,invited,"CHALLENGE_ACCEPT|queueA|16x30");
            check(invited.match==null && invited.last("ERROR")!=null,"Forged acceptance rejected");
            handle(server,a,"FIND_MATCH|99x99");
            check(a.match==null && a.last("ERROR")!=null,"Unknown board rejected");
            Field active = Server.class.getDeclaredField("activePeers");
            active.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String,Server.Peer> peers = (Map<String,Server.Peer>) active.get(server);
            TestGameRoom.Fake challenger = new TestGameRoom.Fake(server,"challenger");
            peers.put(challenger.name,challenger); peers.put(invited.name,invited);
            handle(server,challenger,"CHALLENGE|invited|16x16");
            check(invited.last("CHALLENGE_RECEIVED").endsWith("|16x16"),"Invitation includes selected board");
            handle(server,invited,"CHALLENGE_ACCEPT|challenger|16x30");
            check(invited.match==null,"Cannot accept a different board than invited");
            handle(server,challenger,"CHALLENGE|invited|16x30");
            handle(server,invited,"CHALLENGE_ACCEPT|challenger|16x30");
            check(invited.match==challenger.match && invited.match.type==BoardType.LARGE,"Challenge starts selected board");
            TestGameRoom.Fake rejected = new TestGameRoom.Fake(server,"rejected");
            rejected.invitations.put("queueA",BoardType.MEDIUM);
            handle(server,rejected,"CHALLENGE_REJECT|queueA");
            check(rejected.invitations.isEmpty(),"Reject clears invitation");
            SwingUtilities.invokeAndWait(() -> {
                List<String> sent = new ArrayList<>();
                HomeScreen home = new HomeScreen("test",new HomeScreen.HomeCallback() {
                    public void send(String message) { sent.add(message); }
                    public void onLogout() {}
                });
                home.setProfile(12,4,4,0,2);
                try {
                    Field profile=HomeScreen.class.getDeclaredField("profileBoardSelector");
                    Field leader=HomeScreen.class.getDeclaredField("leaderboardBoardSelector");
                    Field badge=HomeScreen.class.getDeclaredField("rankBadgeLabel");
                    profile.setAccessible(true); leader.setAccessible(true); badge.setAccessible(true);
                    ((JComboBox<?>)profile.get(home)).setSelectedItem(BoardType.MEDIUM);
                    ((JComboBox<?>)leader.get(home)).setSelectedItem(BoardType.LARGE);
                    check(sent.contains("GET_PROFILE|16x16") && sent.contains("GET_LEADERBOARD|16x30"),"Selectors request correct mode");
                    home.setLeaderboard("test~99~33~0~0");
                    check(((JLabel)badge.get(home)).getText().equals("Top 2"),"Other leaderboard cannot overwrite profile rank");
                    Field history = HomeScreen.class.getDeclaredField("historyRowsContainer");
                    history.setAccessible(true);
                    for (BoardType type : BoardType.values()) {
                        for (boolean legacy : new boolean[]{false, true}) {
                            String date = "2026-11-08 15:40";
                            home.setHistory("opponent~LOSE~0~1~" + date + (legacy ? " · " : "~") + type.id);
                            String text = labels((java.awt.Component) history.get(home));
                            check(text.contains("Chế độ: " + type.displayName()), "History mode " + type + ", legacy=" + legacy);
                            check(!text.contains(type.id) && text.contains(date), "Date has no board suffix");
                        }
                    }
                    home.setHistory("opponent~WIN~3~20~2026-11-08 15:40");
                    check(labels((java.awt.Component) history.get(home)).contains("Chế độ: Cơ bản"),
                            "History predating board types remains basic");
                    home.addChallenge("other",10,"16x30");
                    for (BoardType type : BoardType.values())
                        new GameScreen("a","b",type.rows,type.cols,type.mines,type.timeLimit,new GameScreen.GameCallback() {
                            public void onOpenCell(int r,int c) {}
                            public void onFlagCell(int r,int c) {}
                            public void onSurrender() {}
                        });
                } catch(Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("PASS: "+checks+" board, matchmaking, JDBC transaction and Swing checks.");
            System.exit(0);
        } catch(Throwable e) { e.printStackTrace(); System.exit(1); }
    }
}
