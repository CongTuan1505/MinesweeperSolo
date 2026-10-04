-- =====================================================
-- Database cho game Dò mìn đối kháng (MySQL)
-- Cách chạy: mở phpMyAdmin (XAMPP) hoặc MySQL Workbench,
-- import/chạy toàn bộ file này một lần.
-- =====================================================

CREATE DATABASE IF NOT EXISTS domin_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE domin_db;

-- Bảng tài khoản người chơi
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash CHAR(64)    NOT NULL,          -- SHA-256 dạng hex
    score         INT NOT NULL DEFAULT 0,        -- thắng +3, hòa +1, thua +0
    wins          INT NOT NULL DEFAULT 0,
    losses        INT NOT NULL DEFAULT 0,
    draws         INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng lịch sử ván đấu
CREATE TABLE IF NOT EXISTS matches (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    player1      VARCHAR(50) NOT NULL,
    player2      VARCHAR(50) NOT NULL,
    result       VARCHAR(10) NOT NULL,           -- P1_WIN, P2_WIN, DRAW
    reason       VARCHAR(20),                    -- COMPLETE, MINE, SURRENDER, DISCONNECT, TIMEOUT
    duration_sec INT,
    played_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
