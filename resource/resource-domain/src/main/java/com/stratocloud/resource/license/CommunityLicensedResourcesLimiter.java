package com.stratocloud.resource.license;

import org.springframework.stereotype.Component;

@Component
public class CommunityLicensedResourcesLimiter implements LicensedResourcesLimiter {
    @Override
    public void validateLimitForCategory(String category) {}
}
