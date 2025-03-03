package com.stratocloud.account;

import java.util.List;

public record ExternalAccountFilters(List<Long> accountIds,
                                     List<String> providerIds,
                                     String resourceCategory,
                                     String search, Boolean disabled) {
}