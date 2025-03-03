package com.stratocloud.user.response;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateUserResponse extends ApiResponse {
    private Long userId;
}
