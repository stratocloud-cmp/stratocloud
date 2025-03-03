package com.stratocloud.user;


import com.stratocloud.exceptions.StratoException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UserAuthenticatorRegistry {

    private static final Map<String, UserAuthenticator> authenticatorMap = new ConcurrentHashMap<>();

    public static UserAuthenticator getAuthenticator(String authType){
        UserAuthenticator authenticator = authenticatorMap.get(authType);

        if(authenticator==null)
            throw new StratoException("Authenticator not found for type: " + authType);

        return authenticator;
    }

    public static void register(UserAuthenticator authenticator){
        authenticatorMap.put(authenticator.getAuthType(), authenticator);
        log.info("User authenticator {} registered.", authenticator.getAuthType());
    }

}
