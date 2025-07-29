package com.stratocloud.form.info;

import java.util.List;
import java.util.Map;

public record NestedFormFieldDetail(List<Map<String, Object>> defaultValues,
                                    boolean multiple,
                                    int multipleMin,
                                    int multipleMax,
                                    List<String> conditions,
                                    DynamicFormMetaData nestedFormMetadata) implements FieldDetail {
}
