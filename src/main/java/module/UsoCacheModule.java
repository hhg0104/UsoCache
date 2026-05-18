package module;

import com.fasterxml.jackson.core.type.TypeReference;
import keygen.KeyGen;

import java.util.function.Supplier;

public class UsoCacheModule {

    private KeyGen keyGen;
    private CacheFunc cacheFunc;

    /**
     * No new constructor declaration allowed
     */
    UsoCacheModule(KeyGen keyGen, CacheFunc cacheFunc){
        this.keyGen = keyGen;
        this.cacheFunc = cacheFunc;
    }

    public String genKey(Object... objForKeys) {
        if (objForKeys == null || objForKeys.length == 0) {
            return null;
        }
        return keyGen.generate(objForKeys);
    }

    public <T> T get(String key, TypeReference<T> dataType) {
        if (key == null || key.isEmpty() || dataType == null) {
            return null;
        }
        return this.cacheFunc.get(key, dataType);
    }

    public boolean save(String key, Supplier<Object> saveAction) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return this.cacheFunc.save(key, saveAction);
    }
}
