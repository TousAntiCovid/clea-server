package fr.gouv.clea.ws.controller.v1.exception;

import fr.gouv.clea.ws.api.v1.model.ValidationError;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class CleaBadRequestException extends RuntimeException {

    @Getter
    private final List<ValidationError> validationErrors;

    public CleaBadRequestException(String message) {
        super(message);
        this.validationErrors = Collections.emptyList();
    }

    public CleaBadRequestException(ValidationError error) {
        super("Invalid request");
        validationErrors = List.of(error);
    }
}
