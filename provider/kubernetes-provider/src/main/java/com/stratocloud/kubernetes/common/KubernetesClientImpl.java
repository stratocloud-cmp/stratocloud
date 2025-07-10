package com.stratocloud.kubernetes.common;

import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.volume.PodVolume;
import com.stratocloud.kubernetes.volume.PodVolumeId;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.SleepUtil;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.NodeMetricsList;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.PodMetricsList;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.internal.http2.StreamResetException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class KubernetesClientImpl implements KubernetesClient {

    private final KubeConfig kubeConfig;

    private final CacheService cacheService;

    public KubernetesClientImpl(String kubeConfigYaml, CacheService cacheService){
        this.kubeConfig = KubeConfig.loadKubeConfig(
                new StringReader(kubeConfigYaml)
        );
        this.cacheService = cacheService;
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

    private StorageV1Api buildStorageV1Api(){
        return new StorageV1Api(buildClient());
    }

    private Metrics buildMetricsApi(){
        return new Metrics(buildClient());
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
            } else if(e.getCode() == 429 || isCausedByStreamReset(e)) {
                log.warn("Retrying later: {}", e.getMessage());
                SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                return doTryInvoke(invoker, triedTimes + 1);
            } else {
                throw new StratoException(e.getMessage(), e);
            }
        }catch (Exception e){
            if(isCausedByStreamReset(e)){
                log.warn("Stream was reset, retrying later: {}", e.getCause().getMessage());
                SleepUtil.sleepRandomlyByMilliSeconds(500, 3000);
                return doTryInvoke(invoker, triedTimes + 1);
            }

            throw new ProviderConnectionException(e.getMessage(), e);
        }
    }

    private static boolean isCausedByStreamReset(Exception e) {
        return e.getCause() != null && e.getCause() instanceof StreamResetException;
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

    private void handleObjectReplaced(V1ObjectMeta metadata, String objectKind) {
        if(metadata == null){
            log.warn("Replaced object's metadata is null.");
            return;
        }
        log.info("Kubernetes {} replaced. Name={}. UID={}.",
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
    public List<V1Pod> describePodsByNamespace(String namespace) {
        var request = buildCoreV1Api().listNamespacedPod(namespace);

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
    public Optional<V1ReplicaSet> describeReplicaSet(NamespacedRef ref){
        return queryOne(
                () -> buildAppsV1Api().readNamespacedReplicaSet(
                        ref.name(), ref.namespace()
                ).execute()
        );
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
    public V1Deployment updateDeployment(String namespace, V1Deployment deployment, boolean dryRun){
        V1Deployment result = tryInvoke(
                () -> buildAppsV1Api().replaceNamespacedDeployment(
                        KubeUtil.getObjectName(deployment.getMetadata()),
                        namespace,
                        deployment
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectReplaced(result.getMetadata(), "Deployment");

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
    public V1StatefulSet updateStatefulSet(String namespace, V1StatefulSet statefulSet, boolean dryRun){
        V1StatefulSet result = tryInvoke(
                () -> buildAppsV1Api().replaceNamespacedStatefulSet(
                        KubeUtil.getObjectName(statefulSet.getMetadata()),
                        namespace,
                        statefulSet
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectReplaced(result.getMetadata(), "StatefulSet");

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
    public V1DaemonSet updateDaemonSet(String namespace, V1DaemonSet daemonSet, boolean dryRun){
        V1DaemonSet result = tryInvoke(
                () -> buildAppsV1Api().replaceNamespacedDaemonSet(
                        KubeUtil.getObjectName(daemonSet.getMetadata()),
                        namespace,
                        daemonSet
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectReplaced(result.getMetadata(), "DaemonSet");

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
    public V1CronJob updateCronJob(String namespace, V1CronJob cronJob, boolean dryRun){
        V1CronJob result = tryInvoke(
                () -> buildBatchV1Api().replaceNamespacedCronJob(
                        KubeUtil.getObjectName(cronJob.getMetadata()),
                        namespace,
                        cronJob
                ).execute()
        );

        handleObjectReplaced(result.getMetadata(), "CronJob");

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


    @Override
    public List<V1PersistentVolume> describePersistentVolumes(){
        var request = buildCoreV1Api().listPersistentVolume();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1PersistentVolumeList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1PersistentVolume> describePersistentVolume(String name){
        return queryOne(
                () -> buildCoreV1Api().readPersistentVolume(name).execute()
        );
    }

    @Override
    public V1PersistentVolume createPersistentVolume(V1PersistentVolume persistentVolume, boolean dryRun){
        V1PersistentVolume result = tryInvoke(
                () -> buildCoreV1Api().createPersistentVolume(
                        persistentVolume
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "PersistentVolume");

        return result;
    }

    @Override
    public void deletePersistentVolume(String name, boolean dryRun){
        V1PersistentVolume volume = tryInvoke(
                () -> buildCoreV1Api().deletePersistentVolume(name).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectDeleted(volume.getMetadata(), "PersistentVolume");
    }



    @Override
    public List<V1PersistentVolumeClaim> describePersistentVolumeClaims(){
        var request = buildCoreV1Api().listPersistentVolumeClaimForAllNamespaces();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1PersistentVolumeClaimList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1PersistentVolumeClaim> describePersistentVolumeClaim(NamespacedRef ref){
        return queryOne(
                () -> buildCoreV1Api().readNamespacedPersistentVolumeClaim(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1PersistentVolumeClaim createPersistentVolumeClaim(String namespace,
                                                               V1PersistentVolumeClaim persistentVolumeClaim,
                                                               boolean dryRun){
        V1PersistentVolumeClaim result = tryInvoke(
                () -> buildCoreV1Api().createNamespacedPersistentVolumeClaim(
                        namespace,
                        persistentVolumeClaim
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "PersistentVolumeClaim");

        return result;
    }

    @Override
    public void deletePersistentVolumeClaim(NamespacedRef ref, boolean dryRun){
        V1PersistentVolumeClaim volumeClaim = tryInvoke(
                () -> buildCoreV1Api().deleteNamespacedPersistentVolumeClaim(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectDeleted(volumeClaim.getMetadata(), "PersistentVolumeClaim");
    }


    @Override
    public List<V1StorageClass> describeStorageClasses(){
        StorageV1Api.APIlistStorageClassRequest request = buildStorageV1Api().listStorageClass();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1StorageClassList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1StorageClass> describeStorageClass(String name){
        return queryOne(
                () -> buildStorageV1Api().readStorageClass(name).execute()
        );
    }

    @Override
    public V1StorageClass createStorageClass(V1StorageClass storageClass, boolean dryRun){
        V1StorageClass result = tryInvoke(
                () -> buildStorageV1Api().createStorageClass(
                        storageClass
                ).dryRun(getDryRunOption(dryRun)).execute()
        );

        handleObjectCreated(result.getMetadata(), "StorageClass");

        return result;
    }

    @Override
    public void deleteStorageClass(String name, boolean dryRun){
        V1StorageClass result = tryInvoke(
                () -> buildStorageV1Api().deleteStorageClass(name).execute()
        );

        handleObjectDeleted(result.getMetadata(), "StorageClass");
    }


    @Override
    public List<V1ConfigMap> describeConfigMaps(){
        var request = buildCoreV1Api().listConfigMapForAllNamespaces();

        return queryAllByToken(
                request::execute,
                request::limit,
                V1ConfigMapList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1ConfigMap> describeConfigMap(NamespacedRef ref){
        return queryOne(
                () -> buildCoreV1Api().readNamespacedConfigMap(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1ConfigMap createConfigMap(String namespace, V1ConfigMap configMap, boolean dryRun){
        V1ConfigMap result = tryInvoke(
                () -> buildCoreV1Api().createNamespacedConfigMap(
                        namespace, configMap
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "ConfigMap");
        return result;
    }

    @Override
    public void deleteConfigMap(NamespacedRef ref, boolean dryRun){
        V1Status result = tryInvoke(
                () -> buildCoreV1Api().deleteNamespacedConfigMap(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(result, "DeleteConfigMap");
    }

    @Override
    public List<V1Secret> describeSecrets(){
        var request = buildCoreV1Api().listSecretForAllNamespaces();
        return queryAllByToken(
                request::execute,
                request::limit,
                V1SecretList::getItems,
                resp -> getContinueToken(resp.getMetadata()),
                request::_continue
        );
    }

    @Override
    public Optional<V1Secret> describeSecret(NamespacedRef ref){
        return queryOne(
                () -> buildCoreV1Api().readNamespacedSecret(
                        ref.name(), ref.namespace()
                ).execute()
        );
    }

    @Override
    public V1Secret createSecret(String namespace, V1Secret secret, boolean dryRun){
        V1Secret result = tryInvoke(
                () -> buildCoreV1Api().createNamespacedSecret(
                        namespace, secret
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleObjectCreated(result.getMetadata(), "Secret");

        return result;
    }

    @Override
    public void deleteSecret(NamespacedRef ref, boolean dryRun){
        V1Status status = tryInvoke(
                () -> buildCoreV1Api().deleteNamespacedSecret(
                        ref.name(), ref.namespace()
                ).dryRun(
                        getDryRunOption(dryRun)
                ).execute()
        );

        handleResultStatus(status, "DeleteSecret");
    }


    @Override
    public List<PodVolume> describePodVolumes(){
        List<V1Pod> pods = describePods();
        List<PodVolume> result = new ArrayList<>();

        for (V1Pod pod : pods) {
            if(pod.getSpec() == null)
                continue;
            if(Utils.isEmpty(pod.getSpec().getVolumes()))
                continue;
            NamespacedRef podRef = KubeUtil.getNamespacedRef(pod.getMetadata());

            result.addAll(
                    pod.getSpec().getVolumes().stream().map(
                            v -> new PodVolume(
                                    new PodVolumeId(
                                            podRef,
                                            v.getName()
                                    ),
                                    v
                            )
                    ).toList()
            );
        }

        return result;
    }

    @Override
    public Optional<PodVolume> describePodVolume(PodVolumeId podVolumeId){
        Optional<V1Pod> pod = describePod(podVolumeId.podRef());
        if(pod.isEmpty())
            return Optional.empty();

        V1PodSpec spec = pod.get().getSpec();
        if(spec == null)
            return Optional.empty();
        List<V1Volume> volumes = spec.getVolumes();
        if(Utils.isEmpty(volumes))
            return Optional.empty();

        return volumes.stream().filter(
                v -> Objects.equals(v.getName(), podVolumeId.volumeName())
        ).map(
                v -> new PodVolume(podVolumeId, v)
        ).findAny();
    }


    public Optional<NodeMetricsList> describeNodeMetricsList(){
        try {
            NodeMetricsList result = CacheUtil.queryWithCache(
                    cacheService,
                    "K8s-NodeMetricsList-" + kubeConfig.getServer(),
                    15L,
                    () -> tryInvoke(
                            () -> buildMetricsApi().getNodeMetrics()
                    ),
                    new NodeMetricsList()
            );
            return Optional.ofNullable(result);
        }catch (Exception e){
            log.warn("Failed to describe node metrics: {}", e.toString());
            return Optional.empty();
        }
    }

    @Override
    public Optional<NodeMetrics> describeNodeMetrics(String nodeName){
        Optional<NodeMetricsList> metricsList = describeNodeMetricsList();

        if(metricsList.isEmpty())
            return Optional.empty();

        List<NodeMetrics> items = metricsList.get().getItems();

        if(Utils.isEmpty(items))
            return Optional.empty();

        return items.stream().filter(
                m -> m.getMetadata() != null && Objects.equals(nodeName, m.getMetadata().getName())
        ).findAny();
    }

    public Optional<PodMetricsList> describePodMetricsList(String namespace){
        try {
            PodMetricsList result = CacheUtil.queryWithCache(
                    cacheService,
                    "K8s-PodMetricsList-" + kubeConfig.getServer(),
                    15L,
                    () -> tryInvoke(
                            () -> buildMetricsApi().getPodMetrics(namespace)
                    ),
                    new PodMetricsList()
            );
            return Optional.ofNullable(result);
        }catch (Exception e){
            log.warn("Failed to describe pod metrics: {}", e.toString());
            return Optional.empty();
        }
    }

    @Override
    public Optional<PodMetrics> describePodMetrics(NamespacedRef podRef){
        Optional<PodMetricsList> metricsList = describePodMetricsList(podRef.namespace());

        if(metricsList.isEmpty())
            return Optional.empty();

        List<PodMetrics> items = metricsList.get().getItems();

        if(Utils.isEmpty(items))
            return Optional.empty();

        return items.stream().filter(
                m -> m.getMetadata() != null && Objects.equals(m.getMetadata().getName(), podRef.name())
        ).findAny();
    }
}
