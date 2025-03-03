package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.limit.ResourceUsageLimitApi;
import com.stratocloud.limit.ResourceUsageLimitService;
import com.stratocloud.limit.cmd.*;
import com.stratocloud.limit.query.DescribeLimitsRequest;
import com.stratocloud.limit.query.DescribeUsageTypesRequest;
import com.stratocloud.limit.query.DescribeUsageTypesResponse;
import com.stratocloud.limit.query.NestedLimitResponse;
import com.stratocloud.limit.response.*;
import com.stratocloud.permission.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "ResourceUsageLimit", targetName = "配额上限")
@RestController
public class ResourceUsageLimitController implements ResourceUsageLimitApi {

    private final ResourceUsageLimitService service;

    public ResourceUsageLimitController(ResourceUsageLimitService service) {
        this.service = service;
    }


    @Override
    @ReadPermissionRequired
    public Page<NestedLimitResponse> describeLimits(@RequestBody DescribeLimitsRequest request) {
        return service.describeLimits(request);
    }

    @Override
    public DescribeUsageTypesResponse describeUsageTypes(@RequestBody DescribeUsageTypesRequest request) {
        return service.describeUsageTypes(request);
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateLimit",
            actionName = "创建配额上限",
            objectType = "ResourceUsageLimit",
            objectTypeName = "配额上限"
    )
    public CreateLimitResponse createLimit(@RequestBody CreateLimitCmd cmd) {
        return service.createLimit(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateLimit",
            actionName = "修改配额上限",
            objectType = "ResourceUsageLimit",
            objectTypeName = "配额上限"
    )
    public UpdateLimitResponse updateLimit(@RequestBody UpdateLimitCmd cmd) {
        return service.updateLimit(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "EnableLimits",
            actionName = "启用配额上限",
            objectType = "ResourceUsageLimit",
            objectTypeName = "配额上限"
    )
    public EnableLimitsResponse enableLimits(@RequestBody EnableLimitsCmd cmd) {
        return service.enableLimits(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "DisableLimits",
            actionName = "停用配额上限",
            objectType = "ResourceUsageLimit",
            objectTypeName = "配额上限"
    )
    public DisableLimitsResponse disableLimits(@RequestBody DisableLimitsCmd cmd) {
        return service.disableLimits(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteLimits",
            actionName = "删除配额上限",
            objectType = "ResourceUsageLimit",
            objectTypeName = "配额上限"
    )
    public DeleteLimitsResponse deleteLimits(@RequestBody DeleteLimitsCmd cmd) {
        return service.deleteLimits(cmd);
    }
}
