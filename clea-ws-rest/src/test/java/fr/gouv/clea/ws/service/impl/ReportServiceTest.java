package fr.gouv.clea.ws.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IProducerService;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartEncoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
class ReportServiceTest {

    @MockBean
    private IProducerService producerService;

    @Autowired
    private IReportService reportService;

    private Instant now;

    @BeforeEach
    void init() {
        now = Instant.now();
        assertThat(producerService).isNotNull();
        assertThat(reportService).isNotNull();
        doNothing().when(producerService).produceVisits(anyList());
    }

    @Test
    @DisplayName("test successful report with no rejection")
    void can_process_a_valid_report() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now)) /* pass */
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, 0L));

        assertThat(processed)
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .containsExactly(uuid1, uuid2, uuid3);
    }

    @Test
    @DisplayName("test report with non valid qr codes")
    void a_visit_in_the_future_is_not_processed() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.DAYS))) /* don't pass */
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, 0L));

        assertThat(processed)
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .containsExactly(uuid1, uuid2)
                .doesNotContain(uuid3);
    }

    @Test
    @DisplayName("test report with outdated scans")
    void an_outdated_visit_is_not_processed() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS))), // don't pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.minus(14, ChronoUnit.DAYS))), // pass
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                newVisit(uuid4, TimeUtils.ntpTimestampFromInstant(now)) /* pass */
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, 0L));

        assertThat(processed)
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .containsExactly(uuid2, uuid3, uuid4)
                .doesNotContain(uuid1);
    }

    @Test
    @DisplayName("test report with future scans")
    void a_visit_in_the_future_is_not_processed_2() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.plus(2, ChronoUnit.SECONDS))) /* don't pass */
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, 0L));

        assertThat(processed)
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .containsExactly(uuid1)
                .doesNotContain(uuid2);
    }

    @Test
    @DisplayName("test report with duplicated qr codes")
    void successive_visits_in_less_than_duplicateThresholdConfig__3h__are_deduplicated() {
        UUID uuidA = UUID.fromString("60f5ebf7-d2af-4451-a575-7d1a2de7a9fd");
        UUID uuidA2 = UUID.fromString("60f5ebf7-d2af-4451-a575-7d1a2de7a9fd");
        UUID uuidB = UUID.fromString("de4c7b16-d5a2-45fa-a4f4-50fbf1e3880b");
        UUID uuidB2 = UUID.fromString("de4c7b16-d5a2-45fa-a4f4-50fbf1e3880b");
        UUID uuidC = UUID.fromString("bdbf9725-c1ad-42e3-b725-e475272b7f54");
        UUID uuidC2 = UUID.fromString("bdbf9725-c1ad-42e3-b725-e475272b7f54");

        List<Visit> visits = List.of(
                newVisit(uuidA, TimeUtils.ntpTimestampFromInstant(now.minus(4, ChronoUnit.HOURS))), // pass
                newVisit(uuidA2, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuidA2, TimeUtils.ntpTimestampFromInstant(now.plus(15, ChronoUnit.MINUTES))), // don't pass
                newVisit(uuidB, TimeUtils.ntpTimestampFromInstant(now.minus(3, ChronoUnit.HOURS))), // pass
                newVisit(uuidB2, TimeUtils.ntpTimestampFromInstant(now)), // don't pass
                newVisit(uuidB, TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.HOURS))), // don't pass
                newVisit(uuidC, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuidC2, TimeUtils.ntpTimestampFromInstant(now)), /* don't pass */
                newVisit(uuidC2, TimeUtils.ntpTimestampFromInstant(now)) /* don't pass */
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, 0L));

        assertThat(processed)
                .extracting(DecodedVisit::getLocationTemporaryPublicId)
                .containsExactly(uuidA, uuidA, uuidB, uuidC);
    }

    @Test
    @DisplayName("if pivot date is in future, set it to retentionDate and check that all visits are forward")
    void a_report_with_a_future_pivotDate_is_processed_like_a_report_with_a_pivotDate_set_to_retentionDate_and_all_visits_should_be_forwards() {
        long pivotDateInFutureAsNtp = TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.MINUTES));

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))),
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))),
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now))
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, pivotDateInFutureAsNtp));

        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(DecodedVisit::isBackward).count()).isZero();
        assertThat(processed.stream().filter(DecodedVisit::isForward).count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("if pivot date is before retentionDate, set it to retentionDate and check that all visits are forward")
    void a_report_with_an_outdated_pivotDate_is_processed_like_a_report_with_a_pivotDate_set_to_retentionDate_and_all_visits_should_be_forwards() {
        long pivotDateTooOldAsNtp = TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS));

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))),
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))),
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now))
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, pivotDateTooOldAsNtp));

        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(DecodedVisit::isBackward).count()).isZero();
        assertThat(processed.stream().filter(DecodedVisit::isForward).count()).isEqualTo(3L);
    }

    @Test
    @DisplayName("if pivot date is before or equal qrScanTime, visits should be marked as forward")
    void visits_with_qrScanTime_after_or_equal_to_pivotDate_should_be_forwards() {
        long pivotDate = TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS));
        long qrScan = TimeUtils.ntpTimestampFromInstant(now);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, qrScan),
                newVisit(uuid2, pivotDate)
        );

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, pivotDate));

        assertThat(processed.size()).isEqualTo(2);
        assertThat(processed.get(0).isForward()).isTrue();
        assertThat(processed.get(1).isForward()).isTrue();
    }

    @Test
    @DisplayName("if pivot date is strictly after qrScanTime, visits should be marked as backward")
    void visits_with_qrScanTime_strictly_before_pivotDate_should_be_backwards() {
        long pivotDate = TimeUtils.ntpTimestampFromInstant(now);
        long qrScan = TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS));
        UUID uuid = UUID.randomUUID();
        List<Visit> visits = List.of(newVisit(uuid, qrScan));

        List<DecodedVisit> processed = reportService.report(new ReportRequest(visits, pivotDate));

        assertThat(processed.size()).isEqualTo(1);
        assertThat(processed.get(0).isBackward()).isTrue();
    }

    private Visit newVisit(UUID uuid, Long qrCodeScanTime) {
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .locationTemporaryPublicId(uuid)
                .build();
        byte[] qrCodeHeader = new LocationSpecificPartEncoder().binaryEncodedHeader(lsp);
        byte[] encodedQr = new byte[CleaEciesEncoder.HEADER_BYTES_SIZE + CleaEciesEncoder.MSG_BYTES_SIZE];
        System.arraycopy(qrCodeHeader, 0, encodedQr, 0, qrCodeHeader.length);
        String qrCode = Base64.encodeBase64URLSafeString(encodedQr);
        return new Visit(qrCode, qrCodeScanTime);
    }

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class LoggingTest {

        private ListAppender<ILoggingEvent> loggingEventListAppender;

        @BeforeEach
        void setUp() {
            now = Instant.now();
            this.loggingEventListAppender = new ListAppender<>();
            this.loggingEventListAppender.start();
            ((Logger) LoggerFactory.getLogger(ReportService.class)).addAppender(loggingEventListAppender);
        }

        @Test
        void test_that_duplicate_visits_increment_rejected_visits_count() {
            UUID uuidA = UUID.randomUUID();
            UUID uuidB = UUID.randomUUID();
            UUID uuidC = UUID.randomUUID();
            List<Visit> visits = List.of(
                    newVisit(uuidA, TimeUtils.ntpTimestampFromInstant(now.minus(4, ChronoUnit.HOURS))), // pass
                    newVisit(uuidA, TimeUtils.ntpTimestampFromInstant(now)), // pass
                    newVisit(uuidB, TimeUtils.ntpTimestampFromInstant(now.minus(3, ChronoUnit.HOURS))), // pass
                    newVisit(uuidB, TimeUtils.ntpTimestampFromInstant(now)), // don't pass
                    newVisit(uuidC, TimeUtils.ntpTimestampFromInstant(now)), // pass
                    newVisit(uuidC, TimeUtils.ntpTimestampFromInstant(now)) /* don't pass */
            );

            reportService.report(new ReportRequest(visits, 0L));

            List<ILoggingEvent> logsList = loggingEventListAppender.list;

            assertThat(logsList).extracting("formattedMessage")
                    .contains(String.format("BATCH_REPORT %s#%s#%s#%s#%s", visits.size(), 2, 0, 4, 0));
        }

        @Test
        void test_that_backward_visit_increments_backward_visits_count() {
            long pivotDate = TimeUtils.ntpTimestampFromInstant(now);
            long qrScan = TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS));
            UUID uuid = UUID.randomUUID();
            List<Visit> visits = List.of(newVisit(uuid, qrScan));

            reportService.report(new ReportRequest(visits, pivotDate));

            List<ILoggingEvent> logsList = loggingEventListAppender.list;
            assertThat(logsList).extracting("formattedMessage")
                    .contains(String.format("BATCH_REPORT %s#%s#%s#%s#%s", visits.size(), 0, 1, 0, 0));
        }

        @Test
        void test_that_forward_visit_increments_forward_visits_count() {
            long pivotDate = TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS));
            long qrScan = TimeUtils.ntpTimestampFromInstant(now);
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            List<Visit> visits = List.of(
                    newVisit(uuid1, qrScan),
                    newVisit(uuid2, pivotDate)
            );

            reportService.report(new ReportRequest(visits, pivotDate));

            List<ILoggingEvent> logsList = loggingEventListAppender.list;
            assertThat(logsList).extracting("formattedMessage")
                    .contains(String.format("BATCH_REPORT %s#%s#%s#%s#%s", visits.size(), 0, 0, 2, 0));
        }

        @Test
        void test_that_outdated_visits_increments_rejected_visits_count() {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();
            UUID uuid4 = UUID.randomUUID();
            List<Visit> visits = List.of(
                    newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS))), // don't pass
                    newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.minus(13, ChronoUnit.DAYS))), // pass
                    newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                    newVisit(uuid4, TimeUtils.ntpTimestampFromInstant(now)) /* pass */
            );

            ReportRequest reportRequestVo = new ReportRequest(visits, 0L);
            reportService.report(reportRequestVo);

            List<ILoggingEvent> logsList = loggingEventListAppender.list;
            assertThat(logsList).extracting("formattedMessage")
                    .contains(String.format("BATCH_REPORT %s#%s#%s#%s#%s", visits.size(), 1, 0, 3, 0));
        }

        @Test
        void test_that_future_visits_increments_rejected_visits_count() {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            List<Visit> visits = List.of(
                    newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now)), // pass
                    newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.plus(2, ChronoUnit.SECONDS))) // don't pass
            );

            reportService.report(new ReportRequest(visits, 0L));

            List<ILoggingEvent> logsList = loggingEventListAppender.list;
            assertThat(logsList).extracting("formattedMessage")
                    .contains(String.format("BATCH_REPORT %s#%s#%s#%s#%s", visits.size(), 1, 0, 1, 0));
        }
    }
}
