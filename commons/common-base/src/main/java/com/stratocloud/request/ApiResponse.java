package com.stratocloud.request;

import com.stratocloud.auth.CallContext;
import lombok.Getter;

import java.io.Serializable;

@Getter
public abstract class ApiResponse implements Serializable {

    private final String requestId = CallContext.requestId();
}
