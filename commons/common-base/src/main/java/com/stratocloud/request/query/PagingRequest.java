package com.stratocloud.request.query;

import com.stratocloud.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public abstract class PagingRequest implements QueryRequest {
    private Integer current = 0;
    private Integer size = 10;

    private String sortedBy = "id";

    private Sort.Direction direction = Sort.Direction.DESC;


    public Pageable getPageable(){
        if(Utils.isNotBlank(sortedBy)){
            return PageRequest.of(current, size, Sort.by(direction, sortedBy));
        }else {
            return PageRequest.of(current, size);
        }
    }
}
