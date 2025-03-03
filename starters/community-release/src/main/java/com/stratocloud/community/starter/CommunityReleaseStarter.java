package com.stratocloud.community.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("com.stratocloud")
@EnableJpaRepositories("com.stratocloud")
@EntityScan("com.stratocloud")
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
public class CommunityReleaseStarter {
    public static void main(String[] args) {
        SpringApplication.run(CommunityReleaseStarter.class, args);
    }
}