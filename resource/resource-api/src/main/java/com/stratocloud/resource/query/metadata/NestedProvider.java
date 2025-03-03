package com.stratocloud.resource.query.metadata;

import com.stratocloud.form.info.DynamicFormMetaData;
import lombok.Data;

@Data
public class NestedProvider {
    private String id;
    private String name;
    private DynamicFormMetaData accountFormMetaData;
}
