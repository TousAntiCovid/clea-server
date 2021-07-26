package fr.gouv.clea.integrationtests.model;

import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Value
@Document(indexName = "health-clealocations-#{T(java.time.LocalDate).now().toString().replace('-', '.')}")
public class LocationStat {

    @Id
    String id;

    long backwardVisits;

    long forwardVisits;

    @Field(name = "@period", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    Instant period;

    int venueType;

    int venueCategory1;

    int venueCategory2;
}
