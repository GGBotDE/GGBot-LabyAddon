package de.ggbot.core.utils.ttlcache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TTLCache<K, V> {

  private final Map<K, CacheEntry<V>> map = new HashMap<>();

  public synchronized void put(K key, V value, long ttlMillis) {
    map.put(key, new CacheEntry<>(value, ttlMillis));
  }

  public synchronized V get(K key) {
    CacheEntry<V> entry = map.get(key);

    if (entry == null)
      return null;

    if (entry.isExpired()) {
      map.remove(key);
      return null;
    }
    return entry.value;
  }

  public synchronized void cleanup() {
    Iterator<Map.Entry<K, CacheEntry<V>>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      if (it.next().getValue().isExpired()) {
        it.remove();
      }
    }
  }
}
