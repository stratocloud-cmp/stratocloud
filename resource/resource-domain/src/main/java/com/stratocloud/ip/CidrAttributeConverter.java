package com.stratocloud.ip;

import com.stratocloud.jpa.converters.SimpleStringRecordConverter;
import jakarta.persistence.Converter;

@Converter
public class CidrAttributeConverter extends SimpleStringRecordConverter<Cidr> {
}
