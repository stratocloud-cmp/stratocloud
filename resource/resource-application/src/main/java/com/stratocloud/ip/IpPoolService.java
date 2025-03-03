package com.stratocloud.ip;

import com.stratocloud.ip.cmd.*;
import com.stratocloud.ip.query.DescribeIpPoolRequest;
import com.stratocloud.ip.query.DescribeIpsRequest;
import com.stratocloud.ip.query.NestedIpPoolResponse;
import com.stratocloud.ip.query.NestedIpResponse;
import com.stratocloud.ip.response.*;
import org.springframework.data.domain.Page;

public interface IpPoolService {
    CreateIpPoolResponse createIpPool(CreateIpPoolCmd cmd);

    UpdateIpPoolResponse updateIpPool(UpdateIpPoolCmd cmd);

    DeleteIpPoolsResponse deleteIpPools(DeleteIpPoolsCmd cmd);

    AddIpRangeResponse addIpRange(AddIpRangeCmd cmd);

    RemoveIpRangeResponse removeIpRanges(RemoveIpRangesCmd cmd);

    ExcludeIpsResponse excludeIps(ExcludeIpsCmd cmd);

    ReleaseIpsResponse releaseIps(ReleaseIpsCmd cmd);

    Page<NestedIpPoolResponse> describeIpPools(DescribeIpPoolRequest request);

    UpdateAttachedNetworksResponse updateAttachedNetworks(UpdateAttachedNetworksCmd cmd);

    Page<NestedIpResponse> describeIps(DescribeIpsRequest request);


}
