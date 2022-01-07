package fr.gouv.clea.consumer.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
public class ClusterDeclarationRequest {

    @NotNull
    private URL deeplink;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;

    private ZoneId zoneId;

}
