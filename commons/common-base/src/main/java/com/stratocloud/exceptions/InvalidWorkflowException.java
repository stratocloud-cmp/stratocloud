package com.stratocloud.exceptions;

public class InvalidWorkflowException extends StratoException {
    public InvalidWorkflowException() {
    }

    public InvalidWorkflowException(String message) {
        super(message);
    }

    public InvalidWorkflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidWorkflowException(Throwable cause) {
        super(cause);
    }

    public InvalidWorkflowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
