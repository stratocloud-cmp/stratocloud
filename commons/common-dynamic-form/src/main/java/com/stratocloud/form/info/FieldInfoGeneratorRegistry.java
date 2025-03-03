package com.stratocloud.form.info;


import com.stratocloud.exceptions.StratoException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FieldInfoGeneratorRegistry {

    private static final Map<Class<?>, FieldInfoGenerator> generatorMap = new ConcurrentHashMap<>();

    public static FieldInfoGenerator get(Class<?> annotationClass){
        FieldInfoGenerator generator = generatorMap.get(annotationClass);
        if(generator == null)
            throw new StratoException("Field generator not found.");

        return generator;
    }

    public static void register(FieldInfoGenerator generator){
        generatorMap.put(generator.getFieldAnnotationClass(), generator);
        log.info("Filed info generator of {} registered.", generator.getFieldAnnotationClass().getSimpleName());
    }

    public static boolean containsAnnotation(Class<?> annotationClass){
        return generatorMap.containsKey(annotationClass);
    }

}
