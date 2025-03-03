package com.stratocloud.audit;

import com.stratocloud.utils.SecurityUtil;
import com.stratocloud.utils.Utils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AuditLogEncryptConverter implements AttributeConverter<String, String> {

    public static final String AUDIT_LOG_KEY = "AUDIT_LOG_KEY123";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if(Utils.isBlank(attribute))
            return null;

        return SecurityUtil.AESEncrypt(attribute, AUDIT_LOG_KEY);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if(Utils.isBlank(dbData))
            return null;

        return SecurityUtil.AESDecrypt(dbData, AUDIT_LOG_KEY);
    }
}
