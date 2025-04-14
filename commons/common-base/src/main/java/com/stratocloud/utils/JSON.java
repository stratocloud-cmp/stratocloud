package com.stratocloud.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.stratocloud.exceptions.StratoException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class JSON {
    private static final ObjectMapper objectMapper = initObjectMapper();

    private static ObjectMapper initObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(getJavaTimeModule());
        mapper.registerModule(getLongTypeModule());

        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        return mapper;
    }

    private static JavaTimeModule getJavaTimeModule() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        return javaTimeModule;
    }

    private static SimpleModule getLongTypeModule(){
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return simpleModule;
    }


    public static String toJsonString(Object o){
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new StratoException(e);
        }
    }

    public static <T> T toJavaObject(String s, Class<T> clazz){
        try {
            return objectMapper.readValue(s, clazz);
        } catch (JsonProcessingException e) {
            throw new StratoException(e);
        }
    }

    public static <T> T toJavaObject(byte[] bytes, Class<T> clazz){
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new StratoException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T clone(T o){
        if(o==null)
            return null;

        Class<T> clazz = (Class<T>) o.getClass();
        return toJavaObject(toJsonString(o), clazz);
    }

    public static <T> T convert(Object o, Class<T> clazz){
        String s = toJsonString(o);
        return toJavaObject(s, clazz);
    }

    public static <T> List<T> convert(Object o, TypeReference<List<T>> typeReference){
        if(o == null)
            return null;

        return toJavaList(toJsonString(o), typeReference);
    }

    public static <T> List<T> toJavaList(String s, TypeReference<List<T>> typeReference){
        try {
            return objectMapper.readValue(s, typeReference);
        } catch (JsonProcessingException e) {
            throw new StratoException(
                    "Failed to convert to java list of %s: %s".formatted(typeReference.getType().getTypeName(), s), e
            );
        }
    }



    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object o) {
        return convert(o, Map.class);
    }
}
