package com.xincao9.grpc.pure.discovery.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xincao9.grpc.pure.discovery.ServerRegister;
import com.xincao9.grpc.pure.util.IpUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class NacosServerRegister extends ServerRegister {

    private static final Integer REGISTER_TIMER_PERIOD_SECOND = 30;
    private static final String REGISTRATION_TIME = "registration-time";
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final NamingService namingService;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private NacosServerRegister(String appName, Integer port, NamingService namingService) {
        super(appName, port);
        this.namingService = namingService;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.info("ServerRegister 已经启动了！");
            return;
        }
        if (!register()) {
            throw new RuntimeException("注册nacos失败");
        }
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.info("ServerRegister 关闭失败！");
        }
    }

    public Boolean register() {
        if (!registerInstance(appName)) {
            return false;
        }
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                List<Instance> instances = namingService.getAllInstances(appName);
                if (instances == null || instances.isEmpty()) {
                    registerInstance(appName);
                    return;
                }
                for (Instance i : instances) {
                    if (Objects.equals(i.getIp(), IpUtils.getIP()) && Objects.equals(i.getPort(), port)) {
                        return;
                    }
                }
                registerInstance(appName);
            } catch (Throwable e) {
                log.error("服务注册定时任务", e);
            }
        }, REGISTER_TIMER_PERIOD_SECOND, REGISTER_TIMER_PERIOD_SECOND, TimeUnit.SECONDS);
        return true;
    }

    private Boolean registerInstance(String serverName) {
        Instance instance = createInstance();
        Throwable finalException = null;
        int retry = 3;
        while (retry > 0) {
            try {
                namingService.registerInstance(serverName, instance);
                log.info("rpc实例注册nacos成功 {} => {}:{}", serverName, instance.getIp(), instance.getPort());
                return true;
            } catch (Throwable e) {
                finalException = e;
                log.warn("rpc实例注册nacos失败", e);
            }
            retry--;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("sleep interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        log.error("rpc实例注册nacos失败", finalException);
        return false;
    }

    private Instance createInstance() {
        Instance instance = new Instance();
        instance.setIp(IpUtils.getIP());
        instance.setPort(port);
        instance.setEphemeral(true);
        instance.setWeight(1000);
        Map<String, String> metadata = new HashMap<>(1);
        metadata.put(REGISTRATION_TIME, String.valueOf(System.currentTimeMillis()));
        instance.setMetadata(metadata);
        return instance;
    }

    public static class Builder {

        private String appName = "Default";

        private Integer port = 9999;

        private String serverAddress = "127.0.0.1";

        /**
         * 用户名
         */
        private String username = "nacos";
        /**
         * 密码
         */
        private String password = "nacos";

        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder setPort(Integer port) {
            this.port = port;
            return this;
        }

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

        public NacosServerRegister build() throws NacosException {
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddress);
            properties.put(PropertyKeyConst.USERNAME, username);
            properties.put(PropertyKeyConst.PASSWORD, password);
            NamingService namingService = NacosFactory.createNamingService(properties);
            return new NacosServerRegister(appName, port, namingService);
        }
    }
}
