package domin;

import domin.ui.AdminScreen;
import domin.ui.AdminSpectatorScreen;
import domin.ui.GameScreen;
import domin.ui.HomeScreen;
import domin.ui.LoginScreen;
import domin.ui.ResultDialog;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Client dò mìn đối kháng (tương tự Client trong QuizDoiKhang).
 *
 * Gộp các lớp cũ: ClientMain, ServerConnection, LoginFrame, LobbyFrame, GameFrame.
 * Một class duy nhất quản lý kết nối, nhận message, chuyển màn hình.
 *
 * Chạy: java domin.Client [host] [port]
 */
public class Client {

    private final JFrame frame;
    private Wire wire;
    private final String host;
    private final int port;
    private String name;
    private String currentOpponent;
    private boolean isLoggingOut = false;
    private boolean isAdmin = false;

    // Các màn hình hiện tại
    private LoginScreen loginScreen;
    private HomeScreen homeScreen;
    private GameScreen gameScreen;
    private AdminScreen adminScreen;
    private AdminSpectatorScreen spectatorScreen;

    Client(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        wire = new Wire(socket);

        frame = new JFrame("Minesweeper Battle - Đăng nhập");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onWindowClosing();
            }
        });

        try {
            java.io.InputStream is = getClass().getResourceAsStream("/images/logotest.png");
            if (is != null) {
                frame.setIconImage(javax.imageio.ImageIO.read(is));
            }
        } catch (Exception ignored) {}

        showLogin();
        frame.setSize(880, 560);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new java.awt.Color(10, 15, 30));
        frame.setVisible(true);

        startReaderThread();
    }

    private void startReaderThread() {
        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = wire.readLine()) != null) {
                        final String msg = line;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() { onMessage(msg); }
                        });
                    }
                } catch (IOException e) {
                    // mất kết nối
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() { onDisconnected(); }
                });
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    // ====================================================================
    //  Chuyển màn hình
    // ====================================================================

    private void showLogin() {
        homeScreen = null;
        gameScreen = null;
        adminScreen = null;
        spectatorScreen = null;
        name = null;
        isAdmin = false;
        isLoggingOut = false;
        loginScreen = new LoginScreen(new LoginScreen.SendCallback() {
            @Override
            public void send(String message) { wire.send(message); }
        });
        switchScreen(loginScreen);
        frame.setTitle("Minesweeper Battle - Đăng nhập");
        frame.setSize(880, 560);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
    }

    private void showHome() {
        if (adminScreen != null) adminScreen.stopTimer();
        adminScreen = null;
        spectatorScreen = null;
        homeScreen = new HomeScreen(name, new HomeScreen.HomeCallback() {
            @Override
            public void send(String message) {
                if ("LOGOUT".equals(message)) {
                    isLoggingOut = true;
                }
                wire.send(message);
            }
        });
        loginScreen = null;
        gameScreen = null;
        if (isAdmin) {
            homeScreen.setAdminMode(new Runnable() {
                @Override
                public void run() {
                    showAdmin();
                }
            });
        }
        switchScreen(homeScreen);
        frame.setTitle("Minesweeper Battle - Online Arena (" + name + ")");
        frame.setSize(1280, 820);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);

        // Yêu cầu dữ liệu ban đầu từ server
        wire.send("GET_PROFILE");
        wire.send("GET_ONLINE");
        wire.send("GET_LEADERBOARD");
        wire.send("GET_HISTORY");
    }

    private void showAdmin() {
        if (adminScreen != null) adminScreen.stopTimer();
        homeScreen = null;
        gameScreen = null;
        spectatorScreen = null;
        loginScreen = null;
        adminScreen = new AdminScreen(name, new AdminScreen.AdminCallback() {
            @Override
            public void send(String message) {
                wire.send(message);
            }
            @Override
            public void onSpectate(String matchId, String p1, String p2) {
                wire.send("SPECTATE_START|" + matchId);
            }
            @Override
            public void onSwitchToPlayerHome() {
                showHome();
            }
            @Override
            public void onLogout() {
                isLoggingOut = true;
                if (adminScreen != null) adminScreen.stopTimer();
                wire.send("LOGOUT");
            }
        });
        switchScreen(adminScreen);
        frame.setTitle("Minesweeper Battle - ADMIN Panel (" + name + ")");
        frame.setSize(1400, 860);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
    }

    private void showSpectator(String matchId, String p1, String p2,
                               int rows, int cols, int mines, int elapsed,
                               String minesData, String p1Rev, String p1Flags,
                               String p2Rev, String p2Flags,
                               int p1Pct, int p2Pct, int p1FlagCount, int p2FlagCount,
                               String p1Status, String p2Status) {
        if (adminScreen != null) adminScreen.stopTimer();
        spectatorScreen = new AdminSpectatorScreen(matchId, p1, p2,
                rows, cols, mines, elapsed,
                new AdminSpectatorScreen.SpectatorCallback() {
                    @Override
                    public void send(String message) { wire.send(message); }
                    @Override
                    public void onExitSpectate() {
                        wire.send("SPECTATE_STOP");
                        spectatorScreen = null;
                        showAdmin();
                    }
                });
        spectatorScreen.initMines(minesData);
        spectatorScreen.initPlayerRevealed(1, p1Rev);
        spectatorScreen.initPlayerFlags(1, p1Flags);
        spectatorScreen.initPlayerRevealed(2, p2Rev);
        spectatorScreen.initPlayerFlags(2, p2Flags);
        spectatorScreen.updateProgress(p1Pct, p2Pct, p1FlagCount, p2FlagCount, p1Status, p2Status);

        homeScreen = null;
        gameScreen = null;
        adminScreen = null;
        loginScreen = null;
        switchScreen(spectatorScreen);
        frame.setTitle("GIÁM SÁT: " + p1 + " vs " + p2 + " (" + name + ")");
        frame.setSize(1400, 860);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
    }

    private void showGame(String oppName, int rows, int cols, int mines, int timeLimit) {
        if (adminScreen != null) adminScreen.stopTimer();
        this.currentOpponent = oppName;
        gameScreen = new GameScreen(name, oppName, rows, cols, mines, timeLimit,
                new GameScreen.GameCallback() {
                    @Override
                    public void onOpenCell(int r, int c) { wire.send("OPEN|" + r + "|" + c); }
                    @Override
                    public void onFlagCell(int r, int c) { wire.send("FLAG|" + r + "|" + c); }
                    @Override
                    public void onSurrender() { wire.send("SURRENDER"); }
                });
        homeScreen = null;
        adminScreen = null;
        spectatorScreen = null;
        switchScreen(gameScreen);
        frame.setTitle("Dò mìn đối kháng - " + name + " vs " + oppName);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
    }

    private void switchScreen(javax.swing.JPanel panel) {
        frame.getContentPane().removeAll();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    // ====================================================================
    //  Nhận message từ Server
    // ====================================================================

    private void onMessage(String line) {
        String[] p = Wire.parse(line);
        switch (p[0]) {
            // ---- Login/Register/Logout ----
            case "LOGIN_OK": {
                name = Wire.field(p, 1);
                String role = Wire.field(p, 2);
                isAdmin = "ADMIN".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(name);
                if (isAdmin) {
                    showAdmin();
                } else {
                    showHome();
                }
                break;
            }
            case "LOGOUT_OK": {
                showLogin();
                break;
            }
            case "LOGIN_FAIL":
            case "REGISTER_FAIL":
            case "ERROR": {
                String msg = p.length > 1 ? p[1] : "Có lỗi xảy ra";
                if (loginScreen != null) {
                    loginScreen.setStatus(msg);
                } else if (homeScreen != null) {
                    homeScreen.setStatus(msg);
                    homeScreen.resetButtons();
                }
                break;
            }
            case "REGISTER_OK": {
                if (loginScreen != null) loginScreen.setStatus("Đăng ký thành công, hãy đăng nhập");
                break;
            }

            // ---- Home / Matchmaking ----
            case "WAITING": {
                if (homeScreen != null) homeScreen.setFindingMatch(true);
                break;
            }
            case "CANCELED": {
                if (homeScreen != null) {
                    homeScreen.resetButtons();
                    homeScreen.setStatus("Sẵn sàng thi đấu");
                }
                break;
            }

            // ---- Realtime Data ----
            case "PROFILE": {
                if (homeScreen != null && p.length >= 5) {
                    try {
                        homeScreen.setProfile(
                            Integer.parseInt(p[1]),
                            Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]),
                            Integer.parseInt(p[4])
                        );
                        if (p.length >= 6 && !p[5].isEmpty()) {
                            homeScreen.setAvatarBase64(p[5]);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
            case "ONLINE_LIST": {
                if (homeScreen != null) {
                    homeScreen.setOnlinePlayers(p.length > 1 ? p[1] : "");
                }
                break;
            }
            case "PLAYER_ONLINE": {
                if (homeScreen != null && p.length >= 3) {
                    try {
                        homeScreen.addOnlinePlayer(p[1], Integer.parseInt(p[2]));
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
            case "PLAYER_OFFLINE": {
                if (homeScreen != null && p.length >= 2) {
                    homeScreen.removeOnlinePlayer(p[1]);
                }
                break;
            }
            case "PLAYER_STATUS": {
                if (homeScreen != null && p.length >= 3) {
                    homeScreen.updatePlayerStatus(p[1], p[2]);
                }
                break;
            }
            case "LEADERBOARD": {
                if (homeScreen != null) {
                    homeScreen.setLeaderboard(p.length > 1 ? p[1] : "");
                }
                break;
            }
            case "HISTORY": {
                if (homeScreen != null) {
                    homeScreen.setHistory(p.length > 1 ? p[1] : "");
                }
                break;
            }
            case "CHALLENGE_RECEIVED": {
                if (homeScreen != null && p.length >= 3) {
                    try {
                        homeScreen.addChallenge(p[1], Integer.parseInt(p[2]));
                        homeScreen.showNotification("Nhận được lời mời thách đấu từ " + p[1] + "!");
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }

            // ---- Match ----
            case "MATCH_START": {
                // MATCH_START|tên đối thủ|số hàng|số cột|số mìn|giới hạn giây
                showGame(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                        Integer.parseInt(p[4]), Integer.parseInt(p[5]));
                break;
            }
            case "CELLS": {
                if (gameScreen == null) break;
                for (String item : p[1].split(";")) {
                    String[] v = item.split(",");
                    gameScreen.showOpened(Integer.parseInt(v[0]), Integer.parseInt(v[1]),
                            Integer.parseInt(v[2]));
                }
                break;
            }
            case "FLAG": {
                if (gameScreen != null) {
                    gameScreen.showFlag(Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]));
                }
                break;
            }
            case "PROGRESS": {
                if (gameScreen != null) {
                    gameScreen.updateProgress(
                            Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                            p[3], p[4], p[5], p[6]);
                }
                break;
            }
            case "NOTIFY": {
                if (gameScreen != null) {
                    gameScreen.setNotify(p[1]);
                } else if (homeScreen != null) {
                    homeScreen.showNotification(p[1]);
                }
                break;
            }
            case "TIME": {
                if (gameScreen != null) gameScreen.updateTimer(Integer.parseInt(p[1]));
                break;
            }
            case "MINES": {
                if (gameScreen != null && p.length > 1 && !p[1].isEmpty()) {
                    for (String item : p[1].split(";")) {
                        String[] v = item.split(",");
                        gameScreen.showMine(Integer.parseInt(v[0]), Integer.parseInt(v[1]));
                    }
                }
                break;
            }
            case "GAME_OVER": {
                onGameOver(p);
                break;
            }

            // ---- Admin & Spectator Protocols ----
            case "ADMIN_STATS": {
                if (adminScreen != null && p.length >= 5) {
                    try {
                        adminScreen.setStats(
                                Integer.parseInt(p[1]),
                                Integer.parseInt(p[2]),
                                Integer.parseInt(p[3]),
                                Integer.parseInt(p[4])
                        );
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
            case "ADMIN_MATCH_LIST": {
                if (adminScreen != null) {
                    adminScreen.setMatches(p.length > 1 ? p[1] : "");
                }
                break;
            }
            case "ADMIN_USER_LIST": {
                if (adminScreen != null) {
                    adminScreen.setUsers(p.length > 1 ? p[1] : "");
                }
                break;
            }
            case "ADMIN_HISTORY_LIST": {
                if (adminScreen != null) {
                    adminScreen.setHistory(p.length > 1 ? p[1] : "");
                }
                break;
            }
            case "SPECTATE_INIT": {
                if (p.length >= 19) {
                    try {
                        showSpectator(
                                p[1], p[2], p[3],
                                Integer.parseInt(p[4]),
                                Integer.parseInt(p[5]),
                                Integer.parseInt(p[6]),
                                Integer.parseInt(p[7]),
                                p[8], p[9], p[10], p[11], p[12],
                                Integer.parseInt(p[13]),
                                Integer.parseInt(p[14]),
                                Integer.parseInt(p[15]),
                                Integer.parseInt(p[16]),
                                p[17], p[18]
                        );
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                break;
            }
            case "SPECTATE_OPEN": {
                if (spectatorScreen != null && p.length >= 3) {
                    spectatorScreen.handleOpen(p[1], p[2]);
                }
                break;
            }
            case "SPECTATE_FLAG": {
                if (spectatorScreen != null && p.length >= 5) {
                    try {
                        spectatorScreen.handleFlag(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]));
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
            case "SPECTATE_PROGRESS": {
                if (spectatorScreen != null && p.length >= 7) {
                    try {
                        spectatorScreen.updateProgress(
                                Integer.parseInt(p[1]),
                                Integer.parseInt(p[2]),
                                Integer.parseInt(p[3]),
                                Integer.parseInt(p[4]),
                                p[5], p[6]
                        );
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
            case "SPECTATE_TIME": {
                if (spectatorScreen != null && p.length >= 2) {
                    try {
                        spectatorScreen.updateTimer(Integer.parseInt(p[1]));
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
            case "SPECTATE_NOTIFY": {
                if (spectatorScreen != null && p.length >= 2) {
                    spectatorScreen.addCombatLog(p[1]);
                }
                break;
            }
            case "SPECTATE_OVER": {
                if (spectatorScreen != null && p.length >= 8) {
                    try {
                        spectatorScreen.handleGameOver(
                                p[1], p[2],
                                Integer.parseInt(p[3]),
                                Integer.parseInt(p[4]),
                                Integer.parseInt(p[5]),
                                Integer.parseInt(p[6]),
                                Integer.parseInt(p[7])
                        );
                    } catch (NumberFormatException ignored) {}
                }
                break;
            }
            case "SPECTATE_STOPPED": {
                if (spectatorScreen != null) {
                    spectatorScreen = null;
                    showAdmin();
                }
                break;
            }
            default:
                break;
        }
    }

    // ====================================================================
    //  Kết thúc trận
    // ====================================================================

    private void onGameOver(String[] p) {
        if (gameScreen == null) return;
        gameScreen.setGameOver();
        String result = p[1];
        String reason = p[2];
        int seconds = Integer.parseInt(p[3]);
        int mySafeOpened = Integer.parseInt(p[4]);
        int oppSafeOpened = Integer.parseInt(p[5]);
        int myFlags = Integer.parseInt(p[6]);
        int oppFlags = Integer.parseInt(p[7]);
        int points = Integer.parseInt(p[8]);

        String title;
        if (result.equals("WIN")) {
            title = "Bạn đã THẮNG!";
        } else if (result.equals("LOSE")) {
            title = "Bạn đã THUA.";
        } else {
            title = "Trận đấu HÒA.";
        }
        gameScreen.setNotify(title);

        ResultDialog dlg = new ResultDialog(frame, result, reason, seconds,
                mySafeOpened, oppSafeOpened, myFlags, oppFlags, points,
                name, currentOpponent);
        dlg.setVisible(true);

        showHome();
    }

    private String describeReason(String result, String reason) {
        boolean win = result.equals("WIN");
        switch (reason) {
            case "COMPLETE":
                return win ? "Bạn hoàn thành bàn dò mìn nhanh hơn đối thủ."
                        : "Đối thủ hoàn thành bàn dò mìn trước bạn.";
            case "MINE":
                return win ? "Đối thủ đã đạp trúng mìn." : "Bạn đã đạp trúng mìn.";
            case "SURRENDER":
                return win ? "Đối thủ đã bỏ cuộc." : "Bạn đã bỏ cuộc.";
            case "DISCONNECT":
                return "Đối thủ bị mất kết nối.";
            case "TIMEOUT":
                return "Hết giờ mà cả hai chưa hoàn thành.";
            default:
                return "";
        }
    }

    // ====================================================================
    //  Xử lý đóng cửa sổ / mất kết nối
    // ====================================================================

    private void onWindowClosing() {
        if (gameScreen != null && !gameScreen.isOver()) {
            int choice = JOptionPane.showConfirmDialog(frame,
                    "Bạn có chắc muốn bỏ cuộc? Bạn sẽ bị xử thua.", "Bỏ cuộc",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                wire.send("SURRENDER");
            }
        } else {
            wire.close();
            System.exit(0);
        }
    }

    private void onDisconnected() {
        if (isLoggingOut) {
            isLoggingOut = false;
            reconnect();
            return;
        }
        if (gameScreen == null || !gameScreen.isOver()) {
            JOptionPane.showMessageDialog(frame, "Mất kết nối tới Server");
            System.exit(0);
        }
    }

    private void reconnect() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            wire = new Wire(socket);
            startReaderThread();
            showLogin();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Không thể kết nối lại tới Server: " + e.getMessage());
            System.exit(0);
        }
    }

    // ====================================================================
    //  main — Chạy: java domin.Client [host]
    // ====================================================================

    public static void main(String[] args) {
        final String host;
        if (args.length > 0) {
            host = args[0];
        } else {
            Scanner sc = new Scanner(System.in);
            System.out.print("Nhập địa chỉ IP Server (Enter = localhost): ");
            String input = sc.nextLine().trim();
            host = input.isEmpty() ? "localhost" : input;
        }
        System.out.println("Đang kết nối tới Server: " + host);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new Client(host, Model.PORT);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                            "Không kết nối được server: " + e.getMessage());
                }
            }
        });
    }
}
