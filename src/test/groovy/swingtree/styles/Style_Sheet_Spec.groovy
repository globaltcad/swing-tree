package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI
import swingtree.style.Arc
import swingtree.style.Outline
import swingtree.style.ShadingStrategy
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
                             apply(id("unique id!"), it ->
                                 it.style().borderRadius(3)
                             );
                             apply(type(JButton.class), it ->
                                 it.style().borderWidth(7)
                             );
                             apply(type(JPanel.class), it ->
                                it.style().borderColor(Color.GREEN)
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
            s1.border().topLeftArc().get() == new Arc(3, 3)
            s1.border().topRightArc().get() == new Arc(3, 3)
            s1.border().bottomLeftArc().get() == new Arc(3, 3)
            s1.border().bottomRightArc().get() == new Arc(3, 3)
            s1.border().widths().average() == 7
            s2.border().widths().average() == 7
            s3.border().color().get() == Color.GREEN
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
                            apply(type(JTextField.class), it ->
                                    it.style().shadowBlurRadius(9)
                                );
                             apply(type(JPanel.class), it ->
                                    it.style().shadowSpreadRadius(33)
                                );
                            apply(type(JTextComponent.class), it ->
                                    it.style()
                                        .shadowOffset(42, 24)
                                        .shadowColor(Color.BLUE)
                                );
                            apply(type(JComponent.class), it ->
                                    it.style()
                                        .shadowColor(Color.RED)
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
            buttonStyle.color().get() == Color.RED
            buttonStyle.blurRadius() == 17
            buttonStyle.spreadRadius() != 33 // a button is not a panel
            buttonStyle.verticalOffset() != 42 // a button is not a text component
            buttonStyle.horizontalOffset() != 24 // a button is not a text component
            panelStyle.color().get() == Color.RED
            panelStyle.blurRadius() == 17
            panelStyle.spreadRadius() == 33
            panelStyle.verticalOffset() != 42 // a panel is not a text component
            panelStyle.horizontalOffset() != 24 // a panel is not a text component
            fieldStyle.color().get() == Color.BLUE // The text component trait overrides the component trait!
            fieldStyle.blurRadius() == 9 // The text field trait overrides the component trait!
            fieldStyle.spreadRadius() != 33 // a text field is not a panel
            fieldStyle.verticalOffset() == 24
            fieldStyle.horizontalOffset() == 42
            areaStyle.color().get() == Color.BLUE // The text component trait overrides the component trait!
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
                             apply(group("group1"), it ->
                                 it.style().backgroundColor(Color.BLUE)
                             );
                             apply(group("group2"), it ->
                                 it.style().foundationColor(Color.CYAN)
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
            s1.background().color().get() == Color.BLUE
            s2.background().foundationColor().get() == Color.CYAN
            s3.background().color().get() == Color.BLUE
            s3.background().foundationColor().get() == Color.CYAN
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
                             apply(group("group1"), it ->
                                 it.style().pad(1, 2, 3, 4)
                             );
                             apply(group("group2").inherits("group1"), it ->
                                 it.style().foundationColor(Color.CYAN)
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
            s1.padding() == Outline.of(1, 2, 3, 4)
            s2.padding() == Outline.of(1, 2, 3, 4)
            s2.background().foundationColor().get() == Color.CYAN
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
                    apply(group("A").type(JButton.class), it ->
                        it.style().borderRadius(3)
                    );
                    apply(group("B").inherits("A").type(JPanel.class), it ->
                        it.style().borderColor(Color.GREEN)
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
                    apply(group("A"), it ->
                        it.style().borderRadius(3)
                    );
                    apply(group("A"), it ->
                        it.style().borderColor(Color.GREEN)
                    );
                }
            }
        then :
            thrown(IllegalArgumentException)
    }

    def 'A StyleSheet can be created with a default style.'()
    {
        reportInfo """
            The default styles always apply if they are not overridden by
            any style traits defined in the style sheet class.
            Note that we pass the default style as a supplier to the
            `StyleSheet` constructor.
            This constructor is protected, so you should always declare
            a dedicated style sheet class that extends `StyleSheet`.
            
            In the code below we can call the constructor as part of an anonymous class
            because we are in a Groovy script :)
        """
        given :
            var ss = new StyleSheet( (style) -> {
                                  style
                                  .foundationColor(Color.RED)
                                  .border(11, Color.GREEN)
                                  .borderRadius(3)
                                  .pad(42)
                                  .shadowIsInset(false)
                                  .shadowBlurRadius(22)
                                  .shadowSpreadRadius(6)
                     }) {
                        @Override
                        protected void declaration() {
                             apply(group("A"), it ->
                                 it.style().borderRadius(19)
                             );
                            apply(group("B").type(JSlider.class), it ->
                                 it.style().foundationColor(Color.BLUE)
                             );
                            apply(group("B").type(JComponent.class), it ->
                                 it.style().shadowIsInset(true)
                             );
                         }
                     }
        when : 'We create a few UI components:'
            var slider1 = UI.slider(UI.Align.HORIZONTAL).groups("A", "B")
            var slider2 = UI.slider(UI.Align.HORIZONTAL).groups("A")
            var slider3 = UI.slider(UI.Align.HORIZONTAL).groups("B")
            var label1 = UI.label(":)").groups("A")
            var label2 = UI.label(":D").groups("B")
        and : 'We run them through the style sheet...'
            var s1 = ss.run(slider1.component)
            var s2 = ss.run(slider2.component)
            var s3 = ss.run(slider3.component)
            var s4 = ss.run(label1.component)
            var s5 = ss.run(label2.component)
        then : '...and we check the results'
            s1.background().foundationColor().get() == Color.BLUE
            s1.border().widths().average() == 11
            s1.border().color().get() == Color.GREEN
            s1.border().topLeftArc().get() == new Arc(19, 19)
            s1.border().topRightArc().get() == new Arc(19, 19)
            s1.border().bottomLeftArc().get() == new Arc(19, 19)
            s1.border().bottomRightArc().get() == new Arc(19, 19)
            s1.padding() == Outline.of(42, 42, 42, 42)
            s1.shadow().isInset() == true
            s1.shadow().blurRadius() == 22
            s1.shadow().spreadRadius() == 6
            s2.background().foundationColor().get() == Color.RED
            s2.border().widths().average() == 11
            s2.border().color().get() == Color.GREEN
            s2.border().topLeftArc().get() == new Arc(19, 19)
            s2.border().topRightArc().get() == new Arc(19, 19)
            s2.border().bottomLeftArc().get() == new Arc(19, 19)
            s2.border().bottomRightArc().get() == new Arc(19, 19)
            s2.padding() == Outline.of(42, 42, 42, 42)
            s2.shadow().isInset() == false
            s2.shadow().blurRadius() == 22
            s2.shadow().spreadRadius() == 6
            s3.background().foundationColor().get() == Color.BLUE
            s3.border().widths().average() == 11
            s3.border().color().get() == Color.GREEN
            s3.border().topLeftArc().get() == new Arc(3, 3)
            s3.border().topRightArc().get() == new Arc(3, 3)
            s3.border().bottomLeftArc().get() == new Arc(3, 3)
            s3.border().bottomRightArc().get() == new Arc(3, 3)
            s3.padding() == Outline.of(42, 42, 42, 42)
            s3.shadow().isInset() == true
            s3.shadow().blurRadius() == 22
            s3.shadow().spreadRadius() == 6
            s4.background().foundationColor().get() == Color.RED
            s4.border().widths().average() == 11
            s4.border().color().get() == Color.GREEN
            s4.border().topLeftArc().get() == new Arc(19, 19)
            s4.border().topRightArc().get() == new Arc(19, 19)
            s4.border().bottomLeftArc().get() == new Arc(19, 19)
            s4.border().bottomRightArc().get() == new Arc(19, 19)
            s4.padding() == Outline.of(42, 42, 42, 42)
            s4.shadow().isInset() == false
            s4.shadow().blurRadius() == 22
            s4.shadow().spreadRadius() == 6
            s5.background().foundationColor().get() == Color.RED
            s5.border().widths().average() == 11
            s5.border().color().get() == Color.GREEN
            s5.border().topLeftArc().get() == new Arc(3, 3)
            s5.border().topRightArc().get() == new Arc(3, 3)
            s5.border().bottomLeftArc().get() == new Arc(3, 3)
            s5.border().bottomRightArc().get() == new Arc(3, 3)
            s5.padding() == Outline.of(42, 42, 42, 42)
            s5.shadow().isInset() == true
            s5.shadow().blurRadius() == 22
            s5.shadow().spreadRadius() == 6
    }

    def 'You can style the font of a component inside your style sheet.'()
    {
        reportInfo """
            If you specify font information using the styling API then it will
            be used to create a font for the component.
            
            In this example you can see how the styling engine of SwingTree prioritizes
            the different properties of a style trait.
            Namely, it will prioritize explicit group styles over type styles.
        """
        given :
            var ss = new StyleSheet() {
                @Override
                protected void declaration() {
                    apply(group("A"), it ->
                        it.style().font("Arial", 12)
                    );
                    apply(group("B"), it ->
                        it.style().font("Sans", 14)
                    );
                    apply(type(JLabel.class), it ->
                        it.style().font("Papyrus", 15)
                    );
                }
            }
        when : 'We create a few UI components:'
            var label1 = UI.label(":)").groups("A")
            var label2 = UI.label(":D").groups("B")
            var label3 = UI.label(":(") // No group
            var textField = UI.textField().groups("A")
            var textArea = UI.textArea("").groups("B")
        and : 'We run them through the style sheet...'
            var s1 = ss.run(label1.component)
            var s2 = ss.run(label2.component)
            var s3 = ss.run(label3.component)
            var s4 = ss.run(textField.component)
            var s5 = ss.run(textArea.component)
        then : '...and we check the results'
            s1.font().name() == "Arial"
            s1.font().size() == 12
            s2.font().name() == "Sans"
            s2.font().size() == 14
            s3.font().name() == "Papyrus"
            s3.font().size() == 15
            s4.font().name() == "Arial"
            s4.font().size() == 12
            s5.font().name() == "Sans"
            s5.font().size() == 14
    }

    def 'Use the power of `Graphics2D` to render custom backgrounds inside you styles.'()
    {
        reportInfo """
            You can use the `Graphics2D` object to render custom backgrounds
            inside your styles.
        """
        given :
            var ss = new StyleSheet() {
                @Override
                protected void declaration() {
                    apply(group("Gradient"), it ->
                        it.style().backgroundPainter(g2d -> {
                            // Let's render a gradient:
                            var gradient = new GradientPaint(0, 0, Color.RED, 0, 100, Color.BLUE);
                            g2d.setPaint(gradient);
                            g2d.fillRect(0, 0, g2d.getClipBounds().width, g2d.getClipBounds().height);
                        })
                    );
                    apply(group("ChessBoard"), it ->
                        it.style().backgroundPainter(g2d -> {
                            var w = it.component().getWidth() / 8;// We render a checkerboard pattern!
                            var h = it.component().getHeight() / 8;
                            for (var i = 0; i < 8; i++) {
                                for (var j = 0; j < 8; j++) {
                                    if ((i + j) % 2 == 0) {
                                        g2d.setColor(Color.RED);
                                    } else {
                                        g2d.setColor(Color.BLUE);
                                    }
                                    g2d.fillRect(i * w, j * h, w, h);
                                }
                            }
                        })
                    );
                }
            }
        when : 'We create a few UI components:'
            var label1 = UI.label(":)").groups("Gradient")
            var label2 = UI.label(":D").groups("ChessBoard")
        and : 'We run them through the style sheet...'
            var s1 = ss.run(label1.component)
            var s2 = ss.run(label2.component)
        then : '...and we check the results'
            s1.background().hasPainters()
            s1.background().painters().size() == 1
            s2.background().hasPainters()
            s2.background().painters().size() == 1
    }

    def 'The order of inherited style traits determines the order in which they are applied.'()
    {
        reportInfo """
            Inheritance can be when it comes to multiple inheritance, because
            if a style trait inherits from multiple other style traits then
            one might ask: "In which order are the inherited style traits
            applied?".
            
            SwingTree solves this problem by simply applying the inherited
            style traits in the order in which the inheritance was declared.
            So for a style trait declared as `group("A").inherits("B", "C")`
            the style traits of group "B" will be applied first, then the
            style traits of group "C" and finally the style traits of group
            "A" itself.
            
            Take a look at the following example:
        """
        given :
            var ss = new StyleSheet() {
                @Override
                protected void declaration() {
                    apply(group("A").inherits("B", "C"), it ->
                        it.style()
                        .borderShade( s -> s.strategy(ShadingStrategy.BOTTOM_TO_TOP).colors(Color.RED, Color.BLUE) )
                    );
                    apply(group("B"), it ->
                        it.style()
                            .borderWidth(10)
                            .borderColor(Color.GREEN)
                    );
                    apply(group("C"), it ->
                        it.style()
                            .borderWidth(20)
                            .borderColor(Color.YELLOW)
                            .borderShade("named shade",
                                s -> s.strategy(ShadingStrategy.TOP_TO_BOTTOM).colors(Color.CYAN, Color.MAGENTA)
                            )
                    );
                }
            }
        when : 'We create a single UI component using style group "A":'
            var label = UI.label(":)").groups("A")
        and : 'We run it through the style sheet...'
            var s = ss.run(label.component)
        then : '...and we check the results'
            s.border().widths().top().get() == 10
            s.border().widths().left().get() == 10
            s.border().widths().bottom().get() == 10
            s.border().widths().right().get() == 10
            s.border().color().get() == Color.GREEN
            s.border().shade().strategy() == ShadingStrategy.BOTTOM_TO_TOP
            s.border().shade().colors() as java.util.List == [Color.RED, Color.BLUE]
            s.border().shade("named shade").strategy() == ShadingStrategy.TOP_TO_BOTTOM
            s.border().shade("named shade").colors() as java.util.List == [Color.CYAN, Color.MAGENTA]
    }
}
