package com.bank.mortgage.idempotency;

import com.bank.mortgage.exception.IdempotencyInProgressException;
import com.bank.mortgage.exception.MissingIdempotencyKeyException;
import com.bank.mortgage.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.idempotency.enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyAspect {

    private static final String REDIS_KEY_PREFIX = "idempotent:";
    private static final int MAX_POLL_RETRIES = 10;
    private static final long POLL_INTERVAL_MS = 100L;

    private final IdempotencyService idempotencyService;
    private final SecurityUtil securityUtil;

    @Value("${app.idempotency.lock-ttl-minutes:2}")
    private long lockTtlMinutes;

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader(idempotent.header());
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException(idempotent.header());
        }

        String userId = securityUtil.getCurrentUser().getId().toString();
        String methodName = joinPoint.getSignature().getName();
        String redisKey = REDIS_KEY_PREFIX + methodName + ":" + userId + ":" + idempotencyKey.trim();
        Duration responseTtl = Duration.parse(idempotent.ttl());
        Duration lockTtl = Duration.ofMinutes(lockTtlMinutes);
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

        if (idempotencyService.acquireLock(redisKey, lockTtl)) {
            try {
                Object result = joinPoint.proceed();
                idempotencyService.saveResponse(redisKey, result, responseTtl);
                log.debug("Idempotent write completed for key {}", redisKey);
                return result;
            } catch (Exception e) {
                idempotencyService.releaseLock(redisKey);
                throw e;
            }
        }

        log.debug("Duplicate idempotent request detected for key {}", redisKey);
        for (int attempt = 0; attempt < MAX_POLL_RETRIES; attempt++) {
            Object cachedResult = idempotencyService.getResponse(redisKey, returnType);
            if (cachedResult != null) {
                return cachedResult;
            }
            if (!idempotencyService.isProcessing(redisKey)) {
                break;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }

        throw new IdempotencyInProgressException();
    }
}
