package uk.gov.hmcts.reform.hmi.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class ValidationServiceTest {

    @InjectMocks
    private ValidationService validationService;
    private static final String LIBRA_JSON_SCHEMA = "schemas/libra-request.json";
    private static final String LIBRA_VALID_JSON = "mocks/libraValidFile.json";
    private static final String LIBRA_INVALID_JSON = "mocks/libraInvalidFile.json";
    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";

    LogCaptor logCaptor = LogCaptor.forClass(ValidationService.class);

    @Test
    void testIsValidReturnsTrue() throws IOException {
        try (InputStream libraJson = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(LIBRA_VALID_JSON)) {
            byte[] libraJsonAsByte = libraJson.readAllBytes();
            assertTrue(validationService.isValid(LIBRA_JSON_SCHEMA, libraJsonAsByte),
                       EXPECTED_MESSAGE);
        }
    }

    @Test
    void testIsValidReturnsFalse() throws IOException {
        try (InputStream libraJson = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(LIBRA_INVALID_JSON)) {
            byte[] libraJsonAsByte = libraJson.readAllBytes();
            assertFalse(validationService.isValid(LIBRA_JSON_SCHEMA, libraJsonAsByte),
                        EXPECTED_MESSAGE);
            assertTrue(logCaptor.getErrorLogs().get(0).contains("Failed to validate the schema with error message"),
                       "Error log did not contain message");
        }
    }
}

