package com.stratocloud.exceptions;

@SuppressWarnings("unused")
public class TryConsumeLaterException extends StratoException {
    public TryConsumeLaterException() {
    }

    public TryConsumeLaterException(String message) {
        super(message);
    }

    public TryConsumeLaterException(String message, Throwable cause) {
        super(message, cause);
    }

    public TryConsumeLaterException(Throwable cause) {
        super(cause);
    }

    public TryConsumeLaterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
