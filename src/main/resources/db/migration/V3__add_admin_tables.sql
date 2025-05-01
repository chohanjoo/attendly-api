-- 시스템 설정 테이블
CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(50) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 배치 작업 테이블
CREATE TABLE batch_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    parameters TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    error_message TEXT,
    schedule_time TIMESTAMP NULL
);

-- 배치 작업 로그 테이블
CREATE TABLE batch_job_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_job_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    level VARCHAR(10) NOT NULL DEFAULT 'INFO',
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (batch_job_id) REFERENCES batch_jobs(id) ON DELETE CASCADE
);

-- 기본 시스템 설정 값 추가
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('attendance.input.day', 'SUNDAY', '출석 입력 가능 요일'),
    ('attendance.input.start.hour', '0', '출석 입력 시작 시간'),
    ('attendance.input.end.hour', '23', '출석 입력 종료 시간'),
    ('attendance.allow.member.edit', 'false', '조원의 출석 자체 수정 허용 여부'),
    ('attendance.autolock.enabled', 'true', '자동 잠금 활성화 여부'),
    ('attendance.autolock.timeout.hours', '48', '자동 잠금 제한 시간(시간)'),
    ('batch.reminder.enabled', 'true', '출석 입력 알림 활성화 여부'),
    ('batch.reminder.day', 'SATURDAY', '출석 입력 알림 요일'),
    ('batch.reminder.hour', '12', '출석 입력 알림 시간'),
    ('batch.statistics.enabled', 'true', '통계 생성 활성화 여부'),
    ('batch.statistics.day', 'MONDAY', '통계 생성 요일'),
    ('batch.statistics.hour', '2', '통계 생성 시간'),
    ('security.session.timeout.minutes', '30', '세션 타임아웃(분)'),
    ('security.password.expiry.days', '90', '비밀번호 만료일(일)'),
    ('security.password.min.length', '8', '비밀번호 최소 길이'),
    ('security.password.require.special', 'true', '비밀번호 특수문자 요구 여부'),
    ('security.password.require.uppercase', 'true', '비밀번호 대문자 요구 여부'),
    ('security.password.require.number', 'true', '비밀번호 숫자 요구 여부'),
    ('security.login.attempt.limit', '5', '로그인 시도 제한 횟수'),
    ('email.smtp.server', 'smtp.gmail.com', 'SMTP 서버'),
    ('email.smtp.port', '587', 'SMTP 포트'),
    ('email.smtp.enableTLS', 'true', 'TLS 활성화 여부');

-- 사용자 테이블에 추가 필드 생성
ALTER TABLE users ADD COLUMN last_password_change TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN account_locked BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP NULL; 