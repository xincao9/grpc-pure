package fun.golinks.grpc.pure.constant;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class SystemConsts {

    public static final StatusRuntimeException STATUS_RUNTIME_EXCEPTION = new StatusRuntimeException(Status.CANCELLED);
}
