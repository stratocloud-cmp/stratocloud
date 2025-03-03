package com.stratocloud.user;

import com.stratocloud.user.cmd.CreateUserCmd;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
    public User createUser(CreateUserCmd cmd) {
        Long tenantId = cmd.getTenantId();
        String loginName = cmd.getLoginName();
        String realName = cmd.getRealName();
        String emailAddress = cmd.getEmailAddress();
        String phoneNumber = cmd.getPhoneNumber();
        String password = cmd.getPassword();
        Long iconId = cmd.getIconId();
        String description = cmd.getDescription();
        String authType = cmd.getAuthType();

        EncodedPassword encodedPassword = getEncodedPassword(password, authType);

        return new User(
                tenantId, loginName, realName,
                emailAddress, phoneNumber, encodedPassword,
                iconId, description, authType
        );
    }

    public static EncodedPassword getEncodedPassword(String password, String authType) {
        UserAuthenticator authenticator = UserAuthenticatorRegistry.getAuthenticator(authType);
        Password preEncodedPassword = authenticator.preEncodePassword(password);
        return authenticator.encodePassword(preEncodedPassword);
    }
}
