package de.ggbot.core.replay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.labymod.api.Constants;
import net.labymod.api.Laby;

import static de.ggbot.core.replay.ReplayJson.number;
import static de.ggbot.core.replay.ReplayJson.string;

/**
 * The replays in ReplayMod's folder ({@code replay_recordings} in the game folder).
 *
 * <p>This is the only place the addon (replay feature) touches the file system, and it only
 * touches that folder and only after the user asked for it:
 * <ul>
 *   <li>listing and reading the .mcpr files there, so the user sees the replays they can
 *       already play (and which of the bot's replays are downloaded);</li>
 *   <li>writing a replay into it when the user clicks Download, which is how a replay is
 *       kept, since ReplayMod only plays and saves replays from that folder;</li>
 *   <li>deleting a replay when the user clicks Delete and confirms it;</li>
 *   <li>opening the folder in the system's file manager when the user clicks Show.</li>
 * </ul>
 * Playing a replay of the bot without downloading it requires none of this, as it is held
 * in memory ({@link MemoryReplayFile}). Setting {@link #ENABLED} to {@code false} removes all
 * file access: local replays, downloading, showing and deleting files disappear and
 * replays of the bot only play from memory.
 */
public final class ReplayFiles {

  /** Whether the replay feature may access ReplayMod's folder at all. */
  public static final boolean ENABLED = true;

  private static final String FOLDER = "replay_recordings";
  private static final String EXTENSION = ".mcpr";
  private static final byte[] OLD_THUMB_MAGIC = {0, 1, 1, 2, 3, 5, 8};
  private static final long MAX_THUMBNAIL = 2L << 20;

  private ReplayFiles() {}

  /** ReplayMod's folder. */
  public static Path folder() {
    return Constants.Files.LABYMOD_DIRECTORY.toAbsolutePath().getParent().resolve(FOLDER);
  }

  /** Whether the folder exists, so ReplayMod was used here before. */
  public static boolean folderExists() {
    return ENABLED && Files.isDirectory(folder());
  }

  /**
   * The replays in the folder, newest first. Files that cannot be read are left out.
   *
   * @return the replays
   */
  public static List<ReplayInfo> list() {
    List<ReplayInfo> replays = new ArrayList<>();
    if (!folderExists()) return replays;

    try (DirectoryStream<Path> files = Files.newDirectoryStream(folder(), "*" + EXTENSION)) {
      for (Path file : files) {
        ReplayInfo info = read(file);
        if (info != null) replays.add(info);
      }
    } catch (IOException e) {
      return replays;
    }

    replays.sort((a, b) -> Long.compare(b.getDate(), a.getDate()));
    return replays;
  }

  /**
   * Reads what ReplayMod and GGBot store about a replay: {@code metaData.json},
   * {@code ggbot/manifest.json} and the first preview image.
   *
   * @param file the replay
   * @return the replay, {@code null} if it is not one
   */
  public static ReplayInfo read(Path file) {
    try (ZipFile zip = new ZipFile(file.toFile())) {
      JsonObject meta = json(zip, "metaData.json");
      if (meta == null) return null;

      JsonObject manifest = json(zip, "ggbot/manifest.json");
      boolean ggbot = manifest != null && "ggbot-replay".equals(string(manifest, "format"));
      byte[] thumbnail = null;

      if (ggbot && manifest.get("thumbnails") instanceof JsonArray thumbnails
          && thumbnails.size() > 0) {
        thumbnail = bytes(zip, string(thumbnails.get(0).getAsJsonObject(), "file"));
      }

      if (thumbnail == null) thumbnail = bytes(zip, "thumb.jpg");
      if (thumbnail == null) thumbnail = oldThumbnail(bytes(zip, "thumb"));

      String title = string(meta, "customServerName");
      if (title == null || title.isEmpty()) title = string(meta, "serverName");
      if (title == null || title.isEmpty()) title = file.getFileName().toString();

      String remoteId = ggbot ? string(manifest, "replayId") : null;
      return new ReplayInfo(remoteId, file, title, string(meta, "mcversion"),
          number(meta, "date"), number(meta, "duration"), Files.size(file), ggbot, thumbnail);
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }

  /**
   * Saves a replay into the folder. It is first written to a temporary file, so
   * ReplayMod never sees a partially written replay.
   *
   * @param date   when it was recorded, used in the file name
   * @param id     the replay id, used in the file name
   * @param writer writes the .mcpr
   * @return the file
   * @throws IOException if writing failed
   */
  public static Path save(long date, String id, Writer writer) throws IOException {
    if (!ENABLED) throw new IOException("Replay files are disabled");

    Files.createDirectories(folder());
    SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ROOT);
    String name = format.format(new Date(date)) + " GGBot " + id.replaceAll("[^0-9A-Za-z]", "");
    Path target = folder().resolve(name + EXTENSION);
    Path temp = folder().resolve(name + EXTENSION + ".tmp");

    try (OutputStream out = Files.newOutputStream(temp)) {
      writer.write(out);
    } catch (IOException | RuntimeException e) {
      Files.deleteIfExists(temp);
      throw e;
    }

    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    return target;
  }

  /** Writes a replay. */
  public interface Writer {
    void write(OutputStream out) throws IOException;
  }

  /**
   * Deletes a replay of the folder, nothing else.
   *
   * @param file the replay
   * @throws IOException if it is not a replay in the folder or could not be deleted
   */
  public static void delete(Path file) throws IOException {
    Path path = file.toAbsolutePath().normalize();
    if (!ENABLED || !folder().normalize().equals(path.getParent())
        || !path.getFileName().toString().endsWith(EXTENSION)) {
      throw new IOException("Not a replay of ReplayMod's folder");
    }

    Files.deleteIfExists(path);
  }

  /** Opens the folder in the system's file manager. */
  public static void show() {
    if (folderExists()) Laby.references().chatExecutor().openFile(folder().toString());
  }

  private static JsonObject json(ZipFile zip, String name) throws IOException {
    byte[] data = bytes(zip, name);
    if (data == null) return null;

    JsonElement element = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
    return element.isJsonObject() ? element.getAsJsonObject() : null;
  }

  private static byte[] bytes(ZipFile zip, String name) throws IOException {
    if (name == null) return null;

    ZipEntry entry = zip.getEntry(name);
    if (entry == null || entry.getSize() > MAX_THUMBNAIL) return null;

    try (InputStream in = zip.getInputStream(entry)) {
      return in.readAllBytes();
    }
  }

  private static byte[] oldThumbnail(byte[] data) {
    if (data == null || data.length <= OLD_THUMB_MAGIC.length) return null;

    for (int i = 0; i < OLD_THUMB_MAGIC.length; i++) {
      if (data[i] != OLD_THUMB_MAGIC[i]) return data;
    }

    byte[] image = new byte[data.length - OLD_THUMB_MAGIC.length];
    System.arraycopy(data, OLD_THUMB_MAGIC.length, image, 0, image.length);
    return image;
  }
}
