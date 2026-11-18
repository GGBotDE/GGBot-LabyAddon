package de.ggbot.core.nametag;

import java.util.List;
import java.util.Map;

/**
 * Represents the API response for the /all endpoint, including bot tags, team data, and external groups.
 */
public class GGBotTeam {

  private Boolean success;
  private Object error;
  private Integer statusCode;
  private BotTagDTO bots;
  private TeamDTO team;
  private List<GroupDTO> groups;

  /** @return {@code true} if the API call succeeded */
  public Boolean getSuccess() { return success; }

  /** @return error payload when the call failed, {@code null} otherwise */
  public Object getError() { return error; }

  /** @return HTTP status code returned by the endpoint */
  public Integer getStatusCode() { return statusCode; }

  /**
   * Returns the bot tag settings for GGBots.
   * 
   * @return the tag settings for GGBots
   */
  public BotTagDTO getBots() { return bots; }

  /**
   * Returns the team information associated with the bot tag settings.
   *
   * @return the team
   */
  public TeamDTO getTeam() { return team; }

  /**
   * Returns the list of groups outside the team structure.
   *
   * @return the groups outside the team
   */
  public List<GroupDTO> getGroups() { return groups; }

  /**
   * Encapsulates text with its translation key and fallback options.
   */
  public static class LocalizedText {
    private String key;
    private String fallback;
    private Map<String, String> translations;

    public LocalizedText() {}

    /**
     * Sets the plain text fallback for all languages.
     *
     * @param fallback plain text used for every language
     */
    public LocalizedText(String fallback) {
      this.fallback = fallback;
    }

    /**
     * Returns the translation key used for looking up translations.
     *
     * @return the translation key
     */
    public String getKey() { return key; }

    /**
     * Returns the default text when no translation is available.
     *
     * @return the text used when no translation exists
     */
    public String getFallback() { return fallback; }

    /**
     * Retrieves the translated text for a given language, using fallback if needed.
     *
     * @param language the short language code (e.g. {@code de})
     * @return the translation, or the fallback when there is none
     */
    public String resolve(String language) {
      String translated = translations != null ? translations.get(language) : null;
      return translated != null && !translated.isEmpty() ? translated : fallback;
    }
  }

  /**
   * Represents tag settings for GGBots, including visibility, icon, and color.
   */
  public static class BotTagDTO {
    private Boolean enabled;
    private String icon;
    private String color;

    /**
     * Indicates whether GGBots are assigned a tag.
     *
     * @return {@code true} if GGBots get a tag
     */
    public Boolean getEnabled() { return enabled; }

    /**
     * Returns the icon URL for the tag, or null for the client's default logo.
     *
     * @return the icon URL, null for the client's own logo
     */
    public String getIcon() { return icon; }

    /**
     * Returns the tag color as a hexadecimal string.
     *
     * @return the tag color as hex
     */
    public String getColor() { return color; }
  }

  /**
   * Represents the team structure, including tag, color, icon, and departments.
   */
  public static class TeamDTO {
    private LocalizedText tag;
    private String tagColor;
    private String ingameIcon;
    private List<DepartmentDTO> departments;

    /**
     * Returns the localized tag text for the team.
     *
     * @return the tag text of the team
     */
    public LocalizedText getTag() { return tag; }

    /**
     * Returns the tag color of the team as a hexadecimal string.
     *
     * @return the tag color of the team as hex
     */
    public String getTagColor() { return tagColor; }

    /**
     * Returns the icon URL for the team's in-game display, which may be null.
     *
     * @return the icon URL of the team, may be {@code null}
     */
    public String getIngameIcon() { return ingameIcon; }

    /**
     * Returns the list of departments associated with the team.
     *
     * @return the departments of the team
     */
    public List<DepartmentDTO> getDepartments() { return departments; }
  }

  /**
   * Represents a single department within a team, including name, slots, and color.
   */
  public static class DepartmentDTO {
    private LocalizedText name;
    private Integer slots;
    private String tagColor;
    private String ingameIcon;
    private List<MemberDTO> members;

