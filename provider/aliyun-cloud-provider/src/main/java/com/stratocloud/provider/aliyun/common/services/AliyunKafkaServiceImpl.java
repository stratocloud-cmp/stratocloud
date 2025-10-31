package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.alikafka20190916.Client;
import com.aliyun.alikafka20190916.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.provider.aliyun.kafka.KafkaInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class AliyunKafkaServiceImpl extends AliyunAbstractService implements AliyunKafkaService{
    public AliyunKafkaServiceImpl(CacheService cacheService, Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            return new Client(config);
        }catch (Exception e){
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public String createPostPayInstance(CreatePostPayInstanceRequest request){
        request.setRegionId(config.getRegionId());
        var body = tryInvoke(
                () -> buildClient().createPostPayInstance(request)
        ).getBody();

        log.info("Aliyun create kafka post-paid instance request sent. InstanceId={}. RequestId={}.",
                body.getData().getInstanceId(), body.getRequestId());

        return body.getData().getInstanceId();
    }

    @Override
    public String createPrePayInstance(CreatePrePayInstanceRequest request){
        request.setRegionId(config.getRegionId());
        var body = tryInvoke(
                () -> buildClient().createPrePayInstance(request)
        ).getBody();

        log.info("Aliyun create kafka pre-paid instance request sent. InstanceId={}. RequestId={}.",
                body.getData().getInstanceId(), body.getRequestId());

        return body.getData().getInstanceId();
    }


    @Override
    public List<KafkaInstance> describeInstances(GetInstanceListRequest request){
        request.setRegionId(config.getRegionId());

        GetInstanceListResponseBody body = tryInvoke(() -> buildClient().getInstanceList(request)).getBody();

        if(body == null || body.getInstanceList() == null || body.getInstanceList().getInstanceVO() == null)
            return List.of();

        return body.getInstanceList().getInstanceVO().stream().map(KafkaInstance::new).toList();
    }

    @Override
    public Optional<KafkaInstance> describeInstance(String instanceId){
        GetInstanceListRequest request = new GetInstanceListRequest();
        request.setInstanceId(List.of(instanceId));
        return describeInstances(request).stream().findAny();
    }

    @Override
    public void startInstance(StartInstanceRequest request){
        request.setRegionId(config.getRegionId());
        StartInstanceResponseBody body = tryInvoke(() -> buildClient().startInstance(request)).getBody();

        log.info("Aliyun start kafka instance request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }

    @Override
    public void releaseInstance(String instanceId){
        ReleaseInstanceRequest request = new ReleaseInstanceRequest();
        request.setRegionId(config.getRegionId());
        request.setInstanceId(instanceId);

        ReleaseInstanceResponseBody body = tryInvoke(() -> buildClient().releaseInstance(request)).getBody();

        log.info("Aliyun release kafka instance request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }

    @Override
    public void deleteInstance(String instanceId){
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setRegionId(config.getRegionId());
        request.setInstanceId(instanceId);

        DeleteInstanceResponseBody body = tryInvoke(() -> buildClient().deleteInstance(request)).getBody();
        log.info("Aliyun delete kafka instance request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }


    @Override
    public void modifyInstanceName(ModifyInstanceNameRequest request) {
        request.setRegionId(config.getRegionId());

        ModifyInstanceNameResponseBody body = tryInvoke(() -> buildClient().modifyInstanceName(request)).getBody();

        log.info("Aliyun modify kafka instance name request sent. InstanceId={}. RequestId={}.",
                request.getInstanceId(), body.getRequestId());
    }
}
