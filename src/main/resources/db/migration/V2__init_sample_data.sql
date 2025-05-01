-- Sample department
INSERT INTO department (name) VALUES ('대학부');

-- Sample village
INSERT INTO village (name, department_id) 
VALUES ('1마을', (SELECT id FROM department WHERE name = '대학부'));

-- Sample users with different roles
-- 1. Admin
INSERT INTO users (name, role, email, password, department_id)
VALUES ('관리자', 'ADMIN', 'admin@church.com', '$2a$10$mAKXJL22kfEyAcAvdqGEde.RF1srMqtt/ToCJmbOgSdxlueEQfsVW', -- password: admin123
        (SELECT id FROM department WHERE name = '대학부'));

-- 2. Minister
INSERT INTO users (name, role, email, password, department_id)
VALUES ('박교역', 'MINISTER', 'minister@church.com', '$2a$10$ux5KHp5Imd5M6c3sgUZRZOc.1TH1F3qh5eMGkOjifLfF1PYOHt.Hy', -- password: minister123
        (SELECT id FROM department WHERE name = '대학부'));

-- 3. Village Leader
INSERT INTO users (name, role, email, password, department_id)
VALUES ('김마을장', 'VILLAGE_LEADER', 'vleader@church.com', '$2a$10$sAr81nlU94oN7niw.1Mw7emjDPPTCRKJmrk0hE/4Gj62.K37TSNyK', -- password: village123
        (SELECT id FROM department WHERE name = '대학부'));

-- Village Leader assignment
INSERT INTO village_leader (user_id, village_id, start_dt)
VALUES (
    (SELECT id FROM users WHERE name = '김마을장'),
    (SELECT id FROM village WHERE name = '1마을'),
    '2025-01-01'
);

-- 4. GBS Leader
INSERT INTO users (name, role, email, password, department_id)
VALUES ('이리더', 'LEADER', 'leader@church.com', '$2a$10$sjnG9D2xTIWZBZt9fQ1YL.I/PVx3/l85lnCB56wL7CklTycPAZf5y', -- password: leader123
        (SELECT id FROM department WHERE name = '대학부'));

-- 5. Members (3 sample members)
INSERT INTO users (name, birth_date, role, email, password, department_id)
VALUES 
    ('조원1', '2000-01-01', 'MEMBER', 'member1@church.com', '$2a$10$a2A7ifjW3YSu9q.h2SfGnOI4kH.TN8OZGBcb/Ayx/sCSVyT8WRFXW', (SELECT id FROM department WHERE name = '대학부')), -- password: member123
    ('조원2', '2001-02-15', 'MEMBER', 'member2@church.com', '$2a$10$a2A7ifjW3YSu9q.h2SfGnOI4kH.TN8OZGBcb/Ayx/sCSVyT8WRFXW', (SELECT id FROM department WHERE name = '대학부')), -- password: member123
    ('조원3', '2002-08-10', 'MEMBER', 'member3@church.com', '$2a$10$a2A7ifjW3YSu9q.h2SfGnOI4kH.TN8OZGBcb/Ayx/sCSVyT8WRFXW', (SELECT id FROM department WHERE name = '대학부')); -- password: member123

-- Sample GBS group
INSERT INTO gbs_group (name, village_id, term_start_dt, term_end_dt)
VALUES ('믿음 GBS', 
        (SELECT id FROM village WHERE name = '1마을'),
        '2025-01-01',
        '2025-06-30');

-- GBS Leader History
INSERT INTO gbs_leader_history (gbs_id, leader_id, start_dt)
VALUES (
    (SELECT id FROM gbs_group WHERE name = '믿음 GBS'),
    (SELECT id FROM users WHERE name = '이리더'),
    '2025-01-01'
);

-- GBS Member History for 3 members
INSERT INTO gbs_member_history (gbs_id, member_id, start_dt)
VALUES 
    ((SELECT id FROM gbs_group WHERE name = '믿음 GBS'), (SELECT id FROM users WHERE name = '조원1'), '2025-01-01'),
    ((SELECT id FROM gbs_group WHERE name = '믿음 GBS'), (SELECT id FROM users WHERE name = '조원2'), '2025-01-01'),
    ((SELECT id FROM gbs_group WHERE name = '믿음 GBS'), (SELECT id FROM users WHERE name = '조원3'), '2025-01-01'); 