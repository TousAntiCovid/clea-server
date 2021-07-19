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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "health-cleareports-#{T(java.time.LocalDate).now().toString().replace('-', '.')}")
public class ReportStatEntity {

    @Id
    private String id;

    private int reported;

    private int rejected;

    private int backwards;

    private int forwards;

    // même slot => closed (proximité de moins de 30min d'écart)
    private int close;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private Instant timestamp;
}
