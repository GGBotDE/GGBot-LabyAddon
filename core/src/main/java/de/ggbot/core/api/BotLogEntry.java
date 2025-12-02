package de.ggbot.core.api;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;

public class BotLogEntry {
  @SerializedName("message")
  private String message;

  @SerializedName("level")
  private String level;

  @SerializedName("timestamp")
  private String timestamp;

  public String getMessage() { return message; }
  public String getLevel() { return level; }
  public String getTimestamp() { return timestamp; }

  public Instant getTimestampInstant() {
    return Instant.parse(timestamp);
  }
}