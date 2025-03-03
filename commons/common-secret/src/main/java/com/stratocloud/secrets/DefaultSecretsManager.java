package com.stratocloud.secrets;

import com.stratocloud.cache.CacheUtil;
import com.stratocloud.cache.local.LocalCacheService;
import com.stratocloud.constant.StratoMasterKey;
import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.utils.SecurityUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DefaultSecretsManager implements SecretsManager {

    public static final String masterKey = StratoMasterKey.VALUE;

    private final SecretRepository secretRepository;

    private final LocalCacheService cacheService;

    public DefaultSecretsManager(SecretRepository secretRepository,
                                 LocalCacheService cacheService) {
        this.secretRepository = secretRepository;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecretId storeSecret(SecretValue secretValue) {
        String encryptedValue = SecurityUtil.AESEncrypt(
                secretValue.value(),
                masterKey
        );

        Secret secret = CacheUtil.queryWithCache(
                cacheService,
                encryptedValue,
                30,
                () -> secretRepository.findByEncryptedValue(encryptedValue).orElse(null),
                new Secret()
        );

        if(secret != null)
            return new SecretId(secret.getId().toString());

        secret = new Secret(encryptedValue);
        secret = secretRepository.saveWithSystemSession(secret);
        return new SecretId(secret.getId().toString());
    }

    @Override
    @Transactional
    public SecretValue retrieveSecret(SecretId secretId) {
        return CacheUtil.queryWithCache(
                cacheService,
                secretId.id(),
                300,
                () -> doRetrieveSecret(secretId),
                new SecretValue(null)
        );
    }

    private SecretValue doRetrieveSecret(SecretId secretId) {
        Secret secret = secretRepository.findById(Long.valueOf(secretId.id())).orElseThrow(
                () -> new EntityNotFoundException("Secret not found: %s".formatted(secretId.id()))
        );
        return new SecretValue(
                SecurityUtil.AESDecrypt(
                        secret.getEncryptedValue(),
                        masterKey
                )
        );
    }

    @Override
    @Transactional
    public void rotateMasterKey(MasterKey currentMasterKey, MasterKey newMasterKey) {
        List<Secret> secrets = secretRepository.findAll();

        secrets.forEach(secret -> rotateMasterKey(secret, currentMasterKey, newMasterKey));

        secretRepository.saveAll(secrets);

        secrets.forEach(secret -> cacheService.remove(secret.getId().toString()));
    }

    private void rotateMasterKey(Secret secret, MasterKey currentMasterKey, MasterKey newMasterKey) {
        String secretValue = SecurityUtil.AESDecrypt(secret.getEncryptedValue(), currentMasterKey.key());
        String newEncryptedValue = SecurityUtil.AESEncrypt(secretValue, newMasterKey.key());
        secret.setEncryptedValue(newEncryptedValue);
    }
}
