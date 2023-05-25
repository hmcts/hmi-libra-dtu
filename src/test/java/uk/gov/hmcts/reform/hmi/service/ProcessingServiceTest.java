package uk.gov.hmcts.reform.hmi.service;

import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class ProcessingServiceTest {

    @Mock
    AzureBlobService azureBlobService;

    @Mock
    ValidationService validationService;

    @Mock
    ValidationConfiguration validationConfiguration;

    @InjectMocks
    private ProcessingService processingService;

    private static final String TEST = "Test";
    private static final String TEST_DATA = "1234";

    @Test
    void testProcessFile() {
        when(azureBlobService.acquireBlobLease(TEST)).thenReturn(TEST_DATA);
        doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, TEST_DATA);
        when(azureBlobService.downloadBlob(TEST)).thenReturn(TEST.getBytes(StandardCharsets.UTF_8));
        when(validationConfiguration.getLibraHmiSchema()).thenReturn("path");
        when(validationService.isValid(any(), any())).thenReturn(true);

        BlobItem blobItem = new BlobItem();
        blobItem.setName(TEST);

        String result = processingService.processFile(blobItem);
        assertEquals(TEST, result,
                     "Expected and actual didn't match");
    }
}
