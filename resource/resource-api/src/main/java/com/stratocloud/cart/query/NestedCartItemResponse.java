package com.stratocloud.cart.query;

import com.stratocloud.request.query.NestedControllable;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NestedCartItemResponse extends NestedControllable {
    private String jobType;
    private String jobTypeName;
    private Map<String, Object> jobParameters;
    private String summary;
}
