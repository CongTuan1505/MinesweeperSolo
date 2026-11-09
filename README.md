# Dò mìn đối kháng online — Java, Socket API, chạy trên VSCode

Game dò mìn thi đấu 1v1 qua mạng. Client dùng Java Swing, Server dùng `ServerSocket` +
Thread Pool, dữ liệu lưu ở MySQL. Hai người chơi **có thể ở hai máy khác nhau**, chỉ cần
cùng mạng (LAN/Wi-Fi) hoặc Server có IP công khai.

## 1. Cấu trúc project

```
do-min-doi-khang/
├── pom.xml                      <- Maven: khai báo thư viện, cách build/chạy
├── run-server.sh / .bat         <- chạy Server
├── run-client.sh / .bat         <- chạy Client
├── database/schema_mysql.sql    <- script tạo database MySQL
├── src/main/resources/
│   └── db.properties            <- cấu hình kết nối MySQL (sửa ở đây nếu cần)
├── src/main/java/
│   ├── server/                  <- phía Server
│   │   ├── GameServer.java      (ServerSocket + Thread Pool + ghép trận — chạy file này)
│   │   ├── ClientHandler.java   (đọc/ghi lệnh của từng client)
│   │   ├── GameRoom.java        (TRỌNG TÀI: tiến độ, đồng hồ, thắng/thua/hòa)
│   │   ├── Board.java           (bàn mìn thật, chỉ tồn tại ở Server)
│   │   ├── Database.java, UserDAO.java, MatchDAO.java  (MySQL)
│   │   └── PlayerConnection.java
│   └── client/                  <- phía Client (Swing)
│       ├── ClientMain.java      (chạy file này để mở 1 Client)
│       ├── LoginFrame.java, LobbyFrame.java
│       ├── GameFrame.java       (MÀN HÌNH THI ĐẤU)
│       └── ServerConnection.java
└── tests/                       <- bộ kiểm thử (chạy bằng javac/java thường, không cần framework)
    ├── TestGameRoom.java         (kiểm thử logic trọng tài: mở ô, cờ, 4 trường hợp thắng/thua/hòa)
    └── TestE2E.java              (kiểm thử qua socket thật: đăng nhập, ghép trận, chơi, bỏ cuộc...)
```

## 2. Cài đặt môi trường

1. **JDK 17 trở lên**: `java -version` để kiểm tra. Nếu dùng bản JDK khác (vd: 11), sửa
   `maven.compiler.source`/`target` trong `pom.xml`.
2. **Maven**: `mvn -version` để kiểm tra. VSCode cũng có thể tự tải Maven qua Java Extension Pack.
3. **VSCode**: cài extension **Extension Pack for Java** (`vscjava.vscode-java-pack`) — khi mở
   thư mục project, VSCode sẽ tự nhận đây là project Maven (nhờ `pom.xml`) và tải
   `mysql-connector-j` về tự động, không cần tải `.jar` thủ công.
4. **MySQL**: cài **XAMPP**, bật MySQL, mở phpMyAdmin → tab **Import** → chọn file
   `database/schema_mysql.sql`.
   - Nếu MySQL có mật khẩu `root`, sửa trong `src/main/resources/db.properties`.

## 3. Chạy trên VSCode (cách dễ nhất)

Mở thư mục project trong VSCode, đợi thanh trạng thái báo "Java: Ready" (lần đầu sẽ tự tải
thư viện MySQL qua Maven). Mở tab **Run and Debug** (Ctrl+Shift+D), chọn trong danh sách:

1. **"1. Chạy Server"** → bấm nút ▶. Xem tab Terminal/Debug Console sẽ in ra các địa chỉ IP
   của máy (vd: `192.168.1.5`).
2. **"2. Chạy Client (localhost)"** → bấm ▶ hai lần (hai lần chạy = hai cửa sổ Client) để tự
   chơi thử trên 1 máy.
3. Khi chơi với **máy khác**: xem mục 5 bên dưới.

