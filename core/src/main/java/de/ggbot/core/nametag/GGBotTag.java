package de.ggbot.core.nametag;

/**
 * Represents a player's tag after applying fallbacks for member, role, and team.
 */
public final class GGBotTag {

  private final GGBotTeam.LocalizedText text;
  private final String color;
  private final String iconUrl;

  /**
   * Constructs a GGBotTag with text, color, and icon URL.
   * @param text the tag text, may be null
   * @param color the tag color as hex, may be null
   * @param iconUrl the icon URL, null for the client's own logo
   */
  public GGBotTag(GGBotTeam.LocalizedText text, String color, String iconUrl) {
    this.text = text;
    this.color = color;
    this.iconUrl = iconUrl;
  }

  /**
   * Returns the localized tag text, which may be null.
   * @return the tag text, may be null
   */
  public GGBotTeam.LocalizedText getText() { return text; }

  /**
   * Returns the tag color in hex format, which may be null.
   * @return the tag color as hex, may be null
   */
  public String getColor() { return color; }

  /**
   * Returns the icon URL, or null if using the client's default logo.
   * @return the icon URL, null for the client's own logo
   */
  public String getIconUrl() { return iconUrl; }
}
