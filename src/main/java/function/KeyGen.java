package function;

@FunctionalInterface
public interface KeyGen {

    /**
     * Create a key for a cache data.
     *
     * @param objsForKey object data for key generation.
     * @return generated key
     */
    String generate(Object... objsForKey);
}
