package com.csye7255.project.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST)
public class BadRequest extends RuntimeException {
    private String message;
    private HttpStatus status = HttpStatus.BAD_REQUEST;
    public BadRequest(String message) {
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
