package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.configuration.CleaKafkaProperties;
import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.model.LocationStat;
import fr.gouv.clea.consumer.model.LocationStatKafka;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.statistiques.LocationStatRepository;
import fr.gouv.clea.consumer.repository.statistiques.ReportStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsService {

    private final CleaKafkaProperties cleaKafkaProperties;

    private final LocationStatRepository locationStatRepository;

    private final ReportStatRepository reportStatRepository;

    private final VenueConsumerProperties properties;

    private final KafkaTemplate<String, LocationStatKafka> kafkaErrorStatTemplate;

    @Retryable(maxAttempts = 10, backoff = @Backoff(random = true, delay = 1000, maxDelay = 5000, multiplier = 2))
    public void logStats(Visit visit) {
        final var statLocation = toLocationStat(visit);
        final var result = locationStatRepository.findById(statLocation);
        if (result.isEmpty()) {
            locationStatRepository.create(statLocation);
        } else {
            final var existingStatLocation = result.get();
            if (visit.isBackward()) {
                locationStatRepository.update(existingStatLocation.withOneMoreBackwardVisit());
            } else {
                locationStatRepository.update(existingStatLocation.withOneMoreForwardVisit());
            }
        }
    }

    @Recover
    public void logStatToKafka(Exception e, Visit visit) {
        final var event = LocationStatKafka.builder()
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .periodStart(getPeriodStart(visit).atOffset(UTC))
                .backward(true)
                .build();
        log.warn("Failed to log location stat for visit after 3 attempts : {}", visit, e);
        final var statLocation = toLocationStat(visit);
        kafkaErrorStatTemplate.send(cleaKafkaProperties.getErrorLocationStatsTopic(), event)
                .addCallback(
                        result -> log.info("Location Stat {} sent to error queue.", statLocation.toString()),
                        ex -> log.error(
                                "Error sending location stat {} to error queue. Message : {}",
                                statLocation.toString(), ex
                        )
                );
    }

    public void logStats(ReportStat reportStat) {
        var saved = reportStatRepository.create(reportStat.toEntity());
        log.info("Report stat saved to elastic: {}", saved);
    }

    private Instant getPeriodStart(Visit visit) {
        final var secondsToRemove = visit.getQrCodeScanTime().getEpochSecond()
                % properties.getStatSlotDurationInSeconds();
        return visit.getQrCodeScanTime().minus(secondsToRemove, SECONDS)
                .truncatedTo(SECONDS);
    }

    private LocationStat toLocationStat(Visit visit) {
        return LocationStat.builder()
                .periodStart(getPeriodStart(visit))
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .build();

    }

}
