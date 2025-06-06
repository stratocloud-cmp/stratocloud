package com.stratocloud.kubernetes.common;

import io.kubernetes.client.openapi.models.*;

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

    void deleteService(NamespacedRef ref, boolean dryRun);

    List<V1EndpointSlice> describeEndpointSlices();

    Optional<V1EndpointSlice> describeEndpointSlice(NamespacedRef ref);

    V1EndpointSlice createEndpointSlice(String namespace, V1EndpointSlice endpointSlice, boolean dryRun);

    void deleteEndpointSlice(NamespacedRef ref, boolean dryRun);

    List<V1Ingress> describeIngresses();

    Optional<V1Ingress> describeIngress(NamespacedRef ref);

    V1Ingress createIngress(String namespace, V1Ingress ingress, boolean dryRun);

    void deleteIngress(NamespacedRef ref, boolean dryRun);

    List<V1IngressClass> describeIngressClasses();

    Optional<V1IngressClass> describeIngressClass(String name);

    V1IngressClass createIngressClass(V1IngressClass ingressClass, boolean dryRun);

    void deleteIngressClass(String name, boolean dryRun);

    List<V1NetworkPolicy> describeNetworkPolicies();

    Optional<V1NetworkPolicy> describeNetworkPolicy(NamespacedRef ref);

    V1NetworkPolicy createNetworkPolicy(String namespace,
                                        V1NetworkPolicy networkPolicy,
                                        boolean dryRun);

    void deleteNetworkPolicy(NamespacedRef ref, boolean dryRun);

    List<V1RuntimeClass> describeRuntimeClasses();

    Optional<V1RuntimeClass> describeRuntimeClass(String name);

    V1RuntimeClass createRuntimeClass(V1RuntimeClass runtimeClass, boolean dryRun);

    void deleteRuntimeClass(String name, boolean dryRun);

    List<V1Pod> describePods();

    Optional<V1Pod> describePod(NamespacedRef ref);

    V1Pod createPod(String namespace, V1Pod pod, boolean dryRun);

    void deletePod(NamespacedRef ref, boolean dryRun);

    List<V1Deployment> describeDeployments();

    Optional<V1Deployment> describeDeployment(NamespacedRef ref);

    V1Deployment createDeployment(String namespace, V1Deployment deployment, boolean dryRun);

    void deleteDeployment(NamespacedRef ref, boolean dryRun);

    List<V1StatefulSet> describeStatefulSets();

    Optional<V1StatefulSet> describeStatefulSet(NamespacedRef ref);

    V1StatefulSet createStatefulSet(String namespace, V1StatefulSet statefulSet, boolean dryRun);

    void deleteStatefulSet(NamespacedRef ref, boolean dryRun);

    List<V1DaemonSet> describeDaemonSets();

    Optional<V1DaemonSet> describeDaemonSet(NamespacedRef ref);

    V1DaemonSet createDaemonSet(String namespace, V1DaemonSet daemonSet, boolean dryRun);

    void deleteDaemonSet(NamespacedRef ref, boolean dryRun);

    List<V1CronJob> describeCronJobs();

    Optional<V1CronJob> describeCronJob(NamespacedRef ref);

    V1CronJob createCronJob(String namespace, V1CronJob cronJob, boolean dryRun);

    void deleteCronJob(NamespacedRef ref, boolean dryRun);

    List<V1Job> describeJobs();

    Optional<V1Job> describeJob(NamespacedRef ref);

    V1Job createJob(String namespace, V1Job job, boolean dryRun);

    void deleteJob(NamespacedRef ref, boolean dryRun);
}
