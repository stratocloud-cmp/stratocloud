package com.stratocloud.ip;

import com.stratocloud.jpa.converters.SimpleStringRecordConverter;
import jakarta.persistence.Converter;

@Converter
public class IpAttributeConverter extends SimpleStringRecordConverter<IpAddress> {
}
