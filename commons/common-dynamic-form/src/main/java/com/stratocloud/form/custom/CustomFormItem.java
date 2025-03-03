package com.stratocloud.form.custom;

import java.util.Map;

public record CustomFormItem(String type,
                             String key,
                             String label,
                             String description,
                             boolean encrypted,
                             Map<String, Object> detail) {
}
