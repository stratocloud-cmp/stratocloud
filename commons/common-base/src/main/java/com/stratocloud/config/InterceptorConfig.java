package com.stratocloud.config;

import com.stratocloud.audit.AuditLogInterceptor;
import com.stratocloud.auth.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    private final AuditLogInterceptor auditLogInterceptor;

    public InterceptorConfig(AuthenticationInterceptor authenticationInterceptor,
                             AuditLogInterceptor auditLogInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.auditLogInterceptor = auditLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor).addPathPatterns("/**").order(1);
        registry.addInterceptor(auditLogInterceptor).addPathPatterns("/**").order(100);
    }
}
