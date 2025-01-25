package fun.golinks.grpc.pure.balancer;

import fun.golinks.grpc.pure.constant.SystemConsts;
import io.grpc.*;

import java.util.List;
import java.util.stream.Collectors;

public class WeightRoundRobinLoadBalancer extends LoadBalancer {
    private final Helper helper;
    private SubchannelPicker currentPicker;

    public WeightRoundRobinLoadBalancer(Helper helper) {
        this.helper = helper;
    }

    @Override
    public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
        List<EquivalentAddressGroup> addresses = resolvedAddresses.getAddresses();
        if (addresses.isEmpty()) {
            handleNameResolutionError(Status.UNAVAILABLE.withDescription("No servers found"));
            return;
        }
        // 创建 Subchannels
        List<Subchannel> subchannels = addresses.stream()
                .map(equivalentAddressGroup -> helper.createSubchannel(
                        CreateSubchannelArgs.newBuilder().setAddresses(equivalentAddressGroup).build()))
                .collect(Collectors.toList());
        // 更新当前 picker
        currentPicker = new WeightRoundRobinPicker(subchannels);
        helper.updateBalancingState(ConnectivityState.READY, currentPicker);
    }

    @Override
    public void handleNameResolutionError(Status error) {
        helper.updateBalancingState(ConnectivityState.TRANSIENT_FAILURE,
                new FixedResultPicker(PickResult.withError(error)));
    }

    @Override
    public void shutdown() {
        currentPicker = null;
    }

    private static class WeightRoundRobinPicker extends SubchannelPicker {
        private final List<Subchannel> subchannels;
        private final List<Double> weights;

        WeightRoundRobinPicker(List<Subchannel> subchannels) {
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
            double randomPoint = Math.random() * totalWeight;

            for (int i = 0; i < size; i++) {
                randomPoint -= weights.get(i);
                if (randomPoint <= 0) {
                    return PickResult.withSubchannel(subchannels.get(i));
                }
            }
            return PickResult.withSubchannel(subchannels.get(0));
        }
    }
}