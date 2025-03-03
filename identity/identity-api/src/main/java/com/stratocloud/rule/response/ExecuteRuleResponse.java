package com.stratocloud.rule.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteRuleResponse extends ApiResponse {
    private Object output;
}
