package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.eip.v2.model.CreatePrePaidPublicipRequest;
import com.huaweicloud.sdk.eip.v2.model.CreatePublicipRequest;
import com.huaweicloud.sdk.eip.v2.model.ListPublicipsRequest;
import com.huaweicloud.sdk.eip.v2.model.PublicipShowResp;

import java.util.List;
import java.util.Optional;

public interface HuaweiEipService {
    Optional<PublicipShowResp> describeEip(String eipId);

    List<PublicipShowResp> describeEips(ListPublicipsRequest request);

    void associateEip(String eipId, String portId);

    void disassociateEip(String eipId);

    String createPrePaidEip(CreatePrePaidPublicipRequest request);

    String createEip(CreatePublicipRequest request);

    void deleteEip(String eipId);
}
