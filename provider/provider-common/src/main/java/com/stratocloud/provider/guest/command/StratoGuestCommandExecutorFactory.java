package com.stratocloud.provider.guest.command;

import org.springframework.beans.factory.InitializingBean;

public interface StratoGuestCommandExecutorFactory
        extends GuestCommandExecutorFactory<StratoGuestCommandExecutor>, InitializingBean {

    @Override
    default void afterPropertiesSet() {
        StratoGuestCommandExecutorFactoryRegistry.register(this);
    };
}
