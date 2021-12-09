package fr.gouv.clea.consumer.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;

import java.net.URL;
import java.time.LocalDateTime;

@Data
public class ClusterDeclarationRequest {

    @NotNull(message = "{deeplink.notempty}")
    private URL deeplink;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;
}
