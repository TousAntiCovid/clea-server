package fr.gouv.clea.integrationtests.utils;

import fr.gouv.clea.integrationtests.model.Visit;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import lombok.experimental.UtilityClass;

import java.util.Base64;
import java.util.UUID;

@UtilityClass
public class QrCodeDecoder {

    public static UUID getLocationTemporaryId(final Visit visit) throws CleaEncodingException {
        return new LocationSpecificPartDecoder()
                .decodeHeader(Base64.getUrlDecoder().decode(visit.getDeepLinkExtractedInformation()))
                .getLocationTemporaryPublicId();
    }
}
