package function.memorylock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import function.CacheFunc;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.*;
import org.ehcache.event.EventType;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class MemoryCacheFunc implements CacheFunc {

    private static final int HEAP_SIZE = 10;
    private static final int EXPIRY_SECONDS = 60;

    private CacheManager cacheManager;
    private Cache<String, String> memoryCache;

    private Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    private long lockTimeout = 5;
    private TimeUnit lockTimeoutUnit = TimeUnit.SECONDS;

    public MemoryCacheFunc(String cacheName) {
        this.cacheManager = CacheManagerBuilder
                .newCacheManagerBuilder().build();
        this.cacheManager.init();

        CacheEventListenerConfigurationBuilder listenerConfig = CacheEventListenerConfigurationBuilder.newEventListenerConfiguration(
                new MemoryCacheExpiryListener(this.lockMap),
                EventType.EXPIRED
        );

        CacheConfiguration<String, String> cacheConfig = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, String.class,
                        ResourcePoolsBuilder.heap(HEAP_SIZE))
                .withExpiry(
                        ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(EXPIRY_SECONDS))
                )
                .withService(listenerConfig)
                .build();

        this.memoryCache = this.cacheManager.createCache(cacheName, cacheConfig);
    }

    public MemoryCacheFunc(long lockTimeout, TimeUnit lockTimeoutUnit) {
        this.lockTimeout = lockTimeout;
        this.lockTimeoutUnit = lockTimeoutUnit;
    }

    private boolean trylock(String key) {
        try {
            return this.lockMap.putIfAbsent(key, new ReentrantLock())
                    .tryLock(this.lockTimeout, this.lockTimeoutUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void unlock(String key) {
        ReentrantLock lock = this.lockMap.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();

        } else {
            throw new RuntimeException(
                    String.format("The lock held by current thread does not exist. [key: %s]", key)
            );
        }
    }

    @Override
    public <T> T get(String key, TypeReference<T> dataType) {
        String cacheDataJson = this.memoryCache.get(key);
        return this.objectMapper.convertValue(cacheDataJson, dataType);
    }

    @Override
    public boolean save(String key, Supplier<Object> saveAction) {
        try {
            if (trylock(key)) {
                Object data = saveAction.get();
                String dataToBeSaved = this.objectMapper.writeValueAsString(data);

                this.memoryCache.put(key, dataToBeSaved);
                return true;
            }
            return false;

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(key);
        }
    }
}
