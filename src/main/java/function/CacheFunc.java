package function;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.function.Supplier;

public interface CacheFunc {

    <T> T get(String key, TypeReference<T> dataType);

    boolean save(String key, Supplier<Object> saveAction);
}
