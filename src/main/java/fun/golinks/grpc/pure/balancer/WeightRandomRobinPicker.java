package fun.golinks.grpc.pure.balancer;

import fun.golinks.grpc.pure.consts.SystemConsts;
import io.grpc.Attributes;
import io.grpc.LoadBalancer;

import java.util.ArrayList;
import java.util.List;

public class WeightRandomRobinPicker extends LoadBalancer.SubchannelPicker {

    private final List<LoadBalancer.Subchannel> subchannels;
    private final List<Double> weights;
    private final List<Long> registrationTimes;

    public WeightRandomRobinPicker(List<LoadBalancer.Subchannel> subchannels) {
        this.subchannels = subchannels;
        this.weights = new ArrayList<>(subchannels.size());
        this.registrationTimes = new ArrayList<>(subchannels.size());
        for (LoadBalancer.Subchannel subchannel : subchannels) {
            Attributes attributes = subchannel.getAttributes();
            Double weight = null;
            if (attributes != null) {
                weight = attributes.get(SystemConsts.WEIGHT_ATTRIBUTE);
            }
            if (weight == null) {
                weight = 1000.0;
            }
            weights.add(weight);
            Long registrationTime = null;
            if (attributes != null) {
                registrationTime = attributes.get(SystemConsts.REGISTRATION_TIME_ATTRIBUTE);
            }
            if (registrationTime == null) {
                registrationTime = 0L;
            }
            registrationTimes.add(registrationTime);
        }
    }

    /**
     * 根据实例的uptime时间，对启动时长较短的实例进行惩罚
     * 
     * @param registrationTime
     *            注册时间
     * @param weight
     *            初始权重
     * 
     * @return
     */
    private Double calculate(Long registrationTime, Double weight) {
        long uptime = System.currentTimeMillis() - registrationTime;
        if (uptime > 0 && uptime < SystemConsts.WARMUP_TIME_MS) {
            weight *= (double) (uptime / SystemConsts.WARMUP_TIME_MS);
        }
        return weight;
    }

    @Override
    public LoadBalancer.PickResult pickSubchannel(LoadBalancer.PickSubchannelArgs args) {
        if (subchannels.isEmpty()) {
            return LoadBalancer.PickResult.withNoResult();
        }
        int size = subchannels.size();
        List<Double> newWeights = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            long registrationTime = registrationTimes.get(i);
            double weight = weights.get(i);
            newWeights.add(calculate(registrationTime, weight));
        }
        double totalWeight = newWeights.stream().reduce(0.0, Double::sum);
        if (totalWeight <= 0) {
            return LoadBalancer.PickResult.withSubchannel(subchannels.get(0));
        }
        double randomPoint = Math.random() * totalWeight;
        for (int i = 0; i < size; i++) {
            randomPoint -= newWeights.get(i);
            if (randomPoint <= 0) {
                LoadBalancer.Subchannel subchannel = subchannels.get(i);
                return LoadBalancer.PickResult.withSubchannel(subchannel);
            }
        }
        return LoadBalancer.PickResult.withSubchannel(subchannels.get(0));
    }
}
