package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IConsumerService;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.gouv.clea.consumer.service.IStatService;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import fr.gouv.clea.consumer.utils.MessageFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RefreshScope
@RequiredArgsConstructor
@Slf4j
public class ConsumerService implements IConsumerService {

    private final IDecodedVisitService decodedVisitService;

    private final IVisitExpositionAggregatorService visitExpositionAggregatorService;

    private final IStatService statService;

    @Override
    @KafkaListener(topics = "${clea.kafka.qrCodesTopic}", containerFactory = "visitContainerFactory")
    public void consumeVisit(DecodedVisit decodedVisit) {
        log.info(
                "[locationTemporaryPublicId: {}, qrCodeScanTime: {}] retrieved from queue",
                MessageFormatter.truncateUUID(decodedVisit.getStringLocationTemporaryPublicId()),
                decodedVisit.getQrCodeScanTime()
        );
        Optional<Visit> optionalVisit = decodedVisitService.decryptAndValidate(decodedVisit);
        optionalVisit.ifPresentOrElse(
                visit -> {
                    log.debug("Consumer: visit after decrypt + validation: {}, ", visit);
                    visitExpositionAggregatorService.updateExposureCount(visit);
                    statService.logStats(visit);
                },
                () -> log.info("empty visit after decrypt + validation")
        );
    }

    @Override
    @KafkaListener(topics = "${clea.kafka.reportStatsTopic}", containerFactory = "statContainerFactory")
    public void consumeStat(ReportStat reportStat) {
        log.info("stat {} retrieved from queue", reportStat);
        statService.logStats(reportStat);
    }
}
