package fr.gouv.clea.ws.controller.v2;

import fr.gouv.clea.ws.api.v2.DefaultApi;
import fr.gouv.clea.ws.api.v2.model.ManualReportRequest;
import fr.gouv.clea.ws.api.v2.model.ReportResponse;
import fr.gouv.clea.ws.service.ReportService;
import fr.gouv.clea.ws.service.model.Visit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static java.lang.Integer.toUnsignedLong;
import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
public class ManualReportController implements DefaultApi {

    private final ReportService reportService;

    @Override
    public ResponseEntity<ReportResponse> manualReport(final ManualReportRequest manualReportRequest) {
        final var visits = manualReportRequest.getVisits()
                .stream()
                .map(this::toVisitNullSafe)
                .collect(toList());

        final var acceptedVisits = reportService.reportWithoutPivotDate(visits);

        return ResponseEntity.ok(
                ReportResponse.builder()
                        .accepted(toUnsignedLong(acceptedVisits))
                        .rejected(toUnsignedLong(manualReportRequest.getVisits().size() - acceptedVisits))
                        .build()
        );
    }

    private Visit toVisitNullSafe(final fr.gouv.clea.ws.api.v2.model.Visit visit) {
        return visit == null ? null
                : new Visit(visit.getEncryptedLocationSpecificPart(), visit.getScanTime());
    }
}
