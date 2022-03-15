package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.configuration.CleaWsProperties;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportStat;
import fr.gouv.clea.ws.service.model.Visit;
import fr.gouv.clea.ws.utils.MessageFormatter;
import fr.gouv.clea.ws.utils.MetricsService;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final CleaWsProperties properties;

    private final LocationSpecificPartDecoder decoder;

    private final ProducerService producerService;

    private final MetricsService metricsService;

    public int report(Instant pivotDate, List<Visit> visits) {
        final var now = Instant.now();
        final var validatedPivotDate = this.validatePivotDate(pivotDate, now);

        final var verifiedAndDecodedVisits = visits.stream()
                .filter(Objects::nonNull)
                .filter(this::nonBlankBase64urlLocationSpecificPart)
                .filter(this::nonNullScanTime)
                .filter(visit -> !isOutdated(visit, now))
                .filter(visit -> !isFuture(visit, now))
                .map(it -> decode(it, validatedPivotDate))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        final var uniqueDecodedVisits = this.pruneDuplicates(verifiedAndDecodedVisits);
        producerService.produceVisits(uniqueDecodedVisits);

        // evaluate produced visits and count close scan time visits
        final var closeScanTimeVisits = new VisitsInSameUnitCounter(
                properties.getExposureTimeUnitInSeconds()
        );
        uniqueDecodedVisits.stream().sorted(Comparator.comparing((DecodedVisit::getQrCodeScanTime)))
                .forEach(closeScanTimeVisits::incrementIfScannedInSameTimeUnitThanLastScanTime);

        producerService.produceStat(
                ReportStat.builder()
                        .reported(visits.size())
                        .rejected(visits.size() - uniqueDecodedVisits.size())
                        .backwards((int) uniqueDecodedVisits.stream().filter(DecodedVisit::isBackward).count())
                        .forwards((int) uniqueDecodedVisits.stream().filter(DecodedVisit::isForward).count())
                        .close(closeScanTimeVisits.getCount())
                        .timestamp(TimeUtils.currentNtpTime())
                        .build()
        );
        return uniqueDecodedVisits.size();
    }

    private boolean nonBlankBase64urlLocationSpecificPart(Visit visit) {
        return StringUtils.isNotBlank(visit.getLocationSpecificPart());
    }

    private boolean nonNullScanTime(Visit visit) {
        return visit.getQrCodeScanTime() != null;
    }

    private DecodedVisit decode(Visit visit, Instant pivotDate) {
        try {
            final var binaryLocationSpecificPart = Base64.getUrlDecoder().decode(visit.getLocationSpecificPart());
            return DecodedVisit.builder()
                    .encryptedLocationSpecificPart(decoder.decodeHeader(binaryLocationSpecificPart))
                    .qrCodeScanTime(visit.getQrCodeScanTime())
                    .isBackward(visit.getQrCodeScanTime().isBefore(pivotDate))
                    .build();
        } catch (CleaEncodingException e) {
            log.warn(
                    "report: {} rejected: Invalid format",
                    MessageFormatter.truncateQrCode(visit.getLocationSpecificPart())
            );
            return null;
        }
    }

    private boolean isOutdated(Visit visit, Instant now) {
        final var daysBetweenScanTimeAndNow = DAYS.between(visit.getQrCodeScanTime(), now);
        if (daysBetweenScanTimeAndNow > properties.getRetentionDurationInDays()) {
            log.warn("report: {} rejected: Outdated", MessageFormatter.truncateQrCode(visit.getLocationSpecificPart()));
            metricsService.getOutdatedVisitCounter().increment();
            return true;
        }
        return false;
    }

    private boolean isFuture(Visit visit, Instant now) {
        boolean future = visit.getQrCodeScanTime().isAfter(now);
        if (future) {
            log.warn(
                    "report: {} rejected: In future", MessageFormatter.truncateQrCode(visit.getLocationSpecificPart())
            );
            metricsService.getFutureVisitCounter().increment();
        }
        return future;
    }

    private boolean isDuplicatedScan(DecodedVisit lsp, List<DecodedVisit> cleaned) {
        return cleaned.stream().anyMatch(cleanedLsp -> this.isDuplicatedScan(lsp, cleanedLsp));
    }

    private boolean isDuplicatedScan(DecodedVisit one, DecodedVisit other) {
        if (!one.getLocationTemporaryPublicId().equals(other.getLocationTemporaryPublicId())) {
            return false;
        }

        long secondsBetweenScans = Duration.between(one.getQrCodeScanTime(), other.getQrCodeScanTime())
                .abs()
                .toSeconds();
        if (secondsBetweenScans <= properties.getDuplicateScanThresholdInSeconds()) {
            log.warn(
                    "report: {} {} rejected: Duplicate",
                    MessageFormatter.truncateUUID(one.getStringLocationTemporaryPublicId()), one.getQrCodeScanTime()
            );
            metricsService.getDuplicateVisitCounter().increment();
            return true;
        }
        return false;
    }

    private List<DecodedVisit> pruneDuplicates(List<DecodedVisit> locationSpecificParts) {
        List<DecodedVisit> cleaned = new ArrayList<>();
        locationSpecificParts.forEach(it -> {
            if (!this.isDuplicatedScan(it, cleaned)) {
                cleaned.add(it);
            }
        });
        return cleaned;
    }

    private Instant validatePivotDate(Instant pivotDate, Instant now) {
        Instant nowWithoutMilis = now.truncatedTo(ChronoUnit.SECONDS);
        Instant retentionDateLimit = nowWithoutMilis.minus(properties.getRetentionDurationInDays(), DAYS);
        if (pivotDate.isAfter(now) || pivotDate.isBefore(retentionDateLimit)) {
            log.warn(
                    "pivotDate: {} not between retentionLimitDate: {} and now: {}", pivotDate,
                    retentionDateLimit, now
            );
            metricsService.getNotCurrentPivotDatesCounter().increment();
            return retentionDateLimit;
        } else {
            return pivotDate;
        }
    }
}
