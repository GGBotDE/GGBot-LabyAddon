package org.openapitools.client;

import com.google.gson.annotations.SerializedName;

public class BotLogEntry {
  @SerializedName("message")
  private String message;

  @SerializedName("type")
  private String type;

  public String getMessage() { return message; }
  public String getType() { return type; }
}
