package uk.gov.hmcts.reform.hmi.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {

    private Integer statusCode;

    private String message;
}
