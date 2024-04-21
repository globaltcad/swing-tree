package swingtree.style;
/*
 * Copyright (c) 2012, Gerrit Grunwald
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * The names of its contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  Modifications 2018, by Kevin Kieffer
 *  The following modifications have been made and noted with "KK" in the comments:
 *  1. Added caches for the PaintContext and the Rasters, to speed up repaints when nothing has changed
 *  2. Apply the inverse trasform prior to calcuating the angle - this allows graphics transforms (such as rotations, shears) to affect the angle
 *  3. Replace the Pythag + arcos calculation with an arctan calcuation
 *  4. Adjust the colors at 0.0, 1.0 (top dead center) to blend the first and last defined color (rather than setting them to the first color)
 *
 *  Adopted from https://github.com/kkieffer/jZELD/blob/master/src/main/java/com/github/kkieffer/jzeld/attributes/ConicalGradientPaint.java
 *  and incorporated into the SwingTree project in 2024.
 *
 *  Modifications 2014, by Daniel Nepp:
 *  - Deletion of the error-prone method "recalculate" and introduction of a new rotation offset field variable.
 */

import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;


/**
 * A paint class that creates conical gradients around a given center point
 * It could be used in the same way as LinearGradientPaint and RadialGradientPaint
 * and follows the same syntax.
 * You could use floats from 0.0 to 1.0 for the fractions which is standard but it's
 * also possible to use angles from 0.0 to 360 degrees which is most of the times
 * much easier to handle.
 * Gradients always start at the top with a clockwise direction and you could
 * rotate the gradient around the center by given offset.
 * The offset could also be defined from -0.5 to +0.5 or -180 to +180 degrees.
 * If you would like to use degrees instead of values from 0 to 1 you have to use
 * the full constructor and set the USE_DEGREES variable to true
 * @version 1.0
 * @author hansolo
 */
final class ConicalGradientPaint implements Paint {

    /**
     * Cache for the context - when the bounds, center, and transform are unchanged, then the context is the same
     */
    private static class CachedContext {
        private final Rectangle _bounds;
        private final Point2D _center;
        private final AffineTransform _transform;

        private final ConicalGradientPaintContext _cachedContext;

        private CachedContext(
            Rectangle bounds,
            Point2D center,
            AffineTransform transform,
            ConicalGradientPaintContext context
        ) {
            _bounds        = bounds;
            _center        = center;
            _transform     = transform;
            _cachedContext = context;
        }

        /**
         * KK: Get the cached context - if it matches the supplied arguments.  If the arguments aren't equal to the cached version, return null
         * @param bounds A {@link Rectangle} representing the bounds of the context to be created (device space).
         * @param center The center of the gradient (device space).
         * @param transform The transform to be applied to the context (user space to device space).
         * @return the cached context, or null if the arguments don't match
         */
        private @Nullable ConicalGradientPaintContext get(
                Rectangle       bounds, 
                Point2D         center, 
                AffineTransform transform
        ) {
            if (_bounds.equals(bounds) && _center.equals(center) && _transform.equals(transform))
                return _cachedContext;
            else
                return null;
        }
    }


    private final Point2D _center;
    private final float   _angleOffset;
    private final float[] _fractionOffsets;
    private final float[] _redStepLookup;
    private final float[] _greenStepLookup;
    private final float[] _blueStepLookup;
    private final float[] _alphaStepLookup;
    private final Color[] _colors;
    private static final float INT_TO_FLOAT_CONST = 1f / 255f;
    private @Nullable CachedContext _cached;

    
    /**
     * Standard constructor which takes the FRACTIONS in values from 0.0f to 1.0f
     * @param center The center of the gradient circle.
     * @param GIVEN_FRACTIONS The fractions of the gradient in values from 0.0f to 1.0f, these should match the COLORS array.
     * @param GIVEN_COLORS The colors of the gradient, these should match the FRACTIONS array.
     * @throws IllegalArgumentException if fractions and colors are not of the same size.
     */
    public ConicalGradientPaint(final Point2D center, final float[] GIVEN_FRACTIONS, final Color[] GIVEN_COLORS) throws IllegalArgumentException {
        this(false, center, 0.0f, GIVEN_FRACTIONS, GIVEN_COLORS);
    }

