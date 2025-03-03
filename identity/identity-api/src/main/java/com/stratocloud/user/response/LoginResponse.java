package com.stratocloud.user.response;


import com.stratocloud.auth.UserSession;
import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse extends ApiResponse {
    private UserSession userSession;
}
