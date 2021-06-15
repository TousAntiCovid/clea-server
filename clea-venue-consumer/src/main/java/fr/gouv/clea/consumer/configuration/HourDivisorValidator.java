package fr.gouv.clea.consumer.configuration;

import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class HourDivisorValidator implements ConstraintValidator<HourDivisor, Long> {

    @Override
    public boolean isValid(Long aLong, ConstraintValidatorContext constraintValidatorContext) {
        return 3600 % aLong == 0;
    }
}
