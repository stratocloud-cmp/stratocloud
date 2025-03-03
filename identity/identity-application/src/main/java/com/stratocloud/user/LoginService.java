package com.stratocloud.user;

import com.stratocloud.user.cmd.LoginCmd;
import com.stratocloud.user.response.LoginResponse;

public interface LoginService {
    LoginResponse login(LoginCmd cmd);
}
