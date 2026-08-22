package swingtree.style;


import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import swingtree.SwingTree;
import swingtree.api.NoiseFunction;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class NoiseGradientPaint implements Paint
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(NoiseGradientPaint.class);

    /**
     * Cache for the context - when the bounds, center, and transform are unchanged, then the context is the same
     */
    private static class CachedContext
    {
        private final Point2D center;
        private final AffineTransform transform;

        private final NoiseGradientPaintContext cachedContext;


        private CachedContext(Point2D center, AffineTransform transform, NoiseGradientPaintContext context) {
            this.center = center;
            this.transform = transform;
            cachedContext = context;
        }

        private NoiseGradientPaint.@Nullable NoiseGradientPaintContext get(Point2D center, AffineTransform transform) {
            if (this.center.equals(center) && this.transform.equals(transform))
                return cachedContext;
            else
                return null;
        }
    }


    private final Point2D center;
    private final float scaleX;
    private final float scaleY;
    private final float rotation;
    private final NoiseFunction noiseFunction;
    private final float[] localFractions;
    private final float[] redStepLookup;
    private final float[] greenStepLookup;
    private final float[] blueStepLookup;
    private final float[] alphaStepLookup;
    private final Color[] colors;
    /** Whether every pixel this paint produces is fully opaque, see {@link #getTransparency()}. */
    private final boolean isOpaque;
    private static final float INT_TO_FLOAT_CONST = 1f / 255f;
    /** The alpha-less counterpart of {@link ColorModel#getRGBdefault()}, used by an opaque paint. */
    private static final ColorModel OPAQUE_COLOR_MODEL = new DirectColorModel(24, 0xFF0000, 0xFF00, 0xFF);
    private @Nullable CachedContext cached;


    public NoiseGradientPaint(
        final Point2D center,
        final float   scaleX,
        final float   scaleY,
        final float   rotation,
        final float[] fractions,
        final Color[] colors,
        final NoiseFunction noiseFunction
    )
    throws IllegalArgumentException
    {
        this.scaleX        = scaleX;
        this.scaleY        = scaleY;
        this.rotation      = rotation;
        this.noiseFunction = Objects.requireNonNull(noiseFunction);

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

        boolean allColorsOpaque = true;
        for ( Color color : this.colors )
            if ( color.getAlpha() < 255 ) {
                allColorsOpaque = false;
                break;
            }
        this.isOpaque = allColorsOpaque;
    }

    public Point2D getCenter() {
        return center;
    }

    public Point2D getScale() {
        return new Point2D.Float(scaleX, scaleY);
    }

    public float getRotation() {
        return rotation;
    }

    public NoiseFunction getNoiseFunction() {
        return noiseFunction;
    }

    public List<Color> getColors() {
        return Stream.of(colors).collect(Collectors.toList());
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
    public PaintContext createContext(
        final ColorModel      COLOR_MODEL,
        final Rectangle       DEVICE_BOUNDS,
        final Rectangle2D     USER_BOUNDS,
        final AffineTransform TRANSFORM,
        final RenderingHints  HINTS
    ) {

        if (cached != null) {
            NoiseGradientPaintContext c = cached.get(center, TRANSFORM);
            if (c != null)
                return c;
        }

        NoiseGradientPaintContext context = new NoiseGradientPaintContext(center, TRANSFORM);
        cached = new CachedContext(center, TRANSFORM, context);

        return context;
    }

    @Override
    public int getTransparency() {
        return isOpaque ? Transparency.OPAQUE : Transparency.TRANSLUCENT;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.center);
        hash = 97 * hash + Float.floatToIntBits(this.scaleX);
        hash = 97 * hash + Float.floatToIntBits(this.scaleY);
        hash = 97 * hash + Float.floatToIntBits(this.rotation);
        hash = 97 * hash + Objects.hashCode(this.noiseFunction);
        hash = 97 * hash + Arrays.hashCode(this.localFractions);
        hash = 97 * hash + Arrays.deepHashCode(this.colors);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final NoiseGradientPaint other = (NoiseGradientPaint) obj;
        if (Float.floatToIntBits(this.scaleX) != Float.floatToIntBits(other.scaleX))
            return false;
        if (Float.floatToIntBits(this.scaleY) != Float.floatToIntBits(other.scaleY))
            return false;
        if (Float.floatToIntBits(this.rotation) != Float.floatToIntBits(other.rotation))
            return false;
        if (!Objects.equals(this.center, other.center))
            return false;
        if (!Objects.equals(this.noiseFunction, other.noiseFunction))
            return false;
        if (!Objects.deepEquals(this.localFractions, other.localFractions))
            return false;
        if (!Objects.deepEquals(this.colors, other.colors))
            return false;
        return true;
    }

    private final class NoiseGradientPaintContext implements PaintContext
    {
        final private Point2D center;
        private final HashMap<Long, WritableRaster> cachedRasters;
        /**
         *  Reusable scratch buffer for the per-pixel sample data handed to
         *  {@link WritableRaster#setPixels(int, int, int, int, int[])}. Reused (grow-only)
         *  across {@link #getRaster} calls to avoid allocating a fresh array per tile.
         *  Safe to share because access to this context is single-threaded, just like the
         *  unsynchronized {@link #cachedRasters} mutation below.
         */
        private int[] scratchData = new int[0];

        public NoiseGradientPaintContext(final Point2D center, AffineTransform transform) {
            this.cachedRasters = new HashMap<>();
            try {
                this.center = transform.transform(center, null);  //user to device
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override public void dispose() {}

        @Override
        public ColorModel getColorModel() {
            return isOpaque ? OPAQUE_COLOR_MODEL : ColorModel.getRGBdefault();
        }

        @Override
        public @Nullable Raster getRaster(
            final int X,
            final int Y,
            final int TILE_WIDTH,
            final int TILE_HEIGHT
        ) {
            try {
                // The mask matters: a bare (long)Y would sign extend, collapsing every
                // tile of a negative row onto one and the same key.
                long index = ((long)X << 32) | (Y & 0xFFFFFFFFL);
                WritableRaster raster = cachedRasters.get(index);

                if (raster == null || raster.getWidth() < TILE_WIDTH || raster.getHeight() < TILE_HEIGHT)
                    raster = getColorModel().createCompatibleWritableRaster(TILE_WIDTH, TILE_HEIGHT);  // Create raster for given colormodel
                else
                    return raster;

                final int MAX = localFractions.length - 1;

                // Create or reuse data array with place for red, green, blue and alpha values
                final int bands      = isOpaque ? 3 : 4; // The channels
                final int dataLength = TILE_WIDTH * TILE_HEIGHT * bands;
                if ( scratchData.length < dataLength )
                    scratchData = new int[dataLength];
                final int[] data = scratchData;

                final boolean isRotated = ( rotation != 0f && rotation % 360f != 0f );
                final double  rotSin    = isRotated ? Math.sin(Math.toRadians(rotation)) : 0;
                final double  rotCos    = isRotated ? Math.cos(Math.toRadians(rotation)) : 1;

                int base = 0;
                for ( int tileY = 0; tileY < TILE_HEIGHT; tileY++ ) {
                    final double rowY = ( Y + tileY - center.getY() ) / scaleY;
                    for ( int tileX = 0; tileX < TILE_WIDTH; tileX++, base += bands ) {
                        double currentRed   = 0;
                        double currentGreen = 0;
                        double currentBlue  = 0;
                        double currentAlpha = 0;

                        double localX = ( X + tileX - center.getX() ) / scaleX;
                        double localY = rowY;
                        if ( isRotated ) {
                            final double newX = localX * rotCos - localY * rotSin;
                            final double newY = localX * rotSin + localY * rotCos;
                            localX = newX;
                            localY = newY;
                        }
                        final float x = (float) localX;
                        final float y = (float) localY;

                        float onGradientRange = noiseFunction.getFractionAt( x, y );
                        if ( !(onGradientRange > 0f) )
                            onGradientRange = 0f;
                        else if ( onGradientRange > 1f )
                            onGradientRange = 1f;

                        for ( int i = MAX - 1; i >= 0; i-- ) {
                            if ( onGradientRange >= localFractions[i] ) {
                                final float distance = onGradientRange - localFractions[i];
                                currentRed   = colors[i].getRed()   * INT_TO_FLOAT_CONST + distance * redStepLookup[i];
                                currentGreen = colors[i].getGreen() * INT_TO_FLOAT_CONST + distance * greenStepLookup[i];
                                currentBlue  = colors[i].getBlue()  * INT_TO_FLOAT_CONST + distance * blueStepLookup[i];
                                currentAlpha = colors[i].getAlpha() * INT_TO_FLOAT_CONST + distance * alphaStepLookup[i];
                                break;
                            }
                        }

                        data[base    ] = (int) Math.round(currentRed   * 255);
                        data[base + 1] = (int) Math.round(currentGreen * 255);
                        data[base + 2] = (int) Math.round(currentBlue  * 255);
                        if ( bands == 4 )
                            data[base + 3] = (int) Math.round(currentAlpha * 255);
                    }
                }

                // Fill the raster with the data
                raster.setPixels(0, 0, TILE_WIDTH, TILE_HEIGHT, data);

                cachedRasters.put(index, raster);
                return raster;
            }
            catch (Exception ex) {
                log.error(SwingTree.get().logMarker(), "Failed to compute the raster of a noise gradient.", ex);
                return null;
            }
        }

    }

}
