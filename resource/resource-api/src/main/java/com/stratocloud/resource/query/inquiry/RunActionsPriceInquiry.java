package com.stratocloud.resource.query.inquiry;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.resource.cmd.action.RunActionCmd;
import lombok.Data;

import java.util.List;

@Data
public class RunActionsPriceInquiry implements QueryRequest {
    private List<RunActionCmd> actions;
}
