package function;

public class CacheProperties {

    private int heapSize = 1000;

    private int expirySeconds = 300;

    private int cacheLockSeconds = 5;

    private CacheProperties() {
        // no initialize
    }

    private static CacheProperties builder() {
        return new CacheProperties();
    }

    public void heapSize(int heapSize) {
        this.heapSize = heapSize;
    }

    public int heapSize() {
        return this.heapSize;
    }

    public void expirySeconds(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public int expirySeconds() {
        return this.expirySeconds;
    }

    public void cacheLockSeconds(int cacheLockSeconds) {
        this.cacheLockSeconds = cacheLockSeconds;
    }

    public int cacheLockSeconds() {
        return this.cacheLockSeconds;
    }
}
