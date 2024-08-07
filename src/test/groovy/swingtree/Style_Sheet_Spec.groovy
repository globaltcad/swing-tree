package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.UI.Layer
import swingtree.style.ShadowConf
import swingtree.threading.EventProcessor
import swingtree.style.Arc

import swingtree.style.Outline

import swingtree.style.StyleConf
import swingtree.style.StyleSheet
import utility.SwingTreeTestConfigurator

import javax.swing.*
import javax.swing.text.JTextComponent
import java.awt.*

@Title("Creating Style Sheets")
@Narrative("""

    No, SwingTree does not have a CSS parser.  
    It does, however, have something better, which looks similar to CSS.
    An API for configuring styles in a declarative and type-safe way.
    This API is also designed the concept of defining common style rules
    withing a file called a "style sheet".
    
    The main difference to a CSS file however is that it is still plain Java code.
    (Or any other JVM language for that matter.)
    
    The API for creating style sheets is primarily exposed through the `StyleSheet` class,
    an abstract class that you can extend to create your own styles like so:
    ```java
        class MyStyleSheet extends StyleSheet {
           @Override
           protected void configure() {
                add(id("some unique id!"), it -> it
                    .borderRadius(3)
                    // etc...
                );
                add(type(JPanel.class), it -> it
                   .borderColor(Color.GREEN)
                   // etc...
                );
                add(type(JButton.class), it -> it
                    .borderWidth(7)
                    // etc...
                );
            }
        }
    ```
    You can then easily apply this style sheet to you SwingTree views like so:
    ```java
        UI.use(new MyStyleSheet(), () -> new MyView());
    ```
    The style sheet will be applied to all UI components created within the scope of the `use` method.
    
""")
@Subject([StyleSheet, StyleConf])
class Style_Sheet_Spec extends Specification
{
    def setupSpec() {
        SwingTree.initialiseUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def cleanupSpec() {
        SwingTree.clear()
    }

    def setup() {
        // We reset to the default look and feel:
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
        // This is to make sure that the tests are not influenced by
        // other look and feels that might be used in the example code...
    }

    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }

