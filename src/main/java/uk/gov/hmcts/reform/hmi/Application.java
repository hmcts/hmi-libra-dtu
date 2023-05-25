package uk.gov.hmcts.reform.hmi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import uk.gov.hmcts.reform.hmi.config.AzureBlobConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    AzureBlobConfigurationProperties.class
})
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);

        System.exit(0);
    }
}
