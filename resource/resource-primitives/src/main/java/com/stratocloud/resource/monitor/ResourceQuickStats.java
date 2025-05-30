package com.stratocloud.resource.monitor;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.ArrayList;
import java.util.List;

public record ResourceQuickStats(List<Item> items) {

    public record Item(String name, String label, double value, String unit, boolean isPercentage){
        @JsonGetter
        public String getDescription(){
            return "%s:%s%s".formatted(name, getValueStr(), unit);
        }
        @JsonGetter
        public String getValueStr(){
            return "%.1f".formatted(value);
        }
    }

    public static Builder builder(){
        return new Builder();
    }


    public static class Builder {
        private final List<Item> items = new ArrayList<>();

        public Builder addItem(String name, String label, double value, String unit, boolean isPercentage){
            items.add(new Item(name, label, value,  unit, isPercentage));
            return this;
        }

        public Builder addPercentageItem(String name, String label, double percentage){
            return addItem(name, label, percentage, "%", true);
        }

        public Builder addItem(String name, String label, double value, String unit){
            return addItem(name, label, value, unit, false);
        }

        public Builder addCpuPercentage(double percentage){
            return addPercentageItem("cpu", "CPU使用率", percentage);
        }

        public Builder addMemoryPercentage(double percentage){
            return addPercentageItem("mem", "内存使用率", percentage);
        }

        public Builder addStoragePercentage(double percentage){
            return addPercentageItem("storage", "存储使用率", percentage);
        }

        public ResourceQuickStats build(){
            return new ResourceQuickStats(items);
        }
    }
}
