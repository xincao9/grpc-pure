package fun.golinks.grpc.pure;

import fun.golinks.grpc.pure.GreeterGrpc.GreeterBlockingStub;
import fun.golinks.grpc.pure.discovery.nacos.NacosNameResolverProvider;
import fun.golinks.grpc.pure.discovery.nacos.NacosServerRegister;
import fun.golinks.grpc.pure.util.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcChannelsTest {

    private static final String APP_NAME = "greeter";
    /**
     * grpc线程池
     */
    private final EnhanceThreadPoolExecutor enhanceThreadPoolExecutor = Executors.newGrpcThreadPoolExecutor("grpc-invoke",
            1,
            2,
            1L,
            TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @BeforeClass
    public static void setUp() throws Throwable {
        /**
         * 启动多个服务端节点
         */
        for (int port = 9999; port < 10000; port++) {
            setUpServer(port);
        }
    }

    /**
     * 启动服务端
     *
     * @param port 端口
     * @throws Throwable 异常
     */
    private static void setUpServer(int port) throws Throwable {
        /**
         * 服务注册器
         */
        NacosServerRegister nacosServerRegister = NacosServerRegister.newBuilder()
                .setAppName(APP_NAME)
                .setServerAddress("127.0.0.1:8848")
                .setUsername("nacos")
                .setPassword("nacos")
                .setPort(port) // 后端服务监听端口
                .build();
        /**
         * grpc服务器
         */
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
        /**
         * NameResolver；单例；解析 nacos://{应用名}
         */
        NacosNameResolverProvider nacosNameResolverProvider = NacosNameResolverProvider.newBuilder()
                .setServerAddress("127.0.0.1:8848")
                .setUsername("nacos")
                .setPassword("nacos")
                .build();
        /**
         * ManagedChannel管理类；单例
         */
        GrpcChannels grpcChannels = GrpcChannels.newBuilder()
                .setNameResolverProvider(nacosNameResolverProvider)
                .setExecutor(enhanceThreadPoolExecutor)
                .build();
        /**
         * 创建ManagedChannel；一个应用名对应一个实例
         */
        ManagedChannel managedChannel = grpcChannels.create("nacos://" + APP_NAME);
        GreeterBlockingStub greeterBlockingStub = GreeterGrpc.newBlockingStub(managedChannel);
        /**
         * 方法的包裹类；单例
         */
        GrpcInvoker<HelloRequest, HelloReply> grpcInvoker = GrpcInvoker.wrap(helloRequest -> greeterBlockingStub.withDeadlineAfter(10000, TimeUnit.MILLISECONDS)
                .sayHello(helloRequest));
        for (int i = 0; i < 100; i++) {
            try {
                /**
                 * 设置TraceId
                 */
                TraceUtils.setTraceId(String.valueOf(i));
                /**
                 * 请求体
                 */
                HelloRequest helloRequest = HelloRequest.newBuilder().setName(RandomStringUtils.randomAlphabetic(32)).build();
                /**
                 * 执行包裹类
                 */
                HelloReply helloReply = grpcInvoker.apply(helloRequest);
                log.info("helloReply: {}", helloReply.getMessage());
            } catch (GreeterException e) {
                log.info("用于测试的异常 grpc", e);
            }
        }
    }

    /**
     * 服务实现类
     */
    public static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        /**
         * 方法处理类；单例
         */
        private static final GrpcFunction<HelloRequest, HelloReply> sayHelloFunction = helloRequest -> {
            HelloReply reply = HelloReply.newBuilder()
                    .setMessage(String.format("Server:Hello %s", helloRequest.getName())).build();
            if (RandomUtils.nextInt(0, 100) == 0) {
                throw new GreeterException("随机错误");
            }
            return reply;
        };

        /**
         * 方法
         *
         * @param req 请求踢
         * @param responseObserver 响应Observer
         */
        @Override
        public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
            GrpcConsumer<HelloRequest, HelloReply> grpcConsumer = GrpcConsumer.wrap(sayHelloFunction);
            grpcConsumer.accept(req, responseObserver);
        }
    }

    public static class GreeterException extends Exception {

        public GreeterException() {
            super();
        }

        public GreeterException(String message) {
            super(message);
        }
    }
}
