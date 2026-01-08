CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workouts (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id BIGINT,
                          workout_type VARCHAR(50),
                          duration_minutes INT,
                          FOREIGN KEY (user_id) REFERENCES users(id)
);