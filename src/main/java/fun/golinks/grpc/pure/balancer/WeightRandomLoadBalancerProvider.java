package fun.golinks.grpc.pure.balancer;

import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;

public class WeightRandomLoadBalancerProvider extends LoadBalancerProvider {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
        return new WeightRandomLoadBalancer(helper);
    }

    @Override
    public String getPolicyName() {
        return "weight_random";
    }
}