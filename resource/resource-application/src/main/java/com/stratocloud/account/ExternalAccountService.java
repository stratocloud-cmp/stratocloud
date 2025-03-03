package com.stratocloud.account;

import com.stratocloud.account.cmd.*;
import com.stratocloud.account.query.DescribeAccountsRequest;
import com.stratocloud.account.query.NestedAccountResponse;
import com.stratocloud.account.response.*;
import org.springframework.data.domain.Page;

public interface ExternalAccountService {
    CreateExternalAccountResponse createExternalAccount(CreateExternalAccountCmd cmd);

    UpdateExternalAccountResponse updateExternalAccount(UpdateExternalAccountCmd cmd);

    DeleteExternalAccountsResponse deleteExternalAccounts(DeleteExternalAccountsCmd cmd);

    SynchronizeAccountsResponse synchronizeAccounts(SynchronizeAccountsCmd cmd);

    Page<NestedAccountResponse> describeAccounts(DescribeAccountsRequest request);

    EnableAccountsResponse enableExternalAccounts(EnableAccountsCmd cmd);

    DisableAccountsResponse disableExternalAccounts(DisableAccountsCmd cmd);
}
