package fun.golinks.grpc.pure.interceptor;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class LoggerServerInterceptor implements ServerInterceptor {

    private static final StatusRuntimeException STATUS_RUNTIME_EXCEPTION = new StatusRuntimeException(Status.CANCELLED);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        AtomicBoolean run = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        if (run.compareAndSet(false, true)) {
                            long costTime = System.currentTimeMillis() - startTime;
                            if (status.isOk()) {
                                log.info("grpc method: {} perform cost time {} ms",
                                        call.getMethodDescriptor().getFullMethodName(), costTime);
                            } else {
                                log.error("grpc method: {} perform cost time {} ms",
                                        call.getMethodDescriptor().getFullMethodName(), costTime,
                                        status.asRuntimeException());
                            }
                        }
                        super.close(status, trailers);
                    }
                }, headers)) {

            @Override
            public void onCancel() {
                if (run.compareAndSet(false, true)) {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.error("grpc method: {} perform cost time {} ms", call.getMethodDescriptor().getFullMethodName(),
                            costTime, STATUS_RUNTIME_EXCEPTION);
                }
                super.onCancel();
            }
        };
    }
}
