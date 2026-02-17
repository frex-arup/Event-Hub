-- Event Service Schema

CREATE TABLE IF NOT EXISTS venues (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    address     TEXT NOT NULL,
    city        VARCHAR(100) NOT NULL,
    country     VARCHAR(100) NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    capacity    INT NOT NULL DEFAULT 0,
    created_by  UUID NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS venue_layouts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_id        UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    layout_json     JSONB NOT NULL,
    canvas_width    INT NOT NULL DEFAULT 800,
    canvas_height   INT NOT NULL DEFAULT 600,
    is_template     BOOLEAN NOT NULL DEFAULT FALSE,
    template_type   VARCHAR(50),
    version         INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    cover_image_url VARCHAR(512),
    start_date      TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date        TIMESTAMP WITH TIME ZONE NOT NULL,
    venue_id        UUID REFERENCES venues(id),
    layout_id       UUID REFERENCES venue_layouts(id),
    organizer_id    UUID NOT NULL,
    total_seats     INT NOT NULL DEFAULT 0,
    available_seats INT NOT NULL DEFAULT 0,
    min_price       DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_price       DECIMAL(10,2) NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS event_tags (
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    tag         VARCHAR(100) NOT NULL,
    PRIMARY KEY (event_id, tag)
);

CREATE TABLE IF NOT EXISTS event_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    speaker     VARCHAR(255),
    start_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time    TIMESTAMP WITH TIME ZONE NOT NULL,
    room        VARCHAR(100),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_start_date ON events(start_date);
CREATE INDEX idx_events_venue ON events(venue_id);
CREATE INDEX idx_venue_layouts_venue ON venue_layouts(venue_id);
CREATE INDEX idx_event_sessions_event ON event_sessions(event_id);

-- Full-text search index
CREATE INDEX idx_events_search ON events USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));
