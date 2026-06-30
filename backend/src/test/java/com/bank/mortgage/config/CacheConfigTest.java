package com.bank.mortgage.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    void simpleCacheManager_shouldExposeApplicationCaches() {
        CacheConfig config = new CacheConfig();

        CacheManager cacheManager = config.simpleCacheManager();

        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
        assertThat(cacheManager.getCache(CacheConfig.APPLICATION_BY_ID_CACHE)).isNotNull();
        assertThat(cacheManager.getCache(CacheConfig.APPLICATION_QUERIES_CACHE)).isNotNull();
    }

    @Test
    void cacheConstants_shouldHaveExpectedNames() {
        assertThat(CacheConfig.APPLICATION_BY_ID_CACHE).isEqualTo("applicationById");
        assertThat(CacheConfig.APPLICATION_QUERIES_CACHE).isEqualTo("applicationQueries");
    }
}
