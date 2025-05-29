package com.stratocloud.resource;

import com.stratocloud.resource.cmd.*;
import com.stratocloud.resource.cmd.relationship.BatchChangeEssentialRequirementsCmd;
import com.stratocloud.resource.cmd.relationship.BatchConnectResourcesCmd;
import com.stratocloud.resource.cmd.relationship.BatchDisconnectResourcesCmd;
import com.stratocloud.resource.query.*;
import com.stratocloud.resource.query.inquiry.*;
import com.stratocloud.resource.query.metadata.*;
import com.stratocloud.resource.query.monitor.DescribeMetricsRequest;
import com.stratocloud.resource.query.monitor.DescribeMetricsResponse;
import com.stratocloud.resource.query.monitor.DescribeQuickStatsRequest;
import com.stratocloud.resource.query.monitor.DescribeQuickStatsResponse;
import com.stratocloud.resource.response.*;
import org.springframework.data.domain.Page;

public interface ResourceService {
    CreateResourcesResponse create(BatchCreateResourcesCmd cmd);

    RunActionsResponse runActions(BatchRunActionsCmd cmd);

    RecycleResourcesResponse recycle(BatchRecycleCmd cmd);

    RestoreResourcesResponse restore(BatchRestoreCmd cmd);

    Page<NestedResourceResponse> describeResources(DescribeResourcesRequest request);

    Page<NestedResourceResponse> describeUnclaimedResources(DescribeUnclaimedResourcesRequest request);

    SynchronizeResourcesResponse synchronizeResources(SynchronizeResourcesCmd cmd);

    DescribeAvailableActionsResponse describeResourceActions(DescribeAvailableActionsRequest request);

    Page<NestedRelationshipResponse> describeRequirements(DescribeRequirementsRequest request);

    Page<NestedRelationshipResponse> describeCapabilities(DescribeCapabilitiesRequest request);

    Page<NestedRelationshipResponse> describeRelationships(DescribeRelationshipsRequest request);


    ConnectResourcesResponse connectResources(BatchConnectResourcesCmd cmd);

    ChangeEssentialRequirementsResponse changeEssentialRequirements(BatchChangeEssentialRequirementsCmd cmd);

    DescribeResourceTypesResponse describeResourceTypes(DescribeResourceTypesRequest request);

    DescribeResourceCategoriesResponse describeResourceCategories(DescribeResourceCategoriesRequest request);

    DescribeProvidersResponse describeProviders(DescribeProvidersRequest request);

    DescribeResourceActionFormResponse describeResourceActionForm(DescribeResourceActionFormRequest request);

    DescribeRelationshipFormResponse describeRelationshipForm(DescribeRelationshipFormRequest request);

    DescribeRelationshipSpecResponse describeRelationshipSpec(DescribeRelationshipSpecRequest request);

    DisconnectResourcesResponse disconnectResources(BatchDisconnectResourcesCmd cmd);

    CreateResourcesPriceInquiryResponse performCreateResourcesPriceInquiry(CreateResourcesPriceInquiry inquiry);

    DestroyResourcesRefundInquiryResponse performDestroyResourcesRefundInquiry(DestroyResourcesRefundInquiry inquiry);

    RunActionsPriceInquiryResponse performRunActionsPriceInquiry(RunActionsPriceInquiry inquiry);

    ChangeEssentialRequirementsPriceInquiryResponse performChangeEssentialRequirementsPriceInquiry(
            ChangeEssentialRequirementsPriceInquiry inquiry
    );

    TransferResourcesResponse transfer(BatchTransferCmd cmd);

    RunReadActionsResponse runReadActions(RunReadActionsCmd cmd);

    DescribeRelationshipTypesResponse describeRelationshipTypes(DescribeRelationshipTypesRequest request);

    SynchronizeResourceStatesResponse synchronizeResourceStates(SynchronizeResourceStatesCmd cmd);

    DropResourcesResponse dropResources(BatchDropCmd cmd);

    DescribeQuickStatsResponse describeResourceQuickStats(DescribeQuickStatsRequest request);

    DescribeMetricsResponse describeResourceMetrics(DescribeMetricsRequest request);

    AssociateTagsResponse associateTags(AssociateTagsCmd cmd);

    DisassociateTagResponse disassociateTag(DisassociateTagCmd cmd);

    UpdateDescriptionResponse updateDescription(UpdateDescriptionCmd cmd);
}
