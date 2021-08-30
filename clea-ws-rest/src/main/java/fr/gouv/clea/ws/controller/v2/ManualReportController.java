package fr.gouv.clea.ws.controller.v2;

import fr.gouv.clea.ws.api.v2.DefaultApi;
import fr.gouv.clea.ws.api.v2.model.ManualReportRequest;
import fr.gouv.clea.ws.api.v2.model.ReportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ManualReportController implements DefaultApi {

    @Override
    public ResponseEntity<ReportResponse> manualReport(final ManualReportRequest manualReportRequest) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
