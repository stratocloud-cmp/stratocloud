package com.stratocloud.form.info;

import com.stratocloud.form.NumberField;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class NumberFieldInfoGenerator implements FieldInfoGenerator{
    @Override
    public Class<?> getFieldAnnotationClass() {
        return NumberField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = NumberField.class.getSimpleName();

        NumberField numberField = field.getAnnotation(NumberField.class);

        String key = field.getName();
        String label = numberField.label();
        String description = numberField.description();

        FieldDetail detail = new NumberFieldDetail(
                Utils.isNotEmpty(numberField.defaultValue())?numberField.defaultValue()[0]:null,
                numberField.min(),
                numberField.max(),
                numberField.required(),
                List.of(numberField.conditions()),
                numberField.placeHolder()
        );

        return new FieldInfo(type, key, label, description, detail);
    }
}
