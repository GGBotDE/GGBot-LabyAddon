package de.ggbot.core.api.versioning.matrix;

import de.ggbot.core.api.versioning.response.MessageResponse;
import java.util.Collections;
import java.util.List;

/**
 * A client message from the server's rule matrix together with its targeting
 * conditions. All conditions must match (AND logic) for the message to be
 * shown; a message without conditions is shown to everyone.
 */
public class MatrixMessage {

  private String enabledUntil;
  private String uuid;
  private String locale;
  private String message;
  private String link;
  private int showAfter;
  private boolean showOnce;
  private boolean onlyShowAfterInteraction;
  private boolean isToast;
  private boolean isPopup = true;

  /** Targeting conditions; empty means the message applies to everyone. */
  private List<MatrixCondition> rules;

  /** Returns the targeting conditions (never {@code null}). */
  public List<MatrixCondition> getRules() {
    return rules != null ? rules : Collections.emptyList();
  }

  /** Converts this matrix entry to the {@link MessageResponse} the addon uses. */
  public MessageResponse toMessageResponse() {
    MessageResponse resp = new MessageResponse();
    resp.setEnabledUntil(enabledUntil);
    resp.setUuid(uuid);
    resp.setLocale(locale);
    resp.setMessage(message);
    resp.setLink(link);
    resp.setShowAfter(showAfter);
    resp.setShowOnce(showOnce);
    resp.setOnlyShowAfterInteraction(onlyShowAfterInteraction);
    resp.setToast(isToast);
    resp.setPopup(isPopup);
    return resp;
  }
}
