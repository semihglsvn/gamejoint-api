package com.gamejoint.gamejoint_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 403 FORBIDDEN: The user is logged in, but they don't own this specific resource.
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedOperationException extends RuntimeException {
    public UnauthorizedOperationException(String message) {
        super(message);
    }
}