package fr.gouv.clea.integrationtests;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { CleaIntegrationTestsApplication.class })
@CucumberContextConfiguration
public class CucumberSpringConfiguration {
}
