package de.ggbot.core.utils;

import java.text.NumberFormat;
import java.util.Locale;
import net.labymod.api.Laby;

/**
 * Formats money amounts as human readable, locale aware strings for the shop.
 *
 * <p>The locale follows the language the user selected in the client (e.g.
 * {@code de_de} renders {@code 10.000,00}, {@code en_us} renders
 * {@code 10,000.00}), falling back to the system default locale when the
 * client language cannot be resolved. Amounts always carry exactly two
 * fraction digits and thousands grouping.
 */
public final class MoneyFormat {

  /** Client language key the cached format was built for. */
  private static String cachedLanguage;

  /** Cached number format; rebuilt when the client language changes. */
  private static NumberFormat cachedFormat;

  private MoneyFormat() {
  }

  /**
   * Formats an amount using the user's client language.
   *
   * @param amount the money amount
   * @return the formatted amount, e.g. {@code "10.000,00"} for German
   */
  public static String format(double amount) {
    return currentFormat().format(amount);
  }

  /**
   * Returns the number format for the current client language, rebuilding the
   * cached instance when the user switches languages. Only called from the
   * render/UI thread, so the shared instance needs no synchronization.
   */
  private static NumberFormat currentFormat() {
    String language = currentLanguage();
    if (cachedFormat == null || !language.equals(cachedLanguage)) {
      NumberFormat format = NumberFormat.getNumberInstance(resolveLocale(language));
      format.setGroupingUsed(true);
      format.setMinimumFractionDigits(2);
      format.setMaximumFractionDigits(2);
      cachedLanguage = language;
      cachedFormat = format;
    }
    return cachedFormat;
  }

  /** Returns the client language key (e.g. {@code "de_de"}), or {@code ""}. */
  private static String currentLanguage() {
    try {
      String language = Laby.labyAPI().minecraft().options().getCurrentLanguage();
      return language != null ? language : "";
    } catch (Exception e) {
      // Options may be unavailable very early in the client lifecycle.
      return "";
    }
  }

  /**
   * Maps a Minecraft language key like {@code "de_de"} to a {@link Locale},
   * falling back to the system default locale when the key is unusable.
   */
  private static Locale resolveLocale(String language) {
    if (language.isEmpty()) return Locale.getDefault();
    Locale locale = Locale.forLanguageTag(language.replace('_', '-'));
    // forLanguageTag returns an empty-language locale for garbage input.
    return locale.getLanguage().isEmpty() ? Locale.getDefault() : locale;
  }
}
