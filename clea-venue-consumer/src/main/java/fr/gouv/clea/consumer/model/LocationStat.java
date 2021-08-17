package fr.gouv.clea.consumer.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;

// no @Data so "key" fields are read-only
// @Builder on constructor without id so it can be consistently generated from
// "key" fields
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "health-clealocations-*", createIndex = false)
public class LocationStat {

    @Id
    private String id;

    private SeqNoPrimaryTerm optimisticConcurrencyLock;

    @Field(name = "@timestamp", type = Date)
    private Instant periodStart;

    private int venueType;

    private int venueCategory1;

    private int venueCategory2;

    @With
    private long backwardVisits;

    @With
    private long forwardVisits;

    @Builder
    public LocationStat(Instant periodStart, int venueType, int venueCategory1, int venueCategory2, long backwardVisits,
            long forwardVisits) {
        this(null, null, periodStart, venueType, venueCategory1, venueCategory2, backwardVisits, forwardVisits);
        id = buildId();
    }

    public String buildId() {
        final var stringPeriodStart = periodStart
                .atOffset(UTC)
                .format(ISO_DATE_TIME);
        return String.format("%s-vt:%d-vc1:%d-vc2:%d", stringPeriodStart, venueType, venueCategory1, venueCategory2);
    }

    public String buildIndexName() {
        final var date = periodStart.atOffset(UTC)
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        return String.format("health-clealocations-%s", date);
    }

    public LocationStat withOneMoreBackwardVisit() {
        return withBackwardVisits(backwardVisits + 1);
    }

    public LocationStat withOneMoreForwardVisit() {
        return withForwardVisits(forwardVisits + 1);
    }
}
