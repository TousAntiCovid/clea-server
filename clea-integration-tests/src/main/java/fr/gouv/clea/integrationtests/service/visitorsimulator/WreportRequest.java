package fr.gouv.clea.integrationtests.service.visitorsimulator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WreportRequest {

    private Long pivotDate;

    private List<Visit> visits;

}
