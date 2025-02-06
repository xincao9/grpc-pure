package fun.golinks.grpc.pure.util;

import fun.golinks.grpc.pure.consts.SystemConsts;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TraceUtils {

    public static String getTraceId() {
        String traceId = MDC.get(SystemConsts.TRACE_ID);
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString();
            setTraceId(traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        Map<String, String> context = MDC.getCopyOfContextMap();
        if (context == null) {
            context = new HashMap<>();
        }
        context.put(SystemConsts.TRACE_ID, traceId);
        MDC.setContextMap(context);
    }

    public static void removeTraceId(String traceId) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        if (context != null) {
            context.remove(SystemConsts.TRACE_ID);
        }
    }
}
