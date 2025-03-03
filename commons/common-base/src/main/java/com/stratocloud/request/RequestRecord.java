package com.stratocloud.request;

import com.stratocloud.auth.CallContext;
import com.stratocloud.auth.UserSession;
import com.stratocloud.identity.SimpleUser;
import com.stratocloud.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.time.LocalDateTime;

@Slf4j
public record RequestRecord(String requestId,
                            Long tenantId,
                            SimpleUser requestedBy,
                            LocalDateTime requestedAt,
                            String sourceIp,
                            String path,
                            String requestBody,
                            String responseBody,
                            int statusCode) {

    public static RequestRecord build(HttpServletRequest request,
                                      HttpServletResponse response,
                                      CallContext callContext,
                                      String sourceIpHeader,
                                      boolean hideRequestBody,
                                      boolean hideResponseBody){
        UserSession callingUser = callContext.getCallingUser();

        SimpleUser simpleUser = new SimpleUser(callingUser.userId(), callingUser.loginName(), callingUser.realName());

        String sourceIpAddress;

        if(Utils.isNotBlank(sourceIpHeader))
            sourceIpAddress = request.getHeader(sourceIpHeader);
        else
            sourceIpAddress = request.getRemoteAddr();

        String requestBody = null;
        String responseBody = null;

        if(!hideRequestBody && (request instanceof ContentCachingRequestWrapper requestWrapper))
            requestBody = requestWrapper.getContentAsString();
        else if(!hideRequestBody)
            log.warn("Cannot retrieve request body.");


        if(!hideResponseBody && (response instanceof ContentCachingResponseWrapper responseWrapper))
            responseBody = new String(responseWrapper.getContentAsByteArray());
        else if(!hideResponseBody)
            log.warn("Cannot retrieve response body.");

        return new RequestRecord(
                callContext.getRequestId(),
                callingUser.tenantId(),
                simpleUser,
                LocalDateTime.now(),
                sourceIpAddress,
                request.getRequestURI(),
                requestBody,
                responseBody,
                response.getStatus()
        );
    }

}
