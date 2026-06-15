package com.bank.mortgage.config;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.bank.mortgage.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

@Component("userAwareCacheKeyGenerator")
@RequiredArgsConstructor
public class UserAwareCacheKeyGenerator implements KeyGenerator {

    private final SecurityUtil securityUtil;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String userId = securityUtil.getCurrentUser().getId().toString();
        StringJoiner key = new StringJoiner(":");
        key.add(userId);
        key.add(method.getName());
        for (Object param : params) {
            key.add(param != null ? param.toString() : "null");
        }
        return key.toString();
    }
}
