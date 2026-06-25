package de.ggbot.core.overlay;

public class OverlayColor {

  public static final int RED = 0xFFFF0000;
  public static final int GREEN = 0xFF00FF00;
  public static final int BLUE = 0xFF0000FF;
  public static final int YELLOW = 0xFFFFFF00;
  public static final int CYAN = 0xFF00FFFF;
  public static final int MAGENTA = 0xFFFF00FF;
  public static final int WHITE = 0xFFFFFFFF;
  public static final int ORANGE = 0xFFFF8800;

  public static int rgba(int r, int g, int b, int a) {
    return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
  }

  public static int rgb(int r, int g, int b) {
    return rgba(r, g, b, 255);
  }

  public static int withAlpha(int color, int alpha) {
    return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
  }

  public static int getAlpha(int color) {
    return (color >> 24) & 0xFF;
  }

  public static int getRed(int color) {
    return (color >> 16) & 0xFF;
  }

  public static int getGreen(int color) {
    return (color >> 8) & 0xFF;
  }

  public static int getBlue(int color) {
    return color & 0xFF;
  }

  private OverlayColor() {}
}
