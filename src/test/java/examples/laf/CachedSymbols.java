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
 *  A {@link Symbols} set which rasterizes each glyph once and blits the result afterwards, the way
 *  Nimbus does through {@code sun.swing.CachedPainter}. Drawing a check mark costs a rounded
 *  rectangle, an antialiased stroke, a fresh {@code BasicStroke} and a vertical gradient, a list or
 *  a menu asks for one per visible row on every repaint, and a blit of the same pixels is a memory
 *  copy the graphics card can usually do by itself.
 *  <p>
 *  Remembering what a painter drew is only sound if the painter is a pure function of its
 *  arguments, which the {@link Symbols} contract already demands. Three of its inputs are not
 *  arguments and are therefore part of the key: the {@linkplain UI#scale() user scale factor},
 *  which every symbol set scales its geometry through, and the two axes of the {@link Graphics2D}
 *  transform, because a screen at 200% has to be handed twice the pixels rather than the same ones
 *  enlarged. The palette is the fourth, and is handled by lifetime instead of by key:
 *  {@link SwingTreeLookAndFeel.Conf} builds this wrapper against its own palette, so re-initialising
 *  the look and feel starts from an empty cache and drops the previous theme's tiles. A call
 *  arriving with a different palette is drawn straight through.
 *  <p>
 *  {@link #paintSliderTrack} and {@link #paintProgressFill} are never stored, because both are
 *  handed a position rather than a state and would fill the cache with entries used once.
 *  <p>
 *  Tiles are stored in device pixels with a margin around the box the symbol was given, because a
 *  symbol may draw outside it: Material grows a translucent halo five developer pixels beyond a
 *  check box and a slider handle.
 */
final class CachedSymbols implements Symbols
{
    /** How far outside its box a symbol may draw and still be cached, in developer pixels. */
    private static final int BLEED = 8;

    /**
     *  The largest tile worth storing, in device pixels. A larger symbol is mostly one flat fill,
     *  which is what a rasterizer is fastest at, so the blit stops being the cheaper of the two.
     */
    private static final long MAX_TILE_PIXELS = 256L * 256L;

    /** How much the whole cache may hold before the least recently used tiles are dropped. */
    private static final long MAX_BYTES = 4L * 1024 * 1024;

    /**
     *  How finely the position a tile is drawn at is remembered, in steps per device pixel.
     *  <p>
     *  A tile can only be laid down on a whole device pixel, but at a screen scale of 150% a
     *  component pixel lands on half of one, and dropping that half moves every antialiased edge
     *  by a visible amount. So the leftover fraction is rasterized into the tile and keyed on,
     *  which makes a glyph at a half-pixel offset a tile of its own rather than a shifted one.
     *  Rounding that fraction to a thousandth of a pixel keeps the count down: at 150% a
     *  whole-numbered coordinate can only ever land on a whole pixel or on a half.
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

    /** Never stored: the filled side of the groove ends wherever the handle is, so the picture
     *  changes with every pixel the handle moves. */
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

    /** Never stored: an animating bar would leave one entry behind per frame and read none of
     *  them twice. */
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
        void draw( Graphics2D g, int x, int y );
    }

    /**
     *  Draws {@code drawing} through a stored tile, or straight onto {@code g} where a tile would
     *  be wrong or would not pay for itself.
     *
     * @param g the caller's surface
     * @param p the palette the caller resolved; a tile is only used when it is this cache's own
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

        // One image pixel per device pixel, so a tile rasterized at the screen's resolution is
        // not resampled down to the component's on the way back.
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
