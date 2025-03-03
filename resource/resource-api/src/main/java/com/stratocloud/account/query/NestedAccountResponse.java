package com.stratocloud.account.query;

import com.stratocloud.account.ExternalAccountState;
import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NestedAccountResponse extends NestedTenanted {
    private String providerId;
    private String providerName;
    private String name;
    private Map<String, Object> properties;
    private String description;
    private Boolean disabled;

    private ExternalAccountState state;
    private Float balance;
}
