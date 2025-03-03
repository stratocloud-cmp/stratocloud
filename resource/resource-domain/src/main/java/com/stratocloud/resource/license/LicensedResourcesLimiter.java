package com.stratocloud.resource.license;

public interface LicensedResourcesLimiter {
    void validateLimitForCategory(String category);
}
