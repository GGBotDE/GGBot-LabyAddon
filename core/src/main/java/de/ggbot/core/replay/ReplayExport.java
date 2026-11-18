package de.ggbot.core.replay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static de.ggbot.core.replay.ReplayJson.number;
import static de.ggbot.core.replay.ReplayJson.string;

/**
 * Converts a GGBot replay into a ReplayMod replay, mirroring the panel's replay
 * viewer export. The snippets' manifests and segments are read 
 * directly from the replay storage, their records are
 * aligned to a single clock and written as {@code recording.tmcpr} alongside
 * {@code metaData.json}, its checksum and the {@code ggbot/manifest.json}.
 */
public final class ReplayExport {

  private static final int MS_PER_TICK = 50;
  private static final String GENERATOR = "GGBot LabyMod Addon";

  /** Where the export goes: a zip file or entries in memory. */
  public interface Target {
    OutputStream begin(String name) throws IOException;

    void end() throws IOException;
  }

  private final JsonObject replay;
  private final ReplayStorage storage;

  /**
   * @param detail the answer of the replay endpoint ({@code replay}, {@code downloads},
   *               {@code baseUrl})
   */
  public ReplayExport(JsonObject detail) {
    this.replay = detail.getAsJsonObject("replay");
    this.storage = storage(detail);
  }

  /**
   * The storage of a replay.
   *
   * @param detail the answer of the replay endpoint
   * @return where its files are
   */
  public static ReplayStorage storage(JsonObject detail) {
    JsonObject downloads = detail.get("downloads") instanceof JsonObject access ? access : null;
    return new ReplayStorage(downloads, string(detail, "baseUrl"));
  }

