package com.stratocloud.tag;

import com.stratocloud.tag.cmd.*;
import com.stratocloud.tag.query.DescribeTagEntriesRequest;
import com.stratocloud.tag.query.DescribeTagValuesRequest;
import com.stratocloud.tag.query.NestedTagEntryResponse;
import com.stratocloud.tag.query.NestedTagValueResponse;
import com.stratocloud.tag.response.*;
import org.springframework.data.domain.Page;

public interface TagService {
    CreateTagEntryResponse createTagEntry(CreateTagEntryCmd cmd);

    UpdateTagEntryResponse updateTagEntry(UpdateTagEntryCmd cmd);

    DeleteTagEntriesResponse deleteTagEntries(DeleteTagEntriesCmd cmd);

    AddTagValueResponse addTagValue(AddTagValueCmd cmd);

    RemoveTagValuesResponse removeTagValues(RemoveTagValuesCmd cmd);

    Page<NestedTagEntryResponse> describeTagEntries(DescribeTagEntriesRequest request);

    Page<NestedTagValueResponse> describeTagValues(DescribeTagValuesRequest request);

    DisableTagEntriesResponse disableTagEntries(DisableTagEntriesCmd cmd);

    EnableTagEntriesResponse enableTagEntries(EnableTagEntriesCmd cmd);
}
