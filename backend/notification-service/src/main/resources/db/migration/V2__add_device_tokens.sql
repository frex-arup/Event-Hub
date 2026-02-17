-- FCM device token storage for Firebase push notifications

CREATE TABLE IF NOT EXISTS device_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    token       VARCHAR(512) NOT NULL,
    device_type VARCHAR(20) NOT NULL DEFAULT 'WEB',
    device_name VARCHAR(100),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, token)
);

CREATE INDEX idx_device_tokens_user_active ON device_tokens(user_id, active);
CREATE INDEX idx_device_tokens_token ON device_tokens(token);
