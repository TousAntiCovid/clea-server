package fr.gouv.clea.integrationtests.cucumber;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterTypesTest {

    private static final ParameterTypes parameterTypes = new ParameterTypes();

    @Nested
    class NaturalTime {

        @Test
        void can_parse_well_formatted_date_expression() {
            assertThat(parameterTypes.naturalTime("12:30, 6 days ago"))
                    .isEqualTo(
                            ZonedDateTime.now()
                                    .minusDays(6)
                                    .withHour(12)
                                    .withMinute(30)
                                    .truncatedTo(MINUTES)
                                    .toInstant()
                    );
        }

        @Test
        void shloud_raise_error_when_no_date_expression_detected() {
            assertThatThrownBy(() -> parameterTypes.naturalTime("no date expression"))
                    .hasMessage("Expecting to find exactly 1 date expression but found 0 in 'no date expression'");
        }

        @Test
        void should_raise_error_when_more_than_one_date_expression_detected() {
            assertThatThrownBy(() -> parameterTypes.naturalTime("12:30, 6 days ago and 12:30, 6 days ago"))
                    .hasMessage(
                            "Expecting to find exactly 1 date expression but found 2 in '12:30, 6 days ago and 12:30, 6 days ago'"
                    );
        }
    }
}
