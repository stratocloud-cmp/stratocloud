package com.stratocloud.account.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateExternalAccountResponse extends ApiResponse {
    private Long externalAccountId;
}
