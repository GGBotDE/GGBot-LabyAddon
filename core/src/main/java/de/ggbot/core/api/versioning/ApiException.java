package de.ggbot.core.api.versioning;

/**
 * Thrown when the Versioning API returns a non-2xx HTTP status code
 * or when a network/IO error occurs during a request.
 *
 * <p>Example usage:
 * <pre>{@code
 * client.check("my-addon", req).exceptionally(ex -> {
 *     if (ex.getCause() instanceof ApiException apiEx) {
 *         System.err.println("HTTP " + apiEx.getStatusCode() + ": " + apiEx.getResponseBody());
 *     }
 *     return null;
 * });
 * }</pre>
 */
public class ApiException extends RuntimeException {

    /** The HTTP status code returned by the server, or -1 if unknown. */
    private final int statusCode;

    /** The raw response body returned by the server, may be {@code null}. */
    private final String responseBody;

    /**
     * Constructs an {@code ApiException} with a message, HTTP status code, and response body.
     *
     * @param message      human-readable description of the error
     * @param statusCode   HTTP status code ({@code -1} if not applicable)
     * @param responseBody raw response body from the server, may be {@code null}
     */
    public ApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Constructs an {@code ApiException} wrapping an underlying cause (e.g. {@link java.io.IOException}).
     *
     * @param message human-readable description of the error
     * @param cause   the underlying exception
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    /**
     * Returns the HTTP status code returned by the server.
     *
     * @return HTTP status code, or {@code -1} if the error was not HTTP-related
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the raw HTTP response body received from the server.
     *
     * @return response body string, or {@code null} if unavailable
     */
    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        return "ApiException{statusCode=" + statusCode + ", body=" + responseBody + ", message=" + getMessage() + "}";
    }
}
