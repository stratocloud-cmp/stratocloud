package com.stratocloud.account;

import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.jpa.converters.EncryptStringConverter;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.ProviderRegistry;
import com.stratocloud.utils.JSON;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Entity
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalAccount extends Tenanted {
    @Column(nullable = false)
    private String providerId;
    @Column(nullable = false)
    private String name;
    @Convert(converter = EncryptStringConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String propertiesJsonString;
    @Column
    private String description;
    @Column(nullable = false)
    private Boolean disabled;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExternalAccountState state;
    @Column(nullable = false)
    private Float balance;

    public ExternalAccount(String providerId, String name, Map<String, Object> properties, String description) {
        this.providerId = providerId;
        this.name = name;
        this.propertiesJsonString = JSON.toJsonString(properties);
        this.description = description;

        this.disabled = false;
        this.state = ExternalAccountState.CONNECTED;
        this.balance = 0.0f;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProperties(){
        return JSON.toJavaObject(propertiesJsonString, Map.class);
    }

    public void validateConnection() {
        Provider provider = getProvider();
        provider.validateConnection(this);
    }

    public void update(String name, Map<String, Object> properties, String description) {
        this.name = name;
        this.propertiesJsonString = JSON.toJsonString(properties);
        this.description = description;
    }

    public Provider getProvider(){
        return ProviderRegistry.getProvider(providerId);
    }

    public Map<String, Object> getNoSensitiveInfoProperties(){
        HashMap<String, Object> copy = new HashMap<>(getProperties());
        getProvider().eraseSensitiveInfo(copy);
        return copy;
    }

    public void enable() {
        this.disabled = false;
    }

    public void disable() {
        this.disabled = true;
    }

    public void synchronizeState(){
        try {
            getProvider().validateConnection(this);
            this.balance = getProvider().getBalance(this);
            this.state = ExternalAccountState.CONNECTED;
        }catch (ProviderConnectionException e){
            log.error("External account {} connection lost.", name, e);
            this.state = ExternalAccountState.CONNECTION_LOST;
        }catch (ExternalAccountInvalidException e){
            log.error("External account {} is invalid.", name, e);
            this.state = ExternalAccountState.CREDENTIAL_EXPIRED;
        }
    }
}
