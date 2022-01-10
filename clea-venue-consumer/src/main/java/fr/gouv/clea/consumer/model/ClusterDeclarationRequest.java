package fr.gouv.clea.consumer.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Data
public class ClusterDeclarationRequest {

    @NotNull
    private URL deeplink;

    @NotNull
    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime dateTime;

    private ZoneId zoneId;

}
