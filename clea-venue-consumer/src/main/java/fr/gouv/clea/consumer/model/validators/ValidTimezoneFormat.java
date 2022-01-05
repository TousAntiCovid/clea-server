package fr.gouv.clea.consumer.model.validators;

import javax.validation.Constraint;
import javax.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Constraint(validatedBy = { ValidTimezoneFormatValidator.class })
@Target({ FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimezoneFormat {

    String message() default "Timezone format is incvalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
