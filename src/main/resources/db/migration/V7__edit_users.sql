ALTER TABLE users
ADD COLUMN village_id BIGINT,
ADD CONSTRAINT fk_users_village FOREIGN KEY (village_id) REFERENCES village(id); 