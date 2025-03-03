package com.stratocloud.provider.huawei.elb.rule.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiElbRuleBuildInput implements ResourceActionInput {
    @SelectField(
            label = "规则类型",
            options = {
                    "HOST_NAME",
                    "PATH",
                    "METHOD",
                    "HEADER",
                    "QUERY_STRING",
                    "SOURCE_IP",
                    "COOKIE"
            },
            optionNames = {
                    "匹配域名",
                    "匹配请求路径",
                    "匹配请求方法",
                    "匹配请求头",
                    "匹配请求查询参数",
                    "匹配请求源IP地址",
                    "匹配cookie信息",
            }
    )
    private String type;

    @SelectField(
            label = "匹配方式",
            options = {
                    "EQUAL_TO",
                    "REGEX",
                    "STARTS_WITH",
            },
            optionNames = {
                    "精确匹配",
                    "正则匹配",
                    "前缀匹配",
            },
            conditions = "this.type === 'PATH'"
    )
    private String compareType;

    @InputField(
            label = "匹配项名称",
            conditions = "this.type === 'HEADER' || this.type === 'QUERY_STRING' || this.type === 'COOKIE'"
    )
    private String key;

    @SelectField(label = "匹配值", multiSelect = true, allowCreate = true)
    private List<String> values;
}
