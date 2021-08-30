package fr.gouv.clea.ws.controller.v2.exception;

import fr.gouv.clea.ws.api.v2.model.ErrorResponse;
import fr.gouv.clea.ws.api.v2.model.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@ControllerAdvice
@Slf4j
public class CleaRestExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String ERROR_MESSAGE_TEMPLATE = "%s, requested uri: %s";

    @ExceptionHandler(CleaBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleCleaBadRequestException(CleaBadRequestException ex,
            WebRequest webRequest) {
        log.error(String.format(ERROR_MESSAGE_TEMPLATE, ex.getLocalizedMessage(), webRequest.getDescription(false)));
        return ResponseEntity.badRequest()
                .body(
                        new ErrorResponse(
                                ex.getLocalizedMessage(),
                                ex.getValidationErrors()
                        )
                );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error(String.format(ERROR_MESSAGE_TEMPLATE, ex.getLocalizedMessage(), request.getDescription(false)));
        ErrorResponse error = new ErrorResponse(
                ex.getLocalizedMessage().split(":")[0],
                List.of()
        );
        return new ResponseEntity<>(error, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error(String.format(ERROR_MESSAGE_TEMPLATE, ex.getLocalizedMessage(), request.getDescription(false)));
        return ResponseEntity.badRequest()
                .body(
                        new ErrorResponse(
                                "Invalid request",
                                Stream.concat(
                                        ex.getFieldErrors().stream()
                                                .map(
                                                        fieldError -> ValidationError.builder()
                                                                ._object(fieldError.getObjectName())
                                                                .field(fieldError.getField())
                                                                .rejectedValue(fieldError.getRejectedValue())
                                                                .message(fieldError.getDefaultMessage())
                                                                .build()
                                                ),
                                        ex.getGlobalErrors().stream()
                                                .map(
                                                        globalError -> ValidationError.builder()
                                                                ._object(globalError.getObjectName())
                                                                .message(globalError.getDefaultMessage())
                                                                .build()
                                                )
                                ).collect(toList())
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtherException(Exception ex, WebRequest webRequest) {
        final HttpStatus status = getHttpStatus(ex);
        log.error(
                String.format(ERROR_MESSAGE_TEMPLATE, ex.getLocalizedMessage(), webRequest.getDescription(false)), ex
        );
        return ResponseEntity
                .status(status)
                .body(this.exceptionToApiError(ex, status));
    }

    private HttpStatus getHttpStatus(Exception ex) {
        final ResponseStatus responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        return responseStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : responseStatus.value();
    }

    private ErrorResponse exceptionToApiError(Exception ex, HttpStatus status) {
        return new ErrorResponse(
                ex.getLocalizedMessage(),
                List.of()
        );
    }
}
