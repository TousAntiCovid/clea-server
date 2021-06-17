package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;

import java.util.Optional;

public interface IDecodedVisitService {

    Optional<Visit> decryptAndValidate(DecodedVisit decodedVisit);
}
