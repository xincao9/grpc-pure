package fun.golinks.grpc.pure.core;

import fun.golinks.grpc.pure.ExtendGrpc;
import fun.golinks.grpc.pure.Infra;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.InternalClientCalls;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 心跳任务
 */
@Slf4j
public class PingRunner {

    private static final int INITIAL_DELAY_MILLISECONDS = 3000;
    private static final int PERIOD_MILLISECONDS = 3000;
    private static final String INTERNAL_STUB_TYPE = "internal-stub-type";
    private static final int THREAD_POOL_SIZE = 4;
    private static final String PING_EXECUTOR_NAME = "ping-executor";
    private static final String RPC_CLIENT_PING_SCHEDULED_NAME = "rpc-client-ping-scheduled";
    private final Map<String, ManagedChannel> managedChannelMap;

    private final ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor(r -> new Thread(r, RPC_CLIENT_PING_SCHEDULED_NAME));

    private final ExecutorService pingExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE,
            r -> new Thread(r, PING_EXECUTOR_NAME));

    public PingRunner(Map<String, ManagedChannel> managedChannelMap) {
        this.managedChannelMap = managedChannelMap;
    }

    /**
     * 启动任务
     */
    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, ManagedChannel> entry : managedChannelMap.entrySet()) {
                String target = entry.getKey();
                ManagedChannel channel = entry.getValue();
                ping(target, channel);
            }
        }, INITIAL_DELAY_MILLISECONDS, PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    /**
     * 发送心跳
     */
    private void ping(String target, ManagedChannel channel) {
        ClientCall<Infra.Empty, Infra.Empty> call = channel.newCall(ExtendGrpc.getPingMethod(),
                CallOptions.DEFAULT.withExecutor(pingExecutor).withDeadlineAfter(3000, TimeUnit.MILLISECONDS)
                        .withOption(CallOptions.Key.create(INTERNAL_STUB_TYPE), InternalClientCalls.StubType.BLOCKING));
        try {
            ClientCalls.blockingUnaryCall(call, Infra.Empty.newBuilder().build());
            log.debug("heartbeat {} success!", target);
        } catch (Throwable e) {
            log.debug("ping", e);
        }
    }
}
