package com.stratocloud.limit;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.limit.cmd.*;
import com.stratocloud.limit.query.DescribeLimitsRequest;
import com.stratocloud.limit.query.DescribeUsageTypesRequest;
import com.stratocloud.limit.query.DescribeUsageTypesResponse;
import com.stratocloud.limit.query.NestedLimitResponse;
import com.stratocloud.limit.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ResourceUsageLimitApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resource-usage-limits")
    Page<NestedLimitResponse> describeLimits(@RequestBody DescribeLimitsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-usage-types")
    DescribeUsageTypesResponse describeUsageTypes(@RequestBody DescribeUsageTypesRequest request);


    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-resource-usage-limit")
    CreateLimitResponse createLimit(@RequestBody CreateLimitCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-resource-usage-limit")
    UpdateLimitResponse updateLimit(@RequestBody UpdateLimitCmd cmd);


    @PostMapping(StratoServices.RESOURCE_SERVICE+"/enable-resource-usage-limits")
    EnableLimitsResponse enableLimits(@RequestBody EnableLimitsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/disable-resource-usage-limits")
    DisableLimitsResponse disableLimits(@RequestBody DisableLimitsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/delete-resource-usage-limits")
    DeleteLimitsResponse deleteLimits(@RequestBody DeleteLimitsCmd cmd);


}
