package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.coc.v1.CocClient;
import com.huaweicloud.sdk.coc.v1.model.*;
import com.huaweicloud.sdk.coc.v1.region.CocRegion;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.stratocloud.cache.CacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class HuaweiCocServiceImpl extends HuaweiAbstractService implements HuaweiCocService{
    public HuaweiCocServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private CocClient buildClient(){
        return CocClient.newBuilder()
                .withCredential(credential)
                .withRegion(CocRegion.CN_NORTH_4)
                .build();
    }

    @Override
    public String createScript(CreateScriptRequest request){
        String scriptId = tryInvoke(
                () -> buildClient().createScript(request).getData()
        );

        log.info("Huawei create script request sent. ScriptId={}.", scriptId);

        return scriptId;
    }

    @Override
    public void deleteScript(String scriptId){
        tryInvoke(
                () -> buildClient().deleteScript(new DeleteScriptRequest().withScriptUuid(scriptId))
        );

        log.info("Huawei delete script request sent. ScriptId={}.", scriptId);
    }

    @Override
    public ExecuteScriptResponse executeScript(ExecuteScriptRequest request){
        ExecuteScriptResponse response = tryInvoke(
                () -> buildClient().executeScript(request)
        );

        log.info("Huawei execute script request sent. ScriptId={}. ExecutionId={}.",
                request.getScriptUuid(), response.getData());

        return response;
    }

    @Override
    public List<ExectionInstanceModel> describeExecutionBatch(String executionId,
                                                              int batchIndex){
        GetScriptJobBatchRequest request = new GetScriptJobBatchRequest();
        request.withExecuteUuid(executionId).withBatchIndex(batchIndex).withMarker(0L);
        return queryAll(
                () -> buildClient().getScriptJobBatch(request).getData().getExecuteInstances(),
                request::setLimit,
                marker -> request.setMarker(Long.valueOf(marker)),
                e -> e.getId().toString()
        );
    }

    @Override
    public Optional<BatchListResourceResponseData> describeResource(String serverId) {
        ListResourceRequest request = new ListResourceRequest();
        request.withResourceIdList(List.of(serverId)).withProvider("ecs").withType("cloudservers");
        return queryAll(
                () -> buildClient().listResource(request).getData(),
                request::setLimit,
                request::setMarker,
                BatchListResourceResponseData::getResourceId
        ).stream().findAny();
    }
}
