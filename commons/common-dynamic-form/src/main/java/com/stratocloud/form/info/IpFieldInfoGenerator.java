package com.stratocloud.form.info;

import com.stratocloud.form.IpField;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class IpFieldInfoGenerator implements FieldInfoGenerator {
    @Override
    public Class<?> getFieldAnnotationClass() {
        return IpField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = IpField.class.getSimpleName();

        IpField ipField = field.getAnnotation(IpField.class);

        String key = field.getName();
        String label = ipField.label();
        String description = ipField.description();

        FieldDetail detail = new IpFieldDetail(
                ipField.multipleLimit(),
                ipField.protocol(),
                ipField.required(),
                List.of(ipField.conditions()),
                ipField.placeHolder()
        );

        return new FieldInfo(type, key, label, description, detail);
    }
}
