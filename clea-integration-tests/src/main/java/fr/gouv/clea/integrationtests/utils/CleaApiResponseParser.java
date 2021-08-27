package fr.gouv.clea.integrationtests.utils;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class CleaApiResponseParser {

    private static final Pattern REPORT_MESSAGE_RESPONSE_PATTERN = Pattern.compile("(\\d+)/(\\d+) accepted visits");

    public static int getRejectedVisits(final String message) {
        final var matcher = REPORT_MESSAGE_RESPONSE_PATTERN.matcher(message);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    String.format("message '%s' doesn't match pattern %s", message, REPORT_MESSAGE_RESPONSE_PATTERN));
        }
        final var accepted = Integer.parseInt(matcher.group(1));
        final var total = Integer.parseInt(matcher.group(2));
        return total - accepted;
    }
}
