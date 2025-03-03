package com.stratocloud.provider.huawei.elb.member;

import com.huaweicloud.sdk.elb.v3.model.Member;

public record HuaweiMember(MemberId memberId, Member detail) {
}
