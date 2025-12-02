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

public class Callbacks {

  /**
   * Callback for asynchronous Bot API calls.
   * Displays results as LabyMod notifications.
   */
  public static ApiCallback<Void> startCallback = new ApiCallback<>() {

    /**
     * Called when the API call fails.
     */
    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.error", RED))
          .text(Component.translatable("ggbot.toasts.error", WHITE, Component.text(e.getMessage()), Component.text(statusCode)))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called when the API call was successful.
     */
    @Override
    public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.info", NamedTextColor.GREEN))
          .text(Component.translatable("ggbot.toasts.info.start"))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called during upload progress.
     */
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.upload", NamedTextColor.WHITE))
          .text(Component.translatable("ggbot.toasts.upload", Component.text(bytesWritten), Component.text(contentLength)))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called during download progress.
     */
    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.download", NamedTextColor.AQUA))
          .text(Component.translatable("ggbot.toasts.download", Component.text(bytesRead), Component.text(contentLength)))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }
  };

  /**
   * Callback for asynchronous Bot API calls.
   * Displays results as LabyMod notifications.
   */
  public static ApiCallback<Void> stopCallback = new ApiCallback<>() {

    /**
     * Called when the API call fails.
     */
    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.error", RED))
          .text(Component.translatable("ggbot.toasts.error", WHITE, Component.text(e.getMessage()), Component.text(statusCode)))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called when the API call was successful.
     */
    @Override
    public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.info", NamedTextColor.GREEN))
          .text(Component.translatable("ggbot.toasts.info.stop"))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called during upload progress.
     */
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.upload", NamedTextColor.WHITE))
          .text(Component.translatable("ggbot.toasts.upload", Component.text(bytesWritten), Component.text(contentLength)))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called during download progress.
     */
    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.download", NamedTextColor.AQUA))
          .text(Component.translatable("ggbot.toasts.download", Component.text(bytesRead), Component.text(contentLength)))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }
  };

  public static ApiCallback<Void> sendCommandCallback = new ApiCallback<>() {

    /**
     * Called when the API call fails.
     */
    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.error", RED))
          .text(Component.translatable("ggbot.toasts.error", WHITE, Component.text(e.getMessage()), Component.text(statusCode)))
          .type(Type.SYSTEM);
      System.out.println(e.getMessage());
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called when the API call was successful.
     */
    @Override
    public void onSuccess(Void result, int statusCode, Map<String, List<String>> responseHeaders) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.info", NamedTextColor.GREEN))
          .text(Component.translatable("ggbot.toasts.info.command"))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Called during upload progress.
     */
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
    }

    /**
     * Called during download progress.
     */
    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
    }
  };
}
