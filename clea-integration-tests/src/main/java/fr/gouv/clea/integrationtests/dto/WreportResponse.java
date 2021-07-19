package fr.gouv.clea.integrationtests.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WreportResponse {

    private String message;

    private Boolean success;
}
