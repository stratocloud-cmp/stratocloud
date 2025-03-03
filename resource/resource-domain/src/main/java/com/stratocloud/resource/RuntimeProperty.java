package com.stratocloud.resource;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RuntimeProperty extends Auditable {
    @ManyToOne
    private Resource resource;

    @Column(name = "property_key", nullable = false)
    private String key;
    @Column(nullable = false)
    private String keyName;
    @Column(name = "property_value", nullable = false, columnDefinition = "TEXT")
    private String value;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String valueName;
    @Column( nullable = false)
    private Boolean displayable = true;
    @Column(nullable = false)
    private Boolean searchable = true;
    @Column(nullable = false)
    private Boolean displayInList = false;

    public static RuntimeProperty ofDisplayable(String key, String keyName, String value, String valueName){
        RuntimeProperty runtimeProperty = new RuntimeProperty();
        runtimeProperty.setKey(key);
        runtimeProperty.setKeyName(keyName);
        runtimeProperty.setValue(value);
        runtimeProperty.setValueName(valueName);
        return runtimeProperty;
    }

    public static RuntimeProperty ofDisplayInList(String key, String keyName, String value, String valueName){
        RuntimeProperty runtimeProperty = new RuntimeProperty();
        runtimeProperty.setKey(key);
        runtimeProperty.setKeyName(keyName);
        runtimeProperty.setValue(value);
        runtimeProperty.setValueName(valueName);
        runtimeProperty.setDisplayInList(true);
        return runtimeProperty;
    }


    public static RuntimeProperty ofHidden(String key, String keyName, String value, String valueName){
        RuntimeProperty runtimeProperty = new RuntimeProperty();
        runtimeProperty.setKey(key);
        runtimeProperty.setKeyName(keyName);
        runtimeProperty.setValue(value);
        runtimeProperty.setValueName(valueName);
        runtimeProperty.setDisplayable(false);
        runtimeProperty.setSearchable(false);
        runtimeProperty.setDisplayInList(false);
        return runtimeProperty;
    }

    public static RuntimeProperty of(String key, String keyName, String value, String valueName,
                                     boolean displayable, boolean searchable, boolean displayInList){
        RuntimeProperty runtimeProperty = new RuntimeProperty();
        runtimeProperty.setKey(key);
        runtimeProperty.setKeyName(keyName);
        runtimeProperty.setValue(value);
        runtimeProperty.setValueName(valueName);
        runtimeProperty.setDisplayable(displayable);
        runtimeProperty.setSearchable(searchable);
        runtimeProperty.setDisplayInList(displayInList);
        return runtimeProperty;
    }
}
