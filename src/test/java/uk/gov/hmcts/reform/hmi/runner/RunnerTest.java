package uk.gov.hmcts.reform.hmi.runner;

import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.LeaseStatusType;
import com.fasterxml.jackson.core.JsonProcessingException;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.hmi.models.ApiResponse;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;
import uk.gov.hmcts.reform.hmi.service.ProcessingService;
import uk.gov.hmcts.reform.hmi.service.ServiceNowService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LawOfDemeter")
class RunnerTest {

    @Mock
    AzureBlobService azureBlobService;

    @Mock
    DistributionService distributionService;

    @Mock
    ProcessingService processingService;

    @Mock
    ServiceNowService serviceNowService;

    @InjectMocks
    Runner runner;

    private static final String TEST = "Test";
    private static final String ALL_BLOB_RETRIEVED = "All blobs retrieved";
    private static final String MORE_INFO_THEN_EXPECTED = "More info logs than expected";
    private static final String RESPONSE_MESSAGE = "Info logs did not contain expected message";
    private static final String ALL_BLOB_PROCESSED = "Blob processed, shutting down";
    private static final String ELIGIBLE_BLOB_PROCESS = "Eligible blob selected to process";
    private static final String FILE_DELETED = "fileDeleted";

    @Test
    void testRunnerWithNoEligibleBlobToProcess() throws JsonProcessingException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.LOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains(ALL_BLOB_RETRIEVED),
                RESPONSE_MESSAGE
            );

            assertEquals(1, logCaptor.getInfoLogs().size(),
                       MORE_INFO_THEN_EXPECTED
            );
        }
    }

    @Test
    void testRunnerWithEligibleBlobToProcess() throws JsonProcessingException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));
            when(processingService.processFile(any())).thenReturn("MOCK");
            when(distributionService.sendProcessedJson(any()))
                .thenReturn(new ApiResponse(HttpStatus.NO_CONTENT.value(), ""));

            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn(FILE_DELETED);

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains(ALL_BLOB_RETRIEVED),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(1).contains(ELIGIBLE_BLOB_PROCESS),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(2).contains(ALL_BLOB_PROCESSED),
                RESPONSE_MESSAGE
            );

            assertEquals(3, logCaptor.getInfoLogs().size(),
                       MORE_INFO_THEN_EXPECTED
            );
        }
    }

    @Test
    void testRunnerWithEligibleBlobToProcessReturnSuccess() throws JsonProcessingException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));
            when(processingService.processFile(any())).thenReturn("MOCK");
            when(distributionService.sendProcessedJson(any()))
                .thenReturn(new ApiResponse(HttpStatus.NO_CONTENT.value(), ""));

            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn(FILE_DELETED);

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains(ALL_BLOB_RETRIEVED),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(1).contains(ELIGIBLE_BLOB_PROCESS),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(2).contains(ALL_BLOB_PROCESSED),
                RESPONSE_MESSAGE
            );

            assertEquals(3, logCaptor.getInfoLogs().size(),
                         MORE_INFO_THEN_EXPECTED
            );
        }
    }


    @Test
    void testRunnerWithInvalidBlobToProcess() throws JsonProcessingException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));
            when(processingService.processFile(any())).thenReturn(null);

            when(serviceNowService.createServiceNowRequest(any(), any())).thenReturn(true);
            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn(FILE_DELETED);

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains(ALL_BLOB_RETRIEVED),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(1).contains(ELIGIBLE_BLOB_PROCESS),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(2).contains(ALL_BLOB_PROCESSED),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getErrorLogs().get(0).contains("Failed to valid the file"),
                RESPONSE_MESSAGE
            );

            assertEquals(3, logCaptor.getInfoLogs().size(),
                       MORE_INFO_THEN_EXPECTED
            );

            assertEquals(1, logCaptor.getErrorLogs().size(),
                         MORE_INFO_THEN_EXPECTED
            );
        }
    }

    @Test
    void testRunnerWithEligibleBlobButHmiRequestFailed() throws JsonProcessingException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));
            when(processingService.processFile(any())).thenReturn("MOCK");

            when(distributionService.sendProcessedJson(any()))
                .thenReturn(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Failed"));
            when(serviceNowService.createServiceNowRequest(any(), any())).thenReturn(true);
            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn(FILE_DELETED);

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains(ALL_BLOB_RETRIEVED),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(1).contains(ELIGIBLE_BLOB_PROCESS),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(2).contains("Blob failed"),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(3).contains(ALL_BLOB_PROCESSED),
                RESPONSE_MESSAGE
            );

            assertEquals(4, logCaptor.getInfoLogs().size(),
                         MORE_INFO_THEN_EXPECTED
            );
        }
    }
}
