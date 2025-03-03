package com.stratocloud.job.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class DescribeJobTriggersRequest extends PagingRequest {
    private String search;

    @Override
    public Pageable getPageable() {
        Pageable pageable = super.getPageable();
        return PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "triggerId")
        );
    }
}
