package swingtree.style;

import org.jspecify.annotations.Nullable;
import sprouts.Pair;
import sprouts.Tuple;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.*;

final class TextLayoutEngine {
    private TextLayoutEngine() {}

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
        private final Font                font;
        private final Tuple<StyledString> text;
        private final float               boundsWidth;
        private final float               boundsX;
        private final float               boundsY;
        private final boolean             wrapLines;
        private final BoxModelConf        boxModelConf;
        private final Tuple<Shape>        obstacles;
        private final int                 _hash;

        TextLayoutKey(
            Font                font,
            Tuple<StyledString> text,
            float               boundsWidth,
            float               boundsX,
            float               boundsY,
            boolean             wrapLines,
            BoxModelConf        boxModelConf,
            Tuple<Shape>        obstacles
        ) {
            this.font         = font;
            this.text         = text;
            this.boundsWidth  = boundsWidth;
            this.boundsX      = boundsX;
            this.boundsY      = boundsY;
            this.wrapLines    = wrapLines;
            this.boxModelConf = boxModelConf;
            this.obstacles    = obstacles;
            this._hash        = Objects.hash(
                                    font, text,
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
                   text        .equals(other.text)                                               &&
                   boxModelConf.equals(other.boxModelConf)                                       &&
                   obstacles   .equals(other.obstacles);
        }

        @Override public int hashCode() { return _hash; }
    }

