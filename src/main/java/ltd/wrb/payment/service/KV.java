package ltd.wrb.payment.service;

/**
 * KV is a simple key-value store interface.
 * all the KV pairs are stored permanently by default.
 * DO NOT USE REDIS or OTHER MEMORY DATABASES.
 */
public interface KV {

    String get(String key);

    // permanent is true by default
    void put(String key, String value);

    // expireInSeconds is 0 means permanent, otherwise it's the expire time in seconds
    void put(String key, String value, int expireInSeconds);

    void delete(String key);

    void clear();

    boolean contains(String key);
} 