## 4. Chạy bằng dòng lệnh (không cần mở VSCode)

```bash
# Máy chạy Server:
./run-server.sh              # Linux/macOS, cổng mặc định 5555
run-server.bat                # Windows

# Máy chạy Client (có thể cùng máy hoặc máy khác):
./run-client.sh               # Linux/macOS, nối tới localhost
run-client.bat                 # Windows
```

Lần chạy đầu `mvn` sẽ tự tải `mysql-connector-j` và `exec-maven-plugin` về máy (cần Internet).

## 5. Chơi giữa hai máy khác nhau (quan trọng)

1. **Máy A** (chạy Server): chạy `run-server.sh`/`run-server.bat`, đọc dòng
   `Địa chỉ IP khả dụng: ...` để lấy IP LAN (vd `192.168.1.5`).
   - Mở cổng **5555** trên Windows Firewall / macOS Firewall (hoặc tắt tạm firewall khi test).
2. **Máy B, C,...** (chạy Client): chạy
   ```bash
   ./run-client.sh 192.168.1.5      # thay bằng IP thật của máy A
   ```
   hoặc mở Client xong gõ IP đó vào ô **"Địa chỉ Server"** trên giao diện đăng nhập.
3. Cả hai máy đăng ký/đăng nhập tài khoản riêng, bấm **"Tìm trận"** — ai bấm trước sẽ chờ
   (`WAITING`), người thứ hai bấm sẽ ghép trận ngay.

> Lưu ý: chỉ **Server** (Máy A) cần cài MySQL. Các Client không kết nối database trực tiếp.

## 6. Build thành 1 file .jar để mang đi máy khác chạy (không cần cài Maven ở đó)

```bash
mvn package
# Sinh ra target/do-min-doi-khang.jar (đã gồm driver MySQL)

# Máy Server:
java -cp target/do-min-doi-khang.jar server.GameServer

# Máy Client:
java -cp target/do-min-doi-khang.jar client.ClientMain 192.168.1.5
```

## 7. Chạy thử bộ kiểm thử trong `tests/`

```bash
# Biên dịch code chính trước
mkdir -p out
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")

# Kiểm thử logic trọng tài (không cần Server/Socket)
javac -encoding UTF-8 -cp out -d out tests/TestGameRoom.java
java -cp out server.TestGameRoom

# Kiểm thử qua socket thật (cần chạy Server ở một terminal khác trước: ./run-server.sh)
javac -encoding UTF-8 -d out tests/TestE2E.java
java -cp out TestE2E
```
`TestGameRoom` sẽ in lỗi "Không lưu được kết quả trận vào database" — đây là bình thường vì
test không kết nối MySQL thật, chỉ kiểm tra logic game.

## 8. Giao thức Client ↔ Server

Mỗi lệnh là một dòng văn bản, các phần cách nhau bằng `|`.

| Client gửi | Ý nghĩa |
|---|---|
| `REGISTER\|user\|pass`, `LOGIN\|user\|pass` | Đăng ký / đăng nhập |
| `FIND_MATCH`, `CANCEL_MATCH` | Vào / rời hàng đợi ghép trận |
| `OPEN\|r\|c` | Click trái: mở ô |
| `FLAG\|r\|c` | Click phải: đổi trạng thái cờ (không → cờ → dấu hỏi → không) |
| `SURRENDER` | Bỏ cuộc |

| Server gửi | Ý nghĩa |
|---|---|
| `MATCH_START\|đối thủ\|hàng\|cột\|mìn\|giây` | Bắt đầu ván |
| `CELLS\|r,c,số;...` | Các ô vừa mở (đã lan tỏa vùng trống) |
| `FLAG\|r\|c\|trạng thái` | Xác nhận trạng thái cờ |
| `PROGRESS\|%mình\|%đối thủ\|cờ mình\|cờ đối thủ\|trạng thái mình\|trạng thái đối thủ` | Tiến độ (chỉ số liệu tổng hợp) |
| `NOTIFY\|nội dung` | Thông báo realtime về đối thủ |
| `TIME\|giây` | Đồng hồ, gửi mỗi giây |
| `MINES\|r,c;...` | Vị trí mìn, **chỉ gửi khi ván đã kết thúc** |
| `GAME_OVER\|kết quả\|lý do\|giây\|ô mình\|ô đối thủ\|cờ mình\|cờ đối thủ\|điểm` | WIN / LOSE / DRAW |

