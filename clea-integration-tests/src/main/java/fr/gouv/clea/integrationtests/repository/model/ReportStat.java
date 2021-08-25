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
@Document(indexName = "health-cleareports-*", createIndex = false)
public class ReportStat {

    @Id
    String id;

    @Field(name = "@timestamp", type = Date)
    Instant timestamp;

    int reported;

    int rejected;

    int backwards;

    int forwards;

    int close;
}
