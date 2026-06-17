package com.gamejoint.gamejoint_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 403 FORBIDDEN: The user is known (logged in), but the server refuses to let them do this.
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountRestrictedException extends RuntimeException {
    public AccountRestrictedException(String message) {
        super(message);
    }
}	