## 9. Luật thắng / thua / hòa (trong `GameRoom.java`)

| Trường hợp | Điều kiện | Kết quả |
|---|---|---|
| 1. Đua thời gian | Có người mở hết ô an toàn trước | Người xong trước **thắng (+3)** |
| 2. Trúng mìn | Một người bấm trúng mìn | Người trúng **thua (0)**, người kia **thắng (+3)** |
| 3. Bỏ cuộc | Bấm "Bỏ cuộc", đóng cửa sổ, hoặc mất kết nối | Người bỏ **thua (0)**, người kia **thắng (+3)** |
| 4. Hòa | Hết giờ (300 giây) mà chưa ai xong | Cả hai **hòa (+1)** |

Đổi kích thước bàn/số mìn trong `Board.java` (`ROWS`, `COLS`, `MINES`), đổi giới hạn giờ trong
`GameRoom.java` (`TIME_LIMIT_SEC`).

## 10. Lỗi thường gặp

| Lỗi | Cách xử lý |
|---|---|
| VSCode báo đỏ "import com.mysql..." | Đợi Maven tải xong thư viện (xem thanh trạng thái), hoặc chạy `mvn compile` một lần trong terminal |
| `No suitable driver found for jdbc:mysql` khi chạy `java -cp` trực tiếp | Chưa `mvn package` (thiếu driver trong classpath) — dùng `run-server.sh` hoặc file jar ở mục 6 |
| `Communications link failure` / "Lỗi cơ sở dữ liệu" | Chưa bật MySQL (XAMPP), hoặc sai thông tin trong `db.properties` |
| Client "Không kết nối được Server" | Chưa chạy Server, sai IP, hoặc Firewall chặn cổng 5555 |
| Hai máy không thấy nhau | Kiểm tra cùng mạng Wi-Fi/LAN; mạng công ty/trường học hay chặn kết nối P2P giữa các máy |
| Chữ tiếng Việt lỗi font | Thêm `-Dfile.encoding=UTF-8` khi chạy `java` trực tiếp (các script `.sh`/`.bat` không cần vì Maven tự lo) |

## Các loại bàn đấu

| Bàn | Số mìn | Thời gian |
| --- | --- | --- |
| 9×9 | 10 | 5 phút |
| 16×16 | 40 | 10 phút |
| 16×30 | 99 | 15 phút |

Chọn loại bàn phía trên nút tìm trận. Ghép ngẫu nhiên chỉ ghép người chọn cùng loại bàn.
Khi thách đấu, chọn bàn trong hộp thoại; người nhận thấy loại bàn trong lời mời.
Profile và bảng xếp hạng có lựa chọn bàn riêng; điểm, thắng, thua, hòa, tỷ lệ thắng và thứ hạng độc lập.
Lịch sử trận hiển thị thêm loại bàn.

Khởi động lại server và client sau khi nâng cấp. Server tự bổ sung các cột thống kê cho hai loại bàn mới
và cột matches.board_type; tài khoản MySQL cần quyền ALTER. Dữ liệu cũ thuộc bàn 9×9 được giữ nguyên.
Mỗi kết quả trận được lưu cùng thống kê hai người trong một transaction.

## Âm thanh

Hiệu ứng ngắn cho mở ô/vùng, đặt/gỡ cờ, bắt đầu trận, lời mời thách đấu, thắng/thua/hòa,
trúng mìn và cảnh báo 10 giây cuối. Nút Âm thanh có ở thanh trên của sảnh và dưới bàn chơi;
lựa chọn bật/tắt được lưu trên máy. Tắt âm thanh dừng ngay các hiệu ứng đang phát.
Âm thanh được tổng hợp cục bộ bằng Java Sound, không cần tải tệp hoặc thư viện ngoài.
Máy không có thiết bị phát âm thanh vẫn chơi bình thường.

