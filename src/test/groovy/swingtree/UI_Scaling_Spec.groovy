package swingtree


import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import sprouts.From
import sprouts.Var
import swingtree.layout.Size
import swingtree.threading.EventProcessor

import javax.swing.*
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
                    .withPrefSize(new Dimension(60, 20))
                    .withMinSize(new Dimension(70, 80))
                    .withMaxSize(new Dimension(80, 42))
                    .withSize(new Dimension(120, 40))
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
            var reactiveScale = SwingTree.get().createAndGetUiScaleView().onChange(From.ALL, {
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
            var buttonFont = Var.of(new Font("Arial", Font.BOLD, 12))
            var labelFont = Var.of(new Font("Serif", Font.ITALIC, 14))
            var fieldFont = Var.of(new Font("Monospaced", Font.PLAIN, 16))

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
            buttonFont.set(new Font("Arial", Font.BOLD, 16))
            labelFont.set(new Font("Serif", Font.ITALIC, 18))
            fieldFont.set(new Font("Monospaced", Font.PLAIN, 20))
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

        then : 'Custom font sizes scale to the new factor'
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
            var dynamicFont = Var.of(new Font("Arial", Font.PLAIN, 14))

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
            dynamicFont.set(new Font("Times New Roman", Font.BOLD, 16))
            UI.sync()

        then : 'Font updates with new property value at current scale'
            label.font.style == Font.BOLD
            label.font.size == 24  // 16 * 1.5

        when : 'Both scale factor and font property change'
            SwingTree.get().setUiScaleFactor(2.0f)
            dynamicFont.set(new Font("Courier New", Font.ITALIC, 18))
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
            var dynamicFont = Var.of(new Font("Arial", Font.BOLD, 16))
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
            dynamicFont.set(new Font("Verdana", Font.PLAIN, 20))
            dynamicSize.set(18)
            UI.sync()

        then : 'Dynamic fonts update while static ones remain scaled'
            button.font.size == 27  // unchanged custom font at scale
            label.font.style == Font.PLAIN
            label.font.size == 30   // 20 * 1.5
            textField.font.size == 27  // 18 * 1.5
            textArea.font.size == 18   // unchanged static size at scale
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
