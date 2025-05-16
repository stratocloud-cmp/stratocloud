package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.actiontrail20200706.Client;
import com.aliyun.actiontrail20200706.models.LookupEventsRequest;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.provider.aliyun.common.AliyunEvent;
import com.stratocloud.provider.aliyun.common.AliyunTimeUtil;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AliyunTrailServiceImpl extends AliyunAbstractService implements AliyunTrailService{
    public AliyunTrailServiceImpl(CacheService cacheService, Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            return new Client(config);
        } catch (Exception e) {
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public List<AliyunEvent> describeEvents(List<String> eventNames,
                                            String resourceType,
                                            String resourceId,
                                            LocalDateTime startTime){
        LookupEventsRequest request = new LookupEventsRequest();

        List<LookupEventsRequest.LookupEventsRequestLookupAttribute> attributes = new ArrayList<>();
        if(Utils.isNotEmpty(eventNames)){
            for (String eventName : eventNames) {
                var attribute = new LookupEventsRequest.LookupEventsRequestLookupAttribute();
                attribute.setKey("EventName");
                attribute.setValue(eventName);
                attributes.add(attribute);
            }
        }

        if(Utils.isNotBlank(resourceType)){
            var attribute = new LookupEventsRequest.LookupEventsRequestLookupAttribute();
            attribute.setKey("ResourceType");
            attribute.setValue(resourceType);
            attributes.add(attribute);
        }

        if(Utils.isNotBlank(resourceId)){
            var attribute = new LookupEventsRequest.LookupEventsRequestLookupAttribute();
            attribute.setKey("ResourceName");
            attribute.setValue(resourceId);
            attributes.add(attribute);
        }

        var actionTypeAttribute = new LookupEventsRequest.LookupEventsRequestLookupAttribute();

        actionTypeAttribute.setKey("EventRW");
        actionTypeAttribute.setValue("Write");
        attributes.add(actionTypeAttribute);

        request.setLookupAttribute(attributes);

        LocalDateTime endTime = LocalDateTime.now().minusSeconds(5L);
        LocalDateTime earliestStartTime = endTime.minusDays(30L).plusSeconds(1L);

        if(earliestStartTime.isAfter(startTime))
            startTime = earliestStartTime;

        request.setEndTime(AliyunTimeUtil.toAliyunDateTime(endTime));
        request.setStartTime(AliyunTimeUtil.toAliyunDateTime(startTime));

        return queryAllByToken(
                () -> buildClient().lookupEvents(request),
                resp -> resp.getBody().getEvents(),
                resp -> resp.getBody().getNextToken(),
                request::setNextToken
        ).stream().map(
                e -> JSON.convert(e, AliyunEvent.class)
        ).toList();
    }

}
