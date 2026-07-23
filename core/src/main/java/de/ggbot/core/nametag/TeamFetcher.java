package de.ggbot.core.nametag;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import de.ggbot.core.GGBot;
import net.labymod.api.Laby;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TeamFetcher {
  /** Default delay between automatic re-fetches (24 hours, in milliseconds). */
  private static final long REFETCH_INTERVAL_MS = 24L * 60L * 60L * 1000L;

  /** Feature key used for the server-controlled refetch pacing. */
  private static final String FEATURE = "de.ggbot.addon.nametag";

  /** API endpoint for team data. */
  private static final String API_ENDPOINT = "https://msapi.ggbot.de/team";

  /** The globally accessible instance set on construction. */
  public static TeamFetcher teamInstance;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static boolean hasFetched = false;
  private GGBotTeam team;


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
        String jsonString = response.body();
        this.team = new Gson().fromJson(jsonString, GGBotTeam.class);
      } else {
        GGBot.getInstance().logger().error("Failed to fetch data. Response code: " + response.statusCode());
      }

    } catch (IOException | InterruptedException e) {
      GGBot.getInstance().logger().error("Error fetching team data: " + e.getMessage());
      GGBot.getInstance().getVersioningHandler().reportError(e);
    }
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
   * Returns the most recently fetched team data.
   *
   * @return the {@link GGBotTeam} or {@code null} if not yet fetched
   */
  public GGBotTeam getTeam() {
    return team;
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

