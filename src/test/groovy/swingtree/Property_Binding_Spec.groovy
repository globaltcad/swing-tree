package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import sprouts.Val
import sprouts.Var
import swingtree.components.JSplitButton
import swingtree.layout.Size
import swingtree.threading.EventProcessor
import utility.SwingTreeTestConfigurator

import javax.swing.JPanel
import java.awt.*

@Title("Binding Properties to UI Components")
@Narrative('''

    SwingTree includes support for writing UIs using the MVVM pattern,
    by shipping with a set of properties that can be bound to UI components
    to model their state.
    This specification demonstrates how to bind the properties of
    you view model to the SwingTree UI.

''')
@Subject([Val, Var])
class Property_Binding_Spec extends Specification
{
    enum Accept { YES, NO, MAYBE }

    def setupSpec() {
        SwingTree.initialiseUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED)
        // This is so that the test thread is also allowed to perform UI operations
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def 'We can bind a property to the size of a swing component.'( float uiScale )
    {
        reportInfo"""
            Note that the binding of a Swing-Tree property will only have side effects
            when it is deliberately triggered to execute its side effects.
            This is important to allow you to decide yourself when
            the state of a property is "ready" for display in the UI.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)

        and : 'We create a property representing the size of a component.'
            Val<Size> size = Var.of(Size.of(100, 100))
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withPrefSize(size))
                        .add(UI.button("Click Me").withMinSize(size))
                        .add(UI.textField("Hello World").withMaxSize(size))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The components will have the size of the property.'
            panel.components[0].preferredSize == new Dimension((int)(100 * uiScale), (int)(100 * uiScale))
            panel.components[1].minimumSize == new Dimension((int)(100 * uiScale), (int)(100 * uiScale))
            panel.components[2].maximumSize == new Dimension((int)(100 * uiScale), (int)(100 * uiScale))

        when : 'We change the value of the property.'
            size.set(Size.of(200, 200))
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            panel.components[0].preferredSize == new Dimension((int)(200 * uiScale), (int)(200 * uiScale))
            panel.components[1].minimumSize == new Dimension((int)(200 * uiScale), (int)(200 * uiScale))
            panel.components[2].maximumSize == new Dimension((int)(200 * uiScale), (int)(200 * uiScale))
        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }

    def 'Simple integer properties can be bound to the width or height of components.'( float uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create properties representing the width and heights of a components.'
            Var<Integer> minWidth = Var.of(60)
            Var<Integer> prefHeight = Var.of(40)
            Var<Integer> maxWidth = Var.of(90)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withMinWidth(minWidth))
                        .add(UI.button("Click Me").withPrefHeight(prefHeight))
                        .add(UI.textField("Hello World").withMaxWidth(maxWidth))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The components will have the sizes of the properties.'
            panel.components[0].minimumSize.width == (int) ( 60 * uiScale )
            panel.components[1].preferredSize.height == (int) ( 40 * uiScale )
            panel.components[2].maximumSize.width == (int) ( 90 * uiScale )

        when : 'We change the value of the properties.'
            minWidth.set(100)
            prefHeight.set(80)
            maxWidth.set(120)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            panel.components[0].minimumSize.width == (int) ( 100 * uiScale )
            panel.components[1].preferredSize.height == (int) ( 80 * uiScale )
            panel.components[2].maximumSize.width == (int) ( 120 * uiScale )
        where :
            uiScale << [1f, 1.5f, 2f]
    }

    def 'Bind to both width and height independently if you want to.'( int uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
        SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a property representing the width of a component.'
            Var<Integer> width = Var.of(60)
            Var<Integer> height = Var.of(40)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Hello World").withMinSize(width, height))
                        .add(UI.toggleButton("Click Me").withPrefSize(width, height))
                        .add(UI.textArea("Hello World").withMaxSize(width, height))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The components will have the sizes of the properties.'
            panel.components[0].minimumSize == new Dimension(60 * uiScale, 40 * uiScale)
            panel.components[1].preferredSize == new Dimension(60 * uiScale, 40 * uiScale)
            panel.components[2].maximumSize == new Dimension(60 * uiScale, 40 * uiScale)

        when : 'We change the value of the properties.'
            width.set(100)
            height.set(80)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The components will have the new sizes.'
            panel.components[0].minimumSize == new Dimension(100 * uiScale, 80 * uiScale)
            panel.components[1].preferredSize == new Dimension(100 * uiScale, 80 * uiScale)
            panel.components[2].maximumSize == new Dimension(100 * uiScale, 80 * uiScale)
        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }

    def 'We can bind to the color of a component.'()
    {
        given : 'We create a property representing the color of a component.'
            Val<Color> property = Var.of(Color.RED)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.label("I have a Background").withBackground(property))
                        .add(UI.textField("Hello World"))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The label will have the background color of the property.'
            panel.components[1].background == Color.RED

        when : 'We change the value of the property.'
            property.set(Color.BLUE)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The label will have the new color.'
            panel.components[1].background == Color.BLUE
    }

    def 'We can bind to the text of a component.'()
    {
        given : 'We create a property representing the text of a component.'
            Val<String> property = Var.of("Hello World")
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.textField("Hello World").withText(property))
                        .add(UI.checkBox("Hello World"))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The text field will have the text of the property.'
            panel.components[1].text == "Hello World"

        when : 'We change the value of the property.'
            property.set("Goodbye World")
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The text field will have the new text.'
            panel.components[1].text == "Goodbye World"
    }

    def 'We can bind to the `isEditable` flag of a text component.'()
    {
        given : 'We create a property representing the editable state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui =
                        UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.textField("Hello World").isEditableIf(property))
                        .add(UI.textArea("Hello World").isEditableIfNot(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The text field will be editable.'
            panel.components[1].editable == true
        and : 'The text area will be non-editable.'
            panel.components[2].editable == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The text field will be non-editable.'
            panel.components[1].editable == false
        and : 'The text area will be editable.'
            panel.components[2].editable == true
    }


    def 'We can bind to the `Font` property of a text component.'(
        float scalingFactor
    ) {
        given: 'We first initialise SwingTree using the given scaling factor'
            SwingTree.initialiseUsing(it -> it.uiScaleFactor(scalingFactor))
        and : 'We create a property representing the font of a component.'
            Val<UI.Font> property = Var.of(UI.Font.of("Ubuntu", UI.FontStyle.PLAIN, 12))
            property.get().toAwtFont()
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.button("Click Me!"))
                        .add(UI.textField("Hello World").withFont(property))
                        .add(UI.textArea("Hello World").withFont(property.view(f->f.withStyle(UI.FontStyle.ITALIC))))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The text field will have the font of the property.'
            panel.components[1].font.toString() == new Font("Ubuntu", Font.PLAIN, Math.round(12 * scalingFactor) as int).toString()
        and : 'The text area will have the slightly derived font from the property.'
            panel.components[2].font.toString() == new Font("Ubuntu", Font.ITALIC, Math.round(12 * scalingFactor) as int).toString()

        when : 'We change the value of the property.'
            property.set(UI.Font.of("Buggie", UI.FontStyle.BOLD, 16))
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The text field will have the new font.'
            panel.components[1].font.toString() == new Font("Buggie", Font.BOLD, Math.round(16 * scalingFactor) as int).toString()
        and : 'The text area will again have the slightly derived font from the property.'
            panel.components[2].font.toString() == new Font("Buggie", Font.ITALIC, Math.round(16 * scalingFactor) as int).toString()
        where :
            scalingFactor << [1f, 1.25f, 1.5f, 1.75f, 2f]
    }

    def 'We can enable and disable a UI component dynamically through property binding.'()
    {
        given : 'We create a property representing the enabled state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isEnabledIf(property))
                        .add(UI.textArea("I am here for decoration..."))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The spinner will be enabled.'
            panel.components[1].enabled == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be disabled.'
            panel.components[1].enabled == false
    }

    def 'We can select or unselect a UI component dynamically through properties.'()
    {
        given : 'We create a property representing the selected state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a checkbox!"))
                        .add(UI.checkBox("I am a checkbox").isSelectedIf(property))
                        .add(UI.textArea("I am here for decoration..."))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The checkbox will be selected.'
           panel.components[1].selected == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The checkbox will be unselected.'
            panel.components[1].selected == false
    }

    def 'Enable or disable the split items of a JSplitButton through properties.'()
    {
        given : 'We create a property representing the enabled state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.splitButton("I am a split button")
                            .add(UI.splitItem("I am a button").isEnabledIf(property))
                            .add(UI.splitItem("I am a button"))
                            .add(UI.splitItem("I am a button"))
        and : 'We build the component:'
            var splitButton = ui.get(JSplitButton)

        expect : 'The first split item will be enabled.'
            splitButton.popupMenu.components[0].enabled == true

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The first split item will be disabled.'
            splitButton.popupMenu.components[0].enabled == false
    }

    def 'The visibility of a UI component can be modelled dynamically using boolean properties.'()
    {
        given : 'We create a property representing the visibility state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isVisibleIf(property))
                        .add(UI.textArea("I am here for decoration..."))
                        .add(UI.slider(UI.Align.VERTICAL).isVisibleIfNot(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The spinner will be visible.'
            panel.components[1].visible == true
        and : 'The slider will be invisible.'
            panel.components[3].visible == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be invisible.'
            panel.components[1].visible == false
        and : 'The slider will be visible.'
            panel.components[3].visible == true
    }

    def 'The visibility of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are type safe and descriptive. 
            A common use case is to have certain UI components only visible if a certain enum value
            in your view model is selected.

            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the visibility state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isVisibleIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isVisibleIfNot(Accept.YES, property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'Initially the bound button will be visible.'
            panel.components[1].visible == true
        and : 'The bound label will be invisible.'
            panel.components[2].visible == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be invisible.'
            panel.components[1].visible == false
        and : 'The bound label will be visible.'
            panel.components[2].visible == true
    }

    def 'The enabled/disabled state of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are type safe and descriptive. 
            A common use case is to have certain UI components only enabled if a certain enum value
            in your view model is selected.
            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the enabled state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isEnabledIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isEnabledIfNot(Accept.YES, property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'Initially the bound button will be enabled.'
            panel.components[1].enabled == true
        and : 'The bound label will be disabled.'
            panel.components[2].enabled == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be disabled.'
            panel.components[1].enabled == false
        and : 'The bound label will be enabled.'
            panel.components[2].enabled == true
    }

    def 'The focusability of a UI component can be modelled using an enum property.'()
    {
        reportInfo """
            Enums are a common tool for modelling user choices and settings in you view models
            because they are descriptive and type safe (not like string values). 
            A common use case is to have certain UI components only focused if a certain enum value
            in your view model is selected.
            For this example, we will use the following enum:
            ```
                enum Accept { YES, NO, MAYBE }
            ```
        """
        given : 'We create a property representing the focusability state of a component.'
            Var<Accept> property = Var.of(Accept.YES)
        and : 'We create a UI with the enum property based binding.'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("If you accept the terms we can proceed!"))
                        .add(UI.button("Yes proceed!").isFocusableIf(Accept.YES, property))
                        .add(UI.label("Maybe or No is not enough :/").isFocusableIfNot(Accept.YES, property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'Initially the bound button will be focusable.'
            panel.components[1].focusable == true
        and : 'The bound label will be unfocusable.'
            panel.components[2].focusable == false

        when : 'We change the value of the property.'
            property.set(Accept.NO)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The bound button will be unfocusable.'
            panel.components[1].focusable == false
        and : 'The bound label will be focusable.'
            panel.components[2].focusable == true
    }

    def 'The focusability of a UI component can be modelled dynamically using boolean properties.'()
    {
        given : 'We create a property representing the focusability state of a component.'
            Val<Boolean> property = Var.of(true)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a spinner!"))
                        .add(UI.spinner().isFocusableIf(property))
                        .add(UI.textArea("I am here for decoration..."))
                        .add(UI.slider(UI.Align.VERTICAL).isFocusableIfNot(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)
        expect : 'The spinner will be focusable.'
            panel.components[1].focusable == true
        and : 'The slider will be unfocusable.'
            panel.components[3].focusable == false

        when : 'We change the value of the property.'
            property.set(false)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The spinner will be unfocusable.'
            panel.components[1].focusable == false
        and : 'The slider will be focusable.'
            panel.components[3].focusable == true
    }

    def 'Minimum as well as maximum height of UI components can be modelled using integer properties.'( int uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a property representing the minimum and maximum height.'
            Val<Integer> property = Var.of(50)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withMinHeight(property))
                        .add(UI.label("Below me is another text area!"))
                        .add(UI.textArea("Hey").withMaxHeight(property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The minimum height of the first text area will be 50 * uiScale.'
            panel.components[1].minimumSize.height == 50 * uiScale
        and : 'The maximum height of the second text area will be 50 * uiScale.'
            panel.components[3].maximumSize.height == 50 * uiScale

        when : 'We change the value of the property.'
            property.set(100)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The minimum height of the first text area will be 100 * uiScale.'
            panel.components[1].minimumSize.height == 100 * uiScale
        and : 'The maximum height of the second text area will be 100 * uiScale.'
            panel.components[3].maximumSize.height == 100 * uiScale

        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }


    def 'The width and height of UI components can be modelled using integer properties.'( int uiScale )
    {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a property representing the width and height.'
            Val<Integer> widthProperty = Var.of(50)
            Val<Integer> heightProperty = Var.of(100)
        and : 'We create a UI to which we want to bind:'
            var ui = UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withWidth(widthProperty).withHeight(heightProperty))
                        .add(UI.label("Below me is another text area!"))
                        .add(UI.textArea("Hey").withWidth(heightProperty).withHeight(widthProperty))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : 'The width of the first text area will be 50 * uiScale.'
            panel.components[1].size.width == 50 * uiScale
        and : 'The height of the first text area will be 100 * uiScale.'
            panel.components[1].size.height == 100 * uiScale
        and : 'The width of the second text area will be 100 * uiScale.'
            panel.components[3].size.width == 100 * uiScale
        and : 'The height of the second text area will be 50 * uiScale.'
            panel.components[3].size.height == 50 * uiScale

        when : 'We change the value of the property.'
            widthProperty.set(100)
            heightProperty.set(50)
        and : 'Then we wait for the EDT to complete the UI modifications...'
            UI.sync()

        then : 'The dimensions of both UI components will be as expected.'
            panel.components[1].size.width == 100 * uiScale
            panel.components[1].size.height == 50 * uiScale
            panel.components[3].size.width == 50 * uiScale
            panel.components[3].size.height == 100 * uiScale

        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """ 
            uiScale << [3, 2, 1]
    }

