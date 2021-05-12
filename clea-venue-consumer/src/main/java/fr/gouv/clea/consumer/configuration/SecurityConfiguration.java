package fr.gouv.clea.consumer.configuration;

import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final VenueConsumerProperties properties;

    @Bean
    public LocationSpecificPartDecoder getLocationSpecificPartDecoder() {
        return new LocationSpecificPartDecoder(properties.getSecurity().getCrypto().getServerAuthoritySecretKey());
    }

    @Bean
    public CleaEciesEncoder getCleaEciesEncoder() {
        return new CleaEciesEncoder();
    }
}
