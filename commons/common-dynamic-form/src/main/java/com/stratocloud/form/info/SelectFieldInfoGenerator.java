package com.stratocloud.form.info;

import com.stratocloud.form.SelectField;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class SelectFieldInfoGenerator implements FieldInfoGenerator{
    @Override
    public Class<?> getFieldAnnotationClass() {
        return SelectField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = SelectField.class.getSimpleName();

        SelectField selectField = field.getAnnotation(SelectField.class);

        String key = field.getName();
        String label = selectField.label();
        String description = selectField.description();

        FieldDetail detail = new SelectFieldDetail(
                selectField.multiSelect(),
                selectField.allowCreate(),
                List.of(selectField.defaultValues()),
                List.of(selectField.options()),
                List.of(selectField.optionNames()),
                selectField.source(),
                selectField.entityType(),
                List.of(selectField.dependsOn()),
                selectField.required(),
                List.of(selectField.conditions()),
                selectField.type()
        );

        return new FieldInfo(type, key, label, description, detail);
    }
}
