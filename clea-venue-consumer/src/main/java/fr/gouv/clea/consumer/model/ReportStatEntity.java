package fr.gouv.clea.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;
import static org.springframework.data.elasticsearch.annotations.FieldType.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "health-cleareports-*", createIndex = false)
public class ReportStatEntity {

    @Id
    private String id;

    @Field(name = "@timestamp", type = Date)
    private Instant timestamp;

    private int reported;

    private int rejected;

    private int backwards;

    private int forwards;

    // même slot => closed (proximité de moins de 30min d'écart)
    private int close;

    public String buildIndexName() {
        final var date = timestamp.atOffset(UTC)
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        return String.format("health-cleareports-%s", date);
    }
}
