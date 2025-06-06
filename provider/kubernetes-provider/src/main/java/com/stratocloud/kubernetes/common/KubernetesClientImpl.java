package com.stratocloud.kubernetes.common;

import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class KubernetesClientImpl implements KubernetesClient {

    private final KubeConfig kubeConfig;

    public KubernetesClientImpl(String kubeConfigYaml){
        this.kubeConfig = KubeConfig.loadKubeConfig(
                new StringReader(kubeConfigYaml)
        );
    }

    private ApiClient buildClient() {
        try {
            return Config.fromConfig(kubeConfig);
        } catch (IOException e) {
            throw new ExternalAccountInvalidException(e);
        }
    }


    private CoreV1Api buildCoreV1Api(){
        return new CoreV1Api(buildClient());
    }

    private DiscoveryV1Api buildDiscoveryV1Api(){
        return new DiscoveryV1Api(buildClient());
    }

    private NetworkingV1Api buildNetworkingV1Api(){
        return new NetworkingV1Api(buildClient());
    }

    private NodeV1Api buildNodeV1Api(){
        return new NodeV1Api(buildClient());
    }

    private AppsV1Api buildAppsV1Api(){
        return new AppsV1Api(buildClient());
    }

    private ApisApi buildApisApi(){
        return new ApisApi(buildClient());
    }

    private BatchV1Api buildBatchV1Api(){
        return new BatchV1Api(buildClient());
    }

    private interface Invoker<R> {
        R invoke() throws ApiException;
    }

    private static <R> R tryInvoke(Invoker<R> invoker){
        return doTryInvoke(invoker, 0);
    }

    private static <R> R doTryInvoke(Invoker<R> invoker, int triedTimes) {
        if(triedTimes >= 10)
            throw new StratoException("Max triedTimes exceeded: "+triedTimes);

        try {
            return invoker.invoke();
        } catch (ApiException e) {
            log.warn("ErrorCode: {}", e.getCode());
            log.warn("Message: {}", e.getMessage());

            if(e.getCode() >= 500) {
                throw new ProviderConnectionException(e.getMessage(), e);
            } else if(e.getCode() == 401) {
                throw new ExternalAccountInvalidException(e.getMessage(), e);
            } else if(e.getCode() == 404) {
                throw new ExternalResourceNotFoundException(e.getMessage(), e);
            } else if(e.getCode() == 429) {
                log.warn("Retrying later: {}", e.getMessage());
                SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                return doTryInvoke(invoker, triedTimes + 1);
            } else {
                throw new StratoException(e.getMessage(), e);
            }
        }catch (Exception e){
            throw new ProviderConnectionException(e.getMessage(), e);
        }
    }

    private  <E, R> List<E> queryAllByToken(Invoker<R> invoker,
                                            Consumer<Integer> limitSetter,
                                            Function<R, List<E>> listGetter,
                                            Function<R, String> continueTokenGetter,
                                            Consumer<String> nextTokenSetter){
        try {
            limitSetter.accept(50);

            R r = tryInvoke(invoker);

            List<E> result = new ArrayList<>();

            List<E> page = listGetter.apply(r);
            if(Utils.isNotEmpty(page))
                result.addAll(page);

            String nextToken = continueTokenGetter.apply(r);

            while (Utils.isNotBlank(nextToken)){
                nextTokenSetter.accept(nextToken);

                r = tryInvoke(invoker);

                page = listGetter.apply(r);
                if(Utils.isNotEmpty(page))
                    result.addAll(page);

                nextToken = continueTokenGetter.apply(r);
            }

            return result;
        }catch (ExternalResourceNotFoundException e){
            return List.of();
        }
    }

    private <T> Optional<T> queryOne(Invoker<T> invoker){
        try {
            return Optional.ofNullable(tryInvoke(invoker));
        }catch (ExternalResourceNotFoundException e){
            log.warn(e.toString());
            return Optional.empty();
        }
    }

    private String getContinueToken(V1ListMeta listMeta){
        return listMeta != null ? listMeta.getContinue() : null;
    }

    @Override
    public void testConnection(){
        V1APIGroupList apiGroupList = tryInvoke(
                () -> buildApisApi().getAPIVersions().execute()
        );

        log.info("Connected to kubernetes server: {}. ApiVersion={}.",
                kubeConfig.getServer(), apiGroupList.getApiVersion());
    }

    private static void handleResultStatus(V1Status status, String action) {
        if("Success".equals(status.getStatus())){
            log.info("Kubernetes {} action succeeded. Status={}.", action, JSON.toJsonString(status));
        }else if("Failure".equals(status.getStatus())){
            log.error("Kubernetes {} action failed. Status={}.",
                    action, JSON.toJsonString(status));
            throw new StratoException(status.getMessage());
        }else {
            log.warn("Unknown kubernetes result status. Action={}. Status={}.",
                    action, JSON.toJsonString(status));
        }
    }

    private static void handleObjectCreated(V1ObjectMeta metadata, String objectKind) {
        if(metadata == null){
            log.warn("Created object's metadata is null.");
            return;
        }
        log.info("Kubernetes {} created. Name={}. UID={}.",
                objectKind, metadata.getName(), metadata.getUid());
    }

    private static void handleObjectDeleted(V1ObjectMeta metadata, String objectKind) {
        if(metadata == null){
            log.warn("Deleted object's metadata is null.");
            return;
        }
        log.info("Kubernetes {} deleted. Name={}. UID={}.",
                objectKind, metadata.getName(), metadata.getUid());
    }

    private static String getDryRunOption(boolean dryRun) {
        return dryRun ? "All" : null;
    }

    @Override
    public List<V1Namespace> describeNamespaces(){
        CoreV1Api.APIlistNamespaceRequest request = buildCoreV1Api().listNamespace();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1NamespaceList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Namespace> describeNamespace(String name){

        return queryOne(
                () -> buildCoreV1Api().readNamespace(name).execute()
        );
    }

    @Override
    public V1Namespace createNamespace(V1Namespace namespace, boolean dryRun){
        V1Namespace result = tryInvoke(
                () -> buildCoreV1Api().createNamespace(namespace).dryRun(getDryRunOption(dryRun)).execute()
        );
        handleObjectCreated(result.getMetadata(), "Namespace");
        return result;
    }



    @Override
    public void deleteNamespace(String name, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildCoreV1Api().deleteNamespace(name).dryRun(getDryRunOption(dryRun)).execute()
        );
        handleResultStatus(status, "DeleteNamespace");
    }



    @Override
    public List<V1Node> describeNodes(){
        CoreV1Api.APIlistNodeRequest request = buildCoreV1Api().listNode();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1NodeList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Node> describeNode(String name){
        return queryOne(
                () -> buildCoreV1Api().readNode(name).execute()
        );
    }

    @Override
    public V1Node createNode(V1Node node, boolean dryRun){
        V1Node result = tryInvoke(
                () -> buildCoreV1Api().createNode(node).dryRun(getDryRunOption(dryRun)).execute()
        );
        handleObjectCreated(result.getMetadata(), "Node");
        return result;
    }

    @Override
    public void deleteNode(String name, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildCoreV1Api().deleteNode(name).dryRun(getDryRunOption(dryRun)).execute()
        );
        handleResultStatus(status, "DeleteNode");
    }


    @Override
    public List<V1Service> describeServices(){
        CoreV1Api.APIlistServiceForAllNamespacesRequest request = buildCoreV1Api().listServiceForAllNamespaces();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1ServiceList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Service> describeService(NamespacedRef ref){
        return queryOne(
                () -> buildCoreV1Api().readNamespacedService(ref.name(), ref.namespace()).execute()
        );
    }

    @Override
    public V1Service createService(String namespace, V1Service service, boolean dryRun){
        V1Service result = tryInvoke(
                () -> buildCoreV1Api().createNamespacedService(
                        namespace, service
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );
        handleObjectCreated(result.getMetadata(), "Service");

        return result;
    }

    @Override
    public void deleteService(NamespacedRef ref, boolean dryRun){
        V1Service service = tryInvoke(
                () -> buildCoreV1Api().deleteNamespacedService(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );
        handleObjectDeleted(service.getMetadata(), "Service");
    }

    @Override
    public List<V1EndpointSlice> describeEndpointSlices(){
        DiscoveryV1Api.APIlistEndpointSliceForAllNamespacesRequest request
                = buildDiscoveryV1Api().listEndpointSliceForAllNamespaces();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1EndpointSliceList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1EndpointSlice> describeEndpointSlice(NamespacedRef ref){
        return queryOne(
                () -> buildDiscoveryV1Api().readNamespacedEndpointSlice(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1EndpointSlice createEndpointSlice(String namespace, V1EndpointSlice endpointSlice, boolean dryRun){
        V1EndpointSlice result = tryInvoke(
                () -> buildDiscoveryV1Api().createNamespacedEndpointSlice(
                        namespace, endpointSlice
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );
        handleObjectCreated(endpointSlice.getMetadata(), "EndpointSlice");
        return result;
    }

    @Override
    public void deleteEndpointSlice(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildDiscoveryV1Api().deleteNamespacedEndpointSlice(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );
        handleResultStatus(status, "DeleteEndpointSlice");
    }


    @Override
    public List<V1Ingress> describeIngresses(){
        var request = buildNetworkingV1Api().listIngressForAllNamespaces();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1IngressList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Ingress> describeIngress(NamespacedRef ref){
        return queryOne(
                () -> buildNetworkingV1Api().readNamespacedIngress(ref.name(), ref.namespace()).execute()
        );
    }

    @Override
    public V1Ingress createIngress(String namespace, V1Ingress ingress, boolean dryRun){
        V1Ingress result = tryInvoke(
                () -> buildNetworkingV1Api().createNamespacedIngress(
                        namespace, ingress
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "Ingress");

        return result;
    }


    @Override
    public void deleteIngress(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildNetworkingV1Api().deleteNamespacedIngress(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteIngress");
    }

    @Override
    public List<V1IngressClass> describeIngressClasses(){
        var request = buildNetworkingV1Api().listIngressClass();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1IngressClassList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1IngressClass> describeIngressClass(String name){
        return queryOne(
                () -> buildNetworkingV1Api().readIngressClass(name).execute()
        );
    }


    @Override
    public V1IngressClass createIngressClass(V1IngressClass ingressClass, boolean dryRun){
        V1IngressClass result = tryInvoke(
                () -> buildNetworkingV1Api().createIngressClass(
                        ingressClass
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "IngressClass");

        return result;
    }

    @Override
    public void deleteIngressClass(String name, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildNetworkingV1Api().deleteIngressClass(name).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteIngressClass");
    }

    @Override
    public List<V1NetworkPolicy> describeNetworkPolicies(){
        var request = buildNetworkingV1Api().listNetworkPolicyForAllNamespaces();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1NetworkPolicyList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1NetworkPolicy> describeNetworkPolicy(NamespacedRef ref){
        return queryOne(
                () -> buildNetworkingV1Api().readNamespacedNetworkPolicy(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1NetworkPolicy createNetworkPolicy(String namespace,
                                               V1NetworkPolicy networkPolicy,
                                               boolean dryRun){
        V1NetworkPolicy result = tryInvoke(
                () -> buildNetworkingV1Api().createNamespacedNetworkPolicy(
                        namespace, networkPolicy
                ).dryRun(getDryRunOption(dryRun)).execute()
        );

        handleObjectCreated(result.getMetadata(), "NetworkPolicy");

        return result;
    }

    @Override
    public void deleteNetworkPolicy(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildNetworkingV1Api().deleteNamespacedNetworkPolicy(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteNetworkPolicy");
    }

    @Override
    public List<V1RuntimeClass> describeRuntimeClasses(){
        NodeV1Api.APIlistRuntimeClassRequest request = buildNodeV1Api().listRuntimeClass();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1RuntimeClassList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1RuntimeClass> describeRuntimeClass(String name){
        return queryOne(
                () -> buildNodeV1Api().readRuntimeClass(name).execute()
        );
    }

    @Override
    public V1RuntimeClass createRuntimeClass(V1RuntimeClass runtimeClass, boolean dryRun){
        V1RuntimeClass result = tryInvoke(
                () -> buildNodeV1Api().createRuntimeClass(runtimeClass).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "RuntimeClass");

        return result;
    }

    @Override
    public void deleteRuntimeClass(String name, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildNodeV1Api().deleteRuntimeClass(name).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteRuntimeClass");
    }

    @Override
    public List<V1Pod> describePods(){
        var request = buildCoreV1Api().listPodForAllNamespaces();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1PodList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Pod> describePod(NamespacedRef ref){
        return queryOne(
                () -> buildCoreV1Api().readNamespacedPod(ref.name(), ref.namespace()).execute()
        );
    }

    @Override
    public V1Pod createPod(String namespace, V1Pod pod, boolean dryRun){
        V1Pod result = tryInvoke(
                () -> buildCoreV1Api().createNamespacedPod(namespace, pod).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );
        handleObjectCreated(result.getMetadata(), "Pod");
        return result;
    }

    @Override
    public void deletePod(NamespacedRef ref, boolean dryRun){
        V1Pod pod = tryInvoke(
                () -> buildCoreV1Api().deleteNamespacedPod(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectDeleted(pod.getMetadata(), "Pod");
    }

    @Override
    public List<V1Deployment> describeDeployments(){
        var request = buildAppsV1Api().listDeploymentForAllNamespaces();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1DeploymentList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Deployment> describeDeployment(NamespacedRef ref){
        return queryOne(
                () -> buildAppsV1Api().readNamespacedDeployment(ref.name(), ref.namespace()).execute()
        );
    }

    @Override
    public V1Deployment createDeployment(String namespace,
                                         V1Deployment deployment,
                                         boolean dryRun){
        V1Deployment result = tryInvoke(
                () -> buildAppsV1Api().createNamespacedDeployment(
                        namespace, deployment
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "Deployment");
        return result;
    }

    @Override
    public void deleteDeployment(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildAppsV1Api().deleteNamespacedDeployment(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteDeployment");
    }

    @Override
    public List<V1StatefulSet> describeStatefulSets(){
        var request = buildAppsV1Api().listStatefulSetForAllNamespaces();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1StatefulSetList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1StatefulSet> describeStatefulSet(NamespacedRef ref){
        return queryOne(
                () -> buildAppsV1Api().readNamespacedStatefulSet(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1StatefulSet createStatefulSet(String namespace, V1StatefulSet statefulSet, boolean dryRun){
        V1StatefulSet result = tryInvoke(
                () -> buildAppsV1Api().createNamespacedStatefulSet(
                        namespace, statefulSet
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "StatefulSet");

        return result;
    }

    @Override
    public void deleteStatefulSet(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildAppsV1Api().deleteNamespacedStatefulSet(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteStatefulSet");
    }

    @Override
    public List<V1DaemonSet> describeDaemonSets(){
        var request = buildAppsV1Api().listDaemonSetForAllNamespaces();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1DaemonSetList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1DaemonSet> describeDaemonSet(NamespacedRef ref){
        return queryOne(
                () -> buildAppsV1Api().readNamespacedDaemonSet(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1DaemonSet createDaemonSet(String namespace, V1DaemonSet daemonSet, boolean dryRun){
        V1DaemonSet result = tryInvoke(
                () -> buildAppsV1Api().createNamespacedDaemonSet(
                        namespace, daemonSet
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "DaemonSet");

        return result;
    }

    @Override
    public void deleteDaemonSet(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildAppsV1Api().deleteNamespacedDaemonSet(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteDaemonSet");
    }

    @Override
    public List<V1CronJob> describeCronJobs(){
        var request = buildBatchV1Api().listCronJobForAllNamespaces();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1CronJobList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1CronJob> describeCronJob(NamespacedRef ref){
        return queryOne(
                () -> buildBatchV1Api().readNamespacedCronJob(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1CronJob createCronJob(String namespace, V1CronJob cronJob, boolean dryRun){
        V1CronJob result = tryInvoke(
                () -> buildBatchV1Api().createNamespacedCronJob(
                        namespace, cronJob
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "CronJob");

        return result;
    }

    @Override
    public void deleteCronJob(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildBatchV1Api().deleteNamespacedCronJob(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteCronJob");
    }

    @Override
    public List<V1Job> describeJobs(){
        var request = buildBatchV1Api().listJobForAllNamespaces();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1JobList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Job> describeJob(NamespacedRef ref){
        return queryOne(
                () -> buildBatchV1Api().readNamespacedJob(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }


    @Override
    public V1Job createJob(String namespace, V1Job job, boolean dryRun){
        V1Job result = tryInvoke(
                () -> buildBatchV1Api().createNamespacedJob(
                        namespace, job
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "Job");

        return result;
    }

    @Override
    public void deleteJob(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildBatchV1Api().deleteNamespacedJob(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteJob");
    }
}
