package com.stratocloud.resource.query.inquiry;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.resource.cmd.create.CreateResourcesCmd;
import lombok.Data;

@Data
public class CreateResourcesPriceInquiry implements QueryRequest {
    private CreateResourcesCmd createCommand;
}
