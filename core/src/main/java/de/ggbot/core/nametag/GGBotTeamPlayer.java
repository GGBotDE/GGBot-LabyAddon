package de.ggbot.core.nametag;

import de.ggbot.core.utils.ttlcache.TTLCache;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.gui.icon.Icon;
import java.util.UUID;

public class GGBotTeamPlayer {
  private static final TTLCache<UUID, Icon> cache = new TTLCache<>();
  private final TeamFetcher teamFetcher;
  private final UUID uuid;

  public GGBotTeamPlayer(Player player) {
    this.uuid = player.getUniqueId();
    this.teamFetcher = TeamFetcher.teamInstance;
  }

  public GGBotTeamPlayer(UUID uuid) {
    this.uuid = uuid;
    this.teamFetcher = TeamFetcher.teamInstance;
  }


  /**
   * Normalizes a UUID by removing dashes
   */
  private String normalizeUuid(String uuid) {
    return uuid == null ? null : uuid.replace("-", "");
  }

  /**
   * Finds the team member DTO for this player
   */
  private GGBotTeam.TeamDTO.MembersDTO getMemberData() {
    if (teamFetcher == null || !teamFetcher.isFetched()) {
      return null;
    }

    GGBotTeam team = teamFetcher.getTeam();
    if (team == null || team.getTeam() == null || team.getTeam().isEmpty()) {
      return null;
    }

    String playerUuid = normalizeUuid(uuid.toString());
    if (playerUuid == null) {
      return null;
    }

    for (GGBotTeam.TeamDTO teamDto : team.getTeam()) {
      if (teamDto.getMembers() == null) continue;

      for (GGBotTeam.TeamDTO.MembersDTO member : teamDto.getMembers()) {
        if (member.getMinecraft() != null && member.getMinecraft().getUuid() != null) {
          String memberUuid = normalizeUuid(member.getMinecraft().getUuid());
          if (playerUuid.equalsIgnoreCase(memberUuid)) {
            return member;
          }
        }
      }
    }

    return null;
  }

  /**
   * Gets whether the player is marked as a member (isMember field)
   */
  public boolean isTeamMember() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    return member != null && member.getIsMember() != null && member.getIsMember();
  }

  /**
   * Gets the team name the player belongs to
   */
  public String getTeamName() {
    if (teamFetcher == null || !teamFetcher.isFetched()) {
      return null;
    }

    GGBotTeam team = teamFetcher.getTeam();
    if (team == null || team.getTeam() == null || team.getTeam().isEmpty()) {
      return null;
    }

    String playerUuid = normalizeUuid(uuid.toString());
    if (playerUuid == null) {
      return null;
    }

    for (GGBotTeam.TeamDTO teamDto : team.getTeam()) {
      if (teamDto.getMembers() == null) continue;

      for (GGBotTeam.TeamDTO.MembersDTO member : teamDto.getMembers()) {
        if (member.getMinecraft() != null && member.getMinecraft().getUuid() != null) {
          String memberUuid = normalizeUuid(member.getMinecraft().getUuid());
          if (playerUuid.equalsIgnoreCase(memberUuid)) {
            return teamDto.getName();
          }
        }
      }
    }

    return null;
  }

  /**
   * Gets the member image URL
   */
  public String getMemberImage() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    return member != null ? member.getImage() : null;
  }

  /**
   * Gets the ingame icon URL
   */
  public String getIngameIconURL() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    return member != null ? member.getIngameIcon() : null;
  }

  public Icon getIngameIcon() {
    UUID playerUUID = this.uuid;
    Icon cachedIcon = cache.get(playerUUID);
    if (cachedIcon != null) {
      return cachedIcon;
    }

    String url = getIngameIconURL();
    if (url == null || url.isEmpty()) {
      return null;
    }

    Icon icon = Icon.url(url);
    cache.put(playerUUID, icon, 3600);
    return icon;
  }

  /**
   * Gets the Minecraft username
   */
  public String getMinecraftName() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getMinecraft() != null) {
      return member.getMinecraft().getName();
    }
    return null;
  }

  /**
   * Gets the Minecraft UUID (without dashes)
   */
  public String getMinecraftUuid() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getMinecraft() != null) {
      return member.getMinecraft().getUuid();
    }
    return null;
  }

  /**
   * Gets the Discord user ID
   */
  public String getDiscordId() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getId();
    }
    return null;
  }

  /**
   * Gets the Discord username
   */
  public String getDiscordUsername() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getUsername();
    }
    return null;
  }

  /**
   * Gets the Discord global name (display name)
   */
  public String getDiscordGlobalName() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getGlobalName();
    }
    return null;
  }

  /**
   * Gets the Discord name (from discord object, not user)
   */
  public String getDiscordName() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null) {
      return member.getDiscord().getName();
    }
    return null;
  }

  /**
   * Gets the Discord tag (username#discriminator)
   */
  public String getDiscordTag() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getTag();
    }
    return null;
  }

  /**
   * Gets the Discord avatar URL
   */
  public String getDiscordAvatarUrl() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getAvatarURL();
    }
    return null;
  }

  /**
   * Gets the Discord display avatar URL
   */
  public String getDiscordDisplayAvatarUrl() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getDisplayAvatarURL();
    }
    return null;
  }

  /**
   * Gets the Discord discriminator
   */
  public String getDiscordDiscriminator() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getDiscriminator();
    }
    return null;
  }

  /**
   * Gets whether the Discord user is a bot
   */
  public boolean isDiscordBot() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      Boolean bot = member.getDiscord().getUser().getBot();
      return bot != null && bot;
    }
    return false;
  }

  /**
   * Gets the Discord account creation timestamp
   */
  public Long getDiscordCreatedTimestamp() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getCreatedTimestamp();
    }
    return null;
  }

  /**
   * Gets the Discord accent color
   */
  public Integer getDiscordAccentColor() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getAccentColor();
    }
    return null;
  }

  /**
   * Gets the Discord hex accent color
   */
  public String getDiscordHexAccentColor() {
    GGBotTeam.TeamDTO.MembersDTO member = getMemberData();
    if (member != null && member.getDiscord() != null && member.getDiscord().getUser() != null) {
      return member.getDiscord().getUser().getHexAccentColor();
    }
    return null;
  }

  /**
   * Gets the full member DTO for advanced access
   */
  public GGBotTeam.TeamDTO.MembersDTO getFullMemberData() {
    return getMemberData();
  }

  /**
   * Gets the UUID of this player
   */
  public UUID getUuid() {
    return uuid;
  }
}
