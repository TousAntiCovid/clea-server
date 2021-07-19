package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.statistiques.IReportStatRepository;
import fr.gouv.clea.consumer.repository.statistiques.IStatLocationRepository;
import fr.gouv.clea.consumer.service.IStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatService implements IStatService {

    private final IStatLocationRepository repository;

    private final IReportStatRepository reportStatRepository;

    private final VenueConsumerProperties properties;

    @Override
    public void logStats(Visit visit) {

        var statLocation = newStatLocation(visit);
        Optional<StatLocation> optional = repository.findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriod(
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
                "saved stat period: {}, venueType: {} venueCategory1: {}, venueCategory2: {}, backwardVisits: {}, forwardVisits: {}",
                statLocation.getPeriod(),
                statLocation.getVenueType(),
                statLocation.getVenueCategory1(),
                statLocation.getVenueCategory2(),
                statLocation.getBackwardVisits(),
                statLocation.getForwardVisits()
        );
    }

    @Override
    public void logStats(ReportStat reportStat) {
        var saved = reportStatRepository.save(reportStat.toEntity());
        log.info("saved report stat: {}", saved);
    }

    protected StatLocation newStatLocation(Visit visit) {
        return StatLocation.builder()
                .period(this.getStatPeriod(visit))
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
