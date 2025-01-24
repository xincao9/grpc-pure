package fun.golinks.grpc.pure.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcCall {

    /**
     * 应用名
     */
    String appName();

    /**
     * 直连
     */
    boolean direct() default false;

    /**
     * 地址（直连时才有意义）
     */
    String address() default "127.0.0.1:9999";
}