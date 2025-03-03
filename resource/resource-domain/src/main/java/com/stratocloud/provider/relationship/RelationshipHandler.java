package com.stratocloud.provider.relationship;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.repository.ExternalAccountRepository;
import com.stratocloud.resource.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RelationshipHandler {
    String getRelationshipTypeId();

    String getRelationshipTypeName();

    ResourceHandler getSource();

    ResourceHandler getTarget();


    String getCapabilityName();

    String getRequirementName();

    String getConnectActionName();

    String getDisconnectActionName();

    default Class<? extends RelationshipConnectInput> getConnectInputClass(){
        return RelationshipConnectInput.Dummy.class;
    }

    default Optional<DynamicFormMetaData> getDirectConnectInputClassFormMetaData(){
        return Optional.empty();
    }

    default Set<ResourceState> getAllowedSourceStates() {
        return ResourceState.getAliveStateSet();
    }

    default Set<ResourceState> getAllowedTargetStates() {
        return ResourceState.getAliveStateSet();
    }

    void connect(Relationship relationship);

    void disconnect(Relationship relationship);

    default RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship){
        Resource sourceResource = relationship.getSource();
        var externalSource = getSource().describeExternalResource(account, sourceResource.getExternalId());

        if(externalSource.isPresent() && externalSource.get().isAttaching()){
            return RelationshipActionResult.inProgress();
        }

        if(relationship.getHandler().isConnected(relationship, account))
            return RelationshipActionResult.finished();
        else
            return RelationshipActionResult.failed(
                    "Relationship [%s] is not connected.".formatted(relationship.getEntityDescription())
            );
    }
    default RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship){
        Resource sourceResource = relationship.getSource();
        var externalSource = getSource().describeExternalResource(account, sourceResource.getExternalId());

        if(externalSource.isPresent() && externalSource.get().isDetaching()){
            return RelationshipActionResult.inProgress();
        }

        if(relationship.getHandler().isConnected(relationship, account))
            return RelationshipActionResult.failed(
                    "Relationship [%s] is still connected.".formatted(relationship.getEntityDescription())
            );
        else
            return RelationshipActionResult.finished();
    }


    List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                           ExternalResource source);


    default int compareCapability(RelationshipHandler other){
        if(getSource().getResourceCategory().index() != other.getSource().getResourceCategory().index())
            return getSource().getResourceCategory().index() - other.getSource().getResourceCategory().index();

        if(this instanceof PrimaryCapabilityHandler && other instanceof PrimaryCapabilityHandler){
            return 0;
        } else if(this instanceof PrimaryCapabilityHandler) {
            return -1;
        }else if(other instanceof PrimaryCapabilityHandler) {
            return 1;
        }else {
            return 0;
        }
    }

    default int compareRequirement(RelationshipHandler other){
        return getTarget().getResourceCategory().index() - other.getTarget().getResourceCategory().index();
    }

    default boolean isConnected(Relationship relationship, ExternalAccount account){
        Optional<ExternalResource> externalResource = relationship.getSource().toExternalResource();

        if(externalResource.isEmpty())
            return false;

        List<ExternalRequirement> requirements = describeExternalRequirements(account, externalResource.get());
        return requirements.stream().anyMatch(
                externalRequirement -> relationship.getTarget().isSameResource(externalRequirement.target())
        );
    }

    default boolean synchronizeTarget(){
        return false;
    }

    default boolean requireTargetResourceTaskLock(){
        return false;
    }

    default int targetResourceTaskLockMaxSeconds(){
        return 60;
    }


    default ExternalAccountRepository getAccountRepository(){
        return getSource().getAccountRepository();
    }

    default boolean visibleInTarget(){
        return true;
    }

    default boolean disconnectOnLost(){
        return false;
    }

    default boolean isolatedTargetContext(){
        return false;
    }
}
