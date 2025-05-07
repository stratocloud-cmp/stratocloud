package com.stratocloud.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationProviderRegistry {
    private static final Map<String, NotificationProvider> providersMap = new ConcurrentHashMap<>();

    public static void register(NotificationProvider provider){
        providersMap.put(provider.getId(), provider);
    }

    public static Optional<NotificationProvider> getProvider(String providerId) {
        return Optional.ofNullable(providersMap.get(providerId));
    }

    public static List<NotificationProvider> getProviders(){
        return new ArrayList<>(providersMap.values());
    }
}
