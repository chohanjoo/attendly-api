-- System Log table
CREATE TABLE system_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    level VARCHAR(10) NOT NULL COMMENT '로그 레벨 (INFO, WARN, ERROR 등)',
    category VARCHAR(20) NOT NULL COMMENT '로그 카테고리 (APPLICATION, SECURITY, BATCH, AUDIT 등)',
    message VARCHAR(1000) NOT NULL COMMENT '로그 메시지',
    additional_info TEXT NULL COMMENT '추가 정보 (JSON 형태)',
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그 발생 시간',
    ip_address VARCHAR(39) NULL COMMENT '요청 IP 주소',
    user_id BIGINT NULL COMMENT '사용자 ID (로그인된 경우)',
    user_agent VARCHAR(255) NULL COMMENT 'User-Agent 정보',
    server_instance VARCHAR(50) NULL COMMENT '서버 인스턴스 이름',
    PRIMARY KEY (id),
    INDEX idx_system_log_level (level),
    INDEX idx_system_log_category (category),
    INDEX idx_system_log_timestamp (timestamp),
    INDEX idx_system_log_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='시스템 로그 테이블'; 