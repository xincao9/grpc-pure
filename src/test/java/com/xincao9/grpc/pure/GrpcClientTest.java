package com.xincao9.grpc.pure;

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

    @BeforeClass
    public static void setUp() throws Throwable {
        GrpcServer.newBuilder().setPort(9999).addService(new GreeterImpl()).build();
    }

    @AfterClass
    public static void shutdown() throws Throwable {
    }

    @Test
    public void testCreate() throws Throwable {
        GrpcClient grpcClient = GrpcClient.newBuilder().build();
        ManagedChannel managedChannel = grpcClient.create("127.0.0.1:9999");
        GreeterGrpc.GreeterBlockingStub greeterBlockingStub = GreeterGrpc.newBlockingStub(managedChannel);
        for (int i = 0; i < 100; i++) {
            try {
                HelloReply helloReply = greeterBlockingStub.withDeadlineAfter(100, TimeUnit.MILLISECONDS)
                        .sayHello(HelloRequest.newBuilder().setName(RandomStringUtils.randomAlphabetic(32)).build());
                log.info("helloReply = {}", helloReply);
            } catch (Throwable e) {
                log.error("", e);
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
