package uk.gov.hmcts.reform.hmi.service;

import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class ProcessingService {

    private final AzureBlobService azureBlobService;

    private final ValidationService validationService;

    private final ValidationConfiguration validationConfiguration;

    @Autowired
    public ProcessingService(AzureBlobService azureBlobService,
                             ValidationService validationService,
                             ValidationConfiguration validationConfiguration) {
        this.azureBlobService = azureBlobService;
        this.validationService = validationService;
        this.validationConfiguration = validationConfiguration;
    }

    public String processFile(BlobItem blob) {

        // Move the file to the processing container
        moveFileToProcessingContainer(blob);

        // Read the blob contents
        byte[] blobData  = azureBlobService.downloadBlob(blob.getName());

        log.info(String.format("Download blob %s", blob.getName()));

        //VALIDATE JSON FILE AGAINST SCHEMA FILE PROVIDED BY ROTA
        boolean isFileValid = validationService.isValid(validationConfiguration.getLibraHmiSchema(), blobData);

        log.info(String.format("Blob %s validation: %s", blob.getName(), isFileValid));

        if (isFileValid) {
            return new String(blobData, StandardCharsets.UTF_8);
        }

        return null;
    }

    private void moveFileToProcessingContainer(BlobItem blob) {
        // Lease it for 60 seconds
        String leaseId = azureBlobService.acquireBlobLease(blob.getName());

        // Break the lease and copy blob for processing
        azureBlobService.copyBlobToProcessingContainer(blob.getName(), leaseId);

        // Delete the original blob
        azureBlobService.deleteOriginalBlob(blob.getName());
    }
}
