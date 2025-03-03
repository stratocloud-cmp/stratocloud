package com.stratocloud.exceptions;

public class ProviderStockException extends StratoException {
    public ProviderStockException() {
    }

    public ProviderStockException(String message) {
        super(message);
    }

    public ProviderStockException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderStockException(Throwable cause) {
        super(cause);
    }

    public ProviderStockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
