package de.ggbot.core.utils;

/** Small text helpers. */
public final class TextUtil {

  private TextUtil() {}

  /** Removes legacy Minecraft formatting codes ({@code §x}) from a string. */
  public static String stripColors(String input) {
    if (input == null) return "";
    return input.replaceAll("(?i)§[0-9A-FK-OR]", "");
  }
}
