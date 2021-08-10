package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.configuration.CleaKafkaProperties;
import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.model.LocationStat;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.ReportStatEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.statistiques.ReportStatIndex;
import fr.gouv.clea.consumer.repository.statistiques.StatLocationIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsService {

    private final CleaKafkaProperties cleaKafkaProperties;

    private final StatLocationIndex statLocationIndex;

    private final ReportStatIndex reportStatIndex;

    private final VenueConsumerProperties properties;

    private final KafkaTemplate<String, LocationStat> kafkaErrorStatTemplate;

    private final ElasticsearchOperations template;

    @Retryable
    public void logStats(Visit visit) {
        if (!template.indexOps(LocationStat.class).exists()) {
            template.indexOps(LocationStat.class).create();
        }

        final var statLocation = toLocationStat(visit);
        statLocationIndex.findById(statLocation.getId())
                .map(existingLocationStat -> {
                    if (visit.isBackward()) {
                        existingLocationStat.setBackwardVisits(existingLocationStat.getBackwardVisits() + 1);
                    } else {
                        existingLocationStat.setForwardVisits(existingLocationStat.getForwardVisits() + 1);
                    }
                    return existingLocationStat;
                })
                .ifPresentOrElse(
                        statLocationIndex::saveWithIndexTargeting,
                        () -> statLocationIndex.save(statLocation)
                );

        log.debug("Location stat saved to elastic: {}", statLocation);
    }

    @Recover
    public void logStatToKafka(Exception e, Visit visit) {
        log.info("Failed to log location stat for visit after 3 attempts : {}", visit, e);
        final var statLocation = toLocationStat(visit);
        kafkaErrorStatTemplate.send(cleaKafkaProperties.getErrorLocationStatsTopic(), statLocation).addCallback(
                new ListenableFutureCallback<>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        log.error(
                                "Error sending location stat {} to error queue. Message : {}", statLocation.toString(),
                                ex
                        );
                    }

                    @Override
                    public void onSuccess(SendResult<String, LocationStat> result) {
                        log.info("Location Stat {} sent to error queue.", statLocation.toString());
                    }
                }
        );
    }

    private LocationStat toLocationStat(Visit visit) {
        final var secondsToRemove = visit.getQrCodeScanTime().getEpochSecond()
                % properties.getStatSlotDurationInSeconds();
        final var statPeriod = visit.getQrCodeScanTime().minus(secondsToRemove, ChronoUnit.SECONDS)
                .truncatedTo(ChronoUnit.SECONDS);
        final var stringStatPeriod = statPeriod
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final var id = String.format(
                "%s-vt:%d-vc1:%d-vc2:%d", stringStatPeriod, visit.getVenueType(), visit.getVenueCategory1(),
                visit.getVenueCategory2()
        );

        return LocationStat.builder()
                .id(id)
                .periodStart(statPeriod)
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .build();

    }

    public void logStats(ReportStat reportStat) {

        if (!template.indexOps(ReportStatEntity.class).exists()) {
            template.indexOps(ReportStatEntity.class).create();
        }

        var saved = reportStatIndex.save(reportStat.toEntity());
        log.debug("Report stat saved to elastic: {}", saved);
    }
}
