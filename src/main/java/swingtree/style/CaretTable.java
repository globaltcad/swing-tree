package swingtree.style;

import com.google.errorprone.annotations.Immutable;
import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.layout.Bounds;

import java.awt.font.TextHitInfo;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Immutable
@SuppressWarnings("Immutable")
final class CaretTable
{
    static final CaretTable EMPTY = new CaretTable("", "", Collections.emptyList(), UI.CaretShape.BAR, 1f);

    private final String        _component;
    private final String        _text;
    private final List<Line>    _lines;
    private final UI.CaretShape _caretShape;
    private final float         _caretWidth;
    private final int           _hashCode;

    private CaretTable( String component, String text, List<Line> lines, UI.CaretShape caretShape, float caretWidth ) {
        _component  = component;
        _text       = text;
        _lines      = lines;
        _caretShape = caretShape;
        _caretWidth = caretWidth;
        _hashCode   = Objects.hash(component, text, lines, caretShape, caretWidth);
    }

    static CaretTable of( String component, String text, List<TextLayoutEngine.PlacedFragment> fragments, TextSelectionConf look ) {
        final List<Line> lines = new ArrayList<>(fragments.size());
        for ( TextLayoutEngine.PlacedFragment fragment : fragments ) {
            final float[] xs = new float[fragment.charEnd() - fragment.charStart + 1];
            for ( int i = 0; i < xs.length; i++ )
                xs[i] = _xOf(fragment, fragment.charStart + i);
            lines.add(new Line(fragment.charStart, fragment.top(), fragment.bottom(), xs));
        }
        return new CaretTable(component, text, Collections.unmodifiableList(lines), look.caretShape(), look.caretWidth());
    }

    static List<Rectangle2D> areasBetween( List<TextLayoutEngine.PlacedFragment> fragments, int start, int end ) {
        final List<Rectangle2D> areas = new ArrayList<>();
        for ( TextLayoutEngine.PlacedFragment fragment : fragments ) {
            final int from = Math.max(start, fragment.charStart);
            final int to   = Math.min(end, fragment.charEnd());
            if ( from < to ) {
                final float fromX = _xOf(fragment, from);
                final float toX   = _xOf(fragment, to);
                areas.add(new Rectangle2D.Float(Math.min(fromX, toX), fragment.top(), Math.abs(toX - fromX), fragment.bottom() - fragment.top()));
            }
        }
        return areas;
    }

    static Optional<Bounds> caretBoundsOf( List<TextLayoutEngine.PlacedFragment> fragments, int position, boolean atLineEnd, TextSelectionConf look ) {
        final TextLayoutEngine.@Nullable PlacedFragment holder = _holderOf(fragments, position, atLineEnd);
        if ( holder == null )
            return Optional.empty();
        final float nextX = position < holder.charEnd() ? _xOf(holder, position + 1) : Float.NaN;
        return Optional.of(_caretBounds(look.caretShape(), look.caretWidth(), _xOf(holder, position), nextX, holder.top(), holder.bottom()));
    }

    private static TextLayoutEngine.@Nullable PlacedFragment _holderOf( List<TextLayoutEngine.PlacedFragment> fragments, int position, boolean atLineEnd ) {
        if ( atLineEnd )
            for ( TextLayoutEngine.PlacedFragment fragment : fragments )
                if ( fragment.charStart < position && position == fragment.charEnd() )
                    return fragment;
        TextLayoutEngine.PlacedFragment holder = null;
        for ( TextLayoutEngine.PlacedFragment fragment : fragments ) {
            if ( fragment.charStart <= position && position < fragment.charEnd() )
                return fragment;
            if ( fragment.charEnd() == position )
                holder = fragment;
        }
        return holder;
    }

    private static float _xOf( TextLayoutEngine.PlacedFragment fragment, int position ) {
        return fragment.x + fragment.layout.getCaretInfo(TextHitInfo.leading(position - fragment.charStart))[0];
    }

