package fr.gouv.clea.integrationtests.cucumber;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.HOURS;
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

    @Nested
    class DurationExpression {

        @Test
        void can_parse_duration() {
            assertThat(parameterTypes.duration("76 hours"))
                    .isEqualTo(Duration.of(76, HOURS));
        }

        @ParameterizedTest(name = "\"{0}\" should raise an exception")
        @ValueSource(strings = {
                "zz hours",
                "10 unknown",
                "",
                "10",
                "hours",
                "-10"
        })
        void should_raise_error_on_invalid_expression(String durationExpression) {
            assertThatThrownBy(() -> parameterTypes.duration(durationExpression))
                    .hasMessage("'%s' is not a valid duration expression", durationExpression);
        }
    }
}
