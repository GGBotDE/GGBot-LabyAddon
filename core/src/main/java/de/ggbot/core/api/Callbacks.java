package de.ggbot.core.api;

import de.ggbot.sdk.core.ApiCallback;
import de.ggbot.sdk.core.ApiException;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.Type;

import java.util.List;
import java.util.Map;

import static net.labymod.api.client.component.format.NamedTextColor.RED;
import static net.labymod.api.client.component.format.NamedTextColor.WHITE;

/**
 * Pre-built {@link ApiCallback} instances for common asynchronous bot operations.
 * Results and errors are surfaced as LabyMod system notifications.
 */
public class Callbacks {

  /**
   * Callback used when starting a bot.
   * Displays a success or error notification depending on the outcome.
   */
  public static final ApiCallback<Void> START = new ApiCallback<>() {

    /** {@inheritDoc} */
    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
      push(Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.error", RED))
          .text(Component.translatable("ggbot.toasts.error", WHITE,
              Component.text(e.getMessage()), Component.text(statusCode)))
          .type(Type.SYSTEM));
    }

    /** {@inheritDoc} */
    @Override
    public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
      push(Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.info", NamedTextColor.GREEN))
          .text(Component.translatable("ggbot.toasts.info.start"))
          .type(Type.SYSTEM));
    }

    /** {@inheritDoc} */
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {}

    /** {@inheritDoc} */
    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {}
  };

  /**
   * Callback used when stopping a bot.
   * Displays a success or error notification depending on the outcome.
   */
  public static final ApiCallback<Void> STOP = new ApiCallback<>() {

    /** {@inheritDoc} */
    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
      push(Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.error", RED))
          .text(Component.translatable("ggbot.toasts.error", WHITE,
              Component.text(e.getMessage()), Component.text(statusCode)))
          .type(Type.SYSTEM));
    }

    /** {@inheritDoc} */
    @Override
    public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
      push(Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.info", NamedTextColor.GREEN))
          .text(Component.translatable("ggbot.toasts.info.stop"))
          .type(Type.SYSTEM));
    }

    /** {@inheritDoc} */
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {}

    /** {@inheritDoc} */
    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {}
  };

  /**
   * Callback used when sending a command to the bot.
   * Displays a success or error notification depending on the outcome.
   */
  public static final ApiCallback<Void> SEND_COMMAND = new ApiCallback<>() {

    /** {@inheritDoc} */
    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
      push(Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.error", RED))
          .text(Component.translatable("ggbot.toasts.error", WHITE,
              Component.text(e.getMessage()), Component.text(statusCode)))
          .type(Type.SYSTEM));
    }

    /** {@inheritDoc} */
    @Override
    public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
      push(Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.info", NamedTextColor.GREEN))
          .text(Component.translatable("ggbot.toasts.info.command"))
          .type(Type.SYSTEM));
    }

    /** {@inheritDoc} */
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {}

    /** {@inheritDoc} */
    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {}
  };

  /**
   * Convenience helper that builds and pushes a notification.
   *
   * @param builder the notification builder
   */
  private static void push(Notification.Builder builder) {
    Laby.labyAPI().notificationController().push(builder.build());
  }
}
