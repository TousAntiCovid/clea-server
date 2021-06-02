CREATE TABLE exposed_visits (
    id                VARCHAR(64) NOT NULL DEFAULT random_uuid(),
    ltid              UUID      NOT NULL,
	venue_type        INT       NOT NULL,
	venue_category1   INT       NOT NULL,
	venue_category2   INT       NOT NULL,
	period_start      BIGINT    NOT NULL,
	timeslot          INT       NOT NULL,
	backward_visits   BIGINT    NOT NULL DEFAULT 0,
	forward_visits    BIGINT    NOT NULL DEFAULT 0,

	created_at        TIMESTAMP NOT NULL DEFAULT now(),
	updated_at        TIMESTAMP NOT NULL DEFAULT now(),

	PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS exposed_visits_ltidperiodslots ON exposed_visits (ltid, period_start, timeslot);


-- Needs: Clea-Batch
CREATE TABLE cluster_periods
(
    id                          VARCHAR(64) NOT NULL DEFAULT random_uuid(),
    ltid                        UUID        NOT NULL,
    venue_type                  INT         NOT NULL,
    venue_category1             INT         NOT NULL,
    venue_category2             INT         NOT NULL,
    period_start                BIGINT      NOT NULL,
    first_timeslot             INT         NOT NULL,
    last_timeslot              INT         NOT NULL,
    cluster_start               BIGINT      NOT NULL,
    cluster_duration_in_seconds INT         NOT NULL,
    risk_level                  REAL        NOT NULL,

    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS cluster_periods_ltid ON cluster_periods (ltid);

-- Needs: Clea-Statistiques
CREATE TABLE stat_location
(
    period           TIMESTAMP(0) WITH TIME ZONE NOT NULL,
    venue_type       INT NOT NULL,
    venue_category1  INT NOT NULL,
    venue_category2  INT NOT NULL,
    backward_visits  BIGINT NOT NULL,
    forward_visits   BIGINT NOT NULL,
    CONSTRAINT statloc_pk PRIMARY KEY (period, venue_type, venue_category1, venue_category2)
);
CREATE INDEX IF NOT EXISTS statloc_venue  ON stat_location(venue_type, venue_category1, venue_category2);

-- Needs: Clea-Statistiques
CREATE TABLE stat_reports
(
    id          BIGINT NOT NULL,
    backwards   INT    NOT NULL,
    close       INT    NOT NULL,
    forwards    INT    NOT NULL,
    rejected    INT    NOT NULL,
    reported    INT    NOT NULL,
    "timestamp" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT stat_reports_pkey PRIMARY KEY (id)
);
