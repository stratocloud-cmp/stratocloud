package com.stratocloud.resource.query.metadata;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.form.info.DynamicFormMetaData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeResourceActionFormResponse extends ApiResponse {
    private DynamicFormMetaData formMetaData;

    private Long networkResourceId;
}
