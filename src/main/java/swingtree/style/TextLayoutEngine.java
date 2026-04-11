package swingtree.style;

import org.jspecify.annotations.Nullable;
import sprouts.Pair;
import sprouts.Tuple;
import swingtree.UI;
import swingtree.layout.Bounds;

import javax.swing.JComponent;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.*;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

final class TextLayoutEngine {
    private TextLayoutEngine() {}

    /**
     *  LRU cache capped at {@value #_CACHE_MAX_SIZE} entries.
     *  Uses an access-order {@link LinkedHashMap} so the least-recently-used entry is
     *  evicted once the cap is reached.  The map is wrapped in
     *  {@link Collections#synchronizedMap} so concurrent callers on different Swing
     *  repaint threads do not corrupt it.
     */
    private static final int _CACHE_MAX_SIZE = 128;

    /**
     *  Weak-reference cache of {@link ParagraphLayoutsData} entries, keyed by interned
     *  {@link Pooled}{@literal <}{@link Paragraph}{@literal >} instances.
     *  <p>
     *  Each entry bundles the paragraph's {@link AttributedString}, its reusable
     *  {@link LineBreakMeasurer}, the single no-wrap {@link TextLayout} (once computed),
     *  and the map of previously computed {@link LayoutLine} lists for different layout
     *  contexts — see {@link ParagraphLayoutsData} for details.
     *  <p>
     *  Using a {@link WeakHashMap} allows entries to be reclaimed once no other code holds
     *  a strong reference to the {@link Pooled} key — paragraphs that have scrolled off or
     *  been replaced are automatically evicted without a fixed size cap.
     *  <p>
     *  <b>Borrow semantics:</b> an entry is {@link Map#remove removed} from this cache
     *  before use and {@link Map#put returned} afterwards.  This ensures at most one
     *  thread mutates a given entry at any time; a concurrent caller that hits the same
     *  key simply constructs a fresh entry on cache-miss.
     */
    private static final Map<Pooled<Paragraph>, ParagraphLayoutsData> _PARAGRAPH_DATA_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     *  A large but geometrically safe line-width cap used when no explicit
     *  {@code boundsWidth} is available and as the "do not wrap" width passed
     *  to {@link LineBreakMeasurer#nextLayout} when {@code wrapLines} is false.
     *  <p>
     *  {@link Short#MAX_VALUE} (32 767 px) comfortably exceeds any real display width,
     *  yet stays well within the range of {@code float} arithmetic so that
     *  {@link java.awt.geom.Rectangle2D.Float} and {@link java.awt.geom.Area}
     *  operations on it do not overflow or lose precision.
     */
    private static final float _UNBOUNDED_LINE_WIDTH = Short.MAX_VALUE;
    @SuppressWarnings("serial")
    private static final Map<TextLayoutKey, Pair<Float, List<LayoutLine>>> _LAYOUT_CACHE =
            Collections.synchronizedMap(
                    new LinkedHashMap<TextLayoutKey, Pair<Float, List<LayoutLine>>>(
                            _CACHE_MAX_SIZE + 1, 0.75f, true /* access-order */
                    ) {
                        @Override
                        protected boolean removeEldestEntry(
                                Map.Entry<TextLayoutKey, Pair<Float, List<LayoutLine>>> eldest
                        ) {
                            return size() > _CACHE_MAX_SIZE;
                        }
                    }
            );


    static Shape childShapeForArea( Component child, UI.ComponentBoundary area ) {
        if ( area == UI.ComponentBoundary.OUTER_TO_EXTERIOR || !(child instanceof JComponent) )
            return child.getBounds();
        final JComponent asJComponent = (JComponent) child;
        final ComponentConf previousComponentConf = ComponentExtension.from(asJComponent).getConf();
        final Pair<BoxModelConf, ComponentConf> boxAndCompConf = StyleEngine._calculateBoxModelAndComponentConfs(
                Bounds.of(asJComponent.getX(), asJComponent.getY(), asJComponent.getWidth(), asJComponent.getHeight()),
                previousComponentConf.style(),
                StyleInstaller._formerBorderMarginCorrection(asJComponent),
                previousComponentConf
        );
        Shape shapeArea = null;
        ComponentAreas areas = ComponentAreas.of(new Pooled<>(boxAndCompConf.first()));
        switch ( area ) {
            case EXTERIOR_TO_BORDER: shapeArea = areas.get(UI.ComponentArea.BODY); break;
            case BORDER_TO_INTERIOR: shapeArea = areas.get(UI.ComponentArea.INTERIOR); break;
            case INTERIOR_TO_CONTENT: shapeArea = areas.getContentArea(); break;
            case CENTER_TO_CONTENT:
                // Basically a point. We give it a small area to avoid issues with zero-area shapes:
                shapeArea = new Rectangle(0, 0, 1, 1);
                break;
        }
        if ( shapeArea == null )
            return asJComponent.getBounds(); // fallback to the whole component bounds if we failed to calculate the area for some reason
        // Now we need to translate to the parent coordinate space:
        int xOffset = asJComponent.getX(); // -> these are offsets in the parent component coordinate system!
        int yOffset = asJComponent.getY();
        return new TextLayoutEngine.TranslatedShape(shapeArea, xOffset, yOffset);
    }


    /**
     *  Builds the list of {@link TextLayout} objects for the given styled text and measures
     *  the total rendered height of all lines (Phase 1 + Phase 2 of the text rendering pipeline).
     *  When {@code obstacles} is non-empty, each line layout will be built with awareness
     *  of the obstacle shapes and may contain multiple TextLayout segments to skip over obstacles.
     *  <b>But hitting an obstacle does not just lead to a simple line break, within a line,
     *  the text will try to skip over the obstacle shape until it can continue.</b>
     *
     * @param font         The base font to use for unstyled segments.
     * @param frc          The {@link FontRenderContext} used by the measurer.
     * @param paragraphs   The styled text to lay out.
     * @param boundsWidth  The available width for the text. This property is only relevant when line wrapping is active.
     * @param boundsX,     The x-offset of the text in the component space. It is important for intersecting obstacles!
     * @param boundsY,     The y-offset of the text in the component space. It is important for intersecting obstacles!
     * @param wrapLines    Whether long lines should be wrapped at word boundaries.
     * @param boxModelConf The box-model configuration forwarded to per-segment font derivation.
     * @param obstacles    Shapes (in component coordinates) the text must not be rendered on top of.
     *                     The text will skip over these shapes and continue rendering on the other side,
     *                     if possible or break the line if no more horizontal space is left.
     *                     Pass an empty {@link Tuple} when no obstacles are present. Ignored when the {@code placement}
     *                     does not support obstacles (see below).
     * @param placement    The text placement mode, which determines whether obstacle avoidance is active.
     *                     Only TOP-based placements support obstacle avoidance, so when CENTER or BOTTOM placement is used,
     *                     obstacles are ignored even if provided.
     * @return A {@link Pair} whose {@link Pair#first()} is the total pixel height of all lines
     *         and whose {@link Pair#second()} is the ordered list of {@link LayoutLine} entries,
     *         each carrying its layout and the obstacle-free horizontal region it was placed into.
     */
    static Pair<Float, List<LayoutLine>> _buildTextLayoutsAndPreferredHeight(
        final Font                       font,
        final FontRenderContext          frc,
        final Tuple<Pooled<Paragraph>>   paragraphs,
        final float                      boundsWidth,
        final float                      boundsX,
        final float                      boundsY,
        final boolean                    wrapLines,
        final BoxModelConf               boxModelConf,
        final Tuple<Shape>               obstacles,
        final UI.Placement               placement
    ) {
        final Tuple<Shape> compatibleObstacles = _supportsObstacles(placement) ? obstacles : obstacles.clear();
        final TextLayoutKey key = new TextLayoutKey(font, paragraphs, boundsWidth, boundsX, boundsY, wrapLines, boxModelConf, compatibleObstacles);
        final Pair<Float, List<LayoutLine>> cached = _LAYOUT_CACHE.get(key);
        if ( cached != null )
            return cached;

        /*
            ------------------------------------------------
            Phase 1 : Build LayoutLines from paragraphs
            ------------------------------------------------
        */
        final List<LayoutLine> lines = _buildLayoutLines(paragraphs, frc, font, boundsX, boundsY, boundsWidth, wrapLines, compatibleObstacles, boxModelConf);

        /*
            ------------------------------------------------
            Phase 2 : Measure total text height
            ------------------------------------------------
         */
        float totalHeight = 0;
        for ( LayoutLine line : lines ) {
            if ( line.layout == null )
                totalHeight += font.getSize2D();
            else
                totalHeight += line.layout.getAscent() + line.layout.getDescent() + line.layout.getLeading();
        }

        final Pair<Float, List<LayoutLine>> result = Pair.of(totalHeight, Collections.unmodifiableList(lines));
        _LAYOUT_CACHE.put(key, result);
        return result;
    }

    private static boolean _supportsObstacles(UI.Placement placement) {
        // Obstacle avoidance relies on TOP-based vertical placement to track the current line's y-level in component coordinates.
        // When using CENTER or BOTTOM placement, the text block's y-level shifts as lines are added, so obstacle intersection
        // becomes inaccurate and may produce jumbled layouts. For simplicity, we disable obstacles entirely in these modes.
        return placement == UI.Placement.TOP_LEFT || placement == UI.Placement.TOP || placement == UI.Placement.TOP_RIGHT;
    }

    /**
     *  Phase 1 of the text rendering pipeline: converts a list of pre-split paragraphs into
     *  an ordered list of {@link LayoutLine}s, respecting word-wrap and obstacle constraints.
     *  <p>
     *  Each non-blank paragraph produces one or more {@link LayoutLine}s depending on available
     *  width and obstacles.  A paragraph whose {@link Paragraph#isBlankLine} flag is {@code true}
     *  is emitted as a {@link LayoutLine} with a {@code null} layout.  Any trailing blank-line
     *  entry is stripped before returning.
     *  <p>
     *  Results are cached at two levels for maximum reuse:
     *  <ol>
     *    <li>The {@link AttributedString} and {@link LineBreakMeasurer} for each paragraph are
     *        kept in {@link #_PARAGRAPH_DATA_CACHE}, avoiding expensive font-metric setup on
     *        every repaint.</li>
     *    <li>The complete {@link LayoutLine} list for each paragraph is cached inside
     *        {@link ParagraphLayoutsData#wrappedLayouts} (wrap/obstacle path) or
     *        {@link ParagraphLayoutsData#singleLayout} (no-wrap, no-obstacle path), so
     *        that a repaint with identical obstacle geometry and bounds skips the
     *        {@link LineBreakMeasurer#nextLayout} calls entirely.</li>
     *  </ol>
     *
     * @param paragraphs   Interned paragraphs produced by {@link TextConf#simplified()};
     *                     entries whose {@link Paragraph#isBlankLine} flag is {@code true} represent blank lines.
     * @param frc          The {@link FontRenderContext} used by the measurer.
     * @param font         Base font — used for the estimated line height and blank-line placeholders.
     * @param boundsX      Left edge of the text bounds in component coordinates.
     * @param boundsY      Top edge of the text bounds in component coordinates (starting y for obstacle queries).
     * @param boundsWidth  Available width ({@code < 0} means unbounded).
     * @param wrapLines    Whether to wrap text at word boundaries when it exceeds the available width.
     * @param obstacles    Shapes (in component coordinates) the text must not overlap.
     * @return Ordered list of {@link LayoutLine}s ready for Phase 2 height measurement and rendering.
     */
    private static List<LayoutLine> _buildLayoutLines(
        final Tuple<Pooled<Paragraph>>         paragraphs,
        final FontRenderContext                frc,
        final Font                             font,
        final float                            boundsX,
        final float                            boundsY,
        final float                            boundsWidth,
        final boolean                          wrapLines,
        final Tuple<Shape>                     obstacles,
        final BoxModelConf                     boxModelConf
    ) {
        // currentY tracks the top of the next line in component coordinates (TOP-placement
        // assumption). Used solely for obstacle intersection — a good approximation for all
        // placement modes because obstacles are typically large enough that the small vertical
        // offset introduced by CENTER/BOTTOM placement does not change which obstacle a line hits.
        final List<LayoutLine> lines = new ArrayList<>();
        float currentY = boundsY;
        final float estLineHeight = font.getSize2D();

        for ( final Pooled<Paragraph> pooled : paragraphs ) {
            final Paragraph paragraph = pooled.get();
            if ( paragraph.isBlankLine ) {
                lines.add(new LayoutLine(null, boundsX, boundsWidth)); // blank line
                currentY += estLineHeight;
                continue;
            }

            // Borrow (or create) the ParagraphLayoutsData for this paragraph.
            // The remove → use → put borrow pattern ensures thread safety: a concurrent
            // caller that hits the same key simply constructs a fresh entry on miss.
            ParagraphLayoutsData data = _PARAGRAPH_DATA_CACHE.remove(pooled);
            if ( data == null || !data.isCompatibleWith(font) ) {
                final AttributedString attrStr = _paragraphToAttributedString(paragraph.styledStrings, font, boxModelConf);
                data = new ParagraphLayoutsData(font, attrStr);
            }

            if ( (wrapLines && boundsWidth >= 0) || !obstacles.isEmpty() ) {
                // LineBreakMeasurer path: word-wrapping and/or splitting around obstacles.
                // When boundsWidth is undefined, fall back to a large practical limit so
                // that the measurer can still advance and obstacles can still be queried.
                final float effectiveWidth = boundsWidth >= 0 ? boundsWidth : _UNBOUNDED_LINE_WIDTH;

                // Check whether we already have a cached result for this paragraph at this
                // layout context.  The key captures obstacles + paragraphY, which together
                // determine the free intervals at every line within the paragraph — not just
                // the first.  Using only the first line's intervals would give stale cache
                // hits when an obstacle moves into a later line but misses the first.
                final LineLayoutKey  layoutKey      = new LineLayoutKey(obstacles, Math.round(currentY), wrapLines, Math.round(effectiveWidth));
                final List<LayoutLine> cachedLines  = data.wrappedLayouts.get(layoutKey);

                if ( cachedLines != null ) {
                    lines.addAll(cachedLines);
                    for ( LayoutLine ll : cachedLines )
                        currentY += ll.layout == null ? estLineHeight
                                                      : ll.layout.getAscent() + ll.layout.getDescent() + ll.layout.getLeading();
                } else {
                    // Build from scratch and populate the per-paragraph cache.
                    final AttributedCharacterIterator it = data.attrStr.getIterator();
                    if ( data.measurer != null )
                        data.measurer.setPosition(it.getBeginIndex()); // reset to start of paragraph
                    else
                        data.measurer = new LineBreakMeasurer(it, BreakIterator.getLineInstance(), frc);

                    final List<LayoutLine> paraLines = new ArrayList<>();
                    final int end = it.getEndIndex();
                    while ( data.measurer.getPosition() < end ) {
                        final List<Band> intervals = _freeIntervalsAt(currentY, estLineHeight, boundsX, effectiveWidth, obstacles);

                        TextLayout firstLayout = null;
                        float firstX = boundsX;
                        float firstW = effectiveWidth;
                        List<LayoutLine.Segment> extras = Collections.emptyList();

                        if ( intervals.isEmpty() ) {
                            // All space is blocked — advance the measurer so the loop terminates.
                            // When not wrapping, consume all remaining text in one shot.
                            firstLayout = data.measurer.nextLayout(wrapLines ? effectiveWidth : _UNBOUNDED_LINE_WIDTH);
                        } else {
                            final int lastIdx = intervals.size() - 1;
                            for ( int i = 0; i <= lastIdx; i++ ) {
                                if ( data.measurer.getPosition() >= end ) break;
                                final Band  band = intervals.get(i);
                                final float x = band.start, w = band.size;
                                // When not wrapping, give the last band an unlimited width so all
                                // remaining text is consumed here instead of wrapping to a new line.
                                final float      nextWidth = (wrapLines || i < lastIdx) ? w : _UNBOUNDED_LINE_WIDTH;
                                final TextLayout layout    = data.measurer.nextLayout(nextWidth);
                                if ( firstLayout == null ) {
                                    firstLayout = layout;
                                    firstX      = x;
                                    firstW      = w;
                                } else {
                                    if ( extras.isEmpty() ) extras = new ArrayList<>();
                                    extras.add(new LayoutLine.Segment(layout, x, w));
                                }
                            }
                        }

                        if ( firstLayout != null ) {
                            paraLines.add(new LayoutLine(firstLayout, firstX, firstW, extras));
                            currentY += firstLayout.getAscent() + firstLayout.getDescent() + firstLayout.getLeading();
                        } else {
                            currentY += estLineHeight; // all intervals had zero width — skip band
                        }
                    }

                    final List<LayoutLine> immutableParaLines = Collections.unmodifiableList(paraLines);
                    data.wrappedLayouts.put(layoutKey, immutableParaLines);
                    lines.addAll(immutableParaLines);
                }
            } else {
                // No obstacles and no wrapping — a single TextLayout suffices.
                // Cache it so subsequent repaints (e.g. after a bounds change that does not
                // affect wrapping) skip the TextLayout construction entirely.
                if ( data.singleLayout == null ) {
                    final AttributedCharacterIterator it = data.attrStr.getIterator();
                    data.singleLayout = new TextLayout(it, frc);
                }
                lines.add(new LayoutLine(data.singleLayout, boundsX, boundsWidth));
                currentY += data.singleLayout.getAscent() + data.singleLayout.getDescent() + data.singleLayout.getLeading();
            }

            // Return the entry to the cache so the next layout pass can reuse it.
            _PARAGRAPH_DATA_CACHE.put(pooled, data);
        }

        // Strip a trailing blank-line marker that may have been emitted for a paragraph
        // separator appearing at the very end of the text.
        if ( !lines.isEmpty() && lines.get(lines.size() - 1).layout == null )
            lines.remove(lines.size() - 1);

        return lines;
    }

    /**
     *  Returns all contiguous obstacle-free horizontal intervals als {@link Band}s within
     *  {@code [boundsX, boundsX+boundsWidth]} for a line whose vertical band spans
     *  {@code [y, y+lineHeight]} in component coordinates, sorted left to right.
     *  <p>
     *  Each obstacle's contribution is derived from its <em>exact geometry</em> via
     *  {@link Area} intersection (not just the bounding box), so non-rectangular shapes
     *  such as circles or ellipses are handled correctly — only the actual chord at the
     *  current y-level is subtracted, not the full bounding-box width.
     *  <p>
     *  Returning all intervals (rather than just the widest one) allows the caller to
     *  fill text into every free gap on a line, so text appears on both sides of an obstacle.
     *  If all horizontal space is consumed, an empty list is returned.
     *
     * @param y            Top of the line in component coordinates.
     * @param lineHeight   Estimated height of one line (typically {@code font.getSize2D()}).
     * @param boundsX      Left edge of the text bounds in component coordinates.
     * @param boundsWidth  Full width of the text bounds.
     * @param obstacles    Shapes to avoid, in component coordinates.
     * @return List of {@code new Band(xStart, width)} entries for every obstacle-free interval,
     *         ordered left to right.  Each width is guaranteed to be {@code > 0}.
     */
    private static List<Band> _freeIntervalsAt(
        final float        y,
        final float        lineHeight,
        final float        boundsX,
        final float        boundsWidth,
        final Tuple<Shape> obstacles
    ) {
        if ( obstacles.isEmpty() )
            return Collections.singletonList(new Band( boundsX, boundsWidth ));

        // Internal representation: {start, end} pairs (converted to {start, width} on exit)
        List<Range> free = new ArrayList<>();
        free.add(new Range( boundsX, boundsX + boundsWidth ));

        final Area lineStrip = new Area(new Rectangle2D.Float(boundsX, y, boundsWidth, lineHeight));

        for ( Shape obstacle : obstacles ) {
            if ( obstacle instanceof TranslatedShape ) {
                // Unwrap to get the actual translated geometry:
                obstacle = ((TranslatedShape) obstacle).getTranslatedShape();
            }
            // Fast pre-check with the bounding box before the more expensive Area intersection
            final Rectangle2D ob = obstacle.getBounds2D();
            if ( ob.getMaxY() <= y || ob.getMinY() >= y + lineHeight )
                continue;

            // Intersect the exact obstacle geometry with the line strip
            final Area intersection = new Area(obstacle);
            intersection.intersect(lineStrip);
            if ( intersection.isEmpty() )
                continue;

            free = narrowDownRangesToInclude(intersection, free);
        }

        if ( free.isEmpty() )
            return Collections.emptyList();

        // Convert {start, end} → {start, width}, filtering out zero-width gaps
        final List<Band> result = new ArrayList<>(free.size());
        for ( Range r : free ) {
            final float w = r.end - r.start;
            if ( w > 0f ) result.add(new Band( r.start, w ));
        }
        return result;
    }

    private static List<Range> narrowDownRangesToInclude( Area intersection, List<Range> free ) {
        final Rectangle2D ib     = intersection.getBounds2D();
        final float       oLeft  = (float) ib.getMinX();
        final float       oRight = (float) ib.getMaxX();

        // Subtract [oLeft, oRight] from every free interval (1-D interval difference)
        final List<Range> remaining = new ArrayList<>(free.size() + 1);
        for ( Range r : free ) {
            final float a = r.start, b = r.end;
            if ( oRight <= a || oLeft >= b ) {
                remaining.add(r); // no overlap
            } else {
                if ( oLeft  > a ) remaining.add(new Range( a,                   Math.min(b, oLeft)  )); // left fragment
                if ( oRight < b ) remaining.add(new Range( Math.max(a, oRight), b                   )); // right fragment
            }
        }
        return remaining;
    }

    private static AttributedString _paragraphToAttributedString(
        final Tuple<StyledString> paragraph,
        final Font font,
        final BoxModelConf boxModelConf // Needed for some font configs which have paints that depend on the box model (gradient offsets, etc.)
    ) {
        final StringBuilder sb = new StringBuilder();
        for ( StyledString s : paragraph )
            sb.append(s.string());
        final AttributedString attrStr = new AttributedString(sb.toString());
        int index = 0;
        for ( StyledString styledString : paragraph ) {
            int styledStringLength = styledString.string().length();
            if ( styledStringLength <= 0 )
                continue; // Skip zero-length segments to avoid AttributedString IllegalArgumentException for empty ranges
            int endIndex = index + styledStringLength;
            if ( styledString.fontConf().isPresent() ) {
                java.awt.Font localFont = styledString.fontConf().get().createDerivedFrom(font, boxModelConf).orElse(font);
                attrStr.addAttribute(TextAttribute.FONT, localFont, index, endIndex);
                attrStr.addAttributes(localFont.getAttributes(), index, endIndex);
            } else {
                attrStr.addAttribute(TextAttribute.FONT, font, index, endIndex);
                attrStr.addAttributes(font.getAttributes(), index, endIndex);
            }
            index += styledStringLength;
        }
        return attrStr;
    }

    /**
     *  Holds a single laid-out text line together with the obstacle-free horizontal region
     *  it was fitted into.  Both {@code regionX} and {@code regionWidth} are in component
     *  coordinates and are used by the renderer to position the line, including alignment
     *  (LEFT / CENTER / RIGHT) within the available region.
     *  <p>
     *  When obstacles split a visual line into more than one free interval, every interval
     *  beyond the first is captured as an extra {@link Segment} in {@code extraSegments}.
     *  All segments share the same baseline (y is not advanced between them).
     *  <p>
     *  A {@code null} {@code layout} represents a blank / empty line — the region fields
     *  still carry the full text-bounds width so that the blank contributes the correct height.
     */
    static final class LayoutLine {

        /** A secondary text fragment placed in one of the additional free intervals on this line. */
        static final class Segment {
            final TextLayout layout;
            final float regionX;
            final float regionWidth;
            Segment(TextLayout layout, float regionX, float regionWidth) {
                this.layout      = layout;
                this.regionX     = regionX;
                this.regionWidth = regionWidth;
            }
        }

        final @Nullable TextLayout  layout;
        final float                 regionX;
        final float                 regionWidth;
        /** Additional same-baseline fragments produced when obstacles split the line. */
        final List<Segment>         extraSegments;

        LayoutLine(@Nullable TextLayout layout, float regionX, float regionWidth) {
            this(layout, regionX, regionWidth, Collections.emptyList());
        }

        LayoutLine(@Nullable TextLayout layout, float regionX, float regionWidth, List<Segment> extraSegments) {
            this.layout        = layout;
            this.regionX       = regionX;
            this.regionWidth   = regionWidth;
            this.extraSegments = extraSegments;
        }
    }

    private static final class Range {
        final float start, end;
        Range(float start, float end) {
            this.start = start;
            this.end   = end;
        }
    }

    private static final class Band {
        final float start, size;
        Band(float start, float size) {
            this.start = start;
            this.size  = size;
        }

        @Override
        public boolean equals( Object o ) {
            if ( this == o ) return true;
            if ( !(o instanceof Band) ) return false;
            final Band other = (Band) o;
            return Float.floatToIntBits(start) == Float.floatToIntBits(other.start) &&
                    Float.floatToIntBits(size)  == Float.floatToIntBits(other.size);
        }

        @Override
        public int hashCode() {
            return 31 * Float.floatToIntBits(start) + Float.floatToIntBits(size);
        }
    }

    /**
     *  Immutable cache key for {@link #_buildTextLayoutsAndPreferredHeight}.
     *  Contains every parameter of that method <em>except</em> the {@link FontRenderContext},
     *  which is assumed to be consistent across calls in the same rendering environment.
     *  <p>
     *  The {@code hashCode} is pre-computed on construction so repeated cache lookups are cheap.
     *  Floats are compared via {@link Float#floatToIntBits} to avoid {@code NaN} edge cases.
     *  {@link Shape} instances inside {@code obstacles} rely on their own {@code equals}/{@code hashCode}
     *  — most AWT shapes use identity, which is fine since {@link TextConf} is immutable and
     *  reuses the same {@link Tuple} reference across repeated style evaluations.
     */
    private static final class TextLayoutKey {
        private final Font                       font;
        private final Tuple<Pooled<Paragraph>>   paragraphs;
        private final float                      boundsWidth;
        private final float                      boundsX;
        private final float                      boundsY;
        private final boolean                    wrapLines;
        private final BoxModelConf               boxModelConf;
        private final Tuple<Shape>               obstacles;
        private final int                        _hash;

        TextLayoutKey(
                Font                       font,
                Tuple<Pooled<Paragraph>>   paragraphs,
                float                      boundsWidth,
                float                      boundsX,
                float                      boundsY,
                boolean                    wrapLines,
                BoxModelConf               boxModelConf,
                Tuple<Shape>               obstacles
        ) {
            this.font         = font;
            this.paragraphs   = paragraphs;
            this.boundsWidth  = boundsWidth;
            this.boundsX      = boundsX;
            this.boundsY      = boundsY;
            this.wrapLines    = wrapLines;
            this.boxModelConf = boxModelConf;
            this.obstacles    = obstacles;
            this._hash        = Objects.hash(
                    font, paragraphs,
                    Float.floatToIntBits(boundsWidth),
                    Float.floatToIntBits(boundsX),
                    Float.floatToIntBits(boundsY),
                    wrapLines, boxModelConf, obstacles
            );
        }

        @Override
        public boolean equals( Object o ) {
            if ( this == o ) return true;
            if ( !(o instanceof TextLayoutKey) ) return false;
            final TextLayoutKey other = (TextLayoutKey) o;
            return Float.floatToIntBits(boundsWidth) == Float.floatToIntBits(other.boundsWidth) &&
                    Float.floatToIntBits(boundsX)     == Float.floatToIntBits(other.boundsX)     &&
                    Float.floatToIntBits(boundsY)     == Float.floatToIntBits(other.boundsY)     &&
                    wrapLines    == other.wrapLines                                               &&
                    font        .equals(other.font)                                               &&
                    paragraphs  .equals(other.paragraphs)                                        &&
                    boxModelConf.equals(other.boxModelConf)                                       &&
                    obstacles   .equals(other.obstacles);
        }

        @Override public int hashCode() { return _hash; }
    }

    /**
     *  Secondary cache key used inside {@link ParagraphLayoutsData#wrappedLayouts}.
     *  <p>
     *  Together with the paragraph content and font configuration stored in the enclosing
     *  {@link ParagraphLayoutsData} entry, these four fields fully capture the layout
     *  context needed to reproduce the list of {@link LayoutLine}s produced for the
     *  wrap/obstacle path of a single paragraph:
     *  <ul>
     *    <li>{@code obstacles} — the shapes the text must flow around; equality uses each
     *        shape's own {@code equals} (most AWT shapes compare by value), so any
     *        positional change produces a new key and a cache miss.</li>
     *    <li>{@code paragraphY} — the y-coordinate at which this paragraph starts in
     *        component space; together with {@code obstacles} it determines the free
     *        intervals at every line within the paragraph.</li>
     *    <li>{@code wrapLines} — whether word-wrapping is active.</li>
     *    <li>{@code effectiveWidth} — the usable horizontal extent passed to
     *        {@link LineBreakMeasurer#nextLayout}.</li>
     *  </ul>
     *  Using {@code obstacles + paragraphY} rather than a snapshot of the first line's
     *  free intervals is essential for correctness: an obstacle that misses the first line
     *  but covers later lines would otherwise produce a stale cache hit after it moves.
     *  <p>
     *  {@code hashCode} is pre-computed; floats use {@link Float#floatToIntBits} for
     *  well-defined {@code NaN} handling.
     */
    private static final class LineLayoutKey {
        final Tuple<Shape> obstacles;
        final float        paragraphY;
        final boolean      wrapLines;
        final float        effectiveWidth;
        final int          _hash;

        LineLayoutKey( Tuple<Shape> obstacles, float paragraphY, boolean wrapLines, float effectiveWidth ) {
            this.obstacles      = obstacles;
            this.paragraphY     = paragraphY;
            this.wrapLines      = wrapLines;
            this.effectiveWidth = effectiveWidth;
            this._hash          = Objects.hash(obstacles, Float.floatToIntBits(paragraphY), wrapLines, Float.floatToIntBits(effectiveWidth));
        }

        @Override
        public boolean equals( Object o ) {
            if ( this == o ) return true;
            if ( !(o instanceof LineLayoutKey) ) return false;
            final LineLayoutKey other = (LineLayoutKey) o;
            return wrapLines == other.wrapLines &&
                    Float.floatToIntBits(paragraphY)     == Float.floatToIntBits(other.paragraphY)     &&
                    Float.floatToIntBits(effectiveWidth) == Float.floatToIntBits(other.effectiveWidth) &&
                    obstacles.equals(other.obstacles);
        }

        @Override public int hashCode() { return _hash; }
    }

    /**
     *  Per-paragraph cache entry stored in {@link #_PARAGRAPH_DATA_CACHE}.
     *  <p>
     *  Holds everything that can be reused across multiple layout passes for the same
     *  paragraph text and font configuration:
     *  <ul>
     *    <li>{@link #attrStr} — the {@link AttributedString} built once from the styled
     *        strings and their font attributes; avoids repeating the O(n) attribute-setup
     *        work on every repaint.</li>
     *    <li>{@link #measurer} — reusable {@link LineBreakMeasurer} ({@code null} while
     *        borrowed by a concurrent caller or not yet created).</li>
     *    <li>{@link #singleLayout} — cached {@link TextLayout} for the no-wrap,
     *        no-obstacle path.  {@code null} until first computed.</li>
     *    <li>{@link #wrappedLayouts} — cached {@link LayoutLine} lists for the
     *        wrap/obstacle path, keyed by {@link LineLayoutKey}.</li>
     *  </ul>
     *  {@link #font} are stored so that a stale entry built
     *  for a different styling context is detected and discarded on retrieval.
     *  <b>A {@link BoxModelConf} is NOT needed, because although it is used by a font for gradient
     *  and noise paint configuration, this will never affect the layout!</b>
     */
    private static final class ParagraphLayoutsData {
        final Font             font;
        final AttributedString attrStr;
        /** {@code null} while borrowed by a caller or not yet created. */
        @Nullable LineBreakMeasurer measurer;
        /** Non-null once the no-wrap, no-obstacle path has run at least once. */
        @Nullable TextLayout        singleLayout;
        /** Cached full-paragraph {@link LayoutLine} lists for the wrap/obstacle path. */
        final Map<LineLayoutKey, List<LayoutLine>> wrappedLayouts = new LinkedHashMap<LineLayoutKey, List<LayoutLine>>(
                32 + 1, 0.75f, true /* access-order */
        ) {
            @Override protected boolean removeEldestEntry(Map.Entry<LineLayoutKey, List<LayoutLine>> eldest) {
                return size() > 32;
            }
        };

        ParagraphLayoutsData( Font font, AttributedString attrStr ) {
            this.font         = font;
            this.attrStr      = attrStr;
        }

        boolean isCompatibleWith( Font font ) {
            return this.font.equals(font);
        }
    }

    /**
     *  This wrapper class exists to make caching more effective by using the source shape
     *  and the offsets as the basis for equality and hash code, rather than the translated shape's geometry.
     *  <b>
     *      This is because {@code AffineTransform.getTranslateInstance(xOffset, yOffset)} produces shapes
     *      with non-deterministic value semantics for equals() and hashCode().
     *  </b>
     */
    private static final class TranslatedShape implements Shape {
        private final int _xOffset;
        private final int _yOffset;
        private final Shape _sourceShape;
        private final Shape _translatedShape;
        private final AtomicReference<@Nullable Integer> _hashCache = new AtomicReference<>(null);

        public TranslatedShape(Shape shapeArea, int xOffset, int yOffset) {
            java.awt.geom.AffineTransform translate = java.awt.geom.AffineTransform.getTranslateInstance(xOffset, yOffset);
            _xOffset = xOffset;
            _yOffset = yOffset;
            _sourceShape = shapeArea;
            _translatedShape = translate.createTransformedShape(shapeArea);
        }

        public Shape getTranslatedShape() { return _translatedShape; }

        @Override
        public Rectangle getBounds() { return _translatedShape.getBounds(); }

        @Override
        public Rectangle2D getBounds2D() { return _translatedShape.getBounds2D(); }

        @Override
        public boolean contains(double x, double y) { return _translatedShape.contains(x, y); }

        @Override
        public boolean contains(Point2D p) { return _translatedShape.contains(p); }

        @Override
        public boolean intersects(double x, double y, double w, double h) {
            return _translatedShape.intersects(x, y, w, h);
        }

        @Override
        public boolean intersects(Rectangle2D r) {
            return _translatedShape.intersects(r);
        }

        @Override
        public boolean contains(double x, double y, double w, double h) {
            return _translatedShape.contains(x, y, w, h);
        }

        @Override
        public boolean contains(Rectangle2D r) {
            return _translatedShape.contains(r);
        }

        @Override
        public PathIterator getPathIterator(AffineTransform at) {
            return _translatedShape.getPathIterator(at);
        }

        @Override
        public PathIterator getPathIterator(AffineTransform at, double flatness) {
            return _translatedShape.getPathIterator(at, flatness);
        }

        @Override
        public int hashCode() {
            Integer cachedHash = _hashCache.get();
            if ( cachedHash != null ) {
                return cachedHash;
            } else {
                int computedHash = Objects.hash(_xOffset, _yOffset, _sourceShape);
                _hashCache.compareAndSet(null, computedHash);
                return computedHash;
            }
        }
        @Override
        public boolean equals(@Nullable Object obj) {
            if ( this == obj ) return true;
            if ( obj == null || getClass() != obj.getClass() ) return false;
            final TranslatedShape other = (TranslatedShape) obj;
            return _xOffset == other._xOffset &&
                   _yOffset == other._yOffset &&
                   Objects.equals(_sourceShape, other._sourceShape);
        }
        @Override
        public String toString() {
            return "TranslatedShape[source=" + _sourceShape + ", xOffset=" + _xOffset + ", yOffset=" + _yOffset + "]";
        }
    }

}