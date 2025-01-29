-- Create the users table
CREATE TABLE users (
                     id SERIAL PRIMARY KEY,
                     username VARCHAR(50) NOT NULL UNIQUE,
                     email VARCHAR(100) NOT NULL UNIQUE,
                     password_hash VARCHAR(255) NOT NULL,
                     date_of_birth DATE NOT NULL,
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create the sessions table
CREATE TABLE sessions (
                        id SERIAL PRIMARY KEY,
                        user_id INT REFERENCES users(id) ON DELETE CASCADE,
                        session_token VARCHAR(255) NOT NULL UNIQUE,
                        expires_at TIMESTAMP NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create the password_reset_tokens table
CREATE TABLE password_reset_tokens (
                                     id SERIAL PRIMARY KEY,
                                     user_id INT REFERENCES users(id) ON DELETE CASCADE,
                                     token VARCHAR(255) NOT NULL UNIQUE,
                                     expires_at TIMESTAMP NOT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create the chats table
CREATE TABLE chats (
                     id SERIAL PRIMARY KEY,
                     user_id INT REFERENCES users(id) ON DELETE CASCADE,
                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create the messages table
CREATE TABLE messages (
                        id SERIAL PRIMARY KEY,
                        chat_id INT REFERENCES chats(id) ON DELETE CASCADE,
                        sender VARCHAR(50) NOT NULL,
                        message TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
