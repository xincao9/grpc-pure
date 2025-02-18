package fun.golinks.grpc.pure.discovery.nacos;

import com.alibaba.nacos.api.naming.NamingService;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;
import java.util.Objects;

public class NacosNameResolverProvider extends NameResolverProvider {

    private static final String UP = "UP";
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
        return Objects.equals(namingService.getServerStatus(), UP);
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

        private Builder() {
        }

        private NacosNamingService nacosNamingService;

        public Builder setNacosNamingService(NacosNamingService nacosNamingService) {
            this.nacosNamingService = nacosNamingService;
            return this;
        }

        public NacosNameResolverProvider build() {
            return new NacosNameResolverProvider(nacosNamingService.getNamingService());
        }
    }
}
