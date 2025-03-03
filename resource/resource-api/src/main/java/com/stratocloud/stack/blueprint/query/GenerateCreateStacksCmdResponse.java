package com.stratocloud.stack.blueprint.query;

import com.stratocloud.request.ApiResponse;
import com.stratocloud.stack.runtime.cmd.CreateResourceStacksCmd;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GenerateCreateStacksCmdResponse extends ApiResponse {
    private CreateResourceStacksCmd cmd;
}
