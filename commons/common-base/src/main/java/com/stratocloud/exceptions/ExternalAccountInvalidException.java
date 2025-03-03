package com.stratocloud.exceptions;

public class ExternalAccountInvalidException extends StratoException {
    public ExternalAccountInvalidException() {
    }

    public ExternalAccountInvalidException(String message) {
        super(message);
    }

    public ExternalAccountInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalAccountInvalidException(Throwable cause) {
        super(cause);
    }

    public ExternalAccountInvalidException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
