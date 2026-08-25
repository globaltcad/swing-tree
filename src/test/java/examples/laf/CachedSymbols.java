package examples.laf;

import examples.laf.SwingTreeLookAndFeel.Palette;
import swingtree.UI;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 *  A {@link Symbols} set which rasterizes each glyph once and then blits the result, the way
 *  Nimbus does through {@code sun.swing.CachedPainter}. A check mark costs a rounded rectangle,
 *  an antialiased stroke, a fresh {@code BasicStroke} and a vertical gradient every time it is
 *  asked for, and a list or a menu asks for one per visible row on every repaint; a blit of the
 *  same pixels costs a memory copy the graphics card can usually do itself.
 *  <p>
 *  Memoising a painter is only sound if it is a pure function of its arguments, which the
 *  {@link Symbols} contract already requires - nothing there reads a component. The three inputs
 *  it takes which are <i>not</i> arguments are all part of the key: the
 *  {@linkplain UI#scale() user scale factor}, because every symbol set scales its own geometry
 *  through it, and the two axes of the {@link Graphics2D}'s own transform, because a screen at
 *  200% has to be handed twice the pixels rather than the same ones enlarged. The palette is the
 *  fourth such input, and is handled by tying the cache to a configuration rather than by keying
 *  on it: the wrapper is built by {@link SwingTreeLookAndFeel.Conf} against that configuration's
 *  palette, so re-initialising the look and feel starts from an empty cache and the previous
 *  theme's tiles are collected with it. A call arriving with any other palette is drawn straight
 *  through, so the guarantee holds even if some caller resolves its colours elsewhere.
 *  <p>
 *  Two of the fourteen painting methods are always drawn straight through:
 *  {@link #paintSliderTrack} and {@link #paintProgressFill} are both handed a <i>position</i>
 *  rather than a state - a thumb centre and a completion ratio - so consecutive calls almost never
 *  repeat, and a cache of them would be a table of single-use entries the width of the control.
 *  <p>
 *  Everything else is stored in device pixels under a bleed margin, because a symbol may draw
 *  outside the box it is given: Material grows a translucent halo five developer pixels beyond a
 *  check box and a slider handle, and cropping a tile to the box would cut it off.
 */
final class CachedSymbols implements Symbols
{
    /** How far outside its box a symbol may draw and still be cached, in developer pixels. */
    private static final int BLEED = 8;

    /**
     *  The largest tile worth storing, in device pixels. Above it the blit is no longer obviously
     *  cheaper than the drawing - a large tile is mostly one flat fill, which is what a rasterizer
     *  is fastest at - and the tiles start crowding the budget out.
     */
    private static final long MAX_TILE_PIXELS = 256L * 256L;

    /** How much the whole cache may hold before the least recently used tiles are dropped. */
    private static final long MAX_BYTES = 4L * 1024 * 1024;

    /**
     *  How finely the position a tile is drawn at is remembered, in steps per device pixel. A
     *  tile can only be laid down on a whole pixel, but at a screen scale of 150% a component
     *  pixel lands on half of one, and rounding that away moves an antialiased edge by enough to
     *  see. So the leftover fraction is rasterized <i>into</i> the tile and keyed on, which makes
     *  a glyph at a half-pixel offset a different tile rather than a shifted one. Rounding the
     *  fraction to a thousandth of a pixel keeps that a handful of tiles rather than one per
     *  position: at 150% a whole-numbered coordinate can only ever land on a half.
     */
    private static final double PHASE_STEPS = 1024;

    /** Which piece of geometry a tile holds, so that two of them are never mistaken for each other. */
    private enum Symbol
    {
        CHECK, RADIO, DISCLOSURE, SUBMENU_ARROW, COMBO_ARROW, SPINNER_ARROW,
        SLIDER_THUMB, SCROLL_THUMB, SPLIT_GRIP, DRAG_HANDLE, TAB_SURFACE, TAB_ACCENT
    }

    private final Symbols _symbols;
    private final Palette _palette;

    /** Least-recently-used first, which is the order {@link #_store} evicts in. */
    private final Map<Key, BufferedImage> _tiles = new LinkedHashMap<>(64, 0.75f, true);
    private long _bytes = 0;

    CachedSymbols( Symbols symbols, Palette palette ) {
        _symbols = Objects.requireNonNull(symbols);
        _palette = Objects.requireNonNull(palette);
    }

    @Override public boolean drawsItsOwnChrome() { return _symbols.drawsItsOwnChrome(); }

    // ── Metrics, answered by the wrapped set ─────────────────────────────

    @Override public int checkGlyphSize()        { return _symbols.checkGlyphSize(); }
    @Override public int arrowGlyphSize()        { return _symbols.arrowGlyphSize(); }
    @Override public int comboArrowButtonSize()  { return _symbols.comboArrowButtonSize(); }
    @Override public int spinnerButtonWidth()    { return _symbols.spinnerButtonWidth(); }
    @Override public int spinnerButtonHeight()   { return _symbols.spinnerButtonHeight(); }
    @Override public int sliderThumbDiameter()   { return _symbols.sliderThumbDiameter(); }
    @Override public int sliderTrackThickness()  { return _symbols.sliderTrackThickness(); }
    @Override public int scrollBarThickness()    { return _symbols.scrollBarThickness(); }
    @Override public int splitDividerThickness() { return _symbols.splitDividerThickness(); }
    @Override public int progressBarThickness()  { return _symbols.progressBarThickness(); }
    @Override public int separatorThickness()    { return _symbols.separatorThickness(); }
    @Override public int tableRowHeight()        { return _symbols.tableRowHeight(); }
    @Override public int treeRowHeight()         { return _symbols.treeRowHeight(); }
    @Override public int tabPaddingVertical()    { return _symbols.tabPaddingVertical(); }
    @Override public int tabPaddingHorizontal()  { return _symbols.tabPaddingHorizontal(); }
    @Override public int tabAreaGap()            { return _symbols.tabAreaGap(); }

    // ── Glyphs in front of a label ───────────────────────────────────────

    @Override
    public void paintCheckGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    ) {
        _paint(g, p, Symbol.CHECK, _bits(enabled, focused, rollover, pressed, selected), x, y, w, h,
               (tile, tx, ty) -> _symbols.paintCheckGlyph(
                       tile, p, tx, ty, w, h, enabled, focused, rollover, pressed, selected));
    }

    @Override
    public void paintRadioGlyph(
        Graphics2D g, Palette p, int x, int y, int w, int h,
        boolean enabled, boolean focused, boolean rollover, boolean pressed, boolean selected
    ) {
        _paint(g, p, Symbol.RADIO, _bits(enabled, focused, rollover, pressed, selected), x, y, w, h,
               (tile, tx, ty) -> _symbols.paintRadioGlyph(
                       tile, p, tx, ty, w, h, enabled, focused, rollover, pressed, selected));
    }

    // ── Arrows ───────────────────────────────────────────────────────────

    @Override
    public void paintDisclosure(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean expanded, boolean enabled
    ) {
        _paint(g, p, Symbol.DISCLOSURE, _bits(expanded, enabled), x, y, w, h,
               (tile, tx, ty) -> _symbols.paintDisclosure(tile, p, tx, ty, w, h, expanded, enabled));
    }

    @Override
    public void paintSubmenuArrow( Graphics2D g, Palette p, int x, int y, int w, int h, boolean enabled ) {
        _paint(g, p, Symbol.SUBMENU_ARROW, _bits(enabled), x, y, w, h,
               (tile, tx, ty) -> _symbols.paintSubmenuArrow(tile, p, tx, ty, w, h, enabled));
    }

    @Override
    public void paintComboArrow(
        Graphics2D g, Palette p, int w, int h, boolean enabled, boolean rollover, boolean pressed
    ) {
        _paint(g, p, Symbol.COMBO_ARROW, _bits(enabled, rollover, pressed), 0, 0, w, h,
               (tile, tx, ty) -> _symbols.paintComboArrow(tile, p, w, h, enabled, rollover, pressed));
    }

    @Override
    public void paintSpinnerArrow(
        Graphics2D g, Palette p, int w, int h, boolean up,
        boolean enabled, boolean rollover, boolean pressed
    ) {
        _paint(g, p, Symbol.SPINNER_ARROW, _bits(up, enabled, rollover, pressed), 0, 0, w, h,
               (tile, tx, ty) -> _symbols.paintSpinnerArrow(tile, p, w, h, up, enabled, rollover, pressed));
    }

    // ── Chrome ───────────────────────────────────────────────────────────

    /**
     *  Drawn straight through: the filled side of the groove ends wherever the handle currently
     *  is, so the picture changes with every pixel the handle moves and a stored one would be
     *  used once.
     */
    @Override
    public void paintSliderTrack(
        Graphics2D g, Palette p, Rectangle track, int thumbCentre,
        boolean horizontal, boolean inverted, boolean enabled
    ) {
        _symbols.paintSliderTrack(g, p, track, thumbCentre, horizontal, inverted, enabled);
    }

    @Override
    public void paintSliderThumb( Graphics2D g, Palette p, Rectangle thumb, boolean enabled, boolean focused ) {
        _paint(g, p, Symbol.SLIDER_THUMB, _bits(enabled, focused), thumb.x, thumb.y, thumb.width, thumb.height,
               (tile, tx, ty) -> _symbols.paintSliderThumb(
                       tile, p, new Rectangle(tx, ty, thumb.width, thumb.height), enabled, focused));
    }

    @Override
    public void paintScrollThumb( Graphics2D g, Palette p, Rectangle thumb, boolean active ) {
        _paint(g, p, Symbol.SCROLL_THUMB, _bits(active), thumb.x, thumb.y, thumb.width, thumb.height,
               (tile, tx, ty) -> _symbols.paintScrollThumb(
                       tile, p, new Rectangle(tx, ty, thumb.width, thumb.height), active));
    }

    @Override
    public void paintSplitGrip( Graphics2D g, Palette p, int w, int h, boolean horizontalSplit, boolean enabled ) {
        _paint(g, p, Symbol.SPLIT_GRIP, _bits(horizontalSplit, enabled), 0, 0, w, h,
               (tile, tx, ty) -> _symbols.paintSplitGrip(tile, p, w, h, horizontalSplit, enabled));
    }

    @Override
    public void paintDragHandle( Graphics2D g, Palette p, int w, int h, boolean horizontal ) {
        _paint(g, p, Symbol.DRAG_HANDLE, _bits(horizontal), 0, 0, w, h,
               (tile, tx, ty) -> _symbols.paintDragHandle(tile, p, w, h, horizontal));
    }

    /**
     *  Drawn straight through: the ratio is a position rather than a state, so an animating bar
     *  would leave one entry behind per frame and read none of them twice.
     */
    @Override
    public void paintProgressFill(
        Graphics2D g, Palette p, int w, int h, double ratio, boolean horizontal, boolean enabled
    ) {
        _symbols.paintProgressFill(g, p, w, h, ratio, horizontal, enabled);
    }

    @Override
    public void paintTabSurface(
        Graphics2D g, Palette p, int x, int y, int w, int h, boolean selected, boolean rollover
    ) {
        _paint(g, p, Symbol.TAB_SURFACE, _bits(selected, rollover), x, y, w, h,
               (tile, tx, ty) -> _symbols.paintTabSurface(tile, p, tx, ty, w, h, selected, rollover));
    }

    @Override
    public void paintTabAccent(
        Graphics2D g, Palette p, int x, int y, int w, int h, int tabPlacement, boolean enabled
    ) {
        _paint(g, p, Symbol.TAB_ACCENT, tabPlacement << 1 | _bits(enabled), x, y, w, h,
               (tile, tx, ty) -> _symbols.paintTabAccent(tile, p, tx, ty, w, h, tabPlacement, enabled));
    }

    // ── Internals ────────────────────────────────────────────────────────

    /** What a symbol set is asked to do, once, so that the result can be kept. */
    private interface Drawing
    {
        /**
         *  @param g the surface to draw on, which is either the caller's or a tile's
         *  @param x the left edge to draw from, which is the caller's for a direct drawing and
         *           zero for a tile
         *  @param y the top edge to draw from
         */
        void draw( Graphics2D g, int x, int y );
    }

    /**
     *  Draws {@code drawing} through a stored tile, or straight onto {@code g} where a tile would
     *  be wrong or would not pay for itself.
     *
     * @param g the caller's surface
     * @param p the palette the caller resolved, which must be this cache's own to hit it
     * @param symbol which piece of geometry this is
     * @param flags the state the drawing depends on, packed
     * @param x the left edge the symbol is drawn from, in component pixels
     * @param y the top edge the symbol is drawn from
     * @param w the width the symbol was given
     * @param h the height the symbol was given
     * @param drawing what to do on a miss
     */
    private void _paint(
        Graphics2D g, Palette p, Symbol symbol, int flags, int x, int y, int w, int h, Drawing drawing
    ) {
        if ( w <= 0 || h <= 0 )
            return;

        AffineTransform transform = g.getTransform();
        double scaleX = transform.getScaleX();
        double scaleY = transform.getScaleY();
        boolean upright = transform.getShearX() == 0 && transform.getShearY() == 0
                       && scaleX > 0 && scaleY > 0;
        if ( p != _palette || !upright ) {
            drawing.draw(g, x, y);
            return;
        }

        int  margin = Math.max(1, UI.scale(BLEED));
        int  tileW  = (int) Math.ceil((w + 2 * margin) * scaleX) + 1;
        int  tileH  = (int) Math.ceil((h + 2 * margin) * scaleY) + 1;
        if ( (long) tileW * tileH > MAX_TILE_PIXELS ) {
            drawing.draw(g, x, y);
            return;
        }

        Point2D corner = transform.transform(new Point2D.Double(x - margin, y - margin), null);
        int     left   = (int) Math.floor(corner.getX());
        int     top    = (int) Math.floor(corner.getY());
        double  phaseX = _phase(corner.getX() - left);
        double  phaseY = _phase(corner.getY() - top);
        if ( phaseX >= 1 ) { left++; phaseX = 0; }
        if ( phaseY >= 1 ) { top++;  phaseY = 0; }

        Key           key  = new Key(symbol, flags, w, h, UI.scale(), scaleX, scaleY, phaseX, phaseY);
        BufferedImage tile = _lookUp(key);
        if ( tile == null ) {
            tile = new BufferedImage(tileW, tileH, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D tileGraphics = tile.createGraphics();
            try {
                tileGraphics.setRenderingHints(g.getRenderingHints());
                tileGraphics.translate(phaseX, phaseY);
                tileGraphics.scale(scaleX, scaleY);
                tileGraphics.translate(margin, margin);
                drawing.draw(tileGraphics, 0, 0);
            } finally {
                tileGraphics.dispose();
            }
            _store(key, tile);
        }

        // Laid down one image pixel per device pixel, so what was rasterized at the screen's
        // resolution is not resampled back down to the component's on its way there.
        Graphics2D blit = (Graphics2D) g.create();
        try {
            blit.setTransform(new AffineTransform());
            blit.drawImage(tile, left, top, null);
        } finally {
            blit.dispose();
        }
    }

    private static double _phase( double fraction ) {
        return Math.rint(fraction * PHASE_STEPS) / PHASE_STEPS;
    }

    private BufferedImage _lookUp( Key key ) {
        synchronized ( _tiles ) {
            return _tiles.get(key);
        }
    }

    private void _store( Key key, BufferedImage tile ) {
        synchronized ( _tiles ) {
            BufferedImage replaced = _tiles.put(key, tile);
            if ( replaced != null )
                _bytes -= _bytesOf(replaced);
            _bytes += _bytesOf(tile);
            Iterator<Map.Entry<Key, BufferedImage>> leastRecentlyUsed = _tiles.entrySet().iterator();
            while ( _bytes > MAX_BYTES && leastRecentlyUsed.hasNext() ) {
                _bytes -= _bytesOf(leastRecentlyUsed.next().getValue());
                leastRecentlyUsed.remove();
            }
        }
    }

    private static long _bytesOf( BufferedImage tile ) {
        return (long) tile.getWidth() * tile.getHeight() * 4;
    }

    private static int _bits( boolean... flags ) {
        int packed = 0;
        for ( int i = 0; i < flags.length; i++ )
            if ( flags[i] )
                packed |= 1 << i;
        return packed;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + _symbols.getClass().getSimpleName() + "]";
    }

    /** Everything a stored tile's pixels depend on, other than the palette the cache is tied to. */
    private static final class Key
    {
        private final Symbol _symbol;
        private final int    _flags;
        private final int    _width;
        private final int    _height;
        private final float  _userScale;
        private final double _scaleX;
        private final double _scaleY;
        private final double _phaseX;
        private final double _phaseY;
        private final int    _hash;

        Key(
            Symbol symbol, int flags, int width, int height,
            float userScale, double scaleX, double scaleY, double phaseX, double phaseY
        ) {
            _symbol    = symbol;
            _flags     = flags;
            _width     = width;
            _height    = height;
            _userScale = userScale;
            _scaleX    = scaleX;
            _scaleY    = scaleY;
            _phaseX    = phaseX;
            _phaseY    = phaseY;
            _hash      = Objects.hash(symbol, flags, width, height, userScale, scaleX, scaleY, phaseX, phaseY);
        }

        @Override
        public boolean equals( Object other ) {
            if ( this == other ) return true;
            if ( !(other instanceof Key) ) return false;
            Key that = (Key) other;
            return _symbol == that._symbol
                && _flags  == that._flags
                && _width  == that._width
                && _height == that._height
                && Float .compare(_userScale, that._userScale) == 0
                && Double.compare(_scaleX,    that._scaleX)    == 0
                && Double.compare(_scaleY,    that._scaleY)    == 0
                && Double.compare(_phaseX,    that._phaseX)    == 0
                && Double.compare(_phaseY,    that._phaseY)    == 0;
        }

        @Override public int hashCode() { return _hash; }

        @Override
        public String toString() {
            return "Key[" + _symbol + ", flags=" + _flags + ", " + _width + "x" + _height + "]";
        }
    }
}
