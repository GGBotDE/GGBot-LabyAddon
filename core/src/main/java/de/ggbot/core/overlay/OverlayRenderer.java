package de.ggbot.core.overlay;

import net.labymod.api.Laby;
import net.labymod.api.client.render.batch.RectangleRenderContext;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.overlay.IngameOverlayRenderEvent;
import net.labymod.api.event.client.render.world.RenderWorldEvent;
import net.labymod.api.laby3d.GameTransformations;
import net.labymod.api.util.math.vector.DoubleVector3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Renders world-space overlays (boxes and lines) by projecting their 3D world
 * coordinates into 2D screen space and drawing them with LabyMod's batched
 * {@link RectangleRenderContext}.
 *
 * <p>Both thick outlines and fills are rasterised on the CPU into thin
 * axis-aligned strips that are all added to a single batch and uploaded once per
 * frame (one draw call), so frame-rate stays high regardless of how many strips
 * are produced. Drawing lines as solid quads (rather than 1px GPU lines or
 * parallel offset lines) gives clean, gap-free thick lines at any angle.
 *
 * <p>The model-view-projection matrix and camera position are captured during
 * {@link RenderWorldEvent} (when they are valid) and reused during
 * {@link IngameOverlayRenderEvent}, where the 2D GUI stack is active.
 *
 * <p>Guideline note (per-frame allocation): the listener returns immediately when
 * there are no overlays, so it costs nothing in the common case. When overlays are
 * present, a small number of short-lived vectors are created to project corners;
 * the reusable matrix is cached in a field. The transient projection vectors are
 * unavoidable because the projection changes every frame as the camera moves.
 */
public class OverlayRenderer {

  /** Minimum clip-space w; points closer than this are behind the near plane. */
  private static final float NEAR_W = 0.05f;

  /** Player eye height; {@code renderPosition()} reports feet, the camera is the eye. */
  private static final double EYE_HEIGHT = 1.62;

  /** Overlays whose centre is further than this (blocks) are not drawn. */
  private static final double MAX_RENDER_DISTANCE = 64.0;
  private static final double MAX_RENDER_DISTANCE_SQ = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

  /** Label scaling: distance (blocks) at which a label renders at scale 1.0. */
  private static final double LABEL_BASE_DISTANCE = 8.0;
  /** Lower/upper bounds so labels never vanish or grow oversized with distance. */
  private static final float LABEL_MIN_SCALE = 0.5f;
  private static final float LABEL_MAX_SCALE = 1.0f;

  private final OverlayManager manager;

  private final Matrix4f cachedMvp = new Matrix4f();
  private double camX, camY, camZ;
  private boolean ready = false;

  // Logged once if the draw path throws, so a version-specific render API change is
  // visible in the log instead of failing silently.
  private boolean loggedDrawError = false;

  // Some versions emit the render events only with a PRE phase (not POST). Track
  // whether POST is ever seen and, if not, act on PRE so rendering still works.
  private boolean sawWorldPost = false;
  private boolean sawOverlayPost = false;

  // When set (alternate render mode), rectangles/labels are submitted to the new
  // canvas API instead of the legacy render pipeline (needed on 1.21.8+).
  private net.labymod.api.client.gui.screen.state.ScreenCanvas currentCanvas;

  public OverlayRenderer(OverlayManager manager) {
    this.manager = manager;
  }

  @Subscribe
  public void onRenderWorld(RenderWorldEvent event) {
    Phase phase = event.phase();
    if (phase == Phase.POST) {
      sawWorldPost = true;
    }
    // Prefer POST; if a version never emits POST, capture on PRE instead.
    if (!(phase == Phase.POST || (!sawWorldPost && phase == Phase.PRE))) return;
    if (!manager.hasOverlays()) return;

    DoubleVector3 pos = event.camera().renderPosition();
    camX = pos.getX();
    camZ = pos.getZ();
    // renderPosition() reports the feet on some game versions and the eye on others.
    // Normalize to the eye height by comparing against the camera entity's feet/eye:
    // only add the eye height when the reported position is actually at the feet.
    double y = pos.getY();
    net.labymod.api.client.entity.Entity camEntity =
        Laby.labyAPI().minecraft().getCameraEntity();
    if (camEntity != null) {
      double feetY = camEntity.position().getY();
      double eyeY = camEntity.eyePosition().getY();
      if (Math.abs(y - feetY) < Math.abs(y - eyeY)) {
        y += (eyeY - feetY);
      }
    } else {
      y += EYE_HEIGHT;
    }
    camY = y;

    GameTransformations t = Laby.references().gameTransformations();
    new Matrix4f(t.projectionMatrix()).mul(new Matrix4f(t.viewMatrix()), cachedMvp);

    ready = true;
  }

