package de.ggbot.core.api.versioning.response;

/**
 * Represents a single in-client message returned within the {@code messages} array of a
 * {@link de.ggbot.core.api.versioning.VersionCheckResponse}.
 *
 * <p>Messages are conditionally sent by the server based on the request context
 * and are intended to be displayed inside the LabyMod client UI.
 */
public class MessageResponse {

    /**
     * ISO-8601 datetime string (format {@code "yyyy-MM-dd HH:mm"}) until which this
     * message should be displayed. Empty string means no expiry.
     */
    private String enabledUntil;

    /** Unique identifier (UUID) of this message. Used with {@code showOnce} tracking. */
    private String uuid;

    /**
     * BCP-47 locale code for this message, e.g. {@code "en"}, {@code "de"}.
     * Empty string means locale-independent.
     */
    private String locale;

    /** The message text to display to the user. */
    private String message;

    /** Optional URL to open when the user clicks the message. May be empty. */
    private String link;

    /**
     * Number of seconds after client startup before this message is shown.
     * {@code 0} means show immediately.
     */
    private int showAfter;

    /**
     * If {@code true}, the message will only be shown once per client (tracked by UUID).
     */
    private boolean showOnce;

    /**
     * If {@code true}, the message is only shown after the user has interacted with
     * a previous message or specific UI element.
     */
    private boolean onlyShowAfterInteraction;

    /** If {@code true}, display as a toast notification instead of a full popup. */
    private boolean isToast;

    /** If {@code true}, display as a popup dialog. Defaults to {@code true}. */
    private boolean isPopup = true;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates an empty {@code MessageResponse}. */
    public MessageResponse() {
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the datetime string until which this message should be displayed.
     *
     * @return expiry datetime string, or empty if no expiry
     */
    public String getEnabledUntil() { return enabledUntil; }

    /**
     * Returns the unique UUID of this message.
     *
     * @return UUID string
     */
    public String getUuid() { return uuid; }

    /**
     * Returns the BCP-47 locale code for this message.
     *
     * @return locale string, or empty for locale-independent messages
     */
    public String getLocale() { return locale; }

    /**
     * Returns the message text.
     *
     * @return message string
     */
    public String getMessage() { return message; }

    /**
     * Returns the optional URL associated with this message.
     *
     * @return URL string, may be empty
     */
    public String getLink() { return link; }

    /**
     * Returns the delay in seconds before this message should be shown after startup.
     *
     * @return delay in seconds
     */
    public int getShowAfter() { return showAfter; }

    /**
     * Returns whether this message should only be shown once per client.
     *
     * @return {@code true} if show-once
     */
    public boolean isShowOnce() { return showOnce; }

    /**
     * Returns whether this message is only shown after an interaction trigger.
     *
     * @return {@code true} if interaction-gated
     */
    public boolean isOnlyShowAfterInteraction() { return onlyShowAfterInteraction; }

    /**
     * Returns whether this message should be displayed as a toast notification.
     *
     * @return {@code true} if toast
     */
    public boolean isToast() { return isToast; }

    /**
     * Returns whether this message should be displayed as a popup dialog.
     *
     * @return {@code true} if popup (default)
     */
    public boolean isPopup() { return isPopup; }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    /**
     * Sets the expiry datetime string.
     *
     * @param enabledUntil datetime string in format {@code "yyyy-MM-dd HH:mm"}
     * @return this instance for chaining
     */
    public MessageResponse setEnabledUntil(String enabledUntil) { this.enabledUntil = enabledUntil; return this; }

    /**
     * Sets the unique UUID of this message.
     *
     * @param uuid UUID string
     * @return this instance for chaining
     */
    public MessageResponse setUuid(String uuid) { this.uuid = uuid; return this; }

    /**
     * Sets the locale code.
     *
     * @param locale BCP-47 locale code
     * @return this instance for chaining
     */
    public MessageResponse setLocale(String locale) { this.locale = locale; return this; }

    /**
     * Sets the message text.
     *
     * @param message display text
     * @return this instance for chaining
     */
    public MessageResponse setMessage(String message) { this.message = message; return this; }

    /**
     * Sets the optional link URL.
     *
     * @param link URL string
     * @return this instance for chaining
     */
    public MessageResponse setLink(String link) { this.link = link; return this; }

    /**
     * Sets the seconds delay before showing.
     *
     * @param showAfter delay in seconds
     * @return this instance for chaining
     */
    public MessageResponse setShowAfter(int showAfter) { this.showAfter = showAfter; return this; }

    /**
     * Sets whether the message should only be shown once.
     *
     * @param showOnce {@code true} for show-once behaviour
     * @return this instance for chaining
     */
    public MessageResponse setShowOnce(boolean showOnce) { this.showOnce = showOnce; return this; }

    /**
     * Sets whether the message requires a prior interaction to be shown.
     *
     * @param onlyShowAfterInteraction {@code true} to gate on prior interaction
     * @return this instance for chaining
     */
    public MessageResponse setOnlyShowAfterInteraction(boolean onlyShowAfterInteraction) {
        this.onlyShowAfterInteraction = onlyShowAfterInteraction;
        return this;
    }

    /**
     * Sets whether to display as a toast notification.
     *
     * @param toast {@code true} for toast display
     * @return this instance for chaining
     */
    public MessageResponse setToast(boolean toast) { this.isToast = toast; return this; }

    /**
     * Sets whether to display as a popup dialog.
     *
     * @param popup {@code true} for popup display
     * @return this instance for chaining
     */
    public MessageResponse setPopup(boolean popup) { this.isPopup = popup; return this; }

    @Override
    public String toString() {
        return "MessageResponse{uuid='" + uuid + "', locale='" + locale +
               "', message='" + message + "', showOnce=" + showOnce + '}';
    }
}
