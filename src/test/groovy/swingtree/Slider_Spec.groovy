package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Var
import swingtree.api.model.SliderTicks
import utility.LogSpy

import javax.swing.JSlider

@Title("Sliders")
@Narrative('''

    Sliders are a common way to allow the user to select a value
    within a range. Swing-Tree provides a way to create sliders
    in a declarative way, allowing you to configure the slider
    and then show it to the user.
    
    Sliders can be vertical or horizontal, and can have a
    minimum and maximum value, as well as a current value.
    You can bind any of these values to properties in your
    application, so that the slider can update itself
    automatically when your application state changes.

''')
@Subject([UIForSlider, UI])
@CompileDynamic
class Slider_Spec extends Specification
{
    def 'Use the `slider(Axis)` factory method to build a `JSlider`.'()
    {
        reportInfo """
            The `slider(Axis)` factory method returns a builder instance
            which can be used to configure the JSlider instance
            using method chaining.
            Use the "show()" method at the end of your chain to
            show the JSlider to the user.
        """
        given :
            var record = []
        and : 'Then we create UI declaration for the `JSlider` component.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL)
                    .withMin(0)
                    .withMax(100)
                    .withValue(50)
                    .onChange( it -> record << "value changed to ${it.get().value}" )
                    .get(JSlider)

        expect :
            slider.minimum == 0
            slider.maximum == 100
            slider.value == 50

        when :
            UI.runNow({ slider.value = 75 })

        then :
            record == ["value changed to 75"]
    }

    def 'Bind the current slider state to a double property through one of its factory methods.'()
    {
        reportInfo """
            You can use the `slider(Axis, N, N, Var<N>)` factory method
            to bind a number based property to the slider's current value.
            This includes floating point numbers, like `double` or `float`.

            When the slider's value changes through user interaction,
            the bound property will be updated
            and conversely, when the property changes, the slider's
            value will be updated.

            A `JSlider` only knows whole numbers, so SwingTree translates your
            fractional numbers into a range of whole numbers behind the scenes.
            Which whole numbers those are is SwingTree's own business, so this
            scenario never looks at them. It only looks at where the knob sits
            as a fraction of the way along the slider, which is what the user
            actually sees on screen. The slider is roughly 256 whole numbers
            long, so the knob may be up to 1/256 of the way away from the exact
            position of a number.
        """
        given : 'We create a double based Sprouts property and a list for recording changes.'
            var currentState = Var.of(0.42d)
            var trace = []
            currentState.onChange(From.ALL, it -> trace << "property=${it.currentValue().orElseThrowUnchecked()}" )
        and : 'Then we build a declarative JSlider running from -2 to 2.'
            var slider = UI.slider(UI.Axis.HORIZONTAL, -2d, 2d, currentState).get(JSlider)
        when : 'We measure how far along the slider the knob sits.'
            var knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The value 0.42 is 2.42 away from -2, and the slider is 4 long, so the knob sits 2.42 / 4 = 60.5% of the way along.'
            Math.abs(knobFraction - 0.605d) <= 1d / 256

        when : 'The user moves the knob 87.5% of the way along the slider.'
            UI.runNow({ slider.value = slider.minimum + (int) ((slider.maximum - slider.minimum) * 0.875d) })
        then : 'The property now holds the number at that point: -2 + 0.875 * 4 = 1.5.'
            trace == ["property=1.5"]

        when : 'The application changes the property to 0.75.'
            UI.runNow({ currentState.set(0.75d) })
            knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The trace shows the property change.'
            trace == ["property=1.5", "property=0.75"]
        and : 'The knob follows it to 2.75 / 4 = 68.75% of the way along.'
            Math.abs(knobFraction - 0.6875d) <= 1d / 256
    }

