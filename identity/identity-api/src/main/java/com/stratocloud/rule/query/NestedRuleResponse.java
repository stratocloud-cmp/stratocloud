package com.stratocloud.rule.query;

import com.stratocloud.request.query.NestedTenanted;
import com.stratocloud.rule.SuffixType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedRuleResponse extends NestedTenanted {
    private String type;
    private String name;
    private String script;

    private Boolean isNamingRule;

    private SuffixType suffixType;
    private Integer suffixLength;
    private Integer suffixStartNumber;
}
