package com.stratocloud.resource;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.resource.cmd.BatchDropCmd;
import com.stratocloud.resource.cmd.RunReadActionsCmd;
import com.stratocloud.resource.query.*;
import com.stratocloud.resource.query.inquiry.*;
import com.stratocloud.resource.query.metadata.*;
import com.stratocloud.resource.response.DropResourcesResponse;
import com.stratocloud.resource.response.RunReadActionsResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ResourceApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resources")
    Page<NestedResourceResponse> describeResources(@RequestBody DescribeResourcesRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-unclaimed-resources")
    Page<NestedResourceResponse> describeUnclaimedResources(@RequestBody DescribeUnclaimedResourcesRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-resources-price-inquiry")
    CreateResourcesPriceInquiryResponse performCreateResourcesPriceInquiry(@RequestBody CreateResourcesPriceInquiry inquiry);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/destroy-resources-refund-inquiry")
    DestroyResourcesRefundInquiryResponse performDestroyResourcesRefundInquiry(@RequestBody DestroyResourcesRefundInquiry inquiry);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/run-actions-price-inquiry")
    RunActionsPriceInquiryResponse performRunActionsPriceInquiry(@RequestBody RunActionsPriceInquiry inquiry);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/change-essential-requirements-price-inquiry")
    ChangeEssentialRequirementsPriceInquiryResponse performChangeEssentialRequirementsPriceInquiry(
            @RequestBody ChangeEssentialRequirementsPriceInquiry inquiry
    );

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-requirements")
    Page<NestedRelationshipResponse> describeRequirements(@RequestBody DescribeRequirementsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-capabilities")
    Page<NestedRelationshipResponse> describeCapabilities(@RequestBody DescribeCapabilitiesRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-relationships")
    Page<NestedRelationshipResponse> describeRelationships(@RequestBody DescribeRelationshipsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-available-resource-actions")
    DescribeAvailableActionsResponse describeAvailableActions(@RequestBody DescribeAvailableActionsRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resource-types")
    DescribeResourceTypesResponse describeResourceTypes(@RequestBody DescribeResourceTypesRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resource-categories")
    DescribeResourceCategoriesResponse describeResourceCategories(@RequestBody DescribeResourceCategoriesRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-providers")
    DescribeProvidersResponse describeProviders(@RequestBody DescribeProvidersRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resource-action-form")
    DescribeResourceActionFormResponse describeResourceActionForm(@RequestBody DescribeResourceActionFormRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-relationship-form")
    DescribeRelationshipFormResponse describeRelationshipForm(@RequestBody DescribeRelationshipFormRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-relationship-spec")
    DescribeRelationshipSpecResponse describeRelationshipSpec(@RequestBody DescribeRelationshipSpecRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-relationship-types")
    DescribeRelationshipTypesResponse describeRelationshipTypes(@RequestBody DescribeRelationshipTypesRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/run-resource-read-actions")
    RunReadActionsResponse runReadActions(@RequestBody RunReadActionsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/drop-resources")
    DropResourcesResponse dropResources(@RequestBody BatchDropCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-resource-quick-stats")
    DescribeQuickStatsResponse describeResourceQuickStats(@RequestBody DescribeQuickStatsRequest request);
}
