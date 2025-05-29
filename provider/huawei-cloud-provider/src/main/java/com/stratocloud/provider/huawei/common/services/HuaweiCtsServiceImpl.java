package com.stratocloud.provider.huawei.common.services;


import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.cts.v3.CtsClient;
import com.huaweicloud.sdk.cts.v3.model.ListTracesRequest;
import com.huaweicloud.sdk.cts.v3.model.Traces;
import com.huaweicloud.sdk.cts.v3.region.CtsRegion;
import com.stratocloud.cache.CacheService;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class HuaweiCtsServiceImpl extends HuaweiAbstractService implements HuaweiCtsService{
    public HuaweiCtsServiceImpl(CacheService cacheService, ICredential credential, String regionId, String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private CtsClient buildClient(){
        return CtsClient.newBuilder()
                .withCredential(credential)
                .withRegion(CtsRegion.valueOf(regionId))
                .build();
    }


    @Override
    public List<Traces> describeEvents(List<String> traceNames,
                                       String resourceType,
                                       String resourceId,
                                       LocalDateTime startTime){
        List<Traces> result = new ArrayList<>();

        if(Utils.isNotEmpty(traceNames)){
            for (String traceName : traceNames) {
                List<Traces> traces = describeEventsByName(traceName, resourceType, resourceId, startTime);

                if(Utils.isNotEmpty(traces))
                    result.addAll(traces);
            }
        }

        return result;
    }

    private List<Traces> describeEventsByName(String traceName,
                                              String resourceType,
                                              String resourceId,
                                              LocalDateTime startTime){
        ListTracesRequest request = new ListTracesRequest();

        request.setResourceType(resourceType);
        request.setTraceName(traceName);
        request.setResourceId(resourceId);

        LocalDateTime endTime = LocalDateTime.now().minusSeconds(5L);
        LocalDateTime earliestStartTime = endTime.minusDays(7L).plusSeconds(1L);

        if(earliestStartTime.isAfter(startTime))
            startTime = earliestStartTime;

        request.setTraceType(ListTracesRequest.TraceTypeEnum.SYSTEM);

        request.setTo(
                endTime.atZone(
                        ZoneId.systemDefault()
                ).withZoneSameInstant(
                        TimeUtil.UTC_ZONE_ID
                ).toInstant().toEpochMilli()
        );
        request.setFrom(
                startTime.atZone(
                        ZoneId.systemDefault()
                ).withZoneSameInstant(
                        TimeUtil.UTC_ZONE_ID
                ).toInstant().toEpochMilli()
        );

        return queryAll(
                () -> buildClient().listTraces(request).getTraces(),
                request::setLimit,
                request::setNext,
                Traces::getTraceId
        );
    }
}
