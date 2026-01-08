ALTER TABLE users
    ADD COLUMN firstname VARCHAR(50) AFTER username,
    ADD COLUMN lastname VARCHAR(50) AFTER firstname;