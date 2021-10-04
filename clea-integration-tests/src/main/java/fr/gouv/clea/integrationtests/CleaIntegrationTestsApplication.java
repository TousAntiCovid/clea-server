package fr.gouv.clea.integrationtests;

import org.junit.runner.JUnitCore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CleaIntegrationTestsApplication {

    public static void main(String[] args) {
        JUnitCore.main(CucumberTest.class.getName());
    }
}
