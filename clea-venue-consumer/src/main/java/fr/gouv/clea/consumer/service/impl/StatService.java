package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.configuration.CleaKafkaProperties;
import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.ReportStatEntity;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.statistiques.ReportStatRepository;
import fr.gouv.clea.consumer.repository.statistiques.StatLocationRepository;
import fr.gouv.clea.consumer.service.IStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatService implements IStatService {

    private final KafkaTemplate<String, StatLocation> kafkaErrorStatTemplate;

    private final CleaKafkaProperties cleaKafkaProperties;

    private final StatLocationRepository repository;

    private final ReportStatRepository reportStatRepository;

    private final VenueConsumerProperties properties;

    private final ElasticsearchOperations template;

    @Override
    public void logStats(Visit visit) {

        try {

            if (!template.indexOps(StatLocation.class).exists()) {
                template.indexOps(StatLocation.class).create();
            }

            var statLocation = newStatLocation(visit);
            Optional<StatLocation> optional = repository
                    .findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriodStart(
                            visit.getVenueType(),
                            visit.getVenueCategory1(),
                            visit.getVenueCategory2(),
                            getStatPeriod(visit)
                    );
            if (optional.isPresent()) {
                repository.updateByIncrement(optional.get(), statLocation);
            } else {
                repository.save(statLocation);
            }
            log.info(
                    "Saved stat period: {}, venueType: {} venueCategory1: {}, venueCategory2: {}, backwardVisits: {}, forwardVisits: {}",
                    statLocation.getPeriodStart(),
                    statLocation.getVenueType(),
                    statLocation.getVenueCategory1(),
                    statLocation.getVenueCategory2(),
                    statLocation.getBackwardVisits(),
                    statLocation.getForwardVisits()
            );
        } catch (Exception e) {
            log.error("Error while communicating with elasticseach cluster", e);
            this.produceErrorStatLocation(visit);
        }
    }

    @Override
    public void logStats(ReportStat reportStat) {

        if (!template.indexOps(ReportStatEntity.class).exists()) {
            template.indexOps(ReportStatEntity.class).create();
        }

        var saved = reportStatRepository.save(reportStat.toEntity());
        log.info("Saved report stat: {}", saved);
    }

    @Override
    public void produceErrorStatLocation(Visit visit) {
        var statLocation = newStatLocation(visit);
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
                    public void onSuccess(SendResult<String, StatLocation> result) {
                        log.info("Location Stat {} sent to error queue.", statLocation.toString());
                    }
                }
        );
    }

    protected StatLocation newStatLocation(Visit visit) {
        return StatLocation.builder()
                .periodStart(this.getStatPeriod(visit))
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .build();
    }

    protected Instant getStatPeriod(Visit visit) {
        long secondsToRemove = visit.getQrCodeScanTime().getEpochSecond() % properties.getStatSlotDurationInSeconds();
        return visit.getQrCodeScanTime().minus(secondsToRemove, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS);
    }
}
