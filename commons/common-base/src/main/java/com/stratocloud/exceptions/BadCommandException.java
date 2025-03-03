package com.stratocloud.exceptions;

public class BadCommandException extends StratoException {
    public BadCommandException() {
    }

    public BadCommandException(String message) {
        super(message);
    }

    public BadCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadCommandException(Throwable cause) {
        super(cause);
    }

    public BadCommandException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
