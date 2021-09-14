package fr.gouv.clea.ws.controller.v1;

import fr.gouv.clea.ws.api.v1.CleaApi;
import fr.gouv.clea.ws.api.v1.model.ReportRequest;
import fr.gouv.clea.ws.api.v1.model.ReportResponse;
import fr.gouv.clea.ws.api.v1.model.ValidationError;
import fr.gouv.clea.ws.controller.v1.exception.CleaBadRequestException;
import fr.gouv.clea.ws.service.ReportService;
import fr.gouv.clea.ws.service.model.Visit;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@RestController
@RequestMapping(path = "/api/clea/v1")
@RequiredArgsConstructor
@Slf4j
public class CleaController implements CleaApi {

    private final ReportService reportService;

    @Override
    public ResponseEntity<ReportResponse> reportUsingPOST(ReportRequest reportRequest) {
        nonNullPivotDateOrThrowBadRequest(reportRequest);
        nonEmptyVisitsOrThrowBadRequest(reportRequest);

        final var pivotDate = TimeUtils.instantFromTimestamp(reportRequest.getPivotDate());
        final var visits = reportRequest.getVisits()
                .stream()
                .map(this::toVisitNullSafe)
                .collect(toList());

        final var acceptedVisits = reportService.reportWithPivotDate(pivotDate, visits);
        final var message = String.format("%d/%d accepted visits", acceptedVisits, reportRequest.getVisits().size());
        log.info(message);

        return ResponseEntity.ok(
                ReportResponse.builder()
                        .success(true)
                        .message(message)
                        .build()
        );
    }

    private Visit toVisitNullSafe(fr.gouv.clea.ws.api.v1.model.Visit visit) {
        return visit == null ? null : new Visit(visit.getQrCode(), visit.getQrCodeScanTime());
    }

    private void nonNullPivotDateOrThrowBadRequest(ReportRequest reportRequest) {
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

    private void nonEmptyVisitsOrThrowBadRequest(ReportRequest reportRequest) {
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
