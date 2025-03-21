# 后端代码结构
* commons: 通用工具包
* distribution: 安装包的配置文件和脚本
* docs: 项目文档
* identity: 身份认证模块相关代码
* order: 工单模块相关代码
* provider: 云资源抽象层相关代码
* resource: 云资源模块相关代码
* starters: 启动入口模块

# 接入新的云平台
下面我们以阿里云为例，介绍接入阿里云的步骤  
1.在provider模块下新建子模块并添加所需依赖  
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--suppress ALL -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>provider</artifactId>
        <groupId>com.stratocloud</groupId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>aliyun-cloud-provider</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>20</maven.compiler.source>
        <maven.compiler.target>20</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.stratocloud</groupId>
            <artifactId>provider-common</artifactId>
            <version>1.0.0</version>
        </dependency>

        ...
        
    </dependencies>
</project>
```
2.新建账号属性实现类，实现`ExternalAccountProperties`接口，定义账号的属性字段  
```java
package com.stratocloud.provider.aliyun.common;

import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.ExternalAccountProperties;
import lombok.Data;

@Data
public class AliyunAccountProperties implements ExternalAccountProperties {
    @InputField(label = "AccessKeyId", description = "API密钥的AccessKeyId", inputType = "textarea")
    private String accessKeyId;
    @InputField(label = "AccessKeySecret", description = "API密钥的AccessKeySecret", inputType = "textarea")
    private String accessKeySecret;
    @SelectField(
            label = "地域",
            allowCreate = true,
            description = "若所需地域在选项中不存在，可直接输入其地域ID。",
            options = {
                    AliyunRegion.Ids.CN_BEIJING,
            },
            optionNames = {
                    AliyunRegion.Names.CN_BEIJING,
            }
    )
    private String region;
}

```
3.新建Provider实现类，继承`AbstractProvider`  
```java
package com.stratocloud.provider.aliyun;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.cache.CacheService;
import com.stratocloud.provider.AbstractProvider;
import com.stratocloud.provider.ExternalAccountProperties;
import com.stratocloud.provider.aliyun.common.AliyunAccountProperties;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunClientImpl;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.Map;

/** @noinspection ALL*/
@Component
public class AliyunCloudProvider extends AbstractProvider {

    private final CacheService cacheService;

    public AliyunCloudProvider(ExternalAccountRepository accountRepository,
                               CacheService cacheService) {
        super(accountRepository);
        this.cacheService = cacheService;
    }

    @Override
    public String getId() {
        return "ALIYUN_PROVIDER";
    }

    @Override
    public String getName() {
        return "阿里云";
    }

    @Override
    public Class<? extends ExternalAccountProperties> getExternalAccountPropertiesClass() {
        return AliyunAccountProperties.class;
    }

    @Override
    public void validateConnection(ExternalAccount externalAccount) {
        buildClient(externalAccount).validateConnection();
    }


    public AliyunClient buildClient(ExternalAccount externalAccount){
        var properties = JSON.convert(externalAccount.getProperties(), AliyunAccountProperties.class);
        return new AliyunClientImpl(properties, cacheService);
    }

    @Override
    public void eraseSensitiveInfo(Map<String, Object> properties) {
        properties.remove("secretKey");
    }


    @Override
    public Float getBalance(ExternalAccount account) {
        return buildClient(account).describeBalance();
    }
}
```
4.新建ResourceHandler实现类，继承`AbstractResourceHandler`
```java
package com.stratocloud.provider.aliyun.vpc;

