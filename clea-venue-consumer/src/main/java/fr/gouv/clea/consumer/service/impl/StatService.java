package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IReportStatRepository;
import fr.gouv.clea.consumer.repository.IStatLocationJpaRepository;
import fr.gouv.clea.consumer.service.IStatService;
import fr.gouv.clea.consumer.utils.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class StatService implements IStatService {

    private final IStatLocationJpaRepository repository;

    private final IReportStatRepository reportStatRepository;

    private final VenueConsumerProperties properties;

    private final MetricsService metricsService;

    @Override
    public void logStats(Visit visit) {
        StatLocationKey statLocationKey = buildKey(visit);

        var statLocation = newStatLocation(statLocationKey, visit);
        Optional<StatLocation> optional = repository.findById(statLocationKey);
        if (optional.isPresent()) {
            repository.updateByIncrement(statLocation);
        } else {
            try {
                repository.insert(statLocation);
            } catch (DataIntegrityViolationException eex) {
                repository.updateByIncrement(statLocation);
            }
        }
        log.info(
                "saved stat period: {}, venueType: {} venueCategory1: {}, venueCategory2: {}, backwardVisits: {}, forwardVisits: {}",
                statLocation.getStatLocationKey().getPeriod(),
                statLocation.getStatLocationKey().getVenueType(),
                statLocation.getStatLocationKey().getVenueCategory1(),
                statLocation.getStatLocationKey().getVenueCategory2(),
                statLocation.getBackwardVisits(),
                statLocation.getForwardVisits()
        );
    }

    @Override
    public void logStats(ReportStat reportStat) {
        var saved = reportStatRepository.save(reportStat.toEntity());
        log.info("saved report stat: {}", saved);
    }

    protected StatLocation newStatLocation(StatLocationKey statLocationKey, Visit visit) {
        return StatLocation.builder()
                .statLocationKey(statLocationKey)
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .build();
    }

    public StatLocationKey buildKey(Visit visit) {
        return StatLocationKey.builder()
                .period(getStatPeriod(visit))
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .build();
    }

    protected Instant getStatPeriod(Visit visit) {
        long secondsToRemove = visit.getQrCodeScanTime().getEpochSecond() % properties.getStatSlotDurationInSeconds();
        return visit.getQrCodeScanTime().minus(secondsToRemove, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS);
    }
}
