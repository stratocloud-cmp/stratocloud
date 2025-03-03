package com.stratocloud.user;

import com.stratocloud.jpa.converters.SimpleStringRecordConverter;
import jakarta.persistence.Converter;

@Converter
public class EncodedPasswordConverter extends SimpleStringRecordConverter<EncodedPassword> {
}
