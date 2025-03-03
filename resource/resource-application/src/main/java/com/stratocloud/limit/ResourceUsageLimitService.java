package com.stratocloud.limit;

import com.stratocloud.limit.cmd.*;
import com.stratocloud.limit.query.DescribeLimitsRequest;
import com.stratocloud.limit.query.DescribeUsageTypesRequest;
import com.stratocloud.limit.query.DescribeUsageTypesResponse;
import com.stratocloud.limit.query.NestedLimitResponse;
import com.stratocloud.limit.response.*;
import com.stratocloud.resource.Resource;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ResourceUsageLimitService {
    void checkLimits(List<Resource> resourcesToAllocate);

    Page<NestedLimitResponse> describeLimits(DescribeLimitsRequest request);


    CreateLimitResponse createLimit(CreateLimitCmd cmd);

    UpdateLimitResponse updateLimit(UpdateLimitCmd cmd);

    EnableLimitsResponse enableLimits(EnableLimitsCmd cmd);

    DisableLimitsResponse disableLimits(DisableLimitsCmd cmd);

    DeleteLimitsResponse deleteLimits(DeleteLimitsCmd cmd);

    DescribeUsageTypesResponse describeUsageTypes(DescribeUsageTypesRequest request);

    void synchronizeAllLimits();
}
