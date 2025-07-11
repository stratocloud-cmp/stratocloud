package com.stratocloud.kubernetes;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.kubernetes.common.KubeUtil;
import com.stratocloud.kubernetes.common.KubernetesClient;
import com.stratocloud.kubernetes.common.KubernetesMetrics;
import com.stratocloud.kubernetes.common.NamespacedRef;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.monitor.MetricsProvider;
import com.stratocloud.provider.resource.monitor.SupportedMetric;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.alert.ExternalAlertHistory;
import com.stratocloud.resource.monitor.Metric;
import com.stratocloud.resource.monitor.MetricData;
import com.stratocloud.resource.monitor.MetricDataPoint;
import com.stratocloud.resource.monitor.MetricSequence;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class KubernetesMetricsProvider implements MetricsProvider {
    @Override
    public List<SupportedMetric> getSupportedMetrics() {
        return List.of(
                NODE_CPU_UTIL,
                NODE_MEM_UTIL,
                POD_CPU_UTIL,
                POD_MEM_UTIL
        );
    }

    @Override
    public MetricData describeMetricData(Resource resource,
                                         SupportedMetric supportedMetric,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         int periodSeconds) {
        List<MetricSequence> sequences = new ArrayList<>();

        MetricData metricData = new MetricData(supportedMetric.metric(), sequences);

        ExternalAccount account = resource.getResourceHandler().getAccountRepository().findExternalAccount(
                resource.getAccountId()
        );
        KubernetesProvider provider = (KubernetesProvider) resource.getResourceHandler().getProvider();
        KubernetesClient client = provider.buildClient(account);


        if(NODE_METRICS.contains(supportedMetric.metric())){
            Optional<NodeMetrics> nodeMetrics = client.describeNodeMetrics(resource.getExternalId());

            String quantityKey;
            if(supportedMetric.metric().equals(KubernetesMetrics.NODE_CPU_UTIL))
                quantityKey = "cpu";
            else if(supportedMetric.metric().equals(KubernetesMetrics.NODE_MEM_UTIL))
                quantityKey = "memory";
            else throw new StratoException("Unexpected metric: " + supportedMetric.metric());

            if(nodeMetrics.isEmpty() ||
                    Utils.isEmpty(nodeMetrics.get().getUsage()) ||
                    !nodeMetrics.get().getUsage().containsKey(quantityKey))
                return metricData;

            Quantity currentQuantity = nodeMetrics.get().getUsage().get(quantityKey);

            Optional<V1Node> node = client.describeNode(resource.getExternalId());
            if(node.isEmpty() ||
                    node.get().getStatus() == null ||
                    Utils.isEmpty(node.get().getStatus().getCapacity()) ||
                    !node.get().getStatus().getCapacity().containsKey(quantityKey))
                return metricData;

            Quantity totalQuantity = node.get().getStatus().getCapacity().get(quantityKey);

            double percentage = currentQuantity.getNumber().divide(
                    totalQuantity.getNumber(), 5, RoundingMode.HALF_UP
            ).multiply(
                    BigDecimal.valueOf(100L)
            ).setScale(
                    2, RoundingMode.HALF_UP
            ).doubleValue();

            LocalDateTime time = KubeUtil.toLocalDateTime(nodeMetrics.get().getTimestamp());

            MetricSequence.of(
                    resource.getName(),
                    null,
                    List.of(
                            new MetricDataPoint(percentage, time)
                    )
            ).ifPresent(sequences::add);
        }else if(POD_METRICS.contains(supportedMetric.metric())){
            NamespacedRef podRef = NamespacedRef.fromString(resource.getExternalId());
            Optional<PodMetrics> podMetrics = client.describePodMetrics(podRef);

            String quantityKey;
            if(supportedMetric.metric().equals(KubernetesMetrics.POD_CPU_UTIL))
                quantityKey = "cpu";
            else if(supportedMetric.metric().equals(KubernetesMetrics.POD_MEM_UTIL))
                quantityKey = "memory";
            else throw new StratoException("Unexpected metric: " + supportedMetric.metric());

            if(podMetrics.isEmpty() || Utils.isEmpty(podMetrics.get().getContainers()))
                return metricData;

            Optional<V1Pod> pod = client.describePod(podRef);
            if(pod.isEmpty())
                return metricData;

            for (ContainerMetrics containerMetrics : podMetrics.get().getContainers()) {
                Map<String, Quantity> containerUsage = containerMetrics.getUsage();
                if(Utils.isEmpty(containerUsage) || !containerUsage.containsKey(quantityKey))
                    continue;

                Quantity currentQuantity = containerUsage.get(quantityKey);

                V1PodSpec podSpec = pod.get().getSpec();

                if(podSpec == null || Utils.isEmpty(podSpec.getContainers()))
                    continue;

                Optional<V1Container> container = podSpec.getContainers().stream().filter(
                        c -> Objects.equals(c.getName(), containerMetrics.getName())
                ).findAny();

                if(container.isEmpty() || container.get().getResources() == null)
                    continue;

                Map<String, Quantity> containerLimits;
                if(Utils.isNotEmpty(container.get().getResources().getLimits()))
                    containerLimits = container.get().getResources().getLimits();
                else if(Utils.isNotEmpty(container.get().getResources().getRequests()))
                    containerLimits = container.get().getResources().getRequests();
                else
                    containerLimits = new HashMap<>();

                if(Utils.isEmpty(containerLimits) || !containerLimits.containsKey(quantityKey))
                    continue;

                Quantity totalQuantity = containerLimits.get(quantityKey);

                double percentage = currentQuantity.getNumber().divide(
                        totalQuantity.getNumber(), 5, RoundingMode.HALF_UP
                ).multiply(
                        BigDecimal.valueOf(100L)
                ).setScale(
                        2, RoundingMode.HALF_UP
                ).doubleValue();

                LocalDateTime time = KubeUtil.toLocalDateTime(podMetrics.get().getTimestamp());

                MetricSequence.of(
                        containerMetrics.getName(),
                        null,
                        List.of(
                                new MetricDataPoint(percentage, time)
                        )
                ).ifPresent(sequences::add);
            }
        }else throw new StratoException("Unexpected metric: " + supportedMetric.metric());


        return metricData;
    }

    @Override
    public List<ExternalAlertHistory> describeAlertHistories(Resource resource, LocalDateTime happenedAfter) {
        return List.of();
    }


    public static final SupportedMetric NODE_CPU_UTIL = new SupportedMetric(
            KubernetesMetrics.NODE_CPU_UTIL,
            "nodeName",
            r -> List.of(),
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.NODE
    );

    public static final SupportedMetric NODE_MEM_UTIL = new SupportedMetric(
            KubernetesMetrics.NODE_MEM_UTIL,
            "nodeName",
            r -> List.of(),
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.NODE
    );


    public static final SupportedMetric POD_CPU_UTIL = new SupportedMetric(
            KubernetesMetrics.POD_CPU_UTIL,
            "podName",
            r -> List.of(),
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.POD
    );

    public static final SupportedMetric POD_MEM_UTIL = new SupportedMetric(
            KubernetesMetrics.POD_MEM_UTIL,
            "podName",
            r -> List.of(),
            Optional.empty(),
            Optional.empty(),
            true,
            ResourceCategories.POD
    );


    public static final Set<Metric> NODE_METRICS = Set.of(
            KubernetesMetrics.NODE_CPU_UTIL,
            KubernetesMetrics.NODE_MEM_UTIL
    );

    public static final Set<Metric> POD_METRICS = Set.of(
            KubernetesMetrics.POD_CPU_UTIL,
            KubernetesMetrics.POD_MEM_UTIL
    );
}
