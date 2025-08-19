package com.stratocloud.provider.tencent.database.cdb;

public enum CdbInstanceRole {
    master,
    dr,
    ro;


    public static CdbInstanceRole fromLong(Long instanceType){
        if(instanceType == null)
            return master;

        return switch (instanceType.intValue()){
            case 2 -> dr;
            case 3 -> ro;
            default -> master;
        };
    }
}
