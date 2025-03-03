package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.kps.v3.model.CreateKeypairRequest;
import com.huaweicloud.sdk.kps.v3.model.CreateKeypairResp;
import com.huaweicloud.sdk.kps.v3.model.Keypairs;

import java.util.List;
import java.util.Optional;

public interface HuaweiKpsService {
    Optional<Keypairs> describeKeyPair(String keyPairName);

    List<Keypairs> describeKeyPairs();

    CreateKeypairResp createKeyPair(CreateKeypairRequest request);

    void deleteKeyPair(String keyPairName);

    void associateKeyPair(String serverId, String keyPairName);

    void disassociateKeyPair(String serverId, String keyPairName);
}
