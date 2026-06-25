package de.ggbot.core.overlay;

public class OverlayLine {

  private final String id;
  private final WorldPosition from;
  private final WorldPosition to;
  private final int color;
  private final float lineWidth;

  public OverlayLine(String id, WorldPosition from, WorldPosition to, int color, float lineWidth) {
    this.id = id;
    this.from = from;
    this.to = to;
    this.color = color;
    this.lineWidth = lineWidth;
  }

  public OverlayLine(String id, WorldPosition from, WorldPosition to, int color) {
    this(id, from, to, color, 2.0f);
  }

  public String getId() {
    return id;
  }

  public WorldPosition getFrom() {
    return from;
  }

  public WorldPosition getTo() {
    return to;
  }

  public int getColor() {
    return color;
  }

  public float getLineWidth() {
    return lineWidth;
  }
}
