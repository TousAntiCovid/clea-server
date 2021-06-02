package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.Visit;

public interface IStatService {

    void logStats(Visit visit);

    void logStats(ReportStat reportStat);
}
