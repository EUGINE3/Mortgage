package com.bank.mortgage.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class TraceUtilTest {

    private TraceUtil traceUtil;

    @BeforeEach
    void setUp() {
        traceUtil = new TraceUtil();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void setTraceId_shouldGenerateIdWhenMissing() {
        traceUtil.setTraceId();

        String traceId = traceUtil.getTraceId();
        assertThat(traceId).isNotNull();
        assertThat(traceId).isNotBlank();
    }

    @Test
    void setTraceId_shouldNotOverwriteExistingId() {
        MDC.put("traceId", "existing-trace");

        traceUtil.setTraceId();

        assertThat(traceUtil.getTraceId()).isEqualTo("existing-trace");
    }

    @Test
    void clearTraceId_shouldRemoveFromMdc() {
        traceUtil.setTraceId();
        assertThat(traceUtil.getTraceId()).isNotNull();

        traceUtil.clearTraceId();

        assertThat(traceUtil.getTraceId()).isNull();
    }

    @Test
    void getTraceId_whenNotSet_shouldReturnNull() {
        assertThat(traceUtil.getTraceId()).isNull();
    }
}
