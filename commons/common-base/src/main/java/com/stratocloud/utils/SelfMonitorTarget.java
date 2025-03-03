package com.stratocloud.utils;

import org.springframework.beans.factory.InitializingBean;

public interface SelfMonitorTarget extends InitializingBean {
    void logStats();

    @Override
    default void afterPropertiesSet() {
        SelfMonitorWorker.register(this);
    }
}
