package fun.golinks.grpc.pure.util;

public interface GrpcFunction<T, R> {

    R apply(T t) throws Throwable;
}
