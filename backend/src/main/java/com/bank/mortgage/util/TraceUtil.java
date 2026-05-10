package com.bank.mortgage.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TraceUtil {

    private static final String TRACE_ID = "traceId";

    public void setTraceId() {
        if (MDC.get(TRACE_ID) == null) {
            MDC.put(TRACE_ID, UUID.randomUUID().toString());
        }
    }

    public String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public void clearTraceId() {
        MDC.remove(TRACE_ID);
    }
}
