package fun.golinks.grpc.pure.util;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("all")
@Slf4j
public class EnhanceThreadPoolExecutor extends ThreadPoolExecutor {

    public EnhanceThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public EnhanceThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public EnhanceThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public EnhanceThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(TtlRunnable.get(task));
    }

    @Override
    public void execute(Runnable command) {
        super.execute(TtlRunnable.get(command));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(TtlRunnable.get(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(TtlCallable.get(task));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return super.invokeAny(wrap(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return super.invokeAny(wrap(tasks), timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return super.invokeAll(wrap(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return super.invokeAll(wrap(tasks), timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return tasks;
        }
        return tasks.stream().map((Function<Callable<T>, TtlCallable<T>>) callable -> {
            if (callable instanceof TtlCallable) {
                return (TtlCallable) callable;
            }
            return TtlCallable.get(callable);
        }).collect(Collectors.toList());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t != null) {
            log.error("thread pool", t);
        }
    }
}
