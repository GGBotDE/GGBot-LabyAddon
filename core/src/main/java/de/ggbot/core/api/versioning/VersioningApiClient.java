package de.ggbot.core.api.versioning;

import com.google.gson.Gson;
import de.ggbot.core.api.versioning.matrix.MatrixResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async HTTP client for the Versioning API.
 *
 * <p>All network operations are executed on a shared daemon thread pool and return
 * {@link CompletableFuture} instances. No external libraries are required – only
 * standard {@code java.net} and {@code java.io} APIs are used.
 *
 * <p>Create a single instance per application and reuse it:
 * <pre>{@code
 * VersioningApiClient client = new VersioningApiClient("https://api.example.com");
 * // Optional: customise timeout and thread pool
 * VersioningApiClient client = new VersioningApiClient(
 *     "https://api.example.com",
 *     5000,    // connect timeout ms
 *     10000,   // read timeout ms
 *     4        // thread pool size
 * );
 * }</pre>
 *
 * <p>Remember to call {@link #shutdown()} when the client is no longer needed to
 * release thread-pool resources.
 */
public class VersioningApiClient {

    /** Base URL of the API server, without trailing slash. */
    private final String baseUrl;

    /** HTTP connection timeout in milliseconds. */
    private final int connectTimeoutMs;

    /** HTTP read timeout in milliseconds. */
    private final int readTimeoutMs;

    /**
     * Thread pool used for async HTTP requests.
     * Uses daemon threads so they won't prevent JVM shutdown.
     */
    private final ExecutorService executor;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a client with default settings:
     * connect timeout 5 s, read timeout 10 s, 2 worker threads.
     *
     * @param baseUrl API base URL, e.g. {@code "https://api.example.com"}
     */
    public VersioningApiClient(String baseUrl) {
        this(baseUrl, 5_000, 10_000, 2);
    }

    /**
     * Creates a fully customised client.
     *
     * @param baseUrl          API base URL, e.g. {@code "https://api.example.com"}
     * @param connectTimeoutMs HTTP connection timeout in milliseconds
     * @param readTimeoutMs    HTTP read timeout in milliseconds
     * @param threadPoolSize   number of threads in the async worker pool
     */
    public VersioningApiClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs, int threadPoolSize) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        // Daemon threads: they won't prevent JVM shutdown
        this.executor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "versioning-api-worker");
            t.setDaemon(true);
            return t;
        });
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the base URL used by this client.
     *
     * @return base URL string
     */
    public String getBaseUrl() { return baseUrl; }

    /**
     * Returns the HTTP connect timeout.
     *
     * @return connect timeout in milliseconds
     */
    public int getConnectTimeoutMs() { return connectTimeoutMs; }

    /**
     * Returns the HTTP read timeout.
     *
     * @return read timeout in milliseconds
     */
    public int getReadTimeoutMs() { return readTimeoutMs; }

    // -------------------------------------------------------------------------
    // Public API methods
    // -------------------------------------------------------------------------

    /**
     * Fetches the server's rule matrix via {@code GET /v1/matrix/:addonSlug}.
     *
     * <p>This request intentionally carries no environment data - the client
     * evaluates the returned rules locally (see
     * {@link de.ggbot.core.api.versioning.matrix.ClientRuleEngine}). Completes
     * exceptionally with an {@link ApiException} on HTTP errors or network
     * failure. There is deliberately no client-side fallback to the legacy
     * {@code POST /v1/check} endpoint; that endpoint only remains on the
     * backend for older addon builds.
     *
     * @param addonSlug the addon's URL slug
     * @return a {@link CompletableFuture} that completes with the parsed matrix
     */
    public CompletableFuture<MatrixResponse> matrix(String addonSlug) {
        String url = baseUrl + "/v1/matrix/" + addonSlug;
        return CompletableFuture.supplyAsync(() -> {
            String json = doGet(url);
            return new Gson().fromJson(json, MatrixResponse.class);
        }, executor);
    }

    /**
     * Sends the optional, opt-out version report via
     * {@code POST /v1/versionreport/:addonSlug}, fire-and-forget style.
     *
     * <p>Callers must gate this on the user's version report setting; the
     * request body carries the full environment context that the mandatory
     * matrix request deliberately no longer sends.
     *
     * @param addonSlug the addon's URL slug
     * @param request   the populated environment report
     * @return a {@link CompletableFuture} completing when the report was sent
     */
    public CompletableFuture<Void> versionReport(String addonSlug, VersionCheckRequest request) {
        String url = baseUrl + "/v1/versionreport/" + addonSlug;
        String body = request.toJson();
        return CompletableFuture.runAsync(() -> doPost(url, body), executor);
    }

    /**
     * Performs an async error-report request for the specified addon.
     *
     * <p>Sends {@code POST /v1/error/:addonSlug} fire-and-forget style.
     * The returned future resolves with the server-assigned report UUID string,
     * or an empty string if the server did not return an ID.
     *
     * <p>The future completes exceptionally with an {@link ApiException} on HTTP errors
     * or network failures.
     *
     * @param addonSlug the addon's URL slug, e.g. {@code "my-addon"}
     * @param report    the populated error report request
     * @return a {@link CompletableFuture} that resolves with the report UUID string
     */
    public CompletableFuture<String> reportError(String addonSlug, ErrorReportRequest report) {
        String url = baseUrl + "/v1/error/" + addonSlug;
        String body = report.toJson();
        return CompletableFuture.supplyAsync(() -> {
            String json = doPost(url, body);
            // Extract id field from {"ok":true,"id":"..."}
            return extractJsonString(json, "id");
        }, executor);
    }

    /**
     * Performs a health check against {@code GET /health}.
     *
     * @return a {@link CompletableFuture} that resolves with {@code true} if the server is healthy
     */
    public CompletableFuture<Boolean> healthCheck() {
        String url = baseUrl + "/health";
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection conn = openConnection(url, "GET");
                int status = conn.getResponseCode();
                conn.disconnect();
                return status >= 200 && status < 300;
            } catch (IOException e) {
                return false;
            }
        }, executor);
    }

    /**
     * Shuts down the internal thread pool.
     * Should be called when this client is no longer needed.
     * After calling this method, any further requests will be rejected.
     */
    public void shutdown() {
        executor.shutdown();
    }

    // -------------------------------------------------------------------------
    // HTTP helpers (private)
    // -------------------------------------------------------------------------

    /**
     * Opens an {@link HttpURLConnection} for the given URL and method.
     * Applies the configured connect and read timeouts.
     *
     * @param urlString the full URL to connect to
     * @param method    HTTP method string, e.g. {@code "GET"} or {@code "POST"}
     * @return a configured (but not yet connected) {@link HttpURLConnection}
     * @throws IOException if the URL is malformed or a connection error occurs
     */
    private HttpURLConnection openConnection(String urlString, String method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "LabyModVersioningClient/1.0 Java/" + System.getProperty("java.version"));
        return conn;
    }

    /**
     * Sends a GET request and returns the response body as a string.
     * Throws {@link ApiException} for non-2xx responses or I/O errors.
     *
     * @param urlString the full URL to GET
     * @return response body as a UTF-8 string
     * @throws ApiException if the server returns a non-2xx status or an I/O error occurs
     */
    private String doGet(String urlString) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(urlString, "GET");
            int status = conn.getResponseCode();

            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String responseBody = is != null ? readStream(is) : "";

            if (status < 200 || status >= 300) {
                throw new ApiException(
                        "API request failed with HTTP " + status + " for URL: " + urlString,
                        status, responseBody
                );
            }
            return responseBody;

        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("Network error during GET to: " + urlString, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Sends a POST request with a JSON body and returns the response body as a string.
     * Throws {@link ApiException} for non-2xx responses or I/O errors.
     *
     * @param urlString the full URL to POST to
     * @param jsonBody  the JSON request body string
     * @return response body as a UTF-8 string
     * @throws ApiException if the server returns a non-2xx status or an I/O error occurs
     */
    private String doPost(String urlString, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(urlString, "POST");
            conn.setDoOutput(true);

            // Write request body
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }

            int status = conn.getResponseCode();

            // Read response (success or error stream)
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String responseBody = is != null ? readStream(is) : "";

            if (status < 200 || status >= 300) {
                throw new ApiException(
                        "API request failed with HTTP " + status + " for URL: " + urlString,
                        status, responseBody
                );
            }
            return responseBody;

        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("Network error during POST to: " + urlString, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Reads all bytes from an {@link InputStream} and returns them as a UTF-8 string.
     *
     * @param is the stream to read
     * @return string content
     * @throws IOException on I/O error
     */
    private static String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = is.read(chunk)) != -1) {
            buf.write(chunk, 0, read);
        }
        return buf.toString(StandardCharsets.UTF_8.name());
    }

    // -------------------------------------------------------------------------
    // Minimal JSON extraction utilities (private static)
    // -------------------------------------------------------------------------

    /**
     * Extracts a string value for the given key from a flat JSON object string.
     * Returns {@code null} if the key is not found.
     *
     * @param json JSON object string
     * @param key  field name
     * @return string value, or {@code null}
     */
    private static String extractJsonString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = start + 1;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') { end += 2; continue; }
            if (c == '"') break;
            end++;
        }
        return unescape(json.substring(start + 1, end));
    }

    /**
     * Unescapes JSON string escape sequences (e.g. {@code \\n} → newline).
     *
     * @param s escaped JSON string content (without surrounding quotes)
     * @return unescaped Java string
     */
    private static String unescape(String s) {
        if (s == null || !s.contains("\\")) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            try {
                                int codePoint = Integer.parseInt(s.substring(i + 1, i + 5), 16);
                                sb.append((char) codePoint);
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append('\\').append(next);
                            }
                        }
                        break;
                    default: sb.append('\\').append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
