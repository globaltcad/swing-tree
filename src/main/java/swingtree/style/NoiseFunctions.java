package swingtree.style;

/**
 *  A collection of noise functions that can be used to generate procedural textures.
 *  The functions in this class are also supposed to serve as an example
 *  which demonstrates how to create procedural textures yourself.
 */
public final class NoiseFunctions
{
    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 53982894593057L;

    private static final double[] SEED_BYTE_TO_UNIT_DOUBLE = new double[256];
    static {
        for ( int i = 0; i < 256; i++ )
            SEED_BYTE_TO_UNIT_DOUBLE[i] = i / 255.0;
    }

    private static final int SCRATCH_FAMILIES = 4;
    private static final double[] SCRATCH_ANGLE_SIN = new double[SCRATCH_FAMILIES];
    private static final double[] SCRATCH_ANGLE_COS = new double[SCRATCH_FAMILIES];
    static {
        for ( int i = 0; i < SCRATCH_FAMILIES; i++ ) {
            final double angle = 0.24 + i * 0.9;
            SCRATCH_ANGLE_SIN[i] = Math.sin(angle);
            SCRATCH_ANGLE_COS[i] = Math.cos(angle);
        }
    }

    private static final double HALFTONE_SCREEN_ANGLE = 0.3926990816987241;
    private static final double HALFTONE_SCREEN_SIN = Math.sin(HALFTONE_SCREEN_ANGLE);
    private static final double HALFTONE_SCREEN_COS = Math.cos(HALFTONE_SCREEN_ANGLE);

    private static final double OCTAVE_TURN_SIN = 0.479425538604203;
    private static final double OCTAVE_TURN_COS = 0.8775825618903728;


    private NoiseFunctions(){}


    /**
     *  Stochastic pseudorandom grain produced by summing randomly sampled gradients
     *  within a neighborhood, then squashing the result through a sine wave for a
     *  characteristic speckled look.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the stochastic grain intensity at the given location.
     */
    public static float stochastic( float xIn, float yIn ) {
        int kernelSize = 8;
        double sum = _coordinateToGradValue(kernelSize, xIn, yIn);
        return (float) ((Math.sin(sum * (12.0/kernelSize)) + 1)/2);
    }

    private static double _coordinateToGradValue( int kernelSize, float xIn, float yIn ) {
        final int    maxDistance        = kernelSize / 2;
        final int    baseX              = Math.round( xIn );
        final int    baseY              = Math.round( yIn );
        final double maxDistanceSquared = (double) maxDistance * maxDistance;
        double sum = 0;
        for ( int y = 0; y < kernelSize; y++ ) {
            final int    ry  = ( y - maxDistance ) + baseY;
            final double vy  = ry - yIn;
            final double vy2 = vy * vy;
            if ( vy2 >= maxDistanceSquared )
                continue; // No cell in this row can be near enough to matter.
            for ( int x = 0; x < kernelSize; x++ ) {
                final int rx = ( x - maxDistance ) + baseX;
                if ( _fastPseudoRandomByteSeedFrom( ry, rx ) < 0 )
                    continue; // This cell holds no grain.
                final double vx              = rx - xIn;
                final double distanceSquared = vx * vx + vy2;
                if ( distanceSquared >= maxDistanceSquared )
                    continue; // Relevance would clamp to zero.
                final double relevance = 1.0 - Math.sqrt( distanceSquared ) / maxDistance;
                final double frac      = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                sum += ( frac * (relevance*relevance) );
            }
        }
        return sum;
    }

    /**
     *  Undulating hills and valleys: the stochastic field is modulated with multiple
     *  sine waves to create smooth, flowing topology with natural ridges and crests.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the terrain topology height at the given location.
     */
    public static float smoothTopology( float xIn, float yIn ) {
        float scale = 6;
        return (float) ((Math.sin(stochastic(xIn/scale, yIn/scale) * 6 * Math.PI) + 1)/2);
    }

    public static float hardTopology( float xIn, float yIn ) {
        float scale = 6;
        return (stochastic(xIn/scale, yIn/scale)*6)%1;
    }

    public static float hardSpots( float xIn, float yIn ) {
        float scale = 4;
        return Math.round(stochastic(xIn/scale, yIn/scale));
    }

    public static float smoothSpots( float xIn, float yIn ) {
        float scale = 6;
        int kernelSize = 6;
        double sum = _coordinateToGradValue(kernelSize, xIn/scale, yIn/scale);
        return (float) _sigmoid(sum * 64 / kernelSize);
    }

    public static float grainy( float xIn, float yIn ) {
        float scale = 2;
        int kernelSize = 4;
        double sum = _coordinateToGradValue(kernelSize, xIn/scale, yIn/scale);
        double stochastic = (Math.sin(sum * (12.0/kernelSize)) + 1)/2;
        // We make the smallest and largest values both the largest,
        // and the values around 0.5 become close to 0
        return (float) Math.abs((stochastic-0.5)*2);
    }

    public static float tiles( float xIn, float yIn ) {
        float scale = 10;
        int kernelSize = 8;
        double sum = _coordinateToGradTileValue(kernelSize, xIn/scale, yIn/scale);
        return (float) ((Math.sin(sum * (12.0/kernelSize)) + 1)/2);
    }

    private static double _coordinateToGradTileValue( int kernelSize, float xIn, float yIn ) {
        final int maxDistance  = kernelSize / 2;
        final double sampleRate = 0.5;
        final int[] columns = _roundedKernelLine( kernelSize, xIn );
        final int[] rows    = _roundedKernelLine( kernelSize, yIn );
        double sum = 0;
        for ( int y = 0; y < kernelSize; y++ ) {
            final int ry = rows[y];
            for ( int x = 0; x < kernelSize; x++ ) {
                final int rx = columns[x];
                final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
                final boolean takeSample = (255 * sampleRate -128) < score;
                if ( takeSample ) {
                    final double vx = (rx - xIn);
                    final double vy = (ry - yIn);
                    final double distance = Math.max(vy, vx);
                    final double relevance = Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                    sum += ( frac * (relevance*relevance) );
                }
            }
        }
        return sum;
    }

    public static float fabric( float xIn, float yIn ) {
        float scale = 5;
        int kernelSize = 4;
        double sum = _coordinateToFiberValue(kernelSize, xIn/scale, yIn/scale);
        return (float) ((Math.sin(sum * (12.0/kernelSize)) + 1)/2);
    }

    private static double _coordinateToFiberValue( int kernelSize, float xIn, float yIn ) {
        final int maxDistance   = kernelSize / 2;
        final double sampleRate = 0.5;
        double sum = 0;
        for ( int y = 0; y < kernelSize; y++ ) {
            for ( int x = 0; x < kernelSize; x++ ) {
                final float xi = ( x - maxDistance ) + xIn;
                final float yi = ( y - maxDistance ) + yIn;
                final int rx = Math.round( xi );
                final int ry = Math.round( yi );
                final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
                final boolean takeSample = (255 * sampleRate - 128) < score;
                if ( takeSample ) {
                    final double vx = rx - xIn;
                    final double vy = ry - yIn;
                    final double distance = Math.sqrt( _wrapAround(vx*vx, 2) + _wrapAround(vy*vy, 2) );
                    double relevance = Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                    relevance = Math.min(1, (relevance * relevance) * 1.5);
                    sum += ( frac * relevance );
                }
            }
        }
        return sum;
    }

    public static float retro( float xIn, float yIn ) {
        float scale = 4;
        int kernelSize = 4;
        double sum = _coordinateToRetroValue(kernelSize, xIn/scale, yIn/scale);
        return (float) ((Math.sin(sum) + 1)/2);
    }

    private static double _coordinateToRetroValue( int kernelSize, float xIn, float yIn ) {
        final int maxDistance  = kernelSize / 2;
        final double sampleRate = 0.5;
        double sum = 0;
        for ( int y = 0; y < kernelSize; y++ ) {
            for ( int x = 0; x < kernelSize; x++ ) {
                final float xi = ( x - maxDistance ) + xIn;
                final float yi = ( y - maxDistance ) + yIn;
                final int rx = Math.round( xi );
                final int ry = Math.round( yi );
                final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
                final boolean takeSample = (255 * sampleRate -128) < score;
                if ( takeSample ) {
                    final double vx = rx - xIn;
                    final double vy = ry - yIn;
                    final double distance = Math.sqrt( vx * vx + vy * vy );
                    final double relevance = 1-Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                    sum += ( frac * (relevance*relevance) );
                }
            }
        }
        return sum;
    }

    public static float cells( float xIn, float yIn ) {
        float scale = 4;
        int kernelSize = 6;
        double sum = _coordinateToCellsValue(kernelSize, xIn/scale, yIn/scale);
        return (float) sum;
    }

    private static double _coordinateToCellsValue(int kernelSize, float xIn, float yIn ) {
        final int maxDistance  = kernelSize / 2;
        final double sampleRate = 0.65;
        double grad = 0;
        for ( int y = 0; y < kernelSize; y++ ) {
            for ( int x = 0; x < kernelSize; x++ ) {
                final float xi = ( x - maxDistance ) + xIn;
                final float yi = ( y - maxDistance ) + yIn;
                final int rx = Math.round( xi );
                final int ry = Math.round( yi );
                final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
                final boolean takeSample = (255 * sampleRate -128) < score;
                if ( takeSample ) {
                    final double vx = rx - xIn;
                    final double vy = ry - yIn;
                    final double distance = Math.sqrt( vx * vx + vy * vy );
                    final double relevance = Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fastPseudoRandomDoubleFrom(rx, ry);
                    grad = Math.max( grad, frac * (relevance*relevance) );
                }
            }
        }
        return grad;
    }

    public static float haze(float xIn, float yIn ) {
        float scale = 5;
        int kernelSize = 6;
        double sum = _coordinateToHazeValue(kernelSize, xIn/scale, yIn/scale);
        return  (float) ((Math.sin(sum * (12.0/kernelSize)) + 1)/2);
    }

