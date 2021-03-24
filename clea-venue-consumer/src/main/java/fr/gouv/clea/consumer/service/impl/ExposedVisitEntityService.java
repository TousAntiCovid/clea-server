package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
import fr.gouv.clea.consumer.utils.MessageFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class ExposedVisitEntityService implements IExposedVisitEntityService {

    private final IExposedVisitRepository repository;

    private final int retentionDurationInDays;

    @Autowired
    public ExposedVisitEntityService(
            IExposedVisitRepository repository,
            @Value("${clea.conf.retentionDurationInDays}") int retentionDurationInDays
    ) {
        this.repository = repository;
        this.retentionDurationInDays = retentionDurationInDays;
    }

    @Override
    public ExposedVisitEntity persist(ExposedVisitEntity exposedVisitEntity) {
        try {
            log.info("successfully persisted [locationTemporaryPublicId: {}, qrCodeScanTime: {}]", MessageFormatter.truncateUUID(exposedVisitEntity.getLocationTemporaryPublicId()), exposedVisitEntity.getQrCodeScanTime());
            return repository.save(exposedVisitEntity);
        } catch (Exception e) {
            log.error("error persisting [locationTemporaryPublicId: {}, qrCodeScanTime: {}]", MessageFormatter.truncateUUID(exposedVisitEntity.getLocationTemporaryPublicId()), exposedVisitEntity.getQrCodeScanTime());
            throw e;
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "${clea.conf.scheduling.purge.cron}")
    public void deleteOutdatedExposedVisits() {
        try {
            long beforeCount = repository.count();
            this.repository.deleteAllByQrCodeScanTimeBefore(Instant.now().minus(retentionDurationInDays, ChronoUnit.DAYS));
            long afterCount = repository.count();
            log.info("successfully purged {} entries from DB", beforeCount - afterCount);
        } catch (Exception e) {
            log.error("error during purge");
            throw e;
        }
    }
}
