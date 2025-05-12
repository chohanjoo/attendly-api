ALTER TABLE users
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- 데이터 마이그레이션 - 기존 사용자들을 모두 ACTIVE로 설정합니다
UPDATE users SET status = 'ACTIVE'; 