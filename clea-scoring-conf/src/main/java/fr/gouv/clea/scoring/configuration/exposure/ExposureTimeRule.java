package fr.gouv.clea.scoring.configuration.exposure;

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
public class ExposureTimeRule extends ScoringRule {

    @Positive
    private int exposureTimeBackward;

    @Positive
    private int exposureTimeForward;

    @Positive
    private int exposureTimeStaffBackward;

    @Positive
    private int exposureTimeStaffForward;

}
