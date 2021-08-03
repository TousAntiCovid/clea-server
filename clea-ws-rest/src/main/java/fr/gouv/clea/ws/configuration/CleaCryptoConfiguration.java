package fr.gouv.clea.ws.configuration;

import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CleaCryptoConfiguration {

    @Bean
    public LocationSpecificPartDecoder getLocationSpecificPartDecoder() {
        return new LocationSpecificPartDecoder();
    }

}
