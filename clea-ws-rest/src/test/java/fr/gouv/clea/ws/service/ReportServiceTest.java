package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.configuration.CleaWsProperties;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportStat;
import fr.gouv.clea.ws.service.model.Visit;
import fr.gouv.clea.ws.utils.MetricsService;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.LocationSpecificPartEncoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class ReportServiceTest {

    private final CleaWsProperties cleaWsProperties = new CleaWsProperties();

    @Mock
    private ProducerService producerService;

    @Captor
    private ArgumentCaptor<List<DecodedVisit>> acceptedVisits;

    private MetricsService metricsService;

    private ReportService reportService;

    private Instant now;

    @BeforeEach
    void init() {
        now = Instant.now().truncatedTo(SECONDS);

        cleaWsProperties.setRetentionDurationInDays(14);
        cleaWsProperties.setExposureTimeUnitInSeconds(Duration.ofMinutes(30).getSeconds());
        cleaWsProperties.setDuplicateScanThresholdInSeconds(Duration.ofHours(3).getSeconds());

        metricsService = new MetricsService(new SimpleMeterRegistry());

        reportService = new ReportService(
                cleaWsProperties, new LocationSpecificPartDecoder(), producerService, metricsService
        );
    }

    @Test
    void can_process_a_valid_report() {
        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", now.minus(2, DAYS)), // pass
                newVisit("22222222-2222-2222-2222-222222222222", now.minus(1, DAYS)), // pass
                newVisit("33333333-3333-3333-3333-333333333333", now) // pass
        );

        final var acceptedCount = reportService.report(now.minus(5, DAYS), visits);

        verify(producerService).produceVisits(acceptedVisits.capture());
        assertThat(acceptedVisits.getValue())
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .extracting(UUID::toString)
                .containsExactly(
                        "11111111-1111-1111-1111-111111111111",
                        "22222222-2222-2222-2222-222222222222",
                        "33333333-3333-3333-3333-333333333333"
                );
        assertThat(acceptedCount)
                .isEqualTo(3);
    }

    @Test
    void a_visit_in_the_future_is_not_processed() {
        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", now.minus(1, DAYS)), // pass
                newVisit("22222222-2222-2222-2222-222222222222", now), // pass
                newVisit("33333333-3333-3333-3333-333333333333", now.plus(1, DAYS)) // don't pass
        );

        final var acceptedCount = reportService.report(now.minus(5, DAYS), visits);

        verify(producerService).produceVisits(acceptedVisits.capture());
        assertThat(acceptedVisits.getValue())
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .extracting(UUID::toString)
                .containsExactly(
                        "11111111-1111-1111-1111-111111111111",
                        "22222222-2222-2222-2222-222222222222"
                )
                .doesNotContain("33333333-3333-3333-3333-333333333333");
        assertThat(acceptedCount)
                .isEqualTo(2);

        assertThat(metricsService.getFutureVisitCounter().count()).isEqualTo(1.0);
    }

    @Test
    void an_outdated_visit_is_not_processed() {
        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", now.minus(15, DAYS)), // don't pass
                newVisit("22222222-2222-2222-2222-222222222222", now.minus(14, DAYS)), // pass
                newVisit("33333333-3333-3333-3333-333333333333", now.minus(2, DAYS)), // pass
                newVisit("44444444-4444-4444-4444-444444444444", now) // pass
        );

        final var acceptedCount = reportService.report(now.minus(5, DAYS), visits);

        verify(producerService).produceVisits(acceptedVisits.capture());
        assertThat(acceptedVisits.getValue())
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .extracting(UUID::toString)
                .containsExactly(
                        "22222222-2222-2222-2222-222222222222",
                        "33333333-3333-3333-3333-333333333333",
                        "44444444-4444-4444-4444-444444444444"
                )
                .doesNotContain("11111111-1111-1111-1111-111111111111");
        assertThat(acceptedCount)
                .isEqualTo(3);

        assertThat(metricsService.getOutdatedVisitCounter().count()).isEqualTo(1.0);
    }

    @Test
    void a_visit_in_the_future_of_a_few_seconds_is_not_processed_2() {
        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", now), // pass
                newVisit("22222222-2222-2222-2222-222222222222", now.plus(2, SECONDS)) // don't pass
        );

        final var acceptedCount = reportService.report(now.minus(5, DAYS), visits);

        verify(producerService).produceVisits(acceptedVisits.capture());
        assertThat(acceptedVisits.getValue())
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .extracting(UUID::toString)
                .containsExactly("11111111-1111-1111-1111-111111111111")
                .doesNotContain("22222222-2222-2222-2222-222222222222");
        assertThat(acceptedCount)
                .isEqualTo(1);
        assertThat(metricsService.getFutureVisitCounter().count()).isEqualTo(1.0);
    }

    @Test
    void successive_visits_in_less_than_duplicateThresholdConfig__3h__are_deduplicated() {
        final var nowMinus4h = now.minus(4, HOURS).truncatedTo(SECONDS);
        final var nowMinus3h = now.minus(3, HOURS).truncatedTo(SECONDS);
        final var nowMinus1h = now.minus(1, HOURS).truncatedTo(SECONDS);
        final var nowPlus15m = now.plus(15, MINUTES).truncatedTo(SECONDS);

        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", nowMinus4h), // pass
                newVisit("11111111-1111-1111-1111-111111111111", now), // pass
                newVisit("11111111-1111-1111-1111-111111111111", nowPlus15m), // don't pass (future)
                newVisit("22222222-2222-2222-2222-222222222222", nowMinus3h), // pass
                newVisit("22222222-2222-2222-2222-222222222222", now), // don't pass
                newVisit("22222222-2222-2222-2222-222222222222", nowMinus1h), // don't pass
                newVisit("33333333-3333-3333-3333-333333333333", now), // pass
                newVisit("33333333-3333-3333-3333-333333333333", now), // don't pass (duplicate)
                newVisit("33333333-3333-3333-3333-333333333333", now) // don't pass (duplicate)
        );

        final var acceptedCount = reportService.report(now.minus(5, DAYS), visits);

        verify(producerService).produceVisits(acceptedVisits.capture());
        assertThat(acceptedVisits.getValue())
                .extracting(v -> tuple(v.getLocationTemporaryPublicId().toString(), v.getQrCodeScanTime()))
                .containsExactly(
                        tuple("11111111-1111-1111-1111-111111111111", nowMinus4h),
                        tuple("11111111-1111-1111-1111-111111111111", now),
                        tuple("22222222-2222-2222-2222-222222222222", nowMinus3h),
                        tuple("33333333-3333-3333-3333-333333333333", now)
                );
        assertThat(acceptedCount)
                .isEqualTo(4);
        assertThat(metricsService.getDuplicateVisitCounter().count()).isEqualTo(4.0);
        assertThat(metricsService.getFutureVisitCounter().count()).isEqualTo(1.0);
    }

    private Visit newVisit(String uuid, Instant scanTime) {
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .locationTemporaryPublicId(UUID.fromString(uuid))
                .build();
        byte[] qrCodeHeader = new LocationSpecificPartEncoder().binaryEncodedHeader(lsp);
        byte[] encodedQr = new byte[CleaEciesEncoder.HEADER_BYTES_SIZE + CleaEciesEncoder.MSG_BYTES_SIZE];
        System.arraycopy(qrCodeHeader, 0, encodedQr, 0, qrCodeHeader.length);
        String qrCode = Base64.encodeBase64URLSafeString(encodedQr);
        return new Visit(qrCode, TimeUtils.ntpTimestampFromInstant(scanTime));
    }

    @Nested
    class StatisticsTest {

        @Captor
        ArgumentCaptor<ReportStat> reportStat;

        @Test
        void duplicate_visits_should_increment_rejected_visits_count() {
            final var visits = List.of(
                    newVisit("11111111-1111-1111-1111-111111111111", now.minus(4, HOURS)), // pass
                    newVisit("11111111-1111-1111-1111-111111111111", now), // pass
                    newVisit("22222222-2222-2222-2222-222222222222", now.minus(3, HOURS)), // pass
                    newVisit("22222222-2222-2222-2222-222222222222", now), // don't pass
                    newVisit("33333333-3333-3333-3333-333333333333", now), // pass as a closed with uuid 1 at same time
                    newVisit("33333333-3333-3333-3333-333333333333", now) // don't pass
            );

            reportService.report(now.minus(5, DAYS), visits);

            verify(producerService).produceStat(reportStat.capture());
            assertThat(reportStat.getValue())
                    .hasFieldOrPropertyWithValue("reported", 6)
                    .hasFieldOrPropertyWithValue("rejected", 2)
                    .hasFieldOrPropertyWithValue("forwards", 4)
                    .hasFieldOrPropertyWithValue("backwards", 0)
                    .hasFieldOrPropertyWithValue("close", 1);
        }

        @Test
        void backward_visit_should_increments_backward_visits_count() {
            final var visits = List.of(
                    newVisit("11111111-1111-1111-1111-111111111111", now.minus(1, DAYS))
            );

            reportService.report(now, visits);

            verify(producerService).produceStat(reportStat.capture());
            assertThat(reportStat.getValue())
                    .hasFieldOrPropertyWithValue("reported", 1)
                    .hasFieldOrPropertyWithValue("rejected", 0)
                    .hasFieldOrPropertyWithValue("forwards", 0)
                    .hasFieldOrPropertyWithValue("backwards", 1)
                    .hasFieldOrPropertyWithValue("close", 0);
        }

        @Test
        void forward_visit_should_increments_forward_visits_count() {
            final var pivotDate = now.minus(1, DAYS);
            final var visits = List.of(
                    newVisit("11111111-1111-1111-1111-111111111111", now),
                    newVisit("22222222-2222-2222-2222-222222222222", pivotDate)
            );

            reportService.report(pivotDate, visits);

            verify(producerService).produceStat(reportStat.capture());
            assertThat(reportStat.getValue())
                    .hasFieldOrPropertyWithValue("reported", 2)
                    .hasFieldOrPropertyWithValue("rejected", 0)
                    .hasFieldOrPropertyWithValue("forwards", 2)
                    .hasFieldOrPropertyWithValue("backwards", 0)
                    .hasFieldOrPropertyWithValue("close", 0);
        }

        @Test
        void outdated_visits_should_increments_rejected_visits_count() {
            final var visits = List.of(
                    newVisit("11111111-1111-1111-1111-111111111111", now.minus(15, DAYS)), // don't pass
                    newVisit("22222222-2222-2222-2222-222222222222", now.minus(13, DAYS)), // pass
                    newVisit("33333333-3333-3333-3333-333333333333", now.minus(2, DAYS)), // pass
                    newVisit("44444444-4444-4444-4444-444444444444", now) // pass
            );

            reportService.report(now.minus(20, DAYS), visits);

            verify(producerService).produceStat(reportStat.capture());
            assertThat(reportStat.getValue())
                    .hasFieldOrPropertyWithValue("reported", 4)
                    .hasFieldOrPropertyWithValue("rejected", 1)
                    .hasFieldOrPropertyWithValue("forwards", 3)
                    .hasFieldOrPropertyWithValue("backwards", 0)
                    .hasFieldOrPropertyWithValue("close", 0);
        }

        @Test
        void future_visits_should_increments_rejected_visits_count() {
            final var visits = List.of(
                    newVisit("11111111-1111-1111-1111-111111111111", now), // pass
                    newVisit("22222222-2222-2222-2222-222222222222", now.plus(2, SECONDS)) // don't pass
            );

            reportService.report(now.minus(20, DAYS), visits);

            verify(producerService).produceStat(reportStat.capture());
            assertThat(reportStat.getValue())
                    .hasFieldOrPropertyWithValue("reported", 2)
                    .hasFieldOrPropertyWithValue("rejected", 1)
                    .hasFieldOrPropertyWithValue("forwards", 1)
                    .hasFieldOrPropertyWithValue("backwards", 0)
                    .hasFieldOrPropertyWithValue("close", 0);
        }

        @Test
        void close_visits_should_increments_close_visits_count() {
            final var visits = List.of(
                    newVisit("11111111-1111-1111-1111-111111111111", now.minus(4, DAYS)), // pass
                    newVisit("22222222-2222-2222-2222-222222222222", now.minus(2, DAYS)), // close
                    newVisit("33333333-3333-3333-3333-333333333333", now.minus(2, DAYS).minus(29, MINUTES)) // pass
            );

            reportService.report(now, visits);

            verify(producerService).produceStat(reportStat.capture());
            assertThat(reportStat.getValue())
                    .hasFieldOrPropertyWithValue("reported", 3)
                    .hasFieldOrPropertyWithValue("rejected", 0)
                    .hasFieldOrPropertyWithValue("forwards", 0)
                    .hasFieldOrPropertyWithValue("backwards", 3)
                    .hasFieldOrPropertyWithValue("close", 1);
        }

        @Test
        void many_close_visits_should_increments_close_visits_count() {
            // 2, 3 and 4 are close
            // 6 and 6 are close
            final var visits = List.of(
                    newVisit("11111111-1111-1111-1111-111111111111", now.minus(4, DAYS)),
                    newVisit("22222222-2222-2222-2222-222222222222", now.minus(2, DAYS)),
                    newVisit("33333333-3333-3333-3333-333333333333", now.minus(2, DAYS).minus(29, MINUTES)),
                    newVisit("44444444-4444-4444-4444-444444444444", now.minus(2, DAYS).plus(29, MINUTES)),
                    newVisit("55555555-5555-5555-5555-555555555555", now.minus(1, DAYS)),
                    newVisit("66666666-6666-6666-6666-666666666666", now.minus(1, DAYS).plus(29, MINUTES))
            );

            reportService.report(now.minus(20, DAYS), visits);

            verify(producerService).produceStat(reportStat.capture());
            assertThat(reportStat.getValue())
                    .hasFieldOrPropertyWithValue("reported", 6)
                    .hasFieldOrPropertyWithValue("rejected", 0)
                    .hasFieldOrPropertyWithValue("forwards", 6)
                    .hasFieldOrPropertyWithValue("backwards", 0)
                    .hasFieldOrPropertyWithValue("close", 3);
        }
    }
}
