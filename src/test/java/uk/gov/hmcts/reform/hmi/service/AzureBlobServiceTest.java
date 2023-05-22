package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobServiceTest {

    private static final String BLOB_NAME = UUID.randomUUID().toString();
    private static final String TEST = "TEST";
    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";

    @Mock
    BlobContainerClient blobContainerClient;

    @Mock
    BlobClient blobClient;

    @Mock
    PagedIterable<BlobItem> pagedIterable;

    @Mock
    BlobLeaseClient leaseClient;

    @InjectMocks
    @Spy
    AzureBlobService azureBlobService;

    LogCaptor logCaptor = LogCaptor.forClass(AzureBlobService.class);

    @Test
    void testGetBlobs() {
        when(blobContainerClient.listBlobs()).thenReturn(pagedIterable);
        BlobItem blobItem = new BlobItem();
        blobItem.setName(TEST);
        when(pagedIterable.stream()).thenReturn(Stream.of(blobItem));
        List<BlobItem> returnedBlobItems = azureBlobService.getBlobs();
        assertEquals(TEST, returnedBlobItems.get(0).getName(), EXPECTED_MESSAGE);
    }

    @Test
    void testDeleteOriginalBlob() {
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
        assertEquals(String.format("Blob: %s successfully deleted.", BLOB_NAME),
                     azureBlobService.deleteOriginalBlob(BLOB_NAME),
                     EXPECTED_MESSAGE);
    }

    @Test
    void testDeleteProcessingBlob() {
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
        assertEquals(String.format("Blob: %s successfully deleted.", BLOB_NAME),
                     azureBlobService.deleteProcessingBlob(BLOB_NAME),
                     EXPECTED_MESSAGE);
    }

    @Test
    void testCopyBlobToProcessingContainer() {
        BinaryData testData = BinaryData.fromString(TEST);
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
        when(azureBlobService.setupBlobLease(blobClient, Optional.of(TEST))).thenReturn(leaseClient);
        when(blobClient.downloadContent()).thenReturn(testData);
        doNothing().when(leaseClient).releaseLease();

        azureBlobService.copyBlobToProcessingContainer(BLOB_NAME, TEST);

        assertEquals("Blob has been moved to the processing container", logCaptor.getInfoLogs().get(0),
                     EXPECTED_MESSAGE);
    }

    @Test
    void testAcquireBlobLease() {
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
        when(leaseClient.acquireLease(60)).thenReturn(TEST);
        when(azureBlobService.setupBlobLease(blobClient, Optional.empty())).thenReturn(leaseClient);

        String testResponse = azureBlobService.acquireBlobLease(BLOB_NAME);
        assertEquals(TEST, testResponse, EXPECTED_MESSAGE);
    }

    @Test
    void testSetupBlobLeaseWithLeaseId() {
        BlobLeaseClient blobLeaseClient = azureBlobService.setupBlobLease(blobClient, Optional.of(TEST));
        assertEquals(TEST, blobLeaseClient.getLeaseId(), EXPECTED_MESSAGE);
    }

    @Test
    void testSetupBlobLeaseWithoutLeaseId() {
        BlobLeaseClient blobLeaseClient = azureBlobService.setupBlobLease(blobClient, Optional.empty());
        assertNotNull(blobLeaseClient.getLeaseId(), EXPECTED_MESSAGE);
    }

    @Test
    void testDownloadBlob() {
        BinaryData testData = BinaryData.fromString(TEST);
        byte[] testDataBytes = testData.toBytes();
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(testData);

        byte[] testResponse = azureBlobService.downloadBlob(BLOB_NAME);
        assertEquals(testDataBytes, testResponse, EXPECTED_MESSAGE);
    }
}
