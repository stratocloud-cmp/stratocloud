package com.stratocloud.provider;

import com.stratocloud.form.custom.CustomForm;
import com.stratocloud.form.custom.CustomFormItem;
import com.stratocloud.provider.constants.GuestConstants;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.guest.GuestManagementInfo;
import com.stratocloud.provider.guest.GuestOsHandler;
import com.stratocloud.resource.OsType;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.RuntimeProperty;
import com.stratocloud.secrets.SecretUtil;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class RuntimePropertiesUtil {

    public static Optional<RuntimeProperty> getRuntimePropertyByKey(Resource resource, String key){
        if(Utils.isEmpty(resource.getRuntimeProperties()))
            return Optional.empty();

        return resource.getRuntimeProperties().stream().filter(
                rp -> Objects.equals(rp.getKey(), key)
        ).findAny();
    }

    public static Optional<String> getRuntimePropertyValueByKey(Resource resource, String key){
        return getRuntimePropertyByKey(resource, key).map(RuntimeProperty::getValue).filter(Utils::isNotBlank);
    }

    public static Optional<String> getManagementIp(Resource resource){
        return getRuntimePropertyValueByKey(resource, "managementIp");
    }

    public static void setManagementIp(Resource resource, String managementIp){
        if(Utils.isBlank(managementIp))
            return;
        RuntimeProperty managementIpProperty = RuntimeProperty.ofDisplayable(
                "managementIp", "管理IP", managementIp, managementIp
        );
        resource.addOrUpdateRuntimeProperty(managementIpProperty);
    }

    public static Optional<Integer> getManagementPort(Resource resource){
        Optional<String> managementPortStr = getRuntimePropertyValueByKey(resource, "managementPort");

        if(managementPortStr.isEmpty())
            return Optional.empty();

        try {
            return Optional.of(Integer.parseInt(managementPortStr.get()));
        }catch (Exception e){
            log.warn(e.toString());
            return Optional.empty();
        }
    }

    private static int getDefaultManagementPort(Resource resource) {
        if(resource.getResourceHandler() instanceof GuestOsHandler guestOsHandler){
            OsType osType = guestOsHandler.getOsTypeQuietly(resource);
            if(osType == OsType.Linux)
                return GuestConstants.SSH_PORT;
            else if(osType == OsType.Windows)
                return GuestConstants.WINRM_PORT;
            else
                return GuestConstants.SSH_PORT;
        } else {
            return GuestConstants.SSH_PORT;
        }
    }

    public static void setManagementPort(Resource resource, int managementPort){
        RuntimeProperty managementPortProperty = RuntimeProperty.ofDisplayable(
                "managementPort",
                "管理端口",
                String.valueOf(managementPort),
                String.valueOf(managementPort)
        );
        resource.addOrUpdateRuntimeProperty(managementPortProperty);
    }

    public static Optional<String> getManagementUser(Resource resource){
        return getRuntimePropertyValueByKey(resource, "managementUser");
    }

    private static String getDefaultManagementUser(Resource resource) {
        if(resource.getResourceHandler() instanceof GuestOsHandler guestOsHandler)
            if(guestOsHandler.getOsTypeQuietly(resource) == OsType.Windows)
                return GuestConstants.WINDOWS_ADMIN;

        return GuestConstants.LINUX_ROOT;
    }

    public static void setManagementUser(Resource resource, String managementUser){
        if(Utils.isBlank(managementUser))
            return;
        RuntimeProperty managementUserProperty = RuntimeProperty.ofDisplayable(
                "managementUser", "管理用户", managementUser, managementUser
        );
        resource.addOrUpdateRuntimeProperty(managementUserProperty);
    }

    public static Optional<String> getManagementPassword(Resource resource){
        try {
            return getRuntimePropertyValueByKey(resource, "managementPassword").map(SecretUtil::retrieveSecret);
        }catch (Exception e){
            log.warn(e.toString());
            return Optional.empty();
        }
    }

    public static void setManagementPassword(Resource resource, String managementPassword){
        if(Utils.isBlank(managementPassword))
            return;

        String encryptedPassword = SecretUtil.storeSecret(managementPassword);
        RuntimeProperty managementPasswordProperty = RuntimeProperty.ofHidden(
                "managementPassword", "管理密码", encryptedPassword, encryptedPassword
        );
        resource.addOrUpdateRuntimeProperty(managementPasswordProperty);
    }


    public static Optional<String> getManagementPublicKey(Resource resource){
        try {
            return getRuntimePropertyValueByKey(resource, "managementPublicKey").map(SecretUtil::retrieveSecret);
        }catch (Exception e){
            log.warn(e.toString());
            return Optional.empty();
        }
    }

    public static void setManagementPublicKey(Resource resource, String managementPublicKey){
        if(Utils.isBlank(managementPublicKey))
            return;

        String encryptedPublicKey = SecretUtil.storeSecret(managementPublicKey);
        RuntimeProperty managementPublicKeyProperty = RuntimeProperty.ofDisplayable(
                "managementPublicKey", "管理公钥", encryptedPublicKey, encryptedPublicKey
        );
        resource.addOrUpdateRuntimeProperty(managementPublicKeyProperty);
    }

    public static Optional<String> getManagementPrivateKey(Resource resource){
        try {
            return getRuntimePropertyValueByKey(resource, "managementPrivateKey").map(SecretUtil::retrieveSecret);
        }catch (Exception e){
            log.warn(e.toString());
            return Optional.empty();
        }
    }

    public static void setManagementPrivateKey(Resource resource, String managementPrivateKey){
        if(Utils.isBlank(managementPrivateKey))
            return;

        String encryptedPrivateKey = SecretUtil.storeSecret(managementPrivateKey);
        RuntimeProperty managementPrivateKeyProperty = RuntimeProperty.ofHidden(
                "managementPrivateKey", "管理私钥", encryptedPrivateKey, encryptedPrivateKey
        );
        resource.addOrUpdateRuntimeProperty(managementPrivateKeyProperty);
    }

    public static Optional<String> getManagementPassphrase(Resource resource){
        try {
            return getRuntimePropertyValueByKey(resource, "managementPassphrase").map(SecretUtil::retrieveSecret);
        }catch (Exception e){
            log.warn(e.toString());
            return Optional.empty();
        }
    }

    public static void setManagementPassphrase(Resource resource, String managementPassphrase){
        if(Utils.isBlank(managementPassphrase))
            return;

        String encryptedPassphrase = SecretUtil.storeSecret(managementPassphrase);
        RuntimeProperty managementPassphraseProperty = RuntimeProperty.ofHidden(
                "managementPassphrase", "管理私钥短语", encryptedPassphrase, encryptedPassphrase
        );
        resource.addOrUpdateRuntimeProperty(managementPassphraseProperty);
    }

    public static void copyGuestManagementInfoQuietly(Resource source, Resource destination){
        try {
            copyGuestManagementInfo(source, destination);
        }catch (Exception e){
            log.warn(e.toString());
        }
    }

    public static void copyManagementIpInfo(Resource source, Resource destination){
        Optional<String> sourceIp = getManagementIp(source);
        Optional<String> destinationIp = getManagementIp(destination);

        if(sourceIp.isPresent() && destinationIp.isEmpty())
            setManagementIp(destination, sourceIp.get());
    }

    private static void copyGuestManagementInfo(Resource source, Resource destination){
        Optional<Integer> sourcePort = getManagementPort(source);
        Optional<Integer> destinationPort = getManagementPort(destination);

        if(sourcePort.isPresent() && destinationPort.isEmpty()){
            setManagementPort(destination, sourcePort.get());
        }

        Optional<String> sourceUser = getManagementUser(source);
        Optional<String> destinationUser = getManagementUser(destination);

        if(sourceUser.isPresent() && destinationUser.isEmpty()){
            setManagementUser(destination, sourceUser.get());
        }

        Optional<String> sourcePassword = getManagementPassword(source);
        Optional<String> destinationPassword = getManagementPassword(destination);

        if(sourcePassword.isPresent() && destinationPassword.isEmpty()){
            setManagementPassword(destination, sourcePassword.get());
        }

        copyKeyPairInfo(source, destination);
    }

    private static void copyKeyPairInfo(Resource source, Resource destination) {
        Optional<String> sourcePublicKey = getManagementPublicKey(source);
        Optional<String> destinationPublicKey = getManagementPublicKey(destination);

        if(sourcePublicKey.isPresent() && destinationPublicKey.isEmpty()){
            setManagementPublicKey(destination, sourcePublicKey.get());
        }

        Optional<String> sourcePrivateKey = getManagementPrivateKey(source);
        Optional<String> destinationPrivateKey = getManagementPrivateKey(destination);

        if(sourcePrivateKey.isPresent() && destinationPrivateKey.isEmpty()){
            setManagementPrivateKey(destination, sourcePrivateKey.get());
        }

        Optional<String> sourcePassphrase = getManagementPassphrase(source);
        Optional<String> destinationPassphrase = getManagementPassphrase(destination);

        if(sourcePassphrase.isPresent() && destinationPassphrase.isEmpty()){
            setManagementPassphrase(destination, sourcePassphrase.get());
        }
    }

    private static void autoSyncGuestManagementInfo(Resource guestOsResource){
        var image = guestOsResource.getEssentialTarget(ResourceCategories.IMAGE);

        image.ifPresent(img -> copyGuestManagementInfo(img, guestOsResource));

        var keyPair = guestOsResource.getRequirementTargets(ResourceCategories.KEY_PAIR).stream().findAny();

        keyPair.ifPresent(kp -> copyKeyPairInfo(kp, guestOsResource));
    }

    public static void autoSyncGuestManagementInfoQuietly(Resource guestOsResource){
        try {
            autoSyncGuestManagementInfo(guestOsResource);
        }catch (Exception e){
            log.warn(e.toString());
        }
    }


    public static GuestManagementInfo retrieveGuestManagementInfo(Resource resource) {
        String managementIp = getManagementIp(resource).orElse(null);

        int managementPort = getManagementPort(resource).orElse(getDefaultManagementPort(resource));

        String managementUser = getManagementUser(resource).orElse(getDefaultManagementUser(resource));

        String password = getManagementPassword(resource).orElse(null);

        String publicKey = getManagementPublicKey(resource).orElse(null);

        String privateKey = getManagementPrivateKey(resource).orElse(null);

        String passphrase = getManagementPassphrase(resource).orElse(null);

        return GuestManagementInfo.builder()
                .managementIp(managementIp)
                .managementPort(managementPort)
                .username(managementUser)
                .password(password)
                .publicKey(publicKey)
                .privateKey(privateKey)
                .passphrase(passphrase).build();
    }


    public static Map<String, String> getRuntimePropertiesMap(Resource resource) {
        Map<String, String> result = new HashMap<>();
        if(Utils.isNotEmpty(resource.getRuntimeProperties())){
            for (RuntimeProperty runtimeProperty : resource.getRuntimeProperties()) {
                result.put(runtimeProperty.getKey(), runtimeProperty.getValue());
            }
        }
        return result;
    }

    public static void setDisplayableRuntimeProperties(Resource resource, Map<String, String> runtimeProperties){
        if(Utils.isEmpty(runtimeProperties))
            return;

        for (var entry : runtimeProperties.entrySet()) {
            RuntimeProperty runtimeProperty = RuntimeProperty.ofDisplayable(
                    entry.getKey(), entry.getKey(), entry.getValue(), entry.getValue()
            );
            resource.addOrUpdateRuntimeProperty(runtimeProperty);
        }
    }

    public static void setCustomFormRuntimeProperties(Resource resource,
                                                      Map<String, Object> customFormData,
                                                      CustomForm customForm){
        if(Utils.isEmpty(customFormData) || Utils.isEmpty(customForm.items()))
            return;

        for (CustomFormItem item : customForm.items()) {
            Object value = customFormData.get(item.key());

            if(value == null)
                continue;

            String valueStr;

            if(value instanceof List<?> list)
                valueStr = String.join(",", list.stream().map(Object::toString).toList());
            else
                valueStr = value.toString();

            RuntimeProperty runtimeProperty;
            if(item.encrypted()) {
                valueStr = SecretUtil.storeSecret(valueStr);
                runtimeProperty = RuntimeProperty.ofHidden(item.key(), item.key(), valueStr, valueStr);
            } else {
                runtimeProperty = RuntimeProperty.ofDisplayable(item.key(), item.key(), valueStr, valueStr);
            }

            resource.addOrUpdateRuntimeProperty(runtimeProperty);
        }
    }


    public static void decryptCustomFormData(Map<String, String> environment, CustomForm customForm) {
        if(Utils.isNotEmpty(customForm.items())){
            for (CustomFormItem item : customForm.items()) {
                if(item.encrypted()){
                    String value = environment.get(item.key());

                    if(Utils.isNotBlank(value))
                        try {
                            environment.put(item.key(), SecretUtil.retrieveSecret(value));
                        }catch (Exception e){
                            log.warn(e.toString());
                        }
                }
            }
        }
    }
}
