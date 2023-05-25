package uk.gov.hmcts.reform.hmi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "validations")
@Getter
@Setter
public class ValidationConfiguration {

    /**
     * Config option for the master schema file.
     */
    private String libraHmiSchema;
}
