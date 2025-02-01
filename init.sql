-- Create the users table
CREATE TABLE users (
                     id SERIAL PRIMARY KEY,
                     username VARCHAR(50) NOT NULL UNIQUE,
                     email VARCHAR(100) NOT NULL UNIQUE,
                     password_hash VARCHAR(255) NOT NULL,
                     date_of_birth DATE NOT NULL,
                     is_admin BOOLEAN DEFAULT FALSE,
                     can_login BOOLEAN DEFAULT FALSE,
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
                                     used BOOLEAN DEFAULT FALSE,
                                     expires_at TIMESTAMP NOT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create the chats table
CREATE TABLE chats (
                     id SERIAL PRIMARY KEY,
                     user_id INT REFERENCES users(id) ON DELETE CASCADE,
                     description TEXT NOT NULL,
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

-- Create the account_requests table
CREATE TABLE account_requests (
                                id SERIAL PRIMARY KEY,
                                user_id INT REFERENCES users(id) ON DELETE CASCADE,
                                approved BOOLEAN DEFAULT FALSE,
                                requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                approved_at TIMESTAMP DEFAULT NULL,
                                status VARCHAR(50) DEFAULT 'PENDING'
);

CREATE TABLE support_banners (
                               id SERIAL PRIMARY KEY,
                               title VARCHAR(255) NOT NULL,
                               content TEXT NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create trigger function
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to support_banners table
CREATE TRIGGER set_timestamp
  BEFORE UPDATE ON support_banners
  FOR EACH ROW
  EXECUTE FUNCTION update_timestamp();

INSERT INTO support_banners (title, content)
VALUES (
         'Contact Support Team',
         'If you need assistance, please call us at <strong>0800 123 456</strong> or email <a href="mailto:support@example.com">support@example.com</a>.'
       );
