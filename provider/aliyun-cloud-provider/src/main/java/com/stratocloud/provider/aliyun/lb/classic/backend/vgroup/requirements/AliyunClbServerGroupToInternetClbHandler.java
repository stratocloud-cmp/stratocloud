package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunInternetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunClbServerGroupToInternetClbHandler extends AliyunClbServerGroupToClbHandler {
    public AliyunClbServerGroupToInternetClbHandler(AliyunClbServerGroupHandler serverGroupHandler,
                                                    AliyunInternetClbHandler clbHandler) {
        super(serverGroupHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_SERVER_GROUP_TO_INTERNET_CLB_RELATIONSHIP";
    }

    @Override
    public String getRequirementName() {
        return "公网CLB";
    }
}
