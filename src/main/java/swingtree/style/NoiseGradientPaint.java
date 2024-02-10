package swingtree.style;


import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;



final class NoiseGradientPaint implements Paint
{
    private static final long   MULTIPLIER = 0x5DEECE66DL;
    private static final long   ADDEND = 0xBL;
    private static final long   MASK = (1L << 48) - 1;
    private static final double DOUBLE_UNIT = 0x1.0p-53; // 1.0 / (1L << 53)
    private static final long RANDOM_1 = 0x105139C0C031L;
    private static final long RANDOM_2 = 0x4c0e1e9f367dL;
    private static final long RANDOM_3 = 0xa785a819cd72c6fdL;


    /**
     * Cache for the context - when the bounds, center, and transform are unchanged, then the context is the same
     */
    private static class CachedContext
    {
        private final Rectangle bounds;
        private final Point2D center;
        private final AffineTransform transform;

        private final ConicalGradientPaintContext cachedContext;

        private CachedContext(Rectangle bounds, Point2D center, AffineTransform transform, ConicalGradientPaintContext context) {
            this.bounds = bounds;
            this.center = center;
            this.transform = transform;
            cachedContext = context;
        }

        private ConicalGradientPaintContext get(Rectangle bounds, Point2D center, AffineTransform transform) {
            if (this.bounds.equals(bounds) && this.center.equals(center) && this.transform.equals(transform))
                return cachedContext;
            else
                return null;
        }
    }


    private final Point2D CENTER;
    private final float ANGLE_OFFSET;
    private final float[] FRACTION_ANGLES;
    private final float[] RED_STEP_LOOKUP;
    private final float[] GREEN_STEP_LOOKUP;
    private final float[] BLUE_STEP_LOOKUP;
    private final float[] ALPHA_STEP_LOOKUP;
    private final Color[] COLORS;
    private static final float INT_TO_FLOAT_CONST = 1f / 255f;
    private CachedContext cached;


