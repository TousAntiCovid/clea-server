package fr.gouv.clea.integrationtests.dto;

import fr.gouv.clea.integrationtests.model.Visit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WreportRequest implements Request {

    private Long pivotDate;

    private List<Visit> visits;


}
