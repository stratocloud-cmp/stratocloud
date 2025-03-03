package com.stratocloud.script.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateSoftwareDefinitionResponse extends ApiResponse {
    private Long softwareDefinitionId;
}
