package com.stratocloud.orchestrator;

import com.stratocloud.exceptions.CyclicRelationshipException;
import com.stratocloud.job.Execution;
import com.stratocloud.job.ExecutionStep;
import com.stratocloud.job.Task;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Orchestrator {

    public Execution orchestrateBuild(Resource root) {
        return orchestrateBuild(List.of(root));
    }

    public Execution orchestrateBuild(List<Resource> roots){
        Execution execution = new Execution();

        Queue<Resource> resourceQueue = new LinkedList<>();

        roots.forEach(resourceQueue::offer);

        Set<Long> rootIds = roots.stream().map(Resource::getId).collect(Collectors.toSet());

        Set<Long> readyTargetIds = new HashSet<>();

        Map<Long, List<Relationship>> requirementsMap = createRequirementsMap(roots);

        while (!resourceQueue.isEmpty()){
            int size = resourceQueue.size();
            ExecutionStep buildStep = new ExecutionStep();
            ExecutionStep connectStep = new ExecutionStep();
            ExecutionStep syncStep = new ExecutionStep();

            for (int i = 0; i < size; i++) {
                Resource current = Objects.requireNonNull(resourceQueue.poll());

                List<Relationship> requirements = requirementsMap.getOrDefault(current.getId(), new ArrayList<>());

                if(isTargetsNotReady(readyTargetIds, requirements)){
                    log.warn("Requirement targets of {} are not ready yet, skipping this one.", current.getName());
                    continue;
                }

                if(readyTargetIds.contains(current.getId()))
                    continue;

                Task buildTask = current.createBuildTask();

                buildStep.addTask(buildTask);

                for (Relationship requirement : requirements) {
                    Task connectTask = requirement.createConnectTask();
                    connectStep.addTask(connectTask);

                    if(requirement.getHandler().synchronizeTarget())
                        addSyncTask(syncStep, requirement.getTarget());

                }

                addSyncTask(syncStep, current);

                List<Relationship> capabilities = current.getCapabilities();

                for (Relationship capability : capabilities) {
                    Resource capabilityResource = capability.getSource();
                    validateNonRootCapability(rootIds, capabilityResource);
                    resourceQueue.offer(capabilityResource);
                }
            }
            execution.addStep(buildStep);
            execution.addStep(connectStep);
            execution.addStep(syncStep);

            buildStep.getTasks().forEach(task -> readyTargetIds.add(task.getEntityId()));
        }

        return execution;
    }

    private static void addSyncTask(ExecutionStep syncStep, Resource resource) {
        Set<Long> syncResourceIds
                = syncStep.getTasks().stream().map(Task::getEntityId).collect(Collectors.toSet());
        if(!syncResourceIds.contains(resource.getId()))
            syncStep.addTask(resource.createSynchronizeTask());
    }

    private static boolean isTargetsNotReady(Set<Long> readyTargetIds,
                                             List<Relationship> requirements) {
        return requirements.stream().map(
                Relationship::getTarget
        ).filter(
                t -> !ResourceState.getAliveStateSet().contains(t.getState())
        ).anyMatch(
                t -> !readyTargetIds.contains(t.getId())
        );
    }

    private static Map<Long, List<Relationship>> createRequirementsMap(List<Resource> roots){
        Map<Long, List<Relationship>> result = new HashMap<>();

        Queue<Resource> queue = new LinkedList<>();

        for (Resource root : roots) {
            queue.offer(root);

            if(Utils.isEmpty(root.getRequirements()))
                continue;

            for (Relationship requirement : root.getRequirements())
                result.computeIfAbsent(root.getId(), k -> new ArrayList<>()).add(requirement);
        }

        while (!queue.isEmpty()){
            Resource resource = queue.poll();

            if(Utils.isEmpty(resource.getCapabilities()))
                continue;

            for (Relationship capability : resource.getCapabilities()) {
                Resource source = capability.getSource();
                queue.offer(source);
                result.computeIfAbsent(source.getId(), k -> new ArrayList<>()).add(capability);
            }
        }

        return result;
    }


    private static void validateNonRootCapability(Set<Long> rootIds, Resource capabilityResource) {
        if(rootIds.contains(capabilityResource.getId())) {
            String message = "Cyclic relationships detected in resource %s".formatted(
                    capabilityResource.getName()
            );
            log.warn(message);
            throw new CyclicRelationshipException(message);
        }
    }

    public Execution orchestrateDestruction(Resource root,
                                            Map<String, Object> parameters,
                                            boolean isRecycleCapabilities) {
        return orchestrateDestruction(List.of(root), parameters, isRecycleCapabilities);
    }


    public Execution orchestrateDestruction(List<Resource> roots,
                                            Map<String, Object> parameters,
                                            boolean isRecycleCapabilities) {
        Execution execution = new Execution();

        Queue<Resource> resourceQueue = new LinkedList<>();

        roots.forEach(resourceQueue::offer);

        Set<Long> rootIds = roots.stream().map(Resource::getId).collect(Collectors.toSet());

        while (!resourceQueue.isEmpty()){
            int size = resourceQueue.size();
            ExecutionStep stopStep = new ExecutionStep();
            ExecutionStep disconnectStep = new ExecutionStep();
            ExecutionStep destroyStep = new ExecutionStep();
            ExecutionStep syncStep = new ExecutionStep();


            for (int i = 0; i < size; i++) {
                Resource current = Objects.requireNonNull(resourceQueue.poll());

                var stopHandler = current.getResourceHandler().getActionHandler(ResourceActions.STOP);
                if(stopHandler.isPresent() && stopHandler.get().getAllowedStates().contains(current.getState()))
                    stopStep.addTask(current.createResourceTask(ResourceActions.STOP.id(), Map.of()));

                List<Relationship> requirements = current.getRequirements();
                for (Relationship requirement : requirements) {
                    if(requirement.getState() == RelationshipState.DISCONNECTED)
                        continue;

                    Task disconnectTask = requirement.createDisconnectTask();
                    disconnectStep.addTask(disconnectTask);

                    if(requirement.getHandler().synchronizeTarget())
                        addSyncTask(syncStep, requirement.getTarget());
                }


                if(isRecycleCapabilities){
                    for (Relationship capability : current.getCapabilities()) {
                        if(capability.getState() == RelationshipState.DISCONNECTED)
                            continue;

                        Resource capabilityResource = capability.getSource();
                        validateNonRootCapability(rootIds, capabilityResource);
                        resourceQueue.offer(capabilityResource);
                    }
                } else {
                    for (Relationship capability : current.getCapabilities()) {
                        if(capability.getState() == RelationshipState.DISCONNECTED)
                            continue;

                        Task disconnectTask = capability.createDisconnectTask();
                        disconnectStep.addTask(disconnectTask);
                    }
                }

                Task destroyTask = current.createDestroyTask(parameters);
                destroyStep.addTask(destroyTask);
            }

            execution.insertStep(0, syncStep);
            execution.insertStep(0, destroyStep);
            execution.insertStep(0, disconnectStep);
            execution.insertStep(0, stopStep);
        }



        return execution;
    }
}
