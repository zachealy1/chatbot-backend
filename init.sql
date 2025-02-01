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

-- Insert fake users
INSERT INTO users (username, email, password_hash, date_of_birth, is_admin, can_login)
VALUES
  ('john_doe', 'john.doe@example.com', 'hashedpassword1', '1995-06-15', FALSE, TRUE),
  ('jane_admin', 'jane.admin@example.com', 'hashedpassword2', '1985-09-22', TRUE, TRUE),
  ('mark_smith', 'mark.smith@example.com', 'hashedpassword3', '2000-02-10', FALSE, FALSE),
  ('lisa_brown', 'lisa.brown@example.com', 'hashedpassword4', '1978-12-05', FALSE, TRUE),
  ('tom_jones', 'tom.jones@example.com', 'hashedpassword5', '1965-08-25', FALSE, FALSE);

-- Insert fake sessions
INSERT INTO sessions (user_id, session_token, expires_at)
VALUES
  (1, 'sessiontoken1', NOW() + INTERVAL '24 hours'),
  (2, 'sessiontoken2', NOW() + INTERVAL '24 hours'),
  (4, 'sessiontoken3', NOW() + INTERVAL '24 hours');

-- Insert fake password reset tokens
INSERT INTO password_reset_tokens (user_id, token, expires_at)
VALUES
  (1, 'reset_token_1', NOW() + INTERVAL '10 minutes'),
  (3, 'reset_token_2', NOW() + INTERVAL '10 minutes'),
  (4, 'reset_token_3', NOW() + INTERVAL '10 minutes');

-- Insert fake chats
INSERT INTO chats (user_id, description)
VALUES
  (1, 'Customer Support'),
  (2, 'Technical Issues'),
  (3, 'Billing Inquiry'),
  (4, 'General Inquiry'),
  (5, 'Service Feedback');

-- Insert fake messages
INSERT INTO messages (chat_id, sender, message)
VALUES
  (1, 'user', 'I need help with my order.'),
  (2, 'admin', 'Please describe the issue.'),
  (3, 'user', 'I have been charged twice.'),
  (4, 'admin', 'Let me check that for you.'),
  (5, 'user', 'Great service!');

-- Insert fake account requests
INSERT INTO account_requests (user_id, approved, requested_at, approved_at, status)
VALUES
  (3, FALSE, NOW(), NULL, 'PENDING'),
  (5, TRUE, NOW(), NOW(), 'APPROVED'),
  (4, FALSE, NOW(), NULL, 'PENDING');

-- Insert fake support banners
INSERT INTO support_banners (title, content)
VALUES
  ('Contact Support Team',
   'If you need assistance, please call us at <strong>0800 123 456</strong> or email <a href="mailto:support@example.com">support@example.com</a>.');

