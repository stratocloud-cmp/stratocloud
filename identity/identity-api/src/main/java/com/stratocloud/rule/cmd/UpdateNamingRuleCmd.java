package com.stratocloud.rule.cmd;

import com.stratocloud.rule.SuffixType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateNamingRuleCmd extends UpdateRuleCmd {
    private SuffixType suffixType;
    private Integer suffixLength;
    private Integer suffixStartNumber;
}
