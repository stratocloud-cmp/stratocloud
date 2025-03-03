package com.stratocloud.secrets;

import com.stratocloud.utils.Utils;

public class SecretUtil {
    public static String storeSecret(String secret) {
        if(Utils.isBlank(secret))
            return null;

        return SecretsManagerRegistry.getSecretsManager().storeSecret(new SecretValue(secret)).id();
    }

    public static String retrieveSecret(String secretId){
        if(Utils.isBlank(secretId))
            return null;

        return SecretsManagerRegistry.getSecretsManager().retrieveSecret(new SecretId(secretId)).value();
    }
}
