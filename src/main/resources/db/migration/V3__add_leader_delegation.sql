CREATE TABLE leader_delegation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delegator_id BIGINT NOT NULL,
    delegate_id BIGINT NOT NULL,
    gbs_group_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (delegator_id) REFERENCES user(id),
    FOREIGN KEY (delegate_id) REFERENCES user(id),
    FOREIGN KEY (gbs_group_id) REFERENCES gbs_group(id)
); 