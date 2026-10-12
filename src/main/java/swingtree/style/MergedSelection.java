package swingtree.style;

import org.jspecify.annotations.Nullable;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class MergedSelection
{
    private static final double ARC_CONTROL = 0.5522847498;
    private static final double SNAP_DISTANCE = 0.5;

    private static volatile @Nullable MergedSelection _lastPainted;

    private final List<Rectangle2D> _areas;
    private final float             _convexRadius;
    private final float             _concaveRadius;
    private final float             _borderWidth;
    private final Shape             _outline;
    private final @Nullable Shape   _insideTheBorder;

    private MergedSelection( List<Rectangle2D> areas, float convexRadius, float concaveRadius, float borderWidth ) {
        final List<List<Point2D>> outline = _outlineOf(areas);
        _areas           = areas;
        _convexRadius    = convexRadius;
        _concaveRadius   = concaveRadius;
        _borderWidth     = borderWidth;
        _outline         = _rounded(outline, convexRadius, concaveRadius);
        _insideTheBorder = borderWidth > 0 ? _rounded(_eroded(outline, borderWidth), Math.max(0, convexRadius - borderWidth), concaveRadius + borderWidth) : null;
    }

    static MergedSelection of( List<Rectangle2D> areas, float convexRadius, float concaveRadius, float borderWidth ) {
        final @Nullable MergedSelection last = _lastPainted;
        if ( last != null && last._convexRadius == convexRadius && last._concaveRadius == concaveRadius && last._borderWidth == borderWidth && last._areas.equals(areas) )
            return last;
        final MergedSelection merged = new MergedSelection(areas, convexRadius, concaveRadius, borderWidth);
        _lastPainted = merged;
        return merged;
    }

    Shape outline() { return _outline; }

    @Nullable Shape insideTheBorder() { return _insideTheBorder; }

    private static List<List<Point2D>> _outlineOf( List<Rectangle2D> areas ) {
        final Area union = new Area();
        for ( Rectangle2D area : areas ) {
            double bottom = area.getMaxY();
            for ( Rectangle2D below : areas ) {
                final double gap = below.getMinY() - bottom;
                final boolean overlapsHorizontally = below.getMinX() < area.getMaxX() && area.getMinX() < below.getMaxX();
                if ( gap > 0 && gap < SNAP_DISTANCE && overlapsHorizontally )
                    bottom = below.getMinY();
            }
            union.add(new Area(new Rectangle2D.Double(area.getX(), area.getY(), area.getWidth(), bottom - area.getY())));
        }
        return _polygonsOf(union);
    }

    private static List<List<Point2D>> _eroded( List<List<Point2D>> outline, float width ) {
        if ( outline.size() == 1 ) {
            final List<Point2D> polygon = outline.get(0);
            final double interiorSide = Math.signum(_signedArea(polygon));
            final int count = polygon.size();
            final List<Point2D> inset = new ArrayList<>(count);
            for ( int i = 0; i < count; i++ ) {
                final Point2D previous = polygon.get((i + count - 1) % count);
                final Point2D corner   = polygon.get(i);
                final Point2D next     = polygon.get((i + 1) % count);
                final double  inX = _inwardX(previous, corner, interiorSide) + _inwardX(corner, next, interiorSide);
                final double  inY = _inwardY(previous, corner, interiorSide) + _inwardY(corner, next, interiorSide);
                inset.add(new Point2D.Double(corner.getX() + width * inX, corner.getY() + width * inY));
            }
            if ( _keepsEveryEdgeDirection(polygon, inset) )
                return Collections.singletonList(inset);
        }
        final Area eroded = _areaOf(outline);
        for ( List<Point2D> polygon : outline ) {
            for ( int i = 0; i < polygon.size(); i++ ) {
                final Point2D from = polygon.get(i);
                final Point2D to   = polygon.get((i + 1) % polygon.size());
                eroded.subtract(new Area(new Rectangle2D.Double(
                    Math.min(from.getX(), to.getX()) - width, Math.min(from.getY(), to.getY()) - width,
                    Math.abs(to.getX() - from.getX()) + 2 * width, Math.abs(to.getY() - from.getY()) + 2 * width
                )));
            }
        }
        return _polygonsOf(eroded);
    }

    private static Shape _rounded( List<List<Point2D>> polygons, float convexRadius, float concaveRadius ) {
        final double interiorSide = Math.signum(_signedArea(_largest(polygons)));
        final Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for ( List<Point2D> polygon : polygons ) {
            final int count = polygon.size();
            for ( int i = 0; i < count; i++ ) {
                final Point2D previous = polygon.get((i + count - 1) % count);
                final Point2D corner   = polygon.get(i);
                final Point2D next     = polygon.get((i + 1) % count);
                final boolean isConvex = Math.signum(_turn(previous, corner, next)) == interiorSide;
                final double  radius   = Math.min(isConvex ? convexRadius : concaveRadius, Math.min(corner.distance(previous), corner.distance(next)) / 2);
                final Point2D start    = _towards(corner, previous, radius);
                final Point2D end      = _towards(corner, next, radius);
                if ( i == 0 )
                    path.moveTo(start.getX(), start.getY());
                else
                    path.lineTo(start.getX(), start.getY());
                if ( radius > 0 ) {
                    final Point2D control1 = _towards(start, corner, start.distance(corner) * ARC_CONTROL);
                    final Point2D control2 = _towards(end, corner, end.distance(corner) * ARC_CONTROL);
                    path.curveTo(control1.getX(), control1.getY(), control2.getX(), control2.getY(), end.getX(), end.getY());
                }
            }
            path.closePath();
        }
        return path;
    }

    private static List<List<Point2D>> _polygonsOf( Area area ) {
        final List<List<Point2D>> polygons = new ArrayList<>();
        List<Point2D> polygon = new ArrayList<>();
        final double[] coordinates = new double[6];
        for ( PathIterator it = area.getPathIterator(null); !it.isDone(); it.next() ) {
            final int segment = it.currentSegment(coordinates);
            if ( segment == PathIterator.SEG_CLOSE ) {
                final List<Point2D> corners = _withoutStraightPoints(polygon);
                if ( corners.size() >= 3 )
                    polygons.add(corners);
                polygon = new ArrayList<>();
            }
            else
                polygon.add(new Point2D.Double(coordinates[0], coordinates[1]));
        }
        return polygons;
    }

    private static List<Point2D> _withoutStraightPoints( List<Point2D> points ) {
        final List<Point2D> corners = new ArrayList<>(points);
        boolean removed = true;
        while ( removed && corners.size() >= 3 ) {
            removed = false;
            for ( int i = 0; i < corners.size(); i++ ) {
                final int count = corners.size();
                final Point2D previous = corners.get((i + count - 1) % count);
                final Point2D next     = corners.get((i + 1) % count);
                if ( corners.get(i).distance(next) < 1e-6 || Math.abs(_turn(previous, corners.get(i), next)) < 1e-9 ) {
                    corners.remove(i);
                    removed = true;
                    break;
                }
            }
        }
        return corners;
    }

    private static double _inwardX( Point2D from, Point2D to, double interiorSide ) {
        return -interiorSide * (to.getY() - from.getY()) / from.distance(to);
    }

    private static double _inwardY( Point2D from, Point2D to, double interiorSide ) {
        return interiorSide * (to.getX() - from.getX()) / from.distance(to);
    }

    private static boolean _keepsEveryEdgeDirection( List<Point2D> polygon, List<Point2D> inset ) {
        for ( int i = 0; i < polygon.size(); i++ ) {
            final int j = (i + 1) % polygon.size();
            final double dot = (polygon.get(j).getX() - polygon.get(i).getX()) * (inset.get(j).getX() - inset.get(i).getX())
                             + (polygon.get(j).getY() - polygon.get(i).getY()) * (inset.get(j).getY() - inset.get(i).getY());
            if ( dot <= 0 )
                return false;
        }
        return true;
    }

    private static Area _areaOf( List<List<Point2D>> polygons ) {
        final Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for ( List<Point2D> polygon : polygons ) {
            path.moveTo(polygon.get(0).getX(), polygon.get(0).getY());
            for ( int i = 1; i < polygon.size(); i++ )
                path.lineTo(polygon.get(i).getX(), polygon.get(i).getY());
            path.closePath();
        }
        return new Area(path);
    }

    private static List<Point2D> _largest( List<List<Point2D>> polygons ) {
        List<Point2D> largest = new ArrayList<>();
        for ( List<Point2D> polygon : polygons )
            if ( Math.abs(_signedArea(polygon)) > Math.abs(_signedArea(largest)) )
                largest = polygon;
        return largest;
    }

    private static double _signedArea( List<Point2D> polygon ) {
        double area = 0;
        for ( int i = 0; i < polygon.size(); i++ ) {
            final Point2D a = polygon.get(i);
            final Point2D b = polygon.get((i + 1) % polygon.size());
            area += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return area;
    }

    private static double _turn( Point2D previous, Point2D corner, Point2D next ) {
        return (corner.getX() - previous.getX()) * (next.getY() - corner.getY()) - (corner.getY() - previous.getY()) * (next.getX() - corner.getX());
    }

    private static Point2D _towards( Point2D from, Point2D to, double distance ) {
        final double length = from.distance(to);
        if ( length == 0 )
            return from;
        return new Point2D.Double(from.getX() + (to.getX() - from.getX()) * distance / length, from.getY() + (to.getY() - from.getY()) * distance / length);
    }
}
