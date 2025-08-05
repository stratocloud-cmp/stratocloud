package com.stratocloud.form.info;

import java.time.LocalDateTime;
import java.util.List;

public record DateTimeFieldDetail(List<LocalDateTime> defaultValues,
                                  boolean allowFutureTime,
                                  boolean isRange,
                                  boolean dateOnly,
                                  boolean required,
                                  List<String> conditions,
                                  String placeholder) implements FieldDetail {
}
