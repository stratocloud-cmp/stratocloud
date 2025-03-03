package com.stratocloud.resource.query.inquiry;


import com.stratocloud.request.ApiResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateResourcesPriceInquiryResponse extends ApiResponse {
    private String totalCostDescription;
    private String totalMonthlyCostDescription;
}
