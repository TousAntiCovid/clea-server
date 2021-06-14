package fr.gouv.clea.integrationtests.model.malformed;

import fr.gouv.clea.model.Visit;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PivotDateTypeReportRequest {

    private String pivotDate;

    private List<Visit> visits;
}
