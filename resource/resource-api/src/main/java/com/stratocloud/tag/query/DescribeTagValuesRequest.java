package com.stratocloud.tag.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeTagValuesRequest extends PagingRequest {
    private String tagEntryKey;
    private String search;
    private List<String> tagValues;
    private List<Long> parentTagValueIds;
}
