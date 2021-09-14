package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.configuration.CleaWsProperties;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.model.Visit;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.LocationSpecificPartEncoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
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

    private ReportService reportService;

    private Instant now;

    @BeforeEach
    void init() {
        now = Instant.now().truncatedTo(SECONDS);

        cleaWsProperties.setRetentionDurationInDays(14);
        cleaWsProperties.setExposureTimeUnitInSeconds(Duration.ofMinutes(30).getSeconds());
        cleaWsProperties.setDuplicateScanThresholdInSeconds(Duration.ofHours(3).getSeconds());

        reportService = new ReportService(cleaWsProperties, new LocationSpecificPartDecoder(), producerService);
    }

    @Test
    void can_process_a_valid_report() {
        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", now.minus(2, DAYS)), // pass
                newVisit("22222222-2222-2222-2222-222222222222", now.minus(1, DAYS)), // pass
                newVisit("33333333-3333-3333-3333-333333333333", now) // pass
        );

        final var acceptedCount = reportService.reportWithPivotDate(now.minus(5, DAYS), visits);

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

        final var acceptedCount = reportService.reportWithPivotDate(now.minus(5, DAYS), visits);

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
    }

    @Test
    void an_outdated_visit_is_not_processed() {
        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", now.minus(15, DAYS)), // don't pass
                newVisit("22222222-2222-2222-2222-222222222222", now.minus(14, DAYS)), // pass
                newVisit("33333333-3333-3333-3333-333333333333", now.minus(2, DAYS)), // pass
                newVisit("44444444-4444-4444-4444-444444444444", now) // pass
        );

        final var acceptedCount = reportService.reportWithPivotDate(now.minus(5, DAYS), visits);

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
    }

    @Test
    void a_visit_in_the_future_of_a_few_seconds_is_not_processed_2() {
        final var visits = List.of(
                newVisit("11111111-1111-1111-1111-111111111111", now), // pass
                newVisit("22222222-2222-2222-2222-222222222222", now.plus(2, SECONDS)) // don't pass
        );

        final var acceptedCount = reportService.reportWithPivotDate(now.minus(5, DAYS), visits);

        verify(producerService).produceVisits(acceptedVisits.capture());
        assertThat(acceptedVisits.getValue())
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .extracting(UUID::toString)
                .containsExactly("11111111-1111-1111-1111-111111111111")
                .doesNotContain("22222222-2222-2222-2222-222222222222");
        assertThat(acceptedCount)
                .isEqualTo(1);
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

        final var acceptedCount = reportService.reportWithPivotDate(now.minus(5, DAYS), visits);

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

}
