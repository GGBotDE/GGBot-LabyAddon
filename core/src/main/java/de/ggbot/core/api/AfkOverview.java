package de.ggbot.core.api;

import de.ggbot.sdk.model.AfkBot;
import de.ggbot.sdk.model.AfkFleet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Contains all AFK fleets and their associated AFK bots for the linked account.
 */
public final class AfkOverview {

  private final List<AfkFleet> fleets;
  private final List<AfkBot> bots;

  /**
   * Initializes an AfkOverview object with the provided fleets and bots collection.
   *
   * @param fleets List of AFK fleets associated with the account
   * @param bots List of AFK bots linked to the account
   */
  public AfkOverview(List<AfkFleet> fleets, List<AfkBot> bots) {
    this.fleets = fleets;
    this.bots = bots;
  }

  /**
   * Determines if the bot's current status is set to an online session.
   *
   * @param bot The AfkBot object to check
   * @return True if the bot is logged in, false otherwise
   */
  public static boolean isOnline(AfkBot bot) {
    return bot.getStatus() == AfkBot.StatusEnum.LOGGED_IN;
  }

  /**
   * Counts the number of bots in the provided list that are currently logged in.
   *
   * @param bots List of AfkBot objects to evaluate
   * @return The count of bots that are online
   */
  public static int onlineCount(List<AfkBot> bots) {
    int online = 0;
    for (AfkBot bot : bots) {
      if (isOnline(bot)) online++;
    }
    return online;
  }

  /**
   * Selects bots from the list that have token identifiers matching the provided set.
   *
   * @param bots List of AfkBot objects to filter
   * @param picked Set of token identifiers to match
   * @return List of selected bots with matching tokens
   */
  public static List<AfkBot> pick(List<AfkBot> bots, Set<String> picked) {
    if (picked.isEmpty()) return bots;

    List<AfkBot> result = new ArrayList<>();
    for (AfkBot bot : bots) {
      if (picked.contains(bot.getToken())) result.add(bot);
    }
    return result;
  }

  /**
   * Collects token identifiers from bots that satisfy the given filtering condition.
   *
   * @param bots List of AfkBot objects to process
   * @param filter Predicate to determine which bots are included
   * @return List of token IDs from matching bots
   */
  public static List<String> tokens(List<AfkBot> bots, Predicate<AfkBot> filter) {
    List<String> result = new ArrayList<>();
    for (AfkBot bot : bots) {
      if (filter.test(bot)) result.add(bot.getToken());
    }
    return result;
  }

  /**
   * Provides access to the list of AFK fleets associated with this account.
   *
   * @return The list of fleets linked to the account
   */
  public List<AfkFleet> getFleets() {
    return fleets;
  }

  /**
   * Provides access to the list of AFK bots associated with this account.
   *
   * @return The list of bots linked to the account
   */
  public List<AfkBot> getBots() {
    return bots;
  }

  /**
   * Filters and returns bots that are associated with the specified fleet.
   *
   * @param fleet The AfkFleet object to find matching bots for
   * @return List of bots belonging to the specified fleet
   */
  public List<AfkBot> botsOf(AfkFleet fleet) {
    List<AfkBot> result = new ArrayList<>();
    if (fleet == null) return result;
    
    for (AfkBot bot : bots) {
      if (Objects.equals(bot.getFleet(), fleet.getId())) result.add(bot);
    }
    return result;
  }

  /**
   * Locates a fleet by matching the input string against fleet names or IDs, preferring the longest match.
   *
   * @param input The search string to match against fleet names or IDs
   * @return The best matching fleet object, or null if no match is found
   */
  public AfkFleet findByPrefix(String input) {
    AfkFleet best = null;
    int bestLength = -1;
    for (AfkFleet fleet : fleets) {
      for (String label : new String[]{fleet.getName(), String.valueOf(fleet.getId())}) {
        if (label != null && label.length() > bestLength && startsWithWord(input, label)) {
          best = fleet;
          bestLength = label.length();
        }
      }
    }
    return best;
  }

  /**
   * Retrieves the text that comes after the fleet's name or ID in the input string.
   *
   * @param input The string to search for the fleet's name or ID
   * @param fleet The AfkFleet object to match against
   * @return The substring following the matched fleet identifier, trimmed
   */
  public static String textAfter(String input, AfkFleet fleet) {
    for (String label : new String[]{fleet.getName(), String.valueOf(fleet.getId())}) {
      if (label != null && startsWithWord(input, label)) {
        return input.substring(label.length()).trim();
      }
    }
    return "";
  }

  private static boolean startsWithWord(String input, String label) {
    return input.regionMatches(true, 0, label, 0, label.length())
        && (input.length() == label.length() || input.charAt(label.length()) == ' ');
  }

  /**
   * Locates a fleet by its numeric ID, returning null if no matching fleet is found.
   *
   * @param id The unique identifier of the fleet to retrieve
   * @return The matching AfkFleet object, or null if not found
   */
  public AfkFleet findFleet(Integer id) {
    for (AfkFleet fleet : fleets) {
      if (Objects.equals(fleet.getId(), id)) return fleet;
    }
    return null;
  }
}
