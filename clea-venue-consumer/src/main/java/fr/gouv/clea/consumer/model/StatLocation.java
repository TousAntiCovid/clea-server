package fr.gouv.clea.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "health-clealocations-#{T(java.time.LocalDate).now().toString().replace('-', '.')}")
public class StatLocation {

    @Id
    private String id;

    private long backwardVisits;

    private long forwardVisits;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private Instant periodStart;

    private int venueType;

    private int venueCategory1;

    private int venueCategory2;

}
