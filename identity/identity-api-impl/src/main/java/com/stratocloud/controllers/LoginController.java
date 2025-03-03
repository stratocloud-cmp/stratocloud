package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.user.LoginApi;
import com.stratocloud.user.LoginService;
import com.stratocloud.user.cmd.LoginCmd;
import com.stratocloud.user.response.LoginResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController implements LoginApi {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    @SendAuditLog(
            action = "Login",
            actionName = "用户登录",
            objectType = "User",
            objectTypeName = "用户"
    )
    public LoginResponse login(@RequestBody LoginCmd cmd) {
        return loginService.login(cmd);
    }
}
