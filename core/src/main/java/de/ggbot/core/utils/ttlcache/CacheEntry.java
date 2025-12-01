package de.ggbot.core.utils.ttlcache;

class CacheEntry<V> {
  V value;
  long expiryTime;

  CacheEntry(V value, long ttlMillis) {
    this.value = value;
    this.expiryTime = System.currentTimeMillis() + ttlMillis;
  }

  boolean isExpired() {
    return System.currentTimeMillis() > expiryTime;
  }
}