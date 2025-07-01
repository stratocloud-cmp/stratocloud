package com.stratocloud.kubernetes.persistence.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.kubernetes.persistence.KubernetesPvHandler;
import com.stratocloud.kubernetes.persistence.KubernetesPvcHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.RelationshipActionResult;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KubernetesPvcToPvHandler implements ExclusiveRequirementHandler {

    private final KubernetesPvcHandler pvcHandler;

    private final KubernetesPvHandler pvHandler;

    public KubernetesPvcToPvHandler(KubernetesPvcHandler pvcHandler,
                                    KubernetesPvHandler pvHandler) {
        this.pvcHandler = pvcHandler;
        this.pvHandler = pvHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "KUBERNETES_PVC_TO_PV_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "K8s PVC与PV";
    }

    @Override
    public ResourceHandler getSource() {
        return pvcHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return pvHandler;
    }

    @Override
    public String getCapabilityName() {
        return "PVC";
    }

    @Override
    public String getRequirementName() {
        return "PV";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public RelationshipActionResult checkConnectResult(ExternalAccount account, Relationship relationship) {
        return RelationshipActionResult.finished();
    }

    @Override
    public boolean visibleInForm() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<V1PersistentVolumeClaim> pvc = pvcHandler.describePersistentVolumeClaim(
                account, source.externalId()
        );

        if(pvc.isEmpty())
            return List.of();

        if(pvc.get().getSpec() == null)
            return List.of();

        String volumeName = pvc.get().getSpec().getVolumeName();

        Optional<ExternalResource> pv = pvHandler.describeExternalResource(account, volumeName);

        return pv.map(er -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        er,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