  private boolean tooFar(double x, double y, double z) {
    double dx = x - camX;
    double dy = y - camY;
    double dz = z - camZ;
    double max = maxRenderDistance();
    return dx * dx + dy * dy + dz * dz > max * max;
  }

  /** Configurable cull distance (blocks); falls back to the default if unavailable. */
  private double maxRenderDistance() {
    try {
      return de.ggbot.core.GGBot.getInstance().configuration().overlaySub.maxRenderDistance.get();
    } catch (Exception e) {
      return MAX_RENDER_DISTANCE;
    }
  }

  private boolean tooFar(OverlayBox box) {
    return tooFar((box.getMin().getX() + box.getMax().getX()) / 2.0,
        (box.getMin().getY() + box.getMax().getY()) / 2.0,
        (box.getMin().getZ() + box.getMax().getZ()) / 2.0);
  }

  @Subscribe
  public void onOverlayRender(IngameOverlayRenderEvent event) {
    Phase phase = event.phase();
    if (phase == Phase.POST) {
      sawOverlayPost = true;
    }
    if (!(phase == Phase.POST || (!sawOverlayPost && phase == Phase.PRE))) return;
    if (!ready || !manager.hasOverlays()) return;
    // Alternate path (1.21.8+): submit to the new canvas; otherwise use the legacy
    // render pipeline. Both come from this same event, which fires on all versions.
    currentCanvas = altRenderMode() ? event.context().canvas() : null;
    safeDraw(event.stack());
    currentCanvas = null;
  }

  private boolean altRenderMode() {
    return de.ggbot.core.utils.RenderEngine.useCanvas();
  }

  private void safeDraw(net.labymod.api.client.render.matrix.Stack stack) {
    try {
      drawOverlays(stack);
    } catch (Throwable t) {
      if (!loggedDrawError) {
        loggedDrawError = true;
        de.ggbot.core.GGBot.getInstance().logger()
            .error("[GGBot] Overlay draw failed (this version's render API may differ): "
                + t);
      }
    }
  }

  private void drawOverlays(net.labymod.api.client.render.matrix.Stack stack) {
    float sw = Laby.labyAPI().minecraft().minecraftWindow().getScaledWidth();
    float sh = Laby.labyAPI().minecraft().minecraftWindow().getScaledHeight();

    // In canvas mode there is no legacy context; rect emission goes to the canvas.
    RectangleRenderContext ctx = currentCanvas != null ? null
        : Laby.references().renderPipeline().renderContexts().rectangleRenderContext().begin(stack);

    // Fills first, then outlines on top. Far-away overlays are culled. (In canvas
    // mode each strip is an individual submission, so fills use a coarse step; turn
    // off "Fill Areas" in the settings if the frame cost is too high there.)
    for (OverlayBox box : manager.getBoxes()) {
      if (box.hasFill() && !tooFar(box)) {
        fillBox(ctx, box, sw, sh);
      }
    }
    for (OverlayBox box : manager.getBoxes()) {
      if (!tooFar(box)) {
        outlineBox(ctx, box, sw, sh);
      }
    }
    for (OverlayLine line : manager.getLines()) {
      double midX = (line.getFrom().getX() + line.getTo().getX()) / 2.0;
      double midY = (line.getFrom().getY() + line.getTo().getY()) / 2.0;
      double midZ = (line.getFrom().getZ() + line.getTo().getZ()) / 2.0;
      if (tooFar(midX, midY, midZ)) {
        continue;
      }
      Vector4f a = clipProject(line.getFrom().getX(), line.getFrom().getY(), line.getFrom().getZ());
      Vector4f b = clipProject(line.getTo().getX(), line.getTo().getY(), line.getTo().getZ());
      drawLine3D(ctx, a, b, line.getColor(), line.getLineWidth(), sw, sh);
    }

    if (ctx != null) {
      ctx.uploadToBuffer();
    }

    // Labels are drawn after the batched geometry so the text sits on top.
    for (OverlayBox box : manager.getBoxes()) {
      if (box.getLabel() != null && !tooFar(box)) {
        drawLabel(stack, box, sw, sh);
      }
    }
  }

