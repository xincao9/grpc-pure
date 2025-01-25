package fun.golinks.grpc.pure.constant;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class SystemConsts {

    public static final StatusRuntimeException STATUS_RUNTIME_EXCEPTION = new StatusRuntimeException(Status.CANCELLED);

    public static final String TRACE_ID = "trace-id";

    public static final Metadata.Key<String> TRACE_ID_KEY = Metadata.Key.of(TRACE_ID, Metadata.ASCII_STRING_MARSHALLER);

    public static final String REGISTRATION_TIME_PROPS = "registration-time";
    public static final Attributes.Key<Long> REGISTRATION_TIME_ATTRIBUTE = Attributes.Key
            .create(REGISTRATION_TIME_PROPS);

    public static final String WEIGHT_PROPS = "weight";
    public static final Attributes.Key<Double> WEIGHT_ATTRIBUTE = Attributes.Key
            .create(WEIGHT_PROPS);
}
