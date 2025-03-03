package com.stratocloud.stack.blueprint.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateBlueprintResponse extends ApiResponse {
    private Long blueprintId;
}
