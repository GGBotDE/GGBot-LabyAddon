package de.ggbot.core.replay;

import com.google.gson.JsonObject;
import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.sdk.model.Bot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import net.labymod.api.Laby;
import net.labymod.api.client.Minecraft;
import net.labymod.api.modloader.ModLoader;
import net.labymod.api.modloader.ModLoaderRegistry;
import net.labymod.api.util.KeyValue;

/** Watching replays with ReplayMod, when it is installed. */
public final class ReplayModSupport {

  public static final String FEATURE = "de.ggbot.addon.replays";
  public static final String F_ONLINE = FEATURE + ".online";
  public static final String F_LOCAL = FEATURE + ".local";
  public static final String F_PLAY = FEATURE + ".play";
  public static final String F_STREAM = FEATURE + ".stream";
  public static final String F_DOWNLOAD = FEATURE + ".download";
  public static final String F_DELETE = FEATURE + ".delete";
  public static final String F_DETAILS = FEATURE + ".details";
  public static final String F_SEARCH = FEATURE + ".search";
  public static final String F_WEB = FEATURE + ".web";
  private static final String MOD_ID = "replaymod";

  /** Largest replay played from memory, its packets uncompressed. */
  public static final long MAX_MEMORY_BYTES = 256L << 20;

  private ReplayModSupport() {}

  /** Whether ReplayMod runs in this client. */
  public static boolean isLoaded() {
    for (KeyValue<ModLoader> loader : ModLoaderRegistry.instance().getElements()) {
      if (loader.getValue().isModLoaded(MOD_ID)) return true;
    }

    return false;
  }

  /** Whether the replay screen is offered: ReplayMod runs now or was used here before. */
  public static boolean isAvailable(GGBot addon) {
    return has(addon, FEATURE) && (isLoaded() || hasFiles(addon) && ReplayFiles.folderExists());
  }

  /** Whether a part of the replay screen is turned on. */
  public static boolean has(GGBot addon, String feature) {
    return addon.getVersioningHandler().isFeatureEnabled(feature);
  }

  /** Whether the replay folder may be used. */
  public static boolean hasFiles(GGBot addon) {
    return ReplayFiles.ENABLED && has(addon, F_LOCAL);
  }

  /**
   * Whether a replay can be played here: ReplayMod runs and the replay is not newer
   * than this client.
   *
   * @param mcVersion the version it was recorded with
   * @return {@code true} if it can be played
   */
  public static boolean canPlay(String mcVersion) {
    return isLoaded() && isAtMost(mcVersion, Laby.labyAPI().minecraft().getVersion());
  }

  static boolean isAtMost(String version, String running) {
    if (version == null || running == null) return false;

    String[] a = version.split("\\.");
    String[] b = running.split("\\.");

    for (int i = 0; i < Math.max(a.length, b.length); i++) {
      int x = part(a, i);
      int y = part(b, i);
      if (x < 0 || y < 0) return false;

      if (x != y) return x < y;
    }

    return true;
  }

  private static int part(String[] parts, int index) {
    if (index >= parts.length) return 0;

    try {
      return Integer.parseInt(parts[index]);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Reads a replay of the bot into memory.
   *
   * @return the replay's entries
   * @throws IOException if loading failed or it exceeds {@link #MAX_MEMORY_BYTES}
   */
  public static Map<String, byte[]> load(GGBot addon, Bot bot, String replayId) throws Exception {
    JsonObject detail = BotRequests.getReplay(addon, bot, replayId);
    ReplayExport.MemoryTarget target = new ReplayExport.MemoryTarget();

    new ReplayExport(detail).write(ReplayStorage.HTTP, target, MAX_MEMORY_BYTES);
    return target.entries();
  }

  /**
   * Saves a replay of the bot into ReplayMod's folder.
   *
   * @return the file
   */
  public static Path download(GGBot addon, Bot bot, String replayId, long date) throws Exception {
    JsonObject detail = BotRequests.getReplay(addon, bot, replayId);
    ReplayExport export = new ReplayExport(detail);

    return ReplayFiles.save(date, replayId, out -> {
      ReplayExport.ZipTarget zip = new ReplayExport.ZipTarget(out);
      export.write(ReplayStorage.HTTP, zip, 0);
      zip.finish();
    });
  }

  /** Plays a replay file. Leaves the current server first. */
  public static void play(Path file, Consumer<Exception> onError) {
    start(() -> ReplayModPlayer.start(file), onError);
  }

  /** Plays a replay held in memory. Leaves the current server first. */
  public static void play(Map<String, byte[]> entries, Consumer<Exception> onError) {
    start(() -> ReplayModPlayer.start(entries), onError);
  }

  private interface Start {
    void run() throws IOException;
  }

  private static void start(Start start, Consumer<Exception> onError) {
    Minecraft minecraft = Laby.labyAPI().minecraft();
    minecraft.executeOnRenderThread(() -> {
      if (minecraft.isIngame()) Laby.labyAPI().serverController().leaveServer();
      
      minecraft.executeOnRenderThread(() -> {
        try {
          start.run();
        } catch (IOException | RuntimeException | LinkageError e) {
          onError.accept(e instanceof Exception exception ? exception : new IOException(e));
        }
      });
    });
  }
}
