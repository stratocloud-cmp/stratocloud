package com.stratocloud.exceptions;

public class ProviderConnectionException extends StratoException {
    public ProviderConnectionException() {
    }

    public ProviderConnectionException(String message) {
        super(message);
    }

    public ProviderConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderConnectionException(Throwable cause) {
        super(cause);
    }

    public ProviderConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
