package fr.gouv.clea.ws.test;

import org.exparity.hamcrest.date.ZonedDateTimeMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import java.time.ZonedDateTime;

public class TemporalMatchers {

    /**
     * Hamcrest matcher to verify a string representation of a datetime is between
     * now and 10 seconds ago.
     */
    public static Matcher<String> isStringDateBetweenNowAndTenSecondsAgo() {
        final var dateTimeBetweenNowAndOneSecondAgo = isBetweenNowAndTenSecondsAgo();
        return new TypeSafeMatcher<>() {

            @Override
            protected boolean matchesSafely(String value) {
                final var actualDate = ZonedDateTime.parse(value);
                return dateTimeBetweenNowAndOneSecondAgo.matches(actualDate);
            }

            @Override
            public void describeTo(Description description) {
                dateTimeBetweenNowAndOneSecondAgo.describeTo(description);
            }
        };
    }

    /**
     * Hamcrest matcher to verify a {@link ZonedDateTime} is between now and 10
     * seconds ago.
     */
    private static Matcher<ZonedDateTime> isBetweenNowAndTenSecondsAgo() {
        return Matchers.allOf(
                ZonedDateTimeMatchers.sameOrAfter(ZonedDateTime.now().minusSeconds(10)),
                ZonedDateTimeMatchers.sameOrBefore(ZonedDateTime.now())
        );
    }
}
