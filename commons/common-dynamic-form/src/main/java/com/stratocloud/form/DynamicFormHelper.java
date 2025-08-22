package com.stratocloud.form;

import com.stratocloud.form.info.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public static DynamicFormMetaData changeNestedFormFieldMetaData(DynamicFormMetaData formMetaData,
                                                                    String key,
                                                                    DynamicFormMetaData nestedFormMetaData){
        List<FieldInfo> fieldInfoList = formMetaData.fieldInfoList();

        FieldInfo replacingFieldInfo = null;
        Integer replacingFieldIndex = null;
        if(Utils.isNotEmpty(fieldInfoList)){
            for (int i = 0; i < fieldInfoList.size(); i++) {
                FieldInfo fieldInfo = fieldInfoList.get(i);
                if(Objects.equals(fieldInfo.key(), key)){
                    if(fieldInfo.detail() instanceof NestedFormFieldDetail nestedFormFieldDetail){
                        NestedFormFieldDetail newDetail = new NestedFormFieldDetail(
                                nestedFormFieldDetail.defaultValues(),
                                nestedFormFieldDetail.multiple(),
                                nestedFormFieldDetail.multipleMin(),
                                nestedFormFieldDetail.multipleMax(),
                                nestedFormFieldDetail.conditions(),
                                nestedFormMetaData
                        );
                        replacingFieldInfo = new FieldInfo(
                                fieldInfo.type(),
                                fieldInfo.key(),
                                fieldInfo.label(),
                                fieldInfo.description(),
                                newDetail
                        );
                        replacingFieldIndex = i;
                        break;
                    }
                }
            }
        }

        List<FieldInfo> newFieldInfoList = new ArrayList<>(fieldInfoList);
        if(replacingFieldIndex != null){
            newFieldInfoList.set(replacingFieldIndex, replacingFieldInfo);
        }
        return new DynamicFormMetaData(formMetaData.formClass(), newFieldInfoList);
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

    public static DynamicFormMetaData changeOptions(DynamicFormMetaData formMetaData,
                                                    String key,
                                                    List<String> options,
                                                    List<String> optionNames){
        List<FieldInfo> fieldInfoList = formMetaData.fieldInfoList();

        FieldInfo replacingFieldInfo = null;
        Integer replacingFieldIndex = null;
        if(Utils.isNotEmpty(fieldInfoList)){
            for (int i = 0; i < fieldInfoList.size(); i++) {
                FieldInfo fieldInfo = fieldInfoList.get(i);
                if(Objects.equals(fieldInfo.key(), key)){
                    if(fieldInfo.detail() instanceof SelectFieldDetail selectFieldDetail){
                        SelectFieldDetail newDetail = new SelectFieldDetail(
                                selectFieldDetail.multiSelect(),
                                selectFieldDetail.allowCreate(),
                                selectFieldDetail.defaultValues(),
                                options,
                                optionNames,
                                selectFieldDetail.source(),
                                selectFieldDetail.entityType(),
                                selectFieldDetail.dependsOn(),
                                selectFieldDetail.required(),
                                selectFieldDetail.conditions(),
                                selectFieldDetail.type(),
                                selectFieldDetail.placeholder()
                        );
                        replacingFieldInfo = new FieldInfo(
                                fieldInfo.type(),
                                fieldInfo.key(),
                                fieldInfo.label(),
                                fieldInfo.description(),
                                newDetail
                        );
                        replacingFieldIndex = i;
                        break;
                    }
                }
            }
        }

        List<FieldInfo> newFieldInfoList = new ArrayList<>(fieldInfoList);
        if(replacingFieldIndex != null){
            newFieldInfoList.set(replacingFieldIndex, replacingFieldInfo);
        }
        return new DynamicFormMetaData(formMetaData.formClass(), newFieldInfoList);
    }


    public static DynamicFormMetaData changeDefaultValues(DynamicFormMetaData formMetaData,
                                                          DynamicForm formData){
        List<FieldInfo> fieldInfoList = formMetaData.fieldInfoList();

        if(Utils.isNotEmpty(fieldInfoList)){
            Map<String, Object> formDataMap = JSON.toMap(formData);

            for (FieldInfo fieldInfo : fieldInfoList) {
                FieldDetail fieldDetail;

                Object o = formDataMap.get(fieldInfo.key());

                if(fieldInfo.detail() instanceof BooleanFieldDetail booleanFieldDetail){
                    if(o instanceof Boolean b){
                        fieldDetail = new BooleanFieldDetail(
                                b,
                                booleanFieldDetail.conditions()
                        );
                    } else {
                        fieldDetail = booleanFieldDetail;
                    }
                } else if(fieldInfo.detail() instanceof SelectFieldDetail selectFieldDetail){
                    List<String> defaultValues;
                    if(o instanceof String s){
                        defaultValues = List.of(s);
                    } else if(o instanceof List<?> l){
                        defaultValues = l.stream().map(Object::toString).toList();
                    } else {
                        defaultValues = selectFieldDetail.defaultValues();
                    }

                    fieldDetail = new SelectFieldDetail(
                            selectFieldDetail.multiSelect(),
                            selectFieldDetail.allowCreate(),
                            defaultValues,
                            selectFieldDetail.options(),
                            selectFieldDetail.optionNames(),
                            selectFieldDetail.source(),
                            selectFieldDetail.entityType(),
                            selectFieldDetail.dependsOn(),
                            selectFieldDetail.required(),
                            selectFieldDetail.conditions(),
                            selectFieldDetail.type(),
                            selectFieldDetail.placeholder()
                    );
                } else if(fieldInfo.detail() instanceof InputFieldDetail inputFieldDetail){
                    if(o instanceof String s){
                        fieldDetail = new InputFieldDetail(
                                s,
                                inputFieldDetail.required(),
                                inputFieldDetail.conditions(),
                                inputFieldDetail.regex(),
                                inputFieldDetail.regexMessage(),
                                inputFieldDetail.inputType(),
                                inputFieldDetail.disabled()
                        );
                    } else {
                        fieldDetail = inputFieldDetail;
                    }
                } else if(fieldInfo.detail() instanceof NumberFieldDetail numberFieldDetail){
                    Integer defaultValue;
                    if(o instanceof Integer i){
                        defaultValue = i;
                    } else if(o instanceof Long l){
                        try {
                            defaultValue = Math.toIntExact(l);
                        }catch (Exception e){
                            defaultValue = numberFieldDetail.defaultValue();
                        }
                    } else if(o instanceof String s){
                        try {
                            defaultValue = Integer.parseInt(s);
                        }catch (Exception e){
                            defaultValue = numberFieldDetail.defaultValue();
                        }
                    }else {
                        defaultValue = numberFieldDetail.defaultValue();
                    }

                    fieldDetail = new NumberFieldDetail(
                            defaultValue,
                            numberFieldDetail.min(),
                            numberFieldDetail.max(),
                            numberFieldDetail.required(),
                            numberFieldDetail.conditions(),
                            numberFieldDetail.placeholder()
                    );
                } else if(fieldInfo.detail() instanceof CodeBlockFieldDetail codeBlockFieldDetail){
                    if(o instanceof String s){
                        fieldDetail = new CodeBlockFieldDetail(
                                s,
                                codeBlockFieldDetail.required(),
                                codeBlockFieldDetail.conditions(),
                                codeBlockFieldDetail.language()
                        );
                    } else {
                        fieldDetail = codeBlockFieldDetail;
                    }
                } else if(fieldInfo.detail() instanceof NestedFormFieldDetail nestedFormFieldDetail){
                    if(o instanceof List<?> list){
                        fieldDetail = new NestedFormFieldDetail(
                                list.stream().map(JSON::toMap).toList(),
                                nestedFormFieldDetail.multiple(),
                                nestedFormFieldDetail.multipleMin(),
                                nestedFormFieldDetail.multipleMax(),
                                nestedFormFieldDetail.conditions(),
                                nestedFormFieldDetail.nestedFormMetadata()
                        );
                    } else if(o != null){
                        fieldDetail = new NestedFormFieldDetail(
                                List.of(JSON.toMap(o)),
                                nestedFormFieldDetail.multiple(),
                                nestedFormFieldDetail.multipleMin(),
                                nestedFormFieldDetail.multipleMax(),
                                nestedFormFieldDetail.conditions(),
                                nestedFormFieldDetail.nestedFormMetadata()
                        );
                    } else {
                        fieldDetail = nestedFormFieldDetail;
                    }
                } else {
                    fieldDetail = fieldInfo.detail();
                }

                formMetaData = changeFieldDetail(formMetaData, fieldInfo.key(), fieldDetail);
            }
        }

        return formMetaData;
    }
}
