package fun.golinks.grpc.pure;

import fun.golinks.grpc.pure.discovery.ServerRegister;
import fun.golinks.grpc.pure.interceptor.InternalServerInterceptor;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcServer {

    private static final int WORKER_N_THREADS = 200;
    private static final int BOSS_N_THREADS = Runtime.getRuntime().availableProcessors() * 2;
    private final ServerRegister serverRegister;
    private final Server server;

    private GrpcServer(ServerRegister serverRegister, Server server) {
        this.serverRegister = serverRegister;
        this.server = server;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void awaitTermination(Integer seconds) throws Throwable {
        if (serverRegister != null) {
            serverRegister.stop();
        }
        if (server != null) {
            server.awaitTermination(seconds, TimeUnit.SECONDS);
        }
    }

    public static class Builder {

        private final List<BindableService> services = new ArrayList<>();
        private Integer port = 9999;
        private Integer bossNThreads = BOSS_N_THREADS;
        private Integer workerNThreads = WORKER_N_THREADS;
        private ServerRegister serverRegister = null;
        private final Set<ServerInterceptor> serverInterceptors = new HashSet<>();

        public Builder addService(BindableService... service) {
            services.addAll(Arrays.asList(service));
            return this;
        }

        public Builder setPort(Integer port) {
            this.port = port;
            return this;
        }

        public Builder setBossNThreads(Integer bossNThreads) {
            this.bossNThreads = bossNThreads;
            return this;
        }

        public Builder setWorkerNThreads(Integer workerNThreads) {
            this.workerNThreads = workerNThreads;
            return this;
        }

        public Builder setServerRegister(ServerRegister serverRegister) {
            this.serverRegister = serverRegister;
            return this;
        }

        public Builder addServerInterceptor(ServerInterceptor... serverInterceptor) {
            this.serverInterceptors.addAll(Arrays.asList(serverInterceptor));
            return this;
        }

        public GrpcServer build() throws Throwable {
            NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(port)
                    .channelType(NioServerSocketChannel.class).bossEventLoopGroup(new NioEventLoopGroup(bossNThreads))
                    .workerEventLoopGroup(new NioEventLoopGroup(workerNThreads));
            if (!services.isEmpty()) {
                for (BindableService service : services) {
                    serverBuilder.addService(service);
                }
            }
            serverBuilder.addService(new ExtendGrpc.ExtendImplBase() {
                @Override
                public void ping(Infra.Empty request, StreamObserver<Infra.Empty> responseObserver) {
                    responseObserver.onNext(request);
                    responseObserver.onCompleted();
                }
            });
            serverBuilder.intercept(new InternalServerInterceptor());
            serverInterceptors.forEach(serverBuilder::intercept);
            Server server = serverBuilder.build().start();
            if (serverRegister != null) {
                serverRegister.start();
            }
            return new GrpcServer(serverRegister, server);
        }
    }
}
