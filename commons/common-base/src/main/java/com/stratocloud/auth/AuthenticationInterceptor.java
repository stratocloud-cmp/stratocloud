package com.stratocloud.auth;

import com.stratocloud.cache.CacheService;
import com.stratocloud.config.StratoConfiguration;
import com.stratocloud.constant.ErrorCodes;
import com.stratocloud.constant.RequestHeaderConstants;
import com.stratocloud.request.ErrorResult;
import com.stratocloud.exceptions.PermissionNotGrantedException;
import com.stratocloud.exceptions.UnauthorizedException;
import com.stratocloud.permission.PermissionRequired;
import com.stratocloud.permission.PermissionTarget;
import com.stratocloud.permission.ReadPermissionRequired;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final CacheService cacheService;

    public static final long RENEW_MINUTES = 30L;

    private final Boolean enableMockToken;

    public AuthenticationInterceptor(CacheService cacheService,
                                     StratoConfiguration configurations) {
        this.cacheService = cacheService;
        this.enableMockToken = configurations.isEnableMockToken();
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            boolean tokenRequired = isTokenRequired(handlerMethod);

            if (tokenRequired) {
                try {
                    checkToken(request, handlerMethod);
                }catch (UnauthorizedException e){
                    ErrorResult errorResult = new ErrorResult(ErrorCodes.UNAUTHORIZED, e.getMessage());
                    writeErrorResult(response, errorResult);
                    return false;
                }catch (PermissionNotGrantedException e){
                    ErrorResult errorResult = new ErrorResult(ErrorCodes.PERMISSION_NOT_GRANTED, e.getMessage());
                    writeErrorResult(response, errorResult);
                    return false;
                }
            }
        }
        return true;
    }

    private static void writeErrorResult(HttpServletResponse response, ErrorResult errorResult) throws IOException {
        String body = JSON.toJsonString(
                errorResult
        );
        response.setStatus(errorResult.get_errorCode()/10);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(body);
    }

    private void checkToken(HttpServletRequest request, HandlerMethod handlerMethod) {
        if(CallContext.exists()){
            checkPermission(handlerMethod);
            return;
        }

        String token = request.getHeader(RequestHeaderConstants.X_AUTH_TOKEN);

        if(Utils.isBlank(token))
            throw new UnauthorizedException("尚未登录或会话已失效");

        if(enableMockToken && token.equals("MockToken")){
            UserSession systemSession = CallContext.getSystemSession();
            cacheService.set(token, systemSession, RENEW_MINUTES, ChronoUnit.MINUTES);
        }

        UserSession userSession = cacheService.get(token, UserSession.class);

        if(userSession==null)
            throw new UnauthorizedException("尚未登录或会话已失效");


        cacheService.renew(token, RENEW_MINUTES, ChronoUnit.MINUTES);

        CallContext.register(userSession);

        checkPermission(handlerMethod);
    }

    private static void checkPermission(HandlerMethod handlerMethod) {
        var permissionTarget = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(), PermissionTarget.class
        );
        var permission = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), PermissionRequired.class
        );

        if(permissionTarget==null || permission==null)
            return;

        var readPermission = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), ReadPermissionRequired.class
        );

        if(readPermission != null && !readPermission.checkPermission())
            return;

        CallContext.current().validatePermission(permissionTarget.target(), permission.action());
    }

    private static boolean isTokenRequired(HandlerMethod handlerMethod) {
        boolean requireToken = false;
        CheckToken classCheckToken = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(), CheckToken.class
        );
        if (classCheckToken != null) {
            requireToken = classCheckToken.check();
        }


        CheckToken methodCheckToken = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), CheckToken.class
        );
        if (methodCheckToken != null) {
            requireToken = methodCheckToken.check();
        }
        return requireToken;
    }
    

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        CallContext.unregister();
    }
}