    public NoiseGradientPaint(
        final boolean USE_DEGREES,
        final Point2D CENTER,
        final float   GIVEN_OFFSET,
        final float[] GIVEN_FRACTIONS,
        final Color[] GIVEN_COLORS
    )
    throws IllegalArgumentException
    {
        // Check that fractions and colors are of the same size
        if (GIVEN_FRACTIONS.length != GIVEN_COLORS.length) {
            throw new IllegalArgumentException("Fractions and colors must be equal in size");
        }

        final java.util.List<Float> fractionList = new java.util.ArrayList<Float>(GIVEN_FRACTIONS.length);
        final float OFFSET;
        if (USE_DEGREES) {
            final float DEG_FRACTION = 1f / 360f;
            if (Float.compare((GIVEN_OFFSET * DEG_FRACTION), -0.5f) == 0) {
                OFFSET = -0.5f;
            } else if (Float.compare((GIVEN_OFFSET * DEG_FRACTION), 0.5f) == 0) {
                OFFSET = 0.5f;
            } else {
                OFFSET = (GIVEN_OFFSET * DEG_FRACTION);
            }
            for (final float fraction : GIVEN_FRACTIONS) {
                fractionList.add((fraction * DEG_FRACTION));
            }
        } else {
            {
                OFFSET = GIVEN_OFFSET;
            }
            for (final float fraction : GIVEN_FRACTIONS) {
                fractionList.add(fraction);
            }
        }

        // Check for valid offset
        if ( OFFSET >= 0.5f || OFFSET <= -0.5f ) {
            throw new IllegalArgumentException("Offset has to be in the range of -0.5 to 0.5");
        }

        this.ANGLE_OFFSET = GIVEN_OFFSET;

        // Adjust fractions and colors array in the case where startvalue != 0.0f and/or endvalue != 1.0f
        final java.util.List<Color> colorList = new java.util.ArrayList<Color>(GIVEN_COLORS.length);
        colorList.addAll(java.util.Arrays.asList(GIVEN_COLORS));

        //KK: Calculate the Color at the top dead center (mix between first and last)
        final Color start = colorList.get(0);
        final Color last = colorList.get(colorList.size()-1);
        final float centerVal = 1.0f - fractionList.get(fractionList.size()-1);
        final float lastToStartRange = centerVal + fractionList.get(0);

        final float firstFraction = fractionList.get(0);
        final float lastFraction  = fractionList.get(fractionList.size()-1);

        if ( firstFraction != 0f || lastFraction != 1f ) {
            Color centerColor = getColorFromFraction(last, start, (int)(lastToStartRange * 10000), (int)(centerVal * 10000));

            // Assure that fractions start with 0.0f
            if (firstFraction != 0.0f) {
                fractionList.add(0, 0.0f);
                colorList.add(0, centerColor);
            }

            // Assure that fractions end with 1.0f
            if (lastFraction != 1.0f) {
                fractionList.add(1.0f);
                colorList.add(centerColor);
            }
        }

        // Set the values
        this.CENTER = CENTER;
        COLORS = colorList.toArray(new Color[0]);

        // Prepare lookup table for the angles of each fraction
        final int MAX_FRACTIONS = fractionList.size();
        this.FRACTION_ANGLES = new float[MAX_FRACTIONS];
        for (int i = 0; i < MAX_FRACTIONS; i++) {
            FRACTION_ANGLES[i] = fractionList.get(i) * 360f;
        }

        // Prepare lookup tables for the color stepsize of each color
        RED_STEP_LOOKUP = new float[COLORS.length];
        GREEN_STEP_LOOKUP = new float[COLORS.length];
        BLUE_STEP_LOOKUP = new float[COLORS.length];
        ALPHA_STEP_LOOKUP = new float[COLORS.length];

        for (int i = 0; i < (COLORS.length - 1); i++) {
            RED_STEP_LOOKUP[i]   = ((COLORS[i + 1].getRed()   - COLORS[i].getRed()) * INT_TO_FLOAT_CONST)   / (FRACTION_ANGLES[i + 1] - FRACTION_ANGLES[i]);
            GREEN_STEP_LOOKUP[i] = ((COLORS[i + 1].getGreen() - COLORS[i].getGreen()) * INT_TO_FLOAT_CONST) / (FRACTION_ANGLES[i + 1] - FRACTION_ANGLES[i]);
            BLUE_STEP_LOOKUP[i]  = ((COLORS[i + 1].getBlue()  - COLORS[i].getBlue()) * INT_TO_FLOAT_CONST)  / (FRACTION_ANGLES[i + 1] - FRACTION_ANGLES[i]);
            ALPHA_STEP_LOOKUP[i] = ((COLORS[i + 1].getAlpha() - COLORS[i].getAlpha()) * INT_TO_FLOAT_CONST) / (FRACTION_ANGLES[i + 1] - FRACTION_ANGLES[i]);
        }
    }

    private static Color getColorFromFraction(
        final Color START_COLOR,
        final Color DESTINATION_COLOR,
        final int RANGE,
        final int VALUE
    ) {
        final float SOURCE_RED   = START_COLOR.getRed()   * INT_TO_FLOAT_CONST;
        final float SOURCE_GREEN = START_COLOR.getGreen() * INT_TO_FLOAT_CONST;
        final float SOURCE_BLUE  = START_COLOR.getBlue()  * INT_TO_FLOAT_CONST;
        final float SOURCE_ALPHA = START_COLOR.getAlpha() * INT_TO_FLOAT_CONST;

        final float DESTINATION_RED   = DESTINATION_COLOR.getRed()   * INT_TO_FLOAT_CONST;
        final float DESTINATION_GREEN = DESTINATION_COLOR.getGreen() * INT_TO_FLOAT_CONST;
        final float DESTINATION_BLUE  = DESTINATION_COLOR.getBlue()  * INT_TO_FLOAT_CONST;
        final float DESTINATION_ALPHA = DESTINATION_COLOR.getAlpha() * INT_TO_FLOAT_CONST;

        final float RED_DELTA   = DESTINATION_RED   - SOURCE_RED;
        final float GREEN_DELTA = DESTINATION_GREEN - SOURCE_GREEN;
        final float BLUE_DELTA  = DESTINATION_BLUE  - SOURCE_BLUE;
        final float ALPHA_DELTA = DESTINATION_ALPHA - SOURCE_ALPHA;

        final float RED_FRACTION   = RED_DELTA   / RANGE;
        final float GREEN_FRACTION = GREEN_DELTA / RANGE;
        final float BLUE_FRACTION  = BLUE_DELTA  / RANGE;
        final float ALPHA_FRACTION = ALPHA_DELTA / RANGE;

        float red   = SOURCE_RED   + RED_FRACTION   * VALUE;
        float green = SOURCE_GREEN + GREEN_FRACTION * VALUE;
        float blue  = SOURCE_BLUE  + BLUE_FRACTION  * VALUE;
        float alpha = SOURCE_ALPHA + ALPHA_FRACTION * VALUE;

        red   = red   < 0f ? 0f : (red   > 1f ? 1f : red);
        green = green < 0f ? 0f : (green > 1f ? 1f : green);
        blue  = blue  < 0f ? 0f : (blue  > 1f ? 1f : blue);
        alpha = alpha < 0f ? 0f : (alpha > 1f ? 1f : alpha);

        return new Color(red, green, blue, alpha);
    }


