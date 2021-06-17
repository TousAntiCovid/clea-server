package fr.gouv.clea.integrationtests.service;

import fr.gouv.clea.integrationtests.model.Visit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VisitsUpdateService {
    public List<Visit> malformLocalListScanTimes(final List<Visit> localList) {
        return List.copyOf(localList).stream()
                .peek(visit -> visit.setQrCodeScanTime(-1L))
                .collect(Collectors.toList());
    }

    public List<Visit> nullifyLocalListScanTimes(final List<Visit> localList) {
        return List.copyOf(localList).stream()
                .peek(visit -> visit.setQrCodeScanTime(null))
                .collect(Collectors.toList());
    }

    public List<Visit> emptyLocalListQrCodesFields(final List<Visit> localList) {
        return List.copyOf(localList).stream()
                .peek(visit -> visit.setQrCode(""))
                .collect(Collectors.toList());
    }

    public List<Visit> nullifyLocalListQrCodesFields(final List<Visit> localList) {
        return List.copyOf(localList).stream()
                .peek(visit -> visit.setQrCode(null))
                .collect(Collectors.toList());
    }

    public List<Visit> malformLocalListQrCodesFields(final List<Visit> localList) {
        return List.copyOf(localList).stream()
                .peek(visit -> visit.setQrCode("malformed"))
                .collect(Collectors.toList());
    }
}
