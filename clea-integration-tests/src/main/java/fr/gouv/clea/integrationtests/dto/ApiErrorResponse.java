package fr.gouv.clea.integrationtests.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class ApiErrorResponse {

    private int httpStatus;

    private Date timestamp;

    private String message;

    private List<ValidationError> validationErrors;

}
