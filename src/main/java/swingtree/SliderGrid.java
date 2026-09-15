package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.api.model.SliderTicks;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;

final class SliderGrid
{
    private static final Logger log = LoggerFactory.getLogger(SliderGrid.class);

    static final int PREFERRED_STEPS = 256;
    static final int MAX_TICK_MARKS = 100_000;

    private final Class<? extends Number> _numberType;
    private final BigDecimal _min;
    private final BigDecimal _max;
    private final int _intMin;
    private final int _intMax;
    private final BigDecimal _numbersPerStepNumerator;
    private final long _numbersPerStepDenominator;
    private final int _majorSpacingInSteps;
    private final int _minorSpacingInSteps;
    private final @Nullable BigDecimal _majorSpacing;

    static SliderGrid of(
        Class<? extends Number> numberType,
        Number                  min,
        Number                  max,
        @Nullable SliderTicks<?> ticks
    ) {
        BigDecimal minimum = decimalOf(min);
        BigDecimal maximum = decimalOf(max).max(minimum);
        BigDecimal range = maximum.subtract(minimum);
        @Nullable BigDecimal majorSpacing = ticks == null ? null : ticks.majorSpacing().map(SliderGrid::decimalOf).orElse(null);
        int minorTicksBetween = ticks == null ? 0 : ticks.minorTicksBetween();
        if ( isWholeNumberType(numberType) )
            return _wholeNumberGrid(numberType, minimum, maximum, majorSpacing, minorTicksBetween);
        else
            return _fractionalGrid(numberType, minimum, maximum, range, majorSpacing, minorTicksBetween);
    }

    static SliderGrid ofFractionalTickSpacings(
        Class<? extends Number> numberType,
        Number                  min,
        Number                  max,
        @Nullable Number        majorTickSpacing,
        @Nullable Number        minorTickSpacing
    ) {
        BigDecimal minimum = decimalOf(min);
        BigDecimal maximum = decimalOf(max).max(minimum);
        BigDecimal range = maximum.subtract(minimum);
        @Nullable BigDecimal majorSpacing = _positiveDecimalOrNull(majorTickSpacing);
        @Nullable BigDecimal minorSpacing = _positiveDecimalOrNull(minorTickSpacing);
        @Nullable BigDecimal commonDivisor = _largestCommonDivisor(majorSpacing, minorSpacing);
        if ( range.signum() <= 0 || commonDivisor == null )
            return _fractionalGrid(numberType, minimum, maximum, range, null, 0);

        double partsInRange = range.divide(commonDivisor, MathContext.DECIMAL64).doubleValue();
        if ( partsInRange > MAX_TICK_MARKS ) {
            log.warn(SwingTree.get().logMarker(),
                    "A slider from {} to {} can only place major tick marks every {} and minor tick marks every {} exactly " +
                    "by dividing its range into parts of {}, which are more than {} parts. No tick marks are drawn.",
                    minimum.toPlainString(), maximum.toPlainString(), _plainOrZero(majorSpacing), _plainOrZero(minorSpacing),
                    commonDivisor.toPlainString(), MAX_TICK_MARKS
                );
            return _fractionalGrid(numberType, minimum, maximum, range, null, 0);
        }
        long stepsPerPart = Math.max(1, (long) Math.ceil(PREFERRED_STEPS / partsInRange));
        int intMax = _clampToInt(Math.round(partsInRange * stepsPerPart));
        int majorSteps = _clampToInt(_partsIn(majorSpacing, commonDivisor) * stepsPerPart);
        int minorSteps = _clampToInt(_partsIn(minorSpacing, commonDivisor) * stepsPerPart);
        return new SliderGrid(numberType, minimum, maximum, 0, intMax, commonDivisor, stepsPerPart, majorSteps, minorSteps, majorSpacing);
    }

