package de.ggbot.core.overlay;

public class OverlayBox {

  private final String id;
  private final WorldPosition min;
  private final WorldPosition max;
  private final int outlineColor;
  private final int fillColor;
  private final float lineWidth;
  private String label;

  public OverlayBox(String id, WorldPosition min, WorldPosition max,
      int outlineColor, int fillColor, float lineWidth) {
    this.id = id;
    this.min = min;
    this.max = max;
    this.outlineColor = outlineColor;
    this.fillColor = fillColor;
    this.lineWidth = lineWidth;
  }

  /** Sets an optional text label drawn at the box centre, and returns this box. */
  public OverlayBox withLabel(String label) {
    this.label = label;
    return this;
  }

  /** Returns the optional text label, or {@code null} if none. */
  public String getLabel() {
    return label;
  }

  public OverlayBox(String id, WorldPosition min, WorldPosition max, int outlineColor) {
    this(id, min, max, outlineColor, 0, 2.0f);
  }

  public static OverlayBox singleBlock(String id, int blockX, int blockY, int blockZ,
      int outlineColor) {
    return new OverlayBox(id,
        new WorldPosition(blockX, blockY, blockZ),
        new WorldPosition(blockX + 1, blockY + 1, blockZ + 1),
        outlineColor);
  }

  public static OverlayBox singleBlock(String id, int blockX, int blockY, int blockZ,
      int outlineColor, int fillColor) {
    return new OverlayBox(id,
        new WorldPosition(blockX, blockY, blockZ),
        new WorldPosition(blockX + 1, blockY + 1, blockZ + 1),
        outlineColor, fillColor, 2.0f);
  }

  public static OverlayBox singleBlock(String id, int blockX, int blockY, int blockZ,
      int outlineColor, float lineWidth) {
    return new OverlayBox(id,
        new WorldPosition(blockX, blockY, blockZ),
        new WorldPosition(blockX + 1, blockY + 1, blockZ + 1),
        outlineColor, 0, lineWidth);
  }

  public static OverlayBox singleBlock(String id, int blockX, int blockY, int blockZ,
      int outlineColor, int fillColor, float lineWidth) {
    return new OverlayBox(id,
        new WorldPosition(blockX, blockY, blockZ),
        new WorldPosition(blockX + 1, blockY + 1, blockZ + 1),
        outlineColor, fillColor, lineWidth);
  }

  public static OverlayBox area(String id,
      int x1, int y1, int z1, int x2, int y2, int z2,
      int outlineColor, float lineWidth) {
    return new OverlayBox(id,
        new WorldPosition(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
        new WorldPosition(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)),
        outlineColor, 0, lineWidth);
  }

  public static OverlayBox area(String id,
      int x1, int y1, int z1, int x2, int y2, int z2,
      int outlineColor, int fillColor, float lineWidth) {
    return new OverlayBox(id,
        new WorldPosition(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
        new WorldPosition(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)),
        outlineColor, fillColor, lineWidth);
  }

  public static OverlayBox area(String id,
      int x1, int y1, int z1, int x2, int y2, int z2,
      int outlineColor) {
    return new OverlayBox(id,
        new WorldPosition(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
        new WorldPosition(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)),
        outlineColor);
  }

  public static OverlayBox area(String id,
      int x1, int y1, int z1, int x2, int y2, int z2,
      int outlineColor, int fillColor) {
    return new OverlayBox(id,
        new WorldPosition(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
        new WorldPosition(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)),
        outlineColor, fillColor, 2.0f);
  }

  public String getId() {
    return id;
  }

  public WorldPosition getMin() {
    return min;
  }

  public WorldPosition getMax() {
    return max;
  }

  public int getOutlineColor() {
    return outlineColor;
  }

  public int getFillColor() {
    return fillColor;
  }

  public boolean hasFill() {
    return OverlayColor.getAlpha(fillColor) > 0;
  }

  public float getLineWidth() {
    return lineWidth;
  }
}
