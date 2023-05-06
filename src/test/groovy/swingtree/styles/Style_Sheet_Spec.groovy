package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
import swingtree.style.Style
import swingtree.style.StyleSheet

import javax.swing.*
import javax.swing.text.JTextComponent
import java.awt.*

@Title("Creating Style Sheets")
@Narrative("""

    No, SwingTree does not have a CSS parser.  
    It does, however, have something better, which is similar to CSS.
    An API for configuring styles in a declarative and type-safe way.

""")
@Subject([StyleSheet, Style])
class Style_Sheet_Spec extends Specification
{
    def 'Write custom style sheet classes by extending the StyleSheet class.'()
    {
        reportInfo """
            You can think of a `StyleSheet` as a collection of `StyleTrait`s 
            forming a function for processing a `Style` object.
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void declaration() {
                             apply(id("unique id!"), style ->
                                 style.borderRadius(3)
                             );
                             apply(type(JButton.class), style ->
                                 style.border(7)
                             );
                             apply(type(JPanel.class), style ->
                                style.border(Color.GREEN)
                             );
                         }
                     }
        and : 'A few components we are going to style'
            var button = UI.button("hi").id("unique id!")
            var button2 = UI.button("wassup?")
            var panel = UI.panel()
        when :
            var s1 = ss.run(button.component)
            var s2 = ss.run(button2.component)
            var s3 = ss.run(panel.component)
        then :
            s1.border().arcHeight() == 3
            s1.border().arcWidth() == 3
            s1.border().thickness() == 7
            s2.border().thickness() == 7
            s3.border().color() == Color.GREEN
    }

    def 'The `type` style trait allows you to specify how a style trait applies to a component types.'()
    {
        reportInfo """
            Passing a `Class` object to the `type` style trait will cause the style trait to apply 
            to all components of that type.
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void declaration() {
                            apply(type(JTextField.class), style ->
                                    style.shadowBlurRadius(9)
                                );
                             apply(type(JPanel.class), style ->
                                    style.shadowSpreadRadius(33)
                                );
                            apply(type(JTextComponent.class), style ->
                                    style.shadowOffset(42, 24)
                                            .shadowColor(Color.BLUE)
                                );
                            apply(type(JComponent.class), style ->
                                    style.shadowColor(Color.RED)
                                            .shadowBlurRadius(17)
                            );
                         }
                     }
        and : 'A few components we are going to style'
            var button = UI.button("click me!")
            var panel = UI.panel()
            var textField = UI.textField("type something")
            var textArea = UI.textArea("type some more!")
        when : 'We run the style sheet on the components and access the shadow style for each component.'
            var buttonStyle = ss.run(button.component).shadow()
            var panelStyle  = ss.run(panel.component).shadow()
            var fieldStyle  = ss.run(textField.component).shadow()
            var areaStyle   = ss.run(textArea.component).shadow()
        then :
            buttonStyle.color() == Color.RED
            buttonStyle.blurRadius() == 17
            buttonStyle.spreadRadius() != 33 // a button is not a panel
            buttonStyle.verticalOffset() != 42 // a button is not a text component
            buttonStyle.horizontalOffset() != 24 // a button is not a text component
            panelStyle.color() == Color.RED
            panelStyle.blurRadius() == 17
            panelStyle.spreadRadius() == 33
            panelStyle.verticalOffset() != 42 // a panel is not a text component
            panelStyle.horizontalOffset() != 24 // a panel is not a text component
            fieldStyle.color() == Color.BLUE // The text component trait overrides the component trait!
            fieldStyle.blurRadius() == 9 // The text field trait overrides the component trait!
            fieldStyle.spreadRadius() != 33 // a text field is not a panel
            fieldStyle.verticalOffset() == 24
            fieldStyle.horizontalOffset() == 42
            areaStyle.color() == Color.BLUE // The text component trait overrides the component trait!
            areaStyle.blurRadius() == 17
            areaStyle.spreadRadius() != 33 // a text area is not a panel
            areaStyle.verticalOffset() == 24
            areaStyle.horizontalOffset() == 42
    }

    def 'Use the `group` style trait to classify components.'()
    {
        reportInfo """
            In CSS, you can use the `class` attribute to classify elements.
            In SwingTree, there is a similar concept called `group`.
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void declaration() {
                             apply(group("group1"), style ->
                                 style.background(Color.BLUE)
                             );
                             apply(group("group2"), style ->
                                 style.outerBackground(Color.CYAN)
                             );
                         }
                     }
        and : 'A few components we are going to style'
            var label = UI.label("hi").groups("group1")
            var toggle = UI.toggleButton("click me!").groups("group2")
            var panel = UI.panel().groups("group1", "group2")
        when :
            var s1 = ss.run(label.component)
            var s2 = ss.run(toggle.component)
            var s3 = ss.run(panel.component)
        then :
            s1.background().color() == Color.BLUE
            s2.background().outerColor() == Color.CYAN
            s3.background().color() == Color.BLUE
            s3.background().outerColor() == Color.CYAN
    }

    def 'The `group` style trait allows for inheritance, meaning a group can inherit from other ones.'()
    {
        reportInfo """
            In CSS, you can use the `class` attribute to classify elements.
            In SwingTree, there is a similar concept called `group` which
            is similar to CSS classes but more powerful, because you can
            specify that a group inherits from other groups.
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void declaration() {
                             apply(group("group1"), style ->
                                 style.pad(1, 2, 3, 4)
                             );
                             apply(group("group2").inherits("group1"), style ->
                                 style.outerBackground(Color.CYAN)
                             );
                         }
                     }
        and : 'A few components we are going to style'
            var textField = UI.textField("hi").groups("group1")
            var textArea = UI.textArea("wassup?").groups("group2")
        when :
            var s1 = ss.run(textField.component)
            var s2 = ss.run(textArea.component)
        then :
            s1.padding().top() == 1
            s1.padding().right() == 2
            s1.padding().bottom() == 3
            s1.padding().left() == 4
            s2.padding().top() == 1
            s2.padding().right() == 2
            s2.padding().bottom() == 3
            s2.padding().left() == 4
            s2.background().outerColor() == Color.CYAN
    }

    def 'Nonsensical style trait group inheritance rules will throw an exception!'()
    {
        reportInfo """
            A nonsensical style trait is one that does not make sense in relation to other style traits.
            So for example, if we have 1 traits, one with a group identifier `"A"` and another 
            one with a group identifier `"B"` and the one with `"B"` inherits from `"A"`, then it would
            it would be nonsensical when style trait `"A"` specifies a component type of `JButton` 
            and style trait `"B"` specifies a component type of `JPanel`.
            This is because a component cannot be both a `JButton` and a `JPanel` at the same time.
        """
        when :
            new StyleSheet() {
               @Override
               protected void declaration() {
                    apply(group("A").type(JButton.class), style ->
                        style.borderRadius(3)
                    );
                    apply(group("B").inherits("A").type(JPanel.class), style ->
                        style.border(Color.GREEN)
                    );
                }
            }
        then :
            thrown(IllegalArgumentException)
    }

    def 'Duplicate style trait declaration will throw an exception!'()
    {
        reportInfo """
            If you try to declare a style trait more than once, then an exception will be thrown.
        """
        when :
            new StyleSheet() {
               @Override
               protected void declaration() {
                    apply(group("A"), style ->
                        style.borderRadius(3)
                    );
                    apply(group("A"), style ->
                        style.border(Color.GREEN)
                    );
                }
            }
        then :
            thrown(IllegalArgumentException)
    }
}
