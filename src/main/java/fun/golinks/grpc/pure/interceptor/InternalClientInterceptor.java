package fun.golinks.grpc.pure.interceptor;

import fun.golinks.grpc.pure.constant.SystemConsts;
import fun.golinks.grpc.pure.util.GrpcMDC;
import fun.golinks.grpc.pure.util.GrpcUtils;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InternalClientInterceptor implements ClientInterceptor {

    private static final String LOGGER_MESSAGE_FORMAT = "[{}] - invocation performance cost: {} milliseconds";

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        AtomicBoolean run = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        String traceId = GrpcMDC.getTraceId();
        String methodName = method.getFullMethodName();
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(SystemConsts.TRACE_ID_KEY, traceId);
                super.start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                            @Override
                            public void onHeaders(Metadata headers) {
                                super.onHeaders(headers);
                            }

                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                long costTime = System.currentTimeMillis() - startTime;
                                if (run.compareAndSet(false, true)) {
                                    if (status.isOk()) {
                                        log.debug(LOGGER_MESSAGE_FORMAT, methodName, costTime);
                                    } else {
                                        Throwable throwable = GrpcUtils.parseCause(status, trailers);
                                        if (throwable == null) {
                                            throwable = status.asException(trailers);
                                        }
                                        log.error(LOGGER_MESSAGE_FORMAT, methodName, costTime, throwable);
                                    }
                                }
                                super.onClose(status, trailers);
                            }
                        }, headers);
            }

            @Override
            public void cancel(@Nullable String message, @Nullable Throwable cause) {
                if (run.compareAndSet(false, true)) {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.error(LOGGER_MESSAGE_FORMAT, methodName, costTime, cause);
                }
                super.cancel(message, cause);
            }
        };
    }
}