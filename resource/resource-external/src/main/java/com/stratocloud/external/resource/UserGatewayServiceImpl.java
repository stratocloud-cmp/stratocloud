package com.stratocloud.external.resource;

import com.stratocloud.identity.SimpleUser;
import com.stratocloud.user.UserApi;
import com.stratocloud.user.query.DescribeUsersRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("userGatewayForResource")
public class UserGatewayServiceImpl implements UserGatewayService {

    private final UserApi userApi;

    public UserGatewayServiceImpl(UserApi userApi) {
        this.userApi = userApi;
    }

    @Override
    public List<SimpleUser> findUsers(List<Long> userIds) {
        DescribeUsersRequest request = new DescribeUsersRequest();
        request.setUserIds(userIds);
        return userApi.describeSimpleUsers(request).getUsers();
    }
}
