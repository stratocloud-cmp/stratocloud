package com.stratocloud.jpa.converters;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.Utils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@Converter
public abstract class SimpleStringRecordConverter<R extends Record> implements AttributeConverter<R, String> {
    @Override
    public String convertToDatabaseColumn(R attribute) {
        if(attribute == null)
            return null;

        Field[] declaredFields = attribute.getClass().getDeclaredFields();

        if(declaredFields.length != 1)
            throw new StratoException("Simple record attribute must have exactly 1 declared field.");

        Field declaredField = declaredFields[0];
        declaredField.setAccessible(true);
        Object o;
        try {
            o = declaredField.get(attribute);
        } catch (IllegalAccessException e) {
            throw new StratoException(e);
        }

        return (String) o;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R convertToEntityAttribute(String dbData) {
        if(Utils.isBlank(dbData))
            return null;

        Class<?> argumentClass = Utils.getTypeArgumentClass(getClass(), SimpleStringRecordConverter.class);

        try{
            Constructor<?> constructor = argumentClass.getConstructor(String.class);

            return (R) constructor.newInstance(dbData);
        }catch (Exception e){
            throw new StratoException("Class %s is not a simple string record.".formatted(argumentClass.getSimpleName()));
        }
    }

}
