package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.Visit;
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
public class ConsumerService {

    private final DecodedVisitService decodedVisitService;

    private final VisitExpositionAggregatorService visitExpositionAggregatorService;

    private final StatisticsService statisticsService;

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
                    statisticsService.logStats(visit);
                },
                () -> log.info("empty visit after decrypt + validation")
        );
    }

    @KafkaListener(topics = "${clea.kafka.reportStatsTopic}", containerFactory = "statContainerFactory")
    public void consumeStat(ReportStat reportStat) {
        log.info("stat {} retrieved from queue", reportStat);
        statisticsService.logStats(reportStat);
    }
}
