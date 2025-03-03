package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.ip.IpPoolApi;
import com.stratocloud.ip.IpPoolService;
import com.stratocloud.ip.cmd.*;
import com.stratocloud.ip.query.DescribeIpPoolRequest;
import com.stratocloud.ip.query.DescribeIpsRequest;
import com.stratocloud.ip.query.NestedIpPoolResponse;
import com.stratocloud.ip.query.NestedIpResponse;
import com.stratocloud.ip.response.*;
import com.stratocloud.permission.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "IpPool", targetName = "IP池")
@RestController
public class IpPoolController implements IpPoolApi {

    private final IpPoolService service;

    public IpPoolController(IpPoolService service) {
        this.service = service;
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateIpPool",
            actionName = "创建IP池",
            objectType = "IpPool",
            objectTypeName = "IP池"
    )
    public CreateIpPoolResponse createIpPool(@RequestBody CreateIpPoolCmd cmd) {
        return service.createIpPool(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateIpPool",
            actionName = "更新IP池",
            objectType = "IpPool",
            objectTypeName = "IP池"
    )
    public UpdateIpPoolResponse updateIpPool(@RequestBody UpdateIpPoolCmd cmd) {
        return service.updateIpPool(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteIpPools",
            actionName = "删除IP池",
            objectType = "IpPool",
            objectTypeName = "IP池"
    )
    public DeleteIpPoolsResponse deleteIpPools(@RequestBody DeleteIpPoolsCmd cmd) {
        return service.deleteIpPools(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "AddIpRange",
            actionName = "添加IP范围",
            objectType = "IpPool",
            objectTypeName = "IP池"
    )
    public AddIpRangeResponse addIpRange(@RequestBody AddIpRangeCmd cmd) {
        return service.addIpRange(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "RemoveIpRanges",
            actionName = "移除IP范围",
            objectType = "IpPool",
            objectTypeName = "IP池"
    )
    public RemoveIpRangeResponse removeIpRanges(@RequestBody RemoveIpRangesCmd cmd) {
        return service.removeIpRanges(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "ExcludeIps",
            actionName = "排除IP",
            objectType = "ManagedIp",
            objectTypeName = "IP地址"
    )
    public ExcludeIpsResponse excludeIps(@RequestBody ExcludeIpsCmd cmd) {
        return service.excludeIps(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "ReleaseIps",
            actionName = "释放IP",
            objectType = "ManagedIp",
            objectTypeName = "IP地址"
    )
    public ReleaseIpsResponse releaseIps(@RequestBody ReleaseIpsCmd cmd) {
        return service.releaseIps(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateAttachedNetworks",
            actionName = "更新关联网络",
            objectType = "IpPool",
            objectTypeName = "IP池"
    )
    public UpdateAttachedNetworksResponse updateAttachedNetworks(@RequestBody UpdateAttachedNetworksCmd cmd) {
        return service.updateAttachedNetworks(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedIpPoolResponse> describeIpPools(@RequestBody DescribeIpPoolRequest request) {
        return service.describeIpPools(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedIpResponse> describeIps(@RequestBody DescribeIpsRequest request) {
        return service.describeIps(request);
    }
}
