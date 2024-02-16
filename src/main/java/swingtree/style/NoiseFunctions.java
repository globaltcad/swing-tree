package swingtree.style;

import java.util.HashSet;
import java.util.Set;

final class NoiseFunctions
{
    private static final long   MULTIPLIER = 0x5DEECE66DL;
    private static final long   ADDEND = 0xBL;
    private static final long   MASK = (1L << 48) - 1;
    private static final double DOUBLE_UNIT = 0x1.0p-53; // 1.0 / (1L << 53)
    private static final long RANDOM_1 = 257251024006901L;
    private static final long RANDOM_2 = -92458265371817L;
    private static final long RANDOM_3 = 243389330257175L;
    private static final long RANDOM_4 = -0x6a3c08e3f9a3dL;
    private static final long PRIME_1 = 12055296811267L;
    private static final long PRIME_2 = 17461204521323L;
    private static final long PRIME_3 = 28871271685163L;
    private static final long PRIME_4 = 53982894593057L;


    private NoiseFunctions(){}


    static double _coordinateToGradValue( float xIn, float yIn ) {
        final int kernelSize = 12;
        final int maxDistance = kernelSize / 2;
        double average = 0;
        double sampleRate = 0.5;
        double samplingSum = 1e-6;
        for ( int x = 0; x < kernelSize; x++ ) {
            for ( int y = 0; y < kernelSize; y++ ) {
                final float xi = ( x - maxDistance ) + xIn;
                final float yi = ( y - maxDistance ) + yIn;
                final int rx = Math.round( xi );
                final int ry = Math.round( yi );
                final byte score = _fastPseudoRandomByteSeedFrom( rx, ry );
                final boolean takeGaussian = (255 * sampleRate -128) > score;
                if ( takeGaussian ) {
                    final double vx = rx - xIn;
                    final double vy = ry - yIn;
                    final double distance = Math.sqrt( vx * vx + vy * vy );
                    final double relevance = Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fastPseudoRandomDoubleFrom(rx, ry) - 0.5;
                    average += ( frac * relevance );
                    samplingSum += relevance;
                }
            }
        }
        double scaler = 0.75 / ( sampleRate * samplingSum / kernelSize );
        return fastPseudoSig(average * scaler);
    }

    private static double fastPseudoSig( double x ) {
        return ( 1 + x * _invSqrt( 1d + x * x ) ) / 2;
    }


    /**
     *  This is extremely fast and has virtually the same accuracy as {@code 1 / Math.pow(x, 0.5)}.
     */
    private static double _invSqrt(double x) {
        double xhalf = 0.5d * x;
        long i = Double.doubleToLongBits(x);
        i = 0x5fe6ec85e7de30daL - (i >> 1);
        x = Double.longBitsToDouble(i);
        x *= (1.5d - xhalf * x * x);
        x *= (1.5d - xhalf * x * x); // more accuracy...
        x *= (1.5d - xhalf * x * x); // more accuracy...
        x *= (1.5d - xhalf * x * x); // more accuracy...
        return x;
    }

    private static double _fastPseudoRandomDoubleFrom( float x, float y ) {
        final byte randomByte = (byte) (_fastPseudoRandomByteSeedFrom(x, y) + _fastPseudoRandomByteSeedFrom(y, x));
        // The byte is in the range -128 to 127, so -128 is 0.0 and 127 is 1.0
        return (randomByte + 128) / 255.0;
    }

    /**
     *  Takes 2 floats defining an x and y coordinate and returns a long
     *  which is the bits of the two floats consecutively concatenated.
     */
    private static long _pseudoRandomSeedFrom( float x, float y ) {
        long xi = Float.floatToRawIntBits(x);
        long yi = Float.floatToRawIntBits(y);
        long hash = _baseScramble(xi - yi);
        hash = ( hash * PRIME_1 + xi ) ^ RANDOM_1;
        hash = ( hash * PRIME_2 + yi ) ^ RANDOM_2;
        hash = ( hash * PRIME_3 - xi ) ^ RANDOM_3;
        hash = ( hash * PRIME_4 - yi ) ^ RANDOM_4;
        return hash;
    }

    private static byte _fastPseudoRandomByteSeedFrom( float x, float y ) {
        long part1 = Float.floatToRawIntBits(x) * PRIME_1;
        long part2 = Float.floatToRawIntBits(y) * part1;
        return _longSeedToByte(part1 ^ part2);

    }

    private static byte _longSeedToByte(long seed) {
        int asInt = (int) (seed ^ (seed >>> 32));
        short asShort = (short) (asInt ^ (asInt >>> 16));
        return (byte) (asShort ^ (asShort >>> 8));
    }

    private static long _baseScramble( long seed ) {
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

    private static double _fractionFrom( long seed )
    {
        long seed1 = _nextSeed(seed );
        long seed2 = _nextSeed(seed1);
        return _nextDouble( seed1, seed2 );
    }

}
