package fr.gouv.clea.integrationtests;

import org.junit.runner.JUnitCore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.Matchers.is;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CleaIntegrationTestsApplication {

    public static void main(String[] args) {
        var exitCode = 1;
        try {
            await("clea platform is ready")
                    .atMost(3, MINUTES)
                    .pollInterval(fibonacci(SECONDS))
                    .until(() -> JUnitCore.runClasses(SmokeTests.class).wasSuccessful(), is(true));

            exitCode = JUnitCore.runClasses(CucumberTest.class).wasSuccessful() ? 0 : 1;
        } finally {
            System.exit(exitCode);
        }
    }
}
