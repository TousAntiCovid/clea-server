package fr.gouv.clea.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = { "fr.gouv.clea.consumer", "fr.gouv.clea.scoring" })
@EnableRetry
public class CleaVenueConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaVenueConsumerApplication.class, args);
    }
}
