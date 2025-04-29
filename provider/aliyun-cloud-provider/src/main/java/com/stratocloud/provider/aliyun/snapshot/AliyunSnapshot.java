package com.stratocloud.provider.aliyun.snapshot;

import com.aliyun.ecs20140526.models.DescribeSnapshotsResponseBody;

public record AliyunSnapshot(DescribeSnapshotsResponseBody.DescribeSnapshotsResponseBodySnapshotsSnapshot detail) {
}
