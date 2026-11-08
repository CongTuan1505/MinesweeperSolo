-- ====================================================================
-- Database: Dò Mìn Đối Kháng Online (Minesweeper Battle Online)
-- Hệ quản trị CSDL: MySQL / MariaDB (XAMPP phpMyAdmin / MySQL Workbench)
-- Hướng dẫn: Mở MySQL Workbench hoặc phpMyAdmin, import/chạy toàn bộ file này.
-- ====================================================================

CREATE DATABASE IF NOT EXISTS domin_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE domin_db;

-- --------------------------------------------------------------------
-- 1. Bảng USERS: Quản lý thông tin tài khoản và thành tích người chơi
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,          -- Mật khẩu băm SHA-256 dạng hex
    score         INT          NOT NULL DEFAULT 0, -- Thắng: +3, Hòa: +1, Thua: +0
    wins          INT          NOT NULL DEFAULT 0, -- Tổng số trận thắng
    losses        INT          NOT NULL DEFAULT 0, -- Tổng số trận thua
    draws         INT          NOT NULL DEFAULT 0, -- Tổng số trận hòa
    score_16x16   INT NOT NULL DEFAULT 0,
    wins_16x16    INT NOT NULL DEFAULT 0,
    losses_16x16  INT NOT NULL DEFAULT 0,
    draws_16x16   INT NOT NULL DEFAULT 0,
    score_16x30   INT NOT NULL DEFAULT 0,
    wins_16x30    INT NOT NULL DEFAULT 0,
    losses_16x30  INT NOT NULL DEFAULT 0,
    draws_16x30   INT NOT NULL DEFAULT 0,
    avatar        MEDIUMTEXT   NULL,
    is_online     TINYINT(1)   NOT NULL DEFAULT 0, -- 0: Offline, 1: Online
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP    NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    -- Chỉ mục tối ưu cho bảng xếp hạng (Leaderboard)
    INDEX idx_users_score_wins (score DESC, wins DESC, losses ASC),
    INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 2. Bảng MATCHES: Quản lý lịch sử các ván đấu 1v1
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS matches (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    player1             VARCHAR(50) NOT NULL,
    player2             VARCHAR(50) NOT NULL,
    result              VARCHAR(10) NOT NULL, -- P1_WIN, P2_WIN, DRAW
    reason              VARCHAR(20) NOT NULL, -- COMPLETE, MINE, SURRENDER, DISCONNECT, TIMEOUT
    board_type          VARCHAR(10) NOT NULL DEFAULT '9x9',
    duration_sec        INT         NOT NULL DEFAULT 0, -- Thời gian thi đấu tính theo giây
    player1_safe_opened INT         DEFAULT 0,          -- Số ô an toàn P1 đã mở
    player2_safe_opened INT         DEFAULT 0,          -- Số ô an toàn P2 đã mở
    player1_flags       INT         DEFAULT 0,          -- Số cờ P1 đã đặt
    player2_flags       INT         DEFAULT 0,          -- Số cờ P2 đã đặt
    played_at           TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

    -- Khóa ngoại liên kết tới bảng users (đồng bộ khi đổi tên hoặc xóa tài khoản)
    CONSTRAINT fk_matches_player1 FOREIGN KEY (player1) 
        REFERENCES users(username) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_matches_player2 FOREIGN KEY (player2) 
        REFERENCES users(username) ON UPDATE CASCADE ON DELETE CASCADE,

    -- Chỉ mục tăng tốc truy vấn lịch sử đấu
    INDEX idx_matches_p1_date (player1, played_at DESC),
    INDEX idx_matches_p2_date (player2, played_at DESC),
    INDEX idx_matches_played_at (played_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 3. Bảng CHALLENGES (Tùy chọn): Quản lý và lưu vết lời mời thách đấu
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS challenges (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    challenger  VARCHAR(50) NOT NULL,
    challenged  VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED, EXPIRED
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chal_challenger FOREIGN KEY (challenger) 
        REFERENCES users(username) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_chal_challenged FOREIGN KEY (challenged) 
        REFERENCES users(username) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- 4. VIEWS: Hỗ trợ thống kê nhanh cho hệ thống
-- --------------------------------------------------------------------

-- View Bảng xếp hạng chi tiết có tỷ lệ thắng (%)
CREATE OR REPLACE VIEW v_leaderboard AS
SELECT 
    username,
    score,
    wins,
    losses,
    draws,
    (wins + losses + draws) AS total_matches,
    CASE 
        WHEN (wins + losses + draws) > 0 
        THEN ROUND((wins * 100.0) / (wins + losses + draws), 1)
        ELSE 0.0 
    END AS win_rate,
    created_at
FROM users
ORDER BY score DESC, wins DESC, losses ASC;

-- View Lịch sử thi đấu chi tiết
CREATE OR REPLACE VIEW v_match_history AS
SELECT 
    m.id,
    m.player1,
    m.player2,
    m.result,
    m.reason,
    m.duration_sec,
    m.played_at,
    CASE 
        WHEN m.result = 'P1_WIN' THEN m.player1
        WHEN m.result = 'P2_WIN' THEN m.player2
        ELSE 'Hòa'
    END AS winner
FROM matches m
ORDER BY m.played_at DESC;

-- --------------------------------------------------------------------
