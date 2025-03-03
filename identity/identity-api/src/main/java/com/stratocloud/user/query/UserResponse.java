package com.stratocloud.user.query;

import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserResponse extends NestedTenanted {
    private String loginName;
    private String realName;
    private String emailAddress;
    private String phoneNumber;
    private Long iconId;
    private String description;
    private String authType;
    private Boolean disabled;
    private Boolean locked;
    private LocalDateTime lastLoginTime;
    private LocalDateTime passwordExpireTime;

    private List<Long> roleIds;
}
