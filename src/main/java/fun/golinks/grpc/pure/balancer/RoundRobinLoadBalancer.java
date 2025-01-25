package fun.golinks.grpc.pure.balancer;

import io.grpc.ConnectivityState;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Status;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RoundRobinLoadBalancer extends LoadBalancer {
    private final Helper helper;
    private SubchannelPicker currentPicker;

    public RoundRobinLoadBalancer(Helper helper) {
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
                .map(equivalentAddressGroup -> helper.createSubchannel(CreateSubchannelArgs.newBuilder().setAddresses(equivalentAddressGroup).build()))
                .collect(Collectors.toList());
        // 更新当前 picker
        currentPicker = new RoundRobinPicker(subchannels);
        helper.updateBalancingState(ConnectivityState.READY, currentPicker);
    }

    @Override
    public void handleNameResolutionError(Status error) {
        helper.updateBalancingState(ConnectivityState.TRANSIENT_FAILURE, new FixedResultPicker(PickResult.withError(error)));
    }

    @Override
    public void shutdown() {
        currentPicker = null;
    }

    private static class RoundRobinPicker extends SubchannelPicker {
        private final List<Subchannel> subchannels;
        private final AtomicInteger currentIndex = new AtomicInteger(0);

        RoundRobinPicker(List<Subchannel> subchannels) {
            this.subchannels = subchannels;
        }

        @Override
        public PickResult pickSubchannel(PickSubchannelArgs args) {
            if (subchannels.isEmpty()) {
                return PickResult.withNoResult();
            }
            int index = currentIndex.getAndUpdate(i -> (i + 1) % subchannels.size());
            Subchannel subchannel = subchannels.get(index);
            return PickResult.withSubchannel(subchannel);
        }
    }
}