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


    static double _coordinateToGradValue( float x, float y ) {
        final int subPixelDivision = 8;
        final int maxDistance = subPixelDivision / 2;
        double height = 0.5;
        double sum = 0.0;
        int samples = 0;
        final int numberOfPoints = subPixelDivision*subPixelDivision;
        final int padLeft = 0;
        final int padRight = 1;
        int xmin = 0;
        int xmax = subPixelDivision;
        int ymin = 0;
        int ymax = subPixelDivision;
        int i0 = 0;
        int i1 = 0;
        int nextLayerStartIndex = 0;
        int lastLayerIndex = 0;

        for ( int i = 0; i < numberOfPoints; i++ ) {
            final int w  = Math.max(xmax - xmin - (padLeft+padRight), 0); // Minus 1 because one side is already taken
            final int h = Math.max(ymax - ymin - (padLeft+padRight), 0);
            final int currentLayerSize = ( w * 2 + h * 2 );
            final int li = i - lastLayerIndex;
            final int si = li / 4;
            final int mode = li % 4;
            final boolean top    = mode == 0;
            final boolean bottom = mode == 1;
            final boolean left   = mode == 2;
            final boolean right  = mode == 3;

            if ( nextLayerStartIndex == 0 )
                nextLayerStartIndex = currentLayerSize;

            boolean milestoneReached = ( i == nextLayerStartIndex - 1 );
            int upcomingMilestone = nextLayerStartIndex;

            if ( i == nextLayerStartIndex ) {
                upcomingMilestone += currentLayerSize;
            }
            nextLayerStartIndex = upcomingMilestone;

            if ( top ) {
                i0 = xmin + ( si % w ) + padLeft; // from left to right
                i1 = ymin;
            }
            if ( bottom ) {
                i0 = xmax - ( si % w ) - 1 - padLeft; // from right to left
                i1 = ymax - 1;
            }
            if ( left ) {
                i0 = xmin;
                i1 = ymax - ( si % h ) - 1 - padLeft; // from bottom to top
            }
            if ( right ) {
                i0 = xmax - 1;
                i1 = ymin + ( si % h ) + padLeft; // from top to bottom
            }
            if ( milestoneReached ) {
                lastLayerIndex = i + 1;
                xmin++;
                xmax--;
                ymin++;
                ymax--;
            }

                final float xi = ( i0 - maxDistance ) + x;
                final float yi = ( i1 - maxDistance ) + y;
                final int rx = Math.round( xi );
                final int ry = Math.round( yi );
                byte b = _fastPseudoRandomByteSeedFrom( rx, ry );
                final boolean takeGaussian = b < -100;//0.45 > _fractionFrom( _pseudoRandomSeedFrom( ry, rx ) );
                if ( takeGaussian ) {
                    final double vx = rx - x;
                    final double vy = ry - y;
                    final double distance = Math.sqrt( vx * vx + vy * vy );
                    final double relevance = Math.max(0, 1.0 - distance / maxDistance);
                    final double frac = _fractionFrom(_pseudoRandomSeedFrom(rx, ry));
                    sum += (frac * relevance);
                    samples++;
                    height = ( height * (1.0 - relevance) ) + ( frac * relevance );
                }
        }
        return height;
        //height = 1 - sum/samples;
        // We increase the contrast of the noise by squaring the value
        //return 1-Math.pow(height, Math.max(1,samples/3));
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

    private static byte _fastPseudoRandomByteSeedFrom(float x, float y ) {
        long part1 = Float.floatToRawIntBits(x) * PRIME_1;
        long part2 = Float.floatToRawIntBits(y) * PRIME_2;
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

    public static void main(String[] args) {

        Set<Long> visited = new HashSet<>();

        final int size = 16;
        final int numberOfPoints = size*size;
        final int padLeft = 0;
        final int padRight = 1;
        int xmin = 0;
        int xmax = size;
        int ymin = 0;
        int ymax = size;
        int x = 0;
        int y = 0;
        int nextLayerStartIndex = 0;
        int lastLayerIndex = 0;

        for ( int i = 0; i < numberOfPoints; i++ ) {
            final int width  = Math.max(xmax - xmin - (padLeft+padRight), 0); // Minus 1 because one side is already taken
            final int height = Math.max(ymax - ymin - (padLeft+padRight), 0);
            final int currentLayerSize = ( width * 2 + height * 2 );
            final int li = i - lastLayerIndex;
            final int si = li / 4;
            final int mode = li % 4;
            final boolean top    = mode == 0;
            final boolean bottom = mode == 1;
            final boolean left   = mode == 2;
            final boolean right  = mode == 3;

            if ( nextLayerStartIndex == 0 )
                nextLayerStartIndex = currentLayerSize;

            boolean milestoneReached = ( i == nextLayerStartIndex - 1 );
            int upcomingMilestone = nextLayerStartIndex;

            if ( i == nextLayerStartIndex ) {
                upcomingMilestone += currentLayerSize;
            }
            nextLayerStartIndex = upcomingMilestone;

            if ( top ) {
                x = xmin + ( si % width ) + padLeft; // from left to right
                y = ymin;
            }
            if ( bottom ) {
                x = xmax - ( si % width ) - 1 - padLeft; // from right to left
                y = ymax - 1;
            }
            if ( left ) {
                x = xmin;
                y = ymax - ( si % height ) - 1 - padLeft; // from bottom to top
            }
            if ( right ) {
                x = xmax - 1;
                y = ymin + ( si % height ) + padLeft; // from top to bottom
            }
            //System.out.println( (1+i)+"/"+(size*size)+"-> " + mode + " : "+si+"?"+height+"->( " + x + " " + y + " ) |"+rowIsDone+"|"+i+":"+(nextLayerStartIndex)+"| xminmax: " + xmin + "<" + xmax + " yminmax: " + ymin + "<" + ymax + " || "+si+"=("+i+"-"+lastLayerIndex+")/4" );
            if ( milestoneReached ) {
                lastLayerIndex = i + 1;
                xmin++;
                xmax--;
                ymin++;
                ymax--;
                System.out.println("We go to next segment!!");
            }

            long posAsLong = ((long)x << 32) | (long)y;
            if ( visited.contains(posAsLong) ) {
                System.out.println("Already visited: " + x + " " + y);
            }
            visited.add(posAsLong);
        }
    }

}
