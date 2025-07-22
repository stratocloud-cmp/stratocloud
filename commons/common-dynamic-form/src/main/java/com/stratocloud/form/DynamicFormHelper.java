package com.stratocloud.form;

import com.stratocloud.form.info.*;
import com.stratocloud.utils.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DynamicFormHelper {

    public static DynamicFormMetaData generateMetaData(Class<? extends DynamicForm> clazz){
        List<FieldInfo> fieldInfoList = new ArrayList<>();

        Field[] declaredFields = getAllFields(clazz);

        for (Field declaredField : declaredFields) {
            collectFieldInfo(fieldInfoList, declaredField);
        }

        return new DynamicFormMetaData(clazz.getSimpleName(), fieldInfoList);
    }

    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        while (clazz != null){
            fields.addAll(0, List.of(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }

        return fields.toArray(new Field[0]);
    }

    private static void collectFieldInfo(List<FieldInfo> result, Field declaredField) {
        Annotation[] annotations = declaredField.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if(FieldInfoGeneratorRegistry.containsAnnotation(annotationType)){
                FieldInfoGenerator generator = FieldInfoGeneratorRegistry.get(annotationType);
                FieldInfo fieldInfo = generator.generate(declaredField);
                result.add(fieldInfo);
                return;
            }
        }
    }

    public static DynamicFormMetaData changeFieldDetail(DynamicFormMetaData formMetaData,
                                                        String key,
                                                        FieldDetail fieldDetail){
        List<FieldInfo> fieldInfoList = new ArrayList<>();

        if(Utils.isNotEmpty(formMetaData.fieldInfoList())){
            for (FieldInfo fieldInfo : formMetaData.fieldInfoList()) {
                if(Objects.equals(key, fieldInfo.key())){

                    FieldInfo newFieldInfo = new FieldInfo(
                            fieldInfo.type(),
                            fieldInfo.key(),
                            fieldInfo.label(),
                            fieldInfo.description(),
                            fieldDetail
                    );
                    fieldInfoList.add(newFieldInfo);
                } else {
                    fieldInfoList.add(fieldInfo);
                }
            }
        }

        return new DynamicFormMetaData(
                formMetaData.formClass(),
                fieldInfoList
        );
    }
}
