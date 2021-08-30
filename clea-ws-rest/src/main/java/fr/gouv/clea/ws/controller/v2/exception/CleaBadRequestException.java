package fr.gouv.clea.ws.controller.v2.exception;

import fr.gouv.clea.ws.api.v2.model.ValidationError;
import lombok.Getter;

import java.util.List;

public class CleaBadRequestException extends RuntimeException {

    @Getter
    private final List<ValidationError> validationErrors;

    public CleaBadRequestException(ValidationError error) {
        super("Invalid request");
        validationErrors = List.of(error);
    }
}
