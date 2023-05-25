package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AzureBlobService {
    private final BlobContainerClient libraBlobContainerClient;
    private final BlobContainerClient processingBlobContainerClient;

    @Autowired
    public AzureBlobService(@Qualifier("libra") BlobContainerClient libraBlobContainerClient,
                            @Qualifier("processing") BlobContainerClient processingBlobContainerClient) {
        this.libraBlobContainerClient = libraBlobContainerClient;
        this.processingBlobContainerClient = processingBlobContainerClient;
    }

    public List<BlobItem> getBlobs() {
        PagedIterable<BlobItem> blobs = libraBlobContainerClient.listBlobs();
        return blobs.stream().toList();
    }

    public String deleteOriginalBlob(String fileName) {
        BlobClient blobClient = libraBlobContainerClient.getBlobClient(fileName);
        blobClient.delete();
        return String.format("Blob: %s successfully deleted.", fileName);
    }

    public String deleteProcessingBlob(String fileName) {
        BlobClient blobClient = processingBlobContainerClient.getBlobClient(fileName);
        blobClient.delete();
        return String.format("Blob: %s successfully deleted.", fileName);
    }

    public void copyBlobToProcessingContainer(String fileName, String leaseId) {
        BlobClient currentBlob = libraBlobContainerClient.getBlobClient(fileName);
        BlobClient processingBlob = processingBlobContainerClient.getBlobClient(fileName);

        BlobLeaseClient leaseClient = setupBlobLease(currentBlob, Optional.of(leaseId));

        leaseClient.releaseLease();

        byte[] currentBlobData = currentBlob.downloadContent().toBytes();
        processingBlob.upload(new ByteArrayInputStream(currentBlobData), currentBlobData.length, true);
        log.info("Blob has been moved to the processing container");
    }

    public String acquireBlobLease(String fileName) {
        BlobClient blob = libraBlobContainerClient.getBlobClient(fileName);
        BlobLeaseClient leaseClient = setupBlobLease(blob, Optional.empty());

        return leaseClient.acquireLease(60);
    }

    public BlobLeaseClient setupBlobLease(BlobClient blobClient, Optional<String> leaseId) {
        if (leaseId.isPresent()) {
            return new BlobLeaseClientBuilder()
                .blobClient(blobClient)
                .leaseId(leaseId.get())
                .buildClient();
        } else {
            return new BlobLeaseClientBuilder()
                .blobClient(blobClient)
                .buildClient();
        }
    }

    public byte[] downloadBlob(String fileName) {
        BlobClient processingBlob = processingBlobContainerClient.getBlobClient(fileName);
        return processingBlob.downloadContent().toBytes();
    }
}