    private static SliderGrid _wholeNumberGrid(
        Class<? extends Number> numberType,
        BigDecimal              min,
        BigDecimal              max,
        @Nullable BigDecimal    majorSpacing,
        int                     minorTicksBetween
    ) {
        int intMin = _clampToInt(min.longValue());
        int intMax = Math.max(intMin, _clampToInt(max.longValue()));
        int majorSteps = 0;
        int minorSteps = 0;
        if ( majorSpacing != null ) {
            if ( !_isWholeNumber(majorSpacing) ) {
                log.warn(SwingTree.get().logMarker(),
                        "A slider for whole numbers cannot draw tick marks every {}, because that is not a whole number. " +
                        "No tick marks are drawn.", majorSpacing.toPlainString()
                    );
                majorSpacing = null;
            } else if ( _tickMarkCount(intMax - (long) intMin, majorSpacing.longValue(), minorTicksBetween) > MAX_TICK_MARKS ) {
                _warnAboutTooManyTickMarks(majorSpacing, minorTicksBetween);
                majorSpacing = null;
            } else {
                majorSteps = _clampToInt(majorSpacing.longValue());
                if ( minorTicksBetween > 0 ) {
                    if ( majorSteps % (minorTicksBetween + 1) == 0 )
                        minorSteps = majorSteps / (minorTicksBetween + 1);
                    else
                        log.warn(SwingTree.get().logMarker(),
                                "A slider for whole numbers cannot fit {} minor tick marks between major tick marks {} apart, " +
                                "because they would not be whole numbers apart. No minor tick marks are drawn.",
                                minorTicksBetween, majorSteps
                            );
                }
            }
        }
        return new SliderGrid(numberType, min, max, intMin, intMax, BigDecimal.ONE, 1, majorSteps, minorSteps, majorSpacing);
    }

    private static SliderGrid _fractionalGrid(
        Class<? extends Number> numberType,
        BigDecimal              min,
        BigDecimal              max,
        BigDecimal              range,
        @Nullable BigDecimal    majorSpacing,
        int                     minorTicksBetween
    ) {
        if ( range.signum() <= 0 )
            return new SliderGrid(numberType, min, max, 0, 0, BigDecimal.ONE, 1, 0, 0, null);

        if ( majorSpacing != null ) {
            double finestTicksInRange = range.multiply(BigDecimal.valueOf(minorTicksBetween + 1L))
                                             .divide(majorSpacing, MathContext.DECIMAL64)
                                             .doubleValue();
            if ( finestTicksInRange > MAX_TICK_MARKS ) {
                _warnAboutTooManyTickMarks(majorSpacing, minorTicksBetween);
                majorSpacing = null;
            } else {
                long stepsPerFinestTick = Math.max(1, (long) Math.ceil(PREFERRED_STEPS / finestTicksInRange));
                long partsPerMajorSpacing = (minorTicksBetween + 1) * stepsPerFinestTick;
                int intMax = _clampToInt(Math.round(finestTicksInRange * stepsPerFinestTick));
                int majorSteps = _clampToInt(partsPerMajorSpacing);
                int minorSteps = minorTicksBetween > 0 ? _clampToInt(stepsPerFinestTick) : 0;
                return new SliderGrid(numberType, min, max, 0, intMax, majorSpacing, partsPerMajorSpacing, majorSteps, minorSteps, majorSpacing);
            }
        }
        return new SliderGrid(numberType, min, max, 0, PREFERRED_STEPS, range, PREFERRED_STEPS, 0, 0, null);
    }

    private SliderGrid(
        Class<? extends Number> numberType,
        BigDecimal              min,
        BigDecimal              max,
        int                     intMin,
        int                     intMax,
        BigDecimal              numbersPerStepNumerator,
        long                    numbersPerStepDenominator,
        int                     majorSpacingInSteps,
        int                     minorSpacingInSteps,
        @Nullable BigDecimal    majorSpacing
    ) {
        _numberType                = numberType;
        _min                       = min;
        _max                       = max;
        _intMin                    = intMin;
        _intMax                    = intMax;
        _numbersPerStepNumerator   = numbersPerStepNumerator;
        _numbersPerStepDenominator = numbersPerStepDenominator;
        _majorSpacingInSteps       = majorSpacingInSteps;
        _minorSpacingInSteps       = minorSpacingInSteps;
        _majorSpacing              = majorSpacing;
    }

