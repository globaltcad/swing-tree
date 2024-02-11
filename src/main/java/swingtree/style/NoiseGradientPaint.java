package swingtree.style;


import java.awt.*;
import java.awt.geom.AffineTransform;
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


    private final Point2D center;
    private final float scale;
    private final float rotation;
    private final float[] localFractions;
    private final float[] redStepLookup;
    private final float[] greenStepLookup;
    private final float[] blueStepLookup;
    private final float[] alphaStepLookup;
    private final Color[] colors;
    private static final float INT_TO_FLOAT_CONST = 1f / 255f;
    private CachedContext cached;


    public NoiseGradientPaint(
        final Point2D center,
        final float   scale,
        final float   rotation,
        final float[] fractions,
        final Color[] colors
    )
    throws IllegalArgumentException
    {
        this.scale = scale;
        this.rotation = rotation;

        // Check that fractions and colors are of the same size
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Fractions and colors must be equal in size");
        }

        final java.util.List<Float> fractionList = new java.util.ArrayList<Float>(fractions.length);
        for (float f : fractions) {
            if (f < 0 || f > 1) {
                throw new IllegalArgumentException("Fraction values must be in the range 0 to 1: " + f);
            }
            fractionList.add(f);
        }

        // Adjust fractions and colors array in the case where startvalue != 0.0f and/or endvalue != 1.0f
        final java.util.List<Color> colorList = new java.util.ArrayList<Color>(colors.length);
        colorList.addAll(java.util.Arrays.asList(colors));

        // Calculate the Color at the top dead center (mix between first and last)
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
        this.center = center;
        this.colors = colorList.toArray(new Color[0]);

        // Prepare lookup table for the angles of each fraction
        final int MAX_FRACTIONS = fractionList.size();
        this.localFractions = new float[MAX_FRACTIONS];
        for (int i = 0; i < MAX_FRACTIONS; i++) {
            localFractions[i] = fractionList.get(i);
        }

        // Prepare lookup tables for the color stepsize of each color
        this.redStepLookup   = new float[this.colors.length];
        this.greenStepLookup = new float[this.colors.length];
        this.blueStepLookup  = new float[this.colors.length];
        this.alphaStepLookup = new float[this.colors.length];

        for (int i = 0; i < (this.colors.length - 1); i++) {
            this.redStepLookup[i]   = ((this.colors[i + 1].getRed()   - this.colors[i].getRed()) * INT_TO_FLOAT_CONST)   / (localFractions[i + 1] - localFractions[i]);
            this.greenStepLookup[i] = ((this.colors[i + 1].getGreen() - this.colors[i].getGreen()) * INT_TO_FLOAT_CONST) / (localFractions[i + 1] - localFractions[i]);
            this.blueStepLookup[i]  = ((this.colors[i + 1].getBlue()  - this.colors[i].getBlue()) * INT_TO_FLOAT_CONST)  / (localFractions[i + 1] - localFractions[i]);
            this.alphaStepLookup[i] = ((this.colors[i + 1].getAlpha() - this.colors[i].getAlpha()) * INT_TO_FLOAT_CONST) / (localFractions[i + 1] - localFractions[i]);
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
            ConicalGradientPaintContext c = cached.get(DEVICE_BOUNDS, center, TRANSFORM);
            if (c != null)
                return c;
        }

        ConicalGradientPaintContext context = new ConicalGradientPaintContext(center, TRANSFORM);
        cached = new CachedContext(DEVICE_BOUNDS, center, TRANSFORM, context);

        return context;
    }

    @Override
    public int getTransparency() {
        return Transparency.TRANSLUCENT;
    }

    private final class ConicalGradientPaintContext implements PaintContext
    {
        final private Point2D center;
        private final HashMap<Long, WritableRaster> cachedRasters;
        private final AffineTransform transform;

        public ConicalGradientPaintContext(final Point2D center, AffineTransform transform) {
            this.cachedRasters = new HashMap<>();
            this.transform = transform;
            try {
                this.center = transform.transform(center, null);  //user to device
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
            System.out.println("getRaster "+X+" "+Y+" "+TILE_WIDTH+" "+TILE_HEIGHT);
            try {
                long index = ((long)X << 32) | (long)Y;
                WritableRaster raster = cachedRasters.get(index);

                if (raster == null)
                    raster = getColorModel().createCompatibleWritableRaster(TILE_WIDTH, TILE_HEIGHT);  // Create raster for given colormodel
                else
                    return raster;

                final int MAX = localFractions.length - 1;

                // Create data array with place for red, green, blue and alpha values
                final int[] data = new int[(TILE_WIDTH * TILE_HEIGHT * 4)];

                double onGradientRange;
                double currentRed = 0;
                double currentGreen = 0;
                double currentBlue = 0;
                double currentAlpha = 0;

                for (int tileY = 0; tileY < TILE_HEIGHT; tileY++) {
                    for (int tileX = 0; tileX < TILE_WIDTH; tileX++) {

                        int x = (int) (center.getX() + (X + tileX) / scale);
                        int y = (int) (center.getY() + (Y + tileY) / scale);
                        onGradientRange = _coordinateToGradValue(x, y);

                        // Check for each angle in fractionAngles array
                        for (int i = 0; i < MAX; i++) {
                            if ((onGradientRange >= localFractions[i])) {
                                currentRed   = colors[i].getRed()   * INT_TO_FLOAT_CONST + (onGradientRange - localFractions[i]) * redStepLookup[i];
                                currentGreen = colors[i].getGreen() * INT_TO_FLOAT_CONST + (onGradientRange - localFractions[i]) * greenStepLookup[i];
                                currentBlue  = colors[i].getBlue()  * INT_TO_FLOAT_CONST + (onGradientRange - localFractions[i]) * blueStepLookup[i];
                                currentAlpha = colors[i].getAlpha() * INT_TO_FLOAT_CONST + (onGradientRange - localFractions[i]) * alphaStepLookup[i];
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
            catch (Exception ex) {
                System.err.println(ex);
                return null;
            }
        }

    }

    double _coordinateToGradValue(float x, float y ) {
        final int subPixelDivision = 16;
        final int maxDistance = subPixelDivision / 2;
        double height = 0;
        for ( int i0 = 0; i0 < subPixelDivision; i0++ ) {
            for ( int i1 = 0; i1 < subPixelDivision; i1++ ) {
                final float xi = ( i0 - maxDistance ) + x;
                final float yi = ( i1 - maxDistance ) + y;
                final int rx = Math.round( xi );
                final int ry = Math.round( yi );
                final boolean takeGaussian = 0.05 > _fractionFrom( _floatCoordinatesToLongBits( rx, ry ) );
                if ( takeGaussian ) {
                    final double vx = xi - x;
                    final double vy = yi - y;
                    final double distance = Math.sqrt( vx * vx + vy * vy );
                    final double relevance = Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fractionFrom(_floatCoordinatesToLongBits(ry, rx));
                    height = ( height * (1.0 - relevance) ) + ( frac * relevance );
                }
            }
        }
        return height;
    }

    /**
     *  Takes 2 floats defining an x and y coordinate and returns a long
     *  which is the bits of the two floats consecutively concatenated.
     */
    private static long _floatCoordinatesToLongBits( float x, float y ) {
        long xi = _baseScramble(Float.floatToRawIntBits(x/42.6372813208714286f));
        long yi = _baseScramble(Float.floatToRawIntBits(y*0.90982650387f));
        long hash = 91;
        hash = hash * 7 + xi;
        hash = hash * 31 + yi;
        hash = hash * 11 - xi;
        hash = hash * 5 - yi;
        return _baseScramble( hash );//( (long) xi << 32 ) | (long) yi;
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

    public static double _fractionFrom(long seed )
    {
        long seed1 = _nextSeed(seed );
        long seed2 = _nextSeed(seed1);
        return ((2 * _nextDouble( seed1, seed2 ) - 1)+1)/2;
    }

}