    /**
     * Enhanced constructor which takes the FRACTIONS in degress from 0.0f to 360.0f and
     * also an GIVEN_OFFSET in degrees around the rotation CENTER
     * @param usesDegrees true if fractions are in degrees, false if fractions are in 0.0 to 1.0
     * @param center the center of the gradient
     * @param offset the offset of the gradient in degrees
     * @param fractions the fractions of the gradient in degrees or 0.0 to 1.0
     * @param colors the colors of the gradient
     * @throws IllegalArgumentException if fractions and colors are not of the same size
     */
    public ConicalGradientPaint(
        final boolean usesDegrees,
        final Point2D center,
        final float   offset,
        final float[] fractions,
        final Color[] colors
    )
    throws IllegalArgumentException
    {
        // Check that fractions and colors are of the same size
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Fractions and colors must be equal in size");
        }

        final java.util.List<Float> fractionList = new java.util.ArrayList<Float>(fractions.length);
        final float finalOffset;
        if (usesDegrees) {
            final float degFraction = 1f / 360f;
            if (Float.compare((offset * degFraction), -0.5f) == 0) {
                finalOffset = -0.5f;
            } else if (Float.compare((offset * degFraction), 0.5f) == 0) {
                finalOffset = 0.5f;
            } else {
                finalOffset = (offset * degFraction);
            }
            for (final float fraction : fractions) {
                fractionList.add((fraction * degFraction));
            }
        } else {
            // Now it seems to work with rotation of 0.5f, below is the old code to correct the problem
//            if (GIVEN_OFFSET == -0.5)
//            {
//                // This is needed because of problems in the creation of the Raster
//                // with a angle offset of exactly -0.5
//                OFFSET = -0.49999f;
//            }
//            else if (GIVEN_OFFSET == 0.5)
//            {
//                // This is needed because of problems in the creation of the Raster
//                // with a angle offset of exactly +0.5
//                OFFSET = 0.499999f;
//            }
//            else
            {
                finalOffset = offset;
            }
            for (final float fraction : fractions) {
                fractionList.add(fraction);
            }
        }

        // Check for valid offset
        if (finalOffset > 0.5f || finalOffset < -0.5f) {
            throw new IllegalArgumentException("Offset has to be in the range of -0.5 to 0.5");
        }

        _angleOffset = offset;

        // Adjust fractions and colors array in the case where startvalue != 0.0f and/or endvalue != 1.0f
        final java.util.List<Color> colorList = new java.util.ArrayList<Color>(colors.length);
        colorList.addAll(java.util.Arrays.asList(colors));

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
        _center = center;
        _colors = colorList.toArray(new Color[colorList.size()]);

        // Prepare lookup table for the angles of each fraction
        final int MAX_FRACTIONS = fractionList.size();
        _fractionOffsets = new float[MAX_FRACTIONS];
        for (int i = 0; i < MAX_FRACTIONS; i++) {
            _fractionOffsets[i] = fractionList.get(i) * 360f;
        }

        // Prepare lookup tables for the color stepsize of each color
        _redStepLookup = new float[_colors.length];
        _greenStepLookup = new float[_colors.length];
        _blueStepLookup = new float[_colors.length];
        _alphaStepLookup = new float[_colors.length];

