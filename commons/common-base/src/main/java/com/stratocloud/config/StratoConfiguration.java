package com.stratocloud.config;

import com.stratocloud.utils.NetworkUtil;
import com.stratocloud.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConfigurationProperties(prefix = "strato")
public class StratoConfiguration implements InitializingBean {
    @Value("${server.port}")
    private int serverPort;
    private static final String localAddress = NetworkUtil.getLocalInetAddress().getHostAddress();

    @Getter
    @Setter
    private boolean enableMockToken = false;
    @Setter
    private String stratoDomainName;


    public String getStratoDomainName() {
        if(Utils.isBlank(stratoDomainName))
            return "%s:%s".formatted(localAddress, serverPort);

        return stratoDomainName;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Strato domain name: {}", getStratoDomainName());
    }
}
