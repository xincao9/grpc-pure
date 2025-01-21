package com.xincao9.grpc.pure.discovery.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.StatusOr;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NacosNameResolver extends NameResolver {

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
            equivalentAddressGroups
                    .add(new EquivalentAddressGroup(new InetSocketAddress(instance.getIp(), instance.getPort())));
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
