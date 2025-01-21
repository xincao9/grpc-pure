package com.xincao9.grpc.pure.discovery.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
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
        String serviceName = targetUri.getAuthority();
        try {
            init(listener, serviceName);
        } catch (Throwable e) {
            log.error("", e);
        }
        try {
            namingService.subscribe(serviceName, event -> {
                changeHandler(listener, (NamingEvent) event);
            });
        } catch (Throwable e) {
            log.error("", e);
        }
    }

    /**
     * 创建客户端时初始化实例列表
     */
    private void init(Listener2 listener, String serviceName) throws NacosException {
        List<EquivalentAddressGroup> equivalentAddressGroups = new ArrayList<>();
        List<Instance> instances = namingService.getAllInstances(serviceName);
        for (Instance instance : instances) {
            if (!instance.isEnabled() || !instance.isHealthy()) {
                continue;
            }
            equivalentAddressGroups
                    .add(new EquivalentAddressGroup(new InetSocketAddress(instance.getIp(), instance.getPort())));
        }
        listener.onResult(ResolutionResult.newBuilder().setAddresses(equivalentAddressGroups).build());
    }

    /**
     * 实例列表变更本地缓存
     */
    private void changeHandler(Listener2 listener, NamingEvent namingEvent) {
        List<EquivalentAddressGroup> equivalentAddressGroups = new ArrayList<>();
        try {
            List<Instance> instances = namingService.getAllInstances(namingEvent.getServiceName());
            if (instances == null || instances.isEmpty()) {
                listener.onResult(ResolutionResult.newBuilder().setAddresses(equivalentAddressGroups).build());
                return;
            }
            for (Instance instance : instances) {
                if (!instance.isEnabled() || !instance.isHealthy()) {
                    continue;
                }
                equivalentAddressGroups
                        .add(new EquivalentAddressGroup(new InetSocketAddress(instance.getIp(), instance.getPort())));
            }
            listener.onResult(ResolutionResult.newBuilder().setAddresses(equivalentAddressGroups).build());
        } catch (Throwable e) {
            log.error("namingService.getAllInstances {}", namingEvent.getServiceName(), e);
        }
    }

    @Override
    public String getServiceAuthority() {
        return targetUri.getAuthority();
    }

    @Override
    public void shutdown() {
    }
}
