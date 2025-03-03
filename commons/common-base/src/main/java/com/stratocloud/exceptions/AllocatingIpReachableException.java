package com.stratocloud.exceptions;

public class AllocatingIpReachableException extends StratoException {
    public AllocatingIpReachableException() {
    }

    public AllocatingIpReachableException(String message) {
        super(message);
    }

    public AllocatingIpReachableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AllocatingIpReachableException(Throwable cause) {
        super(cause);
    }

    public AllocatingIpReachableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