    def 'Write custom style sheet classes by extending the StyleSheet class.'()
    {
        reportInfo """
            You can think of a `StyleSheet` as a collection of `StyleTrait`s 
            forming a function for processing a `StyleConf` object.
            
            So when you send a `StyleConf` object through a `StyleSheet` then
            a new `StyleConf` object with the updated style properties will be returned.
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void configure() {
                             add(id("unique id!"), it ->
                                 it.borderRadius(3)
                             );
                             add(type(JButton.class), it ->
                                 it.borderWidth(7)
                             );
                             add(type(JPanel.class), it ->
                                it.borderColor(Color.GREEN)
                             );
                         }
                     }
        and : 'A few components we are going to style'
            var button = UI.button("hi").id("unique id!").get(JButton)
            var button2 = UI.button("wassup?").get(JButton)
            var panel = UI.panel().get(JPanel)
        when :
            var s1 = ss.applyTo(button)
            var s2 = ss.applyTo(button2)
            var s3 = ss.applyTo(panel)
        then :
            s1.border().topLeftArc().get() == Arc.of(3, 3)
            s1.border().topRightArc().get() == Arc.of(3, 3)
            s1.border().bottomLeftArc().get() == Arc.of(3, 3)
            s1.border().bottomRightArc().get() == Arc.of(3, 3)
            s1.border().widths().top().get() == 7
            s1.border().widths().bottom().get() == 7
            s1.border().widths().left().get() == 7
            s1.border().widths().right().get() == 7
            s2.border().widths().top().get() == 7
            s2.border().widths().bottom().get() == 7
            s2.border().widths().left().get() == 7
            s2.border().widths().right().get() == 7
            s3.border().colors().everyColor( c -> c == Color.GREEN )
    }

    def 'The `type` style trait factory method determines for which components a particular style should be applied.'()
    {
        reportInfo """
            A `StyleTrait` with the `Class` object of a particular `JComponent` 
            created from the `type` method will ensure that the style is only applied to
            components of that type (or any of it's sub-types).
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void configure() {
                            add(type(JTextField.class), it -> it
                                .shadowBlurRadius(9)
                            );
                            add(type(JPanel.class), it -> it
                                .shadowSpreadRadius(33)
                            );
                            add(type(JTextComponent.class), it -> it
                                .shadowOffset(42, 24)
                                .shadowColor(Color.BLUE)
                            );
                            add(type(JComponent.class), it -> it
                                .shadowColor(Color.RED)
                                .shadowBlurRadius(17)
                            );
                         }
                     }
        and : 'A few components we are going to style using the style sheet.'
            var textField = UI.textField("type something").get(JTextField)
            var button = UI.button("click me!").get(JButton)
            var panel = UI.panel().get(JPanel)
            var textArea = UI.textArea("type some more!").get(JTextArea)

        when : 'We first run the text field through the style sheet...'
            var fieldStyle  = ss.applyTo(textField).shadow()
        then : 'The resulting style has the expected properties:'
            fieldStyle.color().get() == Color.BLUE // The text component trait overrides the component trait!
            fieldStyle.blurRadius() == 9 // The text field trait overrides the component trait!
            fieldStyle.spreadRadius() != 33 // a text field is not a panel
            fieldStyle.verticalOffset() == 24
            fieldStyle.horizontalOffset() == 42

        when : 'We then run the button through the style sheet...'
            var buttonStyle = ss.applyTo(button).shadow()
        then : 'Again, the resulting style is as expected:'
            buttonStyle.color().get() == Color.RED
            buttonStyle.blurRadius() == 17
            buttonStyle.spreadRadius() != 33 // a button is not a panel
            buttonStyle.verticalOffset() != 42 // a button is not a text component
            buttonStyle.horizontalOffset() != 24 // a button is not a text component

        when : 'We now run the panel through the style sheet...'
            var panelStyle  = ss.applyTo(panel).shadow()
        then : 'Also no surprises here:'
            panelStyle.color().get() == Color.RED
            panelStyle.blurRadius() == 17
            panelStyle.spreadRadius() == 33
            panelStyle.verticalOffset() != 42 // a panel is not a text component
            panelStyle.horizontalOffset() != 24 // a panel is not a text component

        when : 'We finally run the text area through the style sheet...'
            var areaStyle   = ss.applyTo(textArea).shadow()
        then : 'It too has the expected style:'
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
            
            You can assign groups to components as part of 
            a SwingTree GUI and then assign style rules to them
            inside your style sheets.
            
            Take a look at the following example:
        """
        given : """
            We create a little example style sheet with two groups
            and that each have their own style rules.
        """
            var ss = new StyleSheet() {
                        @Override
                        protected void configure() {
                             add(group("group1"), it -> it
                                 .backgroundColor(Color.BLUE)
                             );
                             add(group("group2"), it -> it
                                 .foundationColor(Color.CYAN)
                             );
                         }
                     }
        and : 'Also, a few components we are going to style'
            var label = UI.label("hi").group("group1").get(JLabel)
            var toggle = UI.toggleButton("click me!").group("group2").get(JToggleButton)
            var panel = UI.panel().group("group1", "group2").get(JPanel)
        when : 'We run them all through the style sheet...'
            var s1 = ss.applyTo(label)
            var s2 = ss.applyTo(toggle)
            var s3 = ss.applyTo(panel)
        then : 'We can verify that the colors are applied correctly.'
            s1.base().backgroundColor().get() == Color.BLUE
            s2.base().foundationColor().get() == Color.CYAN
            s3.base().backgroundColor().get() == Color.BLUE
            s3.base().foundationColor().get() == Color.CYAN
    }

    def 'The `group` style trait allows for inheritance, meaning a group can inherit from other ones.'()
    {
        reportInfo """
            In CSS, you can use the `class` attribute to classify elements.
            In SwingTree, there is a similar concept called `group` which
            is similar to CSS classes but more powerful, because you can
            specify that a group inherits from other groups.
        """
        given : 'A simple example style sheet with two groups, one of which inherits from the other.'
            var ss = new StyleSheet() {
                        @Override
                        protected void configure() {
                             add(group("group1"), it -> it
                                 .padding(1, 2, 3, 4)
                             );
                             add(group("group2").inherits("group1"), it -> it
                                 .foundationColor(Color.CYAN)
                             );
                         }
                     }
        and : 'A few components we are going to style'
            var ui1 = UI.textField("hi").group("group1")
            var ui2 = UI.textArea("wassup?").group("group2")
        and : 'We build the components:'
            var textField = ui1.get(JTextField)
            var textArea = ui2.get(JTextArea)
        when : 'We run them all through the style sheet...'
            var s1 = ss.applyTo(textField)
            var s2 = ss.applyTo(textArea)
        then : 'We can indeed verify that style 2 inherits from style 1.'
            s1.padding() == Outline.of(1, 2, 3, 4)
            s2.padding() == Outline.of(1, 2, 3, 4)
            s2.base().foundationColor().get() == Color.CYAN
    }

    def 'Nonsensical style trait group inheritance rules will throw an exception!'()
    {
        reportInfo """
            A nonsensical style trait is one that does not make sense in relation to other style traits.
            So for example, a traits with a group identifier `"A"` and another 
            one with a group identifier `"B"` and the one with `"B"` inherits from `"A"`, then it would
            be nonsensical when style trait `"A"` specifies a component type of `JButton` 
            and style trait `"B"` specifies a component type of `JPanel`.
            This is because a component cannot be both a `JButton` and a `JPanel` at the same time.
        """
        given : 'A nonsensical style sheet.'
            var styleSheet =
                    new StyleSheet() {
                       @Override
                       protected void configure() {
                            add(group("A").type(JButton.class), it -> it
                                .borderRadius(3)
                            );
                            add(group("B").inherits("A").type(JPanel.class), it -> it
                                .borderColor(Color.GREEN)
                            );
                        }
                    }
        when : 'We initialized the `StyleSheet` so that it gets configured.'
            styleSheet.reconfigure()
        then : 'We check that an exception was thrown.'
            thrown(IllegalArgumentException)
    }

    def 'Duplicate style trait declarations will not throw an exception!'()
    {
        reportInfo """
            If you try to declare a style trait more than once, then no exception will be thrown
            even though this is technically a programming error as it does not make sense 
            to declare the same style trait more than once.
            They might contain conflicting style properties, which would be nonsensical to allow.
            <br>
            Instead of throwing a fatal exception, SwingTree will throw and catch an exception
            locally inside the style sheet and then simply log the stack trace.
            <br>
            This is because we don't want to crash the entire application just because of a
            dumb little style sheet error like this.
        """
        given : 'We create a style sheet with a duplicate style trait declaration.'
            var styleSheet =
                new StyleSheet() {
                    @Override
                    protected void configure() {
                        add(group("A"), it -> it
                            .borderRadius(3)
                        );
                        add(group("A"), it -> it
                            .borderColor(Color.GREEN)
                        );
                    }
                }
        when : 'We first initialized the `StyleSheet` so that it gets configured.'
            styleSheet.reconfigure()
        then : 'We check that no exception was thrown.'
            noExceptionThrown()
    }

    def 'Any kind of exception inside of the "configure" method of a user StyleSheet will not be fatal!'()
    {
        reportInfo """
            If you throw an exception inside the `configure` method of a user style sheet,
            then it will not be fatal.
            <br>
            Instead of throwing a fatal exception, SwingTree will catch the exception
            locally inside the style sheet and then simply log the stack trace.
            <br>
            This is because we don't want to crash the entire application just because of a
            dumb little style sheet error like this.
            A Style Sheet is a very superficial thing, which is not at all critical to the
            functionality of the application.
        """
        when :
            new StyleSheet() {
               @Override
               protected void configure() {
                    throw new RuntimeException("This is a test exception!");
                }
            }
        then : 'Indeed, no exception was thrown.'
            noExceptionThrown()
    }

    def 'A StyleSheet can be created with a default style.'()
    {
        reportInfo """
            The default style is a `StyleTrait` is declared as `type(JComponent.class)` without 
            any group, id or sub-component properties.
            Therefore it will always serve as a fallback style for all components.
            and all of it's styles will be applied if they are not overridden by
            any other subordinated style traits.
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void configure() {
                            add(type(JComponent.class), it -> it
                                   .foundationColor(Color.RED)
                                   .border(11, Color.GREEN)
                                   .borderRadius(3)
                                   .padding(42)
                                   .shadowIsInset(false)
                                   .shadowBlurRadius(22)
                                   .shadowSpreadRadius(6)
                            );
                             add(group("A"), it -> it
                                 .borderRadius(19)
                             );
                            add(group("B").type(JSlider.class), it -> it
                                .foundationColor(Color.BLUE)
                            );
                            add(group("B").type(JComponent.class), it -> it
                                .shadowIsInset(true)
                            );
                         }
                     }
        when : 'We create a few UI components:'
            var slider1 = UI.slider(UI.Align.HORIZONTAL).group("A", "B").get(JSlider)
            var slider2 = UI.slider(UI.Align.HORIZONTAL).group("A").get(JSlider)
            var slider3 = UI.slider(UI.Align.HORIZONTAL).group("B").get(JSlider)
            var label1 = UI.label(":)").group("A").get(JLabel)
            var label2 = UI.label(":D").group("B").get(JLabel)
        and : 'We run them through the style sheet...'
            var s1 = ss.applyTo(slider1)
            var s2 = ss.applyTo(slider2)
            var s3 = ss.applyTo(slider3)
            var s4 = ss.applyTo(label1)
            var s5 = ss.applyTo(label2)
        then : 'We first verify that they are not equal to another:'
            s1 != s2 && s2 != s3 && s3 != s4 && s4 != s5
        and : 'They are equal to recalculated instances of themselves:'
            s1 == ss.applyTo(slider1)
            s2 == ss.applyTo(slider2)
            s3 == ss.applyTo(slider3)
            s4 == ss.applyTo(label1)
            s5 == ss.applyTo(label2)
        and : 'Then we check the results:'
            s1.base().foundationColor().get() == Color.BLUE
            s1.border().widths().top().get() == 11
            s1.border().widths().left().get() == 11
            s1.border().widths().bottom().get() == 11
            s1.border().widths().right().get() == 11
            s1.border().colors().everyColor( c -> c == Color.GREEN )
            s1.border().topLeftArc().get() == Arc.of(19, 19)
            s1.border().topRightArc().get() == Arc.of(19, 19)
            s1.border().bottomLeftArc().get() == Arc.of(19, 19)
            s1.border().bottomRightArc().get() == Arc.of(19, 19)
            s1.padding() == Outline.of(42, 42, 42, 42)
            s1.shadow().isInset() == true
            s1.shadow().blurRadius() == 22
            s1.shadow().spreadRadius() == 6
            s2.base().foundationColor().get() == Color.RED
            s2.border().widths().top().get() == 11
            s2.border().widths().left().get() == 11
            s2.border().widths().bottom().get() == 11
            s2.border().widths().right().get() == 11
            s2.border().colors().everyColor( c -> c == Color.GREEN )
            s2.border().topLeftArc().get() == Arc.of(19, 19)
            s2.border().topRightArc().get() == Arc.of(19, 19)
            s2.border().bottomLeftArc().get() == Arc.of(19, 19)
            s2.border().bottomRightArc().get() == Arc.of(19, 19)
            s2.padding() == Outline.of(42, 42, 42, 42)
            s2.shadow().isInset() == false
            s2.shadow().blurRadius() == 22
            s2.shadow().spreadRadius() == 6
            s3.base().foundationColor().get() == Color.BLUE
            s3.border().widths().top().get() == 11
            s3.border().widths().left().get() == 11
            s3.border().widths().bottom().get() == 11
            s3.border().widths().right().get() == 11
            s3.border().colors().everyColor( c -> c == Color.GREEN )
            s3.border().topLeftArc().get() == Arc.of(3, 3)
            s3.border().topRightArc().get() == Arc.of(3, 3)
            s3.border().bottomLeftArc().get() == Arc.of(3, 3)
            s3.border().bottomRightArc().get() == Arc.of(3, 3)
            s3.padding() == Outline.of(42, 42, 42, 42)
            s3.shadow().isInset() == true
            s3.shadow().blurRadius() == 22
            s3.shadow().spreadRadius() == 6
            s4.base().foundationColor().get() == Color.RED
            s4.border().widths().top().get() == 11
            s4.border().widths().left().get() == 11
            s4.border().widths().bottom().get() == 11
            s4.border().widths().right().get() == 11
            s4.border().colors().everyColor( c -> c == Color.GREEN )
            s4.border().topLeftArc().get() == Arc.of(19, 19)
            s4.border().topRightArc().get() == Arc.of(19, 19)
            s4.border().bottomLeftArc().get() == Arc.of(19, 19)
            s4.border().bottomRightArc().get() == Arc.of(19, 19)
            s4.padding() == Outline.of(42, 42, 42, 42)
            s4.shadow().isInset() == false
            s4.shadow().blurRadius() == 22
            s4.shadow().spreadRadius() == 6
            s5.base().foundationColor().get() == Color.RED
            s5.border().widths().top().get() == 11
            s5.border().widths().left().get() == 11
            s5.border().widths().bottom().get() == 11
            s5.border().widths().right().get() == 11
            s5.border().colors().everyColor( c -> c == Color.GREEN )
            s5.border().topLeftArc().get() == Arc.of(3, 3)
            s5.border().topRightArc().get() == Arc.of(3, 3)
            s5.border().bottomLeftArc().get() == Arc.of(3, 3)
            s5.border().bottomRightArc().get() == Arc.of(3, 3)
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
                protected void configure() {
                    add(group("A"), it -> it
                        .font("Arial", 12)
                    );
                    add(group("B"), it -> it
                        .font("Sans", 14)
                    );
                    add(type(JLabel.class), it -> it
                        .font("Papyrus", 15)
                    );
                }
            }
        and : 'We create a few UI components:'
            var label1 = UI.label(":)").group("A").get(JLabel)
            var label2 = UI.label(":D").group("B").get(JLabel)
            var label3 = UI.label(":(").get(JLabel) // No group
            var textField = UI.textField().group("A").get(JTextField)
            var textArea = UI.textArea("").group("B").get(JTextArea)
        when : 'We run them through the style sheet...'
            var s1 = ss.applyTo(label1)
            var s2 = ss.applyTo(label2)
            var s3 = ss.applyTo(label3)
            var s4 = ss.applyTo(textField)
            var s5 = ss.applyTo(textArea)
        then : '...and we check the results'
            s1.font().family() == "Arial"
            s1.font().size() == 12
            s2.font().family() == "Sans"
            s2.font().size() == 14
            s3.font().family() == "Papyrus"
            s3.font().size() == 15
            s4.font().family() == "Arial"
            s4.font().size() == 12
            s5.font().family() == "Sans"
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
                protected void configure() {
                    add(group("Gradient"), it ->
                        it.painter(UI.Layer.BACKGROUND, g2d -> {
                            // Let's render a gradient:
                            var gradient = new GradientPaint(0, 0, Color.RED, 0, 100, Color.BLUE);
                            g2d.setPaint(gradient);
                            g2d.fillRect(0, 0, g2d.getClipBounds().width, g2d.getClipBounds().height);
                        })
                    );
                    add(group("ChessBoard"), it ->
                        it.painter(UI.Layer.BACKGROUND, g2d -> {
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
            var label1 = UI.label(":)").group("Gradient").get(JLabel)
            var label2 = UI.label(":D").group("ChessBoard").get(JLabel)
        and : 'We run them through the style sheet...'
            var s1 = ss.applyTo(label1)
            var s2 = ss.applyTo(label2)
        then : '...and we check the results'
            s1.hasPaintersOnLayer(UI.Layer.BACKGROUND)
            s1.painters(UI.Layer.BACKGROUND).size() == 1
            s1.painters(UI.Layer.CONTENT).size() == 1
            s1.painters(UI.Layer.BORDER).size() == 1
            s1.painters(UI.Layer.FOREGROUND).size() == 1
            s2.hasPaintersOnLayer(UI.Layer.BACKGROUND)
            s2.painters(UI.Layer.BACKGROUND).size() == 1
            s2.painters(UI.Layer.CONTENT).size() == 1
            s2.painters(UI.Layer.BORDER).size() == 1
            s2.painters(UI.Layer.FOREGROUND).size() == 1
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
                protected void configure() {
                    add(group("A").inherits("B", "C"), it -> it
                        .gradient(UI.Layer.BORDER, g -> g
                            .span(UI.Span.BOTTOM_TO_TOP)
                            .colors(Color.RED, Color.BLUE)
                            .clipTo(UI.ComponentArea.BORDER)
                        )
                    );
                    add(group("B"), it -> it
                        .borderWidth(10)
                        .borderColor(Color.GREEN)
                    );
                    add(group("C"), it -> it
                        .borderWidth(20)
                        .borderColor(Color.YELLOW)
                        .gradient(UI.Layer.BORDER, "named shade", s -> s
                            .span(UI.Span.TOP_TO_BOTTOM)
                            .colors(Color.CYAN, Color.MAGENTA)
                            .clipTo(UI.ComponentArea.BORDER)
                        )
                    );
                }
            }
        when : 'We create a single UI component using style group "A":'
            var label = UI.label(":)").group("A").get(JLabel)
        and : 'We run it through the style sheet...'
            var s = ss.applyTo(label)
        then : '...and we check the results'
            s.border().widths().top().get() == 10
            s.border().widths().left().get() == 10
            s.border().widths().bottom().get() == 10
            s.border().widths().right().get() == 10
            s.border().colors().everyColor( c -> c == Color.GREEN )
        and : 'Note that only the default border shade will be overridden, not the named one.'
            s.gradient(UI.Layer.BORDER, "default").span() == UI.Span.BOTTOM_TO_TOP
            s.gradient(UI.Layer.BORDER, "default").colors() as java.util.List == [Color.RED, Color.BLUE]
            s.gradient(UI.Layer.BORDER, "default").area() == UI.ComponentArea.BORDER
            s.gradient(UI.Layer.BORDER, "named shade").span() == UI.Span.TOP_TO_BOTTOM
            s.gradient(UI.Layer.BORDER, "named shade").colors() as java.util.List == [Color.CYAN, Color.MAGENTA]
            s.gradient(UI.Layer.BORDER, "named shade").area() == UI.ComponentArea.BORDER
    }


    def 'You can define complex group inheritance graphs inside your style sheets.'()
    {
        reportInfo """
            This is a more complicated example where we demonstrate how
            complex group inheritance graphs can be defined inside your
            style sheets.
            
            The following example defines the following inheritance graph:
            ```
                    A
                  / | \
                 /  |  \
                B   C   |
                |   |   |
                D   |   |
                 \\  |  /
                  \\ | /
                    E
            ```
        """
        given :
            var ss = new StyleSheet() {
                @Override
                protected void configure() {
                    add(group("A").inherits("E", "B", "C"), it -> it
                        .borderWidth(5)
                        .backgroundColor(Color.RED)
                        .gradient(Layer.BORDER, s -> s
                            .span(UI.Span.BOTTOM_TO_TOP)
                            .colors(Color.RED, Color.BLUE)
                        )
                    );
                    add(group("B").inherits("D"), it -> it
                        .borderWidth(7)
                        .borderColor(Color.GREEN)
                    );
                    add(group("C").inherits("E"), it -> it
                        .borderWidth(11)
                        .borderColor(Color.YELLOW)
                        .shadow("named shadow", s -> s
                            .spreadRadius(10)
                            .color(Color.CYAN)
                            .isInset(true)
                        )
                    );
                    add(group("D").inherits("E"), it -> it
                        .borderWidth(20)
                        .borderColor(Color.WHITE)
                        .shadow("named shadow", s -> s
                            .spreadRadius(20)
                            .color(Color.MAGENTA)
                            .isInset(false)
                        )
                    );
                    add(group("E"), it -> it
                        .borderWidth(42)
                        .borderColor(Color.MAGENTA)
                        .shadow("named shadow", s -> s
                            .spreadRadius(42)
                            .isInset(false)
                        )
                        .font(new Font("Ubuntu", Font.BOLD, 12))
                    );
                }
            }
        when : 'We create a single UI component using style group "A":'
            var button = UI.toggleButton(":)").group("A").get(JToggleButton)
        and : 'We run it through the style sheet...'
            var s = ss.applyTo(button)
        then : '...and we check the results'
            s.border().widths().top().get() == 5
            s.border().widths().left().get() == 5
            s.border().widths().bottom().get() == 5
            s.border().widths().right().get() == 5
            s.border().colors().everyColor( c -> c == Color.MAGENTA )
            s.base().backgroundColor().get() == Color.RED
            s.gradient(Layer.BORDER, "default").span() == UI.Span.BOTTOM_TO_TOP
            s.gradient(Layer.BORDER, "default").colors() as java.util.List == [Color.RED, Color.BLUE]
            s.shadow(UI.Layer.BACKGROUND, "named shadow") == ShadowConf.none()
            s.shadow(UI.Layer.CONTENT, "named shadow").spreadRadius() == 42
            s.shadow(UI.Layer.CONTENT, "named shadow").color().get() == Color.CYAN
            s.shadow(UI.Layer.CONTENT, "named shadow").isInset() == false
            s.shadow(UI.Layer.BORDER, "named shadow") == ShadowConf.none()
            s.shadow(UI.Layer.FOREGROUND, "named shadow") == ShadowConf.none()
            s.font().family() == "Ubuntu"
            s.font().size() == 12
            s.font().posture() == 0
    }
    
    
    def 'The style API of a StyleSheet has a rich set of methods for styling the border of components.'()
    {
        reportInfo """
            In this example we demonstrate how the style API of a `StyleSheet` can be used
            to style the border of components and also how these different border styles
            can be combined and inherited.
            
            A border can have different widths, colors and radii for each of it's edges.
        """
        given :
            var ss = new StyleSheet() {
                        @Override
                        protected void configure() {
                            add(type(JComponent.class), it -> it
                                   .borderWidthAt(UI.Edge.TOP, 11)
                                   .borderWidthAt(UI.Edge.LEFT, 6)
                                   .borderWidthAt(UI.Edge.BOTTOM, 42)
                                   .borderWidthAt(UI.Edge.RIGHT, 7)
                                   .borderColors("green", "red", "blue", "hsb(0.5, 0.5, 0.5)")
                                   .borderRadius(9)
                                   .padding(24, 72, 42, 12)
                            );
                             add(group("A"), it -> it
                                 .borderAt(UI.Edge.TOP, 19, UI.Color.WHITE)
                                 .borderRadiusAt(UI.Corner.TOP_LEFT, 19)
                             );
                            add(group("B").type(JButton.class), it -> it
                                 .borderAt(UI.Edge.LEFT, 3, "rgb(0, 0, 42)")
                                 .borderRadiusAt(UI.Corner.BOTTOM_RIGHT, 3, 5)
                            );
                            add(group("B").type(JLabel.class), it -> it
                                .borderWidthAt(UI.Edge.RIGHT, 4)
                                .borderColors(UI.Color.LIME, UI.Color.MAGENTA, UI.Color.CYAN, UI.Color.YELLOW)
                            );
                         }
                     }
        when : 'We create a few UI components:'
            var button1 = UI.button("X").group("A", "B").get(JButton)
            var button2 = UI.button("Y").group("A").get(JButton)
            var button3 = UI.button("Z").group("B").get(JButton)
            var label1 = UI.label(":)").group("A").get(JLabel)
            var label2 = UI.label(":D").group("B").get(JLabel)
        and : 'We run them through the style sheet...'
            var s1 = ss.applyTo(button1)
            var s2 = ss.applyTo(button2)
            var s3 = ss.applyTo(button3)
            var s4 = ss.applyTo(label1)
            var s5 = ss.applyTo(label2)
        then : 'We first verify that they are not equal to another:'
            s1 != s2 && s2 != s3 && s3 != s4 && s4 != s5
        and : 'They are equal to recalculated instances of themselves:'
            s1 == ss.applyTo(button1)
            s2 == ss.applyTo(button2)
            s3 == ss.applyTo(button3)
            s4 == ss.applyTo(label1)
            s5 == ss.applyTo(label2)
        and : 'We also check that they have the expected commonalities:'
            [s1, s2, s3, s4, s5].every { it.padding() == Outline.of(24, 72, 42, 12) }
            [s1, s2, s3, s4, s5].every { it.border().widths().bottom().get() == 42 }
            [s1, s2, s3, s4, s5].every { it.border().topRightArc().get() == Arc.of(9, 9) }
            [s1, s2, s3, s4, s5].every { it.border().bottomLeftArc().get() == Arc.of(9, 9) }

        and : 'Finally we can also verify that the components have the expected styles:'
            s1.border().widths().top().get() == 19
            s1.border().widths().left().get() == 3
            s1.border().widths().right().get() == 7
            s1.border().colors().top().get() == UI.Color.WHITE
            s1.border().colors().right().get() == UI.Color.RED
            s1.border().colors().bottom().get() == UI.Color.BLUE
            s1.border().colors().left().get() == UI.Color.ofRgb(0, 0, 42)
            s1.border().topLeftArc().get() == Arc.of(19, 19)
            s1.border().bottomRightArc().get() == Arc.of(3, 5)

            s2.border().widths().top().get() == 19
            s2.border().widths().left().get() == 6
            s2.border().widths().right().get() == 7
            s2.border().colors().top().get() == UI.Color.WHITE
            s2.border().colors().right().get() == UI.Color.RED
            s2.border().colors().bottom().get() == UI.Color.BLUE
            s2.border().colors().left().get() == UI.Color.ofRgb(64, 128, 128)
            s2.border().topLeftArc().get() == Arc.of(19, 19)
            s2.border().bottomRightArc().get() == Arc.of(9, 9)

            s3.border().widths().top().get() == 11
            s3.border().widths().left().get() == 3
            s3.border().widths().right().get() == 7
            s3.border().colors().top().get() == UI.Color.GREEN
            s3.border().colors().right().get() == UI.Color.RED
            s3.border().colors().bottom().get() == UI.Color.BLUE
            s3.border().colors().left().get() == UI.Color.ofRgb(0, 0, 42)
            s3.border().topLeftArc().get() == Arc.of(9, 9)
            s3.border().bottomRightArc().get() == Arc.of(3, 5)

            s4.border().widths().top().get() == 19
            s4.border().widths().left().get() == 6
            s4.border().widths().right().get() == 7
            s4.border().colors().top().get() == UI.Color.WHITE
            s4.border().colors().right().get() == UI.Color.RED
            s4.border().colors().bottom().get() == UI.Color.BLUE
            s4.border().colors().left().get() == UI.Color.ofRgb(64, 128, 128)
            s4.border().topLeftArc().get() == Arc.of(19, 19)
            s4.border().bottomRightArc().get() == Arc.of(9, 9)

            s5.border().widths().top().get() == 11
            s5.border().widths().left().get() == 6
            s5.border().widths().right().get() == 4
            s5.border().colors().top().get() == UI.Color.LIME
            s5.border().colors().right().get() == UI.Color.MAGENTA
            s5.border().colors().bottom().get() == UI.Color.CYAN
            s5.border().colors().left().get() == UI.Color.YELLOW
            s5.border().topLeftArc().get() == Arc.of(9, 9)
            s5.border().bottomRightArc().get() == Arc.of(9, 9)
    }

}
