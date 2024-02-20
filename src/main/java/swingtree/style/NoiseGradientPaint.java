package swingtree.style;


import swingtree.api.NoiseFunction;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.IntStream;


final class NoiseGradientPaint implements Paint
{
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
    private static final float INT_TO_FLOAT_CONST = 1f / 255f;
    private CachedContext cached;


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

        public ConicalGradientPaintContext(final Point2D center, AffineTransform transform) {
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

                final int MAX = localFractions.length - 1;

                // Create data array with place for red, green, blue and alpha values
                final int[] data = new int[(TILE_WIDTH * TILE_HEIGHT * 4)];

                IntStream.range(0, TILE_WIDTH * TILE_HEIGHT)
                        .parallel()
                        .forEach(tileIndex -> {
                            double currentRed   = 0;
                            double currentGreen = 0;
                            double currentBlue  = 0;
                            double currentAlpha = 0;
                            double onGradientRange;

                            int tileY = tileIndex / TILE_WIDTH;
                            int tileX = tileIndex % TILE_WIDTH;

                            double localX = ( X + tileX - center.getX() ) / scaleX;
                            double localY = ( Y + tileY - center.getY() ) / scaleY;
                            if ( rotation != 0f && rotation % 360f != 0f ) {
                                final double angle = Math.toRadians(rotation);
                                final double sin   = Math.sin(angle);
                                final double cos   = Math.cos(angle);
                                final double newX = localX * cos - localY * sin;
                                final double newY = localX * sin + localY * cos;
                                localX = newX;
                                localY = newY;
                            }
                            float x = (float) localX;
                            float y = (float) localY;

                            onGradientRange = noiseFunction.getFractionAt( x, y );

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

                            data[BASE + 0] = (int) Math.round(currentRed   * 255);
                            data[BASE + 1] = (int) Math.round(currentGreen * 255);
                            data[BASE + 2] = (int) Math.round(currentBlue  * 255);
                            data[BASE + 3] = (int) Math.round(currentAlpha * 255);
                        });

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

}
