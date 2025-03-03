package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.coc.v1.model.*;

import java.util.List;
import java.util.Optional;

public interface HuaweiCocService {
    String createScript(CreateScriptRequest request);

    void deleteScript(String scriptId);

    ExecuteScriptResponse executeScript(ExecuteScriptRequest request);

    List<ExectionInstanceModel> describeExecutionBatch(String executionId,
                                                       int batchIndex);

    Optional<BatchListResourceResponseData> describeResource(String serverId);
}
