package fr.gouv.clea.integrationtests.feature;

import fr.gouv.clea.integrationtests.model.LocationStat;
import fr.gouv.clea.integrationtests.model.ReportStat;
import io.cucumber.java.DataTableType;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.util.Map;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class DataDableConverters {

    private final PrettyTimeParser prettyTimeParser = new PrettyTimeParser();

    @DataTableType
    public LocationStat toLocationStat(Map<String, String> entry) {
        assertThat(entry)
                .containsKeys(
                        "venue type", "venue category1", "venue category2", "backward visits", "forward visits",
                        "period start"
                );

        final var periodStart = prettyTimeParser.parse(entry.get("period start"))
                .get(0)
                .toInstant();
        return LocationStat.builder()
                .id(null)
                .periodStart(periodStart)
                .venueType(parseInt(entry.get("venue type")))
                .venueCategory1(parseInt(entry.get("venue category1")))
                .venueCategory2(parseInt(entry.get("venue category2")))
                .backwardVisits(parseInt(entry.get("backward visits")))
                .forwardVisits(parseInt(entry.get("forward visits")))
                .build();
    }

    @DataTableType
    public ReportStat toReportStat(Map<String, String> entry) {
        assertThat(entry)
                .containsKeys("reported", "rejected", "close", "backwards", "forwards");

        return ReportStat.builder()
                .id(null)
                .reported(parseInt(entry.get("reported")))
                .rejected(parseInt(entry.get("rejected")))
                .close(parseInt(entry.get("close")))
                .backwards(parseInt(entry.get("backwards")))
                .forwards(parseInt(entry.get("forwards")))
                .build();
    }
}
