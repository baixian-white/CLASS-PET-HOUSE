BEGIN;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_is_admin ON users(is_admin);

COMMENT ON COLUMN users.is_admin IS '是否为系统管理员账号，拥有后台管理权限。';

UPDATE users
SET is_admin = TRUE
WHERE username = 'admin';

INSERT INTO users (
    username,
    password_hash,
    activation_code,
    is_activated,
    is_admin,
    settings
)
SELECT
    'admin',
    '$2b$10$6SAtk78Q.aQzItu/90OEwOaDPIXwIwkbuOE9TqxxB5U69vNbyDbna',
    NULL,
    TRUE,
    TRUE,
    '{}'::jsonb
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

COMMIT;
