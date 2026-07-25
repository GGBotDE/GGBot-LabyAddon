package de.ggbot.core.api.versioning.matrix;

import de.ggbot.core.api.versioning.response.IntervalConfig;
import java.util.Collections;
import java.util.List;

/**
 * A feature flag entry from the server's rule matrix: its key, default state,
 * optional compat routing, optional interval pacing and the rules that decide
 * its effective state for this client.
 */
public class MatrixFeature {

  /** Nested compat routing object mirroring the JSON structure. */
  public static class CompatConversion {
    private String path;

    /** Returns the compat base URL path. */
    public String getPath() { return path; }
  }

  /** The feature flag key, e.g. {@code "de.ggbot.addon.shop"}. */
  private String key;

  /** The state used when no rule matches. */
  private boolean enabledDefault;

  /** Optional default compat routing for this feature. */
  private CompatConversion versionCompatabilityConversion;

  /** Optional server-controlled pacing for this feature. */
  private IntervalConfig interval;

  /** The rules deciding the effective state; may be empty. */
  private List<MatrixRule> rules;

  /** Returns the feature flag key. */
  public String getKey() { return key; }

  /** Returns the default enabled state. */
  public boolean isEnabledDefault() { return enabledDefault; }

  /** Returns the default compat routing, or {@code null}. */
  public CompatConversion getVersionCompatabilityConversion() { return versionCompatabilityConversion; }

  /** Returns the interval pacing config, or {@code null}. */
  public IntervalConfig getInterval() { return interval; }

  /** Returns the rules for this feature (never {@code null}). */
  public List<MatrixRule> getRules() {
    return rules != null ? rules : Collections.emptyList();
  }
}
