package com.xincao9.grpc.pure.discovery.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;
import java.util.Properties;

public class NacosNameResolverProvider extends NameResolverProvider {

    private static final String NACOS_SCHEME = "nacos";
    private final NamingService namingService;

    private NacosNameResolverProvider(NamingService namingService) {
        this.namingService = namingService;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        return new NacosNameResolver(namingService, targetUri);
    }

    @Override
    public String getDefaultScheme() {
        return NACOS_SCHEME;
    }

    public static class Builder {

        private String serverAddress = "localhost:8848";
        /**
         * 用户名
         */
        private String username = "nacos";
        /**
         * 密码
         */
        private String password = "nacos";

        public Builder setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public NacosNameResolverProvider build() throws NacosException {
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddress);
            properties.put(PropertyKeyConst.USERNAME, username);
            properties.put(PropertyKeyConst.PASSWORD, password);
            NamingService namingService = NacosFactory.createNamingService(properties);
            return new NacosNameResolverProvider(namingService);
        }
    }
}
