package fr.gouv.clea.ws.controller.v2;

import fr.gouv.clea.ws.api.v2.WithoutValidationApi;
import fr.gouv.clea.ws.api.v2.model.ReportRequest;
import fr.gouv.clea.ws.api.v2.model.ReportResponse;
import fr.gouv.clea.ws.api.v2.model.ValidationError;
import fr.gouv.clea.ws.controller.v2.exception.CleaBadRequestException;
import fr.gouv.clea.ws.service.ReportService;
import fr.gouv.clea.ws.service.model.Visit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static java.lang.Integer.toUnsignedLong;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WReportController implements WithoutValidationApi {

    private final ReportService reportService;

    @Override
    public ResponseEntity<ReportResponse> wreport(final ReportRequest reportRequest) {
        nonNullPivotDateOrThrowBadRequest(reportRequest);
        nonEmptyVisitsOrThrowBadRequest(reportRequest);

        final var visits = reportRequest.getVisits()
                .stream()
                .map(this::toVisitNullSafe)
                .collect(toList());

        final var acceptedVisits = reportService.reportWithPivotDate(reportRequest.getPivotDate(), visits);

        return ResponseEntity.ok(
                ReportResponse.builder()
                        .accepted(toUnsignedLong(acceptedVisits))
                        .rejected(toUnsignedLong(reportRequest.getVisits().size() - acceptedVisits))
                        .build()
        );
    }

    private Visit toVisitNullSafe(final fr.gouv.clea.ws.api.v2.model.Visit visit) {
        return visit == null ? null
                : new Visit(visit.getEncryptedLocationSpecificPart(), visit.getScanTime());
    }

    private void nonNullPivotDateOrThrowBadRequest(final ReportRequest reportRequest) {
        if (reportRequest.getPivotDate() == null) {
            throw new CleaBadRequestException(
                    ValidationError.builder()
                            .rejectedValue(null)
                            ._object("ReportRequest")
                            .message("must not be null")
                            .field("pivotDate")
                            .build()
            );
        }
    }

    private void nonEmptyVisitsOrThrowBadRequest(final ReportRequest reportRequest) {
        if (isEmpty(reportRequest.getVisits())) {
            throw new CleaBadRequestException(
                    ValidationError.builder()
                            .rejectedValue(reportRequest.getVisits())
                            ._object("ReportRequest")
                            .message("must not be empty")
                            .field("visits")
                            .build()
            );
        }
    }
}
