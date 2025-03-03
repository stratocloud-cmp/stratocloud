package com.stratocloud.provider.relationship;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;

public interface EssentialPrimaryCapabilityHandler extends PrimaryCapabilityHandler {

    @Override
    default RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }
}
