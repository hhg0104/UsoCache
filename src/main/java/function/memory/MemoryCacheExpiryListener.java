package function.memory;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MemoryCacheExpiryListener implements CacheEventListener<String, String> {

    private Map<String, ReentrantLock> lockMap;

    MemoryCacheExpiryListener(Map<String, ReentrantLock> lockMap) {
        this.lockMap = lockMap;
    }

    @Override
    public void onEvent(CacheEvent<? extends String, ? extends String> event) {
        if (event.getType() == EventType.EXPIRED) {
            this.lockMap.remove(event.getKey());
        }
    }
}
