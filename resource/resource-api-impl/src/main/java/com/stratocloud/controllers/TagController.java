package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.tag.TagApi;
import com.stratocloud.tag.TagService;
import com.stratocloud.tag.cmd.*;
import com.stratocloud.tag.query.DescribeTagEntriesRequest;
import com.stratocloud.tag.query.DescribeTagValuesRequest;
import com.stratocloud.tag.query.NestedTagEntryResponse;
import com.stratocloud.tag.query.NestedTagValueResponse;
import com.stratocloud.tag.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "ResourceTag", targetName = "标签")
@RestController
public class TagController implements TagApi {

    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateTagEntry",
            actionName = "创建标签",
            objectType = "TagEntry",
            objectTypeName = "标签"
    )
    public CreateTagEntryResponse createTagEntry(@RequestBody CreateTagEntryCmd cmd) {
        return service.createTagEntry(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateTagEntry",
            actionName = "更新标签",
            objectType = "TagEntry",
            objectTypeName = "标签"
    )
    public UpdateTagEntryResponse updateTagEntry(@RequestBody UpdateTagEntryCmd cmd) {
        return service.updateTagEntry(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteTagEntries",
            actionName = "删除标签",
            objectType = "TagEntry",
            objectTypeName = "标签"
    )
    public DeleteTagEntriesResponse deleteTagEntries(@RequestBody DeleteTagEntriesCmd cmd) {
        return service.deleteTagEntries(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "AddTagValue",
            actionName = "添加标签值",
            objectType = "TagEntry",
            objectTypeName = "标签"
    )
    public AddTagValueResponse addTagValue(@RequestBody AddTagValueCmd cmd) {
        return service.addTagValue(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "RemoveTagValues",
            actionName = "移除标签值",
            objectType = "TagEntry",
            objectTypeName = "标签"
    )
    public RemoveTagValuesResponse removeTagValues(@RequestBody RemoveTagValuesCmd cmd) {
        return service.removeTagValues(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "DisableTagEntries",
            actionName = "停用标签",
            objectType = "TagEntry",
            objectTypeName = "标签"
    )
    public DisableTagEntriesResponse disableTagEntries(@RequestBody DisableTagEntriesCmd cmd) {
        return service.disableTagEntries(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "EnableTagEntries",
            actionName = "启用标签",
            objectType = "TagEntry",
            objectTypeName = "标签"
    )
    public EnableTagEntriesResponse enableTagEntries(@RequestBody EnableTagEntriesCmd cmd) {
        return service.enableTagEntries(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedTagEntryResponse> describeTagEntries(@RequestBody DescribeTagEntriesRequest request) {
        return service.describeTagEntries(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedTagValueResponse> describeTagValues(@RequestBody DescribeTagValuesRequest request) {
        return service.describeTagValues(request);
    }
}
