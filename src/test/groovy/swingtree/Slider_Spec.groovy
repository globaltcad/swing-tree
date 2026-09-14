package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.From
import sprouts.Var
import swingtree.api.model.SliderTicks

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
}
