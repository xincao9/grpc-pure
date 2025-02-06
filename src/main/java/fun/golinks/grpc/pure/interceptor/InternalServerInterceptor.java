package fun.golinks.grpc.pure.interceptor;

import fun.golinks.grpc.pure.consts.SystemConsts;
import fun.golinks.grpc.pure.util.TraceUtils;
import fun.golinks.grpc.pure.util.ThrowableUtils;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InternalServerInterceptor implements ServerInterceptor {

    private static final String LOGGER_MESSAGE_FORMAT = "[{}] - performance cost: {} milliseconds";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String traceId = headers.get(SystemConsts.TRACE_ID_KEY);
        TraceUtils.setTraceId(traceId);
        AtomicBoolean run = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        String methodName = call.getMethodDescriptor().getFullMethodName();
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        if (run.compareAndSet(false, true)) {
                            long costTime = System.currentTimeMillis() - startTime;
                            if (status.isOk()) {
                                log.debug(LOGGER_MESSAGE_FORMAT, methodName, costTime);
                            } else {
                                Throwable throwable = ThrowableUtils.setCause(trailers, status);
                                if (throwable == null) {
                                    throwable = status.asException(trailers);
                                }
                                log.error(LOGGER_MESSAGE_FORMAT, methodName, costTime, throwable);
                            }
                        }
                        super.close(status, trailers);
                    }
                }, headers)) {

            @Override
            public void onCancel() {
                if (run.compareAndSet(false, true)) {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.error(LOGGER_MESSAGE_FORMAT, methodName, costTime,
                            SystemConsts.CANCELLED_STATUS_RUNTIME_EXCEPTION);
                }
                super.onCancel();
            }
        };
    }
}
