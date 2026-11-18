package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.network.server.ServerData;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.resources.texture.GameImage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Retrieves item icons from the rendering service when neither Minecraft nor
 * the addon's built-in textures can provide a suitable image. Retrieved icons
 * are stored in memory as textures and are never saved to disk.
 * Each request contains only the item identifier, Minecraft version and server
 * domain, without including any information that could identify the player.
 */
public final class RemoteItemIcons {

  private static final String FEATURE = "de.ggbot.addon.itemrenderer";
  private static final String BASE_URL = "https://itemrenderer.ggbot.de";
  private static final int SIZE = 64;
  private static final long MAX_BYTES = 1_048_576L;
  private static final int NOT_FOUND = 404;

  private static final OkHttpClient CLIENT = new OkHttpClient();
  private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "ggbot-item-render");
    t.setDaemon(true);
    return t;
  });

  private RemoteItemIcons() {}

  /**
   * Returns an icon that displays a placeholder until the requested image is loaded.
   * Item alternatives are checked in sequence, moving to the next one only when
   * the rendering service cannot provide an image for the current item.
   * If the download fails, the placeholder remains displayed.
   *
   * @param itemTypes the list of item types to request
   * @param customModelData the custom model data for the item, may be null
   * @param placeholder the texture to use while waiting for the image
   * @return the loaded icon or the placeholder if failed
   */
  public static Icon load(List<String> itemTypes, Integer customModelData,
      ResourceLocation placeholder) {
    AtomicReference<ResourceLocation> location = new AtomicReference<>(placeholder);
    GGBot addon = GGBot.getInstance();
    
    if (itemTypes.isEmpty() || itemTypes.get(0).isEmpty() || addon == null
        || !addon.getVersioningHandler().isFeatureEnabled(FEATURE)) {
      return Icon.texture(placeholder);
    }

    String version = Laby.labyAPI().minecraft().getVersion();
    String server = currentServer();
    EXECUTOR.execute(() -> {
      for (String itemType : itemTypes) {
        String url = url(BASE_URL, itemType, version, server, customModelData);
        try {
          byte[] data = download(url);
          if (data == null) continue;

          GameImage image = GameImage.IMAGE_PROVIDER.getImage(new ByteArrayInputStream(data));
          ResourceLocation target = ResourceLocation.create("ggbot", "item_render/"
              + UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)));
          Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
            image.uploadTextureAt(target);
            location.set(target);
          });
        } catch (IOException | RuntimeException e) {
          addon.logger().warn("Item render of {} failed: {}", itemType, String.valueOf(e.getMessage()));
        }
        return;
      }
    });
    return Icon.texture(location::get);
  }

  /**
   * Constructs the URL for fetching an item image from the remote service.
   *
   * @param baseUrl the base URL of the remote service
   * @param itemType the type of item to request
   * @param version the game version
   * @param server the server domain
   * @param customModelData the custom model data for the item
   * @return the constructed image URL
   */
  public static String url(String baseUrl, String itemType, String version, String server,
      Integer customModelData) {
    StringBuilder url = new StringBuilder(baseUrl).append("/item/").append(encode(itemType))
        .append(".png?size=").append(SIZE).append("&version=").append(encode(version));
    if (server != null) url.append("&server=").append(encode(server));
    if (customModelData != null) url.append("&cmd=").append(customModelData);
    return url.toString();
  }

  /**
   * Fetches an image from the specified URL, handling errors and size limits.
   *
   * @param url the URL to fetch the image from
   * @return the image data as a byte array or null if the request fails
   * @throws IOException if the request fails due to network issues or invalid responses
   */
  public static byte[] download(String url) throws IOException {
    Request request = new Request.Builder().url(url).build();
    try (Response response = CLIENT.newCall(request).execute()) {
      if (response.code() == NOT_FOUND) return null;
      if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
      if (response.body() == null) return null;
      if (response.body().contentLength() > MAX_BYTES) return null;
      byte[] data = response.body().bytes();
      return data.length > MAX_BYTES ? null : data;
    }
  }

  /**
   * Extracts the server's domain name, used to identify server packs.
   * @return the simplified domain name of the current server
   */
  public static String currentServer() {
    ServerData data = Laby.labyAPI().serverController().getCurrentServerData();
    return data == null ? null : baseDomain(data.address().getHost());
  }

  /**
   * Simplifies a host name by extracting its last two domain labels.
   *
   * @param host the full host name
   * @return the simplified domain name with the last two labels
   */
  public static String baseDomain(String host) {
    String[] parts = host.toLowerCase(Locale.ROOT).split("\\.");
    if (parts.length < 2) return host.toLowerCase(Locale.ROOT);
    return parts[parts.length - 2] + "." + parts[parts.length - 1];
  }

  private static String encode(String text) {
    return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
