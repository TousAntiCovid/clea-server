package fr.gouv.clea.integrationtests.cucumber;

import io.cucumber.java.ParameterType;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

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

    @ParameterType(".*")
    public Duration duration(final String durationExpression) {
        try {
            final var scanner = new Scanner(durationExpression);
            final var amount = scanner.nextLong();
            final var unitExpression = scanner.next();
            final var unit = ChronoUnit.valueOf(unitExpression.toUpperCase());
            return Duration.of(amount, unit);
        } catch (Exception e) {
            final var message = format("'%s' is not a valid duration expression", durationExpression);
            throw new IllegalArgumentException(message, e);
        }
    }
}
