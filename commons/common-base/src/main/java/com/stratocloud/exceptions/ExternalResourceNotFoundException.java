package com.stratocloud.exceptions;

public class ExternalResourceNotFoundException extends StratoException{
    public ExternalResourceNotFoundException() {
    }

    public ExternalResourceNotFoundException(String message) {
        super(message);
    }

    public ExternalResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalResourceNotFoundException(Throwable cause) {
        super(cause);
    }

    public ExternalResourceNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
