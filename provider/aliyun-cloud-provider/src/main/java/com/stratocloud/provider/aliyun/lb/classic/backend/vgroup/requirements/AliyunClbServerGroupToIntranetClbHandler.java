package com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.requirements;

import com.stratocloud.provider.aliyun.lb.classic.AliyunIntranetClbHandler;
import com.stratocloud.provider.aliyun.lb.classic.backend.vgroup.AliyunClbServerGroupHandler;
import org.springframework.stereotype.Component;

@Component
public class AliyunClbServerGroupToIntranetClbHandler extends AliyunClbServerGroupToClbHandler {
    public AliyunClbServerGroupToIntranetClbHandler(AliyunClbServerGroupHandler serverGroupHandler,
                                                    AliyunIntranetClbHandler clbHandler) {
        super(serverGroupHandler, clbHandler);
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_SERVER_GROUP_TO_INTRANET_CLB_RELATIONSHIP";
    }

    @Override
    public String getRequirementName() {
        return "内网CLB";
    }
}
