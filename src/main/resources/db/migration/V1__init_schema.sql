-- Department table
CREATE TABLE department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Village table
CREATE TABLE village (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB;

-- User table
CREATE TABLE user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    birth_date DATE NULL,
    role ENUM('ADMIN', 'MINISTER', 'VILLAGE_LEADER', 'LEADER', 'MEMBER') NOT NULL,
    email VARCHAR(100) UNIQUE NULL,
    password VARCHAR(100) NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB;

-- GBS Group table
CREATE TABLE gbs_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    village_id BIGINT NOT NULL,
    term_start_dt DATE NOT NULL,
    term_end_dt DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (village_id) REFERENCES village(id)
) ENGINE=InnoDB;

-- Village Leader table
CREATE TABLE village_leader (
    user_id BIGINT NOT NULL,
    village_id BIGINT NOT NULL,
    start_dt DATE NOT NULL,
    end_dt DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (village_id) REFERENCES village(id)
) ENGINE=InnoDB;

-- GBS Leader History table
CREATE TABLE gbs_leader_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gbs_id BIGINT NOT NULL,
    leader_id BIGINT NOT NULL,
    start_dt DATE NOT NULL,
    end_dt DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (gbs_id) REFERENCES gbs_group(id),
    FOREIGN KEY (leader_id) REFERENCES user(id)
) ENGINE=InnoDB;

-- GBS Member History table
CREATE TABLE gbs_member_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gbs_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    start_dt DATE NOT NULL,
    end_dt DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (gbs_id) REFERENCES gbs_group(id),
    FOREIGN KEY (member_id) REFERENCES user(id)
) ENGINE=InnoDB;

-- Leader Delegation table
CREATE TABLE leader_delegation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    delegator_id BIGINT NOT NULL,
    delegatee_id BIGINT NOT NULL,
    gbs_id BIGINT NOT NULL,
    start_dt DATE NOT NULL,
    end_dt DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (delegator_id) REFERENCES user(id),
    FOREIGN KEY (delegatee_id) REFERENCES user(id),
    FOREIGN KEY (gbs_id) REFERENCES gbs_group(id),
    INDEX idx_delegation_delegatee (delegatee_id, gbs_id, end_dt)
) ENGINE=InnoDB;

-- Attendance table
CREATE TABLE attendance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    gbs_id BIGINT NOT NULL,
    week_start DATE NOT NULL,
    worship ENUM('O', 'X') NOT NULL,
    qt_count INT NOT NULL,
    ministry ENUM('A', 'B', 'C') NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (member_id) REFERENCES user(id),
    FOREIGN KEY (gbs_id) REFERENCES gbs_group(id),
    FOREIGN KEY (created_by) REFERENCES user(id),
    INDEX idx_attendance_gbs_week (gbs_id, week_start)
) ENGINE=InnoDB; 