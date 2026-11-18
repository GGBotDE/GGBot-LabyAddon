package de.ggbot.core.replay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ggbot.core.api.VersioningHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The files of the bot's replays, read directly from the replay storage just as the
 * panel's viewer reads them. The address is provided by the backend and may change,
 * so only HTTPS on our own domains or Backblaze B2 is allowed and redirects are
 * not followed.
 */
public final class ReplayStorage {

  private static final String[] STORAGE_DOMAINS = {"backblazeb2.com", "backblaze.com"};
  private static final int NOT_FOUND = 404;
  private static final int PARTIAL = 206;
  private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
      .followRedirects(false)
      .followSslRedirects(false)
      .build();

  /** Loads a file or a part of it. */
  public interface Files {
    /**
     * @param url the file
     * @return its bytes, {@code null} when it does not exist
     */
    byte[] get(String url) throws IOException;

    /**
     * @param url   the file
     * @param start the first byte
     * @param end   the last byte
     * @return those bytes, {@code null} when the file does not exist
     */
    default byte[] range(String url, long start, long end) throws IOException {
      byte[] all = get(url);
      if (all == null) return null;

      byte[] part = new byte[(int) (end - start + 1)];
      System.arraycopy(all, (int) start, part, 0, part.length);
      return part;
    }
  }

  /** Loads files over HTTPS. */
  public static final Files HTTP = new Files() {
    @Override
    public byte[] get(String url) throws IOException {
      return request(new Request.Builder().url(url).build(), -1, -1);
    }

    @Override
    public byte[] range(String url, long start, long end) throws IOException {
      return request(new Request.Builder().url(url)
          .header("Range", "bytes=" + start + "-" + end).build(), start, end);
    }
  };

  private final JsonObject downloads;
  private final String baseUrl;

  /**
  * @param downloads signed access as provided by the api, {@code null} for public storage
  * @param baseUrl   the location of the files
  */
  public ReplayStorage(JsonObject downloads, String baseUrl) {
    this.downloads = downloads;
    this.baseUrl = baseUrl;
  }

  /**
   * The address of a file, signed when the storage is private.
   *
   * @param key the storage key
   * @return the address
   * @throws IOException if it is not on an allowed host
   */
  public String url(String key) throws IOException {
    String url = isPresigned(key)
        ? downloads.getAsJsonObject("urls").get(key).getAsString()
        : publicUrl(key);

    if (!isAllowed(url)) throw new IOException("The replay storage is not on an allowed host");
    return url;
  }

  private String publicUrl(String key) throws IOException {
    if (baseUrl == null) throw new IOException("The replay storage is unknown");

    String url = baseUrl.replaceAll("/+$", "") + "/" + key;
    String query = null;

    if (downloads != null && downloads.get("byKey") instanceof JsonObject byKey) {
      query = ReplayJson.string(byKey, key);
    } else if (downloads != null) {
      query = ReplayJson.string(downloads, "query");
    }

    return query != null ? url + "?" + query : url;
  }

  /**
   * The address of a file that changes, such as a manifest. A CDN in front of the
   * storage might cache it longer than the storage permits, so it is given a unique
   * query. A presigned address cannot take one and is never cached.
   */
  private String fresh(String key) throws IOException {
    String url = url(key);
    if (isPresigned(key)) return url;

    return url + (url.contains("?") ? "&" : "?") + "fresh=" + System.currentTimeMillis();
  }

  private boolean isPresigned(String key) {
    return downloads != null && downloads.get("urls") instanceof JsonObject urls && urls.has(key);
  }

  /**
   * Whether replay files may be loaded from an address.
   *
   * @param url the address
   * @return {@code true} if it may be loaded
   */
  public static boolean isAllowed(String url) {
    return VersioningHandler.isAllowedBaseUrl(url, STORAGE_DOMAINS);
  }

  /**
   * A snippet's manifest.
   *
   * @param files loads it
   * @param key   its key
   * @return the manifest, {@code null} while the snippet has no data
   */
  public JsonObject manifest(Files files, String key) throws IOException {
    byte[] data = files.get(fresh(key));
    if (data == null) return null;

    JsonElement manifest = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
    if (!manifest.isJsonObject() || !manifest.getAsJsonObject().has("segments")
        || manifest.getAsJsonObject().get("formatVersion").getAsInt() != 2) {
      return null;
    }

    return manifest.getAsJsonObject();
  }

  /**
   * The events of a snippet, only the events sections of its segments are loaded.
   *
   * @param files    loads them
   * @param manifest the snippet's manifest
   * @return the events in tick order
   */
  public List<ReplayEvents.Event> events(Files files, JsonObject manifest) throws IOException {
    List<ReplayEvents.Event> events = new ArrayList<>();

    for (JsonObject segment : segments(manifest)) {
      if (segment.has("eventCount") && segment.get("eventCount").getAsInt() == 0) continue;

      long start = segment.get("eventsOffset").getAsLong();
      long length = segment.get("eventsLength").getAsLong();
      if (length < ReplayEvents.SECTION_HEADER_SIZE) continue;

      String url = url(ReplayJson.string(segment, "key"));
      byte[] section = files.range(url, start, start + length - 1);

      if (section == null || section.length != length) {
        throw new IOException("A part of the replay is missing");
      }

      events.addAll(ReplayEvents.decodeSection(section));
    }

    return events;
  }

  /**
   * The segments of a snippet, in order.
   *
   * @param manifest the snippet's manifest
   * @return its segments
   */
  public static List<JsonObject> segments(JsonObject manifest) {
    List<JsonObject> segments = new ArrayList<>();
    for (JsonElement segment : manifest.getAsJsonArray("segments")) {
      segments.add(segment.getAsJsonObject());
    }

    segments.sort((a, b) -> Long.compare(
        ReplayJson.number(a, "segmentNo"), ReplayJson.number(b, "segmentNo")));
    return segments;
  }

  private static byte[] request(Request request, long start, long end) throws IOException {
    try (Response response = CLIENT.newCall(request).execute()) {
      if (response.code() == NOT_FOUND) return null;

      if (!response.isSuccessful() || response.body() == null) {
        throw new IOException("Replay storage answered HTTP " + response.code());
      }

      byte[] data = response.body().bytes();
      if (start < 0 || response.code() == PARTIAL) return data;

      if (end >= data.length) throw new IOException("A part of the replay is missing");
      byte[] part = new byte[(int) (end - start + 1)];
      System.arraycopy(data, (int) start, part, 0, part.length);
      return part;
    }
  }
}
