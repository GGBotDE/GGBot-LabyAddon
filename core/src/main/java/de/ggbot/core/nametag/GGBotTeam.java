package de.ggbot.core.nametag;

import java.util.List;

/**
 * API response model for the GGBot team endpoint.
 * Contains the team roster with member Discord and Minecraft account data,
 * nametag icon URLs, and member-status flags.
 */
public class GGBotTeam {

  private Boolean success;
  private Object error;
  private Integer statusCode;
  private List<TeamDTO> team;

  /** @return {@code true} if the API call succeeded */
  public Boolean getSuccess() { return success; }

  /** @return error payload when the call failed, {@code null} otherwise */
  public Object getError() { return error; }

  /** @return HTTP status code returned by the endpoint */
  public Integer getStatusCode() { return statusCode; }

  /** @return list of teams the queried player belongs to */
  public List<TeamDTO> getTeam() { return team; }

  /** A single team entry returned by the API. */
  public static class TeamDTO {
    private String name;
    private Integer slots;
    private List<MembersDTO> members;

    /** @return display name of the team */
    public String getName() { return name; }

    /** @return maximum number of member slots in this team */
    public Integer getSlots() { return slots; }

    /** @return list of current team members */
    public List<MembersDTO> getMembers() { return members; }

/** A single member entry combining Discord and Minecraft identity. */
      public static class MembersDTO {
        private DiscordDTO discord;
        private MinecraftDTO minecraft;
        private String image;
        private String ingameIcon;
        private Boolean isMember;

        /** @return Discord account information for this member */
        public DiscordDTO getDiscord() { return discord; }

        /** @return Minecraft account information for this member */
        public MinecraftDTO getMinecraft() { return minecraft; }

        /** @return URL of the member's profile avatar */
        public String getImage() { return image; }

        /** @return URL of the icon rendered above the player's head in-game */
        public String getIngameIcon() { return ingameIcon; }

        /** @return {@code true} if this person is an active team member */
      public Boolean getIsMember() { return isMember; }

/** Discord identity of a team member. */
        public static class DiscordDTO {
        private TeamDTO.MembersDTO.DiscordDTO.UserDTO user;
        private String name;

        public UserDTO getUser() { return user; }
        public String getName() { return name; }

          /** Full Discord user payload as returned by the Discord.js API. */
          public static class UserDTO {
          private String id;
          private Boolean bot;
          private Boolean system;
          private Integer flags;
          private String username;
          private String globalName;
          private String discriminator;
          private String avatar;
          private Object banner;
          private Integer accentColor;
          private Object avatarDecoration;
          private Long createdTimestamp;
          private String defaultAvatarURL;
          private String hexAccentColor;
          private String tag;
          private String avatarURL;
          private String displayAvatarURL;
          private Object bannerURL;

          public String getId() { return id; }
          public Boolean getBot() { return bot; }
          public Boolean getSystem() { return system; }
          public Integer getFlags() { return flags; }
          public String getUsername() { return username; }
          public String getGlobalName() { return globalName; }
          public String getDiscriminator() { return discriminator; }
          public String getAvatar() { return avatar; }
          public Object getBanner() { return banner; }
          public Integer getAccentColor() { return accentColor; }
          public Object getAvatarDecoration() { return avatarDecoration; }
          public Long getCreatedTimestamp() { return createdTimestamp; }
          public String getDefaultAvatarURL() { return defaultAvatarURL; }
          public String getHexAccentColor() { return hexAccentColor; }
          public String getTag() { return tag; }
          public String getAvatarURL() { return avatarURL; }
          public String getDisplayAvatarURL() { return displayAvatarURL; }
          public Object getBannerURL() { return bannerURL; }
        }
      }

      /** Minecraft account identity of a team member. */
      public static class MinecraftDTO {
        private String name;
        private String uuid;

        /** @return in-game Minecraft username */
        public String getName() { return name; }

        /** @return player UUID */
        public String getUuid() { return uuid; }
      }
    }
  }
}
