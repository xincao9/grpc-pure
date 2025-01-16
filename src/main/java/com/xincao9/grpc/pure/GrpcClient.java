package com.xincao9.grpc.pure;

import com.xincao9.grpc.Infra;
import com.xincao9.grpc.ExtendGrpc;
import io.grpc.*;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.InternalClientCalls;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 缓存客户端，ping心跳包活
 */
@Slf4j
public class GrpcClient {

    private static final String RPC_CLIENT_PING_SCHEDULED_NAME = "rpc-client-ping-scheduled";
    private static final int INITIAL_DELAY_MILLISECONDS = 3000;
    private static final int PERIOD_MILLISECONDS = 3000;
    private static final int THREAD_POOL_SIZE = 4;
    private static final String PING_EXECUTOR_NAME = "ping-executor";
    private static final String INTERNAL_STUB_TYPE = "internal-stub-type";
    private final ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor(r -> new Thread(r, RPC_CLIENT_PING_SCHEDULED_NAME));
    private final ExecutorService pingExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE,
            r -> new Thread(r, PING_EXECUTOR_NAME));
    private final Map<String, ManagedChannel> managedChannelMap = new ConcurrentHashMap<>();
    private final NameResolverProvider nameResolverProvider;
    private final LoadBalancerProvider loadBalancerProvider;
    private final Boolean enablePing;

    private GrpcClient(NameResolverProvider nameResolverProvider, LoadBalancerProvider loadBalancerProvider, Boolean enablePing) {
        this.nameResolverProvider = nameResolverProvider;
        this.loadBalancerProvider = loadBalancerProvider;
        this.enablePing = enablePing;
        pingStart();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private void pingStart() {
        if (!enablePing) {
            return;
        }
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, ManagedChannel> entry : managedChannelMap.entrySet()) {
                String target = entry.getKey();
                ManagedChannel channel = entry.getValue();
                ping(target, channel);
            }
        }, INITIAL_DELAY_MILLISECONDS, PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    private void ping(String target, ManagedChannel channel) {
        ClientCall<Infra.Empty, Infra.Empty> call = channel.newCall(ExtendGrpc.getPingMethod(),
                CallOptions.DEFAULT.withExecutor(pingExecutor).withDeadlineAfter(3000, TimeUnit.MILLISECONDS)
                        .withOption(CallOptions.Key.create(INTERNAL_STUB_TYPE), InternalClientCalls.StubType.BLOCKING));
        try {
            ClientCalls.blockingUnaryCall(call, Infra.Empty.newBuilder().build());
            log.debug("心跳 {} 成功", target);
        } catch (Throwable e) {
            log.debug("ping", e);
        }
    }

    /**
     * 创建客户端
     */
    public ManagedChannel create(String target) {
        if (managedChannelMap.containsKey(target)) {
            return managedChannelMap.get(target);
        }
        ManagedChannelBuilder managedChannelBuilder = createManagedChannelBuilder(target);
        ManagedChannel managedChannel = managedChannelBuilder.build();
        managedChannelMap.put(target, managedChannel);
        return managedChannel;
    }

    public ManagedChannelBuilder createManagedChannelBuilder(String target) {
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

        public GrpcClient build() throws Throwable {
            return new GrpcClient(nameResolverProvider, loadBalancerProvider, enablePing);
        }
    }
}
