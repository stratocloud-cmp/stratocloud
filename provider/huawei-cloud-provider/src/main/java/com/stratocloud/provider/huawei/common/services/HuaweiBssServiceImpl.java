package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.bss.v2.BssClient;
import com.huaweicloud.sdk.bss.v2.model.*;
import com.huaweicloud.sdk.bss.v2.region.BssRegion;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.stratocloud.cache.CacheService;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Objects;

public class HuaweiBssServiceImpl extends HuaweiAbstractService implements HuaweiBssService {

    public HuaweiBssServiceImpl(CacheService cacheService,
                                ICredential credential,
                                String regionId,
                                String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private BssClient buildClient(){
        return BssClient.newBuilder()
                .withCredential(credential)
                .withRegion(BssRegion.CN_NORTH_1)
                .build();
    }

    @Override
    public float describeBalance(){
        ShowCustomerAccountBalancesRequest request = new ShowCustomerAccountBalancesRequest();

        List<AccountBalanceV3> balances = queryAll(
                () -> buildClient().showCustomerAccountBalances(request).getAccountBalances()
        );

        float balance = 0f;

        if(Utils.isNotEmpty(balances)){
            for (AccountBalanceV3 balanceV3 : balances) {
                if(Objects.equals("CNY", balanceV3.getCurrency()))
                    balance = balance + balanceV3.getAmount().floatValue();
            }
        }

        return balance;
    }

    @Override
    public ListOnDemandResourceRatingsResponse inquiryOnDemandResources(ListOnDemandResourceRatingsRequest request){
        return tryInvoke(
                () -> buildClient().listOnDemandResourceRatings(request)
        );
    }

    @Override
    public ListRateOnPeriodDetailResponse inquiryPeriodResources(ListRateOnPeriodDetailRequest request){
        return tryInvoke(
                () -> buildClient().listRateOnPeriodDetail(request)
        );
    }
}
