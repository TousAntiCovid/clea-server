package fr.gouv.clea.integrationtests.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CleaApiResponseParser {

    public static int getRejectedVisits(final String message) {
        return Integer.parseInt(message.replaceAll("[^0-9]+", " ").trim().split(" ")[1]);
    }
}
