package fun.golinks.grpc.pure;

import fun.golinks.grpc.pure.GreeterGrpc.GreeterBlockingStub;
import fun.golinks.grpc.pure.discovery.nacos.NacosNameResolverProvider;
import fun.golinks.grpc.pure.discovery.nacos.NacosServerRegister;
import fun.golinks.grpc.pure.util.GrpcExecutors;
import fun.golinks.grpc.pure.util.GrpcThreadPoolExecutor;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
        NacosServerRegister nacosServerRegister = NacosServerRegister.newBuilder().setAppName(APP_NAME).build();
        GrpcServer.newBuilder().setPort(9999).addService(new GreeterImpl()).setServerRegister(nacosServerRegister).build();
    }

    @AfterClass
    public static void shutdown() throws Throwable {
    }

    @Test
    public void testCreate() throws Throwable {
        NacosNameResolverProvider nacosNameResolverProvider = NacosNameResolverProvider.newBuilder().build();
        GrpcChannels grpcChannels = GrpcChannels.newBuilder().setNameResolverProvider(nacosNameResolverProvider).build();
        ManagedChannel managedChannel = grpcChannels.create("nacos://" + APP_NAME);
        GreeterBlockingStub greeterBlockingStub = GreeterGrpc.newBlockingStub(managedChannel);
        for (int i = 0; i < 100; i++) {
            try {
                HelloReply helloReply = greeterBlockingStub.withExecutor(grpcThreadPoolExecutor).withDeadlineAfter(100, TimeUnit.MILLISECONDS)
                        .sayHello(HelloRequest.newBuilder().setName(RandomStringUtils.randomAlphabetic(32)).build());
                log.info("helloReply: {}", helloReply);
            } catch (Throwable e) {
                log.error("greeter.say", e);
            }
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
