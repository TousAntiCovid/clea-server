package fr.gouv.clea.ws.exception;

import fr.gouv.clea.ws.api.model.ValidationError;
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
