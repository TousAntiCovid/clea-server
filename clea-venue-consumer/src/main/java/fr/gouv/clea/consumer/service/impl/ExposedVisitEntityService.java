package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
import fr.gouv.clea.consumer.utils.MetricsService;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
@RefreshScope
@RequiredArgsConstructor
@Slf4j
public class ExposedVisitEntityService implements IExposedVisitEntityService {

    private final IExposedVisitRepository repository;

    private final VenueConsumerProperties properties;

    private final MetricsService metricsService;

    @Override
    @Transactional
    @Scheduled(cron = "${clea.conf.scheduling.purge.cron}")
    public void deleteOutdatedExposedVisits() {
        try {
            long start = System.currentTimeMillis();
            int count = this.repository.purge(
                    TimeUtils.currentNtpTime(), (int) properties.getDurationUnitInSeconds(),
                    TimeUtils.NB_SECONDS_PER_HOUR * 24, properties.getRetentionDurationInDays()
            );
            long end = System.currentTimeMillis();
            log.info("successfully purged {} entries from DB in {} seconds", count, (end - start) / 1000);
            metricsService.getPurgedCounter().increment(count);
        } catch (Exception e) {
            log.error("error during purge");
            metricsService.getFailedPurgeCounter().increment();
            throw e;
        }
    }
}
