package com.stratocloud.provider.aliyun.disk;

import com.aliyun.ecs20140526.models.DescribeDisksResponseBody;

import java.util.Objects;

public record AliyunDisk(
        DescribeDisksResponseBody.DescribeDisksResponseBodyDisksDisk detail
) {

    public boolean isSystemDisk(){
        return Objects.equals("system", detail().getType());
    }

}
