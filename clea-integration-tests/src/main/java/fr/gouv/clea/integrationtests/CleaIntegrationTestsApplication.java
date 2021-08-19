package fr.gouv.clea.integrationtests;

import org.junit.runner.JUnitCore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("fr.gouv.clea.integrationtests.config")
public class CleaIntegrationTestsApplication {

    public static void main(String[] args) {
        JUnitCore.main(CucumberTest.class.getName());
    }
}
