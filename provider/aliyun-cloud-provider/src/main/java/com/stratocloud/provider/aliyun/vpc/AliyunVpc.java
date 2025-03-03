package com.stratocloud.provider.aliyun.vpc;


import com.aliyun.vpc20160428.models.DescribeVpcsResponseBody;

public record AliyunVpc(DescribeVpcsResponseBody.DescribeVpcsResponseBodyVpcsVpc detail) {
    public String getVpcId(){
        return detail.getVpcId();
    }
}
