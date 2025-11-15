package de.ggbot.core.api;

import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.Type;
import org.openapitools.client.ApiCallback;
import org.openapitools.client.ApiException;
import java.util.List;
import java.util.Map;

import static net.labymod.api.client.component.format.NamedTextColor.RED;
import static net.labymod.api.client.component.format.NamedTextColor.WHITE;

public class Callbacks {
  /**
   * Callback für asynchrone Bot-API-Aufrufe.
   * Zeigt die Ergebnisse als LabyMod-Notifications an.
   */
  public static ApiCallback<Void> startCallback = new ApiCallback<>() {

    /**
     * Wird aufgerufen, wenn der API-Call fehlschlägt.
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
     * Wird aufgerufen, wenn der API-Call erfolgreich war.
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
     * Wird während des Upload-Fortschritts aufgerufen.
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
     * Wird während des Download-Fortschritts aufgerufen.
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
   * Callback für asynchrone Bot-API-Aufrufe.
   * Zeigt die Ergebnisse als LabyMod-Notifications an.
   */
  public static ApiCallback<Void> stopCallback = new ApiCallback<>() {

    /**
     * Wird aufgerufen, wenn der API-Call fehlschlägt.
     */
    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
      Notification.Builder builder = Notification.builder()
          .title(Component.translatable("ggbot.toasts.status.error",RED))
          .text(Component.translatable("ggbot.toasts.error", WHITE, Component.text(e.getMessage()), Component.text(statusCode)))
          .type(Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    }

    /**
     * Wird aufgerufen, wenn der API-Call erfolgreich war.
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
     * Wird während des Upload-Fortschritts aufgerufen.
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
     * Wird während des Download-Fortschritts aufgerufen.
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
     * Wird aufgerufen, wenn der API-Call fehlschlägt.
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
     * Wird aufgerufen, wenn der API-Call erfolgreich war.
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
     * Wird während des Upload-Fortschritts aufgerufen.
     */
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
    }

    /**
     * Wird während des Download-Fortschritts aufgerufen.
     */
    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
    }
  };

}
