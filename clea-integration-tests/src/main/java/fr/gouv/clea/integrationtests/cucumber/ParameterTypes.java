package fr.gouv.clea.integrationtests.cucumber;

import io.cucumber.java.ParameterType;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Instant;

public class ParameterTypes {

    @ParameterType(".*")
    public Instant instant(final String naturalLanguage) {
        return new PrettyTimeParser().parse(naturalLanguage).get(0).toInstant();
    }
}
