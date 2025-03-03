package com.stratocloud.form.info;

import com.stratocloud.ip.InternetProtocol;

import java.util.List;

public record IpFieldDetail(int multipleLimit,
                            InternetProtocol protocol,
                            boolean required,
                            List<String> conditions,
                            String placeholder) implements FieldDetail {
}
