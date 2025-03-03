package com.stratocloud.provider.huawei.common.services;


import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.kps.v3.KpsClient;
import com.huaweicloud.sdk.kps.v3.model.*;
import com.huaweicloud.sdk.kps.v3.region.KpsRegion;
import com.stratocloud.cache.CacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class HuaweiKpsServiceImpl extends HuaweiAbstractService implements HuaweiKpsService {

    public HuaweiKpsServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private KpsClient buildClient(){
        return KpsClient.newBuilder()
                .withCredential(credential)
                .withRegion(KpsRegion.valueOf(regionId))
                .build();
    }

    @Override
    public Optional<Keypairs> describeKeyPair(String keyPairName){
        return describeKeyPairs().stream().filter(
                kp -> Objects.equals(keyPairName, kp.getKeypair().getName())
        ).findAny();
    }

    @Override
    public List<Keypairs> describeKeyPairs(){
        ListKeypairsRequest request = new ListKeypairsRequest();
        return queryAll(
                () -> buildClient().listKeypairs(request).getKeypairs(),
                limit -> request.setLimit(limit.toString()),
                request::setMarker,
                kp -> kp.getKeypair().getName()
        );
    }

    @Override
    public CreateKeypairResp createKeyPair(CreateKeypairRequest request) {
        CreateKeypairResp resp = tryInvoke(
                () -> buildClient().createKeypair(request)
        ).getKeypair();

        log.info("Huawei create keypair request sent. KeyPairName={}.", resp.getName());

        return resp;
    }

    @Override
    public void deleteKeyPair(String keyPairName) {
        tryInvoke(
                () -> buildClient().deleteKeypair(new DeleteKeypairRequest().withKeypairName(keyPairName))
        );

        log.info("Huawei delete keypair request sent. KeyPairName={}.", keyPairName);
    }

    @Override
    public void associateKeyPair(String serverId, String keyPairName) {
        tryInvoke(
                () -> buildClient().associateKeypair(
                        new AssociateKeypairRequest().withBody(
                                new AssociateKeypairRequestBody().withKeypairName(keyPairName).withServer(
                                        new EcsServerInfo().withId(serverId)
                                )
                        )
                )
        );

        log.info("Huawei associate keypair request sent. KeyPairName={}. ServerId={}.",
                keyPairName, serverId);
    }

    @Override
    public void disassociateKeyPair(String serverId, String keyPairName) {
        tryInvoke(
                () -> buildClient().disassociateKeypair(
                        new DisassociateKeypairRequest().withBody(
                                new DisassociateKeypairRequestBody().withServer(
                                        new DisassociateEcsServerInfo().withId(serverId)
                                )
                        )
                )
        );

        log.info("Huawei disassociate keypair request sent. KeyPairName={}. ServerId={}.",
                keyPairName, serverId);
    }
}