    static float thicknessOf( float caretWidth ) {
        return Math.max(1f, caretWidth * UI.scale());
    }

    private static Bounds _caretBounds( UI.CaretShape shape, float caretWidth, float x, float nextX, float top, float bottom ) {
        final float height  = bottom - top;
        final float advance = Float.isNaN(nextX) ? 0 : Math.abs(nextX - x);
        final float width   = shape == UI.CaretShape.BAR ? thicknessOf(caretWidth) : advance > 0 ? advance : height / 2;
        return Bounds.of(Math.round(x), top, width, height);
    }

    String component() { return _component; }

    String text() { return _text; }

    Optional<Bounds> boundsOf( int position, boolean atLineEnd ) {
        final int line = lineOf(position, atLineEnd);
        return line < 0 ? Optional.empty() : _boundsOf(_lines.get(line), position);
    }

    int lineOf( int position, boolean atLineEnd ) {
        if ( atLineEnd )
            for ( int i = 0; i < _lines.size(); i++ )
                if ( _lines.get(i).endsAt(position) )
                    return i;
        int holder = -1;
        for ( int i = 0; i < _lines.size(); i++ ) {
            final Line line = _lines.get(i);
            if ( line.charStart() <= position && position < line.charEnd() )
                return i;
            if ( line.charEnd() == position )
                holder = i;
        }
        return holder;
    }

    int startOfLine( int line ) { return _lines.get(line).charStart(); }

    int endOfLine( int line ) { return _lines.get(line).charEnd(); }

    float xOf( int line, int position ) { return _lines.get(line).xAt(position); }

    int positionOnLine( int line, float x ) {
        final Line holder = _lines.get(line);
        return holder.charStart() + holder.gapNearest(x);
    }

    boolean endsLine( int line, int position ) { return _lines.get(line).endsAt(position); }

    int lineNextTo( int line, int direction, float x ) {
        final Line  origin       = _lines.get(line);
        final float originMiddle = origin.middle();
        int   nearest   = -1;
        float nearestDy = Float.MAX_VALUE;
        float nearestDx = Float.MAX_VALUE;
        for ( int i = 0; i < _lines.size(); i++ ) {
            final Line    candidate      = _lines.get(i);
            final boolean onRequiredSide = direction < 0 ? candidate.middle() < origin.top() : candidate.middle() > origin.bottom();
            if ( !onRequiredSide )
                continue;
            final float dy = Math.abs(candidate.middle() - originMiddle);
            final float dx = _distance(x, candidate.left(), candidate.right());
            if ( dy < nearestDy || ( dy == nearestDy && dx < nearestDx ) ) {
                nearest   = i;
                nearestDy = dy;
                nearestDx = dx;
            }
        }
        return nearest;
    }

    boolean isLineEnd( int position ) {
        for ( Line line : _lines )
            if ( line.endsAt(position) )
                return true;
        return false;
    }

    boolean endsLineAt( float x, float y ) {
        final Line line = _lineNearest(x, y);
        return line != null && line.endsAt(line.charStart() + line.gapNearest(x));
    }

    private Optional<Bounds> _boundsOf( Line line, int position ) {
        final float nextX = position < line.charEnd() ? line.xAt(position + 1) : Float.NaN;
        return Optional.of(_caretBounds(_caretShape, _caretWidth, line.xAt(position), nextX, line.top(), line.bottom()));
    }

    int positionAt( float x, float y ) {
        final Line line = _lineNearest(x, y);
        return line == null ? -1 : line.charStart() + line.gapNearest(x);
    }

    int characterAt( float x, float y ) {
        final Line line = _lineNearest(x, y);
        return line == null ? -1 : line.charStart() + line.characterAt(x);
    }

    boolean isOverText( float x, float y ) {
        for ( Line line : _lines )
            if ( _distance(y, line.top(), line.bottom()) == 0 && _distance(x, line.left(), line.right()) == 0 )
                return true;
        return false;
    }

