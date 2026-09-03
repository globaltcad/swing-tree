package swingtree


import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.From
import sprouts.Val
import sprouts.Var
import swingtree.layout.Size
import swingtree.threading.EventProcessor

import swingtree.style.ComponentExtension

import javax.swing.*
import javax.swing.tree.DefaultTreeCellRenderer
import java.awt.*
import java.lang.ref.WeakReference

@Title("High IPD Scaling")
@Narrative('''

    Higher resolution displays with higher pixel density 
    (measured in pixels per inch (PPI) or dots per inch (DPI)) have become the norm.  
    This is especially true for mobile devices, but it is also true for desktop displays.  
    
    Vanilla Swing does not handle this well unfortunately, even with the introduction of
    the HiDPI support in Java 9, which allows us to determine the DPI of the display.
    Because although we calculate the scaling factor, there is no way to apply it to the UI.
    Instead the task is left to the Look and Feel implementations which may or may not
    scale the UI.
    The problem is that older Look and Feels do not scale the UI, and even newer ones
    may not scale the UI at all. 
    In fact none of the Look and Feels included in the JDK scale the UI.
    
    SwingTree can hardly solve this problem entirely, but it can help by scaling the UI
    where the Look and Feel does not.
    In this specification you will find out how to adjust the SwingTree scaling factor
    and how it affects the properties of the UI components.

''')
class UI_Scaling_Spec extends Specification
{
    def setup() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
    }

    def cleanup() {
        SwingTree.clear()
    }

    def 'The dimensionality of components will be scaled by the scaling factor'() {
        given:
            SwingTree.get().setUiScaleFactor(2.0f)

        when : 'We build a simple panel with a number of various components and custom dimensions'
            var panel =
                UI.panel("wrap 1")
                .add(
                    UI.button("Button")
                    .withPrefSize(100, 50)
                    .withMinSize(75, 25)
                    .withMaxSize(70, 50)
                    .withSize(150, 50)
                )
                .add(
                    UI.toggleButton("Toggle Button")
                    .withPrefSize(Size.of(111, 52))
                    .withMinSize(Size.of(86, 23))
                    .withMaxSize(Size.of(90, 67))
                    .withSize(Size.of(121, 44))
                )
                .add(
                    UI.slider(UI.Align.HORIZONTAL)
                    .withPrefSize(Val.of(Size.of(60, 20)))
                    .withMinSize(Val.of(Size.of(70, 80)))
                    .withMaxSize(Val.of(Size.of(80, 42)))
                    .withSize(Val.of(Size.of(120, 40)))
                )
                .add(
                    UI.label("Label")
                    .withPrefWidth(142)
                    .withMinWidth(110)
                    .withMaxWidth(90)
                    .withWidth(284)
                )
                .add(
                    UI.textField("TextField")
                    .withPrefHeight(30)
                    .withMinHeight(36)
                    .withMaxHeight(40)
                    .withHeight(60)
                )
                .add(
                    UI.textArea("TextArea")
                    .withSizeExactly(Size.of(55, 88))
                )
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button       = panel.components[0]
            var toggleButton = panel.components[1]
            var slider       = panel.components[2]
            var label        = panel.components[3]
            var textField    = panel.components[4]
            var textArea     = panel.components[5]

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(200, 100)
            button.minimumSize == new Dimension(150, 50)
            button.maximumSize == new Dimension(140, 100)
            button.size == new Dimension(300, 100)
            toggleButton.preferredSize == new Dimension(222, 104)
            toggleButton.minimumSize == new Dimension(172, 46)
            toggleButton.maximumSize == new Dimension(180, 134)
            toggleButton.size == new Dimension(242, 88)
            slider.preferredSize == new Dimension(120, 40)
            slider.minimumSize == new Dimension(140, 160)
            slider.maximumSize == new Dimension(160, 84)
            slider.size == new Dimension(240, 80)
            label.preferredSize.width == 284
            label.minimumSize.width == 220
            label.maximumSize.width == 180
            label.size.width == 568
            textField.preferredSize.height == 60
            textField.minimumSize.height == 72
            textField.maximumSize.height == 80
            textField.size.height == 120
            textArea.preferredSize == new Dimension(110, 176)
            textArea.minimumSize == new Dimension(110, 176)
            textArea.maximumSize == new Dimension(110, 176)
            textArea.size == new Dimension(0, 0)
    }


    def 'The dimensionality specified in the styling API are scaled by the scaling factor'()
    {
        reportInfo """
            The preferred API for changing how a component looks is the styling API of SwingTree.
            The styling API allows you to style components based on functional styler lambdas
            which are executed eagerly before every repaint.
            That means that you can determine the dimensions of a component based on 
            some current context (e.g. the size of the parent component) dynamically. 
            How cool is that? :) 
        """
        given:
            SwingTree.get().setUiScaleFactor(2.0f)

        when : 'We build a simple panel with a number of various components and custom dimensions'
            var panel =
                UI.panel()
                .add(
                    UI.textArea("TextArea").withStyle( it -> it
                        .prefSize(75, 25)
                        .minSize(70, 30)
                        .maxSize(60, 22)
                        .size(150, 40)
                    )
                )
                .add(
                    UI.toggleButton("ToggleButton").withStyle( it -> it
                        .prefSize(Size.of(60, 20))
                        .minSize(Size.of(70, 80))
                        .maxSize(Size.of(80, 42))
                        .size(Size.of(120, 40))
                    )
                )
                .add(
                    UI.comboBox("ComboBox").withStyle( it -> it
                        .prefWidth(142)
                        .minWidth(110)
                        .maxWidth(90)
                        .width(284)
                    )
                )
                .add(
                    UI.passwordField().withStyle( it -> it
                        .prefHeight(30)
                        .minHeight(36)
                        .maxHeight(40)
                        .height(60)
                    )
                )
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var textArea = panel.components[0]
            var toggleButton = panel.components[1]
            var comboBox = panel.components[2]
            var passwordField = panel.components[3]

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            textArea.preferredSize == new Dimension(150, 50)
            textArea.minimumSize == new Dimension(140, 60)
            textArea.maximumSize == new Dimension(120, 44)
            textArea.size == new Dimension(300, 80)
            toggleButton.preferredSize == new Dimension(120, 40)
            toggleButton.minimumSize == new Dimension(140, 160)
            toggleButton.maximumSize == new Dimension(160, 84)
            toggleButton.size == new Dimension(240, 80)
            comboBox.preferredSize.width == 284
            comboBox.minimumSize.width == 220
            comboBox.maximumSize.width == 180
            comboBox.size.width == 568
            passwordField.preferredSize.height == 60
            passwordField.minimumSize.height == 72
            passwordField.maximumSize.height == 80
            passwordField.size.height == 120
    }

    def 'Dimensionality scaling also works for bound properties.'()
    {
        reportInfo """
            SwingTree supports MVI, MVL and MVVM (Model-View-ViewModel) and therefore allows you to bind
            properties of the UI components to properties of a view model.
            The values of properties modeling the dimensionality of the components are also scaled by the
            scaling factor when applied to the UI components dynamically.
        """
        given : 'We set the scaling factor to 2.0'
            SwingTree.get().setUiScaleFactor(2.0f)
        and : 'We create a whole lot of properties:'
            var prefSize = Var.of(Size.of(70, 50))
            var minSize  = Var.of(Size.of(75, 25))
            var maxSize  = Var.of(Size.of(80, 45))
            var size     = Var.of(Size.of(20, 22))
            var prefWidth  = Var.of(142)
            var minWidth   = Var.of(110)
            var maxWidth   = Var.of(90)
            var width      = Var.of(284)
            var prefHeight = Var.of(30)
            var minHeight  = Var.of(36)
            var maxHeight  = Var.of(40)
            var height     = Var.of(66)

        and : 'We create a UI with a button where all of these properties are bound to:'
            var panel =
                UI.panel()
                .add(
                    UI.button("Button")
                    .withPrefSize(prefSize)
                    .withMinSize(minSize)
                    .withMaxSize(maxSize)
                    .withSize(size)
                    .withPrefWidth(prefWidth)
                    .withMinWidth(minWidth)
                    .withMaxWidth(maxWidth)
                    .withWidth(width)
                    .withPrefHeight(prefHeight)
                    .withMinHeight(minHeight)
                    .withMaxHeight(maxHeight)
                    .withHeight(height)
                )
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0]

        expect : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(284, 60)
            button.minimumSize == new Dimension(220, 72)
            button.maximumSize == new Dimension(180, 80)
            button.size == new Dimension(568, 132)

        when : 'We change the first set of properties...'
            prefSize.set(Size.of(200, 100))
            minSize.set(Size.of(150, 50))
            maxSize.set(Size.of(140, 100))
            size.set(Size.of(300, 100))
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(400, 200)
            button.minimumSize == new Dimension(300, 100)
            button.maximumSize == new Dimension(280, 200)
            button.size == new Dimension(600, 200)

        when : 'We change the second set of properties...'
            prefWidth.set(200)
            minWidth.set(150)
            maxWidth.set(140)
            width.set(300)
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(400, 200)
            button.minimumSize == new Dimension(300, 100)
            button.maximumSize == new Dimension(280, 200)
            button.size == new Dimension(600, 200)

        when : 'We change the third set of properties...'
            prefHeight.set(60)
            minHeight.set(72)
            maxHeight.set(80)
            height.set(120)
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(400, 120)
            button.minimumSize == new Dimension(300, 144)
            button.maximumSize == new Dimension(280, 160)
            button.size == new Dimension(600, 240)
    }

    def 'Dimensionality scaling works for properties bound to `withSizeExactly`, `withWidthExactly` and `withHeightExactly`.'()
    {
        reportInfo """
            SwingTree supports MVI, MVL and MVVM (Model-View-ViewModel) and therefore allows you to bind
            properties of the UI components to properties of a view model.
            The values of properties modeling the dimensionality of the components are also scaled by the
            scaling factor when applied to the UI components dynamically.
        """
        given : 'We set the scaling factor to 2.0'
            SwingTree.get().setUiScaleFactor(2.0f)
        and : 'We create a whole lot of properties:'
            var size   = Var.of(Size.of(73, 42))
            var width  = Var.of(128)
            var height = Var.of(52)

        and : 'We create a UI with a button where all of these properties are bound to:'
            var panel =
                UI.panel()
                .add(
                    UI.button("Button")
                    .withSizeExactly(size)
                    .withWidthExactly(width)
                    .withHeightExactly(height)
                )
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0]

        expect : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(256, 104)
            button.minimumSize == new Dimension(256, 104)
            button.maximumSize == new Dimension(256, 104)
            button.size == new Dimension(0, 0)

        when : 'We change the first set of properties...'
            size.set(Size.of(300, 100))
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(600, 200)
            button.minimumSize == new Dimension(600, 200)
            button.maximumSize == new Dimension(600, 200)
            button.size == new Dimension(0, 0)

        when : 'We change the second set of properties...'
            width.set(777)
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(1554, 200)
            button.minimumSize == new Dimension(1554, 200)
            button.maximumSize == new Dimension(1554, 200)
            button.size == new Dimension(0, 0)

        when : 'We change the third set of properties...'
            height.set(120)
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(1554, 240)
            button.minimumSize == new Dimension(1554, 240)
            button.maximumSize == new Dimension(1554, 240)
            button.size == new Dimension(0, 0)
    }

    def 'Dimensionality scaling works for properties bound to `withSizeExactly(Val,Val)`.'()
    {
        reportInfo """
            SwingTree supports MVI, MVL and MVVM (Model-View-ViewModel) and therefore allows you to bind
            properties of the UI components to properties of a view model.
            The values of properties modeling the dimensionality of the components are also scaled by the
            scaling factor when applied to the UI components dynamically.
        """
        given : 'We set the scaling factor to 2.0'
            SwingTree.get().setUiScaleFactor(2.0f)
        and : 'We create a whole lot of properties:'
            var width  = Var.of(128)
            var height = Var.of(52)

        and : 'We create a UI with a button where all of these properties are bound to:'
            var panel =
                UI.panel()
                .add(
                    UI.button("Button")
                    .withSizeExactly(width,height)
                )
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0]

        expect : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(256, 104)
            button.minimumSize == new Dimension(256, 104)
            button.maximumSize == new Dimension(256, 104)
            button.size == new Dimension(0, 0)

        when : 'We change the widths of the component...'
            width.set(777)
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(1554, 104)
            button.minimumSize == new Dimension(1554, 104)
            button.maximumSize == new Dimension(1554, 104)
            button.size == new Dimension(0, 0)

        when : 'We change the heights of the component through the property...'
            height.set(120)
            UI.sync() // We need to wait for the UI thread to update the UI

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(1554, 240)
            button.minimumSize == new Dimension(1554, 240)
            button.maximumSize == new Dimension(1554, 240)
            button.size == new Dimension(0, 0)
    }

    def 'The `componentPrefWidth()` and `componentPrefHeight()` of a style delegate are unscaled to "developer pixel".'()
    {
        reportInfo """
            Inside of a `Styler` lambda you have access to a `ComponentStyleDelegate`
            which exposes the underlying component through a couple of accessor methods.
            A raw call to `component().getPreferredSize()` would give you the preferred size
            **already scaled** to the current `UI.scale()`, which is a common source of bugs.

            To save you from that trap, the delegate offers `componentPrefWidth()` and
            `componentPrefHeight()`, which mirror `componentWidth()` and `componentHeight()`
            but for the *preferred* size, and which give you the size back in "developer pixel"
            (i.e. unscaled).
        """
        given : 'We set the scaling factor to 2.0 so that scaling effects become observable.'
            SwingTree.get().setUiScaleFactor(2.0f)
        and : 'A few capture variables we read from inside the styler:'
            int[] rawPrefSize     = new int[2]
            int[] devPixelPrefSize = new int[2]

        when : 'We build a button with a preferred size of 100 x 50 "developer pixel".'
            var panel =
                UI.panel()
                .add(
                    UI.button("Button")
                    .withPrefSize(100, 50)
                    .withStyle( it -> {
                        rawPrefSize[0]      = it.component().getPreferredSize().width
                        rawPrefSize[1]      = it.component().getPreferredSize().height
                        devPixelPrefSize[0] = it.componentPrefWidth()
                        devPixelPrefSize[1] = it.componentPrefHeight()
                        return it
                    })
                )
                .get(JPanel)
        and : 'We unpack the button so that the styler has surely run:'
            var button = panel.components[0]

        then : 'The raw preferred size of the component is scaled by the factor of 2.'
            button.preferredSize == new Dimension(200, 100)
            rawPrefSize[0] == 200
            rawPrefSize[1] == 100
        and : 'But `componentPrefWidth()` and `componentPrefHeight()` are scaled back down to "developer pixel".'
            devPixelPrefSize[0] == 100
            devPixelPrefSize[1] == 50
    }

    def 'Feeding the preferred size back into the styling API requires `componentPrefWidth/Height()` to avoid double scaling.'()
    {
        reportInfo """
            A very common pattern is to read a component's preferred size inside a `Styler`
            and feed it back into a size related styling method, like for example when implementing
            a fold/expand animation where the maximum height grows from `0` to the preferred height.

            The methods on the styling API (like `minHeight(double)` or `maxHeight(double)`) all
            **scale their inputs up** through `UI.scale(..)`. So if you were to read the preferred
            size using the deprecated `component().getPreferredSize()` (which is **already scaled**)
            and pass it straight back in, the value would be scaled **twice**!

            The `componentPrefWidth()` and `componentPrefHeight()` methods solve this by giving you
            the preferred size in "developer pixel", so that the value cleanly round-trips through
            the styling API.
        """
        given : 'We set the scaling factor to 2.0 so that the double scaling becomes observable.'
            SwingTree.get().setUiScaleFactor(2.0f)

        when : """
            We build two buttons, both with a preferred size of 100 x 50 "developer pixel".
            The first reads its preferred height the correct way (`componentPrefHeight()`),
            the second reads it the buggy way (`component().getPreferredSize().height`).
        """
            var panel =
                UI.panel("wrap 1")
                .add(
                    UI.button("Correct")
                    .withPrefSize(100, 50)
                    .withStyle( it -> it
                        .minHeight(it.componentPrefHeight())
                        .maxHeight(it.componentPrefHeight())
                    )
                )
                .add(
                    UI.button("Buggy")
                    .withPrefSize(100, 50)
                    .withStyle( it -> it
                        .minHeight(it.component().getPreferredSize().height)
                        .maxHeight(it.component().getPreferredSize().height)
                    )
                )
                .get(JPanel)
        and : 'We unpack the two buttons:'
            var correct = panel.components[0]
            var buggy   = panel.components[1]

        then : """
            The button using `componentPrefHeight()` round-trips cleanly:
            its min/max height matches the (scaled) preferred height of 100.
        """
            correct.preferredSize.height == 100
            correct.minimumSize.height == 100
            correct.maximumSize.height == 100
        and : """
            The button using the deprecated `component().getPreferredSize().height` is scaled twice:
            its min/max height is 200 instead of the expected 100. This is the bug to avoid!
        """
            buggy.preferredSize.height == 100
            buggy.minimumSize.height == 200
            buggy.maximumSize.height == 200
    }

    def 'You can get a reactive view on the current UI scale to update you components dynamically!'() {
        reportInfo """
            The UI scale factor built into the SwingTree library
            can be viewed reactively and without fearing memory leaks.
            This is done by getting a reactive property view from
            the SwingTree library context.
        """
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and : 'Then we create a reactive property and a list acting as change listener trace...'
            var trace = []
            var reactiveScale = SwingTree.get().getUiScaleView().onChange(From.ALL, {
                trace.add(it.currentValue().orElseThrow())
            })

        when :
            SwingTree.get().setUiScaleFactor(2.0f)
        then :
            trace == [2f]

        when :
            SwingTree.get().setUiScaleFactor(1.234567f)
        then :
            trace == [2f, 1.25f] // rounded

        when :
            SwingTree.get().setUiScaleFactor(42f)
        then :
            trace == [2f, 1.25f, 42f]

        when : 'We set the reactive property to null, to indicate that we no longer need to listen to it!'
            reactiveScale = null
            waitForGarbageCollection()
            SwingTree.get().setUiScaleFactor(3.456f)
        then : 'The trace has not grown, despite setting a new scale globally!'
            trace == [2f, 1.25f, 42f]
    }

    def 'Component dimensions update reactively when UI scale changes'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A button with fixed dimensions'
            var button =
                UI.button("Scale Me!")
                .withPrefSize(100, 50)
                .withMinSize(75, 25)
                .withMaxSize(150, 75)
                .get(JButton)

        expect: 'Initial dimensions are at 1x scale'
            button.preferredSize == new Dimension(100, 50)
            button.minimumSize == new Dimension(75, 25)
            button.maximumSize == new Dimension(150, 75)

        when: 'UI scale changes to 2x'
            SwingTree.get().setUiScaleFactor(2.0f)
            UI.sync() // Wait for UI updates

        then: 'Dimensions are scaled by 2x'
            button.preferredSize == new Dimension(200, 100)
            button.minimumSize == new Dimension(150, 50)
            button.maximumSize == new Dimension(300, 150)

        when: 'UI scale changes to 1.5x'
            SwingTree.get().setUiScaleFactor(1.5f)
            UI.sync()

        then: 'Dimensions are scaled by 1.5x'
            button.preferredSize == new Dimension(150, 75)
            button.minimumSize == new Dimension(113, 38) // 75*1.5=112.5 -> 113, 25*1.5=37.5 -> 38
            button.maximumSize == new Dimension(225, 113) // 150*1.5=225, 75*1.5=112.5 -> 113
    }

    def 'Individual width and height properties update reactively with scale changes'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A text field with individual dimension properties'
            var textField =
                UI.textField("Responsive Field")
                .withPrefWidth(200)
                .withMinWidth(150)
                .withMaxWidth(300)
                .withPrefHeight(30)
                .withMinHeight(25)
                .withMaxHeight(40)
                .get(JTextField)

        expect: 'Initial dimensions at 1x scale'
            textField.preferredSize.width == 200
            textField.minimumSize.width == 150
            textField.maximumSize.width == 300
            textField.preferredSize.height == 30
            textField.minimumSize.height == 25
            textField.maximumSize.height == 40

        when: 'Scale changes to 1.25x'
            SwingTree.get().setUiScaleFactor(1.25f)
            UI.sync()

        then: 'Dimensions scale appropriately'
            textField.preferredSize.width == 250 // 200 * 1.25
            textField.minimumSize.width == 188  // 150 * 1.25 = 187.5 -> 188
            textField.maximumSize.width == 375  // 300 * 1.25
            textField.preferredSize.height == 38 // 30 * 1.25 = 37.5 -> 38
            textField.minimumSize.height == 31  // 25 * 1.25 = 31.25 -> 31
            textField.maximumSize.height == 50  // 40 * 1.25
    }

    def 'Size-exactly properties update reactively across all component types'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'Various components with exact size constraints'
            var panel =
                    UI.panel("wrap 1")
                    .add(UI.button("Button").withSizeExactly(120, 40))
                    .add(UI.label("Label").withWidthExactly(180))
                    .add(UI.textArea("Text").withHeightExactly(60))
                    .add(UI.comboBox(["A", "B"]).withSizeExactly(Size.of(160, 30)))
                    .get(JPanel)

            var button = panel.components[0] as JButton
            var label = panel.components[1] as JLabel
            var textArea = panel.components[2] as JTextArea
            var comboBox = panel.components[3] as JComboBox

        expect: 'Initial sizes at 1x scale'
            button.preferredSize == button.minimumSize
            button.preferredSize == button.maximumSize

            label.preferredSize.width == label.minimumSize.width
            label.preferredSize.width == label.maximumSize.width

            textArea.preferredSize.height == textArea.minimumSize.height
            textArea.preferredSize.height == textArea.maximumSize.height

            comboBox.preferredSize == comboBox.minimumSize
            comboBox.preferredSize == comboBox.maximumSize

            button.preferredSize == new Dimension(120, 40)
            label.preferredSize.width == 180
            textArea.preferredSize.height == 60
            comboBox.preferredSize == new Dimension(160, 30)

        when: 'Scale changes to 1.75x'
            SwingTree.get().setUiScaleFactor(1.75f)
            UI.sync()

        then: 'All components scale their exact sizes'
            button.preferredSize == new Dimension(210, 70) // 120*1.75=210, 40*1.75=70
            label.preferredSize.width == 315 // 180*1.75=315
            textArea.preferredSize.height == 105 // 60*1.75=105
            comboBox.preferredSize == new Dimension(280, 53) // 160*1.75=280, 30*1.75=52.5 -> 53
    }

    def 'Bound property dimensions update reactively with scale changes'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'Properties controlling component dimensions'
            var prefSize = Var.of(Size.of(80, 35))
            var minWidth = Var.of(60)
            var maxHeight = Var.of(45)

        and: 'A toggle button bound to these properties'
            var toggleButton =
                UI.toggleButton("Dynamic Size")
                .withPrefSize(prefSize)
                .withMinWidth(minWidth)
                .withMaxHeight(maxHeight)
                .get(JToggleButton)

        expect: 'Initial dimensions at 1x scale'
            toggleButton.preferredSize == new Dimension(80, 35)
            toggleButton.minimumSize.width == 60
            toggleButton.maximumSize.height == 45

        when: 'Scale changes to 2x and properties update'
            SwingTree.get().setUiScaleFactor(2.0f)
            UI.sync()

        then: 'Dimensions scale with new factor'
            toggleButton.preferredSize == new Dimension(160, 70)
            toggleButton.minimumSize.width == 120
            toggleButton.maximumSize.height == 90

        when: 'Properties change AND scale remains at 2x'
            prefSize.set(Size.of(100, 50))
            minWidth.set(80)
            maxHeight.set(60)
            UI.sync()

        then: 'New property values are also scaled'
            toggleButton.preferredSize == new Dimension(200, 100)
            toggleButton.minimumSize.width == 160
            toggleButton.maximumSize.height == 120

        when: 'Scale changes back to 1x'
            SwingTree.get().setUiScaleFactor(1.0f)
            UI.sync()

        then: 'Dimensions reflect property values at 1x scale'
            toggleButton.preferredSize == new Dimension(100, 50)
            toggleButton.minimumSize.width == 80
            toggleButton.maximumSize.height == 60
    }

    def 'Complex nested layouts maintain proper scaling relationships'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A complex panel with nested components and mixed sizing strategies'
            var mainPanel = UI.panel("wrap 2, insets 10")
                .add(UI.label("Name:").withPrefWidth(80))
                .add(UI.textField().withPrefWidth(200))
                .add(UI.label("Description:").withPrefWidth(80))
                .add(UI.textArea("").withPrefSize(200, 60))
                .add("span 2, center",
                    UI.panel()
                    .add(UI.button("OK").withSizeExactly(90, 30))
                    .add(UI.button("Cancel").withSizeExactly(90, 30))
                )
                .get(JPanel)

            var nameLabel = mainPanel.components[0] as JLabel
            var nameField = mainPanel.components[1] as JTextField
            var descLabel = mainPanel.components[2] as JLabel
            var descArea = mainPanel.components[3] as JTextArea
            var buttonPanel = mainPanel.components[4] as JPanel
            var okButton = buttonPanel.components[0] as JButton
            var cancelButton = buttonPanel.components[1] as JButton

        expect: 'Initial layout proportions at 1x scale'
            nameLabel.preferredSize.width == 80
            nameField.preferredSize.width == 200
            descLabel.preferredSize.width == 80
            descArea.preferredSize == new Dimension(200, 60)
            okButton.preferredSize == new Dimension(90, 30)
            cancelButton.preferredSize == new Dimension(90, 30)

        when: 'Scale changes to 1.5x for better readability'
            SwingTree.get().setUiScaleFactor(1.5f)
            UI.sync()

        then: 'All components scale proportionally maintaining layout relationships'
            nameLabel.preferredSize.width == 120 // 80 * 1.5
            nameField.preferredSize.width == 300 // 200 * 1.5
            descLabel.preferredSize.width == 120 // 80 * 1.5
            descArea.preferredSize == new Dimension(300, 90) // 200*1.5=300, 60*1.5=90
            okButton.preferredSize == new Dimension(135, 45) // 90*1.5=135, 30*1.5=45
            cancelButton.preferredSize == new Dimension(135, 45)
    }

    def 'Style-based dimensions update reactively with scale changes'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'Components with dimensions defined through styling API'
            var styledButton =
                UI.button("Styled Button")
                .withStyle(it -> it
                    .prefSize(120, 40)
                    .minSize(100, 30)
                    .maxSize(150, 50)
                )
                .get(JButton)

            var styledField =
                UI.textField()
                .withStyle( it -> it
                    .prefWidth(180)
                    .minWidth(150)
                    .maxWidth(220)
                    .prefHeight(28)
                )
                .get(JTextField)

        expect: 'Initial styled dimensions at 1x scale'
            styledButton.preferredSize == new Dimension(120, 40)
            styledButton.minimumSize == new Dimension(100, 30)
            styledButton.maximumSize == new Dimension(150, 50)
            styledField.preferredSize == new Dimension(180, 28)
            styledField.minimumSize.width == 150
            styledField.maximumSize.width == 220

        when: 'Scale changes to 1.25x'
            SwingTree.get().setUiScaleFactor(1.25f)
            UI.sync()

        then: 'Styled dimensions scale appropriately'
            styledButton.preferredSize == new Dimension(150, 50) // 120*1.25=150, 40*1.25=50
            styledButton.minimumSize == new Dimension(125, 38) // 100*1.25=125, 30*1.25=37.5 -> 38
            styledButton.maximumSize == new Dimension(188, 63) // 150*1.25=187.5 -> 188, 50*1.25=62.5 -> 63
            styledField.preferredSize == new Dimension(225, 35) // 180*1.25=225, 28*1.25=35
            styledField.minimumSize.width == 188 // 150*1.25=187.5 -> 188
            styledField.maximumSize.width == 275 // 220*1.25=275
    }

    def 'Mixed static and bound dimensions all scale reactively'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A combination of static and property-bound dimensions'
            var dynamicWidth = Var.of(140)
            var dynamicHeight = Var.of(35)

            var panel = UI.panel("wrap 2")
                .add(UI.button("Static").withPrefSize(100, 30))
                .add(UI.button("Dynamic").withPrefWidth(dynamicWidth).withPrefHeight(dynamicHeight))
                .add(UI.button("Mixed").withPrefSize(110, 25).withMinWidth(dynamicWidth))
                .get(JPanel)

            var staticBtn = panel.components[0] as JButton
            var dynamicBtn = panel.components[1] as JButton
            var mixedBtn = panel.components[2] as JButton

        expect: 'Initial sizes at 1x scale'
            staticBtn.preferredSize == new Dimension(100, 30)
            dynamicBtn.preferredSize == new Dimension(140, 35)
            mixedBtn.preferredSize == new Dimension(110, 25)
            mixedBtn.minimumSize.width == 140

        when: 'Scale changes to 1.8x'
            SwingTree.get().setUiScaleFactor(1.8f)
            UI.sync()

        then: 'All dimensions scale including bound properties'
            staticBtn.preferredSize == new Dimension(175, 53)
            dynamicBtn.preferredSize == new Dimension(245, 61)
            mixedBtn.preferredSize == new Dimension(193, 44)
            mixedBtn.minimumSize.width == 245

        when: 'Bound properties change at 1.8x scale'
            dynamicWidth.set(160)
            dynamicHeight.set(40)
            UI.sync()

        then: 'New property values are scaled'
            dynamicBtn.preferredSize == new Dimension(280, 70)
            mixedBtn.minimumSize.width == 280
    }

    def 'Component size constraints work correctly with fractional scaling factors'() {
        given : 'We first reset the UI scale to a simple default!'
            SwingTree.get().setUiScaleFactor(1f)
        and: 'A component with precise dimensions'
            var preciseComponent =
                        UI.textArea("Precise Sizing")
                        .withPrefSize(133, 77)
                        .withMinSize(111, 55)
                        .withMaxSize(155, 99)
                        .withSizeExactly(144, 88)
                        .get(JTextArea)

        expect: 'Initial precise dimensions at 1x scale'
            preciseComponent.preferredSize == new Dimension(144, 88)
            preciseComponent.minimumSize == new Dimension(144, 88)
            preciseComponent.maximumSize == new Dimension(144, 88)

        when: 'Scale changes to 1.333x (common for 125% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.333f)
            UI.sync()

        then: 'Dimensions scale with fractional factors maintaining proportions'
            preciseComponent.preferredSize == new Dimension(180, 110)
            preciseComponent.minimumSize == new Dimension(180, 110)
            preciseComponent.maximumSize == new Dimension(180, 110)
    }

        def 'Font sizes specified with `withFontSize(int)` are scaled by the scaling factor'() {
        reportInfo """
            Just like dimensional properties, font sizes also need to scale appropriately
            when the UI scale factor changes. This ensures that text remains readable and
            properly proportioned relative to other UI elements at different DPI settings.
            
            The `withFontSize(int)` method allows setting a static font size that will
            be automatically scaled by the current UI scale factor.
        """
        given : 'We set the scaling factor to 2.0'
            SwingTree.get().setUiScaleFactor(2.0f)

        when : 'We create components with different font sizes'
            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Button").withFontSize(12))
                .add(UI.label("Label").withFontSize(14))
                .add(UI.textField("TextField").withFontSize(16))
                .add(UI.textArea("TextArea").withFontSize(18))
                .add(UI.comboBox(["Item 1", "Item 2"]).withFontSize(20))
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField
            var textArea = panel.components[3] as JTextArea
            var comboBox = panel.components[4] as JComboBox

        then : 'The font sizes are scaled by the scaling factor'
            button.font.size == 24  // 12 * 2.0
            label.font.size == 28   // 14 * 2.0
            textField.font.size == 32 // 16 * 2.0
            textArea.font.size == 36  // 18 * 2.0
            comboBox.font.size == 40  // 20 * 2.0
    }

    def 'Font sizes specified with `withFontSize(Val<Integer>)` are scaled by the scaling factor'() {
        reportInfo """
            For dynamic applications where font sizes might change based on user preferences
            or application state, SwingTree supports binding font sizes to reactive properties.
            These bound font sizes are also automatically scaled by the UI scale factor,
            providing a consistent reading experience across different display configurations.
        """
        given : 'We set the scaling factor to 1.5f'
            SwingTree.get().setUiScaleFactor(1.5f)
        and : 'We create reactive properties for font sizes'
            var buttonFontSize = Var.of(12)
            var labelFontSize = Var.of(14)
            var fieldFontSize = Var.of(16)

        when : 'We create components with bound font sizes'
            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Button").withFontSize(buttonFontSize))
                .add(UI.label("Label").withFontSize(labelFontSize))
                .add(UI.textField("TextField").withFontSize(fieldFontSize))
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField

        then : 'The initial font sizes are scaled by the scaling factor'
            button.font.size == 18  // 12 * 1.5
            label.font.size == 21   // 14 * 1.5
            textField.font.size == 24 // 16 * 1.5

        when : 'We update the font size properties'
            buttonFontSize.set(16)
            labelFontSize.set(18)
            fieldFontSize.set(20)
            UI.sync() // Wait for UI updates

        then : 'The updated font sizes are also scaled by the scaling factor'
            button.font.size == 24  // 16 * 1.5
            label.font.size == 27   // 18 * 1.5
            textField.font.size == 30 // 20 * 1.5
    }

    def 'Font sizes update reactively when UI scale factor changes'() {
        reportInfo """
            One of the key features of SwingTree's scaling system is that it reacts
            dynamically to changes in the UI scale factor. When the scale factor changes
            (for example, when a user moves an application between different DPI displays),
            all font sizes are automatically recalculated and updated.
            
            This ensures that text remains properly sized and readable regardless of
            the current display configuration.
        """
        given : 'We start with a scale factor of 1.0'
            SwingTree.get().setUiScaleFactor(1.0f)
        and : 'Components with various font sizes'
            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Button").withFontSize(12))
                .add(UI.label("Label").withFontSize(16))
                .add(UI.textField("Field").withFontSize(20))
                .get(JPanel)

            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField

        expect : 'Initial font sizes at 1x scale'
            button.font.size == 12
            label.font.size == 16
            textField.font.size == 20

        when : 'Scale changes to 1.25x (125% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.25f)
            UI.sync()

        then : 'Font sizes scale appropriately'
            button.font.size == 15  // 12 * 1.25
            label.font.size == 20   // 16 * 1.25
            textField.font.size == 25 // 20 * 1.25

        when : 'Scale changes to 1.5x (150% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.5f)
            UI.sync()

        then : 'Font sizes scale to the new factor'
            button.font.size == 18  // 12 * 1.5
            label.font.size == 24   // 16 * 1.5
            textField.font.size == 30 // 20 * 1.5

        when : 'Scale changes to 2.0x (200% DPI scaling)'
            SwingTree.get().setUiScaleFactor(2.0f)
            UI.sync()

        then : 'Font sizes double from original'
            button.font.size == 24  // 12 * 2.0
            label.font.size == 32   // 16 * 2.0
            textField.font.size == 40 // 20 * 2.0
    }

    def 'Bound font sizes update reactively with both property and scale changes'() {
        reportInfo """
            This test demonstrates the powerful combination of reactive properties
            and UI scaling. When both the underlying font size property AND the
            UI scale factor change, the component's font size updates appropriately
            to reflect both changes.
            
            This is particularly useful for applications that need to support both
            user-configurable font sizes and automatic DPI scaling.
        """
        given : 'We start with scale factor 1.0 and a reactive font size'
            SwingTree.get().setUiScaleFactor(1.0f)
            var fontSize = Var.of(14)

        and : 'A component with bound font size'
            var label = UI.label("Dynamic Font").withFontSize(fontSize).get(JLabel)

        expect : 'Initial font size'
            label.font.size == 14

        when : 'Only the scale factor changes to 1.5x'
            SwingTree.get().setUiScaleFactor(1.5f)
            UI.sync()

        then : 'Font size scales with the factor'
            label.font.size == 21  // 14 * 1.5

        when : 'Only the font size property changes to 18'
            fontSize.set(18)
            UI.sync()

        then : 'Font size updates with new property value at current scale'
            label.font.size == 27  // 18 * 1.5

        when : 'Both scale factor and property change'
            SwingTree.get().setUiScaleFactor(2.0f)
            fontSize.set(16)
            UI.sync()

        then : 'Font size reflects both changes'
            label.font.size == 32  // 16 * 2.0
    }

    def 'Font size scaling works correctly with fractional scaling factors'() {
        reportInfo """
            Real-world scaling factors are often fractional values like 1.25, 1.33, or 1.75
            that correspond to common DPI scaling percentages (125%, 133%, 175%).
            This test ensures that font sizes are calculated correctly with these
            fractional factors, maintaining readability and visual consistency.
        """
        given : 'We start with scale factor 1.0'
            SwingTree.get().setUiScaleFactor(1.0f)

        and : 'A component with a specific font size'
            var label = UI.label("Fractional Scaling Test").withFontSize(15).get(JLabel)

        expect : 'Initial font size'
            label.font.size == 15

        when : 'Scale changes to 1.25x (125% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.25f)
            UI.sync()

        then : 'Font size scales with fractional factor'
            label.font.size == 19  // 15 * 1.25 = 18.75 -> rounded to 19

        when : 'Scale changes to 1.333x (133% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.333f)
            UI.sync()

        then : 'Font size scales with repeating decimal factor'
            label.font.size == 19

        when : 'Scale changes to 1.75x (175% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.75f)
            UI.sync()

        then : 'Font size scales with larger fractional factor'
            label.font.size == 26  // 15 * 1.75 = 26.25 -> rounded to 26
    }

    def 'Mixed static and bound font sizes all scale appropriately'() {
        reportInfo """
            In real applications, you'll often have a mix of static and dynamically
            bound font sizes. This test verifies that both approaches work correctly
            together and scale appropriately when the UI scale factor changes.
        """
        given : 'We set scale factor to 1.0 and create a reactive property'
            SwingTree.get().setUiScaleFactor(1.0f)
            var dynamicSize = Var.of(16)

        when : 'We create a panel with mixed font size approaches'
            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Static Small").withFontSize(12))
                .add(UI.label("Dynamic").withFontSize(dynamicSize))
                .add(UI.textField("Static Large").withFontSize(20))
                .get(JPanel)

            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField

        then : 'Initial font sizes'
            button.font.size == 12
            label.font.size == 16
            textField.font.size == 20

        when : 'Scale changes to 1.5x'
            SwingTree.get().setUiScaleFactor(1.5f)
            UI.sync()

        then : 'All font sizes scale appropriately'
            button.font.size == 18  // 12 * 1.5
            label.font.size == 24   // 16 * 1.5
            textField.font.size == 30 // 20 * 1.5

        when : 'Dynamic property changes at scaled factor'
            dynamicSize.set(18)
            UI.sync()

        then : 'Dynamic font size updates while static ones remain scaled'
            button.font.size == 18  // unchanged static size at scale
            label.font.size == 27   // 18 * 1.5
            textField.font.size == 30 // unchanged static size at scale
    }
    def 'Fonts specified with `withFont(Font)` are scaled by the scaling factor'() {
        reportInfo """
            For maximum flexibility in font customization, SwingTree allows setting
            complete Font objects using the `withFont(Font)` method. When a custom font
            is provided this way, its size is automatically scaled by the current UI 
            scale factor, ensuring consistent typography across different display configurations.
            
            This is particularly useful when you need to use specific font families or styles
            while still benefiting from automatic DPI scaling.
        """
        given : 'We set the scaling factor to 2.0'
            SwingTree.get().setUiScaleFactor(2.0f)

        when : 'We create components with different custom fonts'
            var boldFont = new Font("Arial", Font.BOLD, 12)
            var italicFont = new Font("Serif", Font.ITALIC, 14)
            var plainFont = new Font("Monospaced", Font.PLAIN, 16)

            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Bold Button").withFont(boldFont))
                .add(UI.label("Italic Label").withFont(italicFont))
                .add(UI.textField("Plain Field").withFont(plainFont))
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField

        then : 'The font sizes are scaled by the scaling factor while preserving font family and style'
            button.font.style == Font.BOLD
            button.font.size == 24  // 12 * 2.0

            label.font.style == Font.ITALIC
            label.font.size == 28   // 14 * 2.0

            textField.font.style == Font.PLAIN
            textField.font.size == 32 // 16 * 2.0
    }

    def 'Fonts specified with `withFont(Val<Font>)` are scaled by the scaling factor'() {
        reportInfo """
            For dynamic applications where fonts might change based on user preferences,
            theme switching, or other application state, SwingTree supports binding
            complete Font objects to reactive properties. 
            
            When a Font property is bound to a component, the font size is automatically
            scaled by the current UI scale factor. This allows for complex font customization
            while maintaining proper DPI scaling across different displays.
        """
        given : 'We set the scaling factor to 1.5f'
            SwingTree.get().setUiScaleFactor(1.5f)
        and : 'We create reactive properties for fonts'
            var buttonFont = Var.of(UI.Font.of("Arial", UI.FontStyle.BOLD, 12))
            var labelFont = Var.of(UI.Font.of("Serif", UI.FontStyle.ITALIC, 14))
            var fieldFont = Var.of(UI.Font.of("Monospaced", UI.FontStyle.PLAIN, 16))

        when : 'We create components with bound fonts'
            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Button").withFont(buttonFont))
                .add(UI.label("Label").withFont(labelFont))
                .add(UI.textField("TextField").withFont(fieldFont))
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField

        then : 'The initial fonts are scaled by the scaling factor'
            button.font.size == 18  // 12 * 1.5
            label.font.size == 21   // 14 * 1.5
            textField.font.size == 24 // 16 * 1.5

        when : 'We update the font properties with new sizes'
            buttonFont.set(UI.Font.of("Arial", UI.FontStyle.BOLD, 16))
            labelFont.set(UI.Font.of("Serif", UI.FontStyle.ITALIC, 18))
            fieldFont.set(UI.Font.of("Monospaced", UI.FontStyle.PLAIN, 20))
            UI.sync() // Wait for UI updates

        then : 'The updated font sizes are also scaled by the scaling factor'
            button.font.size == 24  // 16 * 1.5
            label.font.size == 27   // 18 * 1.5
            textField.font.size == 30 // 20 * 1.5
    }

    def 'Custom fonts update reactively when UI scale factor changes'() {
        reportInfo """
            One of the key advantages of SwingTree's scaling system is that it works
            seamlessly with custom fonts. When the UI scale factor changes, all custom
            fonts automatically adjust their sizes while preserving their family and style.
            
            This ensures that applications using specialized typography maintain visual
            consistency and readability across different display DPI settings.

            One thing is worth spelling out, because it is easy to get wrong: which two
            numbers each new size is worked out from.

            The first number is the size the application asked for, and it never changes.
            The button in this scenario asks for 14 points, and 14 points is what it is
            still asking for after a dozen changes of the factor. The second number is the
            scale factor that is set right now. At a factor of 1.75 the button's size is
            therefore 14 * 1.75 = 24.5, which rounds to 25.

            There is a second way to work it out, and it is wrong. Instead of the current
            factor you could use the step from the old factor to the new one. The factor
            moves from 1.25 to 1.75, so that step is 1.4, and 14 * 1.4 = 19.6, which rounds
            to 20. Look closely at what 20 is: it is the size 14 points would have at a
            factor of 1.4. But the factor is 1.75, not 1.4. Every further change would
            multiply that mistake again, and after a few of them the text on screen would
            have nothing left to do with the 14 points the application asked for.
        """
        given : 'We start with a scale factor of 1.0'
            SwingTree.get().setUiScaleFactor(1.0f)
        and : 'Components with various custom fonts'
            var customFont1 = new Font("Georgia", Font.BOLD, 14)
            var customFont2 = new Font("Courier New", Font.ITALIC, 16)
            var customFont3 = new Font("Verdana", Font.PLAIN, 18)

            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Georgia Bold").withFont(customFont1))
                .add(UI.label("Courier Italic").withFont(customFont2))
                .add(UI.textField("Verdana Plain").withFont(customFont3))
                .get(JPanel)

            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField

        expect : 'Initial font sizes and styles at 1x scale'
            button.font.style == Font.BOLD
            button.font.size == 14

            label.font.style == Font.ITALIC
            label.font.size == 16

            textField.font.style == Font.PLAIN
            textField.font.size == 18

        when : 'Scale changes to 1.25x (125% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.25f)
            UI.sync()

        then : 'Custom font sizes scale appropriately while preserving family and style'
            button.font.style == Font.BOLD
            button.font.size == 18  // 14 * 1.25 -> rounded to 18

            label.font.style == Font.ITALIC
            label.font.size == 20   // 16 * 1.25

            textField.font.style == Font.PLAIN
            textField.font.size == 23 // 18 * 1.25 = 22.5 -> rounded to 23

        when : 'Scale changes to 1.75x (175% DPI scaling)'
            SwingTree.get().setUiScaleFactor(1.75f)
            UI.sync()

        then : 'Every size is the authored size times the new factor, not the size it happened to have times the step'
            button.font.style == Font.BOLD
            button.font.size == 25  // 14 * 1.75 = 24.5 -> rounded to 25

            label.font.style == Font.ITALIC
            label.font.size == 28   // 16 * 1.75

            textField.font.style == Font.PLAIN
            textField.font.size == 32 // 18 * 1.75 = 31.5 -> rounded to 32
    }

    def 'Bound custom fonts update reactively with both property and scale changes'() {
        reportInfo """
            This test demonstrates the powerful combination of reactive Font properties
            and UI scaling. When both the underlying Font property AND the UI scale factor 
            change, the component's font updates appropriately to reflect both changes.
            
            This enables sophisticated scenarios like theme switching with custom fonts
            that automatically adapt to different display DPI settings.
        """
        given : 'We start with scale factor 1.0 and a reactive font property'
            SwingTree.get().setUiScaleFactor(1.0f)
            var dynamicFont = Var.of(UI.Font.of("Arial", UI.FontStyle.PLAIN, 14))

        and : 'A component with bound custom font'
            var label = UI.label("Dynamic Custom Font").withFont(dynamicFont).get(JLabel)

        expect : 'Initial font properties'
            label.font.style == Font.PLAIN
            label.font.size == 14

        when : 'Only the scale factor changes to 1.5x'
            SwingTree.get().setUiScaleFactor(1.5f)
            UI.sync()

        then : 'Font size scales with the factor while preserving family and style'
            label.font.style == Font.PLAIN
            label.font.size == 21

        when : 'Only the font property changes to a different font with new size'
            dynamicFont.set(UI.Font.of("Times New Roman", UI.FontStyle.BOLD, 16))
            UI.sync()

        then : 'Font updates with new property value at current scale'
            label.font.style == Font.BOLD
            label.font.size == 24  // 16 * 1.5

        when : 'Both scale factor and font property change'
            SwingTree.get().setUiScaleFactor(2.0f)
            dynamicFont.set(UI.Font.of("Courier New", UI.FontStyle.ITALIC, 18))
            UI.sync()

        then : 'Font reflects both changes - new family, style, and scaled size'
            label.font.style == Font.ITALIC
            label.font.size == 36  // 18 * 2.0
    }

    def 'Mixed font specification methods all scale appropriately'() {
        reportInfo """
            Real-world applications often use multiple methods for specifying fonts:
            some components might use custom Font objects, others might use simple 
            font sizes, and others might use reactive properties. This test verifies
            that all these approaches work correctly together and scale appropriately
            when the UI scale factor changes.
        """
        given : 'We set scale factor to 1.0 and create reactive properties'
            SwingTree.get().setUiScaleFactor(1.0f)
            var dynamicFont = Var.of(UI.Font.of("Arial", UI.FontStyle.BOLD, 16))
            var dynamicSize = Var.of(14)

        when : 'We create a panel with mixed font specification approaches'
            var customFont = new Font("Georgia", Font.ITALIC, 18)

            var panel =
                UI.panel("wrap 1")
                .add(UI.button("Custom Font").withFont(customFont))
                .add(UI.label("Dynamic Font Object").withFont(dynamicFont))
                .add(UI.textField("Dynamic Font Size").withFontSize(dynamicSize))
                .add(UI.textArea("Static Font Size").withFontSize(12))
                .get(JPanel)

            var button = panel.components[0] as AbstractButton
            var label = panel.components[1] as JLabel
            var textField = panel.components[2] as JTextField
            var textArea = panel.components[3] as JTextArea

        then : 'Initial font properties'
            button.font.style == Font.ITALIC
            button.font.size == 18

            label.font.style == Font.BOLD
            label.font.size == 16

            textField.font.size == 14
            textArea.font.size == 12

        when : 'Scale changes to 1.5x'
            SwingTree.get().setUiScaleFactor(1.5f)
            UI.sync()

        then : 'All font sizes scale appropriately regardless of specification method'
            button.font.style == Font.ITALIC
            button.font.size == 27

            label.font.style == Font.BOLD
            label.font.size == 24   // 16 * 1.5

            textField.font.size == 21  // 14 * 1.5
            textArea.font.size == 18   // 12 * 1.5

        when : 'Dynamic properties change at scaled factor'
            dynamicFont.set(UI.Font.of("Verdana", UI.FontStyle.PLAIN, 20))
            dynamicSize.set(18)
            UI.sync()

        then : 'Dynamic fonts update while static ones remain scaled'
            button.font.size == 27  // unchanged custom font at scale
            label.font.style == Font.PLAIN
            label.font.size == 30   // 20 * 1.5
            textField.font.size == 27  // 18 * 1.5
            textArea.font.size == 18   // unchanged static size at scale
    }

    def 'A font the user application sets itself becomes the size every later rescaling starts from.'()
    {
        reportInfo """
            Whenever the UI scale factor changes, SwingTree works out a new font size for
            every component it manages. To do that it has to remember two things about each
            component:

            1. A starting size. That is the size of the font the application last handed to
               `setFont(..)`. If the application never called it, the starting size is the
               one the look and feel installed.
            2. The scale factor that was set at the moment that new font arrived.

            For a font the application chose itself, the new size is then the starting size,
            times the current factor, divided by the remembered factor.

            Both remembered values must be replaced every time somebody other than SwingTree
            calls `setFont(..)` on the component. The remembered factor is why: a font handed
            over while the factor is two is already a size meant for a factor of two, and
            reading it as a size meant for a factor of one would double it a second time.

            SwingTree notices those calls by registering a `PropertyChangeListener` on the
            component's own `"font"` property. The listener fires on every `setFont(..)`, and
            every font that arrives while SwingTree is not itself applying a new scale factor
            becomes the new starting size, paired with the factor of that moment.

            Here is what would happen without that listener. Suppose a component remembered
            only the font it was built with, and nothing later. The label in this scenario is
            built at a factor of one and given 20 points. Later, with the factor at two, the
            application gives it 30 points. Now the factor moves to three. The remembered
            starting point is still "20 points, at a factor of one", so the label would jump
            to 20 * 3 = 60 points. The 30 points the application asked for would be gone, and
            no later change of the factor would ever bring them back. With the listener the
            starting point is "30 points, at a factor of two", and the label goes to
            30 * 3 / 2 = 45 points, which is what this scenario checks.
        """
        given : 'A scale factor of one, and a label the application gives a font of 20 points.'
            SwingTree.get().setUiScaleFactor(1f)
            var label = UI.label("Hi").get(JLabel)
            UI.runNow({ label.setFont(new Font("Ubuntu", Font.PLAIN, 20)) })

        expect : 'The label has the 20 points it was given, because the factor has not changed since.'
            label.font.size == 20

        when : 'The factor doubles.'
            SwingTree.get().setUiScaleFactor(2f)
            UI.sync()

        then : 'The 20 points the application asked for are what got doubled.'
            label.font.size == 40

        when : 'The application hands the label a second font of 30 points, with the factor still at two.'
            UI.runNow({ label.setFont(new Font("Ubuntu", Font.PLAIN, 30)) })

        then : 'The label keeps those 30 points exactly. Calling `setFont(..)` is not a change of the scale factor, so nothing is rescaled.'
            label.font.size == 30

        when : 'The factor then goes from two to three.'
            SwingTree.get().setUiScaleFactor(3f)
            UI.sync()

        then : 'The 30 points grew by half, because the factor grew by half. The 20 points of the beginning play no part any more.'
            label.font.size == 45

        when : 'The factor returns to two.'
            SwingTree.get().setUiScaleFactor(2f)
            UI.sync()

        then : 'The label has exactly the 30 points the application handed it, down to the point.'
            label.font.size == 30
    }

    def 'A component with no font of its own inherits a size instead of having one scaled onto it.'()
    {
        reportInfo """
            SwingTree changes the font size of a component only when that component has a
            font of its own. `JComponent.isFontSet()` answers that question: it is true when
            the component holds a font, and false when its font is empty. A component with
            no font of its own returns its parent's font from `Component.getFont()`, so its
            text already grows and shrinks with the parent. Scaling it as well would scale
            the same font size twice.

            A tree cell renderer is the plainest example, and the one that goes worst when a
            component without a font of its own is scaled anyway. A `JTree` paints all of its
            rows with a single `DefaultTreeCellRenderer`, which is a `JLabel`. That renderer
            is meant to have no font of its own, and the JDK takes trouble to keep it that
            way: `DefaultTreeCellRenderer.setFont(..)` throws away any `FontUIResource`
            handed to it and stores null instead, and `DefaultTreeCellRenderer.getFont()`
            then answers with `tree.getFont()`. The renderer is supposed to paint in the
            tree's font.

            Now imagine SwingTree scaled that renderer anyway. The factor goes from one to
            two. The tree and the renderer are reached one after the other, so say the tree
            comes first: its font size is doubled. Then SwingTree reaches the renderer and
            asks it for its font. Back comes the tree's brand new, already doubled size,
            because that is the font `getFont()` forwards. Doubling that a second time gives
            the renderer a font twice the size of the tree's.

            The damage does not stop there. To store the new size SwingTree would call
            `renderer.setFont(..)` with a plain `java.awt.Font`. A plain font is not a
            `FontUIResource`, so `DefaultTreeCellRenderer` keeps it instead of throwing it
            away. From that moment on the renderer has a font of its own, `getFont()` stops
            answering with the tree's font, and no later change of the factor can bring the
            two back together. What the user sees is row labels drawn at twice the size of
            the rest of the window, inside rows measured for the tree's own font.
        """
        given : 'A scale factor of one, and a tree.'
            SwingTree.get().setUiScaleFactor(1f)
            var tree = UI.of(new JTree()).get(JTree)
        and : '''
            Its renderer, holding the `ComponentExtension` that a SwingTree look and feel
            attaches to every component it paints. The extension is the thing that rescales
            a font size, so the renderer has to have one for the mistake to be possible at
            all.
        '''
            var renderer = tree.cellRenderer as DefaultTreeCellRenderer
            ComponentExtension.from(renderer)
        and : '''
            One painted row. `getTreeCellRendererComponent(..)` is where the renderer stores
            the tree it was called for, and `DefaultTreeCellRenderer.getFont()` reads the
            font off that stored tree. Until the renderer has painted once, it has no tree
            to read from.
        '''
            UI.runNow({ renderer.getTreeCellRendererComponent(tree, "leaf", false, false, true, 0, false) })

        expect : 'The renderer answers with the font size of its tree, because it holds no font of its own.'
            !renderer.isFontSet()
            renderer.font.size == tree.font.size

        when : 'The factor doubles.'
            var sizeAtFactorOne = tree.font.size
            SwingTree.get().setUiScaleFactor(2f)
            UI.sync()

        then : 'The font size of the tree grew.'
            tree.font.size > sizeAtFactorOne
        and : 'The renderer still answers with the tree font size exactly, not with twice it.'
            renderer.font.size == tree.font.size

        when : 'The factor goes back to 1.'
            SwingTree.get().setUiScaleFactor(1f)
            UI.sync()
        then : 'The tree is back at the exact font size it started with.'
            tree.font.size == sizeAtFactorOne
    }

    def 'The font size of the text field inside a spinner is scaled exactly once.'()
    {
        reportInfo """
            After a change of the UI scale factor, a `JSpinner` and the text field inside it
            must have the same font size. The number the user reads is drawn by that text
            field. The spinner is the box and the two little arrows around it. If the two
            sizes drift apart, the number is drawn at the wrong size inside a box that is
            the right size.

            Holding them together takes some care, because a spinner is not one component.
            `JSpinner` builds a `JSpinner.NumberEditor`, and that editor holds a
            `JFormattedTextField`. SwingTree manages the spinner and that text field
            separately, so each of them works out a new font size of its own.

            Swing then adds a third party. `BasicSpinnerUI.installListeners()` registers a
            `PropertyChangeListener` on the spinner. Whenever the spinner's `"font"`
            property changes, that listener runs
            `textField.setFont(new FontUIResource(spinner.getFont()))`, copying the
            spinner's new font onto the text field. It does so only while the text field's
            current font is a `UIResource`, that is, a font the look and feel installed
            rather than one the application chose.

            Java 8 hands the font over differently. There the same listener runs
            `textField.setFont(spinner.getFont())`, passing the spinner's font object
            itself, without wrapping it in a `FontUIResource`. A font that SwingTree
            writes is a plain `Font`, so from the first change of the factor onward the
            text field holds a plain font, and Swing never copies the spinner's font onto
            it again. From then on the size of the text field comes from SwingTree's
            arithmetic alone, and nothing corrects it.

            So the moment SwingTree writes the spinner's new font, Swing hands that same,
            already scaled font to the text field. Suppose SwingTree then worked out the
            text field's new size from the font the text field is holding at that instant.
            It would be scaling a size that was scaled a moment earlier. At a factor of two
            the number inside the spinner would come out at four times its original size,
            in a box only twice as large.

            Which of the two SwingTree reaches first is not fixed, and no scenario should
            depend on it. What makes the order stop mattering is the rule that every new
            size is worked out from the size the application asked for and the factor that
            size belongs to, and never from the size a component happens to hold at the
            instant it is asked. This scenario checks the outcome that rule guarantees:
            the text field and the spinner have one and the same font size.

            The arithmetic behind that rule has to be a proportion and nothing else: the
            remembered size, times the current factor, divided by the remembered factor.
            SwingTree once treated a font of the look and feel's default size differently
            and replaced its size with the platform's base size times the factor. On
            Windows a 13 point font became 24 at a factor of two, not 26, and 12 at a
            factor of one, not 13. The spinner never showed that, because a component
            that returns to the factor its font was remembered at gets that font back
            unchanged. The text field on Java 8 did show it. It had remembered the 24 it
            was handed at a factor of two as a font of its own, and half of 24 is 12, in
            a spinner that went back to 13.
        """
        given : '''
            A default font of 14 points, installed on every component type in the
            `UIManager`. SwingTree assumes a base font size per platform: 11 or 12 on
            Windows, 13 on macOS and KDE, 15 on other Linux desktops. A default font of
            14 points matches none of them, so on no platform can a formula built on the
            base size agree with a proportion by coincidence.
        '''
            SwingTree.initializeUsing(it -> it.defaultFont(new Font("Ubuntu", Font.PLAIN, 14), SwingTreeInitConfig.FontInstallation.HARD))
            SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        and : 'A scale factor of one, and a spinner.'
            SwingTree.get().setUiScaleFactor(1f)
            var spinner = UI.spinner(new SpinnerNumberModel(1, 1, 999, 1)).get(JSpinner)
            var textField = (spinner.editor as JSpinner.DefaultEditor).textField
        and : '''
            The `ComponentExtension` that a SwingTree look and feel attaches to every
            component it paints. The extension is the thing that rescales a font size, so the
            spinner and its text field each need one. The text field is given its extension
            first here, because that is the order a look and feel reaches the two in: a
            `JSpinner` builds its editor inside its own constructor, before Swing installs
            the delegate that would style the spinner.
        '''
            ComponentExtension.from(textField)
            ComponentExtension.from(spinner)

        expect : 'The spinner and the text field inside it start out at the same font size.'
            textField.font.size == spinner.font.size

        when : 'The factor doubles.'
            var sizeAtFactorOne = spinner.font.size
            SwingTree.get().setUiScaleFactor(2f)
            UI.sync()

        then : 'The font size of the spinner grew.'
            spinner.font.size > sizeAtFactorOne
        and : 'The text field grew by the same amount, not by twice as much.'
            textField.font.size == spinner.font.size

        when : 'The factor goes back to 1.'
            SwingTree.get().setUiScaleFactor(1f)
            UI.sync()
        then : 'Both are back at the exact font size they started with.'
            textField.font.size == sizeAtFactorOne
            spinner.font.size == sizeAtFactorOne
    }

    def 'A spinner looks the same at a scale factor no matter which factors came before it.'()
    {
        reportInfo """
            The number a spinner shows and the box drawn around it are two fonts on two
            components: `JSpinner` is the box and the two little arrows, and the
            `JFormattedTextField` inside its editor draws the number. Whatever the scale
            factor is, those two sizes have to agree, or the number is drawn at the wrong
            size inside a box that is the right size.

            Below, the factor is set to two, then back to one, then to two a second time.
            The second time it is two, the spinner has to look exactly the way it looked the
            first time. A scale factor means one thing only, and it cannot depend on which
            factors happened to be set before it.

            What makes this hard is a second Swing listener, next to the one on the spinner
            that the previous scenario describes. Java 8 does not have it, Java 11 and every
            later version do: `JSpinner.DefaultEditor` listens to the `"font"` property of
            its text field. Whenever a `UIResource` font arrives there that differs from the
            font of the spinner, the editor at once overwrites the text field with
            `new FontUIResource(spinner.getFont())`. It does so from inside the `setFont(..)`
            call that delivered the differing font, before that call has returned.

            When the factor goes back to one, SwingTree hands the text field its remembered
            factor-one font while the spinner still holds the doubled one, because the
            spinner is rescaled a moment later. So the editor writes the doubled font back
            onto the text field, from inside SwingTree's own call. Once the spinner is
            rescaled too, `BasicSpinnerUI` copies its factor-one font onto the text field,
            and the two agree again.

            Had SwingTree told its own writes apart from foreign ones by comparing the font
            that arrived with the font it had just written, the doubled font would count as
            foreign and be remembered as the size at a factor of one, and the corrective
            font that follows would compare equal to the one SwingTree wrote and be ignored.
            The next doubling would then start from a size that is already doubled. This is
            why a font counts as foreign only when it arrives while SwingTree is not
            applying a scale factor, whatever its value.
        """
        given : 'A spinner, at a scale factor of one, wearing whatever font the look and feel gave it.'
            SwingTree.get().setUiScaleFactor(1f)
            var spinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1))
            var textField = (spinner.editor as JSpinner.DefaultEditor).textField
        and : '''
            The `ComponentExtension` that a SwingTree look and feel attaches to every
            component it paints. The extension is the thing that rescales a font, so the
            spinner and its text field each need one. The text field is given its extension
            first, which is the order a look and feel reaches the two in: a `JSpinner` builds
            its editor, and Swing gives that editor its delegate, inside the `JSpinner`
            constructor, before the spinner is given a delegate of its own.
        '''
            ComponentExtension.from(textField)
            ComponentExtension.from(spinner)

        expect : 'The number and the box around it start out at the same size.'
            textField.font.size == spinner.font.size

        when : 'The scale factor doubles.'
            SwingTree.get().setUiScaleFactor(2f)
            UI.sync()
            var sizeAtFactorTwo = spinner.font.size
        then : 'The number is still the size of its box.'
            textField.font.size == sizeAtFactorTwo

        when : 'The factor goes back to one, and then doubles a second time.'
            SwingTree.get().setUiScaleFactor(1f)
            UI.sync()
            SwingTree.get().setUiScaleFactor(2f)
            UI.sync()
        then : 'The box is exactly where it was the first time the factor was two.'
            spinner.font.size == sizeAtFactorTwo
        and : 'And the number is still the size of that box, rather than twice it.'
            textField.font.size == sizeAtFactorTwo
    }

    def 'The scaling factor is published to the UIManager under "laf.scaleFactor".'()
    {
        reportInfo """
            SwingTree scales what it paints itself, but a layout manager decides how much room
            a component gets, and that has to be scaled by the same number or a scaled component
            ends up in an unscaled hole. MigLayout, which SwingTree lays out with, asks for that
            number under the `UIManager` key `"laf.scaleFactor"` - it reads the key once for every
            logical pixel value it turns into pixels, and a logical pixel is the unit it gives an
            unqualified number in a constraint, as well as the unit of every gap and inset it
            defaults to.

            So the key is not decoration: it is how the layout of every window in the application
            learns the scaling factor. This scenario states that it holds the same number
            `getUiScaleFactor()` reports, and keeps holding it after the factor is changed again.
            The second half is the part worth pinning, because the key is written when the factor
            is set rather than read back from it when asked: a new way of setting the factor which
            forgets to write the key would leave the two disagreeing, and every layout in the
            application would then scale by a number nothing else uses.
        """
        when : 'We set the scaling factor to one and a half.'
            SwingTree.get().setUiScaleFactor(1.5f)
        then : 'The key holds a number, and it is the factor SwingTree reports.'
            UIManager.get("laf.scaleFactor") instanceof Number
            ((Number)UIManager.get("laf.scaleFactor")).floatValue() == SwingTree.get().getUiScaleFactor()
            ((Number)UIManager.get("laf.scaleFactor")).floatValue() == 1.5f

        when : 'We change the scaling factor a second time.'
            SwingTree.get().setUiScaleFactor(2f)
        then : 'The key followed the change instead of keeping the number it was given first.'
            ((Number)UIManager.get("laf.scaleFactor")).floatValue() == SwingTree.get().getUiScaleFactor()
            ((Number)UIManager.get("laf.scaleFactor")).floatValue() == 2f
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    static void waitForGarbageCollection() {
        Object obj = new Object();
        WeakReference ref = new WeakReference<>(obj);
        obj = null;
        while(ref.get() != null) {
            System.gc();
        }
    }
}
