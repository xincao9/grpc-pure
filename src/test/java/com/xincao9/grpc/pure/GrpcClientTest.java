package com.xincao9.grpc.pure;

import com.xincao9.grpc.pure.GreeterGrpc.GreeterBlockingStub;
import com.xincao9.grpc.pure.discovery.nacos.NacosNameResolverProvider;
import com.xincao9.grpc.pure.discovery.nacos.NacosServerRegister;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcClientTest {

    private static final String APP_NAME = GrpcClientTest.class.getSimpleName();

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
        GrpcClient grpcClient = GrpcClient.newBuilder().setNameResolverProvider(nacosNameResolverProvider).build();
        ManagedChannel managedChannel = grpcClient.create("nacos://" + APP_NAME);
        GreeterBlockingStub greeterBlockingStub = GreeterGrpc.newBlockingStub(managedChannel);
        HelloReply helloReply = greeterBlockingStub.withDeadlineAfter(100, TimeUnit.MILLISECONDS)
                .sayHello(HelloRequest.newBuilder().setName(RandomStringUtils.randomAlphabetic(32)).build());
        System.out.print(helloReply);
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
