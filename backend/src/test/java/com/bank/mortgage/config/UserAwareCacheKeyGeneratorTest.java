package com.bank.mortgage.config;

import com.bank.mortgage.domain.User;
import com.bank.mortgage.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAwareCacheKeyGeneratorTest {

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private UserAwareCacheKeyGenerator keyGenerator;

    private User testUser;
    private Method sampleMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        testUser = User.builder()
                .id(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .email("user@example.com")
                .role("APPLICANT")
                .build();
        sampleMethod = SampleService.class.getMethod("findById", String.class);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    @Test
    void generate_shouldIncludeUserIdMethodNameAndParams() {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);

        Object key = keyGenerator.generate(new SampleService(), sampleMethod, "app-123");

        assertThat(key).isEqualTo(
                "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:findById:app-123");
    }

    @Test
    void generate_withNullParam_shouldUseNullLiteral() {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);

        Object key = keyGenerator.generate(new SampleService(), sampleMethod, (Object) null);

        assertThat(key).isEqualTo(
                "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:findById:null");
    }

    @Test
    void generate_withMultipleParams_shouldJoinAll() throws NoSuchMethodException {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        Method method = SampleService.class.getMethod("search", String.class, int.class);

        Object key = keyGenerator.generate(new SampleService(), method, "PENDING", 10);

        assertThat(key).isEqualTo(
                "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:search:PENDING:10");
    }

    @Test
    void generate_withNoParams_shouldIncludeOnlyUserAndMethod() throws NoSuchMethodException {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        Method method = SampleService.class.getMethod("listAll");

        Object key = keyGenerator.generate(new SampleService(), method);

        assertThat(key).isEqualTo("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:listAll");
    }

    static class SampleService {
        public void findById(String id) {}

        public void search(String status, int page) {}

        public void listAll() {}
    }
}
