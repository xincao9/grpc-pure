package fun.golinks.grpc.pure;

import com.alibaba.nacos.common.utils.CollectionUtils;
import fun.golinks.grpc.pure.balancer.WeightRandomLoadBalancerProvider;
import fun.golinks.grpc.pure.core.PingRunner;
import fun.golinks.grpc.pure.interceptor.InternalClientInterceptor;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 客户端
 */
@Slf4j
public class GrpcChannels {

    private final Map<String, ManagedChannel> managedChannelMap;
    private final NameResolverProvider nameResolverProvider;
    private final Set<ClientInterceptor> clientInterceptors;
    private final Executor executor;
    private final String defaultLoadBalancingPolicy;

    private GrpcChannels(NameResolverProvider nameResolverProvider, Boolean enablePing,
            Set<ClientInterceptor> clientInterceptors, Executor executor, String defaultLoadBalancingPolicy) {
        this.nameResolverProvider = nameResolverProvider;
        this.managedChannelMap = new ConcurrentHashMap<>();
        if (enablePing) {
            PingRunner pingRunner = new PingRunner(this.managedChannelMap);
            pingRunner.start();
        }
        this.clientInterceptors = clientInterceptors;
        this.executor = executor;
        this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 创建
     */
    public ManagedChannel create(String target) {
        if (managedChannelMap.containsKey(target)) {
            return managedChannelMap.get(target);
        }
        ManagedChannelBuilder<?> managedChannelBuilder = createManagedChannelBuilder(target);
        ManagedChannel managedChannel = managedChannelBuilder.build();
        managedChannelMap.put(target, managedChannel);
        return managedChannel;
    }

    public ManagedChannelBuilder<?> createManagedChannelBuilder(String target) {
        if (nameResolverProvider != null) {
            NameResolverRegistry.getDefaultRegistry().register(nameResolverProvider);
        }
        LoadBalancerRegistry.getDefaultRegistry().register(new WeightRandomLoadBalancerProvider());
        ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forTarget(target).usePlaintext();
        if (StringUtils.isNotBlank(defaultLoadBalancingPolicy)) {
            managedChannelBuilder.defaultLoadBalancingPolicy(defaultLoadBalancingPolicy);
        }
        managedChannelBuilder.intercept(new InternalClientInterceptor());
        if (CollectionUtils.isNotEmpty(clientInterceptors)) {
            clientInterceptors.forEach(managedChannelBuilder::intercept);
        }
        if (executor != null) {
            managedChannelBuilder.executor(executor);
        }
        return managedChannelBuilder;
    }

    public static class Builder {
        private Boolean enablePing = true;
        private NameResolverProvider nameResolverProvider;
        private final Set<ClientInterceptor> clientInterceptors = new HashSet<>();
        private Executor executor;
        private String defaultLoadBalancingPolicy = "weight_random";

        public Builder enablePing(Boolean enablePing) {
            this.enablePing = enablePing;
            return this;
        }

        public Builder setNameResolverProvider(NameResolverProvider nameResolverProvider) {
            this.nameResolverProvider = nameResolverProvider;
            return this;
        }

        public Builder addClientInterceptor(ClientInterceptor... clientInterceptor) {
            this.clientInterceptors.addAll(Arrays.asList(clientInterceptor));
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder setDefaultLoadBalancingPolicy(String defaultLoadBalancingPolicy) {
            this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
            return this;
        }

        public GrpcChannels build() throws Throwable {
            return new GrpcChannels(nameResolverProvider, enablePing, clientInterceptors, executor,
                    defaultLoadBalancingPolicy);
        }
    }
}
