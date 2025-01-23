package fun.golinks.grpc.pure.interceptor;

import fun.golinks.grpc.pure.constant.SystemConsts;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class LoggerClientInterceptor implements ClientInterceptor {

    private static final String LOGGER_MESSAGE_FORMAT = "grpc method: {} - invocation performance cost: {} milliseconds";

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        AtomicBoolean run = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
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
                                        log.info(LOGGER_MESSAGE_FORMAT, method.getFullMethodName(), costTime);
                                    } else {
                                        log.error(LOGGER_MESSAGE_FORMAT, method.getFullMethodName(), costTime,
                                                status.asRuntimeException());
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
                    log.error(LOGGER_MESSAGE_FORMAT, method.getFullMethodName(), costTime,
                            SystemConsts.STATUS_RUNTIME_EXCEPTION);
                }
                super.cancel(message, cause);
            }
        };
    }
}