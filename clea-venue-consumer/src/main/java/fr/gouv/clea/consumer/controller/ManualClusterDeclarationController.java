package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.model.ClusterDeclarationRequest;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.service.DecodedVisitService;
import fr.gouv.clea.consumer.service.VisitExpositionAggregatorService;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

import java.time.Instant;
import java.util.Base64;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/cluster-declaration")
public class ManualClusterDeclarationController {

    private final DecodedVisitService decodedVisitService;

    private final VisitExpositionAggregatorService visitExpositionAggregatorService;

    private final LocationSpecificPartDecoder decoder;

    @GetMapping
    public String declareCluster(@ModelAttribute final ClusterDeclarationRequest clusterDeclarationRequest) {
        return "cluster-declaration";
    }

    @PostMapping
    public String declareCluster(@Valid @ModelAttribute final ClusterDeclarationRequest clusterDeclarationRequest,
            final BindingResult result, final RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "cluster-declaration";
        }

        final var qrCodeScanTime = clusterDeclarationRequest.getDateTime()
                .atZone(clusterDeclarationRequest.getZoneId())
                .toInstant();
        final var deepLinkLocationSpecificPart = clusterDeclarationRequest.getDeeplink().getRef();

        if (qrCodeScanTime.isAfter(Instant.now())) {
            result.rejectValue("dateTime", "FutureDateError.clusterDeclarationRequest.dateTime");
        } else if (deepLinkLocationSpecificPart == null || deepLinkLocationSpecificPart.isEmpty()) {
            result.rejectValue("deeplink", "InvalidUrlError.clusterDeclarationRequest.deeplink");
        } else {
            try {
                final var binaryLocationSpecificPart = Base64.getUrlDecoder().decode(deepLinkLocationSpecificPart);
                final var decodedVisit = DecodedVisit.builder()
                        .encryptedLocationSpecificPart(decoder.decodeHeader(binaryLocationSpecificPart))
                        .qrCodeScanTime(qrCodeScanTime)
                        .isBackward(false)
                        .build();

                decodedVisitService.decryptAndValidate(decodedVisit).ifPresentOrElse(
                        visit -> visitExpositionAggregatorService.updateExposureCount(visit, true),
                        () -> result.rejectValue("deeplink", "DecryptError.clusterDeclarationRequest.deeplink")
                );
            } catch (CleaEncodingException e) {
                result.rejectValue("deeplink", "DecodingError.clusterDeclarationRequest.deeplink");
            }
        }

        if (!result.hasErrors()) {
            redirectAttributes.addAttribute("success", true);
            return "redirect:/cluster-declaration";
        }
        return "cluster-declaration";
    }

}
