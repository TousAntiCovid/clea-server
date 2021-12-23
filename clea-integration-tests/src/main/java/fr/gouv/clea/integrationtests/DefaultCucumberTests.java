package fr.gouv.clea.integrationtests;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(tags = "not @smoke and not @RequiresElasticsearchAccess", features = "classpath:features", plugin = {
        "pretty",
        "html:target/cucumber-reports.html" })
public class DefaultCucumberTests {
}
