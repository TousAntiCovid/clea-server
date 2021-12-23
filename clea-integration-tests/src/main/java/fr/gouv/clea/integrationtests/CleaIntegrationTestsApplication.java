package fr.gouv.clea.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.JUnitCore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.Matchers.is;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class CleaIntegrationTestsApplication {

    public static void main(String[] args) {
        var exitCode = 1;
        try {
            if (System.getProperty("spring.profiles.active").equals("int")) {
                log.info("Integration platform scenario");
                await("clea platform is ready")
                        .atMost(3, MINUTES)
                        .pollInterval(fibonacci(SECONDS))
                        .until(() -> JUnitCore.runClasses(DefaultSmokeTests.class).wasSuccessful(), is(true));
                exitCode = JUnitCore.runClasses(DefaultCucumberTests.class).wasSuccessful() ? 0 : 1;
            } else {
                await("clea platform is ready")
                        .atMost(3, MINUTES)
                        .pollInterval(fibonacci(SECONDS))
                        .until(
                                () -> JUnitCore.runClasses(RequiresElasticsearchAccessSmokeTests.class).wasSuccessful(),
                                is(true)
                        );
                exitCode = JUnitCore.runClasses(RequiresElasticsearchAccessCucumberTests.class).wasSuccessful() ? 0 : 1;
            }
        } finally {
            System.exit(exitCode);
        }
    }
}
