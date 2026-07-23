package de.ggbot.core.api.versioning.matrix;

import de.ggbot.core.api.versioning.response.CurrentVersion;
import java.util.Collections;
import java.util.List;

/**
 * The full rule matrix returned by {@code GET /v1/matrix/:addonSlug}.
 *
 * <p>Instead of uploading the client environment so the server can compute
 * feature flags, the client downloads this matrix once per launch and
 * evaluates all rules locally via {@link ClientRuleEngine}. The request
 * carries no environment data. IP-based conditions arrive pre-resolved as
 * {@code resolved} conditions (see {@link MatrixCondition}).
 */
public class MatrixResponse {

  /** Matrix format version for future evolution. */
  private int matrixVersion;

  /**
   * Server-controlled delay, in seconds, after which the client should fetch
   * the matrix again. {@code 0} or absent means the client keeps its default
   * refresh cadence. This lets the backend steer how often each client polls
   * from one response to the next.
   */
  private int refreshSeconds;

  /** Information about the latest available addon version. */
  private CurrentVersion currentVersion;

  /** Rules deciding whether the addon as a whole is supported. */
  private List<MatrixRule> supportRules;

  /** All feature flags with their rules. */
  private List<MatrixFeature> features;

  /** All client messages with their targeting conditions. */
  private List<MatrixMessage> messages;

  /** Returns the matrix format version. */
  public int getMatrixVersion() { return matrixVersion; }

  /**
   * Returns the server-requested delay until the next matrix fetch, in
   * seconds, or {@code 0} when the server did not specify one.
   */
  public int getRefreshSeconds() { return refreshSeconds; }

  /** Returns the latest version info, or {@code null}. */
  public CurrentVersion getCurrentVersion() { return currentVersion; }

  /** Returns the addon support rules (never {@code null}). */
  public List<MatrixRule> getSupportRules() {
    return supportRules != null ? supportRules : Collections.emptyList();
  }

  /** Returns the feature flag entries (never {@code null}). */
  public List<MatrixFeature> getFeatures() {
    return features != null ? features : Collections.emptyList();
  }

  /** Returns the message entries (never {@code null}). */
  public List<MatrixMessage> getMessages() {
    return messages != null ? messages : Collections.emptyList();
  }
}
