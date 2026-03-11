package de.ggbot.core.api.versioning;

import de.ggbot.core.api.versioning.response.CurrentVersion;
import de.ggbot.core.api.versioning.response.FeatureFlagResponse;
import de.ggbot.core.api.versioning.response.MessageResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * Performs an async version-check request for the specified addon.
     *
     * <p>Sends {@code POST /v1/check/:addonSlug} with the serialised {@code request} body
     * and deserialises the JSON response into a {@link VersionCheckResponse}.
     *
     * <p>The future completes exceptionally with an {@link ApiException} if:
     * <ul>
     *   <li>The HTTP status code is not 2xx</li>
     *   <li>A network or I/O error occurs</li>
     * </ul>
     *
     * @param addonSlug the addon's URL slug, e.g. {@code "my-addon"}
     * @param request   the populated version-check request
     * @return a {@link CompletableFuture} that completes with the parsed response
     */
    public CompletableFuture<VersionCheckResponse> check(String addonSlug, VersionCheckRequest request) {
        String url = baseUrl + "/v1/check/" + addonSlug;
        String body = request.toJson();
        return CompletableFuture.supplyAsync(() -> {
            String json = doPost(url, body);
            return parseVersionCheckResponse(json);
        }, executor);
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
    // JSON parsing (minimal, no external libraries)
    // -------------------------------------------------------------------------

    /**
     * Parses a {@link VersionCheckResponse} from a raw JSON string.
     *
     * <p><b>Note:</b> This is a minimal, hand-written JSON parser sufficient for the
     * known API response structure. It handles nested objects and arrays but is
     * intentionally simple – for complex or deeply nested responses consider
     * integrating a proper JSON library (e.g. Gson or Jackson) once available.
     *
     * @param json raw JSON string from the API
     * @return parsed {@link VersionCheckResponse}
     */
    private static VersionCheckResponse parseVersionCheckResponse(String json) {
        VersionCheckResponse resp = new VersionCheckResponse();

        resp.setSupported(extractJsonBoolean(json, "supported"));
        resp.setMessage(extractJsonString(json, "message"));

        // --- currentVersion ---
        String cvBlock = extractJsonObject(json, "currentVersion");
        if (cvBlock != null) {
            CurrentVersion cv = new CurrentVersion();
            cv.setVersion(extractJsonString(cvBlock, "version"));
            cv.setFileHash(extractJsonString(cvBlock, "fileHash"));
            cv.setManualDownload(extractJsonString(cvBlock, "manualDownload"));
            cv.setManualDownloadDirect(extractJsonString(cvBlock, "manualDownloadDirect"));
            cv.setOfficiallyReleased(extractJsonBoolean(cvBlock, "isOfficiallyReleased"));
            cv.setFlintReleased(extractJsonBoolean(cvBlock, "isFlintReleased"));
            cv.setWillBeFlintReleased(extractJsonBoolean(cvBlock, "willBeFlintReleased"));
            resp.setCurrentVersion(cv);
        }

        // --- features ---
        String featuresBlock = extractJsonObject(json, "features");
        if (featuresBlock != null) {
            Map<String, FeatureFlagResponse> features = new HashMap<>();
            // Each key is a feature flag name; values are objects with "enabled" and optional "versionCompatabilityConversion"
            List<String[]> pairs = extractJsonObjectEntries(featuresBlock);
            for (String[] pair : pairs) {
                String key = pair[0];
                String value = pair[1];
                FeatureFlagResponse ff = new FeatureFlagResponse();
                ff.setEnabled(extractJsonBoolean(value, "enabled"));
                String compatBlock = extractJsonObject(value, "versionCompatabilityConversion");
                if (compatBlock != null) {
                    ff.setVersionCompatabilityConversionPath(extractJsonString(compatBlock, "path"));
                }
                features.put(key, ff);
            }
            resp.setFeatures(features);
        }

        // --- messages ---
        String messagesArray = extractJsonArray(json, "messages");
        if (messagesArray != null) {
            List<MessageResponse> messages = new ArrayList<>();
            List<String> items = splitJsonArray(messagesArray);
            for (String item : items) {
                MessageResponse msg = new MessageResponse();
                msg.setEnabledUntil(extractJsonString(item, "enabledUntil"));
                msg.setUuid(extractJsonString(item, "uuid"));
                msg.setLocale(extractJsonString(item, "locale"));
                msg.setMessage(extractJsonString(item, "message"));
                msg.setLink(extractJsonString(item, "link"));
                msg.setShowAfter(extractJsonInt(item, "showAfter"));
                msg.setShowOnce(extractJsonBoolean(item, "showOnce"));
                msg.setOnlyShowAfterInteraction(extractJsonBoolean(item, "onlyShowAfterInteraction"));
                msg.setToast(extractJsonBoolean(item, "isToast"));
                msg.setPopup(extractJsonBooleanDefault(item, "isPopup", true));
                messages.add(msg);
            }
            resp.setMessages(messages);
        }

        return resp;
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
     * Extracts a boolean value for the given key from a flat JSON object string.
     * Returns {@code false} if the key is not found.
     *
     * @param json JSON object string
     * @param key  field name
     * @return boolean value
     */
    private static boolean extractJsonBoolean(String json, String key) {
        return extractJsonBooleanDefault(json, key, false);
    }

    /**
     * Extracts a boolean value with a specified default.
     *
     * @param json         JSON object string
     * @param key          field name
     * @param defaultValue value to return if key is absent
     * @return boolean value or {@code defaultValue}
     */
    private static boolean extractJsonBooleanDefault(String json, String key, boolean defaultValue) {
        if (json == null) return defaultValue;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultValue;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return defaultValue;
        // Skip whitespace
        int val = colon + 1;
        while (val < json.length() && Character.isWhitespace(json.charAt(val))) val++;
        if (json.startsWith("true", val)) return true;
        if (json.startsWith("false", val)) return false;
        return defaultValue;
    }

    /**
     * Extracts an integer value for the given key. Returns {@code 0} if not found.
     *
     * @param json JSON object string
     * @param key  field name
     * @return integer value or {@code 0}
     */
    private static int extractJsonInt(String json, String key) {
        if (json == null) return 0;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return 0;
        int val = colon + 1;
        while (val < json.length() && Character.isWhitespace(json.charAt(val))) val++;
        int end = val;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Integer.parseInt(json.substring(val, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Extracts the raw content of a nested JSON object for the given key.
     * Returns the inner content string (without outer braces) or {@code null} if not found.
     *
     * @param json JSON string
     * @param key  field name
     * @return nested object string content, or {@code null}
     */
    private static String extractJsonObject(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int brace = json.indexOf('{', colon + 1);
        if (brace < 0) return null;
        return extractBalanced(json, brace, '{', '}');
    }

    /**
     * Extracts the content of a JSON array for the given key.
     * Returns the raw array content (without outer brackets) or {@code null}.
     *
     * @param json JSON string
     * @param key  field name
     * @return array content string, or {@code null}
     */
    private static String extractJsonArray(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int bracket = json.indexOf('[', colon + 1);
        if (bracket < 0) return null;
        return extractBalanced(json, bracket, '[', ']');
    }

    /**
     * Extracts a balanced-bracketed substring starting at {@code startIdx}.
     * Handles nesting and quoted strings (skips brackets inside strings).
     *
     * @param json     source string
     * @param startIdx index of the opening bracket/brace
     * @param open     opening character
     * @param close    closing character
     * @return full balanced substring including outer delimiters, or {@code null}
     */
    private static String extractBalanced(String json, int startIdx, char open, char close) {
        int depth = 0;
        boolean inString = false;
        for (int i = startIdx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; } // skip escaped char
                if (c == '"') inString = false;
            } else {
                if (c == '"') { inString = true; continue; }
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) return json.substring(startIdx, i + 1);
                }
            }
        }
        return null;
    }

    /**
     * Splits a JSON array string (including outer brackets) into individual element strings.
     * Handles nested objects/arrays and string values.
     *
     * @param arrayJson full array JSON string, e.g. {@code "[{...},{...}]"}
     * @return list of element string tokens
     */
    private static List<String> splitJsonArray(String arrayJson) {
        List<String> items = new ArrayList<>();
        if (arrayJson == null || arrayJson.length() < 2) return items;
        String inner = arrayJson.substring(1, arrayJson.length() - 1).trim();
        if (inner.isEmpty()) return items;

        int depth = 0;
        boolean inString = false;
        int start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    items.add(inner.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        String last = inner.substring(start).trim();
        if (!last.isEmpty()) items.add(last);
        return items;
    }

    /**
     * Extracts all key-value pairs from a flat JSON object string.
     * Values are returned as raw JSON tokens (string, number, boolean, object, or array).
     *
     * @param objectJson full JSON object including outer braces
     * @return list of {@code String[2]} arrays where {@code [0]} is the key and {@code [1]} is the raw value
     */
    private static List<String[]> extractJsonObjectEntries(String objectJson) {
        List<String[]> entries = new ArrayList<>();
        if (objectJson == null || objectJson.length() < 2) return entries;
        String inner = objectJson.substring(1, objectJson.length() - 1).trim();
        if (inner.isEmpty()) return entries;

        int i = 0;
        while (i < inner.length()) {
            // Skip whitespace and commas
            while (i < inner.length() && (Character.isWhitespace(inner.charAt(i)) || inner.charAt(i) == ',')) i++;
            if (i >= inner.length()) break;

            // Read key (must be a quoted string)
            if (inner.charAt(i) != '"') break;
            int keyStart = i + 1;
            i++;
            while (i < inner.length() && inner.charAt(i) != '"') {
                if (inner.charAt(i) == '\\') i++;
                i++;
            }
            String key = inner.substring(keyStart, i);
            i++; // skip closing quote

            // Skip colon
            while (i < inner.length() && (Character.isWhitespace(inner.charAt(i)) || inner.charAt(i) == ':')) i++;

            // Read value
            String value;
            if (i >= inner.length()) break;
            char vc = inner.charAt(i);
            if (vc == '{' || vc == '[') {
                char closeChar = vc == '{' ? '}' : ']';
                value = extractBalanced(inner, i, vc, closeChar);
                if (value == null) break;
                i += value.length();
            } else if (vc == '"') {
                int vs = i;
                i++;
                while (i < inner.length()) {
                    char c = inner.charAt(i);
                    if (c == '\\') { i += 2; continue; }
                    if (c == '"') { i++; break; }
                    i++;
                }
                value = inner.substring(vs, i);
            } else {
                // Number, boolean, null
                int vs = i;
                while (i < inner.length() && inner.charAt(i) != ',' && inner.charAt(i) != '}') i++;
                value = inner.substring(vs, i).trim();
            }
            entries.add(new String[]{key, value});
        }
        return entries;
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
