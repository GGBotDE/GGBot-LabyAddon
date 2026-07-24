package de.ggbot.core.api.versioning.matrix;

import de.ggbot.core.api.versioning.VersionCheckRequest;
import de.ggbot.core.api.versioning.VersionCheckResponse;
import de.ggbot.core.api.versioning.response.FeatureFlagResponse;
import de.ggbot.core.api.versioning.response.MessageResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side mirror of the backend rule engine.
 *
 * <p>The server hands out its full decision matrix (see {@link MatrixResponse})
 * and this engine evaluates it against the local environment, so the version
 * check no longer has to upload any environment data. The evaluation semantics
 * intentionally match the backend implementation: rules are sorted by priority
 * (highest first), all conditions within a rule are AND-ed, the first matching
 * rule wins, and unknown condition types pass (so a newer backend cannot
 * accidentally lock out older clients).
 */
public final class ClientRuleEngine {

  private ClientRuleEngine() {
  }

  /** Result of evaluating a rule list. */
  private static final class RuleResult {
    final boolean matched;
    final boolean enabled;
    final String message;
    final String compatUrl;

    RuleResult(boolean matched, boolean enabled, String message, String compatUrl) {
      this.matched = matched;
      this.enabled = enabled;
      this.message = message;
      this.compatUrl = compatUrl;
    }
  }

  /**
   * Evaluates a full matrix against the local environment and builds the same
   * {@link VersionCheckResponse} the legacy {@code POST /v1/check} endpoint
   * would have returned, so the rest of the addon works unchanged.
   *
   * @param matrix the rule matrix from the server
   * @param env    the local environment (never sent anywhere by this engine)
   * @return the locally computed version check response
   */
  public static VersionCheckResponse evaluate(MatrixResponse matrix, VersionCheckRequest env) {
    VersionCheckResponse response = new VersionCheckResponse();

    RuleResult support = evaluateRules(matrix.getSupportRules(), env, true);
    response.setSupported(support.enabled);
    if (support.message != null) response.setMessage(support.message);

    response.setCurrentVersion(matrix.getCurrentVersion());

    Map<String, FeatureFlagResponse> features = new HashMap<>();
    for (MatrixFeature feature : matrix.getFeatures()) {
      RuleResult result = evaluateRules(feature.getRules(), env, feature.isEnabledDefault());

      FeatureFlagResponse flag = new FeatureFlagResponse();
      flag.setEnabled(result.enabled);

      // Rule override wins over the feature's default compat URL.
      String compatUrl = result.compatUrl != null ? result.compatUrl
          : (feature.getVersionCompatabilityConversion() != null
              ? feature.getVersionCompatabilityConversion().getPath() : null);
      if (compatUrl != null && !compatUrl.isEmpty()) {
        flag.setVersionCompatabilityConversionPath(compatUrl);
      }
      flag.setInterval(feature.getInterval());

      features.put(feature.getKey(), flag);
    }
    response.setFeatures(features);

    List<MessageResponse> messages = new ArrayList<>();
    for (MatrixMessage message : matrix.getMessages()) {
      if (messageMatches(message, env)) {
        messages.add(message.toMessageResponse());
      }
    }
    response.setMessages(messages);

    return response;
  }

  /**
   * Evaluates a rule list: highest priority first, first match wins.
   */
  private static RuleResult evaluateRules(List<MatrixRule> rules, VersionCheckRequest env,
      boolean defaultEnabled) {
    List<MatrixRule> sorted = new ArrayList<>(rules);
    sorted.sort(Comparator.comparingInt(MatrixRule::getPriority).reversed());
    for (MatrixRule rule : sorted) {
      if (ruleMatches(rule, env)) {
        return new RuleResult(true, rule.isResultEnabled(), rule.getResultMessage(),
            rule.getResultVersionCompatConversionUrl());
      }
    }
    return new RuleResult(false, defaultEnabled, null, null);
  }

  /** All conditions in a rule must match (AND logic). */
  private static boolean ruleMatches(MatrixRule rule, VersionCheckRequest env) {
    for (MatrixCondition condition : rule.getConditions()) {
      if (!conditionMatches(condition, env)) return false;
    }
    return true;
  }

  /**
   * Message targeting: all conditions must match (AND logic); a message
   * without conditions is shown to everyone. Mirrors the backend.
   */
  private static boolean messageMatches(MatrixMessage message, VersionCheckRequest env) {
    for (MatrixCondition condition : message.getRules()) {
      if (!conditionMatches(condition, env)) return false;
    }
    return true;
  }

