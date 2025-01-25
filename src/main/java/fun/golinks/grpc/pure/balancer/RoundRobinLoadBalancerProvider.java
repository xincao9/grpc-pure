package fun.golinks.grpc.pure.balancer;

import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;

public class RoundRobinLoadBalancerProvider extends LoadBalancerProvider {

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
        return new RoundRobinLoadBalancer(helper);
    }

    @Override
    public String getPolicyName() {
        return "round_robin";
    }
}