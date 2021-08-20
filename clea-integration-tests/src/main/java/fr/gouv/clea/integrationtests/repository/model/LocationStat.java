package fr.gouv.clea.integrationtests.repository.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.time.Instant;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;

@Value
@Builder
@Document(indexName = "health-clealocations-*", createIndex = false)
public class LocationStat {

    @Id
    String id;

    @Field(name = "@timestamp", type = Date)
    Instant periodStart;

    int venueType;

    int venueCategory1;

    int venueCategory2;

    long backwardVisits;

    long forwardVisits;
}
