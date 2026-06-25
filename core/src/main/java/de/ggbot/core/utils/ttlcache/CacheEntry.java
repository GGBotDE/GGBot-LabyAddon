package de.ggbot.core.utils.ttlcache;

/**
 * Internal value holder for {@link TTLCache} that pairs a cached value with
 * its absolute expiry timestamp.
 *
 * @param <V> the cached value type
 */
class CacheEntry<V> {
  final V value;
  final long expiryTime;

  /**
   * @param value     the value to cache
   * @param ttlMillis time-to-live in milliseconds
   */
  CacheEntry(V value, long ttlMillis) {
    this.value = value;
    this.expiryTime = System.currentTimeMillis() + ttlMillis;
  }

  /**
   * Returns whether this entry has passed its expiry time.
   *
   * @return {@code true} if expired
   */
  boolean isExpired() {
    return System.currentTimeMillis() > expiryTime;
  }
}