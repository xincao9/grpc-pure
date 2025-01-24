package fun.golinks.grpc.pure;

import com.alibaba.nacos.common.utils.CollectionUtils;
import fun.golinks.grpc.pure.core.PingRunner;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

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
    private final LoadBalancerProvider loadBalancerProvider;
    private final Set<ClientInterceptor> clientInterceptors;
    private final Executor executor;

    private GrpcChannels(NameResolverProvider nameResolverProvider, LoadBalancerProvider loadBalancerProvider,
            Boolean enablePing, Set<ClientInterceptor> clientInterceptors, Executor executor) {
        this.nameResolverProvider = nameResolverProvider;
        this.loadBalancerProvider = loadBalancerProvider;
        this.managedChannelMap = new ConcurrentHashMap<>();
        if (enablePing) {
            PingRunner pingRunner = new PingRunner(this.managedChannelMap);
            pingRunner.start();
        }
        this.clientInterceptors = clientInterceptors;
        this.executor = executor;
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
        if (loadBalancerProvider != null) {
            LoadBalancerRegistry.getDefaultRegistry().register(loadBalancerProvider);
        }
        ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forTarget(target).usePlaintext();
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
        private LoadBalancerProvider loadBalancerProvider;
        private Set<ClientInterceptor> clientInterceptors;
        private Executor executor;

        public Builder enablePing(Boolean enablePing) {
            this.enablePing = enablePing;
            return this;
        }

        public Builder setNameResolverProvider(NameResolverProvider nameResolverProvider) {
            this.nameResolverProvider = nameResolverProvider;
            return this;
        }

        public Builder setLoadBalancerProvider(LoadBalancerProvider loadBalancerProvider) {
            this.loadBalancerProvider = loadBalancerProvider;
            return this;
        }

        public Builder setClientInterceptors(Set<ClientInterceptor> clientInterceptors) {
            this.clientInterceptors = clientInterceptors;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public GrpcChannels build() throws Throwable {
            return new GrpcChannels(nameResolverProvider, loadBalancerProvider, enablePing, clientInterceptors,
                    executor);
        }
    }
}
