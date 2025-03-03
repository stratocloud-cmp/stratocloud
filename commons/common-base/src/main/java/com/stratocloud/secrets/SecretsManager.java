package com.stratocloud.secrets;

public interface SecretsManager {
    SecretId storeSecret(SecretValue secretValue);

    SecretValue retrieveSecret(SecretId secretId);

    void rotateMasterKey(MasterKey currentMasterKey, MasterKey newMasterKey);
}
