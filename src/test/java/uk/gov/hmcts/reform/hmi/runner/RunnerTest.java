package uk.gov.hmcts.reform.hmi.runner;

import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.LeaseStatusType;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;
import uk.gov.hmcts.reform.hmi.service.ProcessingService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunnerTest {

    @Mock
    AzureBlobService azureBlobService;

    @Mock
    DistributionService distributionService;

    @Mock
    ProcessingService processingService;

    @InjectMocks
    Runner runner;

    private static final String TEST = "Test";
    private static final String RESPONSE_MESSAGE = "Info logs did not contain expected message";

    @Test
    void testRunnerWithNoEligibleBlobToProcess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.LOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains("All blobs retrieved"),
                RESPONSE_MESSAGE
            );

            assertTrue(logCaptor.getInfoLogs().size() == 1,
                       "More info logs than expected"
            );
        }
    }

    @Test
    void testRunnerWithEligibleBlobToProcess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));
            when(processingService.processFile(any())).thenReturn("MOCK");

            when(distributionService.sendProcessedJson(any())).thenReturn(CompletableFuture.completedFuture(true));
            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn("fileDeleted");

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains("All blobs retrieved"),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(1).contains("Eligible blob selected to process"),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(2).contains("Blob processed, shutting down"),
                RESPONSE_MESSAGE
            );

            assertTrue(logCaptor.getInfoLogs().size() == 3,
                       "More info logs than expected"
            );
        }
    }

    @Test
    void testRunnerWithInvalidBlobToProcess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));
            when(processingService.processFile(any())).thenReturn(null);

            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn("fileDeleted");

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains("All blobs retrieved"),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(1).contains("Eligible blob selected to process"),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(2).contains("Blob processed, shutting down"),
                RESPONSE_MESSAGE
            );

            assertTrue(logCaptor.getInfoLogs().size() == 3,
                       "More info logs than expected"
            );
        }
    }
}
