package de.ggbot.core.api.versioning.matrix;

/**
 * A single rule condition from the server's rule matrix.
 *
 * <p>Condition types mirror the backend rule engine (addon_version,
 * labymod_version, minecraft_version, os, os_version, release_channel,
 * file_hash, api_version, is_flint_addon, is_os_supported, maven_dep). The
 * special type {@code resolved} is an IP-based condition the server already
 * evaluated for this connection, since only the server knows the client IP;
 * its value is the literal string {@code "true"} or {@code "false"}.
 */
public class MatrixCondition {

  /** The context field this condition tests, e.g. {@code "addon_version"}. */
  private String conditionType;

  /** The comparison operator, e.g. {@code "eq"}, {@code "semver_range"}. */
  private String operator;

  /** The value to compare against (raw string, or JSON array for in/not_in). */
  private String value;

  /** Returns the condition type. */
  public String getConditionType() { return conditionType; }

  /** Returns the comparison operator. */
  public String getOperator() { return operator; }

  /** Returns the comparison value. */
  public String getValue() { return value; }

  @Override
  public String toString() {
    return "MatrixCondition{" + conditionType + " " + operator + " " + value + '}';
  }
}
