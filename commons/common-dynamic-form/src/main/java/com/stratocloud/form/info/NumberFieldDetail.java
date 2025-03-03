package com.stratocloud.form.info;

import java.util.List;

public record NumberFieldDetail(Integer defaultValue,
                                int min,
                                int max,
                                boolean required,
                                List<String> conditions,
                                String placeholder) implements FieldDetail {
}
