package com.stratocloud.provider.guest;

import com.stratocloud.utils.Utils;
import lombok.Builder;

@Builder
public record GuestManagementInfo(String managementIp,
                                  int managementPort,
                                  String username,
                                  String password,
                                  String publicKey,
                                  String privateKey,
                                  String passphrase) {


    public boolean hasKeyPairAuthInfo(){
        return Utils.isNotBlank(managementIp) &&
                Utils.isNotBlank(username) &&
                Utils.isNotBlank(publicKey) &&
                Utils.isNotBlank(privateKey);
    }

    public boolean hasPasswordAuthInfo(){
        return Utils.isNotBlank(managementIp) &&
                Utils.isNotBlank(username) &&
                Utils.isNotBlank(password);
    }
}
