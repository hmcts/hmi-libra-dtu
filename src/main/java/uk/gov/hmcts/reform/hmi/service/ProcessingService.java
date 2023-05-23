package uk.gov.hmcts.reform.hmi.service;

import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
public class ProcessingService {

    private final AzureBlobService azureBlobService;

    @Autowired
    public ProcessingService(AzureBlobService azureBlobService) {
        this.azureBlobService = azureBlobService;
    }

    public String processFile(BlobItem blob) {

        // Move the file to the processing container
        moveFileToProcessingContainer(blob);

        // Read the blob contents
        byte[] blobData  = azureBlobService.downloadBlob(blob.getName());
        //TODO Remove and add better log when functionality added
        log.info(Arrays.toString(blobData));
        log.info(String.format("Download blob %s", blob.getName()));

        //TODO Validation in here etc etc for json file

        return "MOCK - LIBRA DTU TEMP";
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
