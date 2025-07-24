package com.stratocloud.provider.tencent.cos.acl;

import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.Grant;
import com.qcloud.cos.model.Permission;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class TencentBucketAclHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentBucketAclHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_COS_BUCKET_ACL";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云存储桶ACL";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.BUCKET_ACL;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeBucketAcl(account, externalId).map(
                acl -> toExternalResource(account, acl)
        );
    }

    public Optional<TencentBucketAcl> describeBucketAcl(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());

        TencentBucketAclId aclId = TencentBucketAclId.fromString(externalId);

        Optional<AccessControlList> acl = cosSession.describeBucketAcl(aclId.bucketName());

        if(acl.isEmpty())
            return Optional.empty();

        List<Grant> grants = acl.get().getGrantsAsList();

        if(Utils.isEmpty(grants))
            return Optional.empty();

        Set<Permission> permissions = grants.stream().filter(
                g -> aclId.equals(TencentBucketAclId.fromGrantee(aclId.bucketName(), g.getGrantee()))
        ).map(Grant::getPermission).collect(Collectors.toSet());
        if(permissions.isEmpty())
            return Optional.empty();

        return Optional.of(
                new TencentBucketAcl(
                        aclId,
                        permissions
                )
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, TencentBucketAcl acl){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                acl.id().toString(),
                acl.id().toString(),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());

        List<Bucket> buckets = cosSession.describeBuckets();

        List<ExternalResource> result = new ArrayList<>();
        for (Bucket bucket : buckets) {
            Optional<AccessControlList> acl = cosSession.describeBucketAcl(bucket.getName());

            if(acl.isEmpty())
                continue;

            List<Grant> grants = acl.get().getGrantsAsList();

            if(Utils.isEmpty(grants))
                continue;

            Map<TencentBucketAclId, Set<Permission>> aclMap = new HashMap<>();

            for (Grant grant : grants) {
                TencentBucketAclId aclId = TencentBucketAclId.fromGrantee(bucket.getName(), grant.getGrantee());
                aclMap.computeIfAbsent(
                        aclId, k -> new HashSet<>()
                ).add(grant.getPermission());
            }

            for (TencentBucketAclId aclId : aclMap.keySet()) {
                Set<Permission> permissions = aclMap.get(aclId);

                if(Utils.isEmpty(permissions))
                    continue;

                result.add(
                        toExternalResource(account, new TencentBucketAcl(aclId, permissions))
                );
            }
        }

        return result;
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var acl = describeBucketAcl(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Bucket acl not found")
        );
        resource.updateByExternal(toExternalResource(account, acl));

        RuntimeProperty granteeProperty = RuntimeProperty.ofDisplayInList(
                "grantee",
                "授权账号",
                acl.id().granteeIdentifier(),
                acl.id().granteeIdentifier()
        );
        resource.addOrUpdateRuntimeProperty(granteeProperty);

        Set<Permission> permissions = acl.permissions();
        if(Utils.isEmpty(permissions))
            return;

        List<String> permissionStrList = new ArrayList<>();
        List<String> permissionNameList = new ArrayList<>();

        if(permissions.contains(Permission.FullControl)){
            permissionStrList.add(Permission.FullControl.toString());
            permissionNameList.add("完全控制");
        } else {
            if(permissions.contains(Permission.Read)){
                permissionStrList.add(Permission.Read.toString());
                permissionNameList.add("数据读取");
            }
            if(permissions.contains(Permission.Write)){
                permissionStrList.add(Permission.Write.toString());
                permissionNameList.add("数据写入");
            }
            if(permissions.contains(Permission.ReadAcp)){
                permissionStrList.add(Permission.ReadAcp.toString());
                permissionNameList.add("权限读取");
            }
            if(permissions.contains(Permission.WriteAcp)){
                permissionStrList.add(Permission.WriteAcp.toString());
                permissionNameList.add("权限写入");
            }
        }

        RuntimeProperty permissionsProperty = RuntimeProperty.ofDisplayInList(
                "permissions",
                "权限",
                String.join(",", permissionStrList),
                String.join(",", permissionNameList)
        );
        resource.addOrUpdateRuntimeProperty(permissionsProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
