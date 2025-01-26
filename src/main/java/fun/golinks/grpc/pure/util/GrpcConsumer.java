package fun.golinks.grpc.pure.util;

import io.grpc.stub.StreamObserver;

import java.util.function.BiConsumer;

public class GrpcConsumer<Req, Resp> implements BiConsumer<Req, StreamObserver<Resp>> {

    private final GrpcFunction<Req, Resp> function;

    private GrpcConsumer(GrpcFunction<Req, Resp> function) {
        this.function = function;
    }

    public static <Req, Resp> GrpcConsumer<Req, Resp> wrap(GrpcFunction<Req, Resp> function) {
        return new GrpcConsumer<>(function);
    }

    @Override
    public void accept(Req req, StreamObserver<Resp> responseObserver) {
        try {
            Resp resp = function.apply(req);
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Throwable e) {
            responseObserver.onError(e); // onError后不应该再调用onCompleted，否则会出现各种异常
        }
    }
}