  /** Evaluates a single condition against the local environment. */
  private static boolean conditionMatches(MatrixCondition condition, VersionCheckRequest env) {
    String type = condition.getConditionType();
    String operator = condition.getOperator();
    String value = condition.getValue();
    if (type == null || operator == null || value == null) return true;

    switch (type) {
      case "addon_version":
        return evalString(nullToEmpty(env.getAddonVersion()), operator, value);
      case "labymod_version":
        return evalString(nullToEmpty(env.getLabymodVersion()), operator, value);
      case "minecraft_version":
        return evalString(nullToEmpty(env.getMinecraftVersion()), operator, value);
      case "os":
        return evalString(nullToEmpty(env.getOs()).toLowerCase(Locale.ROOT), operator, value);
      case "os_version":
        return evalString(env.getOsVersion() != null ? String.valueOf(env.getOsVersion()) : "",
            operator, value);
      case "release_channel":
        return evalString(nullToEmpty(env.getReleaseChannel()), operator, value);
      case "file_hash":
        return evalString(nullToEmpty(env.getFileHash()), operator, value);
      case "api_version":
        return evalString(nullToEmpty(env.getApiVersion()), operator, value);
      case "is_flint_addon":
        return evalBoolean(env.isFlintAddon(), operator, value);
      case "is_os_supported":
        return evalBoolean(env.isCurrentOsSupported(), operator, value);
      case "maven_dep":
        return evalMavenDep(env.getMavenDependencies(), operator, value);
      case "resolved":
        // IP-based condition, already evaluated by the server for this
        // connection; value is the literal outcome.
        return "true".equalsIgnoreCase(value);
      default:
        // Unknown condition = pass, mirroring the backend rule engine.
        return true;
    }
  }

  private static String nullToEmpty(String s) {
    return s != null ? s : "";
  }

  private static boolean evalBoolean(boolean ctxValue, String operator, String value) {
    boolean boolValue = "true".equalsIgnoreCase(value);
    if ("eq".equals(operator)) return ctxValue == boolValue;
    if ("neq".equals(operator)) return ctxValue != boolValue;
    return false;
  }

  private static boolean evalMavenDep(List<String> deps, String operator, String value) {
    switch (operator) {
      case "contains":
        return deps.stream().anyMatch(d -> d.contains(value));
      case "eq":
        return deps.contains(value);
      case "not_contains":
        return deps.stream().noneMatch(d -> d.contains(value));
      case "not_in":
        return !deps.contains(value);
      case "in":
        for (String entry : parseJsonStringArray(value)) {
          if (deps.contains(entry)) return true;
        }
        return false;
      default:
        return false;
    }
  }

  private static boolean evalString(String ctxValue, String operator, String value) {
    switch (operator) {
      case "eq":
        return ctxValue.equals(value);
      case "neq":
        return !ctxValue.equals(value);
      case "contains":
        return ctxValue.contains(value);
      case "not_contains":
        return !ctxValue.contains(value);
      case "in":
        return parseJsonStringArray(value).contains(ctxValue);
      case "not_in":
        return !parseJsonStringArray(value).contains(ctxValue);
      case "semver_range":
        return SemverLite.satisfies(ctxValue, value);
      case "lt":
        return compareVersionsOrNumbers(ctxValue, value) < 0;
      case "lte":
        return compareVersionsOrNumbers(ctxValue, value) <= 0;
      case "gt":
        return compareVersionsOrNumbers(ctxValue, value) > 0;
      case "gte":
        return compareVersionsOrNumbers(ctxValue, value) >= 0;
      default:
        return false;
    }
  }

  /**
   * Compares two values the way the backend does: semver-coerced comparison
   * when both coerce, plain numeric comparison otherwise.
   */
  private static int compareVersionsOrNumbers(String a, String b) {
    int[] va = SemverLite.coerce(a);
    int[] vb = SemverLite.coerce(b);
    if (va != null && vb != null) return SemverLite.compare(va, vb);
    double da = parseDoubleLenient(a);
    double db = parseDoubleLenient(b);
    return Double.compare(da, db);
  }

