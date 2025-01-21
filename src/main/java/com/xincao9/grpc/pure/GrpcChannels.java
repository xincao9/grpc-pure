package com.xincao9.grpc.pure;

import com.xincao9.grpc.pure.core.PingRunner;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端
 */
@Slf4j
public class GrpcChannels {

    private final Map<String, ManagedChannel> managedChannelMap;
    private final NameResolverProvider nameResolverProvider;
    private final LoadBalancerProvider loadBalancerProvider;

    private GrpcChannels(NameResolverProvider nameResolverProvider, LoadBalancerProvider loadBalancerProvider,
                         Boolean enablePing) {
        this.nameResolverProvider = nameResolverProvider;
        this.loadBalancerProvider = loadBalancerProvider;
        this.managedChannelMap = new ConcurrentHashMap<>();
        if (enablePing) {
            PingRunner pingRunner = new PingRunner(this.managedChannelMap);
            pingRunner.start();
        }
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
        return ManagedChannelBuilder.forTarget(target).usePlaintext();
    }

    public static class Builder {
        private Boolean enablePing = true;
        private NameResolverProvider nameResolverProvider;
        private LoadBalancerProvider loadBalancerProvider;

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

        public GrpcChannels build() throws Throwable {
            return new GrpcChannels(nameResolverProvider, loadBalancerProvider, enablePing);
        }
    }
}
