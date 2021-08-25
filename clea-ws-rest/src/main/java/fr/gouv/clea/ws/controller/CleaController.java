package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.api.CleaApi;
import fr.gouv.clea.ws.exception.CleaBadRequestException;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportRequest;
import fr.gouv.clea.ws.model.ReportResponse;
import fr.gouv.clea.ws.model.Visit;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.utils.BadArgumentsLoggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CleaController implements CleaApi {

    public static final String MALFORMED_VISIT_LOG_MESSAGE = "Filtered out %d malformed visits of %d while Exposure Status Request";

    private final IReportService reportService;

    private final BadArgumentsLoggerService badArgumentsLoggerService;

    private final WebRequest webRequest;

    private final Validator validator;

    @Override
    public ResponseEntity<ReportResponse> reportUsingPOST(
            @RequestBody fr.gouv.clea.ws.model.ReportRequest reportRequestVo) {
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
        return new ResponseEntity<>(new ReportResponse(message, true), HttpStatus.CREATED);
    }

    private ReportRequest filterReports(fr.gouv.clea.ws.model.ReportRequest report, WebRequest webRequest) {
        Set<ConstraintViolation<fr.gouv.clea.ws.model.ReportRequest>> reportRequestViolations = validator
                .validate(report);
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
                    ).collect(Collectors.toList());
            if (validVisits.isEmpty()) {
                throw new CleaBadRequestException(Set.of(), visitViolations);
            }
            int nbVisits = report.getVisits().size();
            int nbFilteredVisits = nbVisits - validVisits.size();
            if (nbFilteredVisits > 0) {
                log.warn(String.format(MALFORMED_VISIT_LOG_MESSAGE, nbFilteredVisits, nbVisits));
            }
            return new ReportRequest(report.getPivotDate(), validVisits);
        }
    }
}
