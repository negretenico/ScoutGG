CREATE TABLE riot_data (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    VARCHAR(255) NOT NULL,
    match_id    VARCHAR(255) NOT NULL,
    player_id   VARCHAR(255) NOT NULL,
    team_id     VARCHAR(255),
    kills       INT         NOT NULL,
    deaths      INT         NOT NULL,
    assists     INT         NOT NULL,
    cs          INT         NOT NULL,
    pulled_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_riot_data_match_id ON riot_data (match_id);
CREATE INDEX idx_riot_data_event_id ON riot_data (event_id);
