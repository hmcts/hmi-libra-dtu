package uk.gov.hmcts.reform.hmi.models;

import lombok.Data;

@Data
public class ApiResponse {

    private Integer statusCode;

    private String message;

    public ApiResponse(Integer statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
