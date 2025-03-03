package com.stratocloud.provider.script.software;

public record SoftwareId(String managementIp, int port) {
    @Override
    public String toString() {
        return "%s@%s".formatted(port, managementIp);
    }

    public static SoftwareId fromString(String s){
        String[] arr = s.split("@");
        return new SoftwareId(arr[1], Integer.parseInt(arr[0]));
    }
}
