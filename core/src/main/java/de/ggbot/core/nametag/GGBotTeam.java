package de.ggbot.core.nametag;

import java.util.List;

public class GGBotTeam {

  private Boolean success;
  private Object error;
  private Integer statusCode;
  private List<TeamDTO> team;

  public Boolean getSuccess() { return success; }
  public Object getError() { return error; }
  public Integer getStatusCode() { return statusCode; }
  public List<TeamDTO> getTeam() { return team; }

  public static class TeamDTO {
    private String name;
    private Integer slots;
    private List<MembersDTO> members;

    public String getName() { return name; }
    public Integer getSlots() { return slots; }
    public List<MembersDTO> getMembers() { return members; }

    public static class MembersDTO {
      private DiscordDTO discord;
      private MinecraftDTO minecraft;
      private String image;
      private String ingameIcon;
      private Boolean isMember;

      public DiscordDTO getDiscord() { return discord; }
      public MinecraftDTO getMinecraft() { return minecraft; }
      public String getImage() { return image; }
      public String getIngameIcon() { return ingameIcon; }
      public Boolean getIsMember() { return isMember; }

      public static class DiscordDTO {
        private TeamDTO.MembersDTO.DiscordDTO.UserDTO user;
        private String name;

        public UserDTO getUser() { return user; }
        public String getName() { return name; }

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

      public static class MinecraftDTO {
        private String name;
        private String uuid;

        public String getName() { return name; }
        public String getUuid() { return uuid; }
      }
    }
  }
}
