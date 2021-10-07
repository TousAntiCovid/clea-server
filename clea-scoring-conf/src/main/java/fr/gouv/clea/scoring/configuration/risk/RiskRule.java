package fr.gouv.clea.scoring.configuration.risk;

import fr.gouv.clea.scoring.configuration.ScoringRule;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

@Valid
@SuperBuilder
@ToString(callSuper = true)
@Getter
public class RiskRule extends ScoringRule {

    @Positive
    private int clusterThresholdBackward;

    @Positive
    private int clusterThresholdForward;

    @Positive
    private float riskLevelBackward;

    @Positive
    private float riskLevelForward;

}
