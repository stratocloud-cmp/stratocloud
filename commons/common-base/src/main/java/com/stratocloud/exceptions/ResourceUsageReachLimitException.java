package com.stratocloud.exceptions;

@SuppressWarnings("unused")
public class ResourceUsageReachLimitException extends StratoException {
    public ResourceUsageReachLimitException() {
    }

    public ResourceUsageReachLimitException(String message) {
        super(message);
    }

    public ResourceUsageReachLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceUsageReachLimitException(Throwable cause) {
        super(cause);
    }

    public ResourceUsageReachLimitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