import com.aliyun.vpc20160428.models.DescribeVpcsRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunVpcHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunVpcHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_VPC";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云私有网络";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.VPC;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public boolean isSharedRequirementTarget() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeVpc(account, externalId).map(
                vpc -> toExternalResource(account, vpc)
        );
    }

    public Optional<AliyunVpc> describeVpc(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeVpc(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunVpc vpc) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                vpc.getVpcId(),
                vpc.detail().getVpcName(),
                convertStatus(vpc.detail().getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status){
            case "Pending" -> ResourceState.BUILDING;
            case "Available" -> ResourceState.AVAILABLE;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        return client.vpc().describeVpcs(request).stream().map(vpc -> toExternalResource(account, vpc)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        //TODO
        return List.of();
    }

    @Override
    public void synchronize(Resource resource) {
        //TODO
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
```
5.新建ResourceActionHandler实现类，定义资源操作
```java
package com.stratocloud.provider.aliyun.vpc.actions;

import com.aliyun.vpc20160428.models.CreateVpcRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.vpc.AliyunVpcHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AliyunVpcBuildHandler implements BuildResourceActionHandler {

    private final AliyunVpcHandler vpcHandler;


    public AliyunVpcBuildHandler(AliyunVpcHandler vpcHandler) {
        this.vpcHandler = vpcHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return vpcHandler;
    }

    @Override
    public String getTaskName() {
        return "创建私有网络";
    }


    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunVpcBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        //TODO
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        //TODO
    }
}

```
6.新建第二个ResourceHandler实现类
```java
package com.stratocloud.provider.aliyun.subnet;

import com.aliyun.vpc20160428.models.DescribeVSwitchesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSubnetHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;


    public AliyunSubnetHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_SUBNET";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云子网";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SUBNET;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public boolean canAttachIpPool() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeSubnet(account, externalId).map(
                subnet -> toExternalResource(account, subnet)
        );
    }

    public Optional<AliyunSubnet> describeSubnet(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();
        return provider.buildClient(account).vpc().describeSubnet(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunSubnet subnet) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                subnet.detail().getVSwitchId(),
                subnet.detail().getVSwitchName(),
                convertStatus(subnet.detail().getStatus())
        );
    }

    private ResourceState convertStatus(String status) {
        return switch (status){
            case "Pending" -> ResourceState.BUILDING;
            case "Available" -> ResourceState.AVAILABLE;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeVSwitchesRequest request = new DescribeVSwitchesRequest();
        return client.vpc().describeSubnets(request).stream().map(
                subnet -> toExternalResource(account, subnet)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        //TODO
        return List.of();
    }

    @Override
    public void synchronize(Resource resource) {
        //TODO
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}

```
7.新建RelationshipHandler实现类，定义资源关系
```java
package com.stratocloud.provider.aliyun.subnet.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
import com.stratocloud.provider.aliyun.vpc.AliyunVpcHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSubnetToVpcHandler implements EssentialRequirementHandler {

    public static final String TYPE_ID = "ALIYUN_SUBNET_TO_VPC_RELATIONSHIP";
    private final AliyunSubnetHandler subnetHandler;

    private final AliyunVpcHandler vpcHandler;

    public AliyunSubnetToVpcHandler(AliyunSubnetHandler subnetHandler,
                                    AliyunVpcHandler vpcHandler) {
        this.subnetHandler = subnetHandler;
        this.vpcHandler = vpcHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "子网与私有网络";
    }

    @Override
    public ResourceHandler getSource() {
        return subnetHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return vpcHandler;
    }

    @Override
    public String getCapabilityName() {
        return "子网";
    }

    @Override
    public String getRequirementName() {
        return "私有网络";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<AliyunSubnet> subnet = subnetHandler.describeSubnet(account, source.externalId());

        if(subnet.isEmpty())
            return List.of();

        Optional<ExternalResource> vpc = vpcHandler.describeExternalResource(account, subnet.get().detail().getVpcId());

        return vpc.map(externalResource -> List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                externalResource,
                Map.of()
        ))).orElseGet(List::of);

    }
}
```
8.重复步骤4-步骤7，直到完成所有功能的接入  
9.在启动模块的`pom.xml`中引入刚刚新建的Provider模块

# 替换缓存实现层
StratoCloud社区版默认使用本地缓存，无法实现高可用部署。下面以Redis为例，介绍如何将缓存实现替换为Redis  
1.在commons模块下新建子模块，例如`redis-cache`  
2.新增Redis锁实现类，例如`RedisCacheLock`，实现`CacheLock`接口  
3.新增Redis缓存服务实现类，例如`RedisCacheService`，实现`CacheService`接口，为此类添加`@Component`和`@Primary`注解，并在`getLock`方法中返回`RedisCacheLock`对象  
4.在启动模块的`pom.xml`中引入刚刚新建的`redis-cache`模块

# 使用其他数据库
StratoCloud持久化层使用JPA-Hibernate框架，如果需要使用国产数据库，请参考以下文档
* [Hibernate对接达梦](https://eco.dameng.com/document/dm/zh-cn/app-dev/java-hibernate-frame){:target="_blank"}
* [Hibernate对接OceanBase](https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000002013264){:target="_blank"}
* [Hibernate对接TiDB](https://docs.pingcap.com/zh/tidb/stable/dev-guide-sample-application-java-hibernate/){:target="_blank"}
* [Hibernate对接GaussDB](https://support.huaweicloud.com/intl/zh-cn/qs-gaussdb/gaussdb_01_534.html){:target="_blank"}
* [Hibernate对接PolarDB](https://help.aliyun.com/zh/polardb/polardb-for-oracle/jdbc-o-2-0){:target="_blank"}

TDSQL MySQL版、TDSQL PostgreSQL版等数据库直接使用现有MySQL/PostgreSQL驱动即可，无需引入依赖

