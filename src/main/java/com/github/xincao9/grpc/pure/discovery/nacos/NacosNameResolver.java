package com.github.xincao9.grpc.pure.discovery.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.StatusOr;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class NacosNameResolver extends NameResolver {

    private static final String REGISTRATION_TIME_PROPS = "registration-time";
    private static final Attributes.Key<Long> REGISTRATION_TIME_ATTRIBUTE = Attributes.Key
            .create(REGISTRATION_TIME_PROPS);
    private final NamingService namingService;
    private final URI targetUri;

    public NacosNameResolver(NamingService namingService, URI targetUri) {
        this.namingService = namingService;
        this.targetUri = targetUri;
    }

    @Override
    public void start(Listener2 listener) {
        String serviceName = getServiceAuthority();
        onResult(listener, serviceName);
        try {
            namingService.subscribe(serviceName, event -> {
                changeHandler(listener, (NamingEvent) event);
            });
        } catch (Throwable e) {
            log.error("namingService.subscribe", e);
        }
    }

    private void onResult(Listener2 listener, String serviceName) {
        List<EquivalentAddressGroup> equivalentAddressGroups = new ArrayList<>();
        List<Instance> instances = null;
        try {
            instances = namingService.getAllInstances(serviceName);
        } catch (NacosException e) {
            log.error("namingService.getAllInstances {}", serviceName, e);
            return;
        }
        for (Instance instance : instances) {
            if (!instance.isEnabled() || !instance.isHealthy()) {
                continue;
            }
            Attributes attributes = Attributes.EMPTY;
            Map<String, String> metadata = instance.getMetadata();
            if (metadata != null && !metadata.isEmpty()) {
                Attributes.Builder builder = Attributes.newBuilder();
                String value = metadata.get(REGISTRATION_TIME_PROPS);
                if (StringUtils.isNotBlank(value)) {
                    builder.set(REGISTRATION_TIME_ATTRIBUTE, Long.parseLong(value));
                }
                attributes = builder.build();
            }
            equivalentAddressGroups.add(new EquivalentAddressGroup(
                    new InetSocketAddress(instance.getIp(), instance.getPort()), attributes));
        }
        if (equivalentAddressGroups.isEmpty()) {
            return;
        }
        listener.onResult(
                ResolutionResult.newBuilder().setAddressesOrError(StatusOr.fromValue(equivalentAddressGroups)).build());
    }

    /**
     * 实例列表变更本地缓存
     */
    private void changeHandler(Listener2 listener, NamingEvent namingEvent) {
        String serviceName = namingEvent.getServiceName();
        onResult(listener, serviceName);
    }

    @Override
    public String getServiceAuthority() {
        return targetUri.getAuthority();
    }

    @Override
    public void shutdown() {
    }
}
