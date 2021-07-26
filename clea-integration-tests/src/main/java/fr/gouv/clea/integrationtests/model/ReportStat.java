package fr.gouv.clea.integrationtests.model;

import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Value
@Document(indexName = "health-cleareports-#{T(java.time.LocalDate).now().toString().replace('-', '.')}")
public class ReportStat {

    @Id
    String id;

    int reported;

    int rejected;

    int backwards;

    int forwards;

    int close;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    Instant timestamp;
}
