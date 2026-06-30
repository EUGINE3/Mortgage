package com.bank.mortgage.idempotency;

import com.bank.mortgage.domain.User;
import com.bank.mortgage.dto.response.ApplicationResponse;
import com.bank.mortgage.exception.IdempotencyInProgressException;
import com.bank.mortgage.exception.MissingIdempotencyKeyException;
import com.bank.mortgage.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private IdempotencyAspect idempotencyAspect;

    private User applicant;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(idempotencyAspect, "lockTtlMinutes", 2L);
        applicant = User.builder()
                .id(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .email("applicant@example.com")
                .role("APPLICANT")
                .build();

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void handleIdempotency_shouldExecuteAndCacheOnFirstRequest() throws Throwable {
        request.addHeader("X-Idempotency-Key", "create-123");
        when(securityUtil.getCurrentUser()).thenReturn(applicant);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("createApplication");
        when(methodSignature.getReturnType()).thenReturn(ApplicationResponse.class);
        when(idempotencyService.acquireLock(any(), any())).thenReturn(true);

        ApplicationResponse created = ApplicationResponse.builder()
                .id(UUID.randomUUID())
                .status("PENDING")
                .loanAmount(BigDecimal.valueOf(100000))
                .tenureMonths(60)
                .createdAt(Instant.now())
                .build();
        when(joinPoint.proceed()).thenReturn(created);

        Idempotent annotation = TestService.class.getMethod("create").getAnnotation(Idempotent.class);
        Object result = idempotencyAspect.handleIdempotency(joinPoint, annotation);

        assertThat(result).isEqualTo(created);
        verify(idempotencyService).saveResponse(
                eq("idempotent:createApplication:aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:create-123"),
                eq(created),
                any());
    }

    @Test
    void handleIdempotency_shouldReturnCachedResponseForDuplicate() throws Throwable {
        request.addHeader("X-Idempotency-Key", "create-123");
        when(securityUtil.getCurrentUser()).thenReturn(applicant);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("createApplication");
        when(methodSignature.getReturnType()).thenReturn(ApplicationResponse.class);
        when(idempotencyService.acquireLock(any(), any())).thenReturn(false);

        ApplicationResponse cached = ApplicationResponse.builder()
                .id(UUID.randomUUID())
                .status("PENDING")
                .loanAmount(BigDecimal.valueOf(100000))
                .tenureMonths(60)
                .createdAt(Instant.now())
                .build();
        when(idempotencyService.getResponse(any(), eq(ApplicationResponse.class))).thenReturn(cached);

        Idempotent annotation = TestService.class.getMethod("create").getAnnotation(Idempotent.class);
        Object result = idempotencyAspect.handleIdempotency(joinPoint, annotation);

        assertThat(result).isEqualTo(cached);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void handleIdempotency_shouldThrowWhenHeaderMissing() throws Throwable {
         Idempotent annotation =
            TestService.class.getMethod("create").getAnnotation(Idempotent.class);

        assertThatThrownBy(() -> idempotencyAspect.handleIdempotency(joinPoint, annotation))
            .isInstanceOf(MissingIdempotencyKeyException.class);

        verify(joinPoint, never()).getSignature();
    }

    @Test
    void handleIdempotency_shouldReleaseLockWhenBusinessLogicFails() throws Throwable {
        request.addHeader("X-Idempotency-Key", "create-123");
        when(securityUtil.getCurrentUser()).thenReturn(applicant);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("createApplication");
        when(idempotencyService.acquireLock(any(), any())).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("db error"));

        Idempotent annotation = TestService.class.getMethod("create").getAnnotation(Idempotent.class);

        assertThatThrownBy(() -> idempotencyAspect.handleIdempotency(joinPoint, annotation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");

        verify(idempotencyService).releaseLock(
                "idempotent:createApplication:aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:create-123");
    }

    @Test
    void handleIdempotency_shouldThrowWhenStillProcessing() throws Throwable {
        request.addHeader("X-Idempotency-Key", "create-123");
        when(securityUtil.getCurrentUser()).thenReturn(applicant);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("createApplication");
        when(methodSignature.getReturnType()).thenReturn(ApplicationResponse.class);
        when(idempotencyService.acquireLock(any(), any())).thenReturn(false);
        when(idempotencyService.getResponse(any(), eq(ApplicationResponse.class))).thenReturn(null);
        when(idempotencyService.isProcessing(any())).thenReturn(true);

        Idempotent annotation = TestService.class.getMethod("create").getAnnotation(Idempotent.class);

        assertThatThrownBy(() -> idempotencyAspect.handleIdempotency(joinPoint, annotation))
                .isInstanceOf(IdempotencyInProgressException.class);
    }

    static class TestService {
        @Idempotent
        public ApplicationResponse create() {
            return null;
        }
    }
}
