package de.ggbot.core.api;

import com.google.gson.JsonObject;
import de.ggbot.sdk.core.ApiException;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class AfkHttp {

  private static final MediaType JSON = MediaType.parse("application/json");
  private static final OkHttpClient CLIENT = new OkHttpClient();

  private AfkHttp() {}

  /**
   * Processes each entry in the list, invoking the provided call for every item. Collects any exceptions and rethrows the last one encountered.
   *
   * @param entries List of entries to process
   * @param call Function to execute for each entry
   * @throws Exception if any call execution fails, rethrows the last exception encountered
   */
  public static void forEach(List<String> entries, EntryCall call) throws Exception {
    Exception failure = null;
    for (String entry : entries) {
      try {
        call.run(entry);
      } catch (Exception e) {
        failure = e;
      }
    }
    if (failure != null) throw failure;
  }

  /**
   * Executes a POST request with the given JSON payload and authentication token. Handles API errors and network exceptions.
   *
   * @param url The API endpoint URL
   * @param accessToken The OAuth access token for authentication
   * @param body The JSON payload to send with the request
   * @throws ApiException if the API answers with an error
   * @throws IOException  if the request fails
   */
  public static void post(String url, String accessToken, JsonObject body)
      throws ApiException, IOException {
    Request request = new Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer " + accessToken)
        .post(RequestBody.create(body.toString(), JSON))
        .build();
    
    try (Response response = CLIENT.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new ApiException(response.code(),
            response.body() != null ? response.body().string() : "");
      }
    }
  }

  @FunctionalInterface
  public interface EntryCall {
    void run(String entry) throws Exception;
  }
}