  private static double parseDoubleLenient(String s) {
    try {
      return Double.parseDouble(s.trim());
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }

  /**
   * Parses a JSON array of strings, e.g. {@code ["a","b"]}, without a JSON
   * library dependency in this hot path. Malformed input yields an empty list.
   */
  private static List<String> parseJsonStringArray(String json) {
    List<String> result = new ArrayList<>();
    if (json == null) return result;
    Matcher matcher = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
    while (matcher.find()) {
      result.add(matcher.group(1)
          .replace("\\\"", "\"")
          .replace("\\\\", "\\"));
    }
    return result;
  }

  /**
   * Minimal semver implementation covering the range subset used by the
   * versioning panel: comparators ({@code >= > <= < =}), plain versions,
   * caret ({@code ^1.2.3}), tilde ({@code ~1.2.3}), x-ranges ({@code 1.2.x}),
   * hyphen ranges ({@code 1.2.3 - 1.4.0}), space-separated AND and
   * {@code ||}-separated OR. Panel-side rules should stick to this subset.
   */
  static final class SemverLite {

    private static final Pattern VERSION = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private SemverLite() {
    }

    /**
     * Coerces a version-ish string to {@code [major, minor, patch]}, mirroring
     * semver.coerce (first number sequence wins, missing parts are zero).
     * Returns {@code null} when no number is present.
     */
    static int[] coerce(String input) {
      if (input == null) return null;
      Matcher matcher = VERSION.matcher(input);
      if (!matcher.find()) return null;
      try {
        return new int[]{
            Integer.parseInt(matcher.group(1)),
            matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0,
            matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0,
        };
      } catch (NumberFormatException e) {
        return null;
      }
    }

    /** Standard lexicographic compare of {@code [major, minor, patch]}. */
    static int compare(int[] a, int[] b) {
      for (int i = 0; i < 3; i++) {
        int diff = Integer.compare(a[i], b[i]);
        if (diff != 0) return diff;
      }
      return 0;
    }

    /** Returns whether {@code versionString} satisfies {@code range}. */
    static boolean satisfies(String versionString, String range) {
      int[] version = coerce(versionString);
      if (version == null || range == null || range.trim().isEmpty()) return false;
      for (String orPart : range.split("\\|\\|")) {
        if (satisfiesAndGroup(version, orPart.trim())) return true;
      }
      return false;
    }

    private static boolean satisfiesAndGroup(int[] version, String group) {
      if (group.isEmpty() || "*".equals(group)) return true;

      // Hyphen range: "1.2.3 - 1.4.0"
      Matcher hyphen = Pattern.compile("^(\\S+)\\s+-\\s+(\\S+)$").matcher(group);
      if (hyphen.matches()) {
        int[] low = coerce(hyphen.group(1));
        int[] high = coerce(hyphen.group(2));
        return low != null && high != null
            && compare(version, low) >= 0 && compare(version, high) <= 0;
      }

      for (String comparator : group.split("\\s+")) {
        if (!satisfiesComparator(version, comparator)) return false;
      }
      return true;
    }

    private static boolean satisfiesComparator(int[] version, String comparator) {
      String op = "";
      String rest = comparator;
      for (String candidate : new String[]{">=", "<=", ">", "<", "=", "^", "~"}) {
        if (comparator.startsWith(candidate)) {
          op = candidate;
          rest = comparator.substring(candidate.length());
          break;
        }
      }

      // x-ranges like "1.2.x", "1.x" or "1.2.*" mean a version prefix match.
      if (rest.contains("x") || rest.contains("X") || rest.contains("*")) {
        return satisfiesXRange(version, rest);
      }

      int[] target = coerce(rest);
      if (target == null) return false;

      switch (op) {
        case ">=":
          return compare(version, target) >= 0;
        case "<=":
          return compare(version, target) <= 0;
        case ">":
          return compare(version, target) > 0;
        case "<":
          return compare(version, target) < 0;
        case "^":
          // Same major, at least the target (major-based, sufficient for the
          // documented panel subset).
          return version[0] == target[0] && compare(version, target) >= 0;
        case "~":
          // Same major.minor, at least the target.
          return version[0] == target[0] && version[1] == target[1]
              && compare(version, target) >= 0;
        default:
          return compare(version, target) == 0;
      }
    }

    private static boolean satisfiesXRange(int[] version, String pattern) {
      String[] parts = pattern.split("\\.");
      for (int i = 0; i < Math.min(parts.length, 3); i++) {
        String part = parts[i];
        if (part.isEmpty() || part.equalsIgnoreCase("x") || part.equals("*")) continue;
        try {
          if (version[i] != Integer.parseInt(part)) return false;
        } catch (NumberFormatException e) {
          return false;
        }
      }
      return true;
    }
  }
}
