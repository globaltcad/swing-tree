package swingtree.style;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 *  A transparent {@link Graphics2D} decorator that forwards <b>every</b> drawing
 *  operation to a wrapped delegate unchanged — except plain
 *  {@link #drawString(String, float, float) drawString(String, ..)}, which it
 *  routes through {@link TextRenderCache}. SwingTree hands this to a forwarded
 *  look-and-feel UI (see {@link DynamicLaF}) so that the text of <i>any</i> LaF
 *  can be rasterised once and then blitted, without SwingTree needing to know
 *  anything about how that LaF draws.
 *
 *  <h2>Why this is correct regardless of the look-and-feel</h2>
 *  By the time a {@code BasicXxxUI}/FlatLaf/Metal/Nimbus delegate reaches a
 *  {@code g.drawString(text, x, y)} call, all of its internal logic has already
 *  collapsed into that one operation, whose pixels are — per the Java2D
 *  contract — a pure function of the string, the graphics' font, paint, text
 *  rendering hints and transform scale. {@link TextRenderCache} keys on exactly
 *  those, so it can substitute a cached image with no assumptions about the
 *  caller. Anything it is not sure it can reproduce (non-{@link Color} paints,
 *  rotated/sheared transforms, attributed strings, glyph vectors) is forwarded
 *  untouched.
 *
 *  <p><b>Type fragility:</b> a non-native {@code Graphics2D} can break LaF code
 *  that casts the graphics to a concrete type. {@link DynamicLaF} guards against
 *  this — if a proxied paint throws, it records the offending UI class and never
 *  proxies it again, repainting with the real graphics.
 */
final class CachingTextGraphics2D extends Graphics2D {

    private final Graphics2D            _g;       // the real, delegate graphics
    private final JComponent            _owner;   // component being painted (cache scope)
    private final TextRenderCache.KeyHolder _holder; // keeps this paint's keys alive

    CachingTextGraphics2D(Graphics2D delegate, JComponent owner, TextRenderCache.KeyHolder holder) {
        _g      = delegate;
        _owner  = owner;
        _holder = holder;
    }

    // ── the whole point: intercept plain text ───────────────────────────────

    @Override public void drawString(String str, float x, float y) {
        if ( !TextRenderCache.paintString(_g, _holder, str, x, y) )
            _g.drawString(str, x, y);
    }
    @Override public void drawString(String str, int x, int y) {
        drawString(str, (float) x, (float) y);
    }

    // ── create()/dispose() must keep the wrapper in place for nested paints ──

    @Override public Graphics create() {
        Graphics copy = _g.create();
        return (copy instanceof Graphics2D)
                ? new CachingTextGraphics2D((Graphics2D) copy, _owner, _holder)
                : copy;
    }
    @Override public void dispose() { _g.dispose(); }

    // ── everything else: pure delegation ────────────────────────────────────

    @Override public void draw(Shape s) { _g.draw(s); }
    @Override public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) { return _g.drawImage(img, xform, obs); }
    @Override public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) { _g.drawImage(img, op, x, y); }
    @Override public void drawRenderedImage(RenderedImage img, AffineTransform xform) { _g.drawRenderedImage(img, xform); }
    @Override public void drawRenderableImage(RenderableImage img, AffineTransform xform) { _g.drawRenderableImage(img, xform); }
    @Override public void drawString(AttributedCharacterIterator it, int x, int y) { _g.drawString(it, x, y); }
    @Override public void drawString(AttributedCharacterIterator it, float x, float y) { _g.drawString(it, x, y); }
    @Override public void drawGlyphVector(GlyphVector gv, float x, float y) { _g.drawGlyphVector(gv, x, y); }
    @Override public void fill(Shape s) { _g.fill(s); }
    @Override public boolean hit(Rectangle r, Shape s, boolean onStroke) { return _g.hit(r, s, onStroke); }
    @Override public void setComposite(Composite comp) { _g.setComposite(comp); }
    @Override public void setPaint(Paint paint) { _g.setPaint(paint); }
    @Override public void setStroke(Stroke s) { _g.setStroke(s); }
    @Override public void setRenderingHint(RenderingHints.Key k, @Nullable Object v) { _g.setRenderingHint(k, v); }
    @Override public @Nullable Object getRenderingHint(RenderingHints.Key k) { return _g.getRenderingHint(k); }
    @Override public void setRenderingHints(Map<?, ?> hints) { _g.setRenderingHints(hints); }
    @Override public void addRenderingHints(Map<?, ?> hints) { _g.addRenderingHints(hints); }
    @Override public RenderingHints getRenderingHints() { return _g.getRenderingHints(); }
    @Override public void translate(int x, int y) { _g.translate(x, y); }
    @Override public void translate(double tx, double ty) { _g.translate(tx, ty); }
    @Override public void rotate(double theta) { _g.rotate(theta); }
    @Override public void rotate(double theta, double x, double y) { _g.rotate(theta, x, y); }
    @Override public void scale(double sx, double sy) { _g.scale(sx, sy); }
    @Override public void shear(double shx, double shy) { _g.shear(shx, shy); }
    @Override public void transform(AffineTransform t) { _g.transform(t); }
    @Override public void setTransform(AffineTransform t) { _g.setTransform(t); }
    @Override public AffineTransform getTransform() { return _g.getTransform(); }
    @Override public Paint getPaint() { return _g.getPaint(); }
    @Override public Composite getComposite() { return _g.getComposite(); }
    @Override public void setBackground(Color c) { _g.setBackground(c); }
    @Override public Color getBackground() { return _g.getBackground(); }
    @Override public Stroke getStroke() { return _g.getStroke(); }
    @Override public void clip(Shape s) { _g.clip(s); }
    @Override public FontRenderContext getFontRenderContext() { return _g.getFontRenderContext(); }

    @Override public Color getColor() { return _g.getColor(); }
    @Override public void setColor(Color c) { _g.setColor(c); }
    @Override public void setPaintMode() { _g.setPaintMode(); }
    @Override public void setXORMode(Color c) { _g.setXORMode(c); }
    @Override public Font getFont() { return _g.getFont(); }
    @Override public void setFont(Font font) { _g.setFont(font); }
    @Override public FontMetrics getFontMetrics(Font f) { return _g.getFontMetrics(f); }
    @Override public Rectangle getClipBounds() { return _g.getClipBounds(); }
    @Override public void clipRect(int x, int y, int w, int h) { _g.clipRect(x, y, w, h); }
    @Override public void setClip(int x, int y, int w, int h) { _g.setClip(x, y, w, h); }
    @Override public @Nullable Shape getClip() { return _g.getClip(); }
    @Override public void setClip(@Nullable Shape clip) { _g.setClip(clip); }
    @Override public void copyArea(int x, int y, int w, int h, int dx, int dy) { _g.copyArea(x, y, w, h, dx, dy); }
    @Override public void drawLine(int x1, int y1, int x2, int y2) { _g.drawLine(x1, y1, x2, y2); }
    @Override public void fillRect(int x, int y, int w, int h) { _g.fillRect(x, y, w, h); }
    @Override public void clearRect(int x, int y, int w, int h) { _g.clearRect(x, y, w, h); }
    @Override public void drawRoundRect(int x, int y, int w, int h, int aw, int ah) { _g.drawRoundRect(x, y, w, h, aw, ah); }
    @Override public void fillRoundRect(int x, int y, int w, int h, int aw, int ah) { _g.fillRoundRect(x, y, w, h, aw, ah); }
    @Override public void drawOval(int x, int y, int w, int h) { _g.drawOval(x, y, w, h); }
    @Override public void fillOval(int x, int y, int w, int h) { _g.fillOval(x, y, w, h); }
    @Override public void drawArc(int x, int y, int w, int h, int sa, int aa) { _g.drawArc(x, y, w, h, sa, aa); }
    @Override public void fillArc(int x, int y, int w, int h, int sa, int aa) { _g.fillArc(x, y, w, h, sa, aa); }
    @Override public void drawPolyline(int[] xs, int[] ys, int n) { _g.drawPolyline(xs, ys, n); }
    @Override public void drawPolygon(int[] xs, int[] ys, int n) { _g.drawPolygon(xs, ys, n); }
    @Override public void fillPolygon(int[] xs, int[] ys, int n) { _g.fillPolygon(xs, ys, n); }
    @Override public boolean drawImage(Image img, int x, int y, ImageObserver o) { return _g.drawImage(img, x, y, o); }
    @Override public boolean drawImage(Image img, int x, int y, int w, int h, ImageObserver o) { return _g.drawImage(img, x, y, w, h, o); }
    @Override public boolean drawImage(Image img, int x, int y, Color bg, ImageObserver o) { return _g.drawImage(img, x, y, bg, o); }
    @Override public boolean drawImage(Image img, int x, int y, int w, int h, Color bg, ImageObserver o) { return _g.drawImage(img, x, y, w, h, bg, o); }
    @Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver o) { return _g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, o); }
    @Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bg, ImageObserver o) { return _g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bg, o); }

    // ── authentic delegation of the inherited java.awt.Graphics convenience methods ──
    // These are NOT abstract, so without these overrides they would fall back to
    // java.awt.Graphics' default implementations, which decompose into *different*
    // primitive calls than the wrapped SunGraphics2D would issue itself (drawChars ->
    // drawString, drawRect -> 4x drawLine, drawPolygon(Polygon) -> drawPolygon(int[]),
    // create(x,y,w,h) -> create()+translate+clipRect, ...). Forwarding them directly
    // makes this proxy an exact mirror of the wrapped graphics for everything except
    // plain drawString — so any divergence cannot be blamed on a default decomposition.
    @Override public Graphics create(int x, int y, int width, int height) {
        Graphics copy = _g.create(x, y, width, height);
        return (copy instanceof Graphics2D)
                ? new CachingTextGraphics2D((Graphics2D) copy, _owner, _holder)
                : copy;
    }
    @Override public Rectangle getClipBounds(Rectangle r) { return _g.getClipBounds(r); }
    @Override public boolean hitClip(int x, int y, int w, int h) { return _g.hitClip(x, y, w, h); }
    @Override public void drawRect(int x, int y, int w, int h) { _g.drawRect(x, y, w, h); }
    @Override public void draw3DRect(int x, int y, int w, int h, boolean raised) { _g.draw3DRect(x, y, w, h, raised); }
    @Override public void fill3DRect(int x, int y, int w, int h, boolean raised) { _g.fill3DRect(x, y, w, h, raised); }
    @Override public void drawPolygon(Polygon p) { _g.drawPolygon(p); }
    @Override public void fillPolygon(Polygon p) { _g.fillPolygon(p); }
    @Override public void drawChars(char[] data, int offset, int length, int x, int y) { _g.drawChars(data, offset, length, x, y); }
    @Override public void drawBytes(byte[] data, int offset, int length, int x, int y) { _g.drawBytes(data, offset, length, x, y); }
    @Override public FontMetrics getFontMetrics() { return _g.getFontMetrics(); }

    @Override public @Nullable GraphicsConfiguration getDeviceConfiguration() {
        GraphicsConfiguration gc = _g.getDeviceConfiguration();
        if ( gc == null && !GraphicsEnvironment.isHeadless() )
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                    .getDefaultScreenDevice().getDefaultConfiguration();
        return gc;
    }
}
