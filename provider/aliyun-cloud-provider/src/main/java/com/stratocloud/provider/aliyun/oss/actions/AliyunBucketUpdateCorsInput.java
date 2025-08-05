package com.stratocloud.provider.aliyun.oss.actions;

import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class AliyunBucketUpdateCorsInput implements ResourceActionInput {
    @BooleanField(label = "开启CORS")
    private boolean enabled;

    @NestedFormField(
            label = "CORS规则",
            multiple = true,
            conditions = "this.enabled === true",
            nestedFormClass = Rule.class
    )
    private List<Rule> rules;

    @Data
    public static class Rule implements DynamicForm {
        @SelectField(label = "AllowedOrigin", multiSelect = true, allowCreate = true, defaultValues = "*")
        private List<String> allowedOrigins;
        @SelectField(
                label = "AllowedMethod",
                multiSelect = true,
                defaultValues = "GET",
                options = {
                        "GET",
                        "PUT",
                        "DELETE",
                        "POST",
                        "HEAD"
                },
                optionNames = {
                        "GET",
                        "PUT",
                        "DELETE",
                        "POST",
                        "HEAD"
                }
        )
        private List<String> allowedMethods;
        @SelectField(label = "AllowedHeader", multiSelect = true, allowCreate = true, required = false)
        private List<String> allowedHeaders;
        @SelectField(label = "ExposeHeader", multiSelect = true, allowCreate = true, required = false)
        private List<String> exposeHeaders;
        @NumberField(label = "Max-Age (秒)", defaultValue = 600)
        private Integer maxAgeSeconds;
    }
}
