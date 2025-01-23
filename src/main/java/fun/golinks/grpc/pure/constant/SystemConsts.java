package fun.golinks.grpc.pure.constant;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class SystemConsts {

    public static final StatusRuntimeException STATUS_RUNTIME_EXCEPTION = new StatusRuntimeException(Status.CANCELLED);

    public static final Metadata.Key<String> TRACE_ID = Metadata.Key.of("trace-id", Metadata.ASCII_STRING_MARSHALLER);
}
