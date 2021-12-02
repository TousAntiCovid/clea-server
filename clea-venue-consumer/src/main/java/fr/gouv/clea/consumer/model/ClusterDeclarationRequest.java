package fr.gouv.clea.consumer.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class ClusterDeclarationRequest {

    @NotEmpty(message = "deeplink cannot be empty.")
    private String deeplink;

    @NotEmpty(message = "date cannot be empty.")
    private String date;
}