    private static double _coordinateToHazeValue( int kernelSize, float xIn, float yIn ) {
        final int maxDistance  = kernelSize / 2;
        final double sampleRate = 0.5;
        final int[] columns    = _roundedKernelLine( kernelSize, xIn );
        final int[] rows       = _roundedKernelLine( kernelSize, yIn );
        final int[] subColumns = _roundedKernelLine( kernelSize, xIn, 3 );
        final int[] subRows    = _roundedKernelLine( kernelSize, yIn, 3 );
        double sum = 0;
        for ( int y = 0; y < kernelSize; y++ ) {
            final int ry = rows[y];
            for ( int x = 0; x < kernelSize; x++ ) {
                final int rx = columns[x];
                final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
                final boolean takeSample = (255 * sampleRate -128) < score;
                if ( takeSample ) {
                    final double vx = rx - xIn;
                    final double vy = ry - yIn;
                    final double diagonalMax = Math.max(vx * vx, vy * vy);
                    final double horizontalAndVerticalMax = Math.abs(vx)*Math.abs(vy) * 2;
                    final double distance = Math.sqrt( Math.max(diagonalMax, horizontalAndVerticalMax) * 2 );
                    final double relevance = Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                    final int rx2 = subColumns[x];
                    final int ry2 = subRows[y];
                    final double subNoise = 1 + (_fastPseudoRandomDoubleFrom(rx2, ry2) - 0.5) / 5;
                    sum += ( frac * (relevance*subNoise) );
                }
            }
        }
        return sum;
    }

    public static float spirals(float xIn, float yIn ) {
        float scale = 8;
        int kernelSize = 6;
        double sum = _coordinateToSpiralValue(kernelSize, xIn/scale, yIn/scale);
        return (float) _sigmoid(sum*3);
    }

    private static double _coordinateToSpiralValue(int kernelSize, float xIn, float yIn ) {
        final int maxDistance  = kernelSize / 2;
        final double sampleRate = 0.75;
        final int[] columns = _roundedKernelLine( kernelSize, xIn );
        final int[] rows    = _roundedKernelLine( kernelSize, yIn );
        double result = 0;
        for ( int y = 0; y < kernelSize; y++ ) {
            final int ry = rows[y];
            for ( int x = 0; x < kernelSize; x++ ) {
                final int rx = columns[x];
                final double vx = rx - xIn;
                final double vy = ry - yIn;
                final double reach = vx * vx + vy * vy;
                if ( reach <= maxDistance * maxDistance ) {
                    final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
                    final boolean takeSample = (255 * sampleRate - 128) < score;
                    if ( takeSample ) {
                        final double relevance = 1.0 - Math.sqrt( reach ) / maxDistance;
                        final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                        final double relevance2 = relevance * relevance;
                        // We are calculating the angle between (xIn,yIn) and (rx,ry):
                        final double angle = Math.atan2(vy, vx);
                        int numberOfCones = 1+Math.abs(score)/25;
                        int spiralSign = (Math.abs(score) % 2 == 0 ? 1 : -1);
                        double angleOffset = (frac*Math.PI*numberOfCones+relevance2*6*Math.PI*spiralSign);
                        double conePattern =  (Math.cos(angle*numberOfCones+angleOffset)/2)+0.5;
                        result += ( conePattern * relevance2 ) + frac * relevance2;
                    }
                }
            }
        }
        return result;
    }

    public static float mandelbrot( float xIn, float yIn ) {
        final int    maxIterations           = 32;
        final double bailoutSquared          = 256;
        final double darkestEscapeIterations = 24;
        final double x = xIn / 95.0 - 1.9;
        final double y = yIn / 95.0 - 1.05;
        if ( _isInsideMainCardioidOrBulb(x, y) )
            return 0;
        double ix = 0;
        double iy = 0;
        double magnitudeSquared = 0;
        int iteration = 0;
        while ( magnitudeSquared < bailoutSquared && iteration < maxIterations ) {
            final double nextX = ix * ix - iy * iy + x;
            iy = 2 * ix * iy + y;
            ix = nextX;
            magnitudeSquared = ix * ix + iy * iy;
            iteration++;
        }
        if ( iteration >= maxIterations )
            return 0;
        final double escapeSmoothing = Math.log( Math.log(magnitudeSquared) / 2 ) / Math.log(2);
        final double smoothIteration = Math.max( 1, iteration + 1 - escapeSmoothing );
        return (float) _clamp01( 1 - Math.log(smoothIteration) / Math.log(darkestEscapeIterations) );
    }

    private static boolean _isInsideMainCardioidOrBulb( double x, double y ) {
        final double fromCusp = x - 0.25;
        final double cuspRadiusSquared = fromCusp * fromCusp + y * y;
        if ( cuspRadiusSquared * ( cuspRadiusSquared + fromCusp ) <= 0.25 * y * y )
            return true;
        final double fromBulbCenter = x + 1;
        return fromBulbCenter * fromBulbCenter + y * y <= 0.0625;
    }

    public static float tissue( float xIn, float yIn ) {
        float scale = 32f;
        return _coordinateToWorleyDistanceValue(xIn/scale, yIn/scale);
    }

