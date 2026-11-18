package de.ggbot.core.replay;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A replay's events (replay-core segments/events.ts): what happened, when,
 * where and who was involved, kept in each segment's events section.
 * Searching behaves like replay-core's {@code searchEvents}.
 */
public final class ReplayEvents {

  public static final int CHAT = 1;
  public static final int RESPAWN = 16;
  public static final int MAX_KIND = 17;

  /** Bytes before the data of an events section: raw length, codec and data length. */
  public static final int SECTION_HEADER_SIZE = 9;

  private static final int FIELD_POS = 1;
  private static final int FIELD_ACTOR = 2;
  private static final int FIELD_TARGET = 4;
  private static final int FIELD_TEXT = 8;
  private static final int FIELD_COUNT = 16;
  private static final int FIELD_DIM = 32;
  private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}\\p{N}_]+");

  private ReplayEvents() {}

  /** One event. Positions are block coordinates, the tick counts from the snippet's start. */
  public record Event(int tick, int kind, Integer x, Integer y, Integer z, String dim,
      String actor, String target, String text, Long count) {}

  /** What to look for, every part is optional. */
  public record Query(Set<Integer> kinds, List<String> players, String text, Area area) {}

  /** Events within this distance of a block. */
  public record Area(int x, int z, Integer y, int radius, String dim) {}

  /**
   * Decodes an events section as stored in a segment:
   * [u32 raw length][u8 codec][u32 length][data].
   *
   * @param section the bytes at the segment's eventsOffset, eventsLength long
   * @return the events in tick order
   * @throws IOException if the section is damaged
   */
  public static List<Event> decodeSection(byte[] section) throws IOException {
    if (section.length < SECTION_HEADER_SIZE) throw new IOException("Events section is damaged");

    ByteBuffer buffer = ByteBuffer.wrap(section);
    int rawLength = buffer.getInt(0);
    int codec = section[4];
    int length = buffer.getInt(5);

    if (length < 0 || SECTION_HEADER_SIZE + length > section.length) {
      throw new IOException("Events section is damaged");
    }

    if (codec == 0) {
      byte[] data = new byte[length];
      System.arraycopy(section, SECTION_HEADER_SIZE, data, 0, length);
      return decode(data);
    }

    return decode(ReplaySegments.inflate(section, SECTION_HEADER_SIZE, length, rawLength));
  }

  /**
   * Decodes the events of a segment (uncompressed).
   *
   * @param data the events
   * @return the events in tick order
   * @throws IOException if they are damaged
   */
  public static List<Event> decode(byte[] data) throws IOException {
    List<Event> events = new ArrayList<>();
    if (data.length == 0) return events;

    Reader reader = new Reader(data);
    long version = reader.varint();
    if (version != 1) throw new IOException("Unsupported events version " + version);

    int stringCount = (int) reader.varint();
    List<String> strings = new ArrayList<>(stringCount);
    for (int i = 0; i < stringCount; i++) strings.add(reader.string((int) reader.varint()));

    long count = reader.varint();
    long tick = 0;
    long px = 0;
    long py = 0;
    long pz = 0;

    for (long i = 0; i < count; i++) {
      tick += reader.varint();
      int kind = (int) reader.varint();
      int fields = (int) reader.varint();
      Integer x = null;
      Integer y = null;
      Integer z = null;

      if ((fields & FIELD_POS) != 0) {
        px += unzigzag(reader.varint());
        py += unzigzag(reader.varint());
        pz += unzigzag(reader.varint());
        x = (int) px;
        y = (int) py;
        z = (int) pz;
      }

      String actor = (fields & FIELD_ACTOR) != 0 ? string(strings, reader.varint()) : null;
      String target = (fields & FIELD_TARGET) != 0 ? string(strings, reader.varint()) : null;
      String text = (fields & FIELD_TEXT) != 0 ? string(strings, reader.varint()) : null;
      Long value = (fields & FIELD_COUNT) != 0 ? unzigzag(reader.varint()) : null;
      String dim = (fields & FIELD_DIM) != 0 ? string(strings, reader.varint()) : null;
      events.add(new Event((int) tick, kind, x, y, z, dim, actor, target, text, value));
    }

    return events;
  }

  /**
   * The snippet's matching events, tracking the bot's dimension across respawns.
   *
   * @param events the snippet's events
   * @param query  the search criteria
   * @return the events that match
   */
  public static List<Event> search(List<Event> events, Query query) {
    String dim = "overworld";
    List<Event> matches = new ArrayList<>();

    for (Event event : events) {
      if (event.kind() == RESPAWN && event.dim() != null) dim = event.dim();
      if (matches(event, query, dim)) matches.add(event);
    }

    return matches;
  }

  /**
   * Checks whether an event matches.
   *
   * @param event      the event
   * @param query      the search criteria
   * @param dimAtEvent the bot's dimension at that moment
   * @return {@code true} if it matches
   */
  public static boolean matches(Event event, Query query, String dimAtEvent) {
    Set<Integer> kinds = query.kinds();
    if (kinds != null && !kinds.isEmpty() && !kinds.contains(event.kind())) return false;

    boolean byPlayer = query.players() != null && !query.players().isEmpty();
    boolean byText = query.text() != null && !query.text().isEmpty();

    if (byPlayer || byText) {
      Set<String> words = new HashSet<>(tokenize(event.text() == null ? "" : event.text()));
      if (byPlayer && !involvesPlayer(event, query.players(), words)) return false;

      if (byText) {
        List<String> wanted = tokenize(query.text());
        if (wanted.isEmpty() || !words.containsAll(wanted)) return false;
      }
    }

    return query.area() == null || isInArea(event, query.area(), dimAtEvent);
  }

  private static boolean involvesPlayer(Event event, List<String> players, Set<String> words) {
    for (String player : players) {
      String name = player.toLowerCase(Locale.ROOT);
      boolean named = name.equals(lower(event.actor())) || name.equals(lower(event.target()));
      if (named || words.contains(name)) return true;
    }

    return false;
  }

  private static boolean isInArea(Event event, Area area, String dimAtEvent) {
    if (event.x() == null || event.z() == null) return false;

    String dim = event.dim() != null ? event.dim() : dimAtEvent;
    if (area.dim() != null && dim != null && !dim.equals(area.dim())) return false;

    long dx = event.x() - area.x();
    long dz = event.z() - area.z();
    if (dx * dx + dz * dz > (long) area.radius() * area.radius()) return false;

    return area.y() == null || Math.abs(event.y() - area.y()) <= Math.max(16, area.radius());
  }

  /**
   * Lower case words of 3 to 32 characters, matching how the search splits text.
   *
   * @param text the text
   * @return the words it contains
   */
  public static List<String> tokenize(String text) {
    List<String> words = new ArrayList<>();

    for (String word : WORD_SPLIT.split(text.toLowerCase(Locale.ROOT))) {
      int length = word.codePointCount(0, word.length());
      if (length >= 3 && length <= 32) words.add(word);
    }

    return words;
  }

  private static String lower(String value) {
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }

  private static long unzigzag(long n) {
    return (n & 1) == 0 ? n / 2 : -(n + 1) / 2;
  }

  private static String string(List<String> strings, long index) throws IOException {
    if (index < 0 || index >= strings.size()) {
      throw new IOException("Event points past the dictionary");
    }

    return strings.get((int) index);
  }

  private static final class Reader {
    private final byte[] data;
    private int pos;

    Reader(byte[] data) {
      this.data = data;
    }

    long varint() throws IOException {
      long value = 0;
      long factor = 1;

      for (int i = 0; i < 10; i++) {
        if (pos >= data.length) throw new IOException("Events are cut off");
        int b = data[pos++] & 0xff;
        value += (b & 0x7f) * factor;
        if ((b & 0x80) == 0) return value;

        factor *= 0x80;
      }

      throw new IOException("Varint too long");
    }

    String string(int length) throws IOException {
      if (length < 0 || pos + length > data.length) throw new IOException("Events are cut off");
      String value = new String(data, pos, length, StandardCharsets.UTF_8);
      pos += length;
      return value;
    }
  }
}