    def 'Bind the full slider state to a double property through one of its factory methods.'()
    {
        reportInfo """
            You can use the `slider(Axis, Val<N>, Val<N>, Var<N>)` factory method
            to bind min, max and value properties to the slider's current state.
            This works for any `Number` based property, including `double` or `float`.

            When the slider's value changes through user interaction,
            the bound property will be updated and conversely, when the property changes,
            the slider's value will be updated.

            The min and max range may not be updated by the user, but they can be
            updated programmatically through the bound properties.

            A `JSlider` only knows whole numbers, so SwingTree translates your
            fractional numbers into a range of whole numbers behind the scenes,
            and it translates them again whenever the min or the max changes.
            This scenario only looks at where the knob sits as a fraction of the
            way along the slider, which is what the user sees on screen. The slider
            is roughly 256 whole numbers long, so the knob may be up to 1/256 of
            the way away from the exact position of a number, and a number written
            back by the slider may be up to 1/256 of the slider's length away from
            the number the user pointed at.
        """
        given : 'We create a double based Sprouts property and a list for recording changes.'
            var currentState = Var.of(0.42d)
            var trace = []
            currentState.onChange(From.ALL, it -> trace << "property=${it.currentValue().orElseThrowUnchecked()}" )
        and : 'Two more properties for min and max'
            var min = Var.of(-3d)
            var max = Var.of(4d)
            min.onChange(From.ALL, it -> trace << "min=${it.currentValue().orElseThrowUnchecked()}" )
            max.onChange(From.ALL, it -> trace << "max=${it.currentValue().orElseThrowUnchecked()}" )
        and : 'Then we build a declarative JSlider running from -3 to 4.'
            var slider = UI.slider(UI.Axis.VERTICAL, min, max, currentState).get(JSlider)
        when : 'We measure how far along the slider the knob sits.'
            var knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The value 0.42 is 3.42 away from -3, and the slider is 7 long, so the knob sits 3.42 / 7 = 48.86% of the way along.'
            Math.abs(knobFraction - 3.42d / 7) <= 1d / 256

        when : 'The user moves the knob to where 2.5 is: 5.5 / 7 = 78.57% of the way along.'
            UI.runNow({ slider.value = slider.minimum + Math.round((slider.maximum - slider.minimum) * 5.5d / 7) as int })
        then : 'The property holds a number within 7 / 256 = 0.027 of 2.5.'
            trace.size() == 1
            Math.abs(currentState.get() - 2.5d) <= 7d / 256

        when : 'The application changes the property to 0.75.'
            trace.clear()
            UI.runNow({ currentState.set(0.75d) })
        then : 'The trace shows the property change.'
            trace == ["property=0.75"]

        when : 'The application changes the min to -1, so the slider is now 5 long.'
            UI.runNow({ min.set(-1d) })
            knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The trace shows the min change, and nothing else: the value stays 0.75.'
            trace == ["property=0.75", "min=-1.0"]
        and : 'The knob moves to 1.75 / 5 = 35% of the way along.'
            Math.abs(knobFraction - 0.35d) <= 1d / 256

        when : 'The application changes the max to 2, so the slider is now 3 long.'
            UI.runNow({ max.set(2d) })
            knobFraction = (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
        then : 'The trace shows the max change, and the value is still 0.75.'
            trace == ["property=0.75", "min=-1.0", "max=2.0"]
        and : 'The knob moves to 1.75 / 3 = 58.33% of the way along.'
            Math.abs(knobFraction - 1.75d / 3) <= 1d / 256
    }

    def 'A slider bound to a property writes numbers of the property\'s own type back into it.'(
        Class<?> type, Var<? extends Number> value, Number min, Number max
    ) {
        reportInfo """
            The `slider(Axis, N, N, Var<N>)` factory method accepts any of the number
            types `Integer`, `Long`, `Short`, `Byte`, `Float` and `Double`.
            Whatever type you choose for your property, the slider writes numbers of
            exactly that type back into it. A `Var<Float>` which suddenly held a
            `Double` would break the application code reading it with a
            `ClassCastException`, far away from the slider that caused it.

            In this scenario the user drags the knob to both ends of the slider,
            and the property has to hold the exact minimum and maximum afterwards,
            as numbers of its own type.
        """
        given : 'A slider bound to the property.'
            var slider = UI.slider(UI.Axis.HORIZONTAL, min, max, value).get(JSlider)

        when : 'The user drags the knob all the way to the end.'
            UI.runNow({ slider.value = slider.maximum })
        then : 'The property holds the maximum, as a number of its own type.'
            value.get() == max
            type.isInstance(value.get())

        when : 'The user drags the knob all the way to the start.'
            UI.runNow({ slider.value = slider.minimum })
        then : 'The property holds the minimum, as a number of its own type.'
            value.get() == min
            type.isInstance(value.get())

        where :
            type    | value                   | min             | max
            Integer | Var.of(5)               | 0               | 10
            Long    | Var.of(5L)              | 0L              | 10L
            Short   | Var.of((short) 5)       | (short) 0       | (short) 10
            Byte    | Var.of((byte) 5)        | (byte) 0        | (byte) 10
            Float   | Var.of(0.5f)            | 0f              | 1f
            Double  | Var.of(0.5d)            | -0.25d          | 1.75d
    }

    def 'Moving the slider after dynamic min/max changes keeps the bound value within [min, max].'()
    {
        reportInfo """
            This is a regression test for a bug where the slider's internal
            scaling used for non-whole-number bindings could become stale when
            one of the `min` or `max` properties changed dynamically.
            When the user then moved the slider knob, the resulting value
            written into the bound property could fall outside the current
            `[min, max]` range.
        """
        given : 'A bidirectional slider bound to three double properties.'
            var min = Var.of(5d)
            var max = Var.of(10d)
            var value = Var.of(7.5d)
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, min, max, value)
                      .get(JSlider)
        and : 'We compute the slider integer range after dropping the max.'
            var newMax = 6d
        when : 'The application shrinks the max property significantly.'
            UI.runNow({ max.set(newMax) })
        and : 'The user then drags the slider knob to a position near its minimum.'
            UI.runNow({ slider.value = slider.minimum + (slider.maximum - slider.minimum) / 4 as int })
        then : 'The bound value stays within the new [min, max] range.'
            value.get() >= min.get()
            value.get() <= max.get()
        when : 'The user drags the slider knob all the way to the top.'
            UI.runNow({ slider.value = slider.maximum })
        then : 'The bound value equals the user-defined maximum (within rounding).'
            value.get() == max.get()
        when : 'The user drags the slider knob all the way to the bottom.'
            UI.runNow({ slider.value = slider.minimum })
        then : 'The bound value equals the user-defined minimum (within rounding).'
            value.get() == min.get()
    }

