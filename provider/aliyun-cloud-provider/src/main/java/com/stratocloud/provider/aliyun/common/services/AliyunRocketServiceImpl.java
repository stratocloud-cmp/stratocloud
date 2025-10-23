package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.rocketmq20220801.Client;
import com.aliyun.rocketmq20220801.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.provider.aliyun.rocket.RocketInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class AliyunRocketServiceImpl extends AliyunAbstractService implements AliyunRocketService {
    public AliyunRocketServiceImpl(CacheService cacheService,
                                   Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            config.setEndpoint("rocketmq.%s.aliyuncs.com".formatted(config.getRegionId()));
            return new Client(config);
        } catch (Exception e) {
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public String createInstance(CreateInstanceRequest request){
        CreateInstanceResponseBody body = tryInvoke(() -> buildClient().createInstance(request)).getBody();

        log.info("Aliyun create rocketmq instance request sent. InstanceId={}. RequestId={}.",
                body.getData(), body.getRequestId());

        return body.getData();
    }

    @Override
    public void updateInstance(String instanceId, UpdateInstanceRequest request){
        UpdateInstanceResponseBody body = tryInvoke(() -> buildClient().updateInstance(instanceId, request)).getBody();

        log.info("Aliyun update rocketmq instance request sent. InstanceId={}. RequestId={}.",
                instanceId, body.getRequestId());
    }

    @Override
    public void deleteInstance(String instanceId){
        DeleteInstanceResponseBody body = tryInvoke(() -> buildClient().deleteInstance(instanceId)).getBody();

        log.info("Aliyun delete rocketmq instance request sent. InstanceId={}. RequestId={}.",
                instanceId, body.getRequestId());
    }

    @Override
    public List<RocketInstance> describeInstances(ListInstancesRequest request){
        return queryAll(
                () -> buildClient().listInstances(request).getBody(),
                resp -> resp.getData().getList(),
                resp -> resp.getData().getTotalCount().intValue(),
                request::setPageNumber,
                request::setPageSize
        ).stream().map(RocketInstance::new).toList();
    }

    @Override
    public Optional<RocketInstance> describeInstance(String instanceId){
        ListInstancesRequest request = new ListInstancesRequest();
        request.setFilter(instanceId);

        return describeInstances(request).stream().filter(
                i -> Objects.equals(i.detail().getInstanceId(), instanceId)
        ).findAny();
    }

    @Override
    public GetInstanceResponseBody.GetInstanceResponseBodyData describeInstanceDetail(String instanceId) {
        return tryInvoke(() -> buildClient().getInstance(instanceId)).getBody().getData();
    }
}
