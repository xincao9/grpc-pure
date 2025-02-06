package fun.golinks.grpc.pure.util;

public class GrpcInvoker<Req, Resp> implements GrpcFunction<Req, Resp> {

    private final GrpcFunction<Req, Resp> function;

    private GrpcInvoker(GrpcFunction<Req, Resp> function) {
        this.function = function;
    }

    public static <Req, Resp> GrpcInvoker<Req, Resp> wrap(GrpcFunction<Req, Resp> function) {
        return new GrpcInvoker<>(function);
    }

    @Override
    public Resp apply(Req req) throws Throwable {
        try {
            return function.apply(req);
        } catch (Throwable e) {
            throw ThrowableUtils.parseCause(e);
        }
    }
}