    def 'While the user holds the knob of a slider, changes of its value property do not move the knob.'()
    {
        reportInfo """
            Think of the position slider of a video player. While the video plays, your
            application moves the knob forward by setting the position property, many times
            a second. When the user wants to watch another part of the video, they grab the
            knob and drag it there. The video keeps playing during the drag, so your
            application keeps setting the position property.

            Those changes must not move the knob while the user holds it. The number under
            the knob is the number the user is choosing, and when they let go, that number
            has to end up in the property. After that, the slider follows the property again.

            Here is what would go wrong otherwise. The user drags the knob from 10 to 70.
            Meanwhile the video plays on, and your application sets the position to 11, then
            to 12. If the slider took over the 12, it would report 12 when the user lets go,
            because 12 is the number it holds at that moment, and it would write 12 into the
            property. The video would carry on at 12 seconds, as if the user had never dragged
            the knob at all.

            A `JSlider` knows when the user holds its knob through a flag named
            `valueIsAdjusting`. The look and feel, which is the part of Swing that draws the
            slider and handles the mouse, sets that flag to `true` when the user presses the
            mouse button on the knob, and back to `false` when they release it. This scenario plays the user by
            setting that flag and the positions of the drag, just like the look and feel does
            when it handles the mouse.
        """
        given : 'A property holding the position in a video, in seconds.'
            var position = Var.of(10)
        and : 'A slider for a video which is 100 seconds long, bound to the property.'
            var slider = UI.slider(UI.Axis.HORIZONTAL, 0, 100, position).get(JSlider)

        when : 'The user grabs the knob and drags it to 70.'
            UI.runNow({
                slider.valueIsAdjusting = true
                slider.value = 70
            })
        then : 'The property follows the knob.'
            position.get() == 70

        when : 'The video keeps playing, and the application sets the position to 11, and then to 12.'
            UI.runNow({
                position.set(11)
                position.set(12)
            })
        then : 'The knob stays at 70, where the user holds it.'
            slider.value == 70

        when : 'The user lets go of the knob.'
            UI.runNow({ slider.valueIsAdjusting = false })
        then : 'The property holds 70, the position the user chose, and the knob is still there.'
            position.get() == 70
            slider.value == 70

        when : 'The video plays on from there, and the application sets the position to 71.'
            UI.runNow({ position.set(71) })
        then : 'The knob follows the property again.'
            slider.value == 71
    }

