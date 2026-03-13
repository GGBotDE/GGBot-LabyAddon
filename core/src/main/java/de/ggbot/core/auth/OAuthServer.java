package de.ggbot.core.auth;

import com.google.gson.JsonObject;
import net.labymod.api.util.io.web.request.Request;
import net.labymod.api.util.io.web.request.Request.Method;
import net.labymod.api.util.io.web.request.Response;
import de.ggbot.core.GGBot;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * OAuth server responsible for handling the local redirect URL and retrieving
 * the OAuth authorization code and access token from the GGBot OAuth system.
 */
public class OAuthServer {

  /**
   * Port on which the local redirect server listens.
   */
  public static final int REDIRECT_PORT = 8090;

  /**
   * URL that the OAuth provider redirects to after authentication.
   */
  public static final String REDIRECT_URL = String.format("http://localhost:%s", OAuthServer.REDIRECT_PORT);

  /**
   * Client ID of this application.
   */
  public static final String CLIENT_ID = "ggbot_1c305ba092a14c11504291818f1b0c98";

  /**
   * OAuth scopes requested during authorization.
   */
  public static final String SCOPES = "read:bots%20write:bots%20execute:bots";

  private final GGBot addon;
  private final ServerSocket serverSocket;
  private final ExecutorService executor;

  /**
   * Creates a new local OAuth redirect server.
   *
   * @param addon Reference to the addon instance
   * @throws IOException if the ServerSocket cannot be opened
   */
  public OAuthServer(GGBot addon) throws IOException {
    this.addon = addon;
    this.serverSocket = new ServerSocket(OAuthServer.REDIRECT_PORT);
    this.executor = Executors.newSingleThreadExecutor();
  }

  /**
   * Waits asynchronously for the Authorization Code and passes it to the callback.
   *
   * @param callback Callback that receives the authorization code
   */
  public void listenForCodeAsync(Consumer<String> callback) {
    this.executor.execute(() -> callback.accept(this.listenForCode()));
  }

  /**
   * Waits synchronously for the OAuth redirect and extracts the authorization code.
   * A success HTML page is returned to the browser.
   *
   * @return The received authorization code, or null if an error occurred
   */
  public String listenForCode() {
    while (this.serverSocket.isBound()) {
      try {
        Socket socket = this.serverSocket.accept();
        Scanner scanner = new Scanner(socket.getInputStream());
        String path = scanner.nextLine().split(" ")[1];

        PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
        printWriter.write("HTTP/1.0 200 OK\r\n");
        printWriter.write("Content-Type: text/html; charset=UTF-8\r\n");
        printWriter.write("\r\n");
        printWriter.write("<!doctypehtml><html lang=en><meta charset=UTF-8><meta content=\"width=device-width,initial-scale=1\"name=viewport><title>Success</title><link href=https://ggbot.de/assets/css/globals.css rel=stylesheet><link href=https://unpkg.com/boxicons@2.1.4/css/boxicons.min.css rel=stylesheet><div><i class=\"bx bxs-check-circle\"></i><h1>Success!</h1><p>Your operation was completed successfully.<p class=strong>You can now close this page</div><style>i{display:block;font-size:100px;color:green}div{text-align:center;background:var(--background-100);padding:2em;border-radius:8px;box-shadow:0 4px 8px rgba(0,0,0,.1);position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);width:300px;height:300px}h1{margin-bottom:.5em;font-family:Sora,'Segoe UI',Tahoma,Geneva,Verdana,sans-serif}p{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;color:var(--text-600);margin:.5em 0}.strong{font-weight:600;color:var(--text-800)}</style>");
        printWriter.flush();

        printWriter.close();
        scanner.close();
        socket.close();
        this.close();

        if (path.contains("=") && path.contains("?code=")) {
          return path.substring(path.indexOf("=") + 1);
        } else if (path.contains("?error=")) {
          return null;
        }
      } catch (Exception e) {
        addon.getVersioningHandler().reportError(e);
        break;
      }
    }
    return null;
  }

  /**
   * Closes the local OAuth redirect server and its executor.
   */
  public void close() {
    try {
      this.serverSocket.close();
    } catch (IOException e) {
      addon.getVersioningHandler().reportError(e);
    }
  }

  /**
   * Creates the full OAuth authorization URL.
   *
   * @return URL that should be opened in the browser
   * @throws IOException if the URL is invalid
   */
  public URL getUrl() throws IOException {
    return new URL(String.format(
        "https://api.ggbot.de/oauth/authorize?response_type=token&client_id=%s&redirect_uri=%s&scope=%s",
        CLIENT_ID, REDIRECT_URL, SCOPES
    ));
  }

  /**
   * Returns the OAuth authorization URL as a string.
   *
   * @return Full authorization URL as text
   */
  public String getStringUrl() {
    return String.format(
        "https://api.ggbot.de/oauth/authorize?response_type=token&client_id=%s&redirect_uri=%s&scope=%s",
        CLIENT_ID, REDIRECT_URL, SCOPES
    );
  }

  /**
   * Fetches the access token asynchronously using the Authorization Code.
   *
   * @param code Authorization Code received from OAuth redirect
   * @param callback Callback receiving the JSON response
   */
  public void getTokenAsync(String code, Consumer<JsonObject> callback) {
    this.executor.execute(() -> callback.accept(this.getData(code)));
  }

  /**
   * Sends the token request to the OAuth API and retrieves the access token.
   *
   * @param code Authorization Code from the redirect
   * @return JSON response from the OAuth API containing the access token
   */
  public JsonObject getData(String code) {
    Map<String, String> body = new HashMap<>();
    body.put("grant_type", "authorization_code");
    body.put("code", code);
    body.put("redirect_uri", REDIRECT_URL);
    body.put("client_id", CLIENT_ID);

    Response<JsonObject> result = Request.ofGson(JsonObject.class)
        .method(Method.POST)
        .url("https://api.ggbot.de/oauth/token")
        .body(body)
        .executeSync();
    return result.get();
  }
}