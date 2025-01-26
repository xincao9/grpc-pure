package fun.golinks.grpc.pure.util;

import io.grpc.stub.StreamObserver;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class GrpcBiConsumer<Req, Resp> implements BiConsumer<Req, StreamObserver<Resp>> {

    private final Function<Req, Resp> function;

    private GrpcBiConsumer(Function<Req, Resp> function) {
        this.function = function;
    }

    public static <Req, Resp> GrpcBiConsumer<Req, Resp> wrap(Function<Req, Resp> function) {
        return new GrpcBiConsumer<>(function);
    }

    @Override
    public void accept(Req req, StreamObserver<Resp> responseObserver) {
        try {
            Resp resp = function.apply(req);
            responseObserver.onNext(resp);
        } catch (Throwable e) {
            responseObserver.onError(e);
        } finally {
            responseObserver.onCompleted();
        }
    }
}
