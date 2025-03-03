package com.stratocloud.exceptions;

public class PermissionNotGrantedException extends StratoException{
    public PermissionNotGrantedException() {
    }

    public PermissionNotGrantedException(String message) {
        super(message);
    }

    public PermissionNotGrantedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermissionNotGrantedException(Throwable cause) {
        super(cause);
    }

    public PermissionNotGrantedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
