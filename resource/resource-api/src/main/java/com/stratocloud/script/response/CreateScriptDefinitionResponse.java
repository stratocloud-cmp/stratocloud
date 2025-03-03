package com.stratocloud.script.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateScriptDefinitionResponse extends ApiResponse {
    private Long scriptDefinitionId;
}
