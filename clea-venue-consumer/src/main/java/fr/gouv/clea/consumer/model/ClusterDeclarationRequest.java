package fr.gouv.clea.consumer.model;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class ClusterDeclarationRequest {

    @NotEmpty(message = "Deeplink cannot be empty.")
    private String deeplink;

    @NotEmpty(message = "Date cannot be empty.")
    private String date;
}
