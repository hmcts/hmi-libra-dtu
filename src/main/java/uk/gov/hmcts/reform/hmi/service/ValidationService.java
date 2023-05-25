package uk.gov.hmcts.reform.hmi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Service
@SuppressWarnings({"PMD.UseProperClassLoader"})
public class ValidationService {

    private JsonSchema initValidator(String jsonSchemaPath) {
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

        try (InputStream masterFile = this.getClass().getClassLoader()
            .getResourceAsStream(jsonSchemaPath)) {
            return schemaFactory.getSchema(masterFile); //NOSONAR
        } catch (JsonSchemaException | IOException ex) { //NOSONAR
            log.error(String.format("Failed to get libra json schema file: %s", ex.getMessage()));
            return null;
        }
    }

    public boolean isValid(String jsonSchemaPath, byte[] jsonPayload) {
        JsonSchema libraJsonSchema = initValidator(jsonSchemaPath); //NOSONAR

        try {
            JsonNode jsonNode = new ObjectMapper().readTree(jsonPayload);
            Set<ValidationMessage> error = libraJsonSchema.validate(jsonNode); //NOSONAR

            if (!error.isEmpty()) {
                log.error(String.format("Failed to validate the schema with error message: %s", error));
                return false;
            }

            return true;
        } catch (IOException ex) {
            log.error(String.format("Failed to validate the schema with exception: %s", ex.getMessage()));
            return false;
        }
    }
}
