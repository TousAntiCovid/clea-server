-- Needs: Clea-Statistiques
CREATE TABLE stat_reports
(
    id        VARCHAR(64) NOT NULL DEFAULT uuid_generate_v4(),
    backwards INT         NOT NULL,
    is_closed INT         NOT NULL,
    forwards  INT         NOT NULL,
    rejected  INT         NOT NULL,
    reported  INT         NOT NULL,
    dt_report TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT stat_reports_pkey PRIMARY KEY (id)
);
