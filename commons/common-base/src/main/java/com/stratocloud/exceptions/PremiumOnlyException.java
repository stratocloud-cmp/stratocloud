package com.stratocloud.exceptions;

@SuppressWarnings("unused")
public class PremiumOnlyException extends StratoException{
    public PremiumOnlyException() {
    }

    public PremiumOnlyException(String message) {
        super(message);
    }

    public PremiumOnlyException(String message, Throwable cause) {
        super(message, cause);
    }

    public PremiumOnlyException(Throwable cause) {
        super(cause);
    }

    public PremiumOnlyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
