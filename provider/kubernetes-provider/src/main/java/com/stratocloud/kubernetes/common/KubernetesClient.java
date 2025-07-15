package com.stratocloud.kubernetes.common;

import com.stratocloud.kubernetes.volume.PodVolume;
import com.stratocloud.kubernetes.volume.PodVolumeId;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.openapi.models.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KubernetesClient {
    void testConnection();

    List<V1Namespace> describeNamespaces();

    Optional<V1Namespace> describeNamespace(String name);

    V1Namespace createNamespace(V1Namespace namespace, boolean dryRun);

    void deleteNamespace(String name, boolean dryRun);

    List<V1Node> describeNodes();

    Optional<V1Node> describeNode(String name);

    V1Node createNode(V1Node node, boolean dryRun);

    void deleteNode(String name, boolean dryRun);

    List<V1Service> describeServices();

    Optional<V1Service> describeService(NamespacedRef ref);

    V1Service createService(String namespace, V1Service service, boolean dryRun);

    V1Service updateService(String namespace, V1Service service, boolean dryRun);

    void deleteService(NamespacedRef ref, boolean dryRun);

    List<V1EndpointSlice> describeEndpointSlices();

    List<V1EndpointSlice> describeEndpointSlicesByNamespace(String namespace);

    Optional<V1EndpointSlice> describeEndpointSlice(NamespacedRef ref);

    V1EndpointSlice createEndpointSlice(String namespace, V1EndpointSlice endpointSlice, boolean dryRun);

    void deleteEndpointSlice(NamespacedRef ref, boolean dryRun);

    List<V1Ingress> describeIngresses();

    Optional<V1Ingress> describeIngress(NamespacedRef ref);

    V1Ingress createIngress(String namespace, V1Ingress ingress, boolean dryRun);

    V1Ingress updateIngress(String namespace, V1Ingress ingress, boolean dryRun);

    void deleteIngress(NamespacedRef ref, boolean dryRun);

    List<V1IngressClass> describeIngressClasses();

    Optional<V1IngressClass> describeIngressClass(String name);

    V1IngressClass createIngressClass(V1IngressClass ingressClass, boolean dryRun);

    V1IngressClass updateIngressClass(V1IngressClass ingressClass, boolean dryRun);

    void deleteIngressClass(String name, boolean dryRun);

    List<V1NetworkPolicy> describeNetworkPolicies();

    Optional<V1NetworkPolicy> describeNetworkPolicy(NamespacedRef ref);

    V1NetworkPolicy createNetworkPolicy(String namespace,
                                        V1NetworkPolicy networkPolicy,
                                        boolean dryRun);

    V1NetworkPolicy updateNetworkPolicy(String namespace,
                                        V1NetworkPolicy networkPolicy,
                                        boolean dryRun);

    void deleteNetworkPolicy(NamespacedRef ref, boolean dryRun);

    List<V1RuntimeClass> describeRuntimeClasses();

    Optional<V1RuntimeClass> describeRuntimeClass(String name);

    V1RuntimeClass createRuntimeClass(V1RuntimeClass runtimeClass, boolean dryRun);

    void deleteRuntimeClass(String name, boolean dryRun);

    List<V1Pod> describePods();

    List<V1Pod> describePodsByNamespace(String namespace);

    Optional<V1Pod> describePod(NamespacedRef ref);


    Optional<V1ReplicaSet> describeReplicaSet(NamespacedRef ref);

    List<V1ReplicaSet> describeReplicaSetsByNamespace(String namespace);

    List<V1Deployment> describeDeployments();

    Optional<V1Deployment> describeDeployment(NamespacedRef ref);

    V1Deployment createDeployment(String namespace, V1Deployment deployment, boolean dryRun);

    V1Deployment updateDeployment(String namespace, V1Deployment deployment, boolean dryRun);

    void deleteDeployment(NamespacedRef ref, boolean dryRun);

    List<V1StatefulSet> describeStatefulSets();

    Optional<V1StatefulSet> describeStatefulSet(NamespacedRef ref);

    V1StatefulSet createStatefulSet(String namespace, V1StatefulSet statefulSet, boolean dryRun);

    V1StatefulSet updateStatefulSet(String namespace, V1StatefulSet statefulSet, boolean dryRun);

    void deleteStatefulSet(NamespacedRef ref, boolean dryRun);

    List<V1DaemonSet> describeDaemonSets();

    Optional<V1DaemonSet> describeDaemonSet(NamespacedRef ref);

    V1DaemonSet createDaemonSet(String namespace, V1DaemonSet daemonSet, boolean dryRun);

    V1DaemonSet updateDaemonSet(String namespace, V1DaemonSet daemonSet, boolean dryRun);

    void deleteDaemonSet(NamespacedRef ref, boolean dryRun);

    List<V1CronJob> describeCronJobs();

    Optional<V1CronJob> describeCronJob(NamespacedRef ref);

    V1CronJob createCronJob(String namespace, V1CronJob cronJob, boolean dryRun);

    V1CronJob updateCronJob(String namespace, V1CronJob cronJob, boolean dryRun);

    void deleteCronJob(NamespacedRef ref, boolean dryRun);

    List<V1Job> describeJobs();

    Optional<V1Job> describeJob(NamespacedRef ref);

    V1Job createJob(String namespace, V1Job job, boolean dryRun);

    void deleteJob(NamespacedRef ref, boolean dryRun);

    List<V1PersistentVolume> describePersistentVolumes();

    List<V1PersistentVolumeClaim> describePersistentVolumeClaims();

    Optional<V1PersistentVolume> describePersistentVolume(String name);

    Optional<V1PersistentVolumeClaim> describePersistentVolumeClaim(NamespacedRef ref);

    V1PersistentVolume createPersistentVolume(V1PersistentVolume persistentVolume, boolean dryRun);

    V1PersistentVolumeClaim createPersistentVolumeClaim(String namespace,
                                                        V1PersistentVolumeClaim persistentVolumeClaim,
                                                        boolean dryRun);

    V1PersistentVolume updatePersistentVolume(V1PersistentVolume persistentVolume, boolean dryRun);

    void deletePersistentVolume(String name, boolean dryRun);

    V1PersistentVolumeClaim updatePersistentVolumeClaim(String namespace,
                                                        V1PersistentVolumeClaim pvc,
                                                        boolean dryRun);

    void deletePersistentVolumeClaim(NamespacedRef ref, boolean dryRun);

    List<V1StorageClass> describeStorageClasses();

    Optional<V1StorageClass> describeStorageClass(String name);

    V1StorageClass createStorageClass(V1StorageClass storageClass, boolean dryRun);

    V1StorageClass updateStorageClass(V1StorageClass storageClass, boolean dryRun);

    void deleteStorageClass(String name, boolean dryRun);

    List<V1ConfigMap> describeConfigMaps();

    Optional<V1ConfigMap> describeConfigMap(NamespacedRef ref);

    V1ConfigMap createConfigMap(String namespace, V1ConfigMap configMap, boolean dryRun);

    void deleteConfigMap(NamespacedRef ref, boolean dryRun);

    List<V1Secret> describeSecrets();

    Optional<V1Secret> describeSecret(NamespacedRef ref);

    V1Secret createSecret(String namespace, V1Secret secret, boolean dryRun);

    void deleteSecret(NamespacedRef ref, boolean dryRun);


    List<PodVolume> describePodVolumes();

    Optional<PodVolume> describePodVolume(PodVolumeId podVolumeId);

    List<CoreV1Event> describeEventsByObjectRef(V1ObjectReference ref, LocalDateTime happenedAfter);

    List<CoreV1Event> describeEvents();

    Optional<NodeMetrics> describeNodeMetrics(String nodeName);

    Optional<PodMetrics> describePodMetrics(NamespacedRef podRef);
}
