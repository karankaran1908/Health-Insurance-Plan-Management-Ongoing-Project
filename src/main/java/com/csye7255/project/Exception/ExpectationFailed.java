package com.csye7255.project.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.EXPECTATION_FAILED)
public class ExpectationFailed extends RuntimeException {
    private String message;
    private HttpStatus status = HttpStatus.EXPECTATION_FAILED;
    public ExpectationFailed(String message) {
        super(String.format("%s",message));
        this.message=message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