        for (int i = 0; i < (_colors.length - 1); i++) {
            _redStepLookup[i] = ((_colors[i + 1].getRed() - _colors[i].getRed()) * INT_TO_FLOAT_CONST) / (_fractionOffsets[i + 1] - _fractionOffsets[i]);
            _greenStepLookup[i] = ((_colors[i + 1].getGreen() - _colors[i].getGreen()) * INT_TO_FLOAT_CONST) / (_fractionOffsets[i + 1] - _fractionOffsets[i]);
            _blueStepLookup[i] = ((_colors[i + 1].getBlue() - _colors[i].getBlue()) * INT_TO_FLOAT_CONST) / (_fractionOffsets[i + 1] - _fractionOffsets[i]);
            _alphaStepLookup[i] = ((_colors[i + 1].getAlpha() - _colors[i].getAlpha()) * INT_TO_FLOAT_CONST) / (_fractionOffsets[i + 1] - _fractionOffsets[i]);
        }
    }

    /**
     * With the START_COLOR at the beginning and the DESTINATION_COLOR at the end of the given RANGE the method will calculate
     * and return the color that equals the given VALUE.
     * e.g. a START_COLOR of BLACK (R:0, G:0, B:0, A:255) and a DESTINATION_COLOR of WHITE(R:255, G:255, B:255, A:255)
     * with a given RANGE of 100 and a given VALUE of 50 will return the color that is exactly in the middle of the
     * gradient between black and white which is gray(R:128, G:128, B:128, A:255)
     * So this method is really useful to calculate colors in gradients between two given colors.
     * @param START_COLOR The color at the beginning of the gradient.
     * @param DESTINATION_COLOR The color at the end of the gradient.
     * @param RANGE The range of the gradient.
     * @param VALUE The value in the range of the gradient.
     * @return Color calculated from a range of values by given value
     */
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
    public java.awt.PaintContext createContext(final ColorModel COLOR_MODEL,
                                               final Rectangle DEVICE_BOUNDS,
                                               final Rectangle2D USER_BOUNDS,
                                               final AffineTransform TRANSFORM,
                                               final RenderingHints HINTS) {


        //KK - speed up repaints by caching the context
        if (_cached != null) {
            ConicalGradientPaintContext c = _cached.get(DEVICE_BOUNDS, _center, TRANSFORM);
            if (c != null)
                return c;
        }

        ConicalGradientPaintContext context = new ConicalGradientPaintContext(_center, TRANSFORM);
        _cached = new CachedContext(DEVICE_BOUNDS, _center, TRANSFORM, context);

        return context;
    }

    @Override
    public int getTransparency() {
        return Transparency.TRANSLUCENT;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + _center.hashCode();
        hash = 97 * hash + Float.floatToIntBits(_angleOffset);
        hash = 97 * hash + java.util.Arrays.hashCode(_fractionOffsets);
        hash = 97 * hash + java.util.Arrays.hashCode(_redStepLookup);
        hash = 97 * hash + java.util.Arrays.hashCode(_greenStepLookup);
        hash = 97 * hash + java.util.Arrays.hashCode(_blueStepLookup);
        hash = 97 * hash + java.util.Arrays.hashCode(_alphaStepLookup);
        hash = 97 * hash + java.util.Arrays.hashCode(_colors);
        return hash;
    }

    @Override
    public boolean equals(final Object OBJ) {
        if (this == OBJ) {
            return true;
        }
        if (OBJ == null) {
            return false;
        }
        if (getClass() != OBJ.getClass()) {
            return false;
        }
        final ConicalGradientPaint OTHER = (ConicalGradientPaint) OBJ;
        if (!_center.equals(OTHER._center)) {
            return false;
        }
        if (Float.floatToIntBits(_angleOffset) != Float.floatToIntBits(OTHER._angleOffset)) {
            return false;
        }
        if (!java.util.Arrays.equals(_fractionOffsets, OTHER._fractionOffsets)) {
            return false;
        }
        if (!java.util.Arrays.equals(_redStepLookup, OTHER._redStepLookup)) {
            return false;
        }
        if (!java.util.Arrays.equals(_greenStepLookup, OTHER._greenStepLookup)) {
            return false;
        }
        if (!java.util.Arrays.equals(_blueStepLookup, OTHER._blueStepLookup)) {
            return false;
        }
        if (!java.util.Arrays.equals(_alphaStepLookup, OTHER._alphaStepLookup)) {
            return false;
        }
        if (!java.util.Arrays.equals(_colors, OTHER._colors)) {
            return false;
        }
        return true;
    }

    private final class ConicalGradientPaintContext implements PaintContext
    {
        final private Point2D _center;
        private final HashMap<Long, WritableRaster> _cachedRasters;
        private final AffineTransform _transform;

        public ConicalGradientPaintContext(final Point2D CENTER, AffineTransform transform) {
            _cachedRasters = new HashMap<>();
            _transform = transform;
            try {
                _center = transform.transform(CENTER, null);  //user to device
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void dispose() {
        }

        @Override
        public java.awt.image.ColorModel getColorModel() {
            return ColorModel.getRGBdefault();
        }

        @Override
        public @Nullable Raster getRaster(final int X, final int Y, final int TILE_WIDTH, final int TILE_HEIGHT) {

            try {
                long index = ((long)X << 32) | (long)Y;
                WritableRaster raster = _cachedRasters.get(index);

                if (raster == null)
                    raster = getColorModel().createCompatibleWritableRaster(TILE_WIDTH, TILE_HEIGHT);  // Create raster for given colormodel
                else
                    return raster;

                //get the rotation center in device space
                final double ROTATION_CENTER_X = -X + _center.getX();
                final double ROTATION_CENTER_Y = -Y + _center.getY();

                //Convert to user space
                Point2D rotationCenter = _transform.inverseTransform(new Point2D.Double(ROTATION_CENTER_X, ROTATION_CENTER_Y), null); //device to user


                final int MAX = _fractionOffsets.length - 1;

                // KK: use memory allocated by raster directly
                // Create data array with place for red, green, blue and alpha values
                final int[] data = new int[(TILE_WIDTH * TILE_HEIGHT * 4)];
                //final int[] data = new int[4];

                double dx;
                double dy;
                double angle;
                double currentRed = 0;
                double currentGreen = 0;
                double currentBlue = 0;
                double currentAlpha = 0;

                for (int tileY = 0; tileY < TILE_HEIGHT; tileY++) {
                    for (int tileX = 0; tileX < TILE_WIDTH; tileX++) {

                        Point2D d = _transform.inverseTransform(new Point2D.Double(tileX, tileY), null); //device to user


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

                        angle += _angleOffset;

                        if (angle > 360.0) {
                            angle -= 360.0;
                        } else if (angle < 0.0) {
                            angle += 360.0;
                        }

                        // Check for each angle in fractionAngles array
                        for (int i = 0; i < MAX; i++) {
                            if ((angle >= _fractionOffsets[i])) {
                                currentRed   = _colors[i].getRed() * INT_TO_FLOAT_CONST + (angle - _fractionOffsets[i]) * _redStepLookup[i];
                                currentGreen = _colors[i].getGreen() * INT_TO_FLOAT_CONST + (angle - _fractionOffsets[i]) * _greenStepLookup[i];
                                currentBlue  = _colors[i].getBlue() * INT_TO_FLOAT_CONST + (angle - _fractionOffsets[i]) * _blueStepLookup[i];
                                currentAlpha = _colors[i].getAlpha() * INT_TO_FLOAT_CONST + (angle - _fractionOffsets[i]) * _alphaStepLookup[i];
                            }
                        }

                        // Fill data array with calculated color values
                        final int BASE = (tileY * TILE_WIDTH + tileX) * 4;

                        data[BASE + 0] = (int) Math.round(currentRed   * 255);
                        data[BASE + 1] = (int) Math.round(currentGreen * 255);
                        data[BASE + 2] = (int) Math.round(currentBlue  * 255);
                        data[BASE + 3] = (int) Math.round(currentAlpha * 255);
                        //raster.setPixel(tileX, tileY, data);
                    }
                }

                // Fill the raster with the data
                raster.setPixels(0, 0, TILE_WIDTH, TILE_HEIGHT, data);

                _cachedRasters.put(index, raster);
                return raster;
            }
            catch (NoninvertibleTransformException ex) {
                System.err.println(ex);
                return null;
            }
        }

    }

}
