package com.stratocloud.form.info;


import com.stratocloud.form.SelectEntityType;
import com.stratocloud.form.SelectType;
import com.stratocloud.form.Source;

import java.util.List;

public record SelectFieldDetail(boolean multiSelect,
                                boolean allowCreate,
                                List<String> defaultValues,
                                List<String> options,
                                List<String> optionNames,
                                Source source,
                                SelectEntityType entityType,
                                List<String> dependsOn,
                                boolean required,
                                List<String> conditions,
                                SelectType type) implements FieldDetail {
}
