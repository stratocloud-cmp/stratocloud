package com.stratocloud.exceptions;

@SuppressWarnings("unused")
public class StratoCheckedException extends Exception {
    public StratoCheckedException() {
    }

    public StratoCheckedException(String message) {
        super(message);
    }

    public StratoCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public StratoCheckedException(Throwable cause) {
        super(cause);
    }

    public StratoCheckedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
