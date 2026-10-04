package domin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Giao thức truyền tin giữa Server và Client (tương tự Wire trong QuizDoiKhang).
 *
 * Mỗi lệnh là một dòng văn bản, các phần cách nhau bằng dấu '|'.
 *   Client → Server: REGISTER|user|pass, LOGIN|user|pass, FIND_MATCH, CANCEL_MATCH,
 *                    OPEN|r|c, FLAG|r|c, SURRENDER
 *   Server → Client: MATCH_START, CELLS, FLAG, PROGRESS, NOTIFY, TIME, MINES, GAME_OVER
 */
final class Wire {

    final Socket socket;
    final BufferedReader in;
    final PrintWriter out;

    Wire(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /** Gửi một dòng text. Thread-safe. */
    void send(String message) {
        synchronized (out) {
            out.println(message);
        }
    }

    /** Đọc một dòng text (blocking). Trả về null khi mất kết nối. */
    String readLine() throws IOException {
        return in.readLine();
    }

    /** Tách dòng text thành mảng theo dấu '|'. */
    static String[] parse(String line) {
        return line.split("\\|");
    }

    /** Lấy phần tử thứ i, trả về "" nếu vượt index. */
    static String field(String[] a, int i) {
        return i < a.length ? a[i] : "";
    }

    void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    boolean isConnected() {
        return !socket.isClosed();
    }
}