  /** Draws a box's text label centred at its projected centre point. */
  private void drawLabel(net.labymod.api.client.render.matrix.Stack stack, OverlayBox box,
      float sw, float sh) {
    double cx = (box.getMin().getX() + box.getMax().getX()) / 2.0;
    double cy = (box.getMin().getY() + box.getMax().getY()) / 2.0;
    double cz = (box.getMin().getZ() + box.getMax().getZ()) / 2.0;
    Vector4f p = clipProject(cx, cy, cz);
    if (p.w < NEAR_W) return;
    float screenXpos = screenX(p, sw);
    float screenYpos = screenY(p, sh);

    // Scale the label with distance, clamped so it neither shrinks away nor grows
    // oversized; this keeps text legible without dominating far-away markers.
    double dx = cx - camX, dy = cy - camY, dz = cz - camZ;
    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
    float scale = (float) Math.max(LABEL_MIN_SCALE,
        Math.min(LABEL_MAX_SCALE, LABEL_BASE_DISTANCE / Math.max(dist, 0.001)));

    net.labymod.api.client.component.Component text =
        net.labymod.api.client.component.Component.text(box.getLabel(),
            net.labymod.api.client.component.format.TextColor.color(box.getOutlineColor()));
    float width = Laby.references().renderPipeline().componentRenderer().width(text);

    if (currentCanvas != null) {
      // Canvas (alternate) mode: the legacy componentRenderer().width() returns 0 on
      // these versions, so measure with RenderableComponent for correct centering.
      // Param order is (component, x, y, color, scale, maxWidth).
      float w = net.labymod.api.client.render.font.RenderableComponent.of(text).getWidth();
      float scaledWidth = w * scale;
      currentCanvas.submitComponent(text, screenXpos - scaledWidth / 2f, screenYpos,
          box.getOutlineColor(), scale, (int) Math.ceil(w));
      return;
    }

    stack.push();
    stack.translate(screenXpos, screenYpos, 0f);
    stack.scale(scale, scale, 1f);
    Laby.references().renderPipeline().componentRenderer().builder()
        .text(text)
        .pos(-width / 2f, 0f)
        .shadow(true)
        .render(stack);
    stack.pop();
  }

  // ---- boxes --------------------------------------------------------------

  private void outlineBox(RectangleRenderContext ctx, OverlayBox box, float sw, float sh) {
    Vector4f[] c = projectCorners(box);
    int col = box.getOutlineColor();
    float lw = box.getLineWidth();
    int[][] edges = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    for (int[] e : edges) {
      drawLine3D(ctx, c[e[0]], c[e[1]], col, lw, sw, sh);
    }
  }

  private void fillBox(RectangleRenderContext ctx, OverlayBox box, float sw, float sh) {
    Vector4f[] c = projectCorners(box);
    int fc = box.getFillColor();
    int[][] faces = {
        {0, 1, 2, 3}, {4, 5, 6, 7},
        {0, 1, 5, 4}, {2, 3, 7, 6},
        {0, 3, 7, 4}, {1, 2, 6, 5}
    };
    for (int[] f : faces) {
      // Faces with a corner behind the near plane are skipped (outline still draws).
      if (c[f[0]].w < NEAR_W || c[f[1]].w < NEAR_W
          || c[f[2]].w < NEAR_W || c[f[3]].w < NEAR_W) {
        continue;
      }
      float[] q = {
          screenX(c[f[0]], sw), screenY(c[f[0]], sh),
          screenX(c[f[1]], sw), screenY(c[f[1]], sh),
          screenX(c[f[2]], sw), screenY(c[f[2]], sh),
          screenX(c[f[3]], sw), screenY(c[f[3]], sh)
      };
      fillQuad(ctx, q, fc, sw, sh, true);
    }
  }

  // ---- line drawing -------------------------------------------------------

  /**
   * Clips a 3D line to the near plane, then draws it as a solid quad of the given
   * pixel width (so it is a clean thick line at any screen angle).
   */
  private void drawLine3D(RectangleRenderContext ctx, Vector4f a, Vector4f b,
      int color, float width, float sw, float sh) {
    float aw = a.w, bw = b.w;
    if (aw < NEAR_W && bw < NEAR_W) return;

    float ax = a.x, ay = a.y, bx = b.x, by = b.y;
    if (aw < NEAR_W) {
      float t = (NEAR_W - aw) / (bw - aw);
      ax += t * (bx - ax);
      ay += t * (by - ay);
      aw = NEAR_W;
    } else if (bw < NEAR_W) {
      float t = (NEAR_W - bw) / (aw - bw);
      bx += t * (ax - bx);
      by += t * (ay - by);
      bw = NEAR_W;
    }

    float x1 = (1f + ax / aw) * 0.5f * sw;
    float y1 = (1f - ay / aw) * 0.5f * sh;
    float x2 = (1f + bx / bw) * 0.5f * sw;
    float y2 = (1f - by / bw) * 0.5f * sh;

    float dx = x2 - x1;
    float dy = y2 - y1;
    float len = (float) Math.sqrt(dx * dx + dy * dy);
    if (len < 0.001f) return;

    float hw = Math.max(width, 1f) * 0.5f;
    float px = (-dy / len) * hw;
    float py = (dx / len) * hw;

    float[] quad = {
        x1 + px, y1 + py,
        x2 + px, y2 + py,
        x2 - px, y2 - py,
        x1 - px, y1 - py
    };
    fillQuad(ctx, quad, color, sw, sh, false);
  }