    /**
     * Provides the display name of the department.
     *
     * @return display name of the department
     */
    public LocalizedText getName() { return name; }

    /**
     * Returns the maximum number of member slots available in the department.
     *
     * @return maximum number of member slots
     */
    public Integer getSlots() { return slots; }

    /**
     * Returns the tag color of the department as a hexadecimal string, which may be null.
     *
     * @return the tag color of the department as hex, may be {@code null}
     */
    public String getTagColor() { return tagColor; }

    /**
     * Returns the icon URL for the department's in-game display, which may be null.
     *
     * @return the icon URL of the department, may be {@code null}
     */
    public String getIngameIcon() { return ingameIcon; }

    /**
     * Returns the list of members belonging to the department.
     *
     * @return the members of the department
     */
    public List<MemberDTO> getMembers() { return members; }
  }

  /**
   * Represents a group outside the team, such as creators or partners.
   */
  public static class GroupDTO {
    private LocalizedText name;
    private LocalizedText tag;
    private String tagColor;
    private String ingameIcon;
    private List<MemberDTO> members;

    /**
     * Provides the display name of the group.
     *
     * @return display name of the group
     */
    public LocalizedText getName() { return name; }

    /**
     * Returns the tag text of the group, used for display purposes.
     *
     * @return the tag text of the group
     */
    public LocalizedText getTag() { return tag; }

    /**
     * Returns the tag color of the group as a hexadecimal string.
     *
     * @return the tag color of the group as hex
     */
    public String getTagColor() { return tagColor; }

    /**
     * Returns the icon URL for the group's in-game display, which may be null.
     *
     * @return the icon URL of the group, may be {@code null}
     */
    public String getIngameIcon() { return ingameIcon; }

    /**
     * Returns the list of members belonging to the group.
     *
     * @return the members of the group
     */
    public List<MemberDTO> getMembers() { return members; }
  }

  /**
   * Represents a team or group member with details that override department or group settings.
   */
  public static class MemberDTO {
    private DiscordDTO discord;
    private MinecraftDTO minecraft;
    private String image;
    private LocalizedText tag;
    private String tagColor;
    private String ingameIcon;

    /**
     * Provides access to the Discord account information for the member.
     *
     * @return Discord account information for this member
     */
    public DiscordDTO getDiscord() { return discord; }

    /**
     * Provides access to the Minecraft account information for the member.
     *
     * @return Minecraft account information for this member
     */
    public MinecraftDTO getMinecraft() { return minecraft; }

    /**
     * Provides access to the URL of the member's profile avatar.
     *
     * @return URL of the member's profile avatar
     */
    public String getImage() { return image; }

    /**
     * Returns the member's own tag text, which may be null.
     *
     * @return the member's own tag text, may be {@code null}
     */
    public LocalizedText getTag() { return tag; }

    /**
     * Returns the member's own tag color as a hexadecimal string, which may be null.
     *
     * @return the member's own tag color as hex, may be {@code null}
     */
    public String getTagColor() { return tagColor; }

    /**
     * Returns the member's own icon URL for in-game display, which may be null.
     *
     * @return the member's own icon URL, may be {@code null}
     */
    public String getIngameIcon() { return ingameIcon; }
  }

  /**
   * Represents the Discord identity of a member.
   *
   */
  public static class DiscordDTO {
    private String id;
    private String name;

    /**
     * Provides access to the Discord user ID for the member.
     *
     * @return the Discord user ID
     */
    public String getId() { return id; }

    /**
     * Provides access to the Discord name associated with the member.
     *
     * @return the Discord name
     */
    public String getName() { return name; }
  }

  /**
   * Represents the Minecraft account identity of a member.
   */
  public static class MinecraftDTO {
    private String name;
    private String uuid;

    /**
     * Provides access to the in-game Minecraft username for the member.
     *
     * @return in-game Minecraft username
     */
    public String getName() { return name; }

    /**
     * Provides access to the player's unique UUID.
     *
     * @return player UUID
     */
    public String getUuid() { return uuid; }
  }
}
