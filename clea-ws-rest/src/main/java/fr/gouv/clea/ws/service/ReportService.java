package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.configuration.CleaWsProperties;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportStat;
import fr.gouv.clea.ws.service.model.Visit;
import fr.gouv.clea.ws.utils.MessageFormatter;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static fr.gouv.clea.ws.utils.MessageFormatter.truncateQrCode;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Base64.getUrlDecoder;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final CleaWsProperties properties;

    private final LocationSpecificPartDecoder decoder;

    private final ProducerService producerService;

    public int reportWithPivotDate(final Instant pivotDate, final List<Visit> visits) {
        final var now = now();

        return report(pivotDate, visits, now);
    }

    public int reportWithoutPivotDate(final List<Visit> visits) {
        final var now = now();

        return report(now.minus(14, DAYS), visits, now);
    }

    private int report(final Instant pivotDate, final List<Visit> visits, final Instant now) {
        final var validatedPivotDate = this.validatePivotDate(pivotDate, now);
        final var verifiedAndDecodedVisits = visits.stream()
                .filter(Objects::nonNull)
                .filter(this::nonBlankBase64urlLocationSpecificPart)
                .filter(this::nonNullScanTime)
                .filter(visit -> !isOutdated(visit, now))
                .filter(visit -> !isFuture(visit, now))
                .map(it -> decode(it, validatedPivotDate))
                .filter(Objects::nonNull)
                .collect(toList());
        final var uniqueDecodedVisits = this.pruneDuplicates(verifiedAndDecodedVisits);
        producerService.produceVisits(uniqueDecodedVisits);

        // evaluate produced visits and count close scan time visits
        final var closeScanTimeVisits = new VisitsInSameUnitCounter(
                properties.getExposureTimeUnitInSeconds()
        );
        uniqueDecodedVisits.stream().sorted(comparing((DecodedVisit::getQrCodeScanTime)))
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

    private boolean nonBlankBase64urlLocationSpecificPart(final Visit visit) {
        return isNotBlank(visit.getLocationSpecificPart());
    }

    private boolean nonNullScanTime(final Visit visit) {
        return visit.getScanTime() != null;
    }

    private DecodedVisit decode(final Visit visit, final Instant pivotDate) {
        try {
            final var binaryLocationSpecificPart = getUrlDecoder().decode(visit.getLocationSpecificPart());
            return DecodedVisit.builder()
                    .encryptedLocationSpecificPart(decoder.decodeHeader(binaryLocationSpecificPart))
                    .qrCodeScanTime(visit.getScanTime())
                    .isBackward(visit.getScanTime().isBefore(pivotDate))
                    .build();
        } catch (CleaEncodingException e) {
            log.warn(
                    "report: {} rejected: Invalid format",
                    truncateQrCode(visit.getLocationSpecificPart())
            );
            return null;
        }
    }

    private boolean isOutdated(final Visit visit, final Instant now) {
        final var daysBetweenScanTimeAndNow = DAYS.between(visit.getScanTime(), now);
        if (daysBetweenScanTimeAndNow > properties.getRetentionDurationInDays()) {
            log.warn("report: {} rejected: Outdated", truncateQrCode(visit.getLocationSpecificPart()));
            return true;
        }
        return false;
    }

    private boolean isFuture(final Visit visit, final Instant now) {
        boolean future = visit.getScanTime().isAfter(now);
        if (future) {
            log.warn(
                    "report: {} rejected: In future", truncateQrCode(visit.getLocationSpecificPart())
            );
        }
        return future;
    }

    private boolean isDuplicatedScan(final DecodedVisit lsp, final List<DecodedVisit> cleaned) {
        return cleaned.stream().anyMatch(cleanedLsp -> this.isDuplicatedScan(lsp, cleanedLsp));
    }

    private boolean isDuplicatedScan(final DecodedVisit one, final DecodedVisit other) {
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
            return true;
        }
        return false;
    }

    private List<DecodedVisit> pruneDuplicates(final List<DecodedVisit> locationSpecificParts) {
        List<DecodedVisit> cleaned = new ArrayList<>();
        locationSpecificParts.forEach(it -> {
            if (!this.isDuplicatedScan(it, cleaned)) {
                cleaned.add(it);
            }
        });
        return cleaned;
    }

    private Instant validatePivotDate(final Instant pivotDate, final Instant now) {
        Instant nowWithoutMilis = now.truncatedTo(ChronoUnit.SECONDS);
        Instant retentionDateLimit = nowWithoutMilis.minus(properties.getRetentionDurationInDays(), DAYS);
        if (pivotDate.isAfter(now) || pivotDate.isBefore(retentionDateLimit)) {
            log.warn(
                    "pivotDate: {} not between retentionLimitDate: {} and now: {}", pivotDate,
                    retentionDateLimit, now
            );
            return retentionDateLimit;
        } else {
            return pivotDate;
        }
    }
}
