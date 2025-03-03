package com.stratocloud.form.info;

import java.lang.reflect.Field;

public interface FieldInfoGenerator {

    Class<?> getFieldAnnotationClass();

    FieldInfo generate(Field field);

}
