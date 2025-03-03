package com.stratocloud.form.info;

import java.util.List;

public record BooleanFieldDetail(boolean defaultValue, List<String> conditions) implements FieldDetail {
}
