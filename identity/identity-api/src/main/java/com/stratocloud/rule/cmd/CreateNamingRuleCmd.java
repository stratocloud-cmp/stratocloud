package com.stratocloud.rule.cmd;

import com.stratocloud.rule.SuffixType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNamingRuleCmd extends CreateRuleCmd {
    private SuffixType suffixType;
    private Integer suffixLength;
    private Integer suffixStartNumber;
}
