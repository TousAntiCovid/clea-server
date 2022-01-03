package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.model.ClusterDeclarationRequest;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.service.DecodedVisitService;
import fr.gouv.clea.consumer.service.VisitExpositionAggregatorService;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

import java.time.Instant;
import java.util.Base64;

import static java.time.ZoneOffset.UTC;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ManualClusterDeclarationController {

    private final DecodedVisitService decodedVisitService;

    private final VisitExpositionAggregatorService visitExpositionAggregatorService;

    private final LocationSpecificPartDecoder decoder;

    @GetMapping("/cluster-declaration")
    public String declareCluster(@ModelAttribute final ClusterDeclarationRequest clusterDeclarationRequest) {
        return "cluster-declaration";
    }

    @PostMapping(value = "/cluster-declaration")
    public String declareCluster(@Valid @ModelAttribute final ClusterDeclarationRequest clusterDeclarationRequest,
            final BindingResult result, final RedirectAttributes redirectAttributes) {
        final var qrCodeScanTime = clusterDeclarationRequest.getDate().toInstant(UTC);
        final var deepLinkLocationSpecificPart = clusterDeclarationRequest.getDeeplink().getRef();

        if (result.hasErrors()) {
            // do nothing
        } else if (qrCodeScanTime.isAfter(Instant.now())) {
            result.rejectValue("date", "FutureDateError.clusterDeclarationRequest.date");
        } else if (StringUtils.isEmpty(deepLinkLocationSpecificPart)) {
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
            if (!result.hasErrors()) {
                redirectAttributes.addAttribute("clusterDeclarationSuccess", true);
                return "redirect:/cluster-declaration";
            }
        }
        return "cluster-declaration";
    }
}
