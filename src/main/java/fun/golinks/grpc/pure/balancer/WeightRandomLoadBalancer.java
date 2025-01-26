package fun.golinks.grpc.pure.balancer;

import fun.golinks.grpc.pure.constant.SystemConsts;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class WeightRandomLoadBalancer extends LoadBalancer {

    private static final Attributes.Key<AtomicReference<ConnectivityState>> STATE_INFO = Attributes.Key
            .create("state-info");
    private final Helper helper;
    private final Map<SocketAddress, Subchannel> subchannelMap = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public WeightRandomLoadBalancer(Helper helper) {
        this.helper = helper;
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        List<EquivalentAddressGroup> newEquivalentAddressGroups = resolvedAddresses.getAddresses();
        if (newEquivalentAddressGroups.isEmpty()) {
            handleNameResolutionError(Status.UNAVAILABLE.withDescription("No servers found"));
            return;
        }
        Map<SocketAddress, Attributes> attributesMap = new HashMap<>();
        Set<SocketAddress> newAddresses = newEquivalentAddressGroups.stream()
                .flatMap(new Function<EquivalentAddressGroup, Stream<SocketAddress>>() {
                    @Override
                    public Stream<SocketAddress> apply(EquivalentAddressGroup equivalentAddressGroup) {
                        return equivalentAddressGroup.getAddresses().stream()
                                .map(new Function<SocketAddress, SocketAddress>() {
                                    @Override
                                    public SocketAddress apply(SocketAddress socketAddress) {
                                        attributesMap.put(socketAddress, equivalentAddressGroup.getAttributes());
                                        return socketAddress;
                                    }
                                });
                    }
                }).collect(Collectors.toSet());

        Map<SocketAddress, Subchannel> newSubchannelMap = newAddresses.stream()
                .filter(address -> !subchannelMap.containsKey(address)).map(address -> {
                    Attributes attributes = attributesMap.get(address);
                    Attributes.Builder builder = Attributes.newBuilder();
                    if (attributes != null) {
                        builder.setAll(attributes);
                    }
                    builder.set(STATE_INFO, new AtomicReference<>(ConnectivityState.IDLE));
                    Subchannel subchannel = helper.createSubchannel(CreateSubchannelArgs.newBuilder()
                            .setAddresses(new EquivalentAddressGroup(address)).setAttributes(builder.build()).build());
                    start(address, subchannel);
                    return subchannel;
                }).collect(Collectors.toConcurrentMap(new Function<Subchannel, SocketAddress>() {
                    @Override
                    public SocketAddress apply(Subchannel subchannel) {
                        return subchannel.getAddresses().getAddresses().get(0);
                    }
                }, s -> s));
        for (Map.Entry<SocketAddress, Subchannel> entry : newSubchannelMap.entrySet()) {
            SocketAddress address = entry.getKey();
            if (!subchannelMap.containsKey(address)) {
                subchannelMap.put(address, entry.getValue());
            }
        }
        for (Map.Entry<SocketAddress, Subchannel> entry : subchannelMap.entrySet()) {
            SocketAddress address = entry.getKey();
            if (!newSubchannelMap.containsKey(address)) {
                entry.getValue().shutdown();
            }
        }
    }

    private void start(SocketAddress socketAddress, Subchannel subchannel) {
        if (subchannelMap.containsKey(socketAddress)) {
            return;
        }
        subchannel.start(stateInfo -> {
            synchronized (lock) {
                AtomicReference<ConnectivityState> stateAtomicReference = subchannel.getAttributes().get(STATE_INFO);
                ConnectivityState currentState = Objects.requireNonNull(stateAtomicReference).get();
                ConnectivityState newState = stateInfo.getState();
                log.warn("subchannel socketAddress = {} currentState = {}, newState = {}", socketAddress, currentState,
                        newState);
                stateAtomicReference.set(newState);
                List<Subchannel> subchannels = subchannelMap.values().stream().filter(sc -> Objects
                        .requireNonNull(sc.getAttributes().get(STATE_INFO)).get() == ConnectivityState.READY)
                        .collect(Collectors.toList());
                /*
                 * 判断活跃连接是否位空，更新状态和Picker
                 */
                if (subchannels.isEmpty()) {
                    helper.updateBalancingState(ConnectivityState.CONNECTING,
                            new FixedResultPicker(PickResult.withNoResult()));
                } else {
                    helper.updateBalancingState(ConnectivityState.READY, new WeightRandomRobinPicker(subchannels));
                }
            }
        });
        /*
         * 建立连接
         */
        subchannel.requestConnection();
    }

    @Override
    public void handleNameResolutionError(Status error) {
        helper.updateBalancingState(ConnectivityState.TRANSIENT_FAILURE,
                new FixedResultPicker(PickResult.withError(error)));
    }

    @Override
    public void shutdown() {
        subchannelMap.forEach((socketAddress, subchannel) -> {
            log.warn("subchannel socketAddress = {} shutdown", socketAddress);
            subchannel.shutdown();
        });
    }

    private static class WeightRandomRobinPicker extends SubchannelPicker {

        private final List<Subchannel> subchannels;
        private final List<Double> weights;

        private WeightRandomRobinPicker(List<Subchannel> subchannels) {
            this.subchannels = subchannels;
            this.weights = subchannels.stream().map(subchannel -> {
                double weight = 1000.0;
                Attributes attributes = subchannel.getAttributes();
                if (attributes != null) {
                    Double attributeWeight = attributes.get(SystemConsts.WEIGHT_ATTRIBUTE);
                    if (attributeWeight != null) {
                        weight = attributeWeight;
                    }
                }
                return weight;
            }).collect(Collectors.toList());
        }

        @Override
        public PickResult pickSubchannel(PickSubchannelArgs args) {
            if (subchannels.isEmpty()) {
                return PickResult.withNoResult();
            }
            int size = subchannels.size();
            double totalWeight = weights.stream().reduce(0.0, Double::sum);
            if (totalWeight <= 0) {
                return PickResult.withSubchannel(subchannels.get(0));
            }
            double randomPoint = Math.random() * totalWeight;
            for (int i = 0; i < size; i++) {
                randomPoint -= weights.get(i);
                if (randomPoint <= 0) {
                    Subchannel subchannel = subchannels.get(i);
                    return PickResult.withSubchannel(subchannel);
                }
            }
            return PickResult.withSubchannel(subchannels.get(0));
        }
    }
}