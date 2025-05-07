package com.stratocloud.notification.query;

import com.stratocloud.request.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescribeNotificationProvidersResponse extends ApiResponse {
    private List<NestedNotificationProvider> providers;
}
