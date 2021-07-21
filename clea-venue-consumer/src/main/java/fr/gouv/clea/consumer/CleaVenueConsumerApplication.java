package fr.gouv.clea.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = { "fr.gouv.clea.consumer", "fr.gouv.clea.scoring" })
@EnableJpaRepositories("fr.gouv.clea.consumer.repository.visits")
@EnableElasticsearchRepositories("fr.gouv.clea.consumer.repository.statistiques")
public class CleaVenueConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaVenueConsumerApplication.class, args);
    }
}
