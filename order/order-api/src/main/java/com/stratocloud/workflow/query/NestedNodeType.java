package com.stratocloud.workflow.query;

import com.stratocloud.form.info.DynamicFormMetaData;
import lombok.Data;

@Data
public class NestedNodeType {
    private String nodeType;
    private String nodeTypeName;
    private DynamicFormMetaData nodePropertiesFormMetaData;
}
