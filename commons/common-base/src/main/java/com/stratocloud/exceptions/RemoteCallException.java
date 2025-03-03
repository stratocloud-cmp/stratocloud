package com.stratocloud.exceptions;

import com.stratocloud.request.ErrorResult;
import lombok.Getter;

public class RemoteCallException extends StratoException {

    @Getter
    private ErrorResult errorResult;

    public RemoteCallException() {
    }

    public RemoteCallException(String message) {
        super(message);
    }

    public RemoteCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteCallException(Throwable cause) {
        super(cause);
    }

    public RemoteCallException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RemoteCallException(ErrorResult errorResult){
        super(errorResult.get_errorMessage());
        this.errorResult = errorResult;
    }
}
