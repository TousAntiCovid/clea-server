package fr.gouv.clea.consumer.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import java.time.ZoneId;

public class ValidTimezoneFormatValidator implements ConstraintValidator<ValidTimezoneFormat, ZoneId> {

    @Override
    public boolean isValid(ZoneId value, ConstraintValidatorContext context) {

        return ZoneId.getAvailableZoneIds().contains(value.toString());
    }

}
