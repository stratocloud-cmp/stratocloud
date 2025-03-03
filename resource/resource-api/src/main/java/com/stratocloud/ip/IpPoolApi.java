package com.stratocloud.ip;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.ip.cmd.*;
import com.stratocloud.ip.query.DescribeIpPoolRequest;
import com.stratocloud.ip.query.DescribeIpsRequest;
import com.stratocloud.ip.query.NestedIpPoolResponse;
import com.stratocloud.ip.query.NestedIpResponse;
import com.stratocloud.ip.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface IpPoolApi {
    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/create-ip-pool")
    CreateIpPoolResponse createIpPool(@RequestBody CreateIpPoolCmd cmd);
    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/update-ip-pool")
    UpdateIpPoolResponse updateIpPool(@RequestBody UpdateIpPoolCmd cmd);
    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/delete-ip-pools")
    DeleteIpPoolsResponse deleteIpPools(@RequestBody DeleteIpPoolsCmd cmd);

    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/add-ip-range")
    AddIpRangeResponse addIpRange(@RequestBody AddIpRangeCmd cmd);

    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/remove-ip-ranges")
    RemoveIpRangeResponse removeIpRanges(@RequestBody RemoveIpRangesCmd cmd);

    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/exclude-ips")
    ExcludeIpsResponse excludeIps(@RequestBody ExcludeIpsCmd cmd);
    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/release-ips")
    ReleaseIpsResponse releaseIps(@RequestBody ReleaseIpsCmd cmd);


    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/update-ip-pool-attached-networks")
    UpdateAttachedNetworksResponse updateAttachedNetworks(@RequestBody UpdateAttachedNetworksCmd cmd);


    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/describe-ip-pools")
    Page<NestedIpPoolResponse> describeIpPools(@RequestBody DescribeIpPoolRequest request);


    @PostMapping(path = StratoServices.RESOURCE_SERVICE+"/describe-ips")
    Page<NestedIpResponse> describeIps(@RequestBody DescribeIpsRequest request);
}
