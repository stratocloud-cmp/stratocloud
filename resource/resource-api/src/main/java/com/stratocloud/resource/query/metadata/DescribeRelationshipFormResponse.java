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
public class DescribeRelationshipFormResponse extends ApiResponse {
    private DynamicFormMetaData formMetaData;
}