    private @Nullable Line _lineNearest( float x, float y ) {
        Line nearest = null;
        float nearestDy = Float.MAX_VALUE;
        float nearestDx = Float.MAX_VALUE;
        for ( Line line : _lines ) {
            final float dy = _distance(y, line.top(), line.bottom());
            final float dx = _distance(x, line.left(), line.right());
            if ( dy < nearestDy || ( dy == nearestDy && dx < nearestDx ) ) {
                nearest   = line;
                nearestDy = dy;
                nearestDx = dx;
            }
        }
        return nearest;
    }

    private static float _distance( float value, float from, float to ) {
        return value < from ? from - value : value > to ? value - to : 0;
    }

    @Override
    public boolean equals( Object other ) {
        if ( this == other )
            return true;
        if ( !(other instanceof CaretTable) )
            return false;
        final CaretTable that = (CaretTable) other;
        return _hashCode == that._hashCode &&
               _caretShape == that._caretShape &&
               Float.floatToIntBits(_caretWidth) == Float.floatToIntBits(that._caretWidth) &&
               _component.equals(that._component) &&
               _text.equals(that._text) &&
               _lines.equals(that._lines);
    }

    @Override
    public int hashCode() {
        return _hashCode;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[component=" + _component + ", lines=" + _lines + ", caretShape=" + _caretShape + ", caretWidth=" + _caretWidth + "]";
    }

    @Immutable
    @SuppressWarnings("Immutable")
    private static final class Line
    {
        private final int     _charStart;
        private final float   _top;
        private final float   _bottom;
        private final float[] _xs;
        private final float   _left;
        private final float   _right;

        Line( int charStart, float top, float bottom, float[] xs ) {
            float left  = xs[0];
            float right = xs[0];
            for ( float x : xs ) {
                left  = Math.min(left, x);
                right = Math.max(right, x);
            }
            _charStart = charStart;
            _top       = top;
            _bottom    = bottom;
            _xs        = xs;
            _left      = left;
            _right     = right;
        }

        int charStart() { return _charStart; }

        int charEnd() { return _charStart + _xs.length - 1; }

        float top() { return _top; }

        float bottom() { return _bottom; }

        float middle() { return ( _top + _bottom ) / 2; }

        boolean endsAt( int position ) { return _charStart < position && position == charEnd(); }

        float left() { return _left; }

        float right() { return _right; }

        float xAt( int position ) { return _xs[position - _charStart]; }

        int gapNearest( float x ) {
            int nearest = 0;
            for ( int i = 1; i < _xs.length; i++ )
                if ( Math.abs(_xs[i] - x) < Math.abs(_xs[nearest] - x) )
                    nearest = i;
            return nearest;
        }

        int characterAt( float x ) {
            int nearest = 0;
            float nearestDistance = Float.MAX_VALUE;
            for ( int i = 0; i < _xs.length - 1; i++ ) {
                final float distance = _distance(x, Math.min(_xs[i], _xs[i + 1]), Math.max(_xs[i], _xs[i + 1]));
                if ( distance < nearestDistance ) {
                    nearest = i;
                    nearestDistance = distance;
                }
            }
            return nearest;
        }

        @Override
        public boolean equals( Object other ) {
            if ( !(other instanceof Line) )
                return false;
            final Line that = (Line) other;
            return _charStart == that._charStart &&
                   Float.floatToIntBits(_top)    == Float.floatToIntBits(that._top) &&
                   Float.floatToIntBits(_bottom) == Float.floatToIntBits(that._bottom) &&
                   Arrays.equals(_xs, that._xs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_charStart, _top, _bottom, Arrays.hashCode(_xs));
        }

        @Override
        public String toString() {
            return "Line[charStart=" + _charStart + ", top=" + _top + ", bottom=" + _bottom + ", xs=" + Arrays.toString(_xs) + "]";
        }
    }
}
