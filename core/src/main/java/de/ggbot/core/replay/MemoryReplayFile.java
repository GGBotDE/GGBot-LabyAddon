package de.ggbot.core.replay;

import com.replaymod.replaystudio.lib.guava.base.Optional;
import com.replaymod.replaystudio.replay.AbstractReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A ReplayMod replay kept in memory, letting a bot's replay be watched without
 * saving it to disk. Anything ReplayMod writes during playback (markers,
 * keyframes, caches) is also held here and discarded once the replay closes.
 * Loaded only when ReplayMod is.
 */
final class MemoryReplayFile extends AbstractReplayFile {

  private final Map<String, byte[]> entries;
  private final Map<String, byte[]> cache = new LinkedHashMap<>();

  MemoryReplayFile(Map<String, byte[]> entries) throws IOException {
    super(new ReplayStudio());
    this.entries = entries;
  }

  @Override
  public Optional<InputStream> get(String entry) {
    return stream(entries.get(entry));
  }

  @Override
  public Optional<InputStream> getCache(String entry) {
    return stream(cache.get(entry));
  }

  @Override
  public Map<String, InputStream> getAll(Pattern pattern) {
    Map<String, InputStream> matches = new LinkedHashMap<>();

    for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
      if (pattern.matcher(entry.getKey()).matches()) {
        matches.put(entry.getKey(), new ByteArrayInputStream(entry.getValue()));
      }
    }

    return matches;
  }

  @Override
  public OutputStream write(String entry) {
    return into(entries, entry);
  }

  @Override
  public OutputStream writeCache(String entry) {
    return into(cache, entry);
  }

  @Override
  public void remove(String entry) {
    entries.remove(entry);
  }

  @Override
  public void removeCache(String entry) {
    cache.remove(entry);
  }

  @Override
  public void save() {
  }

  @Override
  public void saveTo(File target) throws IOException {
    throw new IOException("A replay that was not downloaded cannot be saved");
  }

  @Override
  public void close() {
    entries.clear();
    cache.clear();
  }

  private static Optional<InputStream> stream(byte[] data) {
    return data == null ? Optional.absent() : Optional.of(new ByteArrayInputStream(data));
  }

  private static OutputStream into(Map<String, byte[]> map, String entry) {
    return new ByteArrayOutputStream() {
      @Override
      public void close() {
        map.put(entry, toByteArray());
      }
    };
  }
}
