package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.iam.v3.model.ProjectResult;

import java.util.Optional;

public interface HuaweiIamService {
    Optional<ProjectResult> getProject();
}
