package de.ggbot.core.api.versioning.response;

/**
 * Server-controlled pacing for a repeating client feature (polling timers,
 * cached fetch loops). Served per feature flag by the backend.
 *
 * <p>The backend can set a minimum, maximum, default and multiplier for every
 * repeating feature. User-configurable intervals stay user-configurable, but
 * are multiplied by {@code multiplier} and clamped into
 * {@code [minMs, maxMs]}; {@code defaultMs} is used when no interval was
 * requested. This lets the backend throttle (or speed up) how often the API
 * gets polled without shipping a new addon build.
 */
public class IntervalConfig {

    /** Minimum allowed interval in milliseconds, or {@code null} for no minimum. */
    private Long minMs;

    /** Maximum allowed interval in milliseconds, or {@code null} for no maximum. */
    private Long maxMs;

    /** Interval to use when the caller has no own value, or {@code null}. */
    private Long defaultMs;

    /** Factor applied to the requested interval before clamping, or {@code null}. */
    private Double multiplier;

    /** Returns the minimum allowed interval in milliseconds, or {@code null}. */
    public Long getMinMs() { return minMs; }

    /** Returns the maximum allowed interval in milliseconds, or {@code null}. */
    public Long getMaxMs() { return maxMs; }

    /** Returns the default interval in milliseconds, or {@code null}. */
    public Long getDefaultMs() { return defaultMs; }

    /** Returns the interval multiplier, or {@code null}. */
    public Double getMultiplier() { return multiplier; }

    /** Sets the minimum allowed interval. */
    public IntervalConfig setMinMs(Long minMs) { this.minMs = minMs; return this; }

    /** Sets the maximum allowed interval. */
    public IntervalConfig setMaxMs(Long maxMs) { this.maxMs = maxMs; return this; }

    /** Sets the default interval. */
    public IntervalConfig setDefaultMs(Long defaultMs) { this.defaultMs = defaultMs; return this; }

    /** Sets the interval multiplier. */
    public IntervalConfig setMultiplier(Double multiplier) { this.multiplier = multiplier; return this; }

    /**
     * Applies this pacing config to a requested interval.
     *
     * @param requestedMs the interval the caller (usually the user config) wants,
     *                    or a value {@code <= 0} to use the server default
     * @return the effective interval in milliseconds
     */
    public long apply(long requestedMs) {
        long ms = requestedMs > 0 ? requestedMs
            : (defaultMs != null && defaultMs > 0 ? defaultMs : requestedMs);
        if (ms <= 0) return requestedMs;
        if (multiplier != null && multiplier > 0) ms = (long) (ms * multiplier);
        if (minMs != null && minMs > 0) ms = Math.max(ms, minMs);
        if (maxMs != null && maxMs > 0) ms = Math.min(ms, maxMs);
        return ms;
    }

    @Override
    public String toString() {
        return "IntervalConfig{minMs=" + minMs + ", maxMs=" + maxMs
            + ", defaultMs=" + defaultMs + ", multiplier=" + multiplier + '}';
    }
}