  /**
   * Writes out the replay.
   *
   * @param files    provides the manifests and segments
   * @param target   receives the entries
   * @param maxBytes fails once records exceed this many bytes, 0 means unlimited
   * @throws IOException if a file cannot be read or the replay contains no data
   */
  public void write(ReplayStorage.Files files, Target target, long maxBytes) throws IOException {
    List<Snippet> snippets = snippets(files);
    if (snippets.isEmpty()) throw new IOException("The replay has no recorded data yet");

    long first = snippets.get(0).startedAt;
    long clockEnd = 0;

    for (Snippet snippet : snippets) {
      long offset = snippet.startedAt - first;
      snippet.offsetMs = Math.max(offset, clockEnd);
      clockEnd = snippet.offsetMs + (long) snippet.durationTicks * MS_PER_TICK;
    }

    Snippet last = snippets.get(snippets.size() - 1);
    long durationMs = last.offsetMs + (long) (last.durationTicks + 1) * MS_PER_TICK;

    write(target, "metaData.json", metaData(snippets, durationMs));

    CRC32 crc = new CRC32();
    long[] written = {0};
    OutputStream entry = target.begin("recording.tmcpr");

    for (Snippet snippet : snippets) {
      snippet.tmcprOffset = written[0];
      RecordRewriter records = new RecordRewriter(new FilterOutputStream(entry) {
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
          crc.update(b, off, len);
          written[0] += len;
          if (maxBytes > 0 && written[0] > maxBytes) throw new TooLargeException();
          out.write(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
          write(new byte[]{(byte) b}, 0, 1);
        }
      }, (int) snippet.offsetMs);

      for (String key : snippet.segmentKeys) {
        byte[] segment = files.get(storage.url(key));
        if (segment == null) throw new IOException("A part of the replay is missing");
        ReplaySegments.writeRecords(segment, records);
      }

      records.finish();
      snippet.tmcprLength = written[0] - snippet.tmcprOffset;
    }

    target.end();

    long value = crc.getValue();
    byte[] checksum = {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8),
        (byte) value};
    write(target, "recording.tmcpr.crc32", checksum);
    write(target, "ggbot/manifest.json", manifest(snippets));
  }

  /** Thrown when a replay is larger than allowed. */
  public static final class TooLargeException extends IOException {
    public TooLargeException() {
      super("The replay is too large");
    }
  }

  private List<Snippet> snippets(ReplayStorage.Files files) throws IOException {
    List<Snippet> snippets = new ArrayList<>();
    JsonArray rows = replay.has("snippets") ? replay.getAsJsonArray("snippets") : new JsonArray();

    for (JsonElement element : rows) {
      JsonObject row = element.getAsJsonObject();
      int snippetId = row.get("snippet_id").getAsInt();
      JsonObject manifest = storage.manifest(files, manifestKey(replay, row));
      if (manifest == null || manifest.getAsJsonArray("segments").size() == 0) continue;

      Snippet snippet = new Snippet();
      snippet.snippetId = snippetId;
      snippet.parentSnippetId = number(row, "parent_snippet_id");
      snippet.startedAt = Long.parseLong(string(row, "started_at"));
      snippet.durationTicks = (int) Math.max(number(row, "duration_ticks"),
          number(manifest, "durationTicks"));
      snippet.freshJoin = number(row, "fresh_join") != 0;

      for (JsonObject segment : ReplayStorage.segments(manifest)) {
        snippet.segmentKeys.add(string(segment, "key"));
      }

      if (manifest.has("players")) {
        for (JsonElement player : manifest.getAsJsonArray("players")) {
          snippet.players.add(string(player.getAsJsonObject(), "uuid"));
        }
      }

      snippets.add(snippet);
    }

    return snippets;
  }

  /**
   * Locates a snippet's manifest.
   *
   * @param replay the replay
   * @param row    the snippet as listed by the api
   * @return its storage key
   */
  public static String manifestKey(JsonObject replay, JsonObject row) {
    String key = string(row, "manifest_key");
    if (key != null) return key;

    return "replays/" + string(replay, "id") + "/" + string(row, "snippet_id") + "/manifest.json";
  }

  private byte[] metaData(List<Snippet> snippets, long durationMs) {
    Set<String> players = new LinkedHashSet<>();
    for (Snippet snippet : snippets) players.addAll(snippet.players);

    JsonArray playerArray = new JsonArray();
    for (String player : players) playerArray.add(player);

    JsonObject meta = new JsonObject();
    meta.addProperty("singleplayer", number(replay, "singleplayer") != 0);
    meta.addProperty("serverName", string(replay, "server_name"));
    meta.addProperty("customServerName", string(replay, "custom_server_name"));
    meta.addProperty("duration", durationMs);
    meta.addProperty("date", snippets.get(0).startedAt);
    meta.addProperty("mcversion", string(replay, "mc_version"));
    meta.addProperty("fileFormat", "MCPR");
    meta.addProperty("fileFormatVersion", 14);
    meta.addProperty("protocol", number(replay, "protocol_version"));
    meta.addProperty("generator", GENERATOR);
    meta.addProperty("selfId", -1);
    meta.add("players", playerArray);
    return meta.toString().getBytes(StandardCharsets.UTF_8);
  }

  private byte[] manifest(List<Snippet> snippets) {
    JsonObject manifest = new JsonObject();
    manifest.addProperty("format", "ggbot-replay");
    manifest.addProperty("version", 2);
    manifest.addProperty("replayId", string(replay, "id"));
    manifest.addProperty("mcVersion", string(replay, "mc_version"));
    manifest.addProperty("protocolVersion", number(replay, "protocol_version"));
    JsonArray list = new JsonArray();

    for (Snippet snippet : snippets) {
      JsonObject entry = new JsonObject();
      entry.addProperty("snippetId", snippet.snippetId);
      entry.addProperty("parentSnippetId", snippet.parentSnippetId);
      entry.addProperty("tmcprOffset", snippet.tmcprOffset);
      entry.addProperty("tmcprLength", snippet.tmcprLength);
      entry.addProperty("timeOffsetMs", snippet.offsetMs);
      entry.addProperty("durationTicks", snippet.durationTicks);
      entry.addProperty("freshJoin", snippet.freshJoin);
      entry.add("metadataFile", null);
      entry.add("snapshotFile", null);
      entry.add("eventsFile", null);
      entry.add("summary", null);
      entry.addProperty("eventCount", 0);
      list.add(entry);
    }

    manifest.add("snippets", list);
    manifest.add("thumbnails", new JsonArray());
    return manifest.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void write(Target target, String name, byte[] data) throws IOException {
    target.begin(name).write(data);
    target.end();
  }

  private static final class Snippet {
    int snippetId;
    long parentSnippetId;
    long startedAt;
    int durationTicks;
    boolean freshJoin;
    long offsetMs;
    long tmcprOffset;
    long tmcprLength;
    final List<String> segmentKeys = new ArrayList<>();
    final List<String> players = new ArrayList<>();
  }

  /** Adds an offset to the timestamp of every record ([int ms][int length][packet]). */
  static final class RecordRewriter extends OutputStream {
    private final OutputStream out;
    private final int offsetMs;
    private final byte[] header = new byte[8];
    private int headerLength;
    private long remaining;

    RecordRewriter(OutputStream out, int offsetMs) {
      this.out = out;
      this.offsetMs = offsetMs;
    }

    @Override
    public void write(int b) throws IOException {
      write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      int end = off + len;

      while (off < end) {
        if (remaining > 0) {
          int step = (int) Math.min(remaining, end - off);
          out.write(b, off, step);
          remaining -= step;
          off += step;
          continue;
        }

        int step = Math.min(8 - headerLength, end - off);
        System.arraycopy(b, off, header, headerLength, step);
        headerLength += step;
        off += step;

        if (headerLength == 8) {
          int time = readInt(0) + offsetMs;
          header[0] = (byte) (time >>> 24);
          header[1] = (byte) (time >>> 16);
          header[2] = (byte) (time >>> 8);
          header[3] = (byte) time;
          remaining = Math.max(0, readInt(4));
          out.write(header, 0, 8);
          headerLength = 0;
        }
      }
    }

    /** Passes on a header cut off at the end as it is. */
    void finish() throws IOException {
      if (headerLength > 0) out.write(header, 0, headerLength);
      headerLength = 0;
    }

    private int readInt(int at) {
      return ((header[at] & 0xff) << 24) | ((header[at + 1] & 0xff) << 16)
          | ((header[at + 2] & 0xff) << 8) | (header[at + 3] & 0xff);
    }
  }

  /** Entries in memory, by name. */
  public static final class MemoryTarget implements Target {
    private final Map<String, byte[]> entries = new LinkedHashMap<>();
    private String name;
    private ByteArrayOutputStream current;

    @Override
    public OutputStream begin(String name) {
      this.name = name;
      this.current = new ByteArrayOutputStream();
      return current;
    }

    @Override
    public void end() {
      entries.put(name, current.toByteArray());
      current = null;
    }

    public Map<String, byte[]> entries() {
      return entries;
    }
  }

  /** A zip file, the format of ReplayMod's .mcpr files. */
  public static final class ZipTarget implements Target {
    private final ZipOutputStream zip;

    public ZipTarget(OutputStream out) {
      this.zip = new ZipOutputStream(out);
    }

    @Override
    public OutputStream begin(String name) throws IOException {
      zip.putNextEntry(new ZipEntry(name));
      return new FilterOutputStream(zip) {
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
          out.write(b, off, len);
        }

        @Override
        public void close() {
        }
      };
    }

    @Override
    public void end() throws IOException {
      zip.closeEntry();
    }

    public void finish() throws IOException {
      zip.finish();
    }
  }
}
