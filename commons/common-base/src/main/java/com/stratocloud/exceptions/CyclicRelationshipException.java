package com.stratocloud.exceptions;

public class CyclicRelationshipException extends StratoException{
    public CyclicRelationshipException() {
    }

    public CyclicRelationshipException(String message) {
        super(message);
    }

    public CyclicRelationshipException(String message, Throwable cause) {
        super(message, cause);
    }

    public CyclicRelationshipException(Throwable cause) {
        super(cause);
    }

    public CyclicRelationshipException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
