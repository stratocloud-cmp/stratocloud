package com.stratocloud.user;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.user.cmd.LoginCmd;
import com.stratocloud.user.response.LoginResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface LoginApi {
    @PostMapping(path = StratoServices.IDENTITY_SERVICE + "/login")
    LoginResponse login(@RequestBody LoginCmd cmd);
}
