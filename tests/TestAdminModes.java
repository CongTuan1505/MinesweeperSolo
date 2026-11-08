package domin;

import domin.ui.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class TestAdminModes {
    static int checks;
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); checks++; }
    static Object field(Object o,String name) throws Exception {
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    static String lastSql() { return TestBoardModes.sqlLog.get(TestBoardModes.sqlLog.size()-1); }
    public static void main(String[] args) throws Exception {
        try {
            TestBoardModes.verifyDB(); // Install the recording fake JDBC driver; no real database.
            for(BoardScope scope:BoardScope.values()) {
                TestBoardModes.sqlLog.clear();
                DB.getAllUsersForAdmin(50,scope);
                String query=lastSql();
                for(BoardType type:BoardType.values()) {
                    boolean present = type == BoardType.SMALL
                            ? query.contains("(score)") || query.contains("(score + ")
                            : query.contains("score"+type.statsSuffix);
                    check(present==scope.matches(type),"Player stats scope: "+scope+" / "+type);
                }
                DB.getAllMatchesForAdmin(50,scope);
                query=lastSql();
                check(scope.type==null ? !query.contains(" WHERE ") : query.contains("WHERE board_type = '"+scope.id()+"'"),
                        "History scope is applied before LIMIT: "+scope);
                DB.getTotalMatches(scope);
                query=lastSql();
                check(scope.type==null ? !query.contains(" WHERE ") : query.contains("WHERE board_type = '"+scope.id()+"'"),"KPI scope");
                DB.adminResetScore("target",scope);
                query=lastSql();
                for(BoardType type:BoardType.values())
                    check(query.contains("score"+type.statsSuffix+" = 0")==scope.matches(type),"Reset affects only chosen mode");
            }
            Server server=new Server();
            TestGameRoom.Fake admin=new TestGameRoom.Fake(server,"admin");
            admin.isAdmin=true;
            @SuppressWarnings("unchecked")
            Map<String,Server.Peer> peers=(Map<String,Server.Peer>)field(server,"activePeers");
            peers.put(admin.name,admin);
            TestGameRoom.Fake normal=new TestGameRoom.Fake(server,"normal");
            peers.put(normal.name,normal);
            for(String cmd:new String[]{"ADMIN_GET_STATS","ADMIN_GET_USERS","ADMIN_GET_HISTORY","ADMIN_GET_MATCHES",
                    "ADMIN_RESET_SCORE|target|ALL","SPECTATE_START|M1","SPECTATE_STOP"}) {
                int sqlCount=TestBoardModes.sqlLog.size();
                TestBoardModes.handle(server,normal,cmd);
                check(normal.last("ERROR").contains("quản trị"),"Non-admin rejected: "+cmd);
                check(sqlCount==TestBoardModes.sqlLog.size(),"No privileged DB access: "+cmd);
            }
            for(BoardType type:BoardType.values()) {
                TestGameRoom.Fake a=new TestGameRoom.Fake(server,"a"+type.id);
                TestGameRoom.Fake b=new TestGameRoom.Fake(server,"b"+type.id);
                peers.put(a.name,a);peers.put(b.name,b);
                server.new Match(a,b,42,type).start();
            }
            for(BoardScope scope:BoardScope.values()) {
                TestBoardModes.handle(server,admin,"ADMIN_GET_MATCHES|"+scope.id());
                String[] msg=admin.last("ADMIN_MATCH_LIST").split("\\|",-1);
                // Java source uses a literal escaped pipe regex.
                check(msg[2].equals(scope.id()),"Live response scope tag");
                check(msg[1].split(";").length==(scope==BoardScope.ALL?3:1),"Live filtered count");
                if(scope.type!=null) check(msg[1].endsWith("~"+scope.id()),"Live mode metadata");
                TestBoardModes.handle(server,admin,"ADMIN_GET_STATS|"+scope.id());
                String[] stats=admin.last("ADMIN_STATS").split("\\|",-1);
                check(Integer.parseInt(stats[2])==(scope==BoardScope.ALL?3:1),"Scoped live KPI");
                TestBoardModes.handle(server,admin,"ADMIN_GET_USERS|"+scope.id());
                check(admin.last("ADMIN_USER_LIST").endsWith("|"+scope.id()),"Users response tag");
                TestBoardModes.handle(server,admin,"ADMIN_GET_HISTORY|"+scope.id());
                check(admin.last("ADMIN_HISTORY_LIST").endsWith("|"+scope.id()),"History response tag");
            }
            int before=TestBoardModes.sqlLog.size();
            TestBoardModes.handle(server,admin,"ADMIN_RESET_SCORE|a16x16|16x16");
            check(admin.last("ERROR").contains("đang trong trận"),"Reset during play rejected");
            check(before==TestBoardModes.sqlLog.size(),"No database reset during play");
            TestBoardModes.handle(server,admin,"ADMIN_RESET_SCORE|normal|16x16");
            check(TestBoardModes.sqlLog.stream().anyMatch(q -> q.startsWith("UPDATE users SET score_16x16 = 0")),
                    "Server routes scoped reset");
            TestBoardModes.handle(server,admin,"ADMIN_GET_USERS|bogus");
            check(admin.last("ERROR")!=null,"Invalid mode rejected");

            SwingUtilities.invokeAndWait(() -> {
                List<String> sent=new ArrayList<>();
                AdminScreen ui=new AdminScreen("admin",new AdminScreen.AdminCallback() {
                    public void send(String m){sent.add(m);}
                    public void onSpectate(String id,String p1,String p2){}
                    public void onSwitchToPlayerHome(){}
                    public void onLogout(){}
                });
                ui.stopTimer();
                try {
                    check(sent.contains("ADMIN_GET_USERS|ALL"),"Admin defaults to aggregate");
                    ui.selectScope("16x16");
                    for(String cmd:new String[]{"ADMIN_GET_USERS","ADMIN_GET_STATS","ADMIN_GET_MATCHES","ADMIN_GET_HISTORY"})
                        check(sent.contains(cmd+"|16x16"),"UI requests scoped data");
                    check(!ui.acceptsScope("9x9")&&ui.acceptsScope("16x16"),"Stale responses rejected");
                    ui.setUsers("bob~USER~3~1~1~0~2026-11-08;alice~USER~9~3~0~0~2026-11-08");
                    JTable users=(JTable)field(ui,"usersTable");
                    users.setAutoCreateRowSorter(true);users.getRowSorter().toggleSortOrder(0);
                    users.setRowSelectionInterval(0,0);
                    ui.setUsers("alice~USER~12~4~0~0~2026-11-08;bob~USER~3~1~1~0~2026-11-08");
                    check(users.getSelectedRow()>=0 && users.getValueAt(users.getSelectedRow(),0).equals("alice"),
                            "Auto-refresh preserves selected username under sorting");
                    ui.setHistory("1~alice~bob~P1_WIN~COMPLETE~30~2026-11-08 15:40~16x16");
                    DefaultTableModel history=(DefaultTableModel)field(ui,"historyTableModel");
                    check(history.getColumnName(3).equals("Chế Độ")&&history.getValueAt(0,3).equals("Nâng cao"),"History mode before result");
                    ui.setMatches("alice~bob~M1~30~10~20~1~2~Playing~Playing~16x30");
                    @SuppressWarnings("unchecked")
                    List<AdminScreen.MatchItem> matches=(List<AdminScreen.MatchItem>)field(ui,"activeMatches");
                    check(matches.get(0).boardType==BoardType.LARGE,"UI parses large match");
                    String labels=TestBoardModes.labels((java.awt.Component)field(ui,"matchesListPanel"));
                    check(labels.contains("Chuyên gia")&&labels.contains("15:00"),"Live mode and time limit");
                    ui.selectScope("9x9");
                    check(users.getRowCount()==0&&history.getRowCount()==0,"Mode switch clears stale tables");
                    check(((JButton)field(ui,"resetScoreBtn")).getText().contains("Cơ bản"),"Reset scope visible");
                }catch(Exception e){throw new RuntimeException(e);}
                finally {ui.stopTimer();}
            });
            System.out.println("PASS: "+checks+" admin mode, permission, reset and UI checks.");
            System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
