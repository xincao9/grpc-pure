package fun.golinks.grpc.pure.util;

import java.util.function.Function;

public class GrpcInvoker<Req, Resp> {

    private final Function<Req, Resp> function;

    private GrpcInvoker(Function<Req, Resp> function) {
        this.function = function;
    }

    public static <Req, Resp> GrpcInvoker<Req, Resp> wrap(Function<Req, Resp> function) {
        return new GrpcInvoker<>(function);
    }

    public Resp exec(Req req) throws Throwable {
        try {
            return function.apply(req);
        } catch (Throwable e) {
            throw GrpcUtils.parseCause(e);
        }
    }
}
