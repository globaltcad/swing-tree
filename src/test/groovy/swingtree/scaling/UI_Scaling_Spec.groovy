package swingtree.scaling


import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.SwingTreeContext
import swingtree.UI

import javax.swing.*
import java.awt.*

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
    def cleanup() {
        SwingTreeContext.reset()
    }

    def 'The dimensionality of components will be scaled by the scaling factor'() {
        given:
            SwingTreeContext.UIScale.setUserScaleFactor(2.0f)

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
                    UI.slider(UI.Align.HORIZONTAL)
                    .withPrefSize(new Dimension(60, 20))
                    .withMinSize(new Dimension(70, 80))
                    .withMaxSize(new Dimension(80, 42))
                    .withSize(120, 40)
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
                .get(JPanel)

        and : 'We unpack the tree of components:'
            var button = panel.components[0]
            var slider = panel.components[1]
            var label = panel.components[2]
            var textField = panel.components[3]

        then : 'The specified dimensions of the components will be scaled by the scaling factor'
            button.preferredSize == new Dimension(200, 100)
            button.minimumSize == new Dimension(150, 50)
            button.maximumSize == new Dimension(140, 100)
            button.size == new Dimension(300, 100)
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
    }


    def 'The dimensionality specified in the styling API are scaled by the scaling factor'() {
        given:
            SwingTreeContext.UIScale.setUserScaleFactor(2.0f)

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
                        .prefSize(new Dimension(60, 20))
                        .minSize(new Dimension(70, 80))
                        .maxSize(new Dimension(80, 42))
                        .size(new Dimension(120, 40))
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


}
