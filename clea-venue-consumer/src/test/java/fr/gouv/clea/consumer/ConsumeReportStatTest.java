package fr.gouv.clea.consumer;

import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.gouv.clea.consumer.test.KafkaManager;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static fr.gouv.clea.consumer.test.ElasticManager.assertThatAllDocumentsFromElastic;
import static org.awaitility.Awaitility.await;

@IntegrationTest
public class ConsumeReportStatTest {

    @Test
    void should_send_report_stats_to_elastic() {
        final var instant = Instant.parse("2019-07-22T09:37:42.251Z");
        final var reportStat = ReportStat.builder()
                .reported(10)
                .rejected(2)
                .backwards(5)
                .forwards(3)
                .close(4)
                .timestamp(TimeUtils.ntpTimestampFromInstant(instant))
                .build();

        KafkaManager.whenSendReportStat(reportStat);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                () -> assertThatAllDocumentsFromElastic()
                        .containsExactlyInAnyOrder(
                                Map.of(
                                        "@timestamp", "2019-07-22T09:37:42.000Z",
                                        "reported", 10,
                                        "rejected", 2,
                                        "backwards", 5,
                                        "forwards", 3,
                                        "close", 4
                                )
                        )
        );
    }

}
