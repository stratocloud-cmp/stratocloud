package com.stratocloud.provider.huawei.elb.member;

import com.stratocloud.exceptions.StratoException;

public record MemberId(String poolId, String memberId) {

    public static MemberId fromString(String s){
        try {
            String[] arr = s.split("@");
            return new MemberId(arr[1], arr[0]);
        }catch (Exception e){
            throw new StratoException("Failed to parse memberId: "+s, e);
        }
    }

    @Override
    public String toString() {
        return "%s@%s".formatted(memberId, poolId);
    }
}
