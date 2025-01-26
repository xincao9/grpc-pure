package fun.golinks.grpc.pure;

import fun.golinks.grpc.pure.GreeterGrpc.GreeterBlockingStub;
import fun.golinks.grpc.pure.config.LogbackConfig;
import fun.golinks.grpc.pure.discovery.nacos.NacosNameResolverProvider;
import fun.golinks.grpc.pure.discovery.nacos.NacosServerRegister;
import fun.golinks.grpc.pure.interceptor.LoggerClientInterceptor;
import fun.golinks.grpc.pure.util.GrpcExecutors;
import fun.golinks.grpc.pure.util.GrpcThreadPoolExecutor;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcChannelsTest {

    private static final String APP_NAME = "greeter";
    private final GrpcThreadPoolExecutor grpcThreadPoolExecutor = GrpcExecutors.newGrpcThreadPoolExecutor("grpc-invoke",
            1,
            2,
            1L,
            TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @BeforeClass
    public static void setUp() throws Throwable {
        LogbackConfig.init();
        for (int port = 9999; port < 10002; port++) {
            setUpServer(port);
        }
    }

    private static void setUpServer(int port) throws Throwable {
        NacosServerRegister nacosServerRegister = NacosServerRegister.newBuilder()
                .setAppName(APP_NAME)
                .setServerAddress("127.0.0.1:8848")
                .setUsername("nacos")
                .setPassword("nacos")
                .setPort(port) // 后端服务监听端口
                .build();
        GrpcServer.newBuilder()
                .setPort(port)
                .addService(new GreeterImpl())
                .setServerRegister(nacosServerRegister)
                .build();
    }

    @AfterClass
    public static void shutdown() throws Throwable {
    }

    @Test
    public void testCreate() throws Throwable {
        NacosNameResolverProvider nacosNameResolverProvider = NacosNameResolverProvider.newBuilder()
                .setServerAddress("127.0.0.1:8848")
                .setUsername("nacos")
                .setPassword("nacos")
                .build();
        GrpcChannels grpcChannels = GrpcChannels.newBuilder()
                .setNameResolverProvider(nacosNameResolverProvider)
                .setExecutor(grpcThreadPoolExecutor)
                .setClientInterceptors(Collections.singleton(new LoggerClientInterceptor()))
                .build();
        ManagedChannel managedChannel = grpcChannels.create("nacos://" + APP_NAME);
        GreeterBlockingStub greeterBlockingStub = GreeterGrpc.newBlockingStub(managedChannel);
        for (int i = 0; i < 1000; i++) {
                HelloReply helloReply = greeterBlockingStub.withDeadlineAfter(10000, TimeUnit.MILLISECONDS)
                        .sayHello(HelloRequest.newBuilder().setName(RandomStringUtils.randomAlphabetic(32)).build());
                log.info("helloReply: {}", helloReply);
        }
    }

    public static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder()
                    .setMessage(String.format("Server:Hello %s", req.getName())).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

}
