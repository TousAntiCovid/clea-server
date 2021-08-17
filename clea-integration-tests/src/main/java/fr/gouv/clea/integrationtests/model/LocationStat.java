package fr.gouv.clea.integrationtests.model;

import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.time.Instant;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;

@Value
@Document(indexName = "health-clealocations")
public class LocationStat {

    @Id
    String id;

    long backwardVisits;

    long forwardVisits;

    @Field(name = "@timestamp", type = Date)
    Instant periodStart;

    int venueType;

    int venueCategory1;

    int venueCategory2;
}
