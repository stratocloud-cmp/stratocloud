package com.stratocloud.exceptions;

public class StratoAuthenticationException extends RuntimeException {
    public StratoAuthenticationException() {
    }

    public StratoAuthenticationException(String message) {
        super(message);
    }

    public StratoAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public StratoAuthenticationException(Throwable cause) {
        super(cause);
    }

    public StratoAuthenticationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
