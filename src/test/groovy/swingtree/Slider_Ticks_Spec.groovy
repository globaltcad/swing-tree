package swingtree

import groovy.transform.CompileDynamic
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Val
import sprouts.Var
import swingtree.api.IconDeclaration
import swingtree.api.model.SliderTicks
import swingtree.threading.EventProcessor
import utility.LogSpy
import utility.SwingTreeTestConfigurator

import javax.swing.JLabel
import javax.swing.JSlider
import java.awt.Color
import java.awt.Font

@Title("Slider Tick Marks and Labels")
@Narrative('''

    A slider can show tick marks along its track, and labels which tell the
    user what the positions on the track mean. In SwingTree you describe all of
    that with one immutable value, a `SliderTicks`, and hand it to the slider
    through `withTicks(SliderTicks)`, or bind a property holding one through
    `withTicks(Val<SliderTicks>)`.

    A `SliderTicks` describes where the major tick marks are, how many minor
    tick marks sit between them, whether the tick marks are drawn, whether the
    knob snaps to them, and which labels are shown: a label at every major tick
    mark, labels at particular numbers, or both.

    Everything in it is expressed in the numbers of the slider itself. A slider
    for a `Double` property gets its tick marks in `Double`s, even though a plain
    `JSlider` only knows whole numbers underneath.

''')
@Subject([UIForSlider, SliderTicks])
@CompileDynamic
class Slider_Ticks_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'Use `withTicks(SliderTicks)` to draw major and minor tick marks with a label at every major tick mark.'()
    {
        reportInfo """
            A volume slider from 0 to 100 which shows a major tick mark every 25,
            four minor tick marks between two major tick marks, and a label
            with a percentage at every major tick mark is declared like this.

            Four minor tick marks divide the distance of 25 between two major tick
            marks into five equal parts, so a minor tick mark sits at every multiple
            of 5. Counting the minor tick marks, instead of giving them a spacing of
            their own, means you can never declare minor tick marks which miss the
            major ones.

            You do not have to switch the painting of tick marks or labels on
            separately, the way you would with a plain `JSlider`: the slider draws
            whatever the `SliderTicks` value describes.
        """
        given : 'A slider bound to a volume property, with tick marks and labels.'
            var volume = Var.of(40)
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, volume)
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(25)
                        .withMinorTicksBetween(4)
                        .withLabelsAtMajorTicks({ v -> v + "%" })
                    )
                    .get(JSlider)
        when : 'We read the labels out of the slider, as a map from the number of each label to its text.'
            var labels = slider.labelTable.collectEntries { number, label -> [number, label.text] }
        then : 'The tick marks are drawn every 25, with a minor tick mark every 5.'
            slider.paintTicks
            slider.majorTickSpacing == 25
            slider.minorTickSpacing == 5
        and : 'There is a label at every major tick mark, with the text computed by our function.'
            slider.paintLabels
            labels == [0: "0%", 25: "25%", 50: "50%", 75: "75%", 100: "100%"]
    }

    def 'A slider for fractional numbers places every tick mark and label exactly where its number is.'()
    {
        reportInfo """
            A plain `JSlider` only knows whole numbers, so a slider for a `Double`
            property maps its numbers onto a range of whole numbers behind the scenes.
            If it did that without looking at the tick marks, a tick mark every 0.1 on
            a slider from 0 to 1 which is 256 whole numbers long would need to sit every
            25.6 whole numbers, which a `JSlider` cannot draw.

            So SwingTree chooses the range of whole numbers to fit the tick marks.
            In this scenario the slider runs from 0.0 to 1.0, with a major tick mark every
            0.25 and four minor tick marks between them, which is a tick mark every 0.05.
            That is 20 tick marks. The range of whole numbers must be at least 256 long, and
            13 whole numbers per tick mark gives 20 * 13 = 260, so the major tick marks are
            5 * 13 = 65 whole numbers apart.

            We do not want this scenario to depend on those whole numbers, though. What the
            user sees is where a label sits along the slider, as a fraction of its length,
            and those fractions must be exactly 0, 1/4, 1/2, 3/4 and 1.
        """
        given : 'A slider from 0.0 to 1.0 with a label at every major tick mark.'
            var opacity = Var.of(0.3d)
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0.0d, 1.0d, opacity)
                    .withTicks(
                        SliderTicks.of(Double.class)
                        .withMajorSpacing(0.25d)
                        .withMinorTicksBetween(4)
                        .withLabelsAtMajorTicks()
                    )
                    .get(JSlider)
        when : 'We read, for every label from left to right, how far along the slider it sits and what it says.'
            var numbers   = slider.labelTable.keySet().sort()
            var fractions = numbers.collect { (it - slider.minimum) / (double) (slider.maximum - slider.minimum) }
            var texts     = numbers.collect { slider.labelTable.get(it).text }
        then : 'The labels sit at exactly 0, 1/4, 1/2, 3/4 and 1 of the length of the slider.'
            fractions == [0d, 0.25d, 0.5d, 0.75d, 1d]
        and : 'They show their numbers, all with the two decimal places needed to write 0.25 and 0.75 exactly.'
            texts == ["0.00", "0.25", "0.50", "0.75", "1.00"]
        and : 'Five minor tick marks fit exactly between two major tick marks, which fit exactly four times into the slider.'
            slider.majorTickSpacing == 5 * slider.minorTickSpacing
            slider.maximum - slider.minimum == 4 * slider.majorTickSpacing
    }

    def 'The label text function receives the exact number at each major tick mark, without floating point noise.'()
    {
        reportInfo """
            In Java, `0.1 + 0.1 + 0.1` is not `0.3` but `0.30000000000000004`, because a
            `double` cannot hold the number 0.1 exactly, and the small errors add up.
            A slider which found the numbers at its tick marks by adding the spacing over
            and over again would hand exactly that kind of number to your label text
            function, and a label reading "0.30000000000000004" is not what you want.

            SwingTree computes the number at a major tick mark as the minimum of the slider
            plus a whole multiple of the major spacing, in decimal arithmetic. So a slider
            from 0.0 with a major spacing of 0.1 hands 0.0, 0.1, 0.2 and so on up to 1.0 to
            the function, each of them the `double` you would get by writing the number
            down in your source code.
        """
        given : 'A list recording every number the label text function receives.'
            var received = new LinkedHashSet<Double>()
        and : 'A slider from 0.0 to 1.0 with a major tick mark every 0.1.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0.0d, 1.0d, Var.of(0.5d))
                    .withTicks(
                        SliderTicks.of(Double.class)
                        .withMajorSpacing(0.1d)
                        .withLabelsAtMajorTicks({ v -> received.add(v); return String.valueOf(v) })
                    )
                    .get(JSlider)
        expect : 'Adding 0.1 three times really does miss 0.3 in Java.'
            0.1d + 0.1d + 0.1d == 0.30000000000000004d
        and : 'But the function received every number exactly as written in source code.'
            received as List == [0.0d, 0.1d, 0.2d, 0.3d, 0.4d, 0.5d, 0.6d, 0.7d, 0.8d, 0.9d, 1.0d]
        and : 'And that is what the labels read.'
            slider.labelTable.keySet().sort().collect { slider.labelTable.get(it).text } ==
                ["0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0"]
    }

    def 'Number labels do not depend on the locale of the machine, unless you ask for a locale with `withLabelLocale(Locale)`.'(
        Locale locale, double spacing, double max, List<String> texts
    ) {
        reportInfo """
            A label at every major tick mark shows the number at that tick mark, and all of
            these labels are written with the same count of decimal places: the fewest which
            write every one of them exactly. That keeps the labels of a scale looking alike,
            "0.50" next to "0.25", instead of "0.5" next to "0.25".

            By default the numbers are written the way Java source code writes them, with a
            dot as decimal separator and without grouping the thousands, so that a slider
            reads the same on every machine, and so that tests do not depend on the machine
            running them. If your application should follow the conventions of a locale,
            say so with `withLabelLocale(Locale)`. A German user then reads "0,25" and
            "1.234,5", and an American user reads "1,234.5".
        """
        given : 'A slider from 0.0 to the given maximum, with number labels at every major tick mark in the given locale.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0.0d, max, Var.of(0.0d))
                    .withTicks(
                        SliderTicks.of(Double.class)
                        .withMajorSpacing(spacing)
                        .withLabelsAtMajorTicks()
                        .withLabelLocale(locale)
                    )
                    .get(JSlider)
        expect : 'The labels read, from left to right:'
            slider.labelTable.keySet().sort().collect { slider.labelTable.get(it).text } == texts

        where :
            locale         | spacing | max     || texts
            Locale.ROOT    | 0.25d   | 1.0d    || ["0.00", "0.25", "0.50", "0.75", "1.00"]
            Locale.GERMANY | 0.25d   | 1.0d    || ["0,00", "0,25", "0,50", "0,75", "1,00"]
            Locale.ROOT    | 0.5d    | 1.0d    || ["0.0", "0.5", "1.0"]
            Locale.ROOT    | 1234.5d | 3000.0d || ["0.0", "1234.5", "2469.0"]
            Locale.US      | 1234.5d | 3000.0d || ["0.0", "1,234.5", "2,469.0"]
            Locale.GERMANY | 1234.5d | 3000.0d || ["0,0", "1.234,5", "2.469,0"]
    }

    def 'Tick marks and the labels at them count from the minimum of the slider.'()
    {
        reportInfo """
            Every Swing look and feel draws tick marks by starting at the minimum of the
            slider and stepping forward by the spacing. So on a slider running from 3 to 97
            with a major spacing of 25, the major tick marks sit at 3, 28, 53 and 78, not at
            25, 50 and 75. The labels at the major tick marks follow the tick marks, because a
            label is only useful where its tick mark is.

            If you want a label at 50 on such a slider, place it there with `withLabelAt(..)`,
            which puts a label at any number you like.
        """
        given : 'A slider from 3 to 97 with a major tick mark every 25 and a label at each of them.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 3, 97, Var.of(50))
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(25)
                        .withLabelsAtMajorTicks()
                    )
                    .get(JSlider)
        expect : 'The labels sit at 3, 28, 53 and 78.'
            slider.labelTable.collectEntries { number, label -> [number, label.text] } == [3: "3", 28: "28", 53: "53", 78: "78"]
    }

    def 'Use `withLabelAt(N, String)` to put a label at any number, whether or not there is a tick mark.'()
    {
        reportInfo """
            Not every slider wants numbers under its track. A temperature slider may
            rather say "Cold", "Warm" and "Hot". You place such labels at particular
            numbers with `withLabelAt(N, String)`, and they need no tick marks at all.

            A label at a number outside the range of the slider is not shown, since there
            is no place on the track where it could sit. And when a label placed at a
            particular number falls onto the same position as a label at a major tick mark,
            the label you placed wins, which lets you rename a single tick mark.
        """
        given : 'A temperature slider from 0 to 100 with three word labels, and one label outside of its range.'
            var temperature =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, Var.of(20))
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withLabelAt(0, "Cold")
                        .withLabelAt(50, "Warm")
                        .withLabelAt(100, "Hot")
                        .withLabelAt(150, "Boiling")
                    )
                    .get(JSlider)
        and : 'A second slider with number labels at its major tick marks, whose last label is renamed.'
            var progress =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, Var.of(20))
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(50)
                        .withLabelsAtMajorTicks()
                        .withLabelAt(100, "Done")
                    )
                    .get(JSlider)
        expect : 'The temperature slider shows its three word labels, and no tick marks.'
            temperature.labelTable.collectEntries { number, label -> [number, label.text] } == [0: "Cold", 50: "Warm", 100: "Hot"]
            temperature.paintLabels
            !temperature.paintTicks
        and : 'The second slider shows the number labels, except where "Done" replaces one of them.'
            progress.labelTable.collectEntries { number, label -> [number, label.text] } == [0: "0", 50: "50", 100: "Done"]
    }

    def 'Use `withLabelAt(N, IconDeclaration)` to show an icon instead of a text at a number.'()
    {
        reportInfo """
            A label along a slider does not have to be text. A volume slider may show a
            quiet speaker at its start and a loud one at its end. You place an icon label
            with `withLabelAt(N, IconDeclaration)`, which takes the same `IconDeclaration`
            you use everywhere else in SwingTree, so the icon can be kept in a view model
            without loading an image there.
        """
        given : 'A slider with an icon label at its start and a text label at its end.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 10, Var.of(5))
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withLabelAt(0, IconDeclaration.of("img/swing.png"))
                        .withLabelAt(10, "Loud")
                    )
                    .get(JSlider)
        when : 'We look at the two label components.'
            var start = slider.labelTable.get(0) as JLabel
            var end   = slider.labelTable.get(10) as JLabel
        then : 'The first one shows an icon and no text...'
            start.icon != null
            !start.text
        and : '...and the second one shows text and no icon.'
            end.icon == null
            end.text == "Loud"
    }

    def 'Use `withTickMarksVisible(false)` to show the labels at the major tick marks without drawing the marks.'()
    {
        reportInfo """
            A flat design often wants the numbers 0, 25, 50, 75 and 100 below a slider,
            but no marks on its track. Tick marks which are not drawn still exist: the
            labels at the major tick marks are still placed at them, the knob can still
            snap to them, and clicking into the track still moves the knob by one major
            spacing.
        """
        given : 'A slider whose tick marks are not drawn, but labelled.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, Var.of(20))
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(25)
                        .withTickMarksVisible(false)
                        .withLabelsAtMajorTicks()
                    )
                    .get(JSlider)
        expect : 'The slider draws no tick marks...'
            !slider.paintTicks
        and : '...but it knows their spacing, and shows the labels at them.'
            slider.majorTickSpacing == 25
            slider.paintLabels
            slider.labelTable.collectEntries { number, label -> [number, label.text] } == [0: "0", 25: "25", 50: "50", 75: "75", 100: "100"]
    }

    def 'The labels take the font and the foreground colour of their slider.'()
    {
        reportInfo """
            The labels along a slider are not components of your view, you never add
            them anywhere. So you cannot style them one by one. Instead they take the
            font and the foreground colour of the slider they belong to, which means you
            style the labels by styling the slider. This is also how the labels of a plain
            `JSlider` behave when Swing creates them for you.
        """
        given : 'A slider with a red foreground and labels at its major tick marks.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, Var.of(20))
                    .withForeground(Color.RED)
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(50)
                        .withLabelsAtMajorTicks()
                    )
                    .get(JSlider)
        expect : 'Every label is red, like the slider.'
            slider.labelTable.values().every { it.foreground == Color.RED }

        when : 'We give the slider a large bold font.'
            var font = new Font(Font.DIALOG, Font.BOLD, 24)
            UI.runNow({ slider.font = font })
        then : 'Every label now uses that font.'
            slider.labelTable.values().every { it.font == font }
    }

    def 'When the minimum or maximum of a slider changes, its labels are laid out again for the new range.'()
    {
        reportInfo """
            The range of a slider is often not fixed. A timeline slider runs from zero to
            the length of the current video, and that length changes with every video.
            The labels at the major tick marks must then cover the new range, while the
            labels you placed at particular numbers stay where they are.

            This is worth a scenario of its own, because a plain `JSlider` gets this wrong
            in two ways. The automatic labels which `JSlider.createStandardLabels(..)`
            creates keep listening to the slider even after you replaced them with a label
            table of your own, and when the maximum changes they put themselves back in,
            mixed with your labels. And when you change the tick spacing, those automatic
            labels do not move at all. SwingTree builds the label table itself every time
            anything it depends on changes, so neither can happen.
        """
        given : 'Properties for the maximum and the value of a slider.'
            var max = Var.of(100)
            var value = Var.of(20)
        and : 'A slider with number labels every 25, and a label at 100 which says "Old end".'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, Val.of(0), max, value)
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(25)
                        .withLabelsAtMajorTicks()
                        .withLabelAt(100, "Old end")
                    )
                    .get(JSlider)
        expect : 'The slider has the labels you would expect.'
            slider.labelTable.collectEntries { number, label -> [number, label.text] } ==
                [0: "0", 25: "25", 50: "50", 75: "75", 100: "Old end"]

        when : 'The application changes the maximum to 200.'
            UI.runNow({ max.set(200) })
        then : 'The number labels cover the new range, and the label at 100 is still there.'
            slider.labelTable.collectEntries { number, label -> [number, label.text] } ==
                [0: "0", 25: "25", 50: "50", 75: "75", 100: "Old end", 125: "125", 150: "150", 175: "175", 200: "200"]
        and : 'The knob still sits at the value of 20.'
            slider.value == 20
    }

    def 'Use `withTicks(Val<SliderTicks>)` to change the tick marks and labels of a slider while it is shown.'()
    {
        reportInfo """
            The tick marks and labels of a slider can come from a property, in which case
            the slider follows every change of it. The property may live in your view model,
            or you can derive it from view model state right in the view, like in this
            scenario, where a flag decides whether the labels are shown.

            Because `SliderTicks` is generic in the number type of the slider,
            `SliderTicks.class` alone would give you a property of the raw type. Use
            `SliderTicks.classTyped(Integer.class)` instead, which keeps the number type.
        """
        given : 'A flag, as it would live in a view model, telling whether the labels are shown.'
            var showLabels = Var.of(false)
        and : 'Tick marks with and without labels, and a property picking one of them depending on the flag.'
            var plain = SliderTicks.of(Integer.class).withMajorSpacing(25)
            var labelled = plain.withLabelsAtMajorTicks()
            var ticks = showLabels.viewAs(SliderTicks.classTyped(Integer.class), { show -> show ? labelled : plain })
        and : 'A slider bound to that property.'
            var slider = UI.slider(UI.Axis.HORIZONTAL, 0, 100, Var.of(20)).withTicks(ticks).get(JSlider)
        expect : 'The slider starts out with tick marks and without labels.'
            slider.paintTicks
            !slider.paintLabels
            slider.labelTable.isEmpty()

        when : 'The application switches the labels on.'
            UI.runNow({ showLabels.set(true) })
        then : 'The slider shows the labels.'
            slider.paintLabels
            slider.labelTable.collectEntries { number, label -> [number, label.text] } == [0: "0", 25: "25", 50: "50", 75: "75", 100: "100"]

        when : 'The application switches the labels off again.'
            UI.runNow({ showLabels.set(false) })
        then : 'The labels are gone, and the tick marks are still there.'
            !slider.paintLabels
            slider.labelTable.isEmpty()
            slider.paintTicks
            slider.majorTickSpacing == 25
    }

    def 'Tick marks which a slider cannot draw correctly are not drawn, and SwingTree logs a warning explaining why.'(
        int max, int spacing, int minorTicksBetween, int expectedMajorSpacing, int expectedMinorSpacing, String warning
    ) {
        reportInfo """
            A slider for whole numbers can only place a tick mark at a whole number. With
            major tick marks 25 apart, three minor tick marks between them would divide 25
            into four parts of 6.25, which are not whole numbers. Drawing them at 6 or at 7
            would put tick marks where no number is, so SwingTree draws no minor tick marks
            at all and logs a warning telling you why.

            A tick mark every 1 on a slider from 0 to 10 million would be ten million tick
            marks, which no screen can show and no look and feel can draw in a reasonable
            time. So SwingTree does not draw more than 100 000 tick marks on a slider, and
            logs a warning instead.
        """
        given : 'A spy on the log.'
            var log = LogSpy.attach()
        and : 'A slider for whole numbers with the given tick marks.'
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, max, Var.of(0))
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(spacing)
                        .withMinorTicksBetween(minorTicksBetween)
                    )
                    .get(JSlider)
        expect : 'The slider has the tick spacings it can draw correctly.'
            slider.majorTickSpacing == expectedMajorSpacing
            slider.minorTickSpacing == expectedMinorSpacing
        and : 'A warning explains what was left out.'
            log.warnings().any { it.contains(warning) }

        cleanup :
            log?.detach()

        where :
            max        | spacing | minorTicksBetween || expectedMajorSpacing | expectedMinorSpacing | warning
            100        | 25      | 3                 || 25                   | 0                    | "cannot fit 3 minor tick marks between major tick marks 25 apart"
            10_000_000 | 1       | 0                 || 0                    | 0                    | "more than 100000 tick marks"
    }

    def 'Use `withSnapToTicks(true)` to let the knob snap to the tick marks when the user moves it.'()
    {
        reportInfo """
            A slider which snaps to its tick marks only lets the user pick the numbers at
            the tick marks. While the user drags the knob, the value of the slider is the
            number at the nearest tick mark, and when the user lets go of the knob, it moves
            onto that tick mark. An arrow key moves the knob to the next tick mark in the
            direction of the key, because moving it by the smallest possible step would only
            land it at the same tick mark again.

            A `JSlider` knows when the user is dragging its knob, because it reports
            `getValueIsAdjusting() == true` while the drag lasts. This scenario plays the
            user by setting that flag around the positions of a drag, just like the look and
            feel does when it handles the mouse.

            Snapping only applies to what the user does. When your application sets the
            value to a number between two tick marks, the knob sits between them, and the
            value stays exactly what your application set. A plain `JSlider` which snaps to
            ticks would move the knob onto a tick mark instead, and keep a different number
            than your property does.
        """
        given : 'A slider from 0 to 100 which snaps to tick marks every 25, bound to a property holding 50.'
            var value = Var.of(50)
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, value)
                    .withTicks(
                        SliderTicks.of(Integer.class)
                        .withMajorSpacing(25)
                        .withSnapToTicks(true)
                    )
                    .get(JSlider)

        when : 'The user starts dragging the knob and moves it to 60.'
            UI.runNow({ slider.valueIsAdjusting = true; slider.value = 60 })
        then : 'The nearest tick mark to 60 is at 50, so the value stays 50.'
            value.get() == 50

        when : 'The user lets go of the knob.'
            UI.runNow({ slider.valueIsAdjusting = false })
        then : 'The knob moves onto the tick mark at 50.'
            slider.value == 50
            value.get() == 50

        when : 'The user presses the right arrow key, which moves the knob one step to 51.'
            UI.runNow({ slider.value = 51 })
        then : 'The knob moves on to the next tick mark to the right, at 75.'
            slider.value == 75
            value.get() == 75

        when : 'The user presses the left arrow key, which moves the knob one step to 74.'
            UI.runNow({ slider.value = 74 })
        then : 'The knob moves back to the next tick mark to the left, at 50.'
            slider.value == 50
            value.get() == 50

        when : 'The application sets the value to 33, which is between two tick marks.'
            UI.runNow({ value.set(33) })
        then : 'The knob sits at 33, and the value is still 33.'
            slider.value == 33
            value.get() == 33
    }

    def 'A snapping slider for fractional numbers writes the exact number at the tick mark into its property.'()
    {
        reportInfo """
            When the knob of a slider for fractional numbers snaps to a tick mark, the number
            written into the property is the number at that tick mark, computed in decimal
            arithmetic. On a slider from 0.0 to 1.0 with a tick mark every 0.1, the user can
            therefore pick 0.3, and not 0.30000000000000004, which is what adding 0.1 three
            times gives you in Java.
        """
        given : 'A slider from 0.0 to 1.0 which snaps to tick marks every 0.1.'
            var value = Var.of(0.0d)
            var slider =
                    UI.slider(UI.Axis.HORIZONTAL, 0.0d, 1.0d, value)
                    .withTicks(
                        SliderTicks.of(Double.class)
                        .withMajorSpacing(0.1d)
                        .withSnapToTicks(true)
                    )
                    .get(JSlider)
        when : 'The user drags the knob to 31% of the length of the slider and lets go.'
            UI.runNow({
                slider.valueIsAdjusting = true
                slider.value = slider.minimum + Math.round((slider.maximum - slider.minimum) * 0.31d) as int
                slider.valueIsAdjusting = false
            })
        then : 'The property holds exactly 0.3.'
            value.get() == 0.3d
    }

    def 'A `SliderTicks` is an immutable value.'()
    {
        reportInfo """
            Every method of a `SliderTicks` which sounds like it changes something returns a
            new `SliderTicks` and leaves the one you called it on untouched. Two of them which
            describe the same tick marks and labels are equal, which is what lets a slider bound
            to a property of them tell whether anything changed, and what lets you compare the
            tick marks your view model computed against the ones you expected in a unit test.
        """
        given : 'Tick marks without anything, and tick marks derived from them.'
            var none = SliderTicks.of(Integer.class)
            var ticks = none.withMajorSpacing(10).withMinorTicksBetween(1).withLabelAt(5, "Five")
        expect : 'The tick marks we derived from did not change.'
            !none.majorSpacing().isPresent()
            none.labelPositions().isEmpty()
        and : 'The derived tick marks read back the way we wrote them.'
            ticks.majorSpacing().get() == 10
            ticks.minorTicksBetween() == 1
            ticks.labelTextAt(5).get() == "Five"
        and : 'Tick marks built the same way are equal, and have the same hash code.'
            ticks == SliderTicks.of(Integer.class).withMajorSpacing(10).withMinorTicksBetween(1).withLabelAt(5, "Five")
            ticks.hashCode() == SliderTicks.of(Integer.class).withMajorSpacing(10).withMinorTicksBetween(1).withLabelAt(5, "Five").hashCode()
        and : 'A major spacing of zero means there are no tick marks, like never having set one.'
            none.withMajorSpacing(10).withMajorSpacing(0) == none
    }

    def 'A `SliderTicks` refuses numbers which cannot describe tick marks.'( Closure<?> declaration )
    {
        reportInfo """
            A `SliderTicks` checks what you give it right away, instead of drawing something
            strange later. A slider only supports the number types `Integer`, `Long`, `Short`,
            `Byte`, `Float` and `Double`, a spacing can be neither negative nor "not a number",
            and there cannot be a negative number of minor tick marks.
        """
        when : 'We declare the tick marks.'
            declaration()
        then : 'The declaration is refused with an `IllegalArgumentException`.'
            thrown(IllegalArgumentException)

        where :
            declaration << [
                { -> SliderTicks.of(BigDecimal.class) },
                { -> SliderTicks.of(Integer.class).withMajorSpacing(-5) },
                { -> SliderTicks.of(Double.class).withMajorSpacing(Double.NaN) },
                { -> SliderTicks.of(Integer.class).withMinorTicksBetween(-1) }
            ]
    }
}
