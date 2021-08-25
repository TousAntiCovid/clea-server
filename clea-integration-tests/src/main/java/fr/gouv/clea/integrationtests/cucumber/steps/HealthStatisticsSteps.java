package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.repository.LocationStatRepository;
import fr.gouv.clea.integrationtests.repository.ReportStatRepository;
import fr.gouv.clea.integrationtests.repository.model.LocationStat;
import fr.gouv.clea.integrationtests.repository.model.ReportStat;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

@RequiredArgsConstructor
public class HealthStatisticsSteps {

    private final ReportStatRepository reportStatRepository;

    private final LocationStatRepository locationStatRepository;

    @Then("statistics by location are")
    public void statisticsByLocationAre(List<LocationStat> expectedLocationStatContent) {
        await("location statistics records")
                .atMost(10, SECONDS)
                .pollInterval(fibonacci())
                .untilAsserted(
                        () -> assertThat(locationStatRepository.findAll())
                                .usingElementComparatorIgnoringFields("id")
                                .containsExactlyInAnyOrderElementsOf(expectedLocationStatContent)
                );
    }

    @Then("statistics by wreport are")
    public void statisticsByWreportAre(List<ReportStat> expectedReportStatContent) {
        await("report statistics records")
                .atMost(10, SECONDS)
                .pollInterval(fibonacci())
                .untilAsserted(
                        () -> assertThat(reportStatRepository.findAll())
                                .usingElementComparatorIgnoringFields("id", "timestamp")
                                .containsExactlyInAnyOrderElementsOf(expectedReportStatContent)
                );
    }
}
