CREATE TABLE team (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)  NOT NULL,
    short_name VARCHAR(20)  NOT NULL,
    logo_url   VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE player (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    team_id     BIGINT      NOT NULL,
    name        VARCHAR(50) NOT NULL,
    position    VARCHAR(20) NOT NULL COMMENT '투수(P)/포수(C)/내야수(IF)/외야수(OF)',
    back_number INT,
    PRIMARY KEY (id),
    FOREIGN KEY (team_id) REFERENCES team (id)
);

CREATE TABLE game (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    game_date    DATE        NOT NULL,
    home_team_id BIGINT      NOT NULL,
    away_team_id BIGINT      NOT NULL,
    home_score   INT         NOT NULL DEFAULT 0,
    away_score   INT         NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED/IN_PROGRESS/FINISHED',
    inning       INT                  DEFAULT 0,
    start_time   VARCHAR(10),
    stadium      VARCHAR(100),
    PRIMARY KEY (id),
    FOREIGN KEY (home_team_id) REFERENCES team (id),
    FOREIGN KEY (away_team_id) REFERENCES team (id)
);

CREATE TABLE game_lineup (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    game_id       BIGINT      NOT NULL,
    player_id     BIGINT      NOT NULL,
    team_id       BIGINT      NOT NULL,
    batting_order INT,
    position      VARCHAR(20),
    is_starter    BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    FOREIGN KEY (game_id)   REFERENCES game (id),
    FOREIGN KEY (player_id) REFERENCES player (id)
);

CREATE TABLE pitcher_stats (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    player_id       BIGINT         NOT NULL,
    season          INT            NOT NULL,
    era             DECIMAL(4, 2)  NOT NULL DEFAULT 0.00,
    whip            DECIMAL(4, 2)  NOT NULL DEFAULT 0.00,
    wins            INT            NOT NULL DEFAULT 0,
    losses          INT            NOT NULL DEFAULT 0,
    saves           INT            NOT NULL DEFAULT 0,
    games           INT            NOT NULL DEFAULT 0,
    innings_pitched DECIMAL(5, 1)  NOT NULL DEFAULT 0.0,
    PRIMARY KEY (id),
    FOREIGN KEY (player_id) REFERENCES player (id),
    UNIQUE KEY uk_pitcher_season (player_id, season)
);

CREATE TABLE batter_stats (
    id        BIGINT        NOT NULL AUTO_INCREMENT,
    player_id BIGINT        NOT NULL,
    season    INT           NOT NULL,
    avg       DECIMAL(4, 3) NOT NULL DEFAULT 0.000,
    ops       DECIMAL(4, 3) NOT NULL DEFAULT 0.000,
    obp       DECIMAL(4, 3) NOT NULL DEFAULT 0.000,
    slg       DECIMAL(4, 3) NOT NULL DEFAULT 0.000,
    at_bats   INT           NOT NULL DEFAULT 0,
    hits      INT           NOT NULL DEFAULT 0,
    home_runs INT           NOT NULL DEFAULT 0,
    rbis      INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (player_id) REFERENCES player (id),
    UNIQUE KEY uk_batter_season (player_id, season)
);

CREATE TABLE pitcher_vs_batter (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    pitcher_id BIGINT        NOT NULL,
    batter_id  BIGINT        NOT NULL,
    at_bats    INT           NOT NULL DEFAULT 0,
    hits       INT           NOT NULL DEFAULT 0,
    avg        DECIMAL(4, 3) NOT NULL DEFAULT 0.000,
    home_runs  INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (pitcher_id) REFERENCES player (id),
    FOREIGN KEY (batter_id)  REFERENCES player (id),
    UNIQUE KEY uk_pitcher_batter (pitcher_id, batter_id)
);
