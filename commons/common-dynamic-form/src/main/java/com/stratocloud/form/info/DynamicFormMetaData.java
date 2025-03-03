package com.stratocloud.form.info;

import java.util.List;

public record DynamicFormMetaData(String formClass, List<FieldInfo> fieldInfoList) {
}
