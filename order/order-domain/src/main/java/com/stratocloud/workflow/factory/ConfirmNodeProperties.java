package com.stratocloud.workflow.factory;

import com.stratocloud.form.SelectEntityType;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.Source;
import com.stratocloud.workflow.NodeProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConfirmNodeProperties implements NodeProperties {
    @SelectField(label = "处理人", multiSelect = true, source = Source.ENTITY, entityType = SelectEntityType.USER)
    private List<Long> confirmHandlerIds = new ArrayList<>();
}
