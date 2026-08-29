-- Ch20JdbcStorage.java에서 사용하는 MySQL 테이블 생성 스크립트
CREATE DATABASE IF NOT EXISTS delivery
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE delivery;

CREATE TABLE IF NOT EXISTS parcels (
    tracking_number VARCHAR(30) PRIMARY KEY,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone_number VARCHAR(30) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    weight INT NOT NULL,
    delivery_type VARCHAR(20) NOT NULL,
    fee INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    registered_at DATE NOT NULL,
    expected_delivery_at DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS delivery_histories (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(30) NOT NULL,
    before_status VARCHAR(20) NOT NULL,
    after_status VARCHAR(20) NOT NULL,
    changed_at DATETIME NOT NULL,
    CONSTRAINT fk_delivery_histories_parcels
        FOREIGN KEY (tracking_number) REFERENCES parcels(tracking_number)
        ON DELETE CASCADE
);
