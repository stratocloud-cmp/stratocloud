package com.stratocloud.form.info;

import java.util.List;

public record InputFieldDetail(String defaultValue,
                               boolean required,
                               List<String> conditions,
                               String regex,
                               String regexMessage,
                               String inputType) implements FieldDetail {
}
