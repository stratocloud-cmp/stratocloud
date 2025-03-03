package com.stratocloud.resource.query.inquiry;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.resource.cmd.recycle.RecycleCmd;
import lombok.Data;

import java.util.List;

@Data
public class DestroyResourcesRefundInquiry implements QueryRequest {
    private List<RecycleCmd> resources;
}