  // ---- convex-quad rasterisation -----------------------------------------

  /**
   * Fills a convex quad (4 points: x0,y0,x1,y1,x2,y2,x3,y3) by scanning along its
   * dominant axis and emitting thin axis-aligned strips into the batch. Scanning
   * along the longer axis ensures thin/slanted shapes never fall between sample
   * rows and vanish.
   */
  /** Emits one rectangle to the canvas (alternate mode) or the legacy context. */
  private void emitRect(RectangleRenderContext ctx, float x1, float y1, float x2, float y2,
      int color) {
    if (currentCanvas != null) {
      currentCanvas.submitAbsoluteRect(x1, y1, x2, y2, color);
    } else if (ctx != null) {
      ctx.render(x1, y1, x2, y2, color);
    }
  }

  private void fillQuad(RectangleRenderContext ctx, float[] q, int color, float sw, float sh,
      boolean fill) {
    float minX = Math.min(Math.min(q[0], q[2]), Math.min(q[4], q[6]));
    float maxX = Math.max(Math.max(q[0], q[2]), Math.max(q[4], q[6]));
    float minY = Math.min(Math.min(q[1], q[3]), Math.min(q[5], q[7]));
    float maxY = Math.max(Math.max(q[1], q[3]), Math.max(q[5], q[7]));

    boolean scanVertical = (maxX - minX) > (maxY - minY);
    // Each strip is its own (expensive) canvas submission, so use a coarser step in
    // canvas mode to keep the submission count - and the frame cost - manageable.
    // Fills cover far more area than outlines, so they step even coarser.
    int step = currentCanvas == null ? 1 : (fill ? 6 : 3);

    if (scanVertical) {
      int start = (int) Math.max(0, Math.floor(minX));
      int end = (int) Math.min(sw, Math.ceil(maxX));
      for (int x = start; x < end; x += step) {
        float c = x + 0.5f;
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
          float ax = q[i * 2], ay = q[i * 2 + 1];
          int j = (i + 1) % 4;
          float bx = q[j * 2], by = q[j * 2 + 1];
          if ((ax <= c && bx > c) || (bx <= c && ax > c)) {
            float t = (c - ax) / (bx - ax);
            float y = ay + t * (by - ay);
            if (y < lo) lo = y;
            if (y > hi) hi = y;
          }
        }
        if (hi > lo) {
          float yl = Math.max(0, lo);
          float yr = Math.min(sh, hi);
          if (yr > yl) emitRect(ctx, x, yl, x + step, yr, color);
        }
      }
    } else {
      int start = (int) Math.max(0, Math.floor(minY));
      int end = (int) Math.min(sh, Math.ceil(maxY));
      for (int y = start; y < end; y += step) {
        float c = y + 0.5f;
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
          float ax = q[i * 2], ay = q[i * 2 + 1];
          int j = (i + 1) % 4;
          float bx = q[j * 2], by = q[j * 2 + 1];
          if ((ay <= c && by > c) || (by <= c && ay > c)) {
            float t = (c - ay) / (by - ay);
            float x = ax + t * (bx - ax);
            if (x < lo) lo = x;
            if (x > hi) hi = x;
          }
        }
        if (hi > lo) {
          float xl = Math.max(0, lo);
          float xr = Math.min(sw, hi);
          if (xr > xl) emitRect(ctx, xl, y, xr, y + step, color);
        }
      }
    }
  }

  // ---- projection ---------------------------------------------------------

  private Vector4f[] projectCorners(OverlayBox box) {
    double x0 = box.getMin().getX(), y0 = box.getMin().getY(), z0 = box.getMin().getZ();
    double x1 = box.getMax().getX(), y1 = box.getMax().getY(), z1 = box.getMax().getZ();
    return new Vector4f[]{
        clipProject(x0, y0, z0), clipProject(x1, y0, z0),
        clipProject(x1, y1, z0), clipProject(x0, y1, z0),
        clipProject(x0, y0, z1), clipProject(x1, y0, z1),
        clipProject(x1, y1, z1), clipProject(x0, y1, z1)
    };
  }

  private float screenX(Vector4f p, float sw) {
    return (1f + p.x / p.w) * 0.5f * sw;
  }

  private float screenY(Vector4f p, float sh) {
    return (1f - p.y / p.w) * 0.5f * sh;
  }

  private Vector4f clipProject(double wx, double wy, double wz) {
    Vector4f p = new Vector4f(
        (float) (wx - camX), (float) (wy - camY), (float) (wz - camZ), 1.0f);
    cachedMvp.transform(p);
    return p;
  }
}
