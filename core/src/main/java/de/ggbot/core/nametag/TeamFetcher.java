package de.ggbot.core.nametag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import de.ggbot.core.GGBot;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TeamFetcher {
  /** Default delay between automatic re-fetches (24 hours, in milliseconds). */
  private static final long REFETCH_INTERVAL_MS = 24L * 60L * 60L * 1000L;

  /** Feature key used for the server-controlled refetch pacing. */
  private static final String FEATURE = "de.ggbot.addon.nametag";

  /**
   * Provides the API endpoint for fetching in-game tag data.
   */
  private static final String API_ENDPOINT = "https://msapi.ggbot.de/all";

  /**
   * Defines the default team tag color when no specific color is set.
   */
  private static final String DEFAULT_TEAM_COLOR = "#2ecc71";

  /**
   * Ensures compatibility with plain strings for tags without translations.
   */
  private static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(GGBotTeam.LocalizedText.class,
          (JsonDeserializer<GGBotTeam.LocalizedText>) (json, type, context) ->
              json.isJsonPrimitive()
                  ? new GGBotTeam.LocalizedText(json.getAsString())
                  : new Gson().fromJson(json, GGBotTeam.LocalizedText.class))
      .create();

  /** The globally accessible instance set on construction. */
  public static TeamFetcher teamInstance;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static boolean hasFetched = false;
  private volatile GGBotTeam team;
  private volatile Map<String, GGBotTag> tags = new HashMap<>();

  public TeamFetcher() {
    teamInstance = this;
  }

  /**
   * Fetches team data from the API and caches it locally.
   * On the first call, also registers the periodic auto-refresh interval.
   */
  public void fetch() {
    if (!hasFetched) scheduleNextFetch();
    hasFetched = true;

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(API_ENDPOINT))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        GGBotTeam parsed = GSON.fromJson(response.body(), GGBotTeam.class);
        this.tags = buildTags(parsed);
        this.team = parsed;
      } else {
        GGBot.getInstance().logger().error("Failed to fetch data. Response code: " + response.statusCode());
      }

    } catch (IOException | InterruptedException | JsonParseException e) {
      GGBot.getInstance().logger().error("Error fetching team data: " + e.getMessage());
      GGBot.getInstance().getVersioningHandler().reportError(e);
    }
  }

  /**
   * Indexes all tagged players by their UUID, prioritizing team data over group data.
   *
   * @param data the team data used to build the tag index
   * @return a map of player UUIDs to their corresponding tag information
   */
  private static Map<String, GGBotTag> buildTags(GGBotTeam data) {
    Map<String, GGBotTag> result = new HashMap<>();
    if (data.getGroups() != null) {
      for (GGBotTeam.GroupDTO group : data.getGroups()) {
        addMembers(result, group.getMembers(), group.getTag(), group.getTagColor(),
            group.getIngameIcon());
      }
    }
    GGBotTeam.TeamDTO team = data.getTeam();
    if (team != null && team.getDepartments() != null) {
      for (GGBotTeam.DepartmentDTO department : team.getDepartments()) {
        addMembers(result, department.getMembers(), team.getTag(),
            first(department.getTagColor(), team.getTagColor(), DEFAULT_TEAM_COLOR),
            first(department.getIngameIcon(), team.getIngameIcon()));
      }
    }
    return result;
  }

  private static void addMembers(Map<String, GGBotTag> result, List<GGBotTeam.MemberDTO> members,
      GGBotTeam.LocalizedText tag, String color, String icon) {
    if (members == null) return;
    for (GGBotTeam.MemberDTO member : members) {
      if (member.getMinecraft() == null || member.getMinecraft().getUuid() == null) continue;
      result.put(normalize(member.getMinecraft().getUuid()), new GGBotTag(
          member.getTag() != null ? member.getTag() : tag,
          first(member.getTagColor(), color),
          first(member.getIngameIcon(), icon)));
    }
  }

  private static String first(String... values) {
    for (String value : values) {
      if (value != null && !value.isEmpty()) return value;
    }
    return null;
  }

  private static String normalize(String uuid) {
    return uuid.replace("-", "").toLowerCase(Locale.ROOT);
  }

  /**
   * Schedules the next automatic re-fetch. The delay is re-read from the
   * server-controlled interval pacing on every hop, so the backend can adjust
   * how often this endpoint is polled without a new addon build.
   */
  private void scheduleNextFetch() {
    long delayMs = GGBot.getInstance().getVersioningHandler()
        .clampIntervalMs(FEATURE, REFETCH_INTERVAL_MS);
    scheduler.schedule(this::runScheduledFetch, delayMs, TimeUnit.MILLISECONDS);
  }

  /**
   * Scheduled fetch hop. The event bus stops delivering events to disabled
   * addons, so this scheduler would otherwise keep polling after the user
   * disables the addon mid-session - re-check the enabled switch on every hop
   * and only reschedule, never fetch, while disabled.
   */
  private void runScheduledFetch() {
    if (Boolean.TRUE.equals(GGBot.getInstance().configuration().enabled().get())) {
      fetch();
    }
    scheduleNextFetch();
  }

  /**
   * Provides access to the most recently fetched team data.
   *
   * @return the GGBotTeam or null if not yet fetched
   */
  public GGBotTeam getTeam() {
    return team;
  }

  /**
   * Retrieves the tag associated with a player.
   *
   * @param uuid the player's UUID
   * @return the tag, or {@code null} if the player has none
   */
  public GGBotTag getTag(UUID uuid) {
    return tags.get(normalize(uuid.toString()));
  }

  /**
   * Provides the tag displayed on GGBots.
   *
   * @return the tag, or {@code null} if bot tags are disabled
   */
  public GGBotTag getBotTag() {
    GGBotTeam data = team;
    if (data == null || data.getBots() == null
        || !Boolean.TRUE.equals(data.getBots().getEnabled())) {
      return null;
    }
    return new GGBotTag(null, data.getBots().getColor(), data.getBots().getIcon());
  }

  /**
   * Returns whether team data has been successfully fetched at least once.
   *
   * @return {@code true} if team data is available
   */
  public boolean isFetched() {
    return team != null;
  }
}