    def 'Bind the foreground of a component to a conditional property and 2 color properties.'()
    {
        reportInfo """
            A common use case is to have a foreground color switching between two colors depending on a condition.
            This can be achieved by using properties for the condition and the colors.
            If any of these change in the view model, the UI component will be updated accordingly.
        """
        given : 'We create 3 properties, 1 boolean one and 2 color properties.'
            Val<Boolean> conditionProperty = Var.of(true)
            Val<Color>   color1Property    = Var.of(Color.RED)
            Val<Color>   color2Property    = Var.of(Color.BLUE)
        and : 'We create a UI to which we want to bind:'
            var ui =
                        UI.panel("fill, wrap 1")
                        .add(UI.label("Below me is a text area!"))
                        .add(UI.textArea("hi").withForegroundIf(conditionProperty, color1Property, color2Property))
        and : 'We build the component:'
            var panel = ui.get(JPanel)

        expect : """
                The foreground of the text area will be red, because the condition is true, 
                meaning the first color is selected!
            """
            panel.components[1].foreground == Color.RED

        when : 'We change the value of the condition property.'
            conditionProperty.set(false)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will now switch to blue, because the condition is false.'
            panel.components[1].foreground == Color.BLUE

        when : 'We change the value of the color properties.'
            color1Property.set(Color.GREEN)
            color2Property.set(Color.YELLOW)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will be yellow, because the condition is false.'
            panel.components[1].foreground == Color.YELLOW

        when : 'We change the value of the condition property.'
            conditionProperty.set(true)
            UI.sync() // Wait for the EDT to complete the UI modifications...
        then : 'The foreground of the text area will be green, because the condition is true.'
            panel.components[1].foreground == Color.GREEN
    }

}
