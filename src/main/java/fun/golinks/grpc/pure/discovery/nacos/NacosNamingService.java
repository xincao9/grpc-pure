package fun.golinks.grpc.pure.discovery.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import lombok.Getter;

import java.util.Properties;

@Getter
public class NacosNamingService {

    private final NamingService namingService;

    private NacosNamingService(NamingService namingService) {
        this.namingService = namingService;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Builder() {
        }

        /**
         * nacos地址
         */
        private String serverAddress = "localhost:8848";

        /**
         * 用户名
         */
        private String username = "nacos";

        /**
         * 密码
         */
        private String password = "nacos";

        /**
         * 命名空间
         */
        private String namespace = "public";

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

        public Builder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public NacosNamingService build() throws NacosException {
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddress);
            properties.put(PropertyKeyConst.USERNAME, username);
            properties.put(PropertyKeyConst.PASSWORD, password);
            properties.put(PropertyKeyConst.NAMESPACE, namespace);
            NamingService namingService = NacosFactory.createNamingService(properties);
            return new NacosNamingService(namingService);
        }
    }
}
