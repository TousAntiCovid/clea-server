package fr.gouv.clea.integrationtests.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CleaApiResponseParserTest {

    static Stream<Arguments> can_extract_rejected_visits_count() {
        return Stream.of(
                arguments("0/0 accepted visits", 0),
                arguments("10/10 accepted visits", 0),
                arguments("5/6 accepted visits", 1),
                arguments("0/22 accepted visits", 22),
                arguments("1/10 accepted visits", 9)
        );
    }

    @ParameterizedTest
    @MethodSource
    void can_extract_rejected_visits_count(String message, int expectedRejectedCount) {
        assertThat(CleaApiResponseParser.getRejectedVisits(message))
                .isEqualTo(expectedRejectedCount);
    }

    @Test
    void return_incoherent_result() {

        assertThat(CleaApiResponseParser.getRejectedVisits("10/5 accepted visits"))
                .isEqualTo(-5);
    }
}
