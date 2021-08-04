package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.api.CleaApi;
import fr.gouv.clea.ws.api.model.ReportResponse;
import fr.gouv.clea.ws.exception.CleaBadRequestException;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.utils.BadArgumentsLoggerService;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(path = "/api/clea/v1")
@RequiredArgsConstructor
@Slf4j
public class CleaController implements CleaApi {

    public static final String MALFORMED_VISIT_LOG_MESSAGE = "Filtered out %d malformed visits of %d while Exposure Status Request";

    private final IReportService reportService;

    private final BadArgumentsLoggerService badArgumentsLoggerService;

    private final WebRequest webRequest;

    private final Validator validator;

    @Override
    public ResponseEntity<ReportResponse> reportUsingPOST(fr.gouv.clea.ws.api.model.ReportRequest reportRequest) {
        final var visits = reportRequest.getVisits() == null ? Collections.<Visit>emptyList()
                : reportRequest.getVisits().stream()
                        .map(visit -> new Visit(visit.getQrCode(), visit.getQrCodeScanTime()))
                        .collect(toList());
        final var reportRequestVo = new ReportRequest(
                visits,
                reportRequest.getPivotDate()
        );
        ReportRequest filtered = this.filterReports(reportRequestVo, webRequest);
        List<DecodedVisit> reported = List.of();
        if (!filtered.getVisits().isEmpty()) {
            reported = reportService.report(filtered);
        }
        String message = String.format(
                "%s reports processed, %s rejected", reported.size(),
                reportRequestVo.getVisits().size() - reported.size()
        );
        log.info(message);
        return ResponseEntity.ok(new ReportResponse(message, true));
    }

    private ReportRequest filterReports(ReportRequest report, WebRequest webRequest) {
        Set<ConstraintViolation<ReportRequest>> reportRequestViolations = validator.validate(report);
        if (!reportRequestViolations.isEmpty()) {
            throw new CleaBadRequestException(reportRequestViolations, Set.of());
        } else {
            Set<ConstraintViolation<Visit>> visitViolations = new HashSet<>();
            List<Visit> validVisits = report.getVisits().stream()
                    .filter(
                            visit -> {
                                visitViolations.addAll(validator.validate(visit));
                                if (!visitViolations.isEmpty()) {
                                    this.badArgumentsLoggerService
                                            .logValidationErrorMessage(visitViolations, webRequest);
                                    return false;
                                } else {
                                    return true;
                                }
                            }
                    ).collect(toList());
            if (validVisits.isEmpty()) {
                throw new CleaBadRequestException(Set.of(), visitViolations);
            }
            int nbVisits = report.getVisits().size();
            int nbFilteredVisits = nbVisits - validVisits.size();
            if (nbFilteredVisits > 0) {
                log.warn(String.format(MALFORMED_VISIT_LOG_MESSAGE, nbFilteredVisits, nbVisits));
            }
            return new ReportRequest(validVisits, report.getPivotDateAsNtpTimestamp());
        }
    }
}
