package com.stratocloud.user;

import com.stratocloud.identity.BuiltInAuthTypes;
import com.stratocloud.exceptions.StratoAuthenticationException;
import com.stratocloud.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class DefaultUserAuthenticator implements UserAuthenticator {

    @Override
    public String getAuthType() {
        return BuiltInAuthTypes.DEFAULT_AUTH_TYPE;
    }

    @Override
    public void authenticate(User user, Password password) {
        EncodedPassword encodedPassword = encodePassword(password);
        EncodedPassword userPassword = user.getPassword();

        if(!Objects.equals(encodedPassword, userPassword))
            throw new StratoAuthenticationException("用户名或密码不正确");
    }

    @Override
    public EncodedPassword encodePassword(Password password) {
        String value = password.value();
        String encodedValue = SecurityUtil.toSaltMD5(value);
        return new EncodedPassword(encodedValue);
    }

    @Override
    public Password preEncodePassword(String password) {
        return new Password(
                SecurityUtil.toMD5(password)
        );
    }
}
