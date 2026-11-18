package de.ggbot.core.gui.botmenu;

import com.google.gson.JsonObject;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.world.item.ItemData;
import net.labymod.api.client.world.item.ItemStack;
import net.labymod.api.loader.platform.PlatformEnvironment;
import net.labymod.api.nbt.NBTFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a Minecraft item type to the way it is drawn. The sources are tried in
 * this order:
 * <ol>
 *   <li>The game itself, through an {@link ItemStack}. It uses the active resource
 *       packs and server pack, knows installed mods and applies the NBT of the item,
 *       so it is the closest to what players see in game. Clients before 1.13 are
 *       asked for the old name of the item, see {@link LegacyItems}.</li>
 *   <li>The textures bundled with the addon in
 *       {@code themes/vanilla/textures/icons/items/}. These provide coverage for
 *       items unknown to the current client, such as items introduced in newer
 *       Minecraft versions. They work offline and have consistent appearances,
 *       but do not account for resource pack modifications.</li>
 *   <li>The GGBot item renderer, see {@link RemoteItemIcons}, for items neither of
 *       them knows. It comes last since it costs a request per item.</li>
 *   <li>The barrier texture, used as a temporary placeholder while an icon is
 *       loading and as a fallback when all available sources fail.</li> * </ol>
 *
 * <p>An item type may carry fallbacks, see {@link #withFallback}. The game and the
 * textures are asked for the item first and then for each fallback, the item
 * renderer the same way, but only after both failed for all of them.
 *
 * <p>Resolved icons are stored in an in-memory cache keyed by item type, server
 * and NBT data. This avoids repeating the lookup process, which would otherwise
 * involve querying the Minecraft client on every render frame.
 * When the cache exceeds {@value #MAX_CACHED} entries, the oldest entries
 * are removed.
 *
 * <p>Cache contents are never persisted to disk. Consequently, failed downloads
 * are not retried until the application is restarted or the corresponding cache
 * entry is evicted. Resource pack changes require no manual cache invalidation
 * because items handled by Minecraft are rendered directly by the game.
 */
public final class ItemIcons {

  private static final String DEFAULT_NAMESPACE = "minecraft";
  private static final String FALLBACK = " ";
  private static final String TEXTURE_PATH = "themes/vanilla/textures/icons/items/";
  private static final int MAX_CACHED = 512;

  /**
   * Identifies the Minecraft protocol version that introduced float-based custom model data.
   */
  private static final int FLOAT_MODEL_DATA_PROTOCOL = 769;

  private static final Map<String, ItemIcon> cache = Collections.synchronizedMap(
      new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ItemIcon> eldest) {
          return size() > MAX_CACHED;
        }
      });

  private ItemIcons() {}

  /**
   * Determines the visual representation of an item based on its type and NBT data.
   *
   * @param itemType the item type of the API
   * @param nbt the NBT of the API, may be {@code null}
   * @return the item icon, never {@code null}
   */
  public static ItemIcon forItem(String itemType, String nbt) {
    String type = itemType == null ? "" : itemType;
    String key = type + '\n' + RemoteItemIcons.currentServer() + '\n' + nbt;
    ItemIcon cached = cache.get(key);
    if (cached != null) return cached;
    ItemIcon resolved = resolve(type, ItemNbt.parse(nbt));
    cache.put(key, resolved);
    return resolved;
  }

  /**
   * Constructs a lookup key by combining an item type with fallback options.
   *
   * @param itemType the primary item type to start the lookup with
   * @param fallbacks additional item types to consider if the primary type is not found
   * @return a combined string used as a key for item lookup
   */
  public static String withFallback(String itemType, String... fallbacks) {
    StringBuilder combined = new StringBuilder(itemType);
    for (String fallback : fallbacks) {
      if (fallback == null || fallback.isEmpty()) continue;
      if (Arrays.asList(combined.toString().split(FALLBACK)).contains(fallback)) continue;
      combined.append(FALLBACK).append(fallback);
    }
    return combined.toString();
  }

  private static ItemIcon resolve(String itemType, JsonObject nbt) {
    for (String type : itemType.toLowerCase(Locale.ROOT).split(FALLBACK)) {
      int separator = type.indexOf(':');
      String namespace = separator < 0 ? DEFAULT_NAMESPACE : type.substring(0, separator);
      String path = type.substring(separator + 1);

      ItemStack stack = createStack(namespace, path, nbt);
      ResourceLocation bundled = texture(path);
      boolean hasTexture = DEFAULT_NAMESPACE.equals(namespace) && !path.isEmpty()
          && bundled.exists();
      if (stack != null || hasTexture) {
        return new ItemIcon(stack, Icon.texture(hasTexture ? bundled : texture("barrier")));
      }
    }
    return new ItemIcon(null, RemoteItemIcons.load(Arrays.asList(itemType.split(FALLBACK)),
        ItemNbt.customModelData(nbt), texture("barrier")));
  }

  private static ResourceLocation texture(String name) {
    return ResourceLocation.create("ggbot", TEXTURE_PATH + name + ".png");
  }

  private static ItemStack createStack(String namespace, String path, JsonObject nbt) {
    if (path.isEmpty()) return null;
    ItemStack plain = plainStack(namespace, path, 0);
    if (plain != null) {
      return nbt == null ? plain : withNbt(namespace, path, 0, nbt, plain);
    }
    LegacyItems.Item legacy = LegacyItems.find(namespace, path);
    if (legacy == null) return null;
    plain = plainStack(namespace, legacy.path(), legacy.data());
    if (plain == null || nbt == null) return plain;
    return withNbt(namespace, legacy.path(), legacy.data(), nbt, plain);
  }

  /**
   * Handles item creation for unknown IDs with version-specific behavior.
   *
   * @param namespace the namespace of the item
   * @param path the path to the item
   * @param data the legacy item data
   * @return the created item stack or null if it cannot be found
   */
  private static ItemStack plainStack(String namespace, String path, int data) {
    try {
      ItemStack stack = Laby.references().itemStackFactory().create(namespace, path);
      if (!isItem(stack, path)) return null;
      if (data != 0) stack.setLegacyItemData(data);
      return stack;
    } catch (RuntimeException e) {
      return null;
    }
  }

  /**
   * Creates an item stack with NBT metadata, falling back to a plain version if metadata is not supported.
   *
   * @param namespace the namespace of the item
   * @param path the path to the item
   * @param data the legacy item data
   * @param nbt the NBT data to apply to the item
   * @param plain the original item stack without metadata
   * @return the item stack with metadata if supported, otherwise the plain version
   */
  private static ItemStack withNbt(String namespace, String path, int data, JsonObject nbt,
      ItemStack plain) {
    try {
      NBTFactory factory = Laby.references().nbtFactory();
      boolean components = PlatformEnvironment.ITEM_COMPONENTS;
      boolean floats = Laby.labyAPI().minecraft().getProtocolVersion() >= FLOAT_MODEL_DATA_PROTOCOL;
      ItemStack stack = new ItemData(namespace, path,
          components ? null : ItemNbt.toCompound(factory, nbt),
          components ? ItemNbt.toComponents(factory, nbt, floats) : null,
          data).toItemStack();
      return isItem(stack, path) ? stack : plain;
    } catch (RuntimeException e) {
      return plain;
    }
  }

  private static boolean isItem(ItemStack stack, String path) {
    return stack != null && !stack.isAir() && path.equals(stack.getIdentifier().getPath());
  }
}
