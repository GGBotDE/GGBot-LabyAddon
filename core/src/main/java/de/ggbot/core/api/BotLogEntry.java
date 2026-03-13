package de.ggbot.core.api;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;

/**
 * Represents a single log entry returned by the bot log API endpoint.
 * Fields are populated by Gson from the JSON response.
 */
public class BotLogEntry {

  @SerializedName("message")
  private String message;

  @SerializedName("level")
  private String level;

  @SerializedName("timestamp")
  private String timestamp;

  /** @return the human-readable log message */
  public String getMessage() { return message; }

  /** @return the log level string (e.g. {@code "info"}, {@code "error"}) */
  public String getLevel() { return level; }

  /** @return the raw ISO-8601 timestamp string */
  public String getTimestamp() { return timestamp; }

  /**
   * Parses the raw timestamp string into a Java {@link Instant}.
   *
   * @return the log entry timestamp as an {@link Instant}
   */
  public Instant getTimestampInstant() {
    return Instant.parse(timestamp);
  }
}