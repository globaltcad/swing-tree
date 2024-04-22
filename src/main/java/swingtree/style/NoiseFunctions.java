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


    private NoiseFunctions(){}


    public static float stochastic( float xIn, float yIn ) {
        int kernelSize = 8;
        double sum = _coordinateToGradValue(kernelSize, xIn, yIn);
        return (float) ((Math.sin(sum * (12.0/kernelSize)) + 1)/2);
    }

    private static double _coordinateToGradValue( int kernelSize, float xIn, float yIn ) {
        final int maxDistance  = kernelSize / 2;
        final int kernelPoints = kernelSize * kernelSize;
        final double sampleRate = 0.5;
        double sum = 0;
        for ( int i = 0; i < kernelPoints; i++ ) {
            final int x = i % kernelSize;
            final int y = i / kernelSize;
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
                final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                sum += ( frac * (relevance*relevance) );
            }
        }
        return sum;
    }

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
        final int kernelPoints = kernelSize * kernelSize;
        final double sampleRate = 0.5;
        double sum = 0;
        for ( int i = 0; i < kernelPoints; i++ ) {
            final int x = i % kernelSize;
            final int y = i / kernelSize;
            final float xi = ( x - maxDistance ) + xIn;
            final float yi = ( y - maxDistance ) + yIn;
            final int rx = Math.round( xi );
            final int ry = Math.round( yi );
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
        return sum;
    }

    public static float fibery( float xIn, float yIn ) {
        float scale = 5;
        int kernelSize = 4;
        double sum = _coordinateToFiberValue(kernelSize, xIn/scale, yIn/scale);
        return (float) ((Math.sin(sum * (12.0/kernelSize)) + 1)/2);
    }

    private static double _coordinateToFiberValue( int kernelSize, float xIn, float yIn ) {
        final int maxDistance   = kernelSize / 2;
        final int kernelPoints  = kernelSize * kernelSize;
        final double sampleRate = 0.5;
        double sum = 0;
        for ( int i = 0; i < kernelPoints; i++ ) {
            final int x = i % kernelSize;
            final int y = i / kernelSize;
            final float xi = ( x - maxDistance ) + xIn;
            final float yi = ( y - maxDistance ) + yIn;
            final int rx = Math.round( xi );
            final int ry = Math.round( yi );
            final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
            final boolean takeSample = (255 * sampleRate - 128) < score;
            if ( takeSample ) {
                final double vx = rx - xIn;
                final double vy = ry - yIn;
                final double distance = Math.sqrt( vx*vx % 2 + vy*vy % 2);
                double relevance = Math.max(0, 1.0 - distance / maxDistance);
                final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                relevance = Math.min(1, (relevance * relevance) * 1.5);
                sum += ( frac * relevance );
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
        final int kernelPoints = kernelSize * kernelSize;
        final double sampleRate = 0.5;
        double sum = 0;
        for ( int i = 0; i < kernelPoints; i++ ) {
            final int x = i % kernelSize;
            final int y = i / kernelSize;
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
        final int kernelPoints = kernelSize * kernelSize;
        final double sampleRate = 0.65;
        double grad = 0;
        for ( int i = 0; i < kernelPoints; i++ ) {
            final int x = i % kernelSize;
            final int y = i / kernelSize;
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
        final int kernelPoints = kernelSize * kernelSize;
        final double sampleRate = 0.5;
        double sum = 0;
        for ( int i = 0; i < kernelPoints; i++ ) {
            final int x = i % kernelSize;
            final int y = i / kernelSize;
            final float xi = ( x - maxDistance ) + xIn;
            final float yi = ( y - maxDistance ) + yIn;
            final int rx = Math.round( xi );
            final int ry = Math.round( yi );
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
                final int rx2 = Math.round( xi * 3 );
                final int ry2 = Math.round( yi * 3 );
                final double subNoise = 1 + (_fastPseudoRandomDoubleFrom(rx2, ry2) - 0.5) / 5;
                sum += ( frac * (relevance*subNoise) );
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
        final int kernelPoints = kernelSize * kernelSize;
        final double sampleRate = 0.75;
        double result = 0;
        for ( int i = 0; i < kernelPoints; i++ ) {
            final int x = i % kernelSize;
            final int y = i / kernelSize;
            final float xi = ( x - maxDistance ) + xIn;
            final float yi = ( y - maxDistance ) + yIn;
            final int rx = Math.round( xi );
            final int ry = Math.round( yi );
            final double vx = rx - xIn;
            final double vy = ry - yIn;
            final double distance = Math.sqrt( vx * vx + vy * vy );
            final double relevance = 1.0 - distance / maxDistance;
            if ( relevance >= 0 ) {
                final byte score = _fastPseudoRandomByteSeedFrom( ry, rx );
                final boolean takeSample = (255 * sampleRate - 128) < score;
                if ( takeSample ) {
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
        return result;
    }

    public static float mandelbrot( float xIn, float yIn ) {
        final int MAX_ITERATIONS = 5000;
        double x = xIn/100.0;
        double y = yIn/100.0;
        double ix = 0;
        double iy = 0;
        int iteration = 0;
        while (ix * ix + iy * iy < 4 && iteration < MAX_ITERATIONS) {
            double xtemp = ix * ix - iy * iy + x;
            iy = 2 * ix * iy + y;
            ix = xtemp;
            iteration++;
        }
        return (float) (1 - Math.log(iteration) / Math.log(MAX_ITERATIONS));
    }

    public static float voronoiBasedCells(float xIn, float yIn ) {
        float scale = 1f/32;
        return _coordinateToWorleyDistanceValue(xIn*scale, yIn*scale);
    }

    private static float _coordinateToWorleyDistanceValue(float xIn, float yIn ) {
        final int minX1 = (int) Math.floor(xIn) - 1 ;
        final int minX2 = (int) Math.floor(xIn)     ;
        final int minX3 = (int) Math.floor(xIn) + 1 ;
        final int minY1 = (int) Math.floor(yIn) - 1 ;
        final int minY2 = (int) Math.floor(yIn)     ;
        final int minY3 = (int) Math.floor(yIn) + 1 ;
        final double centerX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY2);
        final double centerY = minY2 + _fastPseudoRandomDoubleFrom(minY2, minX2);
        final double distanceCenter = _distanceBetween(centerX, centerY, xIn, yIn);
        final double leftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY2);
        final double leftY = minY2 + _fastPseudoRandomDoubleFrom(minY2, minX1);
        final double distanceLeft = _distanceBetween(leftX, leftY, xIn, yIn);
        final double rightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY2);
        final double rightY = minY2 + _fastPseudoRandomDoubleFrom(minY2, minX3);
        final double distanceRight = _distanceBetween(rightX, rightY, xIn, yIn);
        final double topX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY1);
        final double topY = minY1 + _fastPseudoRandomDoubleFrom(minY1, minX2);
        final double distanceTop = _distanceBetween(topX, topY, xIn, yIn);
        final double bottomX = minX2 + _fastPseudoRandomDoubleFrom(minX2, minY3);
        final double bottomY = minY3 + _fastPseudoRandomDoubleFrom(minY3, minX2);
        final double distanceBottom = _distanceBetween(bottomX, bottomY, xIn, yIn);
        final double topLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY1);
        final double topLeftY = minY1 + _fastPseudoRandomDoubleFrom(minY1, minX1);
        final double distanceTopLeft = _distanceBetween(topLeftX, topLeftY, xIn, yIn);
        final double topRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY1);
        final double topRightY = minY1 + _fastPseudoRandomDoubleFrom(minY1, minX3);
        final double distanceTopRight = _distanceBetween(topRightX, topRightY, xIn, yIn);
        final double bottomLeftX = minX1 + _fastPseudoRandomDoubleFrom(minX1, minY3);
        final double bottomLeftY = minY3 + _fastPseudoRandomDoubleFrom(minY3, minX1);
        final double distanceBottomLeft = _distanceBetween(bottomLeftX, bottomLeftY, xIn, yIn);
        final double bottomRightX = minX3 + _fastPseudoRandomDoubleFrom(minX3, minY3);
        final double bottomRightY = minY3 + _fastPseudoRandomDoubleFrom(minY3, minX3);
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

    private static double _distanceBetween( double x1, double y1, double x2, double y2 ) {
        return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );
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
        final byte randomByte = _fastPseudoRandomByteSeedFrom(x, y);
        // The byte is in the range -128 to 127, so -128 is 0.0 and 127 is 1.0
        return (randomByte + 128) / 255.0;
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
