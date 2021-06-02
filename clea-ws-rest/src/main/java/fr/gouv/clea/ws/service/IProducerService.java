package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportStat;

import java.util.List;

public interface IProducerService {

    void produceVisits(List<DecodedVisit> decodedVisits);

    void produceStat(ReportStat reportStat);
}
