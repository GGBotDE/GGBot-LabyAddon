package de.ggbot.core.replay;

import java.nio.file.Path;

/** A replay file in ReplayMod's folder, maybe a downloaded replay of a bot. */
public final class ReplayInfo {

  private final String remoteId;
  private final Path file;
  private final String title;
  private final String mcVersion;
  private final long date;
  private final long durationMs;
  private final long size;
  private final boolean ggbot;
  private final byte[] thumbnail;

  public ReplayInfo(String remoteId, Path file, String title, String mcVersion, long date,
      long durationMs, long size, boolean ggbot, byte[] thumbnail) {
    this.remoteId = remoteId;
    this.file = file;
    this.title = title;
    this.mcVersion = mcVersion;
    this.date = date;
    this.durationMs = durationMs;
    this.size = size;
    this.ggbot = ggbot;
    this.thumbnail = thumbnail;
  }

  /** The id of the bot's replay it was downloaded from, {@code null} for other replays. */
  public String getRemoteId() {
    return remoteId;
  }

  public Path getFile() {
    return file;
  }

  public String getTitle() {
    return title;
  }

  public String getMcVersion() {
    return mcVersion;
  }

  public long getDate() {
    return date;
  }

  public long getDurationMs() {
    return durationMs;
  }

  public long getSize() {
    return size;
  }

  public boolean isGgbot() {
    return ggbot;
  }

  /** A JPEG or PNG preview, {@code null} if there is none. */
  public byte[] getThumbnail() {
    return thumbnail;
  }
}
