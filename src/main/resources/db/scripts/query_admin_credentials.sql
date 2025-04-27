-- 관리자의 이메일과 비밀번호 조회 쿼리
SELECT email, password 
FROM user 
WHERE role = 'ADMIN';

-- 참고: 비밀번호는 암호화되어 있으며, 실제 값은 'admin123'입니다 