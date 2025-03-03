package com.stratocloud.resource.query.inquiry;

import com.stratocloud.request.query.QueryRequest;
import com.stratocloud.resource.cmd.relationship.ChangeEssentialRequirementCmd;
import lombok.Data;

import java.util.List;

@Data
public class ChangeEssentialRequirementsPriceInquiry implements QueryRequest {
    private List<ChangeEssentialRequirementCmd> changes;
}
