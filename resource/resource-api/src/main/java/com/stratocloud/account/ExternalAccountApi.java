package com.stratocloud.account;

import com.stratocloud.account.cmd.*;
import com.stratocloud.account.query.DescribeAccountsRequest;
import com.stratocloud.account.query.NestedAccountResponse;
import com.stratocloud.account.response.*;
import com.stratocloud.constant.StratoServices;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ExternalAccountApi {
    @PostMapping(StratoServices.RESOURCE_SERVICE+"/create-external-account")
    CreateExternalAccountResponse createExternalAccount(@RequestBody CreateExternalAccountCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/update-external-account")
    UpdateExternalAccountResponse updateExternalAccount(@RequestBody UpdateExternalAccountCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/delete-external-accounts")
    DeleteExternalAccountsResponse deleteExternalAccounts(@RequestBody DeleteExternalAccountsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/enable-external-accounts")
    EnableAccountsResponse enableExternalAccounts(@RequestBody EnableAccountsCmd cmd);

    @PostMapping(StratoServices.RESOURCE_SERVICE+"/disable-external-accounts")
    DisableAccountsResponse disableExternalAccounts(@RequestBody DisableAccountsCmd cmd);


    @PostMapping(StratoServices.RESOURCE_SERVICE+"/describe-external-accounts")
    Page<NestedAccountResponse> describeAccounts(@RequestBody DescribeAccountsRequest request);

}
