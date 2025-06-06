package com.stratocloud.form.info;

import java.util.List;

public record CodeBlockFieldDetail(String defaultValue,
                                   boolean required,
                                   List<String> conditions,
                                   String language) implements FieldDetail {
}
