package com.stratocloud.form.info;

import com.stratocloud.form.InputField;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class InputFieldInfoGenerator implements FieldInfoGenerator {


    @Override
    public Class<?> getFieldAnnotationClass() {
        return InputField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = InputField.class.getSimpleName();

        InputField inputField = field.getAnnotation(InputField.class);

        String key = field.getName();
        String label = inputField.label();
        String description = inputField.description();

        FieldDetail detail = new InputFieldDetail(
                inputField.defaultValue(),
                inputField.required(),
                List.of(inputField.conditions()),
                inputField.regex(),
                inputField.regexMessage(),
                inputField.inputType()
        );

        return new FieldInfo(type, key, label, description, detail);
    }
}
