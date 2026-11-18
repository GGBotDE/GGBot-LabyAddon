package de.ggbot.core.gui.replays;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.resources.texture.GameImage;

/**
 * Replay preview images as icons. Replays save them as JPEG, a format the game
 * cannot load, so each is converted to PNG once and cached for the session.
 */
final class ReplayThumbnails {

  private static final ResourceLocation PLACEHOLDER = ResourceLocation.create("ggbot",
      "themes/vanilla/textures/icons/items/painting.png");
  private static final Map<String, AtomicReference<ResourceLocation>> LOADED =
      new ConcurrentHashMap<>();

  private ReplayThumbnails() {}

  /** The icon of a replay without a preview. */
  static Icon placeholder() {
    return Icon.texture(PLACEHOLDER);
  }

  /**
   * @param key  identifies the image, such as the file and its size
   * @param data the image data
   * @return the icon, a placeholder painting until the image finishes loading
   */
  static Icon icon(String key, byte[] data) {
    AtomicReference<ResourceLocation> location = LOADED.computeIfAbsent(key, k -> {
      AtomicReference<ResourceLocation> ref = new AtomicReference<>(PLACEHOLDER);

      Thread thread = new Thread(() -> load(k, data, ref), "ggbot-replay-thumbnail");
      thread.setDaemon(true);
      thread.start();
      return ref;
    });
    return Icon.texture(location::get);
  }

  private static void load(String key, byte[] data, AtomicReference<ResourceLocation> ref) {
    try {
      BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(data));
      if (decoded == null) return;

      ByteArrayOutputStream png = new ByteArrayOutputStream();
      ImageIO.write(decoded, "png", png);

      GameImage image = GameImage.IMAGE_PROVIDER.getImage(
          new ByteArrayInputStream(png.toByteArray()));
      UUID name = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
      ResourceLocation target = ResourceLocation.create("ggbot", "replay_thumbnail/" + name);

      Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
        image.uploadTextureAt(target);
        ref.set(target);
      });
    } catch (IOException | RuntimeException e) {
      LOADED.remove(key);
    }
  }
}
