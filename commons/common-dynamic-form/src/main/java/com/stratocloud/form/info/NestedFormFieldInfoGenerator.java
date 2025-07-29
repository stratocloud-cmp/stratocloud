package com.stratocloud.form.info;

import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.NestedFormField;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NestedFormFieldInfoGenerator implements FieldInfoGenerator{
    @Override
    public Class<?> getFieldAnnotationClass() {
        return NestedFormField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = NestedFormField.class.getSimpleName();

        NestedFormField nestedFormField = field.getAnnotation(NestedFormField.class);

        String key = field.getName();
        String label = nestedFormField.label();
        String description = nestedFormField.description();
        String[] conditions = nestedFormField.conditions();

        FieldDetail detail = new NestedFormFieldDetail(
                convertDefaultValues(nestedFormField.defaultJsonValues()),
                nestedFormField.multiple(),
                nestedFormField.multipleMin(),
                nestedFormField.multipleMax(),
                Utils.isNotEmpty(conditions) ? List.of(conditions) : new ArrayList<>(),
                DynamicFormHelper.generateMetaData(nestedFormField.nestedFormClass())
        );

        return new FieldInfo(type, key, label, description, detail);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertDefaultValues(String[] defaultJsonValues) {
        List<Map<String, Object>> result = new ArrayList<>();

        if(Utils.isNotEmpty(defaultJsonValues)){
            for (String defaultJsonValue : defaultJsonValues) {
                try {
                    result.add(JSON.toJavaObject(defaultJsonValue, Map.class));
                }catch (Exception e){
                    log.warn(e.toString());
                }
            }
        }

        return result;
    }
}
