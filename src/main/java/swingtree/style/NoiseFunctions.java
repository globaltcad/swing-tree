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
        final double sampleRate = 0.5;
        double sum = 0;
        // Nested loops over (x,y) produce the exact same point sequence as the former
        // single 'i' loop with x = i % kernelSize / y = i / kernelSize, but without the
        // per-point integer division and modulo.
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
                    final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                    sum += ( frac * (relevance*relevance) );
                }
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

    public static float fabric( float xIn, float yIn ) {
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
        final double sampleRate = 0.5;
        double sum = 0;
        // Nested loops over (x,y) produce the exact same point sequence as the former
        // single 'i' loop with x = i % kernelSize / y = i / kernelSize, but without the
        // per-point integer division and modulo.
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

    public static float voronoiBasedCellTissue(float xIn, float yIn ) {
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

    public static float voronoiBasedCellMosaic(float xIn, float yIn ) {
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

    public static float voronoiBasedPolygonCell(float xIn, float yIn ) {
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
        /*
            We now have a line that spans between 2 cell centers
            which have their boundary in the middle.
            We now project the current pixel position onto
            that line.
        */
        double t = projectPointOntoLine(closestX, closestY, secondClosestX, secondClosestY, xIn, yIn);
        double distanceBetweenTheTwo = _distanceBetween(closestX, closestY, secondClosestX, secondClosestY);
        double frac = ((0.5-t)*Math.pow(distanceBetweenTheTwo, 2));
        return (float) Math.max(0, Math.min(1, Math.sqrt(frac)));
    }

    public static double projectPointOntoLine(double x1, double y1, double x2, double y2, double px, double py) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = (dx * dx + dy * dy);

        if (lengthSquared == 0) {
            throw new IllegalArgumentException("The two points defining the line must not be the same.");
        }

        return ((px - x1) * dx + (py - y1) * dy) / lengthSquared;
    }


    private static double _distanceBetween( double x1, double y1, double x2, double y2 ) {
        return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );
    }

    public static float voronoiBasedPondInDrizzle(float xIn, float yIn ) {
        float scale = 0.5f/32;
        double pool = _voronoiBasedWavesSum(xIn*scale, yIn*scale);
        return (float) _wave(Math.pow(Math.abs(pool), 2));
    }

    public static float voronoiBasedPondInRain(float xIn, float yIn ) {
        float scale = 0.5f/32;
        double pool = _voronoiBasedWavesSum(xIn*scale, yIn*scale)*1.5;
        pool += _voronoiBasedWavesSum(yIn*scale*2, -xIn*scale*2)/1.5;
        return (float) _wave(Math.abs(pool*1.5));
    }

    public static float voronoiBasedPondOfStrings(float xIn, float yIn ) {
        float scale = 0.5f/32;
        double pool = _voronoiBasedWavesSum(xIn*scale, yIn*scale);
        return (float) _wave(Math.pow(Math.abs(pool*4), 0.5));
    }

    public static float voronoiBasedPondOfTangledStrings(float xIn, float yIn ) {
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
     */
    public static float clouds( float xIn, float yIn ) {
        final float scale = 64;
        final double density = _fractalNoise(xIn / scale, yIn / scale, 6);
        return (float) _sigmoid( ( density - 0.5 ) * 7 );
    }

    /**
     *  A network of thin cracks separating irregular plates, computed from the
     *  difference between the two closest Worley (Voronoi) feature points.
     */
    public static float cracks( float xIn, float yIn ) {
        final float scale = 28;
        final double[] f1f2 = _worleyF1F2(xIn / scale, yIn / scale);
        final double edge = f1f2[1] - f1f2[0];
        return (float) _sigmoid( ( edge - 0.06 ) * 30 );
    }

    /**
     *  A swirling vortex created by rotating the sampling angle as a function of
     *  the radius and an underlying fractal noise field.
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
     *  Returns the distances to the closest and second-closest Worley feature
     *  points around the given coordinate as a {@code [F1, F2]} pair.
     */
    private static double[] _worleyF1F2( double x, double y ) {
        final int cellX = (int) Math.floor(x);
        final int cellY = (int) Math.floor(y);
        double f1 = Double.POSITIVE_INFINITY;
        double f2 = Double.POSITIVE_INFINITY;
        for ( int oy = -1; oy <= 1; oy++ ) {
            for ( int ox = -1; ox <= 1; ox++ ) {
                final int gx = cellX + ox;
                final int gy = cellY + oy;
                final double px = gx + _fastPseudoRandomDoubleFrom( gx, gy );
                final double py = gy + _fastPseudoRandomDoubleFrom( gy, -gx );
                final double distance = _distanceBetween( px, py, x, y );
                if ( distance < f1 ) {
                    f2 = f1;
                    f1 = distance;
                } else if ( distance < f2 ) {
                    f2 = distance;
                }
            }
        }
        return new double[]{ f1, f2 };
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
