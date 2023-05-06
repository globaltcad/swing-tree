package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
import swingtree.style.Style
import swingtree.style.StyleSheet

import javax.swing.*
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
                             apply(id("unique id!"), style ->{
                                 return style.borderRadius(3)
                             });
                             apply(type(JButton.class), style ->{
                                 return style.border(7)
                             });
                             apply(type(JPanel.class), style ->{
                                return style.border(Color.GREEN)
                             });
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
                             apply(group("group1"), style ->{
                                 return style.background(Color.BLUE)
                             });
                             apply(group("group2"), style ->{
                                 return style.outerBackground(Color.CYAN)
                             });
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
                             apply(group("group1"), style ->{
                                 return style.pad(1, 2, 3, 4)
                             });
                             apply(group("group2").inherits("group1"), style ->{
                                 return style.outerBackground(Color.CYAN)
                             });
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
                    apply(group("A").type(JButton.class), style ->{
                        return style.borderRadius(3)
                    });
                    apply(group("B").inherits("A").type(JPanel.class), style ->{
                        return style.border(Color.GREEN)
                    });
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
                    apply(group("A"), style ->{
                        return style.borderRadius(3)
                    });
                    apply(group("A"), style ->{
                        return style.border(Color.GREEN)
                    });
                }
            }
        then :
            thrown(IllegalArgumentException)
    }
}
