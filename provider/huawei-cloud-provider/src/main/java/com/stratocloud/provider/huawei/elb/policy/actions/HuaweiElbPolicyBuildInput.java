package com.stratocloud.provider.huawei.elb.policy.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiElbPolicyBuildInput implements ResourceActionInput {
    @SelectField(
            label = "转发动作",
            options = {
                    "REDIRECT_TO_POOL",
                    "REDIRECT_TO_LISTENER",
                    "REDIRECT_TO_URL",
                    "FIXED_RESPONSE"
            },
            optionNames = {
                    "转发到后端服务器组",
                    "重定向到监听器",
                    "重定向到URL",
                    "返回固定响应体"
            }
    )
    private String action;

    @NumberField(label = "转发优先级", max = 10000, required = false)
    private Integer priority;

    @InputField(label = "重定向协议", required = false, conditions = "this.action === 'REDIRECT_TO_URL'")
    private String redirectUrlProtocol;
    @InputField(label = "重定向主机名", conditions = "this.action === 'REDIRECT_TO_URL'")
    private String redirectUrlHost;
    @NumberField(label = "重定向端口", required = false, conditions = "this.action === 'REDIRECT_TO_URL'")
    private Integer redirectUrlPort;
    @InputField(label = "重定向路径", required = false, conditions = "this.action === 'REDIRECT_TO_URL'")
    private String redirectUrlPath;
    @InputField(label = "重定向查询字符串", required = false, conditions = "this.action === 'REDIRECT_TO_URL'")
    private String redirectUrlQuery;
    @SelectField(
            label = "重定向后的响应码",
            options = {
                    "301",
                    "302",
                    "303",
                    "307",
                    "308"
            },
            optionNames = {
                    "301",
                    "302",
                    "303",
                    "307",
                    "308"
            },
            conditions = "this.action === 'REDIRECT_TO_URL'"
    )
    private String redirectUrlStatusCode;

    @InputField(label = "响应码", conditions = "this.action === 'FIXED_RESPONSE'")
    private String fixedStatusCode;

    @InputField(label = "响应体类型", conditions = "this.action === 'FIXED_RESPONSE'", required = false)
    private String fixedContentType;

    @InputField(label = "响应体", conditions = "this.action === 'FIXED_RESPONSE'", required = false)
    private String fixedMessageBody;

    @SelectField(
            label = "要添加的请求头参数",
            defaultValues = "{\"key\": \"\", \"value\":\"\", \"valueType\":\"USER_DEFINED\"}",
            multiSelect = true,
            allowCreate = true,
            conditions = "this.action !== 'REDIRECT_TO_LISTENER'"
    )
    private List<String> insertHeadersConfig;
    @SelectField(
            label = "要移除的请求头参数",
            multiSelect = true,
            allowCreate = true,
            conditions = "this.action !== 'REDIRECT_TO_LISTENER'"
    )
    private List<String> removeHeadersConfig;
}
