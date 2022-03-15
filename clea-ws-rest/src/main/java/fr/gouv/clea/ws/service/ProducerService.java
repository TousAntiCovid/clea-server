package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.configuration.CleaKafkaProperties;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportStat;
import fr.gouv.clea.ws.utils.MessageFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProducerService {

    private final KafkaTemplate<String, DecodedVisit> kafkaQrTemplate;

    private final KafkaTemplate<String, ReportStat> kafkaStatTemplate;

    private final CleaKafkaProperties cleaKafkaProperties;

    public void produceVisits(List<DecodedVisit> serializableDecodedVisits) {
        serializableDecodedVisits.forEach(
                it -> kafkaQrTemplate.send(cleaKafkaProperties.getQrCodesTopic(), it).addCallback(
                        new ListenableFutureCallback<>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                // TODO: Do we want a mechanism to do not loose the message (e.g. spring retry)?
                                log.error(
                                        "error sending [locationTemporaryPublicId: {}, qrCodeScanTime: {}] to queue. message: {}",
                                        MessageFormatter.truncateUUID(it.getStringLocationTemporaryPublicId()),
                                        it.getQrCodeScanTime(), ex.getLocalizedMessage()
                                );
                            }

                            @Override
                            public void onSuccess(SendResult<String, DecodedVisit> result) {
                                log.info(
                                        "[locationTemporaryPublicId: {}, qrCodeScanTime: {}] sent to queue",
                                        MessageFormatter.truncateUUID(it.getStringLocationTemporaryPublicId()),
                                        it.getQrCodeScanTime()
                                );
                            }
                        }
                )
        );
    }

    public void produceStat(ReportStat reportStat) {
        kafkaStatTemplate.send(cleaKafkaProperties.getStatsTopic(), reportStat).addCallback(
                new ListenableFutureCallback<>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        log.error(
                                "error sending stat {} to queue. message: {}", reportStat.toString(),
                                ex.getLocalizedMessage()
                        );
                    }

                    @Override
                    public void onSuccess(SendResult<String, ReportStat> result) {
                        log.info("stat {} sent to queue", reportStat.toString());
                    }
                }
        );
    }
}
