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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

import java.util.Base64;

import static java.time.ZoneOffset.UTC;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GenerateClusterController {

    private final DecodedVisitService decodedVisitService;

    private final VisitExpositionAggregatorService visitExpositionAggregatorService;

    private final LocationSpecificPartDecoder decoder;

    @GetMapping("/cluster-declaration")
    public String generate(
            @ModelAttribute final ClusterDeclarationRequest clusterDeclarationRequest) {
        return "cluster-declaration";
    }

    @PostMapping(value = "/cluster-declaration")
    public String generate(
            @Valid @ModelAttribute final ClusterDeclarationRequest clusterDeclarationRequest,
            final BindingResult result, final RedirectAttributes redirectAttributes) throws CleaEncodingException {

        if (result.hasErrors()) {
            return "cluster-declaration";
        }

        final var qrCodeScanTime = clusterDeclarationRequest.getDate().toInstant(UTC);
        final var deepLinkLocationSpecificPart = clusterDeclarationRequest.getDeeplink().getRef();

        final var binaryLocationSpecificPart = Base64.getUrlDecoder().decode(deepLinkLocationSpecificPart);
        final var decodedVisit = DecodedVisit.builder()
                .encryptedLocationSpecificPart(decoder.decodeHeader(binaryLocationSpecificPart))
                .qrCodeScanTime(qrCodeScanTime)
                .isBackward(false)
                .build();

        decodedVisitService.decryptAndValidate(decodedVisit).ifPresentOrElse(
                visit -> {
                    log.info("Consumer: visit after decrypt + validation: {}, ", visit);
                    visitExpositionAggregatorService.updateExposureCount(visit, true);
                },
                () -> result.rejectValue(
                        "deeplink", "Error when decrypting the qrCode",
                        clusterDeclarationRequest.getDeeplink().toString()
                )
        );

        if (!result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "success_message",
                    "Visites enregistrées avec succès, le cluster sera actif au prochain déclenchement du batch"
            );
        }
        return "redirect:/cluster-declaration";

    }

}