    def 'Clicking the knob of a slider for fractional numbers without moving it keeps the exact number in the property.'()
    {
        reportInfo """
            A `JSlider` only knows whole numbers. So a slider for `Double` numbers maps its
            numbers onto a range of whole numbers of its own choosing, and most of your numbers
            fall between two of those whole numbers. Say a slider from 0.0 to 1.0 runs over the
            whole numbers from 0 to 256. The number 0.42 then belongs at 0.42 * 256 = 107.52,
            and the knob can only sit at the closest whole number, 108. Going back the other way,
            the number at 108 is 108 / 256 = 0.421875.

            That is fine for showing the knob, which sits less than one 256th of the length of
            the slider away from where 0.42 belongs. But your property must keep 0.42 until the
            user actually moves the knob, and when the user does move it, the property receives
            the number at the new position of the knob.

            A click on the knob is not a move. A `JSlider` has a flag named `valueIsAdjusting`.
            The look and feel, which is the part of Swing that draws the slider and handles the
            mouse, sets that flag to `true` when the user presses the mouse button on the knob,
            and back to `false` when they release it. The slider reports each change of that
            flag to its change listeners, just like a change of its number.
            A slider which answered each of those two reports by writing the number at the knob
            into your property would replace your 0.42 with 0.421875, although the user did
            nothing but click.

            This scenario plays the user by setting `valueIsAdjusting` and the value of the
            slider, just like the look and feel does when it handles the mouse.
        """
        given : 'A property holding 0.42, and a slider from 0.0 to 1.0 bound to it.'
            var opacity = Var.of(0.42d)
            var slider = UI.slider(UI.Axis.HORIZONTAL, 0.0d, 1.0d, opacity).get(JSlider)

        when : 'The user presses the mouse button on the knob.'
            UI.runNow({ slider.valueIsAdjusting = true })
        then : 'The property still holds exactly 0.42.'
            opacity.get() == 0.42d

        when : 'The user releases the mouse button, without having moved the mouse.'
            UI.runNow({ slider.valueIsAdjusting = false })
        then : 'The property still holds exactly 0.42.'
            opacity.get() == 0.42d

        when : 'The user drags the knob by the smallest step there is, one whole number to the right.'
            UI.runNow({
                slider.valueIsAdjusting = true
                slider.value = slider.value + 1
                slider.valueIsAdjusting = false
            })
        then : 'Now the property holds the number at the new position of the knob.'
            opacity.get() == (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum)
            opacity.get() > 0.42d
    }

    def 'A minimum or maximum set on the `JSlider` itself becomes the range of a slider for whole numbers.'()
    {
        reportInfo """
            SwingTree builds a real `JSlider`, and that slider is yours to use. Maybe you hand
            it to an older part of your application, which configures it the Swing way, through
            `setMinimum(..)` and `setMaximum(..)`. On a slider for whole numbers, such as
            `Integer` or `Long` numbers, those calls simply work. The new minimum or maximum
            becomes the range of the slider, the tick marks and labels are laid out again for
            that range, and the range stays until something changes it again.

            When the new range no longer contains the number of the slider, the `JSlider` itself
            moves the knob to the nearest end of the new range. The value property then receives
            the number at the knob, just as if the user had moved the knob there, so that the
            property and the knob agree.

            This needs care, because SwingTree keeps its own copy of the minimum, the maximum
            and the value of a slider. It lays out the tick marks from that copy, and whenever
            your application changes the value, it writes the minimum, the maximum and the value
            into the slider at once. If SwingTree did not notice a call to `setMaximum(200)`,
            its copy would still say 100. The labels would stop at 100, and the next time the
            application set the value, SwingTree would write its old maximum of 100 back into
            the slider, and the 200 would be gone without a trace.

            A slider for `Double` or `Float` numbers is different, because its whole numbers
            are not numbers of your application, so there SwingTree undoes such a call.
        """
        given : 'A property holding 50.'
            var value = Var.of(50)
        and : 'A slider from 0 to 100 bound to the property, with a label every 50.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, value)
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(50)
                        .withLabelsAtMajorTicks()
                    )
                    .get(JSlider)
        expect : 'The labels read 0, 50 and 100.'
            slider.labelTable.collectEntries { number, label -> [number, label.text] } == [0: "0", 50: "50", 100: "100"]

        when : 'Some Swing code sets the maximum of the `JSlider` to 200.'
            UI.runNow({ slider.maximum = 200 })
        then : 'The slider runs to 200, and the labels go on up to 200.'
            slider.maximum == 200
            slider.labelTable.collectEntries { number, label -> [number, label.text] } == [0: "0", 50: "50", 100: "100", 150: "150", 200: "200"]

        when : 'The application sets the value to 150.'
            UI.runNow({ value.set(150) })
        then : 'The knob moves to 150, and the slider still runs to 200.'
            slider.value == 150
            slider.maximum == 200

