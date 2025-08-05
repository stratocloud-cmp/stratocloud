package com.stratocloud.form.info;

import com.stratocloud.form.DateTimeField;
import com.stratocloud.utils.TimeUtil;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DateTimeFieldInfoGenerator implements FieldInfoGenerator{
    @Override
    public Class<?> getFieldAnnotationClass() {
        return DateTimeField.class;
    }

    @Override
    public FieldInfo generate(Field field) {
        String type = DateTimeField.class.getSimpleName();

        DateTimeField dateTimeField = field.getAnnotation(DateTimeField.class);

        String key = field.getName();
        String label = dateTimeField.label();
        String description = dateTimeField.description();

        FieldDetail detail = new DateTimeFieldDetail(
                convertDefaultValues(dateTimeField.defaultValues()),
                dateTimeField.allowFutureTime(),
                dateTimeField.isRange(),
                dateTimeField.dateOnly(),
                dateTimeField.required(),
                List.of(dateTimeField.conditions()),
                dateTimeField.placeHolder()
        );

        return new FieldInfo(type, key, label, description, detail);
    }

    private List<LocalDateTime> convertDefaultValues(long[] defaultValues) {
        List<LocalDateTime> result = new ArrayList<>();

        if(defaultValues != null){
            for (long defaultValue : defaultValues) {
                result.add(TimeUtil.fromBeijingEpochMillis(defaultValue));
            }
        }

        return result;
    }
}