    /**
     *  LRU cache capped at {@value #_CACHE_MAX_SIZE} entries.
     *  Uses an access-order {@link LinkedHashMap} so the least-recently-used entry is
     *  evicted once the cap is reached.  The map is wrapped in
     *  {@link Collections#synchronizedMap} so concurrent callers on different Swing
     *  repaint threads do not corrupt it.
     */
    private static final int _CACHE_MAX_SIZE = 128;
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
     * @param text         The styled text to lay out.
     * @param boundsWidth  The available width for the text. This property is only relevant when line wrapping is active.
     * @param boundsX,     The x-offset of the text in the component space. It is important for intersecting obstacles!
     * @param boundsY,     The y-offset of the text in the component space. It is important for intersecting obstacles!
     * @param wrapLines    Whether long lines should be wrapped at word boundaries.
     * @param boxModelConf The box-model configuration forwarded to per-segment font derivation.
     * @param obstacles    Shapes (in component coordinates) the text must not be rendered on top of.
     *                     The text will skip over these shapes and continue rendering on the other side,
     *                     if possible or break the line if no more horizontal space is left.
     *                     Pass an empty {@link Tuple} when no obstacles are present.
     * @return A {@link Pair} whose {@link Pair#first()} is the total pixel height of all lines
     *         and whose {@link Pair#second()} is the ordered list of {@link LayoutLine} entries,
     *         each carrying its layout and the obstacle-free horizontal region it was placed into.
     */
    static Pair<Float, List<LayoutLine>> _buildTextLayoutsAndPreferredHeight(
        final Font                font,
        final FontRenderContext   frc,
        final Tuple<StyledString> text,
        final float               boundsWidth,
        final float               boundsX,
        final float               boundsY,
        final boolean             wrapLines,
        final BoxModelConf        boxModelConf,
        final Tuple<Shape>        obstacles
    ) {
        final TextLayoutKey key = new TextLayoutKey(font, text, boundsWidth, boundsX, boundsY, wrapLines, boxModelConf, obstacles);
        final Pair<Float, List<LayoutLine>> cached = _LAYOUT_CACHE.get(key);
        if ( cached != null )
            return cached;

        final List<LayoutLine> lines = new ArrayList<>();
        /*
            ------------------------------------------------
            Phase 1 : Build layouts using LineBreakMeasurer
            ------------------------------------------------
        */
        // currentY tracks the top of the next line in component coordinates (TOP-placement
        // assumption). Used solely for obstacle intersection — a good approximation for all
        // placement modes because obstacles are typically large enough that the small vertical
        // offset introduced by CENTER/BOTTOM placement does not change which obstacle a line hits.
        float currentY = boundsY;
        final float estLineHeight = font.getSize2D();

        final List<@Nullable AttributedString> paragraphs = _splitStyledTextIntoParagraphs(text, font, boxModelConf);
        for ( @Nullable AttributedString attrStr : paragraphs ) {
            if ( attrStr == null ) {
                lines.add(new LayoutLine(null, boundsX, boundsWidth)); // blank line
                currentY += estLineHeight;
                continue;
            }
            final AttributedCharacterIterator it = attrStr.getIterator();

            /*
                TODO - Fix bug:
                When there is a non empty obstacle tuple and the 'wrapLines' flag is set to false,
                the text layout engine does not handle obstacles AT ALL! So text will be drawn right through
                obstacles if they lie in the path of the text, which is obviously not the intended behavior.
            */
            if ( wrapLines && boundsWidth >= 0 ) {// Word wrapping using LineBreakMeasurer
                final LineBreakMeasurer measurer = new LineBreakMeasurer(it, BreakIterator.getLineInstance(), frc);
                final int end = it.getEndIndex();
                while ( measurer.getPosition() < end ) {
                    final List<Band> intervals = _freeIntervalsAt(currentY, estLineHeight, boundsX, boundsWidth, obstacles);

                    TextLayout           firstLayout = null;
                    float                firstX      = boundsX;
                    float                firstW      = boundsWidth;
                    List<LayoutLine.Segment> extras  = Collections.emptyList();

                    if ( intervals.isEmpty() ) {
                        // All space is blocked — fall back to full width so the measurer advances
                        firstLayout = measurer.nextLayout(boundsWidth);
                    } else {
                        for ( Band band : intervals ) {
                            if ( measurer.getPosition() >= end ) break;
                            final float x = band.start, w = band.size;
                            final TextLayout layout = measurer.nextLayout(w);
                            if ( firstLayout == null ) {
                                firstLayout = layout; firstX = x; firstW = w;
                            } else {
                                if ( extras.isEmpty() ) extras = new ArrayList<>();
                                extras.add(new LayoutLine.Segment(layout, x, w));
                            }
                        }
                    }

                    if ( firstLayout != null ) {
                        lines.add(new LayoutLine(firstLayout, firstX, firstW, extras));
                        currentY += firstLayout.getAscent() + firstLayout.getDescent() + firstLayout.getLeading();
                    } else {
                        currentY += estLineHeight; // all intervals had zero width — skip band
                    }
                }
            } else {// No wrapping — render full line even if wider than bounds
                final TextLayout layout = new TextLayout(it, frc);
                lines.add(new LayoutLine(layout, boundsX, boundsWidth));
                currentY += layout.getAscent() + layout.getDescent() + layout.getLeading();
            }
        }

        /*
            remove trailing blank-line marker
         */
        if ( !lines.isEmpty() && lines.get(lines.size() - 1).layout == null )
            lines.remove(lines.size() - 1);

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
    }

    private static List<@Nullable AttributedString> _splitStyledTextIntoParagraphs(
        final Tuple<StyledString> text,
        final Font                font,
        final BoxModelConf        boxModelConf
    ) {
        List<@Nullable AttributedString> paragraphs = new ArrayList<>();
        List<StyledString> currentParagraph = null;
        for ( StyledString styledString : text ) {
            String[] parts = styledString.string().split("\n", -1);
            if ( parts.length <= 1 ) {
                if ( currentParagraph == null )
                    currentParagraph = new ArrayList<>();
                currentParagraph.add(styledString);
            } else {
                for ( int i = 0; i < parts.length; i++ ) {
                    String part = parts[i];
                    if ( currentParagraph == null )
                        currentParagraph = new ArrayList<>();
                    if ( !part.isEmpty() )
                        currentParagraph.add(styledString.withString(part));
                    // if it is not the last part, we start a new line/paragraph:
                    if ( i < parts.length - 1 ) {
                        paragraphs.add(_paragraphToAttributedString(currentParagraph, font, boxModelConf));
                        currentParagraph = null;
                    }
                }
            }
        }
        if ( currentParagraph != null )
            paragraphs.add(_paragraphToAttributedString(currentParagraph, font, boxModelConf));
        return paragraphs;
    }

    private static @Nullable AttributedString _paragraphToAttributedString(
        final List<StyledString> paragraph,
        final Font font,
        final BoxModelConf       boxModelConf
    ) {
        int length = paragraph.stream().mapToInt(s -> s.string().length()).sum();
        if ( length <= 0 )
            return null;
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

}
