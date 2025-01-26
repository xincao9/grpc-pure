# Grpc-Pure

## 项目简介

Grpc作为一个优秀的开源RPC框架，广泛受到互联网大厂的青睐。然而，对小型企业而言，其扩展性和复杂度可能成为障碍。有幸参与过一家互联网公司大规模Grpc服务的落地实践，因此将这些实战经验沉淀后，推出了**Grpc-Pure**，希望能为需要简化Grpc接入的小型团队提供帮助。

---

## 功能特性

1. **服务端注册中心支持**
    - 提供可插拔接口，便于接入任意注册中心。
    - 内置对 Nacos 的支持。
    - 后端异常透传客户端。

2. **客户端增强功能**
    - 内置 Ping 机制，支持连接健康检查。
    - 提供服务发现可插拔接口，轻松实现服务自动发现。
    - 内置 `nacos://{应用名}` 协议支持，实现与 Nacos 的无缝对接。
    - 添加权重随机负载均衡算法。
    - 后端节点跟随启动时间，流量平滑爬坡。

---

## 快速开始

以下示例依赖 [Nacos Server](https://nacos.io/docs/v2.3/quickstart/quick-start)，请确保已正确安装。

---

### 添加项目依赖

在 `pom.xml` 中引入如下依赖：

```xml
<dependency>
    <groupId>fun.golinks</groupId>
    <artifactId>grpc-pure</artifactId>
    <version>1.0.4</version>
</dependency>
```

---

### [可选] 配置 Protobuf 编译插件

如果需要在项目中使用 `protobuf` 文件，可添加以下 Maven 插件用于编译：

```xml
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.6.2</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:3.19.2:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.42.1:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

### 定义 Protobuf 文件

为 GRPC 定义基本的 Protobuf 文件，例如 `greeter.proto`，建议放置于 `src/main/proto` 目录下。

示例内容如下：

```protobuf
syntax = "proto3";

package fun.golinks.grpc.pure;

option java_multiple_files = true;

// 服务接口定义
service Greeter {
  // 定义 SayHello 方法
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// 请求消息结构
message HelloRequest {
  string name = 1; // 用户名称
}

// 响应消息结构
message HelloReply {
  string message = 1; // 返回消息内容
}
```

---

### 启动服务端

服务端代码示例：

```java
import fun.golinks.grpc.pure.HelloRequest;
import fun.golinks.grpc.pure.HelloReply;
import fun.golinks.grpc.pure.GreeterGrpc;
import fun.golinks.grpc.pure.GreeterGrpc.GreeterBlockingStub;
import fun.golinks.grpc.pure.discovery.nacos.NacosNameResolverProvider;
import fun.golinks.grpc.pure.discovery.nacos.NacosServerRegister;
import fun.golinks.grpc.pure.util.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class Server {

    private static final String APP_NAME = "greeter";

    public static void main(String... args) {
        int port = 9999;
       /**
        * 服务注册器
        */
        NacosServerRegister nacosServerRegister = NacosServerRegister.newBuilder()
                .setAppName(APP_NAME) // 应用名称
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
                .addService(new GreeterImpl()) // 添加服务实现类
                .setServerRegister(nacosServerRegister)
                .build();
    }

   /**
    * 服务实现类
    */
    public static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

       /**
        * 方法处理类；单例
        */
       private static final GrpcFunction<HelloRequest, HelloReply> sayHelloFunction = helloRequest -> {
          return HelloReply.newBuilder()
                  .setMessage(String.format("Server:Hello %s", helloRequest.getName())).build();
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
}
```

---

### 启动客户端

客户端代码示例：

```java
import fun.golinks.grpc.pure.HelloRequest;
import fun.golinks.grpc.pure.HelloReply;
import fun.golinks.grpc.pure.GreeterGrpc;
import fun.golinks.grpc.pure.GreeterGrpc.GreeterBlockingStub;
import fun.golinks.grpc.pure.discovery.nacos.NacosNameResolverProvider;
import fun.golinks.grpc.pure.discovery.nacos.NacosServerRegister;
import fun.golinks.grpc.pure.util.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class Client {

    private static final String APP_NAME = "greeter";

    public static void main(String... args) {
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
                .build();
       /**
        * 创建ManagedChannel；一个应用名对应一个实例
        */
        ManagedChannel managedChannel = grpcChannels.create("nacos://" + APP_NAME);
       /**
        * 方法的包裹类；单例
        */
       GrpcInvoker<HelloRequest, HelloReply> grpcInvoker = GrpcInvoker.wrap(helloRequest -> greeterBlockingStub.withDeadlineAfter(10000, TimeUnit.MILLISECONDS)
               .sayHello(helloRequest));
       /**
        * 请求体
        */
       HelloRequest helloRequest = HelloRequest.newBuilder().setName("grpc-pure").build();
       /**
        * 执行包裹类
        */
       HelloReply helloReply = grpcInvoker.apply(helloRequest);
       System.out.print(helloReply);
    }
}
```

---

至此，您已经成功完成了基于 **Grpc-Pure** 框架的简单服务端和客户端应用配置，欢迎尝试更多特性并反馈！
