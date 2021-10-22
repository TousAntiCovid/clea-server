
-- Need for a Primary-key (cf TAC-750)
DROP INDEX statloc_period;

ALTER TABLE stat_location ADD CONSTRAINT statloc_pk PRIMARY KEY (period, venue_type,venue_category1,venue_category2);

