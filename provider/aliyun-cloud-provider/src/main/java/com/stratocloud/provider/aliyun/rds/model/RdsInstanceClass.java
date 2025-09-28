package com.stratocloud.provider.aliyun.rds.model;

import com.aliyun.rds20140815.models.ListClassesResponseBody;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.provider.constants.DbEngine;
import com.stratocloud.utils.Utils;
import lombok.Getter;

public record RdsInstanceClass(DbEngine engine, ListClassesResponseBody.ListClassesResponseBodyItems detail) {
    @Getter
    public enum Category {
        Basic("基础系列"),
        HighAvailability("高可用系列"),
        cluster("集群系列"),
        AlwaysOn("SQL Server集群系列"),
        Finance("三节点企业系列"),
        serverless_basic("Serverless 基础系列"),
        serverless_standard("Serverless 高可用系列"),
        serverless_ha("SQL Server Serverless 高可用系列"),
        Empty(""),
        Unknown("未知系列");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public static Category fromString(String s){
            if(Utils.isBlank(s))
                return Empty;
            for (Category category : values()) {
                if(category.name().equalsIgnoreCase(s))
                    return category;
            }
            return Category.Unknown;
        }
    }

    @Getter
    public enum Group {
        General("通用型", "通用型"),
        Exclusive("独享套餐", "独享型"),
        ExclusiveHost("独占物理机", "独占物理机"),
        Economical("经济型", "经济型"),
        Unknown("Unknown", "未知规格族");

        private final String id;
        private final String displayName;

        Group(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public static Group fromString(String s){
            for (Group group : values()) {
                if(group.getId().equalsIgnoreCase(s))
                    return group;
            }
            return Group.Unknown;
        }
    }

    @JsonIgnore
    public Category getCategory(){
        return Category.fromString(detail.getCategory());
    }

    @JsonIgnore
    public Group getGroup(){
        return Group.fromString(detail.getClassGroup());
    }

    @JsonIgnore
    public String getName(){
        return "%s %s %sC%s 最大连接数:%s 芯片架构:%s [%s]".formatted(
                engine.name(),
                getCategory().getDisplayName(),
                detail.getCpu().trim(),
                detail.getMemoryClass(),
                detail.getMaxConnections() == null ? "无限制" : detail.getMaxConnections(),
                Utils.isBlank(detail.getInstructionSetArch()) ? "X86" : detail.getInstructionSetArch(),
                detail.getClassCode()
        );
    }

    @JsonIgnore
    public int getMaxConnections() {
        if(detail.getMaxConnections() == null)
            return -1;
        try {
            return Integer.parseInt(detail.getMaxConnections().trim());
        }catch (Exception e){
            return -1;
        }
    }

    @JsonIgnore
    public int getCpuCores(){
        return getPrefixNumber(detail.getCpu());
    }

    @JsonIgnore
    public int getMemoryGb(){
        return getPrefixNumber(detail.getMemoryClass());
    }

    private static int getPrefixNumber(String s){
        if(s == null)
            return 0;

        try {
            char[] charArray = s.trim().toCharArray();
            StringBuilder stringBuilder = new StringBuilder();
            for (char c : charArray) {
                if(Character.isDigit(c))
                    stringBuilder.append(c);
                else
                    break;
            }
            if(stringBuilder.isEmpty())
                return 0;

            return Integer.parseInt(stringBuilder.toString());
        }catch (Exception e){
            return 0;
        }
    }
}
