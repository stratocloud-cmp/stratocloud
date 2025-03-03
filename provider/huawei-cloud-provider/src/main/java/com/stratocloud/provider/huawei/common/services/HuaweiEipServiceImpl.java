package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.eip.v2.EipClient;
import com.huaweicloud.sdk.eip.v2.model.*;
import com.huaweicloud.sdk.eip.v2.region.EipRegion;
import com.stratocloud.cache.CacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class HuaweiEipServiceImpl extends HuaweiAbstractService implements HuaweiEipService {
    public HuaweiEipServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private EipClient buildClient(){
        return EipClient.newBuilder()
                .withCredential(credential)
                .withRegion(EipRegion.valueOf(regionId))
                .build();
    }

    @Override
    public Optional<PublicipShowResp> describeEip(String eipId){
        return queryOne(
                () -> buildClient().showPublicip(
                        new ShowPublicipRequest().withPublicipId(eipId)
                ).getPublicip()
        );
    }

    @Override
    public List<PublicipShowResp> describeEips(ListPublicipsRequest request){
        return queryAll(
                () -> buildClient().listPublicips(request).getPublicips(),
                request::setLimit,
                request::setMarker,
                PublicipShowResp::getId
        );
    }

    @Override
    public void associateEip(String eipId, String portId) {
        tryInvoke(
                () -> buildClient().updatePublicip(
                        new UpdatePublicipRequest().withPublicipId(eipId).withBody(
                                new UpdatePublicipsRequestBody().withPublicip(
                                        new UpdatePublicipOption().withPortId(portId)
                                )
                        )
                )
        );

        log.info("Huawei associate eip request sent. EipId={}. PortId={}.",
                eipId, portId);
    }

    @Override
    public void disassociateEip(String eipId) {
        tryInvoke(
                () -> buildClient().updatePublicip(
                        new UpdatePublicipRequest().withPublicipId(eipId).withBody(
                                new UpdatePublicipsRequestBody().withPublicip(
                                        new UpdatePublicipOption()
                                )
                        )
                )
        );

        log.info("Huawei disassociate eip request sent. EipId={}.",
                eipId);
    }

    @Override
    public String createPrePaidEip(CreatePrePaidPublicipRequest request) {
        String eipId = tryInvoke(
                () -> buildClient().createPrePaidPublicip(request)
        ).getPublicip().getId();

        log.info("Huawei create pre-paid eip request sent. EipId={}.",
                eipId);

        return eipId;
    }

    @Override
    public String createEip(CreatePublicipRequest request) {
        String eipId = tryInvoke(
                () -> buildClient().createPublicip(request)
        ).getPublicip().getId();

        log.info("Huawei create post-paid eip request sent. EipId={}.",
                eipId);

        return eipId;
    }

    @Override
    public void deleteEip(String eipId) {
        tryInvoke(
                () -> buildClient().deletePublicip(
                        new DeletePublicipRequest().withPublicipId(eipId)
                )
        );

        log.info("Huawei delete eip request sent. EipId={}.",
                eipId);
    }
}
