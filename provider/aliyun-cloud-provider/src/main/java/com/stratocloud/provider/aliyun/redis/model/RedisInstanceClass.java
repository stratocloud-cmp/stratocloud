package com.stratocloud.provider.aliyun.redis.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.*;

@Data
public class RedisInstanceClass {
    private String classCode;
    private String remark;
    private Long capacityMb;

    private String productType;
    private String instanceScene;

    private List<Group> supportedGroups = new ArrayList<>();

    private Set<String> supportedZones = new HashSet<>();

    @JsonIgnore
    public void addGroup(Group group){
        if(supportedGroups == null)
            supportedGroups = new ArrayList<>();

        if(supportedGroups.contains(group))
            return;

        supportedGroups.add(group);
    }

    @JsonIgnore
    public void addZone(String zone){
        if(supportedZones == null)
            supportedZones = new HashSet<>();

        supportedZones.add(zone);
    }

    @JsonIgnore
    public boolean supportFamily(RedisInstanceFamily family) {
        if(family.getProductType() != null && !Objects.equals(family.getProductType(), productType))
            return false;

        if(family.getInstanceScene() != null && !Objects.equals(family.getInstanceScene(), instanceScene))
            return false;

        return supportedGroups.stream().filter(
                g -> family.getEditionType() == null || Objects.equals(family.getEditionType(), g.getEditionType())
        ).filter(
                g -> family.getArchitecture() == null || Objects.equals(family.getArchitecture(), g.getArchitecture())
        ).anyMatch(
                g -> family.getNodeType() == null || Objects.equals(family.getNodeType(), g.getNodeType())
        );
    }

    public boolean supportShardNumber(Long classicShardNumber) {
        return supportedGroups.stream().anyMatch(
                g -> Objects.equals(g.getShardNumber(), classicShardNumber.toString())
        );
    }

    @Data
    public static class Group {
        private String engine;
        private String editionType;
        private String seriesType;
        private String engineVersion;
        private String architecture;
        private String shardNumber;
        private String nodeType;
    }
}
