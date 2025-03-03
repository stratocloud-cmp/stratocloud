package com.stratocloud.script.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeSoftwareDefinitionsRequest extends PagingRequest {
    private String search;

    private List<Long> tenantIds;
    private List<Long> ownerIds;


    private Boolean disabled;
}
