package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.permission.PermissionRequired;
import com.stratocloud.permission.PermissionTarget;
import com.stratocloud.resource.ResourceApi;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.*;
import com.stratocloud.resource.query.*;
import com.stratocloud.resource.query.inquiry.*;
import com.stratocloud.resource.query.metadata.*;
import com.stratocloud.resource.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@PermissionTarget(target = ResourcePermissionTarget.ID, targetName = ResourcePermissionTarget.NAME)
@RestController
public class ResourceController implements ResourceApi {

    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @Override
    public Page<NestedResourceResponse> describeResources(@RequestBody DescribeResourcesRequest request) {
        return service.describeResources(request);
    }

    @Override
    public Page<NestedResourceResponse> describeUnclaimedResources(
            @RequestBody DescribeUnclaimedResourcesRequest request
    ) {
        return service.describeUnclaimedResources(request);
    }

    @Override
    public CreateResourcesPriceInquiryResponse performCreateResourcesPriceInquiry(@RequestBody CreateResourcesPriceInquiry inquiry) {
        return service.performCreateResourcesPriceInquiry(inquiry);
    }

    @Override
    public DestroyResourcesRefundInquiryResponse performDestroyResourcesRefundInquiry(@RequestBody DestroyResourcesRefundInquiry inquiry) {
        return service.performDestroyResourcesRefundInquiry(inquiry);
    }

    @Override
    public RunActionsPriceInquiryResponse performRunActionsPriceInquiry(@RequestBody RunActionsPriceInquiry inquiry) {
        return service.performRunActionsPriceInquiry(inquiry);
    }


    @Override
    public ChangeEssentialRequirementsPriceInquiryResponse performChangeEssentialRequirementsPriceInquiry(
            @RequestBody ChangeEssentialRequirementsPriceInquiry inquiry
    ) {
        return service.performChangeEssentialRequirementsPriceInquiry(inquiry);
    }

    @Override
    public Page<NestedRelationshipResponse> describeRequirements(@RequestBody DescribeRequirementsRequest request) {
        return service.describeRequirements(request);
    }

    @Override
    public Page<NestedRelationshipResponse> describeCapabilities(@RequestBody DescribeCapabilitiesRequest request) {
        return service.describeCapabilities(request);
    }

    @Override
    public Page<NestedRelationshipResponse> describeRelationships(@RequestBody DescribeRelationshipsRequest request) {
        return service.describeRelationships(request);
    }

    @Override
    public DescribeAvailableActionsResponse describeAvailableActions(@RequestBody DescribeAvailableActionsRequest request) {
        return service.describeResourceActions(request);
    }


    @Override
    public DescribeResourceTypesResponse describeResourceTypes(@RequestBody DescribeResourceTypesRequest request) {
        return service.describeResourceTypes(request);
    }

    @Override
    public DescribeResourceCategoriesResponse describeResourceCategories(@RequestBody DescribeResourceCategoriesRequest request){
        return service.describeResourceCategories(request);
    }

    @Override
    public DescribeProvidersResponse describeProviders(@RequestBody DescribeProvidersRequest request) {
        return service.describeProviders(request);
    }

    @Override
    public DescribeResourceActionFormResponse describeResourceActionForm(@RequestBody DescribeResourceActionFormRequest request) {
        return service.describeResourceActionForm(request);
    }

    @Override
    public DescribeRelationshipFormResponse describeRelationshipForm(@RequestBody DescribeRelationshipFormRequest request) {
        return service.describeRelationshipForm(request);
    }

    @Override
    public DescribeRelationshipSpecResponse describeRelationshipSpec(@RequestBody DescribeRelationshipSpecRequest request) {
        return service.describeRelationshipSpec(request);
    }

    @Override
    public DescribeRelationshipTypesResponse describeRelationshipTypes(@RequestBody DescribeRelationshipTypesRequest request) {
        return service.describeRelationshipTypes(request);
    }

    @Override
    @SendAuditLog(
            action = "RunReadAction",
            actionName = "执行读取动作",
            objectType = "Resource",
            objectTypeName = "云资源",
            hideResponseBody = true
    )
    public RunReadActionsResponse runReadActions(@RequestBody RunReadActionsCmd cmd) {
        return service.runReadActions(cmd);
    }

    @Override
    @PermissionRequired(action = "DROP", actionName = "解除纳管")
    @SendAuditLog(
            action = "DropResources",
            actionName = "解除纳管",
            objectType = "Resource",
            objectTypeName = "云资源"
    )
    public DropResourcesResponse dropResources(@RequestBody BatchDropCmd cmd) {
        try {
            return service.dropResources(cmd);
        }catch (DataIntegrityViolationException e){
            log.warn(e.toString());
            throw new BadCommandException("该资源仍被资源栈使用");
        }
    }

    @Override
    public DescribeQuickStatsResponse describeResourceQuickStats(@RequestBody DescribeQuickStatsRequest request) {
        return service.describeResourceQuickStats(request);
    }

    @Override
    @PermissionRequired(action = "ASSOCIATE_TAGS", actionName = "绑定标签")
    @SendAuditLog(
            action = "AssociateTags",
            actionName = "绑定标签",
            objectType = "Resource",
            objectTypeName = "云资源"
    )
    public AssociateTagsResponse associateTags(@RequestBody AssociateTagsCmd cmd) {
        return service.associateTags(cmd);
    }

    @Override
    @PermissionRequired(action = "DISASSOCIATE_TAG", actionName = "解绑标签")
    @SendAuditLog(
            action = "DisassociateTag",
            actionName = "解绑标签",
            objectType = "Resource",
            objectTypeName = "云资源"
    )
    public DisassociateTagResponse disassociateTag(@RequestBody DisassociateTagCmd cmd) {
        return service.disassociateTag(cmd);
    }

    @Override
    @PermissionRequired(action = "UPDATE_DESCRIPTION", actionName = "更新资源描述")
    @SendAuditLog(
            action = "UpdateDescription",
            actionName = "更新资源描述",
            objectType = "Resource",
            objectTypeName = "云资源"
    )
    public UpdateDescriptionResponse updateDescription(@RequestBody UpdateDescriptionCmd cmd) {
        return service.updateDescription(cmd);
    }
}
