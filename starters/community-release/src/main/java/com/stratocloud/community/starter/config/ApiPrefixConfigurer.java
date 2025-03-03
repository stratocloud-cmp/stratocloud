package com.stratocloud.community.starter.config;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class ApiPrefixConfigurer implements WebMvcConfigurer {

    public static final String GLOBAL_API_PREFIX = "/api";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedHeaders("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedOriginPatterns("*");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                GLOBAL_API_PREFIX,
                c -> c.isAnnotationPresent(RestController.class) || c.isAnnotationPresent(Controller.class)
        );
    }
}
