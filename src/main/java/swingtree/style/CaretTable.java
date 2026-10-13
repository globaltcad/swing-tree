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
    static final CaretTable EMPTY = new CaretTable("", "", Collections.emptyList(), 1f);

    private final String     _component;
    private final String     _text;
    private final List<Line> _lines;
    private final float      _caretWidth;
    private final int        _hashCode;

    private CaretTable( String component, String text, List<Line> lines, float caretWidth ) {
        _component  = component;
        _text       = text;
        _lines      = lines;
        _caretWidth = caretWidth;
        _hashCode   = Objects.hash(component, text, lines, caretWidth);
    }

    static CaretTable of( String component, String text, List<TextLayoutEngine.PlacedFragment> fragments ) {
        final List<Line> lines = new ArrayList<>(fragments.size());
        for ( TextLayoutEngine.PlacedFragment fragment : fragments ) {
            final float[] xs = new float[fragment.charEnd() - fragment.charStart + 1];
            for ( int i = 0; i < xs.length; i++ )
                xs[i] = _xOf(fragment, fragment.charStart + i);
            lines.add(new Line(fragment.charStart, fragment.top(), fragment.bottom(), xs));
        }
        return new CaretTable(component, text, Collections.unmodifiableList(lines), _caretWidth());
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

    static Optional<Bounds> caretBoundsOf( List<TextLayoutEngine.PlacedFragment> fragments, int position ) {
        TextLayoutEngine.PlacedFragment holder = null;
        for ( TextLayoutEngine.PlacedFragment fragment : fragments ) {
            if ( fragment.charStart <= position && position < fragment.charEnd() ) {
                holder = fragment;
                break;
            }
            if ( fragment.charEnd() == position )
                holder = fragment;
        }
        return holder == null ? Optional.empty() : Optional.of(_caretBounds(_xOf(holder, position), holder.top(), holder.bottom(), _caretWidth()));
    }

    private static float _xOf( TextLayoutEngine.PlacedFragment fragment, int position ) {
        return fragment.x + fragment.layout.getCaretInfo(TextHitInfo.leading(position - fragment.charStart))[0];
    }

    private static float _caretWidth() {
        return Math.max(1f, UI.scale());
    }

    private static Bounds _caretBounds( float x, float top, float bottom, float width ) {
        return Bounds.of(Math.round(x), top, width, bottom - top);
    }

    String component() { return _component; }

    String text() { return _text; }

    Optional<Bounds> boundsOf( int position, boolean endsSelection ) {
        Line holder = null;
        for ( Line line : _lines ) {
            if ( endsSelection && line.charStart() < position && position == line.charEnd() )
                return _boundsOf(line, position);
        }
        for ( Line line : _lines ) {
            if ( line.charStart() <= position && position < line.charEnd() ) {
                holder = line;
                break;
            }
            if ( line.charEnd() == position )
                holder = line;
        }
        return holder == null ? Optional.empty() : _boundsOf(holder, position);
    }

    private Optional<Bounds> _boundsOf( Line line, int position ) {
        return Optional.of(_caretBounds(line.xAt(position), line.top(), line.bottom(), _caretWidth));
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
        return getClass().getSimpleName() + "[component=" + _component + ", lines=" + _lines + ", caretWidth=" + _caretWidth + "]";
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
