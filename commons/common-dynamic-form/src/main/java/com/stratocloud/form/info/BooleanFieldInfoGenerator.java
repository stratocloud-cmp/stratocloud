package com.stratocloud.form.info;

import com.stratocloud.form.BooleanField;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class BooleanFieldInfoGenerator implements FieldInfoGenerator{
    @Override
    public Class<?> getFieldAnnotationClass() {
        return BooleanField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = BooleanField.class.getSimpleName();

        BooleanField booleanField = field.getAnnotation(BooleanField.class);

        String key = field.getName();
        String label = booleanField.label();
        String description = booleanField.description();

        FieldDetail detail = new BooleanFieldDetail(
                booleanField.defaultValue(),
                List.of(booleanField.conditions())
        );

        return new FieldInfo(type, key, label, description, detail);
    }
}
