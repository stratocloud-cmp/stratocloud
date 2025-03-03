package com.stratocloud.jpa.converters;

import com.stratocloud.secrets.SecretUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return SecretUtil.storeSecret(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return SecretUtil.retrieveSecret(dbData);
    }
}
