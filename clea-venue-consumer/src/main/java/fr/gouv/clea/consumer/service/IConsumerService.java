package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;

public interface IConsumerService {

    void consumeVisit(DecodedVisit decodedVisit);

    void consumeStat(ReportStat reportStat);
}
