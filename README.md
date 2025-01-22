# Grpc-Pure

Grpc作为优秀的开源框架受到大厂的青睐，但是对于小企业来说，不具备进行扩展的能力。有幸本人曾就职过一家互联网公司做过大规模Grpc服务的实战落地项目。所以将以往经验沉淀到这个项目中提供给需要的人使用

## 提供的能力点

1. 服务端添加对注册中心的插座，方便添加注册中心；目前内置对nacos的支持
2. 客户端添加ping机制
3. 客户端添加对注册中心的插座，方便添加服务发现；目前内置对nacos://{服务名} 协议的支持

## 代码示例

演示代码依赖nacos-server，请自行安装 [nacos-server](https://nacos.io/docs/v2.3/quickstart/quick-start/?spm=5238cd80.6a33be36.0.0.378b1e5dQqZvG2)

### 添加依赖

```xml

<dependency>
    <groupId>fun.golinks</groupId>
    <artifactId>grpc-pure</artifactId>
    <version>1.0.0</version>
</dependency>
```
#### 编译protobuf文件插件 【可选】

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

### 定义protobuf文件（greeter.proto）; 一般位于 src/main/proto目录下

```protobuf
syntax = "proto3";

package com.github.xincao9.grpc.pure;

option java_multiple_files = true;

// The greeting service definition.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

### 启动服务端

```java
import fun.golinks.grpc.pure.discovery.nacos.NacosServerRegister;
import fun.golinks.grpc.pure.GrpcServer;

public class Server {

    private static final String APP_NAME = "greeter";

    public static void main(String... args) {
        // 注册中心，服务注册
        NacosServerRegister nacosServerRegister = NacosServerRegister.newBuilder()
                .setAppName(APP_NAME) // 应用名
                .build();
        // 启动后端服务
        GrpcServer.newBuilder()
                .setPort(9999)
                .addService(new GreeterImpl()) // 添加服务实现类
                .setServerRegister(nacosServerRegister)
                .build();
    }

    /**
     * 实现Greeter服务
     */
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
```

### 启动客户端

```java
import fun.golinks.xincao9.grpc.pure.GreeterGrpc.GreeterBlockingStub;
import fun.golinks.grpc.pure.discovery.nacos.NacosNameResolverProvider;
import io.grpc.ManagedChannel;
import fun.golinks.grpc.pure.GrpcChannels;
import fun.golinks.xincao9.grpc.pure.HelloReply;
import fun.golinks.xincao9.grpc.pure.GreeterGrpc;

public class Client {

    private static final String APP_NAME = "greeter";

    public static void main(String... args) {
        // 注册中心，服务发现；对nacos:{服务名}的支持
        NacosNameResolverProvider nacosNameResolverProvider = NacosNameResolverProvider.newBuilder().build();
        GrpcChannels grpcChannels = GrpcChannels.newBuilder()
                .setNameResolverProvider(nacosNameResolverProvider)
                .build();
        // 创建channel
        ManagedChannel managedChannel = grpcChannels.create("nacos://" + APP_NAME);
        // 使用channel调用后端服务
        GreeterBlockingStub greeterBlockingStub = GreeterGrpc.newBlockingStub(managedChannel);
        HelloReply helloReply = greeterBlockingStub.withDeadlineAfter(100, TimeUnit.MILLISECONDS)
                .sayHello(HelloRequest.newBuilder().setName("grpc-pure").build());
        System.out.print(helloReply);
    }
}
```