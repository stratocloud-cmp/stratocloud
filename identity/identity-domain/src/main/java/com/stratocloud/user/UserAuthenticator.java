package com.stratocloud.user;

public interface UserAuthenticator {

    String getAuthType();

    void authenticate(User user, Password password);

    EncodedPassword encodePassword(Password password);

    Password preEncodePassword(String password);

}
