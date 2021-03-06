package fr.gouv.clea.integrationtests.cucumber;

import io.cucumber.java.ParameterType;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class ParameterTypes {

    @ParameterType(".*")
    public Instant naturalTime(final String timeExpression) {
        final var dates = new PrettyTimeParser()
                .parse(timeExpression);
        if (dates.size() != 1) {
            final var message = format(
                    "Expecting to find exactly 1 date expression but found %d in '%s'",
                    dates.size(), timeExpression
            );
            throw new IllegalArgumentException(message);
        }
        return dates.stream()
                .findAny()
                .orElseThrow()
                .toInstant();
    }

    @ParameterType("(\\d+) (days|hours|minutes)")
    public Duration duration(final String amountExpression, final String unitExpression) {
        final var amount = Integer.parseInt(amountExpression);
        final var unit = ChronoUnit.valueOf(unitExpression.toUpperCase());
        return Duration.of(amount, unit);
    }

    @ParameterType(".*")
    public List<String> wordList(final String words) {
        return Arrays.asList(words.split("\\s*,\\s*|\\s*and\\s*"));
    }
}
