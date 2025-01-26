package fun.golinks.grpc.pure.util;

import fun.golinks.grpc.pure.constant.SystemConsts;
import io.grpc.Metadata;
import io.grpc.Status;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

public class GrpcUtils {

    public static Throwable setCause(Metadata trailers, Status status) {
        if (status.getCode() == Status.Code.UNKNOWN) {
            Throwable throwable = status.getCause();
            if (throwable != null) {
                trailers.put(SystemConsts.EXCEPTION_STACK_TRACE_KEY, GrpcUtils.getStackTraceAsString(throwable));
            }
            return throwable;
        }
        return null;
    }

    public static Throwable parseCause(Status status, Metadata trailers) {
        if (status.getCode() != Status.Code.UNKNOWN) {
            return null;
        }
        if (trailers == null) {
            return null;
        }
        String stacktrace = trailers.get(SystemConsts.EXCEPTION_STACK_TRACE_KEY);
        if (StringUtils.isBlank(stacktrace)) {
            return null;
        }
        String[] rows = StringUtils.split(stacktrace, "\t");
        if (ArrayUtils.isEmpty(rows)) {
            return null;
        }
        String firstRow = rows[0];
        if (StringUtils.isBlank(firstRow)) {
            return null;
        }
        int index = StringUtils.indexOf(firstRow, ":");
        Throwable throwable;
        if (index < 0) {
            throwable = createCause(firstRow.trim(), StringUtils.substring(stacktrace, firstRow.length()));
        } else {
            throwable = createCause(StringUtils.substring(stacktrace, 0, index).trim(),
                    StringUtils.substring(stacktrace, index + 1));
        }
        if (throwable != null) {
            return throwable;
        }
        return null;
    }

    public static Throwable createCause(String typeName, String message) {
        Class<?> clazz;
        try {
            clazz = ClassUtils.getClass(typeName);
        } catch (ClassNotFoundException e) {
            return null;
        }
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class);
            return (Throwable) constructor.newInstance(message);
        } catch (Throwable e) {
        }
        try {
            Constructor<?> constructor = clazz.getConstructor();
            return (Throwable) constructor.newInstance();
        } catch (Throwable e) {
        }
        return null;
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
