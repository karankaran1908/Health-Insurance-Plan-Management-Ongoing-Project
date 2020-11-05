package com.csye7255.project.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
public class PreconditionFailed extends RuntimeException {
    private String message;
    private HttpStatus status = HttpStatus.PRECONDITION_FAILED;
    public PreconditionFailed(String message) {
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