    int intMin() { return _intMin; }

    int intMax() { return _intMax; }

    int majorSpacingInSteps() { return _majorSpacingInSteps; }

    int minorSpacingInSteps() { return _minorSpacingInSteps; }

    boolean isInRange( Number number ) {
        BigDecimal decimal = decimalOf(number);
        return decimal.compareTo(_min) >= 0 && decimal.compareTo(_max) <= 0;
    }

    int stepOf( Number number ) {
        double offset = decimalOf(number).subtract(_min).doubleValue();
        double steps = offset * _numbersPerStepDenominator / _numbersPerStepNumerator.doubleValue();
        long step = _intMin + Math.round(steps);
        return (int) Math.max(_intMin, Math.min(_intMax, step));
    }

    Number numberAt( int step ) {
        if ( step <= _intMin )
            return convert(_numberType, _min);
        if ( step >= _intMax )
            return convert(_numberType, _max);
        BigDecimal offset = _numbersPerStepNumerator
                                .multiply(BigDecimal.valueOf(step - (long) _intMin))
                                .divide(BigDecimal.valueOf(_numbersPerStepDenominator), MathContext.DECIMAL64);
        return convert(_numberType, _min.add(offset));
    }

    int majorTickCount() {
        if ( _majorSpacingInSteps <= 0 )
            return 0;
        return (int) ((_intMax - (long) _intMin) / _majorSpacingInSteps) + 1;
    }

    int stepOfMajorTick( int index ) {
        return _intMin + index * _majorSpacingInSteps;
    }

    BigDecimal numberAtMajorTick( int index ) {
        return _majorSpacing == null ? _min : _min.add(_majorSpacing.multiply(BigDecimal.valueOf(index)));
    }

    int nearestTick( int step ) {
        int spacing = _finestSpacingInSteps();
        if ( spacing <= 0 )
            return step;
        long ticks = Math.round((step - (long) _intMin) / (double) spacing);
        return _tickWithinRange(ticks);
    }

    int nextTickTowards( int step, int from ) {
        int spacing = _finestSpacingInSteps();
        if ( spacing <= 0 || step == from )
            return nearestTick(step);
        double ticks = (step - (long) _intMin) / (double) spacing;
        boolean towardsTheMaximum = step > from;
        int tick = _tickWithinRange(towardsTheMaximum ? (long) Math.ceil(ticks) : (long) Math.floor(ticks));
        boolean tickLiesBehindTheStart = towardsTheMaximum ? tick < from : tick > from;
        return tickLiesBehindTheStart ? from : tick;
    }

    private int _finestSpacingInSteps() {
        return _minorSpacingInSteps > 0 ? _minorSpacingInSteps : _majorSpacingInSteps;
    }

    private int _tickWithinRange( long tick ) {
        int spacing = _finestSpacingInSteps();
        long lastTick = (_intMax - (long) _intMin) / spacing;
        long clamped = Math.max(0, Math.min(lastTick, tick));
        return (int) (_intMin + clamped * spacing);
    }

    static boolean isWholeNumberType( Class<?> numberType ) {
        return numberType == Integer.class || numberType == Long.class || numberType == Short.class || numberType == Byte.class;
    }

    static BigDecimal decimalOf( Number number ) {
        if ( number instanceof BigDecimal )
            return (BigDecimal) number;
        if ( number instanceof Double || number instanceof Float ) {
            double asDouble = number.doubleValue();
            if ( Double.isNaN(asDouble) || Double.isInfinite(asDouble) )
                return BigDecimal.ZERO;
            return new BigDecimal(number.toString());
        }
        return BigDecimal.valueOf(number.longValue());
    }

