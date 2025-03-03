package com.stratocloud.rule.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteNamingRuleResponse extends ApiResponse {
    private String nextName;
}
