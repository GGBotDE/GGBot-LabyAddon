package de.ggbot.core.overlay;

public class WorldPosition {

  private final double x;
  private final double y;
  private final double z;

  public WorldPosition(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public WorldPosition add(double dx, double dy, double dz) {
    return new WorldPosition(x + dx, y + dy, z + dz);
  }

  public double distanceTo(WorldPosition other) {
    double dx = other.x - x;
    double dy = other.y - y;
    double dz = other.z - z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof WorldPosition other)) return false;
    return Double.compare(other.x, x) == 0
        && Double.compare(other.y, y) == 0
        && Double.compare(other.z, z) == 0;
  }

  @Override
  public int hashCode() {
    long bits = Double.doubleToLongBits(x);
    bits = 31 * bits + Double.doubleToLongBits(y);
    bits = 31 * bits + Double.doubleToLongBits(z);
    return (int) (bits ^ (bits >>> 32));
  }

  @Override
  public String toString() {
    return "WorldPosition{" + x + ", " + y + ", " + z + "}";
  }
}
