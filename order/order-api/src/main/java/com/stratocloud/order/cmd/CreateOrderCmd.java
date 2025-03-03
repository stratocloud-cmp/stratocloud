package com.stratocloud.order.cmd;

import com.stratocloud.request.ApiCommand;
import com.stratocloud.utils.Assert;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderCmd implements ApiCommand {
    private String orderNo;
    private String orderName;
    private String note;
    private Long workflowId;
    private List<NestedOrderItemCmd> items;

    @Override
    public void validate() {
        Assert.isNotNull(workflowId, "未指定流程");
        Assert.isNotEmpty(items, "订单项不能为空");
    }
}
