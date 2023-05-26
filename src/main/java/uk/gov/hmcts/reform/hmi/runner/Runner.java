package uk.gov.hmcts.reform.hmi.runner;

import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.LeaseStatusType;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;
import uk.gov.hmcts.reform.hmi.service.ProcessingService;
import uk.gov.hmcts.reform.hmi.service.ServiceNowService;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@Slf4j
public class Runner implements CommandLineRunner {

    private final AzureBlobService azureBlobService;
    private final DistributionService distributionService;
    private final ProcessingService processingService;
    private final ServiceNowService serviceNowService;

    @Autowired
    public Runner(AzureBlobService azureBlobService, DistributionService distributionService,
                  ProcessingService processingService, ServiceNowService serviceNowService) {
        this.azureBlobService = azureBlobService;
        this.distributionService = distributionService;
        this.processingService = processingService;
        this.serviceNowService = serviceNowService;
    }

    @Override
    @SuppressWarnings("PMD")
    public void run(String... args) throws JsonProcessingException {
        List<BlobItem> listOfBlobs = azureBlobService.getBlobs();
        log.info("All blobs retrieved");

        // Select an unlocked blob
        Predicate<BlobItem> isUnlocked = blob -> blob.getProperties().getLeaseStatus().equals(LeaseStatusType.UNLOCKED);
        Optional<BlobItem> blobToProcess =  listOfBlobs.stream().filter(isUnlocked).findFirst();

        if (blobToProcess.isPresent()) {
            log.info("Eligible blob selected to process");
            BlobItem blob = blobToProcess.get();
            StringBuilder responseErrors = new StringBuilder();

            //Process the selected blob
            String jsonData = processingService.processFile(blob);
            if (!StringUtils.isEmpty(jsonData)) {
                String response = distributionService.sendProcessedJson(jsonData);

                if (!StringUtils.isEmpty(response)
                    && response.contains("java.lang.Exception")) {
                    log.info("Blob failed");
                    formatErrorResponse(responseErrors, blob.getName(), response);
                }
            } else {
                log.error(String.format("Failed to valid the file %s against schema.", blob.getName()));
                formatErrorResponse(responseErrors, blob.getName(),
                                    "Failed to valid the file against schema.");
            }

            //Raise SNOW ticket
            if (!responseErrors.toString().isEmpty()) {
                serviceNowService.createServiceNowRequest(responseErrors,
                    String.format("Error while send request to HMI for blob: %s", blob.getName()));
            }

            // Delete the processed file as we no longer need it
            azureBlobService.deleteProcessingBlob(blob.getName());
            log.info("Blob processed, shutting down");
        }
    }

    private void formatErrorResponse(StringBuilder responseErrors, String blobName, String error) {
        String newLine = "\n";
        responseErrors.append(blobName)
            .append(" - ")
            .append(error)
            .append(newLine);
    }
}
