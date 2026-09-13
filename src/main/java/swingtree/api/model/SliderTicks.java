package swingtree.api.model;

import org.jspecify.annotations.Nullable;
import sprouts.Association;
import sprouts.Pair;
import sprouts.Tuple;
import swingtree.api.IconDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 *  An immutable value describing the tick marks and the labels along a slider:
 *  where the major and minor tick marks are, whether they are drawn, whether the
 *  knob snaps to them, and which labels are shown at which numbers.
 *  Hand one of these to a slider through
 *  {@link swingtree.UIForSlider#withTicks(SliderTicks)}, or bind a property holding one through
 *  {@link swingtree.UIForSlider#withTicks(sprouts.Val)}:
 *  <pre>{@code
 *  UI.slider(UI.Axis.HORIZONTAL, 0, 100, volume)
 *  .withTicks(
 *      SliderTicks.of(Integer.class)
 *      .withMajorSpacing(25)
 *      .withMinorTicksBetween(4)
 *      .withLabelsAtMajorTicks( v -> v + "%" )
 *  );
 *  }</pre>
 *  This slider draws a major tick mark at 0, 25, 50, 75 and 100, labelled
 *  "0%", "25%", "50%", "75%" and "100%", and four minor tick marks between each
 *  pair of major tick marks, which puts a minor tick mark at every multiple of 5.
 *
 *  <h2>Numbers in the slider's own type</h2>
 *
 *  Every spacing and every label position is a number of the same type as the
 *  value of the slider, which is the {@code N} of this class. A slider for
 *  a {@code Double} property therefore has its tick marks declared in {@code Double}s:
 *  <pre>{@code
 *  UI.slider(UI.Axis.HORIZONTAL, 0.0, 1.0, opacity)
 *  .withTicks(
 *      SliderTicks.of(Double.class)
 *      .withMajorSpacing(0.25)
 *      .withTickMarksVisible(false)
 *      .withLabelsAtMajorTicks( v -> Math.round(v * 100) + "%" )
 *  );
 *  }</pre>
 *  A plain {@link javax.swing.JSlider} only knows whole numbers, so SwingTree
 *  maps fractional numbers onto a range of whole numbers behind the scenes. It
 *  chooses that range so that every tick mark lands exactly on one of its whole numbers.
 *
 *  <h2>Counting starts at the minimum</h2>
 *
 *  Tick marks, and the labels at the major tick marks, count from the minimum of
 *  the slider. On a slider running from 3 to 97 with a major spacing of 25, the major
 *  tick marks sit at 3, 28, 53 and 78. This is how every Swing look and feel draws tick
 *  marks, and a label is only useful where its tick mark is. A label which has to sit
 *  somewhere else can be placed at any number through {@link #withLabelAt(Number, String)}.
 *
 *  <h2>It is a value</h2>
 *
 *  Every method which sounds like it changes something returns a new {@link SliderTicks},
 *  leaving the one you called it on untouched. Two instances describing the same tick
 *  marks and labels are {@link #equals(Object)} to each other, which is how a slider bound
 *  to a property holding a {@link SliderTicks} knows whether anything actually changed.
 *  A label text function is compared by identity, so a method reference or a lambda
 *  which captures nothing is equal to itself every time it is evaluated.
 *  <p>
 *  Since this value holds no Swing component, it is safe to keep in a view model and
 *  to hand from the application thread to the UI thread.
 *  To declare a property of it, use {@link #classTyped(Class)}:
 *  <pre>{@code
 *  Val<SliderTicks<Integer>> ticks = showTicks.viewAs(
 *      SliderTicks.classTyped(Integer.class),
 *      show -> show ? SliderTicks.of(Integer.class).withMajorSpacing(10) : SliderTicks.of(Integer.class)
 *  );
 *  }</pre>
 *
 * @param <N> The number type of the slider, which is also the type of every spacing and label position.
 */
public final class SliderTicks<N extends Number>
{
    private static final Association<Number, Label> NO_LABELS = Association.betweenLinked(Number.class, Label.class);

    private final Class<N>                   _numberType;
    private final @Nullable N                _majorSpacing;
    private final int                        _minorTicksBetween;
    private final boolean                    _tickMarksVisible;
    private final boolean                    _snapsToTicks;
    private final boolean                    _labelsAtMajorTicks;
    private final @Nullable Function<N, String> _majorTickLabelText;
    private final Locale                     _labelLocale;
    private final Association<Number, Label> _labels;

    /**
     *  Creates a description of no tick marks and no labels for a slider whose value is
     *  of the given number type. This is where every {@link SliderTicks} starts out:
     *  <pre>{@code
     *  SliderTicks.of(Integer.class).withMajorSpacing(10)
     *  }</pre>
     *  The supported number types are {@link Integer}, {@link Long}, {@link Short},
     *  {@link Byte}, {@link Float} and {@link Double}, which are also the number types
     *  a slider can be bound to.
     *
     * @param numberType The type of the value of the slider, and therefore of every spacing and label position.
     * @param <N> The number type of the slider.
     * @return A {@link SliderTicks} with no tick marks and no labels.
     * @throws NullPointerException If {@code numberType} is {@code null}.
     * @throws IllegalArgumentException If {@code numberType} is not one of the supported number types.
     */
    public static <N extends Number> SliderTicks<N> of( Class<N> numberType ) {
        Objects.requireNonNull(numberType, "numberType");
        Class<N> wrapperType = _wrapperOf(numberType);
        return new SliderTicks<>(wrapperType, null, 0, true, false, false, null, Locale.ROOT, NO_LABELS);
    }

    /**
     *  An alternative to {@code SliderTicks.class} which keeps the number type in
     *  the type signature, so that you can declare a property holding a
     *  {@link SliderTicks} without casting:
     *  <pre>{@code
     *  Var<SliderTicks<Double>> ticks = Var.of(SliderTicks.classTyped(Double.class), SliderTicks.of(Double.class));
     *  }</pre>
     *
     * @param numberType The number type {@code N} in the returned {@code Class<SliderTicks<N>>}.
     * @param <N> The number type of the slider.
     * @return The {@code SliderTicks.class}, typed as {@code Class<SliderTicks<N>>}.
     * @throws NullPointerException If {@code numberType} is {@code null}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <N extends Number> Class<SliderTicks<N>> classTyped( Class<N> numberType ) {
        Objects.requireNonNull(numberType, "numberType");
        return (Class) SliderTicks.class;
    }

    private SliderTicks(
        Class<N>                      numberType,
        @Nullable N                   majorSpacing,
        int                           minorTicksBetween,
        boolean                       tickMarksVisible,
        boolean                       snapsToTicks,
        boolean                       labelsAtMajorTicks,
        @Nullable Function<N, String> majorTickLabelText,
        Locale                        labelLocale,
        Association<Number, Label>    labels
    ) {
        _numberType         = numberType;
        _majorSpacing       = majorSpacing;
        _minorTicksBetween  = minorTicksBetween;
        _tickMarksVisible   = tickMarksVisible;
        _snapsToTicks       = snapsToTicks;
        _labelsAtMajorTicks = labelsAtMajorTicks;
        _majorTickLabelText = majorTickLabelText;
        _labelLocale        = labelLocale;
        _labels             = labels;
    }

    /**
     *  Returns the type of the value of the slider, which is also the type of every spacing and label position.
     *
     * @return The number type of the slider.
     */
    public Class<N> numberType() {
        return _numberType;
    }

    /**
     *  Returns a copy with major tick marks at the given spacing, counted from the minimum
     *  of the slider. A spacing of 10 on a slider running from 0 to 100 puts a major tick
     *  mark at 0, 10, 20 and so on up to 100.
     *  The major spacing is also how far the knob moves when the user clicks into the
     *  track beside it, or presses page up or page down, unless there are minor tick
     *  marks, whose spacing is used instead.
     *  <p>
     *  A spacing of zero removes the tick marks, and with them the labels at the major tick marks.
     *
     * @param spacing The distance between two neighbouring major tick marks, in the numbers of the slider.
     * @return A new {@link SliderTicks} with the given major spacing.
     * @throws NullPointerException If {@code spacing} is {@code null}.
     * @throws IllegalArgumentException If {@code spacing} is negative, not a number or infinite.
     */
    public SliderTicks<N> withMajorSpacing( N spacing ) {
        Objects.requireNonNull(spacing, "spacing");
        double asDouble = spacing.doubleValue();
        if ( Double.isNaN(asDouble) || Double.isInfinite(asDouble) )
            throw new IllegalArgumentException("The major spacing of slider ticks must be a finite number, but was " + spacing + ".");
        if ( asDouble < 0 )
            throw new IllegalArgumentException("The major spacing of slider ticks must not be negative, but was " + spacing + ".");
        @Nullable N majorSpacing = asDouble == 0 ? null : spacing;
        if ( Objects.equals(majorSpacing, _majorSpacing) )
            return this;
        return new SliderTicks<>(_numberType, majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks, _labelsAtMajorTicks, _majorTickLabelText, _labelLocale, _labels);
    }

    /**
     *  Returns the distance between two neighbouring major tick marks, if there are tick marks.
     *
     * @return The major spacing, or an empty {@link Optional} if there are no tick marks.
     */
    public Optional<N> majorSpacing() {
        return Optional.ofNullable(_majorSpacing);
    }

    /**
     *  Returns a copy with the given number of minor tick marks between each pair of
     *  neighbouring major tick marks. Four minor tick marks between major tick marks
     *  which are 25 apart divide that distance into five equal parts, so a minor tick
     *  mark sits at every multiple of 5. Zero means that there are no minor tick marks.
     *  <p>
     *  Counting the tick marks between the major ones, instead of declaring a second
     *  spacing, makes it impossible to declare minor tick marks which miss the major ones.
     *  On a slider for whole numbers the major spacing must still be divisible into
     *  that many equal whole number parts: a major spacing of 25 with 3 minor tick marks
     *  between them would need a minor tick mark every 6.25, so SwingTree logs a warning
     *  and draws no minor tick marks.
     *
     * @param count The number of minor tick marks between two neighbouring major tick marks.
     * @return A new {@link SliderTicks} with the given number of minor tick marks.
     * @throws IllegalArgumentException If {@code count} is negative.
     */
    public SliderTicks<N> withMinorTicksBetween( int count ) {
        if ( count < 0 )
            throw new IllegalArgumentException("The number of minor tick marks between major tick marks must not be negative, but was " + count + ".");
        if ( count == _minorTicksBetween )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, count, _tickMarksVisible, _snapsToTicks, _labelsAtMajorTicks, _majorTickLabelText, _labelLocale, _labels);
    }

    /**
     *  Returns how many minor tick marks sit between two neighbouring major tick marks.
     *
     * @return The number of minor tick marks between two neighbouring major tick marks.
     */
    public int minorTicksBetween() {
        return _minorTicksBetween;
    }

    /**
     *  Returns a copy whose tick marks are drawn or not drawn, depending on the given flag.
     *  Tick marks are drawn by default.
     *  <p>
     *  Tick marks which are not drawn still exist: the labels at the major tick marks are
     *  still shown, and the knob still snaps to them if {@link #withSnapToTicks(boolean)}
     *  says so. This is how you build a slider with labels at 0, 25, 50, 75 and 100
     *  but without any marks on its track.
     *
     * @param visible {@code true} to draw the tick marks, {@code false} to only use their positions.
     * @return A new {@link SliderTicks} with the given visibility of its tick marks.
     */
    public SliderTicks<N> withTickMarksVisible( boolean visible ) {
        if ( visible == _tickMarksVisible )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, visible, _snapsToTicks, _labelsAtMajorTicks, _majorTickLabelText, _labelLocale, _labels);
    }

    /**
     *  Tells whether the tick marks are drawn, or whether only their positions are used.
     *
     * @return {@code true} if the tick marks are drawn, {@code false} if only their positions are used.
     */
    public boolean hasVisibleTickMarks() {
        return _tickMarksVisible;
    }

    /**
     *  Returns a copy whose tick marks the knob snaps to when the user moves it, or not,
     *  depending on the given flag. The knob snaps to the minor tick marks if there are
     *  any, and to the major tick marks otherwise. Without a major spacing there is nothing
     *  to snap to, and the flag has no effect.
     *  <p>
     *  While the user drags the knob, the value of the slider is the number at the nearest
     *  tick mark, and when the user lets go of the knob, it moves onto that tick mark.
     *  An arrow key moves the knob to the next tick mark in the direction of the key.
     *  <p>
     *  Snapping only ever applies to what the user does. When your application sets the
     *  value of the slider to a number between two tick marks, the knob sits between those
     *  tick marks, and the value is not changed behind your back.
     *
     * @param snap {@code true} to let the knob snap to the tick marks, {@code false} to let it move freely.
     * @return A new {@link SliderTicks} with the given snapping behaviour.
     */
    public SliderTicks<N> withSnapToTicks( boolean snap ) {
        if ( snap == _snapsToTicks )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, snap, _labelsAtMajorTicks, _majorTickLabelText, _labelLocale, _labels);
    }

    /**
     *  Tells whether the knob snaps to the tick marks when the user moves it.
     *
     * @return {@code true} if the knob snaps to the tick marks when the user moves it.
     */
    public boolean isSnappingToTicks() {
        return _snapsToTicks;
    }

    /**
     *  Returns a copy with a label at every major tick mark, showing the number at that
     *  tick mark. All labels show their numbers with the same count of decimal places,
     *  namely the fewest which write every one of them exactly: labels at 0, 0.25, 0.5,
     *  0.75 and 1 read "0.00", "0.25", "0.50", "0.75" and "1.00".
     *  <p>
     *  The numbers are written independently of the locale of the machine, with a dot as
     *  decimal separator and without grouping, so "1234.5" reads the same everywhere.
     *  To write them the way a particular locale does, use {@link #withLabelLocale(Locale)}.
     *  <p>
     *  The labels only exist while there is a major spacing, see {@link #withMajorSpacing(Number)}.
     *
     * @return A new {@link SliderTicks} with a number label at every major tick mark.
     */
    public SliderTicks<N> withLabelsAtMajorTicks() {
        if ( _labelsAtMajorTicks && _majorTickLabelText == null )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks, true, null, _labelLocale, _labels);
    }

    /**
     *  Returns a copy with a label at every major tick mark, whose text is computed from
     *  the number at that tick mark by the given function:
     *  <pre>{@code
     *  SliderTicks.of(Integer.class).withMajorSpacing(25).withLabelsAtMajorTicks( v -> v + "%" )
     *  }</pre>
     *  The function receives the exact number at the tick mark, computed as the minimum of
     *  the slider plus a whole multiple of the major spacing, so a slider from 0.0 with a
     *  major spacing of 0.1 hands 0.3 to the function, and never 0.30000000000000004.
     *  <p>
     *  The labels only exist while there is a major spacing, see {@link #withMajorSpacing(Number)}.
     *
     * @param text The function computing the text of a label from the number at its tick mark.
     * @return A new {@link SliderTicks} with a label at every major tick mark.
     * @throws NullPointerException If {@code text} is {@code null}.
     */
    public SliderTicks<N> withLabelsAtMajorTicks( Function<N, String> text ) {
        Objects.requireNonNull(text, "text");
        if ( _labelsAtMajorTicks && _majorTickLabelText == text )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks, true, text, _labelLocale, _labels);
    }

    /**
     *  Returns a copy without labels at the major tick marks. The labels placed at
     *  particular numbers through {@link #withLabelAt(Number, String)} are kept.
     *
     * @return A new {@link SliderTicks} without labels at the major tick marks.
     */
    public SliderTicks<N> withoutLabelsAtMajorTicks() {
        if ( !_labelsAtMajorTicks )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks, false, null, _labelLocale, _labels);
    }

    /**
     *  Tells whether there is a label at every major tick mark.
     *
     * @return {@code true} if there is a label at every major tick mark.
     */
    public boolean hasLabelsAtMajorTicks() {
        return _labelsAtMajorTicks;
    }

    /**
     *  Returns the function computing the text of the labels at the major tick marks,
     *  if one was supplied through {@link #withLabelsAtMajorTicks(Function)}.
     *
     * @return The label text function, or an empty {@link Optional} if the labels at the major
     *         tick marks show their numbers, or if there are no labels at the major tick marks.
     */
    public Optional<Function<N, String>> majorTickLabelText() {
        return Optional.ofNullable(_majorTickLabelText);
    }

    /**
     *  Returns a copy whose labels at the major tick marks write their numbers the way the
     *  given locale does. With {@link Locale#GERMANY} a label at 1234.5 reads "1.234,5",
     *  whereas with {@link Locale#US} it reads "1,234.5".
     *  <p>
     *  The default is {@link Locale#ROOT}, which writes numbers independently of any locale:
     *  with a dot as decimal separator and without grouping, so the same label reads "1234.5".
     *  Pass {@code Locale.getDefault()} to follow the locale of the machine.
     *  <p>
     *  The locale only affects labels showing numbers through {@link #withLabelsAtMajorTicks()}.
     *  A text function passed to {@link #withLabelsAtMajorTicks(Function)} does its own formatting.
     *
     * @param locale The locale whose conventions the number labels follow.
     * @return A new {@link SliderTicks} with the given label locale.
     * @throws NullPointerException If {@code locale} is {@code null}.
     */
    public SliderTicks<N> withLabelLocale( Locale locale ) {
        Objects.requireNonNull(locale, "locale");
        if ( locale.equals(_labelLocale) )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks, _labelsAtMajorTicks, _majorTickLabelText, locale, _labels);
    }

    /**
     *  Returns the locale whose conventions the number labels at the major tick marks follow.
     *
     * @return The label locale, which is {@link Locale#ROOT} unless {@link #withLabelLocale(Locale)} says otherwise.
     */
    public Locale labelLocale() {
        return _labelLocale;
    }

    /**
     *  Returns a copy with a text label at the given number, replacing any label which was
     *  at that number before. The number does not need to be at a tick mark:
     *  <pre>{@code
     *  SliderTicks.of(Integer.class)
     *  .withLabelAt(0, "Cold")
     *  .withLabelAt(50, "Warm")
     *  .withLabelAt(100, "Hot")
     *  }</pre>
     *  A label at a number outside the range of the slider is not shown.
     *  When a label at a particular number and a label at a major tick mark fall onto the
     *  same position, the label at the particular number is shown.
     *
     * @param value The number at which the label is shown.
     * @param text The text of the label.
     * @return A new {@link SliderTicks} with the given label.
     * @throws NullPointerException If {@code value} or {@code text} is {@code null}.
     */
    public SliderTicks<N> withLabelAt( N value, String text ) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(text, "text");
        return _withLabel(value, new Label(text, null));
    }

    /**
     *  Returns a copy with an icon label at the given number, replacing any label which was
     *  at that number before. This is how a volume slider shows a muted speaker at its start
     *  and a loud speaker at its end:
     *  <pre>{@code
     *  SliderTicks.of(Integer.class)
     *  .withLabelAt(0, Icons.MUTED)
     *  .withLabelAt(100, Icons.LOUD)
     *  }</pre>
     *  A label at a number outside the range of the slider is not shown.
     *  When a label at a particular number and a label at a major tick mark fall onto the
     *  same position, the label at the particular number is shown.
     *
     * @param value The number at which the label is shown.
     * @param icon The icon of the label.
     * @return A new {@link SliderTicks} with the given label.
     * @throws NullPointerException If {@code value} or {@code icon} is {@code null}.
     */
    public SliderTicks<N> withLabelAt( N value, IconDeclaration icon ) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(icon, "icon");
        return _withLabel(value, new Label(null, icon));
    }

    /**
     *  Returns a copy without the label at the given number, if there is one.
     *  The labels at the major tick marks are not affected.
     *
     * @param value The number of the label to remove.
     * @return A new {@link SliderTicks} without a label at the given number.
     * @throws NullPointerException If {@code value} is {@code null}.
     */
    public SliderTicks<N> withoutLabelAt( N value ) {
        Objects.requireNonNull(value, "value");
        if ( !_labels.containsKey(value) )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks, _labelsAtMajorTicks, _majorTickLabelText, _labelLocale, _labels.remove(value));
    }

    /**
     *  Returns the numbers at which a label was placed through {@link #withLabelAt(Number, String)}
     *  or {@link #withLabelAt(Number, IconDeclaration)}, in the order they were placed.
     *
     * @return The positions of the labels placed at particular numbers.
     */
    @SuppressWarnings("unchecked")
    public Tuple<N> labelPositions() {
        List<N> positions = new ArrayList<>(_labels.size());
        for ( Pair<Number, Label> entry : _labels )
            positions.add((N) entry.first());
        return Tuple.of(_numberType, positions);
    }

    /**
     *  Returns the text of the label placed at the given number, if it is a text label.
     *
     * @param value The number of the label.
     * @return The text of the label at the given number, or an empty {@link Optional}
     *         if there is no label at that number, or if it is an icon label.
     */
    public Optional<String> labelTextAt( N value ) {
        Objects.requireNonNull(value, "value");
        return _labels.get(value).map( label -> label.text );
    }

    /**
     *  Returns the icon of the label placed at the given number, if it is an icon label.
     *
     * @param value The number of the label.
     * @return The icon of the label at the given number, or an empty {@link Optional}
     *         if there is no label at that number, or if it is a text label.
     */
    public Optional<IconDeclaration> labelIconAt( N value ) {
        Objects.requireNonNull(value, "value");
        return _labels.get(value).map( label -> label.icon );
    }

    private SliderTicks<N> _withLabel( N value, Label label ) {
        if ( _labels.get(value).map(label::equals).orElse(false) )
            return this;
        return new SliderTicks<>(_numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks, _labelsAtMajorTicks, _majorTickLabelText, _labelLocale, _labels.put(value, label));
    }

    @SuppressWarnings("unchecked")
    private static <N extends Number> Class<N> _wrapperOf( Class<N> numberType ) {
        if ( numberType == int.class    || numberType == Integer.class ) return (Class<N>) Integer.class;
        if ( numberType == long.class   || numberType == Long.class    ) return (Class<N>) Long.class;
        if ( numberType == short.class  || numberType == Short.class   ) return (Class<N>) Short.class;
        if ( numberType == byte.class   || numberType == Byte.class    ) return (Class<N>) Byte.class;
        if ( numberType == float.class  || numberType == Float.class   ) return (Class<N>) Float.class;
        if ( numberType == double.class || numberType == Double.class  ) return (Class<N>) Double.class;
        throw new IllegalArgumentException(
                "A slider supports the number types Integer, Long, Short, Byte, Float and Double, but not " + numberType.getName() + "."
            );
    }

    @Override
    public boolean equals( @Nullable Object obj ) {
        if ( obj == this ) return true;
        if ( !(obj instanceof SliderTicks) ) return false;
        SliderTicks<?> other = (SliderTicks<?>) obj;
        return _numberType         == other._numberType
            && _minorTicksBetween  == other._minorTicksBetween
            && _tickMarksVisible   == other._tickMarksVisible
            && _snapsToTicks       == other._snapsToTicks
            && _labelsAtMajorTicks == other._labelsAtMajorTicks
            && _majorTickLabelText == other._majorTickLabelText
            && Objects.equals(_majorSpacing, other._majorSpacing)
            && _labelLocale.equals(other._labelLocale)
            && _labels.equals(other._labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                    _numberType, _majorSpacing, _minorTicksBetween, _tickMarksVisible, _snapsToTicks,
                    _labelsAtMajorTicks, System.identityHashCode(_majorTickLabelText), _labelLocale, _labels
                );
    }

    @Override
    public String toString() {
        String majorTickLabels = !_labelsAtMajorTicks ? "none" : ( _majorTickLabelText == null ? "numbers" : "text" );
        String locale = _labelLocale.equals(Locale.ROOT) ? "ROOT" : _labelLocale.toLanguageTag();
        return getClass().getSimpleName() + "[" +
                    "numberType="         + _numberType.getSimpleName() + ", " +
                    "majorSpacing="       + _majorSpacing               + ", " +
                    "minorTicksBetween="  + _minorTicksBetween          + ", " +
                    "tickMarksVisible="   + _tickMarksVisible           + ", " +
                    "snapToTicks="        + _snapsToTicks               + ", " +
                    "majorTickLabels="    + majorTickLabels             + ", " +
                    "labelLocale="        + locale                      + ", " +
                    "labels="             + _labels                     +
                "]";
    }

    private static final class Label
    {
        final @Nullable String          text;
        final @Nullable IconDeclaration icon;

        Label( @Nullable String text, @Nullable IconDeclaration icon ) {
            this.text = text;
            this.icon = icon;
        }

        @Override
        public boolean equals( @Nullable Object obj ) {
            if ( obj == this ) return true;
            if ( !(obj instanceof Label) ) return false;
            Label other = (Label) obj;
            return Objects.equals(text, other.text) && Objects.equals(icon, other.icon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, icon);
        }

        @Override
        public String toString() {
            return text != null ? "\"" + text + "\"" : String.valueOf(icon);
        }
    }
}
