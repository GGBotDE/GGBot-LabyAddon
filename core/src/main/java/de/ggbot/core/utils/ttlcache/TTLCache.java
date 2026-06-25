package de.ggbot.core.utils.ttlcache;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple thread-safe cache that evicts entries after a per-entry time-to-live.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class TTLCache<K, V> {

  private final Map<K, CacheEntry<V>> map = new HashMap<>();

  /**
   * Stores a value under the given key with the specified TTL.
   *
   * @param key       the cache key
   * @param value     the value to store
   * @param ttlMillis how long (in ms) the entry should remain valid
   */
  public synchronized void put(K key, V value, long ttlMillis) {
    map.put(key, new CacheEntry<>(value, ttlMillis));
  }

  /**
   * Returns the cached value for the given key, or {@code null} if absent or expired.
   * Expired entries are removed lazily on access.
   *
   * @param key the cache key
   * @return the cached value, or {@code null}
   */
  public synchronized V get(K key) {
    CacheEntry<V> entry = map.get(key);
    if (entry == null) return null;
    if (entry.isExpired()) {
      map.remove(key);
      return null;
    }
    return entry.value;
  }

  /**
   * Removes all expired entries from the cache.
   * Call periodically to reclaim memory if {@link #get} is infrequently used.
   */
  public synchronized void cleanup() {
    map.entrySet().removeIf(e -> e.getValue().isExpired());
  }
}