        when : 'Some Swing code sets the maximum to 120, below the knob.'
            UI.runNow({ slider.maximum = 120 })
        then : 'The `JSlider` moved the knob back to 120, and the property received the 120.'
            slider.value == 120
            value.get() == 120

        when : 'Some Swing code sets the minimum to 20.'
            UI.runNow({ slider.minimum = 20 })
        then : 'Tick marks count from the minimum of a slider, so the labels now sit at 20, 70 and 120.'
            slider.labelTable.collectEntries { number, label -> [number, label.text] } == [20: "20", 70: "70", 120: "120"]
        and : 'The knob and the property stay at 120.'
            slider.value == 120
            value.get() == 120
    }

    def 'A slider for fractional numbers undoes a minimum or maximum set on its `JSlider` right away.'()
    {
        reportInfo """
            A `JSlider` only knows whole numbers. So a slider for `Double` or `Float` numbers
            maps its numbers onto a range of whole numbers of SwingTree's own choosing. Say
            SwingTree lets a slider from 0.0 to 1.0 run over the whole numbers from 0 to 256.
            Then the number 0.5 sits at the whole number 128, halfway along.

            Those whole numbers are SwingTree's business, and they are not numbers of your
            application. If some Swing code calls `setMaximum(512)` on such a slider, there is
            nothing sensible the call could mean: the slider should not run to 512, because
            512 is not a number of the slider, and it should not run to 2.0 either, because
            nobody asked for 2.0. So SwingTree puts back the range of whole numbers it chose,
            right away, and the slider keeps running from 0.0 to 1.0. You change the range of
            such a slider through the numbers of your application instead, with `withMin(..)`
            and `withMax(..)`, or with properties for the minimum and maximum.

            It has to happen right away. If the doubled range stayed in the slider until the
            next time your application sets the value, the knob for 0.5 would sit at 128 of
            512 until then, a quarter of the way along the slider instead of halfway.

            This scenario does not depend on which whole numbers SwingTree chose. It remembers
            them before the Swing code changes the range, and compares against them afterwards.
        """
        given : 'A property holding 0.5, and a slider from 0.0 to 1.0 bound to it.'
            var opacity = Var.of(0.5d)
            var slider = UI.slider(UI.Axis.HORIZONTAL, 0.0d, 1.0d, opacity).get(JSlider)
        and : 'We remember the range of whole numbers which SwingTree chose for the slider.'
            var minimum = slider.minimum
            var maximum = slider.maximum

        when : 'Some Swing code doubles the maximum of the `JSlider`.'
            UI.runNow({ slider.maximum = maximum * 2 })
        then : 'The slider runs over the whole numbers SwingTree chose, as before.'
            slider.minimum == minimum
            slider.maximum == maximum
        and : 'The knob still sits halfway along the slider, and the property still holds 0.5.'
            (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum) == 0.5d
            opacity.get() == 0.5d

        when : 'Some Swing code moves the minimum of the `JSlider` a quarter of the way along.'
            UI.runNow({ slider.minimum = minimum + (maximum - minimum).intdiv(4) })
        then : 'The slider runs over the whole numbers SwingTree chose, as before.'
            slider.minimum == minimum
            slider.maximum == maximum
        and : 'The knob still sits halfway along the slider, and the property still holds 0.5.'
            (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum) == 0.5d
            opacity.get() == 0.5d
    }

    def 'Use `withMajorTickSpacing(N)` and `withMinorTickSpacing(N)` to set how far apart the tick marks of a slider are.'()
    {
        reportInfo """
            A `JSlider` has two settings for its tick marks, `setMajorTickSpacing(int)` and
            `setMinorTickSpacing(int)`, which tell it how far apart its long and its short tick
            marks are. The builder of a slider has the same two settings, `withMajorTickSpacing(N)`
            and `withMinorTickSpacing(N)`, where `N` is the number type of the slider. The sliders
            in this scenario are built from a plain `JSlider`, so their number type is `Integer`.

            Just like the settings of a `JSlider`, the two methods only say where the tick marks are.
            They do not draw anything. A `JSlider` draws its tick marks once its `paintTicks` flag
            is `true`, and you can switch that flag on through `peek(..)`, which hands you the
            `JSlider` while the slider is built.

            This means that the two methods never change how a slider looks on their own. A slider
            which never switches on its `paintTicks` flag shows no tick marks, whatever its spacings are.
            A slider which does switch it on shows its tick marks at the spacings you gave it.
        """
        given : 'A slider from 1 to 100 with a major tick mark every 10 and a minor tick mark every 5.'
            var plain =
                    UI.of(new JSlider())
                    .withMajorTickSpacing(10)
                    .withMinorTickSpacing(5)
                    .withMin(1)
                    .get(JSlider)
        and : 'The same slider, except that it switches on the painting of its tick marks through `peek(..)`.'
            var painted =
                    UI.of(new JSlider())
                    .withMajorTickSpacing(10)
                    .withMinorTickSpacing(5)
                    .withMin(1)
                    .peek( s -> s.setPaintTicks(true) )
                    .get(JSlider)
        expect : 'Both sliders run from 1 to 100, with their tick marks 10 and 5 apart.'
            plain.minimum == 1
            plain.maximum == 100
            plain.majorTickSpacing == 10
            plain.minorTickSpacing == 5
            painted.minimum == 1
            painted.maximum == 100
            painted.majorTickSpacing == 10
            painted.minorTickSpacing == 5
        and : 'The first slider draws no tick marks and shows no labels, because nothing switched them on.'
            !plain.paintTicks
            !plain.paintLabels
            plain.labelTable == null
        and : 'The second slider draws its tick marks.'
            painted.paintTicks
    }

    def 'The tick spacings of a slider for fractional numbers put every tick mark exactly at its number.'()
    {
        reportInfo """
            On a slider for `Double` numbers, `withMajorTickSpacing(N)` and `withMinorTickSpacing(N)`
            take `Double` numbers. In this scenario the slider runs from 0.0 to 1.0, with a major tick
            mark every 0.25 and a minor tick mark every 0.1.

            A `JSlider` only knows whole numbers, so SwingTree maps the numbers of the slider onto a
            range of whole numbers behind the scenes, and a `JSlider` can only draw a tick mark at one
            of those whole numbers. If that range ran from 0 to 256, a minor tick mark every 0.1 would
            have to sit every 25.6 whole numbers, and 25.6 is not a whole number. Rounding it to 26 would
            move every minor tick mark a little further away from its number than the one before it.
            The ninth minor tick mark would sit at 9 * 26 = 234, which is 234 / 256 = 0.914 of the way
            along the slider, where it should sit at 0.9.

            So SwingTree chooses the range of whole numbers to fit both spacings. Both 0.25 and 0.1 are
            whole multiples of 0.05, and 0.05 fits exactly 20 times into a slider from 0.0 to 1.0. When the
            range of whole numbers is a whole multiple of 20 long, every tick mark sits on a whole number.

            We do not want this scenario to depend on the range SwingTree chose, though. What your users
            see is where a tick mark sits along the slider, as a fraction of its length, and which number
            the property holds when they move the knob onto a tick mark.
        """
        given : 'A property holding 0.5, and a slider from 0.0 to 1.0 bound to it, with tick marks every 0.25 and every 0.1.'
            var opacity = Var.of(0.5d)
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0.0d, 1.0d, opacity)
                    .withMajorTickSpacing(0.25d)
                    .withMinorTickSpacing(0.1d)
                    .peek( s -> s.setPaintTicks(true) )
                    .get(JSlider)
        when : 'We measure how far apart the tick marks are, as a fraction of the length of the slider.'
            var length = slider.maximum - slider.minimum
            var majorFraction = slider.majorTickSpacing / (double) length
            var minorFraction = slider.minorTickSpacing / (double) length
        then : 'The major tick marks are exactly 1/4 of the slider apart, and the minor tick marks exactly 1/10.'
            majorFraction == 0.25d
            minorFraction == 0.1d

        when : 'The user moves the knob onto the third minor tick mark.'
            UI.runNow({ slider.value = slider.minimum + 3 * slider.minorTickSpacing })
        then : 'The property holds exactly 0.3, and not a number close to it.'
            opacity.get() == 0.3d

        when : 'The user moves the knob onto the third major tick mark.'
            UI.runNow({ slider.value = slider.minimum + 3 * slider.majorTickSpacing })
        then : 'The property holds exactly 0.75.'
            opacity.get() == 0.75d
    }

    def 'Use `withMajorTickSpacing(Val<N>)` and `withMinorTickSpacing(Val<N>)` to change the tick spacings of a slider while it is shown.'()
    {
        reportInfo """
            The spacings of the tick marks of a slider can come from properties, which may live in
            your view model. The slider then follows every change of them.

            A spacing of 0 removes the tick marks of that kind, just like it does on a `JSlider`.
            The slider in this scenario starts with a major tick mark every 25 and a minor tick mark
            every 5, and the application changes both spacings, and then sets the minor spacing to 0.
        """
        given : 'Two properties holding the spacings 25 and 5.'
            var majorSpacing = Var.of(25)
            var minorSpacing = Var.of(5)
        and : 'A slider from 0 to 100 bound to them, which switches on the painting of its tick marks.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, Var.of(50))
                    .withMajorTickSpacing(majorSpacing)
                    .withMinorTickSpacing(minorSpacing)
                    .peek( s -> s.setPaintTicks(true) )
                    .get(JSlider)
        expect : 'The tick marks are 25 and 5 apart.'
            slider.majorTickSpacing == 25
            slider.minorTickSpacing == 5

        when : 'The application changes the spacings to 20 and 10.'
            UI.runNow({
                majorSpacing.set(20)
                minorSpacing.set(10)
            })
        then : 'The tick marks are 20 and 10 apart.'
            slider.majorTickSpacing == 20
            slider.minorTickSpacing == 10

        when : 'The application sets the minor spacing to 0.'
            UI.runNow({ minorSpacing.set(0) })
        then : 'The slider has no minor tick marks any more, and its major tick marks are still 20 apart.'
            slider.minorTickSpacing == 0
            slider.majorTickSpacing == 20
    }

    def 'When the range of a slider for fractional numbers changes, its tick marks stay at the numbers of their spacing.'()
    {
        reportInfo """
            A slider for `Double` numbers maps its numbers onto a range of whole numbers behind the scenes,
            and SwingTree chooses that range so that every tick mark sits on one of its whole numbers. The
            `JSlider` itself only knows how many whole numbers apart its tick marks are. How many that is
            depends on the minimum and the maximum of the slider as well as on the spacing. So when the
            maximum changes, SwingTree has to tell the `JSlider` a new number of whole numbers between
            its tick marks.

            In this scenario a slider from 0.0 to 1.0 has a major tick mark every 0.25, which is a quarter
            of the length of the slider. When the maximum grows to 2.0, a tick mark every 0.25 is an eighth
            of the length of the slider. If the `JSlider` kept drawing a tick mark every quarter of its
            length, its tick marks would sit at 0.5, 1.0 and 1.5, and a user who moved the knob onto the
            first tick mark would expect 0.25 and get 0.5.
        """
        given : 'Properties for the minimum, the maximum and the value of a slider.'
            var min   = Var.of(0.0d)
            var max   = Var.of(1.0d)
            var value = Var.of(0.5d)
        and : 'A slider bound to them, with a major tick mark every 0.25.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, min, max, value)
                    .withMajorTickSpacing(0.25d)
                    .peek( s -> s.setPaintTicks(true) )
                    .get(JSlider)
        expect : 'The major tick marks are a quarter of the length of the slider apart.'
            slider.majorTickSpacing / (double) (slider.maximum - slider.minimum) == 0.25d

        when : 'The application changes the maximum to 2.0.'
            UI.runNow({ max.set(2.0d) })
        then : 'The major tick marks are an eighth of the length of the slider apart, because 0.25 is an eighth of 2.0.'
            slider.majorTickSpacing / (double) (slider.maximum - slider.minimum) == 0.125d

        when : 'The user moves the knob onto the third major tick mark.'
            UI.runNow({ slider.value = slider.minimum + 3 * slider.majorTickSpacing })
        then : 'The property holds exactly 0.75.'
            value.get() == 0.75d
    }

    def 'A slider given a `SliderTicks` value takes its tick marks from that value and ignores `withMajorTickSpacing(N)` and `withMinorTickSpacing(N)`.'(
        Closure<JSlider> buildSlider
    ) {
        reportInfo """
            There are two ways to tell a slider where its tick marks are. `withMajorTickSpacing(N)` and
            `withMinorTickSpacing(N)` set the spacings the way a `JSlider` does, and leave drawing them to
            you. `withTicks(SliderTicks)` describes the tick marks, the labels and the snapping of a slider
            in one value, and the slider then draws whatever that value describes.

            When a slider gets both, the `SliderTicks` value decides, whichever of the methods is called
            last. In this scenario the `SliderTicks` value asks for a major tick mark every 25, with four
            minor tick marks between two major tick marks, which is a minor tick mark every 5. The spacing
            methods ask for a major tick mark every 10 and a minor tick mark every 2. The table at the end
            of this scenario builds the slider twice, once with the spacing methods called first, and once
            with `withTicks(SliderTicks)` called first.

            The rule has to hold after the range of the slider changes, too. SwingTree lays out the tick
            marks of a `SliderTicks` value again for every new range of a slider. If the spacing methods won
            whenever they were called last, the slider would show a tick mark every 10 only until its range
            changed, and then the tick marks of the `SliderTicks` value would suddenly come back.
        """
        given : 'A property holding the maximum 100.'
            var max = Var.of(100)
        and : 'A slider from 0 to that maximum, which gets both kinds of tick marks.'
            var slider = buildSlider(max)
        expect : 'The slider has the tick marks of the `SliderTicks` value, 25 and 5 apart.'
            slider.majorTickSpacing == 25
            slider.minorTickSpacing == 5
            slider.paintTicks

        when : 'The application changes the maximum of the slider to 200.'
            UI.runNow({ max.set(200) })
        then : 'The slider still has the tick marks of the `SliderTicks` value.'
            slider.maximum == 200
            slider.majorTickSpacing == 25
            slider.minorTickSpacing == 5

        where : 'The slider is built with the spacing methods called first, or with `withTicks(SliderTicks)` called first.'
            buildSlider << [
                { Var<Integer> maximum ->
                    UI.slider(UI.Axis.HORIZONTAL, Var.of(0), maximum, Var.of(50))
                    .withMajorTickSpacing(10)
                    .withMinorTickSpacing(2)
                    .withTicks(SliderTicks.of(Integer.class).withMajorSpacing(25).withMinorTicksBetween(4))
                    .get(JSlider)
                },
                { Var<Integer> maximum ->
                    UI.slider(UI.Axis.HORIZONTAL, Var.of(0), maximum, Var.of(50))
                    .withTicks(SliderTicks.of(Integer.class).withMajorSpacing(25).withMinorTicksBetween(4))
                    .withMajorTickSpacing(10)
                    .withMinorTickSpacing(2)
                    .get(JSlider)
                }
            ]
    }

    def 'Tick spacings which a slider for fractional numbers cannot place exactly are not drawn, and SwingTree logs a warning explaining why.'()
    {
        reportInfo """
            A `JSlider` only knows whole numbers, so a slider for `Double` numbers maps its numbers onto a
            range of whole numbers behind the scenes, and a tick mark can only sit on one of those whole
            numbers. To place a major tick mark every 1.0 and a minor tick mark every 0.25 exactly, SwingTree
            divides the slider into equal parts which both spacings are whole multiples of, here parts of
            0.25, and gives every part the same number of whole numbers.

            Some spacings need very small parts. In this scenario the minor tick marks are meant to sit at
            every third of a whole number, and the spacing is written as `1.0d / 3`. As a `double`, that is
            0.3333333333333333, a number with 16 decimal places. The largest parts which both 1.0 and
            0.3333333333333333 are whole multiples of are 0.0000000000000001 long, and a slider from 0.0 to
            100.0 holds 10^18 of them.

            No screen can show that many positions, so SwingTree never divides a slider into more than
            100 000 parts for its tick marks. It draws no tick marks at all, and logs a warning which tells
            you why. If you want exactly three minor tick marks per whole number, describe them with
            `SliderTicks.withMajorSpacing(1.0d).withMinorTicksBetween(2)`, which counts the minor tick
            marks instead of spacing them.
        """
        given : 'A spy on the log.'
            var log = LogSpy.attach()
        and : 'A slider from 0.0 to 100.0 with a major tick mark every 1.0 and a minor tick mark every 1.0 / 3.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0.0d, 100.0d, Var.of(50.0d))
                    .withMajorTickSpacing(1.0d)
                    .withMinorTickSpacing(1.0d / 3)
                    .peek( s -> s.setPaintTicks(true) )
                    .get(JSlider)
        expect : 'The slider has no tick marks.'
            slider.majorTickSpacing == 0
            slider.minorTickSpacing == 0
        and : 'A warning explains why.'
            log.warnings().any { it.contains("more than 100000 parts") }
        and : 'The knob still sits halfway along the slider.'
            (slider.value - slider.minimum) / (double) (slider.maximum - slider.minimum) == 0.5d

        cleanup :
            log?.detach()
    }
}
