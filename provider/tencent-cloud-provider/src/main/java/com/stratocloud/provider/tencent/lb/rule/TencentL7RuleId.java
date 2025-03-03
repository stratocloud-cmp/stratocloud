package com.stratocloud.provider.tencent.lb.rule;

public record TencentL7RuleId(String lbId, String listenerId, String locationId) {
    public static TencentL7RuleId fromString(String externalId) {

        String[] arr = externalId.split("@");
        return new TencentL7RuleId(arr[2], arr[1], arr[0]);
    }

    @Override
    public String toString() {
        return "%s@%s@%s".formatted(locationId, listenerId, lbId);
    }
}
