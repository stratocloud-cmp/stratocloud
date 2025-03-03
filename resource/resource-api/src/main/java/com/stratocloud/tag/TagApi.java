package com.stratocloud.tag;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.tag.cmd.*;
import com.stratocloud.tag.query.DescribeTagEntriesRequest;
import com.stratocloud.tag.query.DescribeTagValuesRequest;
import com.stratocloud.tag.query.NestedTagEntryResponse;
import com.stratocloud.tag.query.NestedTagValueResponse;
import com.stratocloud.tag.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TagApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-tag-entry")
    CreateTagEntryResponse createTagEntry(@RequestBody CreateTagEntryCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-tag-entry")
    UpdateTagEntryResponse updateTagEntry(@RequestBody UpdateTagEntryCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/delete-tag-entries")
    DeleteTagEntriesResponse deleteTagEntries(@RequestBody DeleteTagEntriesCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/add-tag-value")
    AddTagValueResponse addTagValue(@RequestBody AddTagValueCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/remove-tag-values")
    RemoveTagValuesResponse removeTagValues(@RequestBody RemoveTagValuesCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/disable-tag-entries")
    DisableTagEntriesResponse disableTagEntries(@RequestBody DisableTagEntriesCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/enable-tag-entries")
    EnableTagEntriesResponse enableTagEntries(@RequestBody EnableTagEntriesCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-tag-entries")
    Page<NestedTagEntryResponse> describeTagEntries(@RequestBody DescribeTagEntriesRequest request);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-tag-values")
    Page<NestedTagValueResponse> describeTagValues(@RequestBody DescribeTagValuesRequest request);
}
