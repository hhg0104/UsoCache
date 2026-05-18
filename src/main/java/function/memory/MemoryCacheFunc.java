package function.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import function.CacheProperties;
import module.CacheFunc;
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

    private CacheManager cacheManager;
    private Cache<String, String> memoryCache;

    private Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    private CacheProperties cacheProperties;

    public MemoryCacheFunc(String cacheName, CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
        this.cacheManager = CacheManagerBuilder
                .newCacheManagerBuilder().build();
        this.cacheManager.init();

        CacheEventListenerConfigurationBuilder listenerConfig = CacheEventListenerConfigurationBuilder.newEventListenerConfiguration(
                new MemoryCacheExpiryListener(this.lockMap),
                EventType.EXPIRED
        );

        int heapSize = this.cacheProperties.heapSize();
        int expirySeconds = this.cacheProperties.expirySeconds();

        if (heapSize <= 0) {
            throw new RuntimeException("'Heap size' cannot be minus.");
        }
        if (expirySeconds <= 0) {
            throw new RuntimeException("'Cache expiry seconds' cannot be minus.");
        }

        CacheConfiguration<String, String> cacheConfig = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class, String.class,
                        ResourcePoolsBuilder.heap(heapSize))
                .withExpiry(
                        ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(expirySeconds))
                )
                .withService(listenerConfig)
                .build();

        this.memoryCache = this.cacheManager.createCache(cacheName, cacheConfig);
    }

    private boolean trylock(String key) {
        try {
            return this.lockMap.putIfAbsent(key, new ReentrantLock())
                    .tryLock(this.cacheProperties.cacheLockSeconds(), TimeUnit.SECONDS);
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
