package com.stratocloud.stack.blueprint.query;

import com.stratocloud.request.query.QueryRequest;
import lombok.Data;

@Data
public class GenerateCreateStacksCmdRequest implements QueryRequest {
    private Long blueprintId;
}
