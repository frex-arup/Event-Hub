-- Event Activities: Polls, Announcements, Check-ins

CREATE TABLE IF NOT EXISTS event_polls (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    author_id   UUID NOT NULL,
    question    TEXT NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    ends_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS poll_options (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id     UUID NOT NULL REFERENCES event_polls(id) ON DELETE CASCADE,
    label       VARCHAR(255) NOT NULL,
    vote_count  INT NOT NULL DEFAULT 0,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS poll_votes (
    poll_id     UUID NOT NULL REFERENCES event_polls(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    option_id   UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (poll_id, user_id)
);

CREATE TABLE IF NOT EXISTS event_announcements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    author_id   UUID NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    priority    VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS session_checkins (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES event_sessions(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    checked_in_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(session_id, user_id)
);

CREATE INDEX idx_polls_event ON event_polls(event_id);
CREATE INDEX idx_poll_options_poll ON poll_options(poll_id);
CREATE INDEX idx_poll_votes_poll ON poll_votes(poll_id);
CREATE INDEX idx_announcements_event ON event_announcements(event_id);
CREATE INDEX idx_checkins_session ON session_checkins(session_id);
CREATE INDEX idx_checkins_user ON session_checkins(user_id);
