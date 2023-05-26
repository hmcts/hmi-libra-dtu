package uk.gov.hmcts.reform.hmi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

@Slf4j
@Service
public class ServiceNowService {

    private final WebClient webClient;
    private final String url;
    private final String assignmentGroup;
    private final String callerId;
    private final String serviceOffering;
    private final String roleType;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ServiceNowService(@Value("${service-now.sn_url}") String url,
                             @Value("${service-now.username}") String username,
                             @Value("${service-now.password}") String password,
                             @Value("${service-now.assignment_group}") String assignmentGroup,
                             @Value("${service-now.caller_id}") String callerId,
                             @Value("${service-now.service-offering}") String serviceOffering,
                             @Value("${service-now.role_type}") String roleType) {
        this.webClient = WebClient.builder()
            .filter(basicAuthentication(username, password))
            .build();
        this.url = url;
        this.assignmentGroup = assignmentGroup;
        this.callerId = callerId;
        this.serviceOffering = serviceOffering;
        this.roleType = roleType;
    }

    public boolean createServiceNowRequest(StringBuilder errorDescription, String shortDescription)
        throws JsonProcessingException {
        try {
            String response = this.webClient.post().uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(BodyInserters.fromValue(formatBody(errorDescription, shortDescription))).retrieve()
                .bodyToMono(String.class).block();
            if (response != null
                && response.contains("INC")) {
                log.info("ServiceNow ticket has been created");
                return true;
            }
            log.error("Error while create ServiceNow ticket");
            return false;
        } catch (WebClientException ex) {
            log.error("Error while create ServiceNow ticket:", ex.getMessage());
            return false;
        }
    }

    private String formatBody(StringBuilder errorDescription, String shortDescription)
        throws JsonProcessingException {
        ObjectNode body = MAPPER.createObjectNode();
        (body).put("assignment_group", this.assignmentGroup);
        (body).put("caller_id", this.callerId);
        (body).put("category", "Data Issue");
        (body).put("contact_type", "Alert");
        (body).put("description", errorDescription.toString());
        (body).put("impact", "2");
        (body).put("service_offering", this.serviceOffering);
        (body).put("short_description", shortDescription);
        (body).put("subcategory", "Data or File Error - other");
        (body).put("u_role_type", this.roleType);
        (body).put("urgency", "3");
        return MAPPER.writeValueAsString(body);
    }
}
