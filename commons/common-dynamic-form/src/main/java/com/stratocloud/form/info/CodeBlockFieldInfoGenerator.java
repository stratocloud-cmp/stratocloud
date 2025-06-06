package com.stratocloud.form.info;

import com.stratocloud.form.CodeBlockField;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class CodeBlockFieldInfoGenerator implements FieldInfoGenerator {


    @Override
    public Class<?> getFieldAnnotationClass() {
        return CodeBlockField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = CodeBlockField.class.getSimpleName();

        CodeBlockField codeBlockField = field.getAnnotation(CodeBlockField.class);

        String key = field.getName();
        String label = codeBlockField.label();
        String description = codeBlockField.description();

        FieldDetail detail = new CodeBlockFieldDetail(
                codeBlockField.defaultValue(),
                codeBlockField.required(),
                List.of(codeBlockField.conditions()),
                codeBlockField.language()
        );

        return new FieldInfo(type, key, label, description, detail);
    }
}
