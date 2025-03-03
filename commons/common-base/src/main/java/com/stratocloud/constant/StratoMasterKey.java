package com.stratocloud.constant;

import com.stratocloud.utils.EnvUtil;

public class StratoMasterKey {
    public static final String VALUE = EnvUtil.getEnv("STRATO_MASTER_KEY", "STRATOCLOUD_KEY1");
}
