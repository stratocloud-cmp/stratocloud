package com.stratocloud.exceptions;

@SuppressWarnings("unused")
public class StratoUnsupportedException extends StratoException {
    public StratoUnsupportedException() {
    }

    public StratoUnsupportedException(String message) {
        super(message);
    }

    public StratoUnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public StratoUnsupportedException(Throwable cause) {
        super(cause);
    }

    public StratoUnsupportedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
