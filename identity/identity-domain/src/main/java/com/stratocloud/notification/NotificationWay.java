package com.stratocloud.notification;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.jpa.converters.EncryptStringConverter;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationWay extends Tenanted {
    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String name;
    @Column
    private String description;

    @Convert(converter = EncryptStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String propertiesJsonString;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationProviderStatus providerStatus = NotificationProviderStatus.NO_STATE;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "notificationWay")
    private List<NotificationPolicy> policies;

    public NotificationWay(String providerId,
                           String name,
                           String description,
                           Map<String, Object> properties) {
        this.providerId = providerId;
        this.name = name;
        this.description = description;

        if(Utils.isNotEmpty(properties))
            this.propertiesJsonString = JSON.toJsonString(properties);
    }

    public NotificationProvider getProvider(){
        return NotificationProviderRegistry.getProvider(providerId).orElseThrow(
                () -> new StratoException("Notification provider not found: %s".formatted(providerId))
        );
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProperties(){
        if(Utils.isBlank(propertiesJsonString))
            return new HashMap<>();
        return JSON.toJavaObject(propertiesJsonString, Map.class);
    }

    public void validateConnection(){
        getProvider().validateConnection(this);
    }

    public void checkConnectionQuietly(){
        try {
            validateConnection();
            providerStatus = NotificationProviderStatus.NORMAL;
            errorMessage = null;
        }catch (Exception e){
            log.warn(e.toString());
            providerStatus = NotificationProviderStatus.ABNORMAL;
            errorMessage = e.getMessage();
        }
    }

    public void update(String name,
                       String description,
                       Map<String, Object> properties) {
        this.name = name;
        this.description = description;
        if(Utils.isNotEmpty(properties))
            this.propertiesJsonString = JSON.toJsonString(properties);
    }
}
