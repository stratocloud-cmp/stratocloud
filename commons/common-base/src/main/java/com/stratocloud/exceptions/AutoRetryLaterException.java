package com.stratocloud.exceptions;

public class AutoRetryLaterException extends StratoException {
    public AutoRetryLaterException() {
    }

    public AutoRetryLaterException(String message) {
        super(message);
    }

    public AutoRetryLaterException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoRetryLaterException(Throwable cause) {
        super(cause);
    }

    public AutoRetryLaterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
