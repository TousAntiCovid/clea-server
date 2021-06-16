package fr.gouv.clea.consumer.configuration;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HourDivisorValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface HourDivisor {
    String message() default "Variable value must divide 3600 with a quotient equal to 0";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
