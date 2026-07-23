package de.ggbot.core.api.versioning.matrix;

import java.util.Collections;
import java.util.List;

/**
 * A single rule from the server's rule matrix. Rules are evaluated highest
 * priority first; within a rule all conditions must match (AND logic) and the
 * first matching rule wins.
 */
public class MatrixRule {

  /** Evaluation priority; higher values are evaluated first. */
  private int priority;

  /** The enabled state that applies when this rule matches. */
  private boolean resultEnabled;

  /** Optional message shown to the user when this rule matches. */
  private String resultMessage;

  /** Optional compat base URL override that applies when this rule matches. */
  private String resultVersionCompatConversionUrl;

  /** The conditions that must all match for this rule to apply. */
  private List<MatrixCondition> conditions;

  /** Returns the evaluation priority. */
  public int getPriority() { return priority; }

  /** Returns the enabled state this rule results in. */
  public boolean isResultEnabled() { return resultEnabled; }

  /** Returns the optional result message, or {@code null}. */
  public String getResultMessage() { return resultMessage; }

  /** Returns the optional compat URL override, or {@code null}. */
  public String getResultVersionCompatConversionUrl() { return resultVersionCompatConversionUrl; }

  /** Returns the conditions of this rule (never {@code null}). */
  public List<MatrixCondition> getConditions() {
    return conditions != null ? conditions : Collections.emptyList();
  }
}