    @SuppressWarnings("unchecked")
    static <N extends Number> N convert( Class<N> numberType, Number number ) {
        if ( numberType == Integer.class ) return (N) Integer.valueOf(number.intValue());
        if ( numberType == Long.class    ) return (N) Long.valueOf(number.longValue());
        if ( numberType == Short.class   ) return (N) Short.valueOf(number.shortValue());
        if ( numberType == Byte.class    ) return (N) Byte.valueOf(number.byteValue());
        if ( numberType == Float.class   ) return (N) Float.valueOf(number.floatValue());
        if ( numberType == Double.class  ) return (N) Double.valueOf(number.doubleValue());
        throw new IllegalArgumentException("Unsupported number type: " + numberType);
    }

    private static @Nullable BigDecimal _positiveDecimalOrNull( @Nullable Number number ) {
        if ( number == null )
            return null;
        BigDecimal decimal = decimalOf(number);
        return decimal.signum() > 0 ? decimal : null;
    }

    private static @Nullable BigDecimal _largestCommonDivisor( @Nullable BigDecimal a, @Nullable BigDecimal b ) {
        if ( a == null )
            return b;
        if ( b == null )
            return a;
        int scale = Math.max(0, Math.max(a.stripTrailingZeros().scale(), b.stripTrailingZeros().scale()));
        BigInteger divisor = a.setScale(scale).unscaledValue().gcd(b.setScale(scale).unscaledValue());
        return new BigDecimal(divisor, scale);
    }

    private static long _partsIn( @Nullable BigDecimal spacing, BigDecimal part ) {
        return spacing == null ? 0 : spacing.divide(part, MathContext.DECIMAL64).longValue();
    }

    private static String _plainOrZero( @Nullable BigDecimal decimal ) {
        return decimal == null ? "0" : decimal.toPlainString();
    }

    private static boolean _isWholeNumber( BigDecimal decimal ) {
        return decimal.signum() == 0 || decimal.stripTrailingZeros().scale() <= 0;
    }

    private static long _tickMarkCount( long range, long majorSpacing, int minorTicksBetween ) {
        if ( majorSpacing <= 0 )
            return 0;
        return (range / majorSpacing) * (minorTicksBetween + 1);
    }

    private static void _warnAboutTooManyTickMarks( BigDecimal majorSpacing, int minorTicksBetween ) {
        log.warn(SwingTree.get().logMarker(),
                "A major spacing of {} with {} minor tick marks between major tick marks puts more than {} tick marks " +
                "onto a slider. No tick marks are drawn.",
                majorSpacing.toPlainString(), minorTicksBetween, MAX_TICK_MARKS
            );
    }

    private static boolean _isSameDecimal( @Nullable BigDecimal a, @Nullable BigDecimal b ) {
        if ( a == null )
            return b == null;
        return b != null && a.compareTo(b) == 0;
    }

    private static int _clampToInt( long value ) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    @Override
    public boolean equals( @Nullable Object obj ) {
        if ( obj == this ) return true;
        if ( !(obj instanceof SliderGrid) ) return false;
        SliderGrid other = (SliderGrid) obj;
        return _numberType == other._numberType
            && _intMin == other._intMin
            && _intMax == other._intMax
            && _numbersPerStepDenominator == other._numbersPerStepDenominator
            && _majorSpacingInSteps == other._majorSpacingInSteps
            && _minorSpacingInSteps == other._minorSpacingInSteps
            && _min.compareTo(other._min) == 0
            && _max.compareTo(other._max) == 0
            && _numbersPerStepNumerator.compareTo(other._numbersPerStepNumerator) == 0
            && _isSameDecimal(_majorSpacing, other._majorSpacing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_numberType, _intMin, _intMax, _numbersPerStepDenominator, _majorSpacingInSteps, _minorSpacingInSteps, _min.doubleValue(), _max.doubleValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                    "numberType=" + _numberType.getSimpleName() + ", " +
                    "min=" + _min + ", max=" + _max + ", " +
                    "steps=" + _intMin + ".." + _intMax + ", " +
                    "majorSpacingInSteps=" + _majorSpacingInSteps + ", " +
                    "minorSpacingInSteps=" + _minorSpacingInSteps +
                "]";
    }
}