## Quản trị theo chế độ

Bộ lọc ở trang admin áp dụng cho thành tích người chơi, trận đang đấu, lịch sử và số trận thống kê.
“Tất cả chế độ” cộng thành tích của ba loại bàn; chọn Cơ bản/Nâng cao/Chuyên gia để xem riêng.
Số tài khoản và số người chơi online luôn tính toàn hệ thống.

Nút đặt lại thành tích chỉ xóa điểm và thắng/thua/hòa trong phạm vi đang chọn; lịch sử trận được giữ nguyên.
Không đặt lại thành tích khi người chơi đang thi đấu. Danh sách giữ lựa chọn người chơi khi tự làm mới.
Giám sát hiển thị chế độ, thời gian tương ứng và hỗ trợ cuộn bàn lớn; bộ lọc được giữ khi quay lại admin.
Âm thanh bật/tắt tại admin chỉ áp dụng cho máy hiện tại, không thay đổi lựa chọn của người chơi khác.
Các lệnh quản trị và giám sát được kiểm tra quyền ở server. Sau nâng cấp cần khởi động lại server và client.


Các nút bấm, menu, checkbox và bộ chọn chế độ có tiếng click nhẹ, kể cả khi kích hoạt bằng bàn phím.
Âm click dùng chung nút bật/tắt âm thanh; cập nhật dữ liệu tự động không phát tiếng click.

Nhạc nền điện tử kiểu game giải đố (140 BPM, giai điệu arcade, bass đảo quãng tám và nhịp trống rõ hơn) được tổng hợp cục bộ, lặp liên tục trong ứng dụng và phát độc lập với hiệu ứng.
Nút Âm thanh bật/tắt đồng thời cả nhạc nền và hiệu ứng; tắt sẽ dừng nhạc ngay, bật sẽ phát lại.
Không cần tải nhạc hoặc thêm thư viện.

Bảng xếp hạng có bục top 3, ảnh đại diện dạng chữ cái và thanh tỷ lệ thắng; profile hiển thị vòng tỷ lệ thắng
và thống kê riêng theo chế độ. Ba ảnh hướng dẫn gốc nằm trong src/main/resources/images/guide,
minh họa mục tiêu mở ô, điều khiển chuột và cách tính điểm. Nội dung hướng dẫn có thể cuộn khi thu nhỏ cửa sổ.

## Trò chuyện riêng trong sảnh

Chọn một người chơi rồi bấm Trò chuyện bên dưới Thách đấu để chuyển khung bên phải sang chat riêng.
Nút Quay lại đưa bạn về danh sách. Enter gửi tin; Shift+Enter xuống dòng; tối đa 500 ký tự mỗi tin.
Tin gửi chỉ xuất hiện trong lịch sử sau khi server xác nhận. Khi người nhận ngoại tuyến, bản nháp được giữ.
Có báo tin chưa đọc và lịch sử trò chuyện gần đây, kể cả khi đối phương đã thoát.
Lịch sử giữ tối đa 100 tin mỗi người trong phiên đăng nhập, được giữ khi vào trận/quay về sảnh và xóa khi đăng xuất;
không lưu tin nhắn vào database. Chat cần server và client cùng được cập nhật.

Nếu nút chat bị giữ ở “Đang gửi”, hãy khởi động lại server sau khi cập nhật mã nguồn.
Các script chạy nay tự biên dịch trước khi khởi động. Client nhận biết phản hồi server cũ,
và tự bỏ trạng thái chờ sau 5 giây nếu không có xác nhận; bản nháp được giữ để người dùng kiểm tra và thử lại.
Tin chưa được xác nhận không tự động gửi lại, tránh gửi trùng khi mạng chậm.
