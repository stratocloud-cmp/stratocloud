package com.stratocloud.request;

import com.stratocloud.auth.CallContext;
import com.stratocloud.constant.ErrorCodes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResult {
    private int _errorCode = ErrorCodes.UNEXPECTED_ERROR;
    private String _errorMessage;
    private List<Object> details;
    private String requestId;

    public ErrorResult(int errorCode, String _errorMessage) {
        this._errorCode = errorCode;
        this._errorMessage = _errorMessage;
        requestId = CallContext.requestId();
    }
}
