package com.itsm.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends ItsmException {

    private AuthenticationException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static AuthenticationException invalidLogin() {
        return new AuthenticationException("INVALID_LOGIN", "Invalid username or password");
    }

    public static AuthenticationException accountDisabled() {
        return new AuthenticationException("ACCOUNT_DISABLED", "This account has been deactivated");
    }

    public static AuthenticationException missingToken() {
        return new AuthenticationException("AUTH_REQUIRED", "Please log in to continue");
    }

    public static AuthenticationException invalidToken() {
        return new AuthenticationException("INVALID_TOKEN", "Your session has expired, please log in again");
    }
}
