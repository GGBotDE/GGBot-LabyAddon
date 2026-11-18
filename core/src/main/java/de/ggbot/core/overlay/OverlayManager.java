package de.ggbot.core.overlay;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OverlayManager {

  private static OverlayManager instance;

  private final List<OverlayLine> lines = new CopyOnWriteArrayList<>();
  private final List<OverlayBox> boxes = new CopyOnWriteArrayList<>();

  // These values are accessed multiple times per frame by the renderer,
  // so their read-only views are created once and reused. Since the underlying
  // lists use copy-on-write, live views can be safely iterated without creating
  // separate snapshots.
  private final List<OverlayLine> lineView = Collections.unmodifiableList(lines);
  private final List<OverlayBox> boxView = Collections.unmodifiableList(boxes);

  public static OverlayManager getInstance() {
    if (instance == null) {
      instance = new OverlayManager();
    }
    return instance;
  }

  public void addLine(OverlayLine line) {
    removeLineById(line.getId());
    lines.add(line);
  }

  public void addBox(OverlayBox box) {
    removeBoxById(box.getId());
    boxes.add(box);
  }

  public void removeLine(OverlayLine line) {
    lines.remove(line);
  }

  public void removeBox(OverlayBox box) {
    boxes.remove(box);
  }

  public boolean removeLineById(String id) {
    return lines.removeIf(l -> l.getId().equals(id));
  }

  public boolean removeBoxById(String id) {
    return boxes.removeIf(b -> b.getId().equals(id));
  }

  public boolean removeById(String id) {
    boolean removed = removeLineById(id);
    removed |= removeBoxById(id);
    return removed;
  }

  public void clearLines() {
    lines.clear();
  }

  public void clearBoxes() {
    boxes.clear();
  }

  public void clearAll() {
    lines.clear();
    boxes.clear();
  }

  public List<OverlayLine> getLines() {
    return lineView;
  }

  public List<OverlayBox> getBoxes() {
    return boxView;
  }

  public boolean hasOverlays() {
    return !lines.isEmpty() || !boxes.isEmpty();
  }
}
