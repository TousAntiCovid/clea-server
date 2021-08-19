package fr.gouv.clea.integrationtests.dto;

import fr.gouv.clea.integrationtests.model.Visit;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PivotDateStringWreportRequest {

    private String pivotDate;

    private List<Visit> visits;
}
