package fr.gouv.clea.integrationtests.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    private String object;

    private String field;

    private Object rejectedValue;

    private String message;
}
