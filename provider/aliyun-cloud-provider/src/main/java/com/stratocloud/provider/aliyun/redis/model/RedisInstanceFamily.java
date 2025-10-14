package com.stratocloud.provider.aliyun.redis.model;

import com.stratocloud.provider.aliyun.redis.actions.AliyunRedisBuildInput;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Getter
public enum RedisInstanceFamily {
    ENT_NATIVE_MEM_STD_SA(
            "Tair_rdb", "professional",
            "Enterprise", "non_cluster",
            null, "Tair企业版-云原生版-内存型-标准架构-单副本"
    ),
    ENT_NATIVE_MEM_STD_HA(
            "Tair_rdb", "professional",
            "Enterprise", "non_cluster",
            null, "Tair企业版-云原生版-内存型-标准架构-高可用"
    ),
    ENT_NATIVE_MEM_CLUSTER_HA(
            "Tair_rdb", "professional",
            "Enterprise", "cluster",
            null, "Tair企业版-云原生版-内存型-集群架构-高可用"
    ),

    ENT_NATIVE_SCM_STD_SA(
            "Tair_scm", "professional",
            "Enterprise", "non_cluster",
            null, "Tair企业版-云原生版-持久内存型-标准架构-单副本"
    ),
    ENT_NATIVE_SCM_STD_HA(
            "Tair_scm", "professional",
            "Enterprise", "non_cluster",
            null, "Tair企业版-云原生版-持久内存型-标准架构-高可用"
    ),
    ENT_NATIVE_SCM_CLUSTER_HA(
            "Tair_scm", "professional",
            "Enterprise", "cluster",
            null, "Tair企业版-云原生版-持久内存型-集群架构-高可用"
    ),


    ENT_NATIVE_ESSD_STD_HA(
            "Tair_essd", "professional",
            "Enterprise", "non_cluster",
            null, "Tair企业版-云原生版-磁盘型-标准架构-高可用"
    ),
    ENT_NATIVE_ESSD_CLUSTER_HA(
            "Tair_essd", "professional",
            "Enterprise", "cluster",
            null, "Tair企业版-云原生版-磁盘型-集群架构-高可用"
    ),

    ENT_CLASSIC_MEM_STD_SA(
            "Local", "professional",
            "Enterprise", "standard",
            "single", "Tair企业版-经典版-内存型-标准架构-单副本"
    ),
    ENT_CLASSIC_MEM_STD_HA(
            "Local", "professional",
            "Enterprise", "standard",
            "double", "Tair企业版-经典版-内存型-标准架构-双副本"
    ),
    ENT_CLASSIC_MEM_CLUSTER_HA(
            "Local", "professional",
            "Enterprise", "cluster",
            "double", "Tair企业版-经典版-内存型-集群架构-双副本"
    ),

    CE_NATIVE_MEM_STD_SA(
            "OnECS", "professional",
            "Community", "non_cluster",
            null, "Redis开源版-云原生版-内存型-标准架构-单副本"
    ),
    CE_NATIVE_MEM_STD_HA(
            "OnECS", "professional",
            "Community", "non_cluster",
            null, "Redis开源版-云原生版-内存型-标准架构-高可用"
    ),
    CE_NATIVE_MEM_CLUSTER_HA(
            "OnECS", "professional",
            "Community", "cluster",
            null, "Redis开源版-云原生版-内存型-集群架构-高可用"
    ),

    CE_CLASSIC_MEM_STD_SA(
            "Local", "professional",
            "Community", "standard",
            "single", "Redis开源版-经典版-内存型-标准架构-单副本"
    ),
    CE_CLASSIC_MEM_STD_HA(
            "Local", "professional",
            "Community", "standard",
            "double", "Redis开源版-经典版-内存型-标准架构-双副本"
    ),
    CE_CLASSIC_MEM_CLUSTER_SA(
            "Local", "professional",
            "Community", "cluster",
            "single", "Redis开源版-经典版-内存型-集群架构-单副本"
    ),
    CE_CLASSIC_MEM_CLUSTER_HA(
            "Local", "professional",
            "Community", "cluster",
            "double", "Redis开源版-经典版-内存型-集群架构-双副本"
    ),

    ECONOMICAL(
            "OnECS", "economical",
            "Community", "non_cluster",
            null, "倚天版"
    );

    private final String productType;

    private final String instanceScene;

    private final String editionType;

    private final String architecture;

    private final String nodeType;

    private final String label;


    RedisInstanceFamily(String productType,
                        String instanceScene,
                        String editionType,
                        String architecture,
                        String nodeType,
                        String label) {
        this.productType = productType;
        this.instanceScene = instanceScene;
        this.editionType = editionType;
        this.architecture = architecture;
        this.nodeType = nodeType;
        this.label = label;
    }


    public static Optional<RedisInstanceFamily> from(AliyunRedisBuildInput input){
        for (RedisInstanceFamily family : values()) {
            if(!Objects.equals(input.getEdition(), family.getEditionType()))
                continue;

            if(Objects.equals(input.getEdition(), "Enterprise")){
                if(!Objects.equals(input.getEnterpriseProductType(), family.getProductType()))
                    continue;

                if(Objects.equals(input.getArchitecture(), "cluster")){
                    if(!Objects.equals(family.getArchitecture(), "cluster"))
                        continue;
                } else {
                    if(Objects.equals(family.getArchitecture(), "cluster"))
                        continue;
                }

                if(Objects.equals(input.getNodeType(), "ha")){
                    if(family.getNodeType()==null || Set.of("double", "MASTER_SLAVE").contains(family.getNodeType()))
                        return Optional.of(family);
                } else {
                    if(family.getNodeType()==null || Set.of("single", "STAND_ALONE").contains(family.getNodeType()))
                        return Optional.of(family);
                }
            }else if(Objects.equals(input.getEdition(), "Community")){
                if(!Objects.equals(input.getCommunityProductType(), family.getProductType()))
                    continue;

                if(Objects.equals(input.getCommunityProductType(), "OnECS")){
                    if(!Objects.equals(input.getScene(), family.getInstanceScene()))
                        continue;
                }

                if(Objects.equals(input.getArchitecture(), "cluster")){
                    if(!Objects.equals(family.getArchitecture(), "cluster"))
                        continue;
                } else {
                    if(Objects.equals(family.getArchitecture(), "cluster"))
                        continue;
                }

                if(Objects.equals(input.getNodeType(), "ha")){
                    if(family.getNodeType()==null || Set.of("double", "MASTER_SLAVE").contains(family.getNodeType()))
                        return Optional.of(family);
                } else {
                    if(family.getNodeType()==null || Set.of("single", "STAND_ALONE").contains(family.getNodeType()))
                        return Optional.of(family);
                }
            }
        }

        return Optional.empty();
    }
}