    @Override
    public PaintContext createContext(final ColorModel COLOR_MODEL,
                                               final Rectangle DEVICE_BOUNDS,
                                               final Rectangle2D USER_BOUNDS,
                                               final AffineTransform TRANSFORM,
                                               final RenderingHints HINTS) {


        //KK - speed up repaints by caching the context
        if (cached != null) {
            ConicalGradientPaintContext c = cached.get(DEVICE_BOUNDS, CENTER, TRANSFORM);
            if (c != null)
                return c;
        }

        ConicalGradientPaintContext context = new ConicalGradientPaintContext(CENTER, TRANSFORM);
        cached = new CachedContext(DEVICE_BOUNDS, CENTER, TRANSFORM, context);

        return context;
    }

    @Override
    public int getTransparency() {
        return Transparency.TRANSLUCENT;
    }

    private final class ConicalGradientPaintContext implements PaintContext
    {
        final private Point2D CENTER;
        private final HashMap<Long, WritableRaster> cachedRasters;
        private final AffineTransform transform;

        public ConicalGradientPaintContext(final Point2D CENTER, AffineTransform transform) {
            this.cachedRasters = new HashMap<>();
            this.transform = transform;
            try {
                this.CENTER = transform.transform(CENTER, null);  //user to device
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void dispose() {
        }

        @Override
        public ColorModel getColorModel() {
            return ColorModel.getRGBdefault();
        }

        @Override
        public Raster getRaster(
            final int X,
            final int Y,
            final int TILE_WIDTH,
            final int TILE_HEIGHT
        ) {
            try {
                long index = ((long)X << 32) | (long)Y;
                WritableRaster raster = cachedRasters.get(index);

                if (raster == null)
                    raster = getColorModel().createCompatibleWritableRaster(TILE_WIDTH, TILE_HEIGHT);  // Create raster for given colormodel
                else
                    return raster;

                //get the rotation center in device space
                final double ROTATION_CENTER_X = -X + CENTER.getX();
                final double ROTATION_CENTER_Y = -Y + CENTER.getY();

                //Convert to user space
                Point2D rotationCenter = transform.inverseTransform(new Point2D.Double(ROTATION_CENTER_X, ROTATION_CENTER_Y), null); //device to user

                final int MAX = FRACTION_ANGLES.length - 1;

                // Create data array with place for red, green, blue and alpha values
                final int[] data = new int[(TILE_WIDTH * TILE_HEIGHT * 4)];

                double dx;
                double dy;
                double angle;
                double currentRed = 0;
                double currentGreen = 0;
                double currentBlue = 0;
                double currentAlpha = 0;

                for (int tileY = 0; tileY < TILE_HEIGHT; tileY++) {
                    for (int tileX = 0; tileX < TILE_WIDTH; tileX++) {

                        Point2D d = transform.inverseTransform(new Point2D.Double(tileX, tileY), null); //device to user

                        // Calculate the distance between the current position and the rotation angle
                        dx = d.getX() - rotationCenter.getX();
                        dy = d.getY() - rotationCenter.getY();

                        //KK: simplify by just taking the arctangent
                        double distance = Math.sqrt(dx * dx + dy * dy);

                        // Avoid division by zero
                        if (distance == 0) {
                            distance = 1;
                        }

                        // 0 degree on top
                        angle = Math.abs(Math.toDegrees(Math.acos(dx / distance)));
                        //angle = Math.abs(Math.toDegrees(-Math.atan2(dy, dx)));

                        if (dx >= 0 && dy <= 0) {
                            angle = 90.0 - angle;
                        } else if (dx >= 0 && dy >= 0) {
                            angle += 90.0;
                        } else if (dx <= 0 && dy >= 0) {
                            angle += 90.0;
                        } else if (dx <= 0 && dy <= 0) {
                            angle = 450.0 - angle;
                        }

                        angle += ANGLE_OFFSET;

                        if (angle > 360.0) {
                            angle -= 360.0;
                        } else if (angle < 0.0) {
                            angle += 360.0;
                        }

                        // Check for each angle in fractionAngles array
                        for (int i = 0; i < MAX; i++) {
                            if ((angle >= FRACTION_ANGLES[i])) {
                                currentRed   = COLORS[i].getRed()   * INT_TO_FLOAT_CONST + (angle - FRACTION_ANGLES[i]) * RED_STEP_LOOKUP[i];
                                currentGreen = COLORS[i].getGreen() * INT_TO_FLOAT_CONST + (angle - FRACTION_ANGLES[i]) * GREEN_STEP_LOOKUP[i];
                                currentBlue  = COLORS[i].getBlue()  * INT_TO_FLOAT_CONST + (angle - FRACTION_ANGLES[i]) * BLUE_STEP_LOOKUP[i];
                                currentAlpha = COLORS[i].getAlpha() * INT_TO_FLOAT_CONST + (angle - FRACTION_ANGLES[i]) * ALPHA_STEP_LOOKUP[i];
                            }
                        }

                        // Fill data array with calculated color values
                        final int BASE = (tileY * TILE_WIDTH + tileX) * 4;

                        data[BASE + 0] = (int) (currentRed   * 255);
                        data[BASE + 1] = (int) (currentGreen * 255);
                        data[BASE + 2] = (int) (currentBlue  * 255);
                        data[BASE + 3] = (int) (currentAlpha * 255);
                    }
                }

                // Fill the raster with the data
                raster.setPixels(0, 0, TILE_WIDTH, TILE_HEIGHT, data);

                cachedRasters.put(index, raster);
                return raster;
            }
            catch (NoninvertibleTransformException ex) {
                System.err.println(ex);
                return null;
            }
        }

    }
    
    /**
     *  Takes 2 floats defining an x and y coordinate and returns a long
     *  which is the bits of the two floats consecutively concatenated.
     */
    private static long _floatCoordinatesToLongBits(float x, float y ) {
        int xi = Float.floatToRawIntBits(x);
        int yi = Float.floatToRawIntBits(y);
        return ( (long) xi << 32 ) | (long) yi;
    }

    private static long _seedFromCoordinates( float x, float y )
    {
        long seed = _baseScramble( _floatCoordinatesToLongBits( x, y ) );
        return (( seed * RANDOM_1 + RANDOM_2     ) ^ seed) ^ RANDOM_3;
    }

    public static long _baseScramble( long seed ) {
        return ADDEND + (seed ^ MULTIPLIER) & MASK;
    }


    private static double _nextDouble( long seed1, long seed2 ) {
        return (((long)(_next(26, seed1)) << 27) + _next(27, seed2)) * DOUBLE_UNIT;
    }

    private static long _nextSeed( long currentSeed )
    {
        long oldseed, nextseed;
        do {
            oldseed = currentSeed;
            nextseed = (oldseed * MULTIPLIER + ADDEND) & MASK;
        } while ( oldseed == (currentSeed = nextseed) );
        return nextseed;
    }

    private static int _intFrom( long seed ) {
        return _next(32, _nextSeed(seed));
    }

    private static int _next( int bits, long seed ) { return (int)(seed >>> (48 - bits)); }

    private static boolean _boolFrom( long seed ) {
        return _next(1, _nextSeed(seed)) != 0;
    }

    public static double _gaussianFrom( long seed )
    {
        // See Knuth, ACP, Section 3.4.1 Algorithm C.
        double v1, v2, s;
        do {
            long seed1 = _nextSeed(seed );
            long seed2 = _nextSeed(seed1);
            long seed3 = _nextSeed(seed2);
            long seed4 = _nextSeed(seed3);
            v1 = 2 * _nextDouble( seed1, seed2 ) - 1; // between -1 and 1
            v2 = 2 * _nextDouble( seed3, seed4 ) - 1; // between -1 and 1
            s = v1 * v1 + v2 * v2;
            seed = seed4;
        }
        while ( s >= 1 || s == 0 );

        double multiplier = StrictMath.sqrt( -2 * StrictMath.log(s) / s );

        if ( _boolFrom(seed) )
            return v1 * multiplier;
        else
            return v2 * multiplier;
    }

}
