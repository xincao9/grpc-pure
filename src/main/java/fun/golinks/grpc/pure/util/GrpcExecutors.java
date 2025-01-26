package fun.golinks.grpc.pure.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("all")
public class GrpcExecutors {

    /**
     * 创建线程池
     */
    public static GrpcThreadPoolExecutor newGrpcThreadPoolExecutor(String name, Integer corePoolSize,
            Integer maximumPoolSize, Long keepAliveTime, TimeUnit timeUnit, BlockingQueue<Runnable> blockingQueue,
            RejectedExecutionHandler rejectedExecutionHandler) {
        return new GrpcThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, blockingQueue,
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable runnable) {
                        return new Thread(runnable, String.format("%s-%d-executor", name, counter.getAndIncrement()));
                    }
                }, rejectedExecutionHandler);
    }
}
