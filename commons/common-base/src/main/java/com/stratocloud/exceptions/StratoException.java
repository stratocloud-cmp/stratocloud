package com.stratocloud.exceptions;

public class StratoException extends RuntimeException {

    public StratoException() {
    }

    public StratoException(String message) {
        super(message);
    }

    public StratoException(String message, Throwable cause) {
        super(message, cause);
    }

    public StratoException(Throwable cause) {
        super(cause);
    }

    public StratoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