    private static float _coordinateToWorleyDistanceValue(float xIn, float yIn ) {
        final int minX1 = (int) Math.floor(xIn) - 1 ;
        final int minX2 = (int) Math.floor(xIn)     ;
        final int minX3 = (int) Math.floor(xIn) + 1 ;
        final int minY1 = (int) Math.floor(yIn) - 1 ;
        final int minY2 = (int) Math.floor(yIn)     ;
        final int minY3 = (int) Math.floor(yIn) + 1 ;
        final double centerX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY2);
        final double centerY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX2);
        final double distanceCenter = _distanceBetween(centerX, centerY, xIn, yIn);
        final double leftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY2);
        final double leftY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX1);
        final double distanceLeft = _distanceBetween(leftX, leftY, xIn, yIn);
        final double rightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY2);
        final double rightY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX3);
        final double distanceRight = _distanceBetween(rightX, rightY, xIn, yIn);
        final double topX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY1);
        final double topY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX2);
        final double distanceTop = _distanceBetween(topX, topY, xIn, yIn);
        final double bottomX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY3);
        final double bottomY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX2);
        final double distanceBottom = _distanceBetween(bottomX, bottomY, xIn, yIn);
        final double topLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY1);
        final double topLeftY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX1);
        final double distanceTopLeft = _distanceBetween(topLeftX, topLeftY, xIn, yIn);
        final double topRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY1);
        final double topRightY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX3);
        final double distanceTopRight = _distanceBetween(topRightX, topRightY, xIn, yIn);
        final double bottomLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY3);
        final double bottomLeftY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX1);
        final double distanceBottomLeft = _distanceBetween(bottomLeftX, bottomLeftY, xIn, yIn);
        final double bottomRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY3);
        final double bottomRightY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX3);
        final double distanceBottomRight = _distanceBetween(bottomRightX, bottomRightY, xIn, yIn);
        double min = 1;
        min = Math.min(min, distanceCenter);
        min = Math.min(min, distanceLeft);
        min = Math.min(min, distanceRight);
        min = Math.min(min, distanceTop);
        min = Math.min(min, distanceBottom);
        min = Math.min(min, distanceTopLeft);
        min = Math.min(min, distanceTopRight);
        min = Math.min(min, distanceBottomLeft);
        min = Math.min(min, distanceBottomRight);
        return (float) (1 - min);
    }

    public static float mosaic( float xIn, float yIn ) {
        float scale = 32f;
        return _coordinateToRandomValueFromClosestWorleyCell(xIn/scale, yIn/scale);
    }

    private static float _coordinateToRandomValueFromClosestWorleyCell( float xIn, float yIn ) {
        final int minX1 = (int) Math.floor(xIn) - 1 ;
        final int minX2 = (int) Math.floor(xIn)     ;
        final int minX3 = (int) Math.floor(xIn) + 1 ;
        final int minY1 = (int) Math.floor(yIn) - 1 ;
        final int minY2 = (int) Math.floor(yIn)     ;
        final int minY3 = (int) Math.floor(yIn) + 1 ;

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minDistance = Double.POSITIVE_INFINITY;
        final double centerX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY2);
        final double centerY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX2);
        final double distanceCenter = _distanceBetween(centerX, centerY, xIn, yIn);
        if (distanceCenter < minDistance) {
            minDistance = distanceCenter;
            minX = centerX;
            minY = centerY;
        }
        final double leftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY2);
        final double leftY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX1);
        final double distanceLeft = _distanceBetween(leftX, leftY, xIn, yIn);
        if (distanceLeft < minDistance) {
            minDistance = distanceLeft;
            minX = leftX;
            minY = leftY;
        }
        final double rightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY2);
        final double rightY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX3);
        final double distanceRight = _distanceBetween(rightX, rightY, xIn, yIn);
        if (distanceRight < minDistance) {
            minDistance = distanceRight;
            minX = rightX;
            minY = rightY;
        }
        final double topX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY1);
        final double topY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX2);
        final double distanceTop = _distanceBetween(topX, topY, xIn, yIn);
        if (distanceTop < minDistance) {
            minDistance = distanceTop;
            minX = topX;
            minY = topY;
        }
        final double bottomX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY3);
        final double bottomY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX2);
        final double distanceBottom = _distanceBetween(bottomX, bottomY, xIn, yIn);
        if (distanceBottom < minDistance) {
            minDistance = distanceBottom;
            minX = bottomX;
            minY = bottomY;
        }
        final double topLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY1);
        final double topLeftY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX1);
        final double distanceTopLeft = _distanceBetween(topLeftX, topLeftY, xIn, yIn);
        if (distanceTopLeft < minDistance) {
            minDistance = distanceTopLeft;
            minX = topLeftX;
            minY = topLeftY;
        }
        final double topRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY1);
        final double topRightY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX3);
        final double distanceTopRight = _distanceBetween(topRightX, topRightY, xIn, yIn);
        if (distanceTopRight < minDistance) {
            minDistance = distanceTopRight;
            minX = topRightX;
            minY = topRightY;
        }
        final double bottomLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY3);
        final double bottomLeftY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX1);
        final double distanceBottomLeft = _distanceBetween(bottomLeftX, bottomLeftY, xIn, yIn);
        if (distanceBottomLeft < minDistance) {
            minDistance = distanceBottomLeft;
            minX = bottomLeftX;
            minY = bottomLeftY;
        }
        final double bottomRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY3);
        final double bottomRightY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX3);
        final double distanceBottomRight = _distanceBetween(bottomRightX, bottomRightY, xIn, yIn);
        if (distanceBottomRight < minDistance) {
            minDistance = distanceBottomRight;
            minX = bottomRightX;
            minY = bottomRightY;
        }
        return (float) _fastPseudoRandomDoubleFrom((float) minX, (float) minY);
    }

    public static float gemStones( float xIn, float yIn ) {
        float scale = 32f;
        return _coordinateToClosestWorleyCellEdge(xIn/scale, yIn/scale);
    }

    private static float _coordinateToClosestWorleyCellEdge( float xIn, float yIn ) {
        final int minX1 = (int) Math.floor(xIn) - 1 ;
        final int minX2 = (int) Math.floor(xIn)     ;
        final int minX3 = (int) Math.floor(xIn) + 1 ;
        final int minY1 = (int) Math.floor(yIn) - 1 ;
        final int minY2 = (int) Math.floor(yIn)     ;
        final int minY3 = (int) Math.floor(yIn) + 1 ;

        double closestX = Double.POSITIVE_INFINITY;
        double closestY = Double.POSITIVE_INFINITY;
        double secondClosestX = Double.POSITIVE_INFINITY;
        double secondClosestY = Double.POSITIVE_INFINITY;
        double minDistance1 = Double.POSITIVE_INFINITY;
        double minDistance2 = Double.POSITIVE_INFINITY;

        final double centerX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY2);
        final double centerY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX2);
        final double distanceCenter = _distanceBetween(centerX, centerY, xIn, yIn);
        if (distanceCenter < minDistance1 || distanceCenter < minDistance2) {
            if (distanceCenter < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceCenter;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = centerX;
                closestY = centerY;
            } else {
                minDistance2 = distanceCenter;
                secondClosestX = centerX;
                secondClosestY = centerY;
            }
        }
        final double leftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY2);
        final double leftY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX1);
        final double distanceLeft = _distanceBetween(leftX, leftY, xIn, yIn);
        if (distanceLeft < minDistance1 || distanceLeft < minDistance2) {
            if (distanceLeft < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceLeft;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = leftX;
                closestY = leftY;
            } else {
                minDistance2 = distanceLeft;
                secondClosestX = leftX;
                secondClosestY = leftY;
            }
        }
        final double rightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY2);
        final double rightY = minY2 + _fastPseudoRandomDoubleFrom(minY2, -minX3);
        final double distanceRight = _distanceBetween(rightX, rightY, xIn, yIn);
        if (distanceRight < minDistance1 || distanceRight < minDistance2) {
            if (distanceRight < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceRight;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = rightX;
                closestY = rightY;
            } else {
                minDistance2 = distanceRight;
                secondClosestX = rightX;
                secondClosestY = rightY;
            }
        }
        final double topX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY1);
        final double topY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX2);
        final double distanceTop = _distanceBetween(topX, topY, xIn, yIn);
        if (distanceTop < minDistance1 || distanceTop < minDistance2) {
            if (distanceTop < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceTop;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = topX;
                closestY = topY;
            } else {
                minDistance2 = distanceTop;
                secondClosestX = topX;
                secondClosestY = topY;
            }
        }
        final double bottomX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY3);
        final double bottomY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX2);
        final double distanceBottom = _distanceBetween(bottomX, bottomY, xIn, yIn);
        if (distanceBottom < minDistance1 || distanceBottom < minDistance2) {
            if (distanceBottom < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceBottom;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = bottomX;
                closestY = bottomY;
            } else {
                minDistance2 = distanceBottom;
                secondClosestX = bottomX;
                secondClosestY = bottomY;
            }
        }
        final double topLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY1);
        final double topLeftY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX1);
        final double distanceTopLeft = _distanceBetween(topLeftX, topLeftY, xIn, yIn);
        if (distanceTopLeft < minDistance1 || distanceTopLeft < minDistance2) {
            if (distanceTopLeft < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceTopLeft;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = topLeftX;
                closestY = topLeftY;
            } else {
                minDistance2 = distanceTopLeft;
                secondClosestX = topLeftX;
                secondClosestY = topLeftY;
            }
        }
        final double topRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY1);
        final double topRightY = minY1 + _fastPseudoRandomDoubleFrom(minY1, -minX3);
        final double distanceTopRight = _distanceBetween(topRightX, topRightY, xIn, yIn);
        if (distanceTopRight < minDistance1 || distanceTopRight < minDistance2) {
            if (distanceTopRight < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceTopRight;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = topRightX;
                closestY = topRightY;
            } else {
                minDistance2 = distanceTopRight;
                secondClosestX = topRightX;
                secondClosestY = topRightY;
            }
        }
        final double bottomLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY3);
        final double bottomLeftY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX1);
        final double distanceBottomLeft = _distanceBetween(bottomLeftX, bottomLeftY, xIn, yIn);
        if (distanceBottomLeft < minDistance1 || distanceBottomLeft < minDistance2) {
            if (distanceBottomLeft < minDistance1) {
                minDistance2 = minDistance1;
                minDistance1 = distanceBottomLeft;
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = bottomLeftX;
                closestY = bottomLeftY;
            } else {
                minDistance2 = distanceBottomLeft;
                secondClosestX = bottomLeftX;
                secondClosestY = bottomLeftY;
            }
        }
        final double bottomRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY3);
        final double bottomRightY = minY3 + _fastPseudoRandomDoubleFrom(minY3, -minX3);
        final double distanceBottomRight = _distanceBetween(bottomRightX, bottomRightY, xIn, yIn);
        if (distanceBottomRight < minDistance1 || distanceBottomRight < minDistance2) {
            if (distanceBottomRight < minDistance1) {
                secondClosestX = closestX;
                secondClosestY = closestY;
                closestX = bottomRightX;
                closestY = bottomRightY;
            } else {
                secondClosestX = bottomRightX;
                secondClosestY = bottomRightY;
            }
        }
        final double alongCenterLine = _relativePositionOnLine(closestX, closestY, secondClosestX, secondClosestY, xIn, yIn);
        final double betweenCenters   = _distanceBetween(closestX, closestY, secondClosestX, secondClosestY);
        final double beyondTheEdge    = ((0.5-alongCenterLine)*Math.pow(betweenCenters, 2));
        return (float) Math.max(0, Math.min(1, Math.sqrt(beyondTheEdge)));
    }

    private static double _relativePositionOnLine(double x1, double y1, double x2, double y2, double px, double py) {
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double lengthSquared = (dx * dx + dy * dy);

        if (lengthSquared == 0) {
            throw new IllegalArgumentException("The two points defining the line must not be the same.");
        }

        return ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
    }


    private static double _distanceBetween( double x1, double y1, double x2, double y2 ) {
        return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );
    }

    public static float pondInDrizzle( float xIn, float yIn ) {
        float scale = 0.5f/32;
        double pool = _voronoiBasedWavesSum(xIn*scale, yIn*scale);
        return (float) _wave(Math.pow(Math.abs(pool), 2));
    }

    public static float pondInRain( float xIn, float yIn ) {
        float scale = 0.5f/32;
        double pool = _voronoiBasedWavesSum(xIn*scale, yIn*scale)*1.5;
        pool += _voronoiBasedWavesSum(yIn*scale*2, -xIn*scale*2)/1.5;
        return (float) _wave(Math.abs(pool*1.5));
    }

    public static float pondOfStrings( float xIn, float yIn ) {
        float scale = 0.5f/32;
        double pool = _voronoiBasedWavesSum(xIn*scale, yIn*scale);
        return (float) _wave(Math.pow(Math.abs(pool*4), 0.5));
    }

    public static float pondOfTangledStrings( float xIn, float yIn ) {
        float scale = 0.5f/32;
        double pool = _voronoiBasedWavesSum(xIn*scale, yIn*scale)*1.5;
        pool += _voronoiBasedWavesSum(yIn*scale*2, -xIn*scale*2)/1.5;
        return (float) _wave(Math.pow(Math.abs(pool*3), 0.5));
    }

    /*
        ~~~ A few more procedural textures, built on the smooth value-noise toolkit below. ~~~
    */

    /**
     *  A turbulent marble texture: a regular striped pattern is distorted by several
     *  octaves of value noise, bending the stripes into organic, swirling veins.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the marble texture intensity at the given location.
     */
    public static float marble( float xIn, float yIn ) {
        final float scale = 28;
        final double x = xIn / scale;
        final double y = yIn / scale;
        final double turbulence = ( _fractalNoise(x, y, 5) - 0.5 ) * 2;
        final double pattern = Math.sin( ( x + y ) * Math.PI + turbulence * 5 );
        // 'abs' puts a sharp valley at every zero-crossing, 'pow' thins it into a vein:
        return (float) Math.pow( Math.abs( pattern ), 0.35 );
    }

    /**
     *  Concentric, slightly distorted growth rings reminiscent of a cross-cut piece
     *  of timber. The rings are warped by fractal noise to give them a natural grain.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the wood grain intensity at the given location.
     */
    public static float wood( float xIn, float yIn ) {
        final float scale = 48;
        final double x = xIn / scale;
        final double y = yIn / scale;
        final double distortion = _fractalNoise(x, y, 4) - 0.5;
        final double rings = Math.sqrt( x * x + y * y ) + distortion * 1.5;
        final double grain = ( rings * 5 ) % 1.0;
        return (float) ( ( Math.sin( grain * Math.PI ) + 1 ) / 2 );
    }

    /**
     *  A smooth, flowing interference pattern built from a handful of summed sine
     *  waves - the classic "plasma" demo effect, great for vivid color gradients.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the plasma intensity at the given location.
     */
    public static float plasma( float xIn, float yIn ) {
        final double scale = 36;
        final double x = xIn / scale;
        final double y = yIn / scale;
        double v = Math.sin( x );
        v += Math.sin( y / 0.9 );
        v += Math.sin( ( x + y ) / 1.7 );
        final double cx = x + 0.5 * Math.sin( x / 3.0 );
        final double cy = y + 0.5 * Math.cos( y / 2.0 );
        v += Math.sin( Math.sqrt( cx * cx + cy * cy + 1 ) );
        return (float) ( ( Math.sin( v * Math.PI / 2 ) + 1 ) / 2 );
    }

    /**
     *  Soft, billowing clouds produced by fractal Brownian motion and a sigmoid
     *  contrast curve which crisps the cloud edges up against the open sky.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the cloud density at the given location.
     */
    public static float clouds( float xIn, float yIn ) {
        final float scale = 64;
        final double density = _fractalNoise(xIn / scale, yIn / scale, 6);
        return (float) _sigmoid( ( density - 0.5 ) * 7 );
    }

    /**
     *  A network of thin cracks separating irregular plates, computed from the
     *  difference between the two closest Worley (Voronoi) feature points.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the crack pattern intensity at the given location.
     */
    public static float cracks( float xIn, float yIn ) {
        final float scale = 28;
        final double edge = _worleyEdgeGap(xIn / scale, yIn / scale);
        return (float) _sigmoid( ( edge - 0.06 ) * 30 );
    }

    /**
     *  A swirling vortex created by rotating the sampling angle as a function of
     *  the radius and an underlying fractal noise field.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the vortex intensity at the given location.
     */
    public static float vortex( float xIn, float yIn ) {
        final float scale = 40;
        final double x = xIn / scale;
        final double y = yIn / scale;
        final double radius = Math.sqrt( x * x + y * y );
        final double angle = Math.atan2( y, x ) + radius * 0.8 + _fractalNoise(x, y, 4) * 3;
        final double swirl = Math.sin( angle * 3 + radius * 2 );
        return (float) ( ( swirl + 1 ) / 2 );
    }

    /**
     *  A fluid, organic flow field produced by "domain warping": fractal noise is
     *  sampled at coordinates that are themselves displaced by other fractal noise.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the flow field intensity at the given location.
     */
    public static float flow( float xIn, float yIn ) {
        final float scale = 56;
        final double x = xIn / scale;
        final double y = yIn / scale;
        final double warpX = _fractalNoise(x, y, 4);
        final double warpY = _fractalNoise(x + 5.2, y + 1.3, 4);
        final double warped = _fractalNoise(x + 4 * warpX, y + 4 * warpY, 5);
        return (float) _clamp01(warped);
    }

    /**
     *  Crackling electric arcs. A fractal noise field is traced along the contour
     *  where it crosses its mid value - that contour naturally branches and loops -
     *  while a jagged domain warp makes the arcs zig-zag like a real discharge.
     *  <p>
     *  The contour is rendered as a uniformly thin bolt by dividing the distance to
     *  the mid value by the local gradient: {@code |field - 0.5| / |gradient|} is an
     *  estimate of the true distance to the contour, so the bolt keeps the same
     *  width regardless of how steep the field is (no fat blobs on flat spots).
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the lightning bolt intensity at the given location.
     */
    public static float lightning( float xIn, float yIn ) {
        final float scale = 110;
        double x = xIn / scale;
        double y = yIn / scale;
        // Jagged domain warp so the bolts fork and zig-zag instead of curving smoothly:
        final double warpX = _fractalNoise(x + 1.7, y - 3.1, 4) - 0.5;
        final double warpY = _fractalNoise(x - 4.3, y + 2.9, 4) - 0.5;
        x += warpX * 1.6;
        y += warpY * 1.6;

        final int octaves = 3;
        final double eps = 0.012;
        final double field = _fractalNoise(x, y, octaves);
        // Central-difference gradient of the field, used to normalize the bolt width:
        final double dx = _fractalNoise(x + eps, y, octaves) - _fractalNoise(x - eps, y, octaves);
        final double dy = _fractalNoise(x, y + eps, octaves) - _fractalNoise(x, y - eps, octaves);
        final double gradient = Math.sqrt( dx * dx + dy * dy ) / ( 2 * eps ) + 1e-3;

        final double distance = Math.abs( field - 0.5 ) / gradient; // ~distance to the contour
        final double bolt = Math.exp( -distance * 24 ); // razor-thin glowing filament
        final double glow = Math.exp( -distance *  5 ) * 0.25; // soft halo around it
        return (float) _clamp01( bolt + glow );
    }

    /*
     *  The shape parameters of a single {@link #foliage} leaf. They are named rather than
     *  inlined because {@link #_isOutsideLeafBounds} derives a bounding circle from them, and
     *  that derivation is only sound as long as it sees the same numbers the shape does. A leaf
     *  reshaped by editing a literal in place would silently start being clipped.
     */
    private static final double LEAF_MIN_HALF_LENGTH    = 0.45;
    private static final double LEAF_HALF_LENGTH_SPREAD = 0.6;  // so the half length is 0.45 .. 1.05
    private static final double LEAF_MIN_ASPECT         = 0.30;
    private static final double LEAF_ASPECT_SPREAD      = 0.16; // so the aspect is 0.30 .. 0.46
    private static final double LEAF_MAX_ASYMMETRY      = 0.6;
    private static final double LEAF_WAVE_AMPLITUDE     = 0.18;
    private static final double LEAF_MAX_SPINE_BOW      = 0.35;
    /** The widest a leaf can get, relative to its half length - the first of the two bounds
     *  {@link #_isOutsideLeafBounds} is derived from, see there. */
    private static final double LEAF_MAX_HALF_WIDTH_PER_HALF_LENGTH =
            ( LEAF_MIN_ASPECT + LEAF_ASPECT_SPREAD ) * ( 1 + LEAF_MAX_ASYMMETRY ) * ( 1 + LEAF_WAVE_AMPLITUDE );

    /**
     *  Whether the pixel offset {@code (dx, dy)} from a leaf's center lies outside the smallest
     *  circle that is guaranteed to contain that leaf - in which case the coverage tests further
     *  down are certain to reject it, and none of the work leading up to them has to happen. <br>
     *  <br>
     *  This is what makes {@link #foliage} affordable. A pixel scans a 5x5 neighbourhood of leaf
     *  cells, but a leaf is roughly one cell across, so all but a handful of those 25 candidates
     *  are nowhere near the pixel - and each of them was paying for five pseudo randoms, a
     *  {@link Math#sin} and a {@link Math#cos} before being discarded on distance grounds anyway.
     *  <br>
     *  <b>The test is exact, not an approximation:</b> it rejects a strict superset of what the
     *  coverage tests reject, so the rendered pixels are unchanged. In the leaf's own frame
     *  coverage requires {@code |u| < halfLength} and {@code |v - spineV| < halfWidth}, and since
     *  the frame is a pure rotation, {@code dx² + dy² == u² + v²}. Bounding the two:
     *  <ul>
     *      <li>{@code halfWidth = aspect * halfLength * profile * wave}, where
     *          {@code aspect <= LEAF_MIN_ASPECT + LEAF_ASPECT_SPREAD},
     *          {@code wave <= 1 + LEAF_WAVE_AMPLITUDE} and
     *          {@code profile = max(0, (1-t²)(1 - asym*t)) <= 1 + LEAF_MAX_ASYMMETRY} for
     *          {@code |t| < 1}.</li>
     *      <li>{@code |spineV| = |curve| * (1-t²) <= LEAF_MAX_SPINE_BOW}.</li>
     *  </ul>
     *  so {@code |v| <= halfWidth + |spineV| <= LEAF_MAX_HALF_WIDTH_PER_HALF_LENGTH * halfLength
     *  + LEAF_MAX_SPINE_BOW}, and the radius below follows. The bounds are deliberately the
     *  obvious analytic ones rather than the tightest numeric ones: a tighter radius rejects a
     *  little more, but it would have to be re-derived by hand every time a leaf is reshaped.
     */
    private static boolean _isOutsideLeafBounds( double dx, double dy, double halfLength ) {
        final double acrossReach = LEAF_MAX_HALF_WIDTH_PER_HALF_LENGTH * halfLength + LEAF_MAX_SPINE_BOW;
        return dx * dx + dy * dy >= halfLength * halfLength + acrossReach * acrossReach;
    }

    /**
     *  A leafy foliage texture. Leaves are scattered from a jittered grid - jittered
     *  far enough that the underlying grid disappears - and layered by a random depth
     *  so they overlap naturally. To avoid a sterile, too-perfect look, every leaf is
     *  individually irregular: its spine bends like a banana, its outline is
     *  asymmetric (rounded toward the base, drawn to a point at the tip) with a wavy
     *  edge, and its surface is broken up by value-noise mottling. Each leaf carries a
     *  lit midrib and faint herringbone side veins, and leaves further back are shaded
     *  darker for depth.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the foliage texture intensity at the given location.
     */
    public static float foliage( float xIn, float yIn ) {
        final float scale = 72;
        final double x = xIn / scale;
        final double y = yIn / scale;
        final int cellX = (int) Math.floor(x);
        final int cellY = (int) Math.floor(y);

        double bestZ = -1;
        double value = 0.13 + ( _valueNoise(x * 6, y * 6) - 0.5 ) * 0.07; // mottled shade in the gaps

        // Leaves are jittered well beyond their own cell, so a wide neighbourhood is scanned:
        for ( int oy = -2; oy <= 2; oy++ ) {
            for ( int ox = -2; ox <= 2; ox++ ) {
                final int gx = cellX + ox;
                final int gy = cellY + oy;
                final double z = _fastPseudoRandomDoubleFrom( gx + 7919, gy + 104729 );
                if ( z <= bestZ )
                    continue; // a leaf nearer to the viewer already won this pixel

                final double leafX = gx + 0.5 + ( _fastPseudoRandomDoubleFrom( gx, gy ) - 0.5 ) * 1.4;
                final double leafY = gy + 0.5 + ( _fastPseudoRandomDoubleFrom( gy, -gx ) - 0.5 ) * 1.4;
                final double dx = x - leafX;
                final double dy = y - leafY;

                if ( _isOutsideLeafBounds(dx, dy, LEAF_MIN_HALF_LENGTH + LEAF_HALF_LENGTH_SPREAD) )
                    continue; // Out of reach of even the largest possible leaf.

                final double halfLength = LEAF_MIN_HALF_LENGTH
                                        + _fastPseudoRandomDoubleFrom( gx - 1597, gy - 2749 ) * LEAF_HALF_LENGTH_SPREAD;

                if ( _isOutsideLeafBounds(dx, dy, halfLength) )
                    continue; // Cannot possibly be covered - and this is the common case, see below.

                final double angle = _fastPseudoRandomDoubleFrom( gx + 101, gy - 57 ) * 2 * Math.PI;

                // Rotate the offset into the leaf's own frame (u = along, v = across):
                final double sin = Math.sin(angle);
                final double cos = Math.cos(angle);
                final double u = dx * cos - dy * sin;
                final double v = dx * sin + dy * cos;

                if ( Math.abs(u) >= halfLength )
                    continue;
                final double t = u / halfLength; // -1 at the base, +1 at the tip

                // The spine bows like a banana, so the leaf is not a rigid symmetric lens:
                final double curve  = ( _fastPseudoRandomDoubleFrom( gx + 53, gy + 877 ) - 0.5 ) * 2 * LEAF_MAX_SPINE_BOW;
                final double spineV = curve * ( 1 - t * t );

                // Outline: a lens skewed toward the base, with a per-leaf wavy edge:
                final double asym    = _fastPseudoRandomDoubleFrom( gx - 71, gy + 311 ) * LEAF_MAX_ASYMMETRY;
                final double aspect  = LEAF_MIN_ASPECT + _fastPseudoRandomDoubleFrom( gx + 211, gy - 19 ) * LEAF_ASPECT_SPREAD;
                final double wave    = 1 + LEAF_WAVE_AMPLITUDE * Math.sin( u * ( 7 + 7 * asym ) + angle * 3 );
                final double profile = Math.max( 0, ( 1 - t * t ) * ( 1 - asym * t ) );
                final double halfWidth = aspect * halfLength * profile * wave;

                final double vRel = v - spineV;
                if ( halfWidth <= 0 || Math.abs(vRel) >= halfWidth )
                    continue;

                // This leaf both covers the pixel and sits on top, so it wins:
                bestZ = z;
                final double rim    = Math.abs(vRel) / halfWidth;       // 0 at the spine .. 1 at the edge
                final double midrib = Math.exp( -(vRel * vRel) / 0.0016 ); // glowing central vein
                final double side   = Math.pow( Math.max( 0, Math.sin( u * 9 - Math.abs(vRel) * 16 ) ), 8 );
                final double bright = _fastPseudoRandomDoubleFrom( gx - 313, gy + 191 );
                final double mottle = _valueNoise( x * 10 + gx * 7.0, y * 10 + gy * 7.0 ) - 0.5;

                double shade = 0.40 + bright * 0.42; // every leaf gets its own green tone
                shade += ( 1 - t ) * 0.10;           // a touch lighter toward the base
                shade -= rim * rim * 0.34;           // darker rim gives the leaves depth
                shade += midrib * 0.24;              // the midrib catches the light
                shade += side * ( 1 - rim ) * 0.11;  // faint herringbone side veins
                shade += mottle * 0.15;              // organic blotchy surface variation
                shade -= ( 1 - z ) * 0.14;           // leaves further back sit in shadow
                value = _clamp01( shade );
            }
        }
        return (float) value;
    }


    /**
     *  Plain fractal Brownian motion: several octaves of value noise summed with
     *  halving amplitude. The neutral, all purpose cloud field other looks are built
     *  from, and the one to reach for when a background just needs to stop being flat.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the fractal noise intensity at the given location.
     */
    public static float fractal( float xIn, float yIn ) {
        double field = _fractalNoise( xIn / 46.0, yIn / 46.0, 6 );
        return (float) _clamp01( 0.5 + ( field - 0.5 ) * 1.9 );
    }

    /**
     *  Wispy, veined noise, made by creasing the noise field at every zero crossing
     *  instead of rounding it off. Reads as smoke, steam or weathered stone.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the turbulent noise intensity at the given location.
     */
    public static float turbulence( float xIn, float yIn ) {
        return (float) _clamp01( _turbulentNoise( xIn / 62.0, yIn / 62.0, 6 ) * 1.32 );
    }

    /**
     *  A branching network of sharp crests separated by smooth valleys, the way a
     *  mountain range looks from above. The opposite character to {@link #clouds}.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the ridge pattern intensity at the given location.
     */
    public static float ridges( float xIn, float yIn ) {
        return (float) _clamp01( _ridgedNoise( xIn / 90.0, yIn / 90.0, 6 ) * 1.15 );
    }

    /**
     *  Finely brushed metal: streaks stretched far along the horizontal axis at three
     *  different frequencies, under a broad, soft sheen.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the brushed metal texture intensity at the given location.
     */
    public static float brushedMetal( float xIn, float yIn ) {
        double fineStreaks  = _valueNoise( xIn / 50.0,  yIn * 1.15 );
        double midStreaks   = _valueNoise( xIn / 115.0, yIn * 0.44 );
        double broadStreaks = _valueNoise( xIn / 230.0, yIn * 0.17 );
        double sheen        = _fractalNoise( xIn / 330.0, yIn / 200.0, 3 );
        double shade = 0.5 + ( fineStreaks  - 0.5 ) * 0.34
                           + ( midStreaks   - 0.5 ) * 0.32
                           + ( broadStreaks - 0.5 ) * 0.28
                           + ( sheen        - 0.5 ) * 0.62;
        return (float) _clamp01( shade );
    }

    /**
     *  A worn metal surface, scuffed by sparse straight scratches running at several
     *  angles. Each scratch fades in and out along its length instead of crossing the
     *  whole surface.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the scratch pattern intensity at the given location.
     */
    public static float scratches( float xIn, float yIn ) {
        double marks = 0;
        for ( int i = 0; i < SCRATCH_FAMILIES; i++ ) {
            double sin = SCRATCH_ANGLE_SIN[i];
            double cos = SCRATCH_ANGLE_COS[i];
            double along  = ( xIn * cos - yIn * sin ) / 190.0 + i * 31.7;
            double across = ( xIn * sin + yIn * cos ) / 1.9 + i * 57.3;
            double interrupted = _valueNoise( along * 9.0 + i * 13.9, across * 0.31 );
            double visible = _smoothStep( _clamp01( ( interrupted - 0.5 ) * 4.5 ) );
            if ( visible > 0 ) {
                double crest = _valueNoise( along, across );
                double crestSquared = crest * crest;
                double crestToTheFourth = crestSquared * crestSquared;
                double line = crestToTheFourth * crestToTheFourth * crestToTheFourth * crestSquared;
                marks = Math.max( marks, line * visible );
            }
        }
        double patina = _fractalNoise( xIn / 70.0, yIn / 70.0, 3 );
        double dust   = _valueNoise( xIn * 1.7, yIn * 1.7 );
        return (float) _clamp01( 0.34 + ( patina - 0.5 ) * 0.30 + ( dust - 0.5 ) * 0.10 + marks * 1.5 );
    }

    /**
     *  Poured concrete: broad cement mottling, a fine sandy grit and scattered air pockets
     *  whose positions are pushed off their lattice so they do not fall into rows.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the concrete texture intensity at the given location.
     */
    public static float concrete( float xIn, float yIn ) {
        double mottling = _fractalNoise( xIn / 30.0, yIn / 30.0, 5 );
        double grit     = _valueNoise( xIn * 1.4, yIn * 1.4 );
        double pitDriftX = _fractalNoise( xIn / 19.0 + 4.7, yIn / 19.0 - 9.1, 2 ) - 0.5;
        double pitDriftY = _fractalNoise( xIn / 19.0 - 6.3, yIn / 19.0 + 2.5, 2 ) - 0.5;
        double toNearestPit = _worleyNearestDistance( xIn / 11.0 + pitDriftX * 0.8, yIn / 11.0 + pitDriftY * 0.8 );
        double pits         = _smoothStep( _clamp01( toNearestPit * 2.4 ) );
        return (float) _clamp01( 0.30 + mottling * 0.62 + ( grit - 0.5 ) * 0.28 - ( 1 - pits ) * 0.35 );
    }

    /**
     *  Uncoated paper: crossed short fibres, scattered flecks and a faint unevenness in
     *  the pulp. Subtle enough to sit under text.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the paper texture intensity at the given location.
     */
    public static float paper( float xIn, float yIn ) {
        double fibresAcross = _valueNoise( xIn * 1.9, yIn * 0.30 );
        double fibresDown   = _valueNoise( xIn * 0.30, yIn * 1.9 );
        double flecks       = _valueNoise( xIn * 3.7, yIn * 3.7 );
        double blotches     = _fractalNoise( xIn / 55.0, yIn / 55.0, 4 );
        double shade = 0.5 + ( fibresAcross - 0.5 ) * 0.45
                           + ( fibresDown   - 0.5 ) * 0.45
                           + ( flecks       - 0.5 ) * 0.30
                           + ( blotches     - 0.5 ) * 0.55;
        return (float) _clamp01( shade );
    }

    /**
     *  Wind blown sand: long ripples curving with the wind direction, over a fine grain
     *  and slow rises and dips of the dunes underneath.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the sand texture intensity at the given location.
     */
    public static float sand( float xIn, float yIn ) {
        double windDrift = _fractalNoise( xIn / 150.0, yIn / 150.0, 3 ) - 0.5;
        double ripples   = Math.sin( xIn * 0.105 + yIn * 0.036 + windDrift * 11 );
        double fineGrain = _valueNoise( xIn * 3.3, yIn * 3.3 );
        double dunes     = _valueNoise( xIn / 55.0, yIn / 55.0 );
        return (float) _clamp01( 0.48 + ripples * 0.23 + ( fineGrain - 0.5 ) * 0.36 + ( dunes - 0.5 ) * 0.34 );
    }

    /**
     *  Full grain leather: a network of soft creases enclosing rounded pebbles, with a
     *  fine grain over the top.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the leather texture intensity at the given location.
     */
    public static float leather( float xIn, float yIn ) {
        double pebbleEdge = _worleyEdgeGap( xIn / 15.0, yIn / 15.0 );
        double creases = _smoothStep( _clamp01( pebbleEdge * 2.6 ) );
        double grain   = _valueNoise( xIn * 1.7, yIn * 1.7 );
        double bloom   = _fractalNoise( xIn / 60.0, yIn / 60.0, 3 );
        return (float) _clamp01( 0.16 + creases * 0.62 + ( grain - 0.5 ) * 0.24 + ( bloom - 0.5 ) * 0.3 );
    }

    /**
     *  Denim: the diagonal ribs of a twill weave, crossed by warp and weft threads, with
     *  slubs in the yarn and gentle fading across the cloth.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the denim texture intensity at the given location.
     */
    public static float denim( float xIn, float yIn ) {
        double twillPhase = ( xIn * 0.5 + yIn ) * 0.36;
        double twill = Math.sin( twillPhase + _valueNoise( xIn * 0.09, yIn * 0.09 ) * 1.6 );
        double warpThreads = _valueNoise( xIn * 0.85, yIn * 0.16 );
        double weftThreads = _valueNoise( xIn * 0.16, yIn * 0.85 );
        double slub = _valueNoise( xIn * 2.4, yIn * 2.4 );
        double wear = _fractalNoise( xIn / 80.0, yIn / 80.0, 3 );
        return (float) _clamp01( 0.44 + twill * 0.20
                                      + ( warpThreads - 0.5 ) * 0.26
                                      + ( weftThreads - 0.5 ) * 0.18
                                      + ( slub - 0.5 ) * 0.16
                                      + ( wear - 0.5 ) * 0.34 );
    }

    /**
     *  A brick wall in running bond, every course offset by half a brick. Each brick
     *  carries its own fired tone and the mortar joints sit between them.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the brick texture intensity at the given location.
     */
    public static float bricks( float xIn, float yIn ) {
        final double courseHeight = 24;
        final double brickLength  = 58;
        final double jointWidth   = 3.5;
        double course = Math.floor( yIn / courseHeight );
        double shifted = xIn + ( _wrapAround( course, 2 ) < 1 ? 0 : brickLength / 2 );
        double column = Math.floor( shifted / brickLength );
        double alongBrick  = shifted - column * brickLength;
        double acrossBrick = yIn - course * courseHeight;
        double toJoint = Math.min(
                Math.min( alongBrick, brickLength - alongBrick ),
                Math.min( acrossBrick, courseHeight - acrossBrick )
            );
        double face = _smoothStep( _clamp01( ( toJoint - jointWidth ) / 2.5 ) );
        double tone = _fastPseudoRandomDoubleFrom( (float) column, (float) course );
        double grain = _valueNoise( xIn * 0.9, yIn * 0.9 );
        return (float) _clamp01( face * ( 0.42 + tone * 0.5 + ( grain - 0.5 ) * 0.22 ) );
    }

    /**
     *  Herringbone parquet: planks laid at right angles in a zig zag, each with its own
     *  tone and a grain running along its length.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the herringbone texture intensity at the given location.
     */
    public static float herringbone( float xIn, float yIn ) {
        final double plankLength = 60;
        final double plankWidth  = 20;
        double u = ( xIn + yIn ) / 1.4142136;
        double v = ( yIn - xIn ) / 1.4142136;
        int blockU = (int) Math.floor( u / plankLength );
        int blockV = (int) Math.floor( v / plankLength );
        boolean lyingAlong = ( ( blockU + blockV ) & 1 ) == 0;
        double across = lyingAlong ? v : u;
        double along  = lyingAlong ? u : v;
        double inPlank = _wrapAround( across, plankWidth ) / plankWidth;
        double alongPlank = _wrapAround( along, plankLength );
        double toEnd = Math.min( alongPlank, plankLength - alongPlank );
        double joint = _smoothStep( _clamp01( ( Math.min( inPlank, 1 - inPlank ) * plankWidth - 1.2 ) / 1.6 ) )
                     * _smoothStep( _clamp01( ( toEnd - 1.2 ) / 1.6 ) );
        double grain = _valueNoise( along * 0.25, across * 3.0 );
        double tone  = _fastPseudoRandomDoubleFrom( (float) blockU, (float) blockV );
        return (float) _clamp01( joint * ( 0.35 + tone * 0.35 + grain * 0.4 ) );
    }

    /**
     *  A honeycomb of tight packed hexagonal cells, each shaded like a shallow dome and
     *  separated by dark walls.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the honeycomb texture intensity at the given location.
     */
    public static float honeycomb( float xIn, float yIn ) {
        final double combWidth = 30;
        final double rowPitch = 1.7320508075688772;
        double x = xIn / combWidth;
        double y = yIn / combWidth;
        double ax = _wrapAround( x, 1.0 ) - 0.5;
        double ay = _wrapAround( y, rowPitch ) - rowPitch / 2;
        double bx = _wrapAround( x - 0.5, 1.0 ) - 0.5;
        double by = _wrapAround( y - rowPitch / 2, rowPitch ) - rowPitch / 2;
        double gx;
        double gy;
        if ( ax * ax + ay * ay < bx * bx + by * by ) { gx = ax; gy = ay; } else { gx = bx; gy = by; }
        double toEdge = Math.max( Math.abs(gx), Math.abs(gx) * 0.5 + Math.abs(gy) * 0.8660254037844386 );
        double wall = _smoothStep( _clamp01( ( 0.5 - toEdge ) * 9 ) );
        double depth = _valueNoise( xIn / 45.0, yIn / 45.0 );
        return (float) _clamp01( wall * ( 0.55 + depth * 0.45 ) );
    }

    /**
     *  Plain woven cloth: strands passing over and under one another, rounded across their
     *  width, shaded where they dip below the crossing strand.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the weave texture intensity at the given location.
     */
    public static float weave( float xIn, float yIn ) {
        final double strandWidth = 15;
        double x = xIn / strandWidth;
        double y = yIn / strandWidth;
        int cellX = (int) Math.floor(x);
        int cellY = (int) Math.floor(y);
        double fx = x - cellX;
        double fy = y - cellY;
        boolean lyingAcross = ( ( cellX + cellY ) & 1 ) == 0;
        double acrossStrand = lyingAcross ? fy : fx;
        double alongStrand  = lyingAcross ? fx : fy;
        double strandTone = _fastPseudoRandomDoubleFrom( lyingAcross ? cellY : cellX, lyingAcross ? 1 : -1 );
        double round = Math.sin( acrossStrand * Math.PI );
        double shadowAtEnds = 0.75 + 0.25 * Math.sin( alongStrand * Math.PI );
        double fibre = _valueNoise( xIn * 2.1, yIn * 2.1 );
        double slub  = _valueNoise( xIn * 0.33, yIn * 0.33 );
        return (float) _clamp01( round * shadowAtEnds * ( 0.66 + strandTone * 0.30 )
                                 + ( fibre - 0.5 ) * 0.16 + ( slub - 0.5 ) * 0.18 );
    }

    /**
     *  A print halftone screen: a grid of dots on the classic 45 degree angle, whose size
     *  follows an underlying tone. The look of newsprint and comic shading.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the halftone dot intensity at the given location.
     */
    public static float halftone( float xIn, float yIn ) {
        final double dotPitch = 13;
        double sx = ( xIn * HALFTONE_SCREEN_COS - yIn * HALFTONE_SCREEN_SIN ) / dotPitch;
        double sy = ( xIn * HALFTONE_SCREEN_SIN + yIn * HALFTONE_SCREEN_COS ) / dotPitch;
        double dx = sx - Math.floor(sx) - 0.5;
        double dy = sy - Math.floor(sy) - 0.5;
        double toDotCenter = Math.sqrt( dx*dx + dy*dy );
        double tone = _fractalNoise( xIn / 95.0, yIn / 95.0, 4 );
        double dotRadius = 0.1 + tone * 0.52;
        return (float) _clamp01( _smoothStep( _clamp01( ( dotRadius - toDotCenter ) * 9 ) ) );
    }

    /**
     *  Overlapping scales in offset rows, each row laid over the one behind it. Reads as
     *  fish or reptile skin, or as a roof of shingles.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the scale texture intensity at the given location.
     */
    public static float scales( float xIn, float yIn ) {
        final double scaleWidth = 30;
        final double rowPitch   = 16;
        int frontRow = (int) Math.floor( yIn / rowPitch );
        for ( int rowsBack = 0; rowsBack <= 2; rowsBack++ ) {
            int row = frontRow - rowsBack;
            double rowShift = ( ( row & 1 ) == 0 ) ? 0 : scaleWidth / 2;
            double column = Math.round( ( xIn - rowShift ) / scaleWidth );
            double centerX = column * scaleWidth + rowShift;
            double centerY = row * rowPitch;
            double acrossScale = ( xIn - centerX ) / ( scaleWidth * 0.56 );
            double downScale   = ( yIn - centerY ) / ( rowPitch * 1.8 );
            if ( downScale < 0 )
                continue;
            double radius = Math.sqrt( acrossScale * acrossScale + downScale * downScale );
            if ( radius > 1 )
                continue;
            double edgeShadow   = _smoothStep( _clamp01( ( 0.94 - radius ) * 11 ) );
            double towardRim = _clamp01( ( radius - 0.6 ) / 0.34 );
            double rimHighlight = towardRim * towardRim * towardRim * 0.42;
            double tone  = _fastPseudoRandomDoubleFrom( (float) column, (float) row );
            double sheen = _valueNoise( xIn * 0.55, yIn * 0.55 );
            double shade = 0.10 + downScale * 0.40 + ( 1 - radius ) * 0.24
                         + tone * 0.34 + ( sheen - 0.5 ) * 0.16;
            return (float) _clamp01( shade * edgeShadow + rimHighlight );
        }
        return 0.06f;
    }

    /**
     *  A printed circuit board: right angled and quarter turn traces running between
     *  occasional ring shaped solder pads.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the circuit board texture intensity at the given location.
     */
    public static float circuit( float xIn, float yIn ) {
        final double trackPitch = 26;
        double x = xIn / trackPitch;
        double y = yIn / trackPitch;
        int cellX = (int) Math.floor(x);
        int cellY = (int) Math.floor(y);
        double fx = x - cellX;
        double fy = y - cellY;
        byte seed = _fastPseudoRandomByteSeedFrom( cellX, cellY );
        double toTrack;
        if ( ( seed & 2 ) == 0 ) {
            double u = ( ( seed & 1 ) == 0 ) ? fx : 1 - fx;
            double toArcA = Math.abs( Math.sqrt( u * u + fy * fy ) - 0.5 );
            double toArcB = Math.abs( Math.sqrt( ( 1 - u ) * ( 1 - u ) + ( 1 - fy ) * ( 1 - fy ) ) - 0.5 );
            toTrack = Math.min( toArcA, toArcB );
        } else {
            toTrack = ( ( seed & 1 ) == 0 ) ? Math.abs( fy - 0.5 ) : Math.abs( fx - 0.5 );
        }
        double track = _smoothStep( _clamp01( ( 0.075 - toTrack ) * 16 ) );
        double toPadCenter = Math.sqrt( ( fx - 0.5 ) * ( fx - 0.5 ) + ( fy - 0.5 ) * ( fy - 0.5 ) );
        double pad = ( ( seed & 28 ) == 0 )
                        ? _smoothStep( _clamp01( ( 0.19 - toPadCenter ) * 14 ) )
                          - _smoothStep( _clamp01( ( 0.07 - toPadCenter ) * 20 ) )
                        : 0;
        double board = _valueNoise( xIn * 0.7, yIn * 0.7 );
        return (float) _clamp01( Math.max( track, pad ) * 0.92 + 0.04 + ( board - 0.5 ) * 0.06 );
    }

    /**
     *  Soap foam: overlapping bubbles of differing size, each domed by its own curvature
     *  and outlined by a bright film at the rim.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the bubble foam texture intensity at the given location.
     */
    public static float bubbles( float xIn, float yIn ) {
        final double foamSize = 24;
        double x = xIn / foamSize;
        double y = yIn / foamSize;
        int cellX = (int) Math.floor(x);
        int cellY = (int) Math.floor(y);
        double film = 0;
        for ( int oy = -1; oy <= 1; oy++ )
            for ( int ox = -1; ox <= 1; ox++ ) {
                int gx = cellX + ox;
                int gy = cellY + oy;
                double bubbleX = gx + _fastPseudoRandomDoubleFrom( gx, gy );
                double bubbleY = gy + _fastPseudoRandomDoubleFrom( gy, -gx );
                double radius  = 0.35 + _fastPseudoRandomDoubleFrom( gx + 331, gy - 977 ) * 0.55;
                double distance = _distanceBetween( bubbleX, bubbleY, x, y );
                if ( distance < radius ) {
                    double curvature = Math.sqrt( 1 - ( distance / radius ) * ( distance / radius ) );
                    double towardRim = distance / radius;
                    double towardRimCubed = towardRim * towardRim * towardRim;
                    double rim = towardRimCubed * towardRimCubed;
                    film = Math.max( film, 0.25 + curvature * 0.35 + rim * 0.7 );
                }
            }
        double sheen = _valueNoise( xIn / 40.0, yIn / 40.0 );
        return (float) _clamp01( film + ( sheen - 0.5 ) * 0.2 );
    }

    /**
     *  A four tone camouflage pattern: irregular patches with torn, interlocking edges,
     *  quantised into flat bands of colour.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the camouflage pattern intensity at the given location.
     */
    public static float camouflage( float xIn, float yIn ) {
        double warpX = _turbulentNoise( xIn / 30.0 + 3.1, yIn / 30.0 - 5.7, 3 ) - 0.37;
        double warpY = _turbulentNoise( xIn / 30.0 - 8.3, yIn / 30.0 + 1.9, 3 ) - 0.37;
        double blobs = _fractalNoise( xIn / 46.0 + warpX * 0.9, yIn / 46.0 + warpY * 0.9, 3 );
        double spread = _clamp01( 0.5 + ( blobs - 0.5 ) * 2.1 );
        return (float) ( Math.floor( spread * 3.999 ) / 3.0 );
    }

    /**
     *  The rippling net of light cast on the floor of a swimming pool: two overlaid webs
     *  of bright cell edges, brightest where they meet.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the caustics pattern intensity at the given location.
     */
    public static float caustics( float xIn, float yIn ) {
        double x = xIn / 46.0;
        double y = yIn / 46.0;
        double warpX = _fractalNoise( x + 2.3, y - 1.1, 2 ) - 0.5;
        double warpY = _fractalNoise( x - 3.7, y + 4.9, 2 ) - 0.5;
        double wideCellEdge = _worleyEdgeGap( x + warpX * 1.1, y + warpY * 1.1 );
        double fineCellEdge = _worleyEdgeGap( x * 1.9 - warpY * 1.4, y * 1.9 + warpX * 1.4 );
        double wideCore = 1 - _clamp01( wideCellEdge * 2.6 );
        double fineCore = 1 - _clamp01( fineCellEdge * 3.2 );
        double wideWeb = wideCore * wideCore * wideCore;
        double fineWeb = fineCore * fineCore * fineCore;
        double shimmer = _valueNoise( xIn / 26.0, yIn / 26.0 );
        return (float) _clamp01( wideWeb * 0.85 + fineWeb * 0.55 + ( shimmer - 0.5 ) * 0.12 );
    }

    /**
     *  Ice crystals creeping across a cold window: jagged, feathery veins branching out
     *  between fern like fronds, over a finely frosted surface.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the frost crystal intensity at the given location.
     */
    public static float frost( float xIn, float yIn ) {
        double featherX = _turbulentNoise( xIn / 18.0 + 5.1, yIn / 18.0 - 2.3, 4 ) - 0.37;
        double featherY = _turbulentNoise( xIn / 18.0 - 7.7, yIn / 18.0 + 8.9, 4 ) - 0.37;
        double crystalEdge = _worleyEdgeGap( xIn / 38.0 + featherX * 0.42, yIn / 38.0 + featherY * 0.42 );
        double spineCore = 1 - _clamp01( crystalEdge * 3.4 );
        double frondCore = 1 - _turbulentNoise( xIn / 17.0, yIn / 17.0, 5 );
        double frondCoreSquared = frondCore * frondCore;
        double spines    = spineCore * spineCore * spineCore;
        double dendrites = frondCoreSquared * frondCoreSquared * frondCore;
        double icedGlass = _valueNoise( xIn * 1.3, yIn * 1.3 );
        return (float) _clamp01( spines * 0.9 + dendrites * 0.75 + ( icedGlass - 0.5 ) * 0.16 );
    }

    /**
     *  A column of smoke drifting upward, stretched along the vertical axis and sheared
     *  sideways so the plume curls as it rises.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the smoke density at the given location.
     */
    public static float smoke( float xIn, float yIn ) {
        double x = xIn / 80.0;
        double y = yIn / 150.0;
        double drift = _fractalNoise( x * 0.8, y * 0.8, 3 ) - 0.5;
        double plume = _turbulentNoise( x + drift * 1.8, y, 5 );
        return (float) _clamp01( 1 - Math.pow( plume, 0.7 ) * 1.35 );
    }

    /**
     *  A field of stars, each with a bright core and a soft halo, scattered over a faint
     *  nebula. Made for dark backgrounds.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the star field brightness at the given location.
     */
    public static float stars( float xIn, float yIn ) {
        final double fieldSize = 22;
        double x = xIn / fieldSize;
        double y = yIn / fieldSize;
        int cellX = (int) Math.floor(x);
        int cellY = (int) Math.floor(y);
        double light = 0;
        for ( int oy = -1; oy <= 1; oy++ )
            for ( int ox = -1; ox <= 1; ox++ ) {
                int gx = cellX + ox;
                int gy = cellY + oy;
                double starX = gx + _fastPseudoRandomDoubleFrom( gx, gy );
                double starY = gy + _fastPseudoRandomDoubleFrom( gy, -gx );
                double reach = ( starX - x ) * ( starX - x ) + ( starY - y ) * ( starY - y );
                if ( reach > 0.64 )
                    continue;
                double magnitude = _fastPseudoRandomDoubleFrom( gx + 7919, gy + 104729 );
                double core = Math.exp( -reach * 420 );
                double halo = Math.exp( -Math.sqrt( reach ) * 9 ) * 0.4;
                light = Math.max( light, ( core + halo ) * Math.pow( magnitude, 2.2 ) );
            }
        double nebula = _fractalNoise( xIn / 130.0, yIn / 130.0, 4 );
        return (float) _clamp01( light * 1.3 + Math.pow( nebula, 3.2 ) * 0.3 );
    }

    /**
     *  An open water swell: long parallel crests, gently meandering, each breaking into a
     *  thin line of foam at its peak.
     *
     *  @param xIn The x coordinate in translated, scaled and rotated virtual space.
     *  @param yIn The y coordinate in translated, scaled and rotated virtual space.
     *  @return A float in the range [0, 1] representing the water wave intensity at the given location.
     */
    public static float waves( float xIn, float yIn ) {
        double swellDrift  = _fractalNoise( xIn / 300.0, yIn / 300.0, 3 ) - 0.5;
        double meander     = _fractalNoise( xIn / 120.0, yIn / 120.0, 2 ) - 0.5;
        double mainPhase   = ( xIn * 0.34 + yIn * 0.94 ) / 6.0 + swellDrift * 3.0 + meander * 1.0;
        double secondSwell = ( xIn * 0.62 + yIn * 0.78 ) / 17.0;
        double height = Math.sin( mainPhase )
                      + Math.sin( mainPhase * 1.87 + 0.9 ) * 0.30
                      + Math.sin( secondSwell ) * 0.34;
        double crest = _clamp01( ( height / 1.64 + 1 ) / 2 );
        double body  = Math.pow( crest, 2.8 ) * 0.60;
        double crestSquared = crest * crest;
        double crestToTheFifth = crestSquared * crestSquared * crest;
        double foam  = crestToTheFifth * crestToTheFifth * 0.55;
        double chop  = _valueNoise( xIn / 6.0, yIn / 6.0 );
        return (float) _clamp01( body + foam + ( chop - 0.5 ) * 0.11 );
    }

    /**
     *  Smoothly interpolated value noise: pseudo random values are placed on an
     *  integer lattice and blended with a smooth-step fade, giving a continuous
     *  field in the range 0..1. This is the building block for {@link #_fractalNoise}.
     */
    private static double _valueNoise( double x, double y ) {
        final int x0 = (int) Math.floor(x);
        final int y0 = (int) Math.floor(y);
        final double fx = _smoothStep( x - x0 );
        final double fy = _smoothStep( y - y0 );
        final double v00 = _fastPseudoRandomDoubleFrom( x0,     y0     );
        final double v10 = _fastPseudoRandomDoubleFrom( x0 + 1, y0     );
        final double v01 = _fastPseudoRandomDoubleFrom( x0,     y0 + 1 );
        final double v11 = _fastPseudoRandomDoubleFrom( x0 + 1, y0 + 1 );
        final double top    = v00 + ( v10 - v00 ) * fx;
        final double bottom = v01 + ( v11 - v01 ) * fx;
        return top + ( bottom - top ) * fy;
    }

    /**
     *  Fractal Brownian motion: several octaves of {@link #_valueNoise} are summed
     *  with halving amplitude and doubling frequency. The result stays in 0..1.
     */
    private static double _fractalNoise( double x, double y, int octaves ) {
        double sum = 0;
        double amplitude = 1;
        double frequency = 1;
        double totalAmplitude = 0;
        for ( int i = 0; i < octaves; i++ ) {
            sum += _valueNoise( x * frequency, y * frequency ) * amplitude;
            totalAmplitude += amplitude;
            amplitude *= 0.5;
            frequency *= 2;
        }
        return sum / totalAmplitude;
    }

    /**
     *  Fractal Brownian motion of the absolute deviation from the mid value, which
     *  creases the field at every zero crossing instead of rounding it off. Each
     *  octave is turned as well as scaled, so the creases do not line up with the
     *  lattice the way plain {@link #_fractalNoise} octaves would.
     */
    private static double _turbulentNoise( double x, double y, int octaves ) {
        double sum = 0;
        double amplitude = 1;
        double totalAmplitude = 0;
        double px = x, py = y;
        for ( int i = 0; i < octaves; i++ ) {
            sum += Math.abs( _valueNoise( px, py ) * 2 - 1 ) * amplitude;
            totalAmplitude += amplitude;
            amplitude *= 0.5;
            final double turnedX = ( px * OCTAVE_TURN_COS - py * OCTAVE_TURN_SIN ) * 2 + 37.13;
            final double turnedY = ( px * OCTAVE_TURN_SIN + py * OCTAVE_TURN_COS ) * 2 - 19.71;
            px = turnedX;
            py = turnedY;
        }
        return sum / totalAmplitude;
    }

    /**
     *  A ridged multifractal: the creases of {@link #_turbulentNoise} are inverted into
     *  crests and each octave is weighted by the one before it, so detail accumulates on
     *  the crests and the valleys stay smooth - the classic mountain ridge field.
     */
    private static double _ridgedNoise( double x, double y, int octaves ) {
        double sum = 0;
        double amplitude = 1;
        double totalAmplitude = 0;
        double weight = 1;
        double px = x, py = y;
        for ( int i = 0; i < octaves; i++ ) {
            double crest = 1 - Math.abs( _valueNoise( px, py ) * 2 - 1 );
            crest *= crest;
            crest *= weight;
            weight = _clamp01( crest * 2.6 );
            sum += crest * amplitude;
            totalAmplitude += amplitude;
            amplitude *= 0.55;
            final double turnedX = ( px * OCTAVE_TURN_COS - py * OCTAVE_TURN_SIN ) * 2 + 11.37;
            final double turnedY = ( px * OCTAVE_TURN_SIN + py * OCTAVE_TURN_COS ) * 2 - 43.19;
            px = turnedX;
            py = turnedY;
        }
        return sum / totalAmplitude;
    }

    /**
     *  The distance from the given coordinate to the closest Worley (Voronoi) feature point,
     *  which is small inside a cell and largest at the cell corners.
     */
    private static double _worleyNearestDistance( double x, double y ) {
        final int cellX = (int) Math.floor(x);
        final int cellY = (int) Math.floor(y);
        double nearest = Double.POSITIVE_INFINITY;
        for ( int oy = -1; oy <= 1; oy++ ) {
            for ( int ox = -1; ox <= 1; ox++ ) {
                final int gx = cellX + ox;
                final int gy = cellY + oy;
                final double px = gx + _fastPseudoRandomDoubleFrom( gx, gy );
                final double py = gy + _fastPseudoRandomDoubleFrom( gy, -gx );
                final double reach = ( px - x ) * ( px - x ) + ( py - y ) * ( py - y );
                if ( reach < nearest )
                    nearest = reach;
            }
        }
        return Math.sqrt( nearest );
    }

    /**
     *  How much further the second closest Worley (Voronoi) feature point is than the closest
     *  one. This vanishes exactly on the border between two cells, which is what draws the
     *  cell walls of a Voronoi diagram.
     */
    private static double _worleyEdgeGap( double x, double y ) {
        final int cellX = (int) Math.floor(x);
        final int cellY = (int) Math.floor(y);
        double nearest       = Double.POSITIVE_INFINITY;
        double secondNearest = Double.POSITIVE_INFINITY;
        for ( int oy = -1; oy <= 1; oy++ ) {
            for ( int ox = -1; ox <= 1; ox++ ) {
                final int gx = cellX + ox;
                final int gy = cellY + oy;
                final double px = gx + _fastPseudoRandomDoubleFrom( gx, gy );
                final double py = gy + _fastPseudoRandomDoubleFrom( gy, -gx );
                final double reach = ( px - x ) * ( px - x ) + ( py - y ) * ( py - y );
                if ( reach < nearest ) {
                    secondNearest = nearest;
                    nearest = reach;
                } else if ( reach < secondNearest ) {
                    secondNearest = reach;
                }
            }
        }
        return Math.sqrt( secondNearest ) - Math.sqrt( nearest );
    }

    private static double _wrapAround( double value, double period ) {
        if ( value >= 0 && value < period )
            return value;
        final double wrapped = value % period;
        return wrapped < 0 ? wrapped + period : wrapped;
    }

    private static int[] _roundedKernelLine( int kernelSize, float coordinate ) {
        final int maxDistance = kernelSize / 2;
        final int[] line = new int[kernelSize];
        for ( int i = 0; i < kernelSize; i++ )
            line[i] = Math.round( ( i - maxDistance ) + coordinate );
        return line;
    }

    private static int[] _roundedKernelLine( int kernelSize, float coordinate, float frequency ) {
        final int maxDistance = kernelSize / 2;
        final int[] line = new int[kernelSize];
        for ( int i = 0; i < kernelSize; i++ )
            line[i] = Math.round( ( ( i - maxDistance ) + coordinate ) * frequency );
        return line;
    }

    private static double _smoothStep( double t ) {
        return t * t * ( 3 - 2 * t );
    }

    private static double _clamp01( double value ) {
        return value < 0 ? 0 : ( value > 1 ? 1 : value );
    }

    private static double _voronoiBasedWavesSum( float xIn, float yIn ) {
        final int minX1 = (int) Math.floor(xIn) - 1 ;
        final int minX2 = (int) Math.floor(xIn)     ;
        final int minX3 = (int) Math.floor(xIn) + 1 ;
        final int minY1 = (int) Math.floor(yIn) - 1 ;
        final int minY2 = (int) Math.floor(yIn)     ;
        final int minY3 = (int) Math.floor(yIn) + 1 ;
        final double centerX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY2);
        final double centerY = minY2 + _fastPseudoRandomDoubleFrom(minY2, minX2);
        final double randomCenter = _fastPseudoRandomDoubleFrom((float) centerX, (float) centerY);
        final double distanceCenter = _invDistanceBetween(centerX, centerY, xIn, yIn);
        final double leftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY2);
        final double leftY = minY2 + _fastPseudoRandomDoubleFrom(minY2, minX1);
        final double randomLeft = _fastPseudoRandomDoubleFrom((float) leftX, (float) leftY);
        final double distanceLeft = _invDistanceBetween(leftX, leftY, xIn, yIn);
        final double rightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY2);
        final double rightY = minY2 + _fastPseudoRandomDoubleFrom(minY2, minX3);
        final double randomRight = _fastPseudoRandomDoubleFrom((float) rightX, (float) rightY);
        final double distanceRight = _invDistanceBetween(rightX, rightY, xIn, yIn);
        final double topX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY1);
        final double topY = minY1 + _fastPseudoRandomDoubleFrom(minY1, minX2);
        final double randomTop = _fastPseudoRandomDoubleFrom((float) topX, (float) topY);
        final double distanceTop = _invDistanceBetween(topX, topY, xIn, yIn);
        final double bottomX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY3);
        final double bottomY = minY3 + _fastPseudoRandomDoubleFrom(minY3, minX2);
        final double randomBottom = _fastPseudoRandomDoubleFrom((float) bottomX, (float) bottomY);
        final double distanceBottom = _invDistanceBetween(bottomX, bottomY, xIn, yIn);
        final double topLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY1);
        final double topLeftY = minY1 + _fastPseudoRandomDoubleFrom(minY1, minX1);
        final double randomTopLeft = _fastPseudoRandomDoubleFrom((float) topLeftX, (float) topLeftY);
        final double distanceTopLeft = _invDistanceBetween(topLeftX, topLeftY, xIn, yIn);
        final double topRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY1);
        final double topRightY = minY1 + _fastPseudoRandomDoubleFrom(minY1, minX3);
        final double randomTopRight = _fastPseudoRandomDoubleFrom((float) topRightX, (float) topRightY);
        final double distanceTopRight = _invDistanceBetween(topRightX, topRightY, xIn, yIn);
        final double bottomLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY3);
        final double bottomLeftY = minY3 + _fastPseudoRandomDoubleFrom(minY3, minX1);
        final double randomBottomLeft = _fastPseudoRandomDoubleFrom((float) bottomLeftX, (float) bottomLeftY);
        final double distanceBottomLeft = _invDistanceBetween(bottomLeftX, bottomLeftY, xIn, yIn);
        final double bottomRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY3);
        final double bottomRightY = minY3 + _fastPseudoRandomDoubleFrom(minY3, minX3);
        final double randomBottomRight = _fastPseudoRandomDoubleFrom((float) bottomRightX, (float) bottomRightY);
        final double distanceBottomRight = _invDistanceBetween(bottomRightX, bottomRightY, xIn, yIn);
        double pool = 0;
        pool += _rippleAmplitude( distanceCenter     , randomCenter      );
        pool += _rippleAmplitude( distanceLeft       , randomLeft        );
        pool += _rippleAmplitude( distanceRight      , randomRight       );
        pool += _rippleAmplitude( distanceTop        , randomTop         );
        pool += _rippleAmplitude( distanceBottom     , randomBottom      );
        pool += _rippleAmplitude( distanceTopLeft    , randomTopLeft     );
        pool += _rippleAmplitude( distanceTopRight   , randomTopRight    );
        pool += _rippleAmplitude( distanceBottomLeft , randomBottomLeft  );
        pool += _rippleAmplitude( distanceBottomRight, randomBottomRight );
        return pool;
    }

    private static double _rippleAmplitude( double distance, double random ) {
        if ( distance == 0 )
            return 0;
        double impactForce = ( 3 + 32 * random );
        double amplitude = distance * Math.sin( ( 1 + Math.pow( distance, 2 ) ) * impactForce );
        double fadeAway = ( 0.5 + random );
        return amplitude * fadeAway;
    }

    private static double _wave(double in) {
        return 1 - ( 1 + Math.cos(in) ) / 2;
    }

    private static double _invDistanceBetween(double x1, double y1, double x2, double y2 ) {
        return Math.max(0, 1 - Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) ));
    }

    private static double _sigmoid( double x ) {
        return 1 / (1 + Math.exp(-x));
    }

    /**
     * @param x The x coordinate
     * @param y The y coordinate
     * @return A pseudo random double in the range 0.0 to 1.0
     */
    private static double _fastPseudoRandomDoubleFrom( float x, float y ) {
        return SEED_BYTE_TO_UNIT_DOUBLE[ _fastPseudoRandomByteSeedFrom(x, y) + 128 ];
    }

    private static byte _fastPseudoRandomByteSeedFrom( float a, float b ) {
        return _fastPseudoRandomByteSeedFrom(
                    Float.floatToRawIntBits(a),
                    Float.floatToRawIntBits(b)
                 );
    }

    private static byte _fastPseudoRandomByteSeedFrom( int a, int b ) {
        long x = PRIME_1 * a;
        long y = PRIME_2 * (x + b);
        return _longSeedToByte(x ^ y);
    }

    private static byte _longSeedToByte(long seed) {
        int asInt = (int) (seed ^ (seed >>> 32));
        short asShort = (short) (asInt ^ (asInt >>> 16));
        return (byte) (asShort ^ (asShort >>> 8));
    }

}
