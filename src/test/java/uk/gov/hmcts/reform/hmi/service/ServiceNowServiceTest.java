package uk.gov.hmcts.reform.hmi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("PMD.LawOfDemeter")
@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class ServiceNowServiceTest {

    private static MockWebServer mockWebServerEndpoint;

    LogCaptor logCaptor = LogCaptor.forClass(ServiceNowService.class);

    ServiceNowService serviceNowService;

    @BeforeEach
    void setup() throws IOException {
        serviceNowService = new ServiceNowService("http://localhost:1234",
                                                  "username",
                                                  "password",
                                                  "assignmentGroup",
                                                  "callerId",
                                                  "serviceOffering",
                                                  "roleType");
        mockWebServerEndpoint = new MockWebServer();
        mockWebServerEndpoint.start(1234);
    }

    @AfterEach
    void teardown() throws IOException {
        mockWebServerEndpoint.shutdown();
    }

    @Test
    void testCreateServiceNow() throws JsonProcessingException {
        mockWebServerEndpoint.enqueue(new MockResponse().setBody("INC123456"));

        boolean result = serviceNowService.createServiceNowRequest(new StringBuilder(), "");
        assertTrue(result, "Did not receive expected result");
        assertTrue(logCaptor.getInfoLogs().get(0).contains("ServiceNow ticket has been created"),
                   "Info log did not contain message");
    }

    @Test
    void testCreateServiceNowReturnFalse() throws JsonProcessingException {
        mockWebServerEndpoint.enqueue(new MockResponse().setBody("Test error"));

        boolean result = serviceNowService.createServiceNowRequest(new StringBuilder(), "");
        assertFalse(result, "Did not receive expected result");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Error while create ServiceNow ticket"),
                   "Info log did not contain message");
    }

    @Test
    void testCreateServiceNowFailed() throws JsonProcessingException {
        mockWebServerEndpoint.enqueue(new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value()));

        serviceNowService.createServiceNowRequest(new StringBuilder(), "");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Error while create ServiceNow ticket:"),
                   "Error logs did not contain message");
    }

    @Test
    void testCreateServiceNowReturnNull() throws JsonProcessingException {
        mockWebServerEndpoint.enqueue(new MockResponse());

        boolean result = serviceNowService.createServiceNowRequest(new StringBuilder(), "");
        assertFalse(result, "Did not receive expected result");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Error while create ServiceNow ticket"),
                   "Info log did not contain message");
    }
}

