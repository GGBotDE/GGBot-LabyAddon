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
  private static final int refetchInterval = 24; // Fetch every 24h to also update non-restarting clients.
  private static final String apiEndpoint = "https://msapi.ggbot.de/team";

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static boolean hasFetched = false;
  public static TeamFetcher teamInstance;
  private GGBotTeam team;


  public TeamFetcher() {
    teamInstance = this;
  }

  public void fetch() {
    if(!hasFetched) registerAutoFetchingInterval();
    hasFetched = true;

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiEndpoint))
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
      e.printStackTrace();
      GGBot.getInstance().getVersioningHandler().reportError(e);
    }
  }

  private void registerAutoFetchingInterval() {
    scheduler.scheduleAtFixedRate(this::fetch, refetchInterval, refetchInterval, TimeUnit.HOURS);
  }

  public GGBotTeam getTeam() {
    return team;
  }

  public boolean isFetched() {
    return team != null;
  }
}

