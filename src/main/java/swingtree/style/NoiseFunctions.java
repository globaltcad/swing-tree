package swingtree.style;

public final class NoiseFunctions
{
    private static final long PRIME = 12055296811267L;


    private NoiseFunctions(){}


    public static double stochastic( float xIn, float yIn ) {
        int kernelSize = 8;
        double sum = _coordinateToGradValue(kernelSize, xIn, yIn);
        return (Math.sin(sum * (12.0/kernelSize)) + 1)/2;
    }

    public static double smoothTopology( float xIn, float yIn ) {
        float scale = 6;
        return (Math.sin(stochastic(xIn/scale, yIn/scale) * 6 * Math.PI) + 1)/2;
    }

    public static double hardTopology( float xIn, float yIn ) {
        float scale = 6;
        return (stochastic(xIn/scale, yIn/scale)*6)%1;
    }

    public static double hardSpots( float xIn, float yIn ) {
        float scale = 4;
        return Math.round(stochastic(xIn/scale, yIn/scale));
    }

    public static double smoothSpots( float xIn, float yIn ) {
        float scale = 6;
        int kernelSize = 6;
        double sum = _coordinateToGradValue(kernelSize, xIn/scale, yIn/scale);
        return _sigmoid(sum * 64 / kernelSize);
    }

    public static double grainy(float xIn, float yIn ) {
        float scale = 2;
        int kernelSize = 4;
        double sum = _coordinateToGradValue(kernelSize, xIn/scale, yIn/scale);
        return (Math.sin(sum * (12.0/kernelSize)) + 1)/2;
    }

    public static double tiles(float xIn, float yIn ) {
        float scale = 10;
        int kernelSize = 8;
        double sum = _coordinateToGradTileValue(kernelSize, xIn/scale, yIn/scale);
        return (Math.sin(sum * (12.0/kernelSize)) + 1)/2;
    }

    public static double fibery( float xIn, float yIn ) {
        float scale = 5;
        int kernelSize = 4;
        double sum = _coordinateToFiberValue(kernelSize, xIn/scale, yIn/scale);
        return (Math.sin(sum * (12.0/kernelSize)) + 1)/2;
    }

    private static double _sigmoid( double x ) {
        return 1 / (1 + Math.exp(-x));
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


    private static double _coordinateToGradTileValue(int kernelSize, float xIn, float yIn ) {
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
                final double relevance = Math.max(0, 1.0 - distance / maxDistance);
                final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                sum += ( frac * ( relevance * relevance ) );
            }
        }
        return sum;
    }


    private static double _fastPseudoRandomDoubleFrom( float x, float y ) {
        final byte randomByte = (byte) (_fastPseudoRandomByteSeedFrom(x, y) + _fastPseudoRandomByteSeedFrom(y, x));
        // The byte is in the range -128 to 127, so -128 is 0.0 and 127 is 1.0
        return (randomByte + 128) / 255.0;
    }

    private static byte _fastPseudoRandomByteSeedFrom( float a, float b ) {
        long part1 = Float.floatToRawIntBits(a) * PRIME;
        long part2 = Float.floatToRawIntBits(b) * part1;
        return _longSeedToByte(part1 ^ part2);

    }

    private static byte _longSeedToByte(long seed) {
        int asInt = (int) (seed ^ (seed >>> 32));
        short asShort = (short) (asInt ^ (asInt >>> 16));
        return (byte) (asShort ^ (asShort >>> 8));
    }

}
