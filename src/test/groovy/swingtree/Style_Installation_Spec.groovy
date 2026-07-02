package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.Styler
import swingtree.layout.Size
import swingtree.style.ComponentExtension
import swingtree.style.StyleConf
import swingtree.style.StyleSheet

import javax.swing.*
import javax.swing.plaf.metal.MetalButtonUI
import javax.swing.plaf.metal.MetalTextFieldUI
import java.awt.*
import java.awt.image.BufferedImage

@Title("Style Installation")
@Narrative('''

    **This specification covers the behaviour of the style installation process!**
    Which means that the contents of this may not be relevant to you.
    Keep reading however if you are interested in some of the obscure details
    of the SwingTree library internals.

    SwingTree offers advanced styling options as part of **the style API**,
    which is most commonly used through the `withStyle(Styler)` method
    on any declarative builder node.
   
    The installation of styles is a complex process that involves
    the partial override of the component's UI delegate, the application of
    the style's properties to the component and the installation of
    a custom border, all depending on the style configuration.
   
    This is a very finicky process that requires a lot of 
    testing to ensure that the styles are applied correctly.
    Here you will find most of the tests that ensure that after the
    installation of a style, the component has the expected plugin installed.
   
''')
@Subject([UI, Styler])
class Style_Installation_Spec extends Specification
{
    def 'Different `Styler`s may or may not lead to the installation of a custom UI.'(
        boolean isCustom, Styler<JButton> styler
    ){
        reportInfo """
            This is a data driven test that takes a `Styler` 
            which will be applied to a `JButton` by passing it to the
            `withStyle(Styler)` method.
            Then we build the component and check if the custom UI was installed.
            
            This specification may not be relevant to you if you are not interested
            in the details of the SwingTree library internals.
            But it demonstrates the complexity of the style installation process
            and should give you a good idea of what it took to build the SwingTree library.
        """
        given: 'We create a button UI with the given styler'
            var applyStyle = true
            var ui =
                    UI.button()
                    .withSize(80,50)
                    .withStyle( it -> applyStyle ? styler(it) : it )
        when: 'We build the button'
            var button = ui.get(JButton)
        then: 'The custom UI may or may not be installed:'
            !(button.getUI() instanceof MetalButtonUI) == isCustom
        when : """
            We re-install the component UI, to check that if 
            SwingTree style is robust enough to survive look and feel switches.
        """
            button.updateUI()
        then : 'The condition remains unchanged, the style survived:'
            !(button.getUI() instanceof MetalButtonUI) == isCustom

        when : """
            The style is deactivated and updated, then we expect the
            former UI to be reinstalled.
            We test this by deactivating the style
            and then simulating a repaint of the button.
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
        then : 'The original UI should be installed because the component is no longer styled'
            (button.getUI() instanceof MetalButtonUI)

        where :
            isCustom | styler
            false    | { it }
            false    | { it.backgroundColor(Color.BLACK) }
            false    | { it.foregroundColor(Color.BLUE) }
            false    | { it.foundationColor(Color.GREEN) }
            false    | { it.cursor(UI.Cursor.HAND) }
            false    | { it.margin(5) }
            false    | { it.padding(5).margin(5) }
            false    | { it.border(2, "black") }
            false    | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }
            false    | { it.shadowColor("green") }
            false    | { it.shadowColor("blue").shadowBlurRadius(5) }
            false    | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }

            true     | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
            false    | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
            false    | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }

            true     | { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.ALL, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }

            false    | { it.parentFilter( conf -> conf.blur(1) ) }
            false    | { it.parentFilter( conf -> conf.blur(0.75) ) }
            false    | { it.parentFilter( conf -> conf.blur(0.0) ) }
            false    | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
    }

    def 'Applying styles to a regular Swing component may or may not lead to the installation of a custom UI.'(
        boolean isCustom, Styler<JTextField> styler
    ){
        reportInfo """
            SwingTree also supports passing custom components
            to its declarative API and then working with the component
            as if it was SwingTree native (like `UI.TextField` or `UI.Button`).
            This includes support for styling the component.
            
            As expected, SwingTree will try to modify the underlying `ComponentUI`
            to meet your styling requirements!
        """
        given: 'We create a UI declaration for a plain old `JTextField` saying "Hello World!".'
            var applyStyle = true
            var ui =
                    UI.of(new JTextField("Hello World!"))
                    .withSize(95,36)
                    .withStyle( it -> applyStyle ? styler(it) : it )
        when:
            var textField = ui.get(JTextField.class)
        then: 'The `ComponentUI` of the text field may or may not be overridden!'
            !(textField.getUI() instanceof MetalTextFieldUI) == isCustom
        when : """
            We re-install the component UI of the text field, to check that if 
            SwingTree style is robust enough to survive look and feel switches.
        """
            textField.updateUI()
        then :
            !(textField.getUI() instanceof MetalTextFieldUI) == isCustom

        when : """
            We deactivate the custom style, and then simulate the component being used (painted and displayed).
            Internally this should trigger a re-evaluation of the styles...
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            textField.paint(image.createGraphics())
        then : 'The native look and feel is back!'
            (textField.getUI() instanceof MetalTextFieldUI)

        where :
            isCustom | styler
            false    | { it }
            false    | { it.backgroundColor(Color.BLACK) }
            false    | { it.foregroundColor(Color.BLUE) }
            false    | { it.foregroundColor(Color.WHITE).cursor(UI.Cursor.WAIT).margin(2) }
            false    | { it.padding(5).margin(5).border(2, "oak") }
            false    | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }
            false    | { it.shadowColor("green").shadowBlurRadius(5).shadowSpreadRadius(7) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").offset(0,5).spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("salmon").spreadRadius(1).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }

            true     | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
            false    | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
            false    | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }

            true     | { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.ALL, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }

            false    | { it.parentFilter( conf -> conf.blur(1) ) }
            false    | { it.parentFilter( conf -> conf.blur(0.75) ) }
            false    | { it.parentFilter( conf -> conf.blur(0.0) ) }
            false    | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
    }

    def 'Different `Styler`s may or may not lead to the installation of a custom Border.'(
        boolean isCustom, Styler<JButton> styler
    ){
        reportInfo """
            This is a data driven test that takes a `Styler` 
            which will be applied to a `JButton` by passing it to the
            `withStyle(Styler)` method.
            Then we build the component and check if a custom border was installed.
            
            This specification may not be relevant to you if you are not interested
            in the details of the SwingTree library internals.
            But it demonstrates the complexity of the style installation process
            and can give you a good idea of what it took to build the SwingTree library.
        """
        given: 'We create a button UI with the given styler'
            var applyStyle = true
            var ui =
                    UI.button()
                    .withSize(80,50)
                    .withStyle( it -> applyStyle ? styler(it) : it )

        when: 'We build the button'
            var button = ui.get(JButton)
        then: 'The custom `Border` may or may not be installed:'
            (button.getBorder() instanceof swingtree.style.StyleAndAnimationBorder) == isCustom

        when : """
            The style is deactivated and updated, then we expect the
            former border to be reinstalled.
            We test this by deactivating the style
            and then simulating a repaint of the button.
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
        then : """
            The standard look and feel border based border should be installed
            because the component is no longer styled.
            We test this by comparing the border of the button with the border
            of a new button.
        """
            button.getBorder() == new JButton().getBorder()

        where :
            isCustom | styler
            false    | { it }
            false    | { it.backgroundColor(Color.BLACK) }
            false    | { it.foregroundColor(Color.BLUE) }
            false    | { it.foundationColor(Color.GREEN) }
            false    | { it.cursor(UI.Cursor.HAND) }
            true     | { it.margin(5) }
            true     | { it.padding(5).margin(5) }
            true     | { it.border(2, "black") }
            true     | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }
            true     | { it.shadowColor("green") }
            true     | { it.shadowColor("blue").shadowBlurRadius(5) }
            true     | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
            true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) }
            false    | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }

            false    | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
            false    | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }

            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
            false    | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }

            false    | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }

            false    | { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.ALL, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }

            false    | { it.parentFilter( conf -> conf.blur(1) ) }
            false    | { it.parentFilter( conf -> conf.blur(0.75) ) }
            false    | { it.parentFilter( conf -> conf.blur(0.0) ) }
            false    | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
    }

    def 'Different `Styler`s may or may not override the `JButton.setContentAreaFilled(boolean)` property.'(
        boolean isFilled, Styler<JButton> styler
    ){
        reportInfo """
            This is a data driven test that takes a `Styler` 
            which will be applied to a `JButton` by passing it to the
            `withStyle(Styler)` method.
            Then we build the component and check if the "isContentAreaFilled" property
            of a button was or was not modified.
            
            Although not intuitive from the outside perspective, but internally
            SwingTree sometimes needs to set this flag to false in order to
            prevent the look and feel from rendering it so that SwingTree can take over
            and paint its style instead!
            
            This specification may not be relevant to you if you are not interested
            in the details of the SwingTree library internals.
            But it demonstrates the complexity of the style installation process
            and can give you a good idea of what it took to build the SwingTree library.
        """
        given: 'We create a button UI with the given styler turned off initially!'
            var applyStyle = false
            var ui =
                    UI.button()
                    .withSize(80,50)
                    .withStyle( it -> applyStyle ? styler(it) : it )

        when: 'We build the button'
            var button = ui.get(JButton)
        then: 'Initially, the `isContentAreaFilled` is set to true:'
            button.isContentAreaFilled()

        when : """
            The style is activated and updated, then we expect
            SwingTree to evaluate if it is necessary to override the look and feel.
        """
            applyStyle = true
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics()) // We need to simulate the component being painted
        then : """
            The flag has the expected value:
        """
            button.isContentAreaFilled() == isFilled

        when : 'We now turn off the style and update the component...'
            applyStyle = false
            image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics()) // We need to simulate the component being painted
        then: 'The `isContentAreaFilled` is set to true like it was initially:'
            button.isContentAreaFilled()

        where :
            isFilled | styler
            true     | { it }
            true     | { it.backgroundColor(Color.BLACK) }
            true     | { it.foregroundColor(Color.BLUE) }
            true     | { it.foundationColor(Color.GREEN) }
            true     | { it.cursor(UI.Cursor.HAND) }
            true     | { it.margin(5) }
            true     | { it.padding(5).margin(5) }
            true     | { it.border(2, "black") }
            true     | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }
            true     | { it.shadowColor("green") }
            true     | { it.shadowColor("blue").shadowBlurRadius(5) }
            true     | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
            true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) }
            true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) }
            true     | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }

            false    | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
            true     | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }

            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
            true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
            true     | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }

            false    | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }

            false    | { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.ALL, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) }
            true     | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }

            true     | { it.parentFilter( conf -> conf.blur(1) ) }
            true     | { it.parentFilter( conf -> conf.blur(0.75) ) }
            true     | { it.parentFilter( conf -> conf.blur(0.0) ) }
            true     | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
    }

    def 'Style sheets can be dynamically reconfigured at runtime to switch between different visual themes.'()
    {
        reportInfo """
            This test demonstrates how to create a dynamic style sheet that can switch between
            different visual themes at runtime. This is achieved by:
            
            1. Creating a custom StyleSheet implementation with a configure() method that
               uses a switch statement to apply different styles based on a current theme
            2. Binding a SwingTree GUI to this style sheet using UI.use(StyleSheet, Supplier)
            3. Calling reconfigure() on the style sheet to switch themes
            4. Verifying that components receive the new styles
            
            This powerful feature allows you to create applications with dynamic theming
            capabilities, similar to what you might find in modern web applications.
            
            The style sheet in this test switches between three themes:
            - LIGHT: Bright colors with dark text
            - DARK: Dark colors with light text  
            - RAINBOW: A colorful, playful theme
            
            Each theme applies distinct styles to JButton and JLabel components,
            demonstrating how different visual identities can be achieved through
            style sheet reconfiguration.
        """
        given: 'A custom style sheet with theme switching capability'
            var currentTheme = "LIGHT"
            var styleSheet = new StyleSheet() {
                @Override
                protected void configure() {
                    switch (currentTheme) {
                        case "LIGHT":
                            add(type(JButton.class), it -> it
                                .backgroundColor(Color.WHITE)
                                .foregroundColor(Color.BLACK)
                                .border(2, "darkgray")
                                .borderRadius(8)
                                .fontBold(true)
                            )
                            add(type(JLabel.class), it -> it
                                .backgroundColor(new Color(240, 240, 240))
                                .foregroundColor(Color.DARK_GRAY)
                                .fontSize(14)
                                .padding(5)
                            )
                            break
                        case "DARK":
                            add(type(JButton.class), it -> it
                                .backgroundColor(new Color(45, 45, 45))
                                .foregroundColor(Color.WHITE)
                                .border(2, "lightgray")
                                .borderRadius(8)
                                .fontBold(true)
                                .shadowColor("white")
                                .shadowBlurRadius(3)
                            )
                            add(type(JLabel.class), it -> it
                                .backgroundColor(new Color(30, 30, 30))
                                .foregroundColor(Color.LIGHT_GRAY)
                                .fontSize(14)
                                .padding(5)
                            )
                            break
                        case "RAINBOW":
                            add(type(JButton.class), it -> it
                                .gradient(UI.Layer.BACKGROUND, "rainbow", grad -> grad
                                    .colors(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA)
                                    .span(UI.Span.LEFT_TO_RIGHT)
                                )
                                .backgroundColor(Color.PINK)
                                .foregroundColor(Color.BLACK)
                                .borderRadius(12)
                                .fontBold(true)
                                .padding(10)
                            )
                            add(type(JLabel.class), it -> it
                                .backgroundColor(Color.PINK)
                                .foregroundColor(Color.DARK_GRAY)
                                .fontSize(16)
                                .borderRadius(5)
                                .padding(8)
                            )
                            break
                    }
                }
            }

        and: 'A SwingTree GUI bound to our custom style sheet'
            var panel = UI.use(styleSheet) { ->
                                    UI.panel("fill, wrap 1")
                                        .add(UI.button("Test Button"))
                                        .add(UI.label("Test Label"))
                                        .get(JPanel)
                                }

        when: 'We build the UI components with the initial LIGHT theme'
            var button = panel.getComponent(0) as JButton
            var label = panel.getComponent(1) as JLabel

        then: 'The components should have the LIGHT theme styles'
            button.background == Color.WHITE
            button.foreground == Color.BLACK
            label.foreground == Color.DARK_GRAY

        when: 'We switch to DARK theme and reconfigure the style sheet'
            currentTheme = "DARK"
            styleSheet.reconfigure()

            // Force style re-evaluation by simulating a component update
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
            label.paint(image.createGraphics())

        then: 'The components should now have the DARK theme styles'
            button.background == new Color(45, 45, 45)
            button.foreground == Color.WHITE
            label.foreground == Color.LIGHT_GRAY

        when: 'We switch to RAINBOW theme and reconfigure again'
            currentTheme = "RAINBOW"
            styleSheet.reconfigure()

            // Force style re-evaluation
            image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
            label.paint(image.createGraphics())

        then: 'The components should now have the RAINBOW theme styles'
            // For the button, we check that a gradient was installed (custom UI)
            !(button.getUI() instanceof MetalButtonUI)
            label.foreground == Color.DARK_GRAY

        and: 'The style configurations reflect the theme changes'
            var buttonStyle = ComponentExtension.from(button).getStyle()
            var labelStyle = ComponentExtension.from(label).getStyle()

            buttonStyle.base().backgroundColor().get() == Color.PINK // Gradient primer color
            labelStyle.base().backgroundColor().get() == Color.PINK

        when: 'We switch back to LIGHT theme to complete the cycle'
            currentTheme = "LIGHT"
            styleSheet.reconfigure()

            // Force style re-evaluation
            image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
            label.paint(image.createGraphics())

        then: 'The components should return to their original LIGHT theme styles'
            button.background == Color.WHITE
            button.foreground == Color.BLACK
            label.foreground == Color.DARK_GRAY
    }

    def 'A SwingTree can install as well as uninstall a custom font defined in the style API.'(
        boolean fontChanged, Styler<JTextField> styler
    ){
        reportInfo """
            This is a data-driven test verifying that fonts defined via the style API
            are properly installed and uninstalled when styles are toggled.
            It ensures that activating the style changes the font as expected,
            and deactivating the style restores the original font.
        """
        given: 'We create a text field UI with the given styler turned off initially!'
            var applyStyle = false
            var ui =
                    UI.textField("I am simply text... :)")
                    .withSize(110,32)
                    .withStyle( it -> applyStyle ? styler(it) : it )

        and: 'We build the text field...'
            var textField = ui.get(JTextField)
        and: 'We get the initial font installed on the text field:'
            var initialFont = textField.getFont()

        when : """
            The style is activated and updated, then we expect
            SwingTree to evaluate if it is necessary to override the look and feel
            as well as the font property of the component.
        """
            applyStyle = true
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            textField.paint(image.createGraphics()) // We need to simulate the component being painted
        then : 'The font may or may not be changed:'
            ( initialFont != textField.getFont() ) == fontChanged

        when : 'We now turn off the style and update the text field...'
            applyStyle = false
            image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            textField.paint(image.createGraphics()) // We need to simulate the component being painted
        then: 'The text field has the initial font again:'
            initialFont == textField.getFont()

        where :
            fontChanged | styler
             false      | { it }
             false      | { it.backgroundColor(Color.BLACK) }
             false      | { it.foregroundColor(Color.BLUE) }
             false      | { it.cursor(UI.Cursor.HAND) }
             false      | { it.margin(5) }
             false      | { it.padding(5).margin(5) }
             false      | { it.border(2, "black") }
             false      | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }
             false      | { it.shadowColor("green") }
             false      | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
             false      | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
             false      | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
             false      | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
             false      | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
             false      | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
             false      | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
             false      | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
             false      | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }
             false      | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
             false      | { it.parentFilter( conf -> conf.blur(0.0) ) }
             false      | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }

             true       | { it.fontColor("oak").fontBackgroundColor("orange") }
             false      | { it.backgroundColor(Color.BLACK).fontColor("oak") }  // solid font color -> foreground channel, font untouched! (see Font_Color_Channel_Spec)
             true       | { it.foregroundColor(Color.BLUE).fontSize(42) }
             true       | { it.cursor(UI.Cursor.HAND).fontSize(42) }
             true       | { it.margin(5).fontWeight(73).fontColor("oak") }
             true       | { it.border(2, "black").fontBackgroundColor("orange") }
             true       | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS).fontBackgroundColor("orange") }
             false      | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)).fontColor("oak") }  // solid font color -> foreground channel, font untouched! (see Font_Color_Channel_Spec)
             false      | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)).fontColor("oak") }  // solid font color -> foreground channel, font untouched! (see Font_Color_Channel_Spec)
             true       | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])).fontSize(42) }
             false      | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)).fontColor("oak") }  // solid font color -> foreground channel, font untouched! (see Font_Color_Channel_Spec)
             false      | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)).fontColor("oak") }  // solid font color -> foreground channel, font untouched! (see Font_Color_Channel_Spec)
             false      | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}).fontColor("oak") }  // solid font color -> foreground channel, font untouched! (see Font_Color_Channel_Spec)
             true       | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ).fontBackgroundColor("orange") }
             false      | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}).fontColor("oak") }  // solid font color -> foreground channel, font untouched! (see Font_Color_Channel_Spec)

             true       | { it.parentFilter( conf -> conf.blur(0.0) ).fontWeight(73) }
             true       | { it.padding(5).margin(5).fontWeight(73) }
             true       | { it.shadowColor("green").fontSpacing(24) }
             true       | { it.shadowColor("green").fontSpacing(-13) }
             true       | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7).fontSpacing(42) }
             true       | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)).fontWeight(73) }
             true       | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BORDER, "myPainter", g2d -> {}).fontWeight(73) }
    }

    def 'Some styles, which would not lead to any visual effect when rendered, will be simplified to "no-style".'(
        boolean hasEffect, Styler<JButton> styler
    ){
        reportInfo """
            Certain style information does not make any sense in that it
            would not lead to any visual effect at all. For example, a border
            with a width of 0 would not lead to any difference. In such cases, 
            SwingTree will simplify the style and then install that.
            Very often, such a style can be simplified to no style at alL!
        """
        given :
            var applyStyle = true
        and : 'We create a button UI with the given styler:'
            var ui =
                    UI.button("Click me!")
                    .withStyle( it -> applyStyle ? styler(it) : it )
                    .withSize(80,50)
        when: 'We build the button'
            var button = ui.get(JButton)
        then:
            (ComponentExtension.from(button).getStyle() != StyleConf.none()) == hasEffect

        when : """
            We de-activate the style and check if the style was properly reset to being "none"!
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
        then :
            ComponentExtension.from(button).getStyle() == StyleConf.none()

        where : """
            We populate this test with various styles and "hasEffect" flags
            If the flag is `false`, then this means the style produced by the lambda 
            was simplified to being no-style.
        """
            hasEffect | styler
             false    | { it }
             true     | { it.backgroundColor(Color.BLACK) }
             true     | { it.foregroundColor(Color.BLUE) }
             true     | { it.foundationColor(Color.GREEN) }
             true     | { it.cursor(UI.Cursor.HAND) }
             false    | { it.margin(0) }
             true     | { it.margin(5) }
             true     | { it.padding(5).margin(5) }
             true     | { it.border(2, "black") }
             false    | { it.border(0, "black") }
             true     | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }
             true     | { it.shadowColor("green") }
             true     | { it.shadowColor("blue").shadowBlurRadius(5) }
             true     | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
             false    | { it.shadowColor("rgba(0,0,0,0)").shadowBlurRadius(2).shadowSpreadRadius(7) }
             true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
             true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
             true     | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
             true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
             true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
             true     | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
             false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("").spreadRadius(1).blurRadius(5)) }
             true     | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
             false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) }
             false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) }
             true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
             true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
             false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).blurRadius(5).isOutset(true)) }
             true     | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
             false    | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
             true     | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
             true     | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
             true     | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
             true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
             true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
             false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
             true     | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
             true     | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
             true     | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
             true     | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
             true     | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
             true     | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
             true     | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }
             true     | { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) }
             true     | { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) }
             true     | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
             true     | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) }
             true     | { it.parentFilter( conf -> conf.blur(1) ) }
             true     | { it.parentFilter( conf -> conf.blur(0.75) ) }
             false    | { it.parentFilter( conf -> conf.blur(0.0) ) }
             true     | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
             false    | { it.shadow(UI.Layer.BACKGROUND, "s", c->c.color("").blurRadius(5)).gradient(UI.Layer.BACKGROUND, "g", c->c.colors("", "")) }
             false    | { it.gradient(UI.Layer.CONTENT, "g1", c->c.colors("","")).gradient(UI.Layer.CONTENT, "g2", c->c.colors("","")).gradient(UI.Layer.CONTENT, "g3", c->c.colors("","")) }
             true     | { it.gradient(UI.Layer.CONTENT, "g1", c->c.colors("","")).gradient(UI.Layer.CONTENT, "g2", c->c.colors("blue","green")).gradient(UI.Layer.CONTENT, "g3", c->c.colors("white","red")) }
             false    | { it.border(0, "black").shadow(UI.Layer.CONTENT, "s1", c->c.color("").blurRadius(5)).shadow(UI.Layer.CONTENT, "s2", c->c.color("")) }
             true     | { it.border(0, "black").shadow(UI.Layer.CONTENT, "s2", c->c.color("")).shadow(UI.Layer.CONTENT, "s1", c->c.color("blue").blurRadius(5)).shadow(UI.Layer.CONTENT, "s2", c->c.color("")) }
    }

    def 'The style engine installs and then uninstalls a minimum size, restoring the natural minimum.'()
    {
        reportInfo """
            The SwingTree style engine may override a component's minimum size
            through the style API (e.g. `it.minHeight(120)`).

            Crucially, when the style stops specifying a minimum size again, the engine
            must **restore** the component to its natural minimum size. If it did not,
            then a stale minimum would stick around and prevent the component from ever
            shrinking again. This is exactly the kind of trouble that can arise with
            transitional or animated styles (like a fold animation that temporarily
            clamps the height of a panel), so the install/uninstall symmetry matters.
        """
        given: 'A simple panel whose style is only applied when a flag is set:'
            var applyStyle = false
            var panel =
                    UI.panel("fill")
                    .add(UI.label("Some content"))
                    .withSize(200, 100)
                    .withStyle( it -> applyStyle ? it.minHeight(120) : it )
                    .get(JPanel)
        and: 'We remember the natural minimum size, the one the component has before any styling:'
            var naturalMinimum = panel.getMinimumSize()
        expect: 'Initially the component does not have an explicit minimum size:'
            !panel.isMinimumSizeSet()

        when: 'We activate the style and let the component paint, which gathers and installs the style:'
            applyStyle = true
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The style engine has installed an explicit, larger minimum size:'
            panel.isMinimumSizeSet()
            panel.getMinimumSize().height > naturalMinimum.height

        when: 'We deactivate the style again and let the component paint:'
            applyStyle = false
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The component is restored to its natural minimum size, free to shrink again:'
            !panel.isMinimumSizeSet()
            panel.getMinimumSize() == naturalMinimum
    }

    def 'The minimum and maximum sizes are installed and then uninstalled together with the style.'(
        String kind, Styler<JPanel> styler, Closure<Boolean> isSet, Closure<Dimension> sizeOf
    ){
        reportInfo """
            The style API can override a component's `minimum` and `maximum` sizes.
            This data driven test verifies, for each of these two kinds of size, that
            the style engine:

            - installs the size when the style specifies it, and
            - uninstalls it again (restoring the natural size) when the style drops it.

            The '$kind' size is the one exercised in this iteration.

            (The *preferred* size deliberately behaves differently and is covered by a
            separate test, because it is only a hint and is also driven by the auto
            preferred height feature.)
        """
        given: 'A panel whose style is only applied when a flag is set:'
            var applyStyle = false
            var panel =
                    UI.panel("fill")
                    .add(UI.label("Content"))
                    .withSize(200, 100)
                    .withStyle( it -> applyStyle ? styler(it) : it )
                    .get(JPanel)
        and: 'We remember the natural size of this kind, before any styling:'
            var natural = sizeOf(panel)
        expect: 'No explicit size of this kind is set initially:'
            !isSet(panel)

        when: 'The style is activated and the component painted:'
            applyStyle = true
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The size of this kind is now explicitly set by the style engine:'
            isSet(panel)

        when: 'The style is removed again and the component painted:'
            applyStyle = false
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The component returns to its natural size of this kind:'
            !isSet(panel)
            sizeOf(panel) == natural

        where :
            kind        | styler                    | isSet                       | sizeOf
            'minimum'   | { it.minSize(150, 130) }  | { it.isMinimumSizeSet() }   | { it.getMinimumSize() }
            'maximum'   | { it.maxSize(150, 130) }  | { it.isMaximumSizeSet() }   | { it.getMaximumSize() }
    }

    def 'A preferred size set through the style API is intentionally NOT reset when the style drops it.'()
    {
        reportInfo """
            The preferred size is treated differently from the minimum and maximum sizes.

            Unlike a minimum size (which can *pin* a component and stop it from shrinking),
            the preferred size is only a hint to the layout manager. More importantly, the
            preferred height is also driven by the auto preferred height feature
            (see `TextConf#autoPreferredHeight`): SwingTree feeds the computed text height
            into the very same preferred-size channel of the style.

            The established behaviour, which applications rely on, is that switching such a
            preferred size off again leaves the last value in place rather than snapping the
            component back to its natural preferred size. This test pins that behaviour down,
            so that the minimum/maximum size restoration logic never accidentally starts
            resetting the preferred size as well.
        """
        given: 'A panel whose style only sometimes specifies a preferred size:'
            var applyStyle = false
            var panel =
                    UI.panel("fill")
                    .add(UI.label("Content"))
                    .withSize(200, 100)
                    .withStyle( it -> applyStyle ? it.prefSize(150, 130) : it )
                    .get(JPanel)
        expect: 'Initially there is no explicit preferred size:'
            !panel.isPreferredSizeSet()

        when: 'We activate the style and paint:'
            applyStyle = true
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The preferred size is now explicitly set by the style engine:'
            panel.isPreferredSizeSet()
        and : 'We remember the preferred size that was installed:'
            var installed = panel.getPreferredSize()

        when: 'We remove the style again and paint:'
            applyStyle = false
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The preferred size sticks at the last value, it is deliberately not reset:'
            panel.isPreferredSizeSet()
            panel.getPreferredSize() == installed
    }

    def 'A minimum size set on the component directly is preserved across style installation and uninstallation.'()
    {
        reportInfo """
            A user may set a minimum size on a component directly, outside of the style API.
            When the style engine temporarily overrides that minimum size and later removes
            its override again, it must restore the user's **original** minimum size, and not
            simply wipe it. In other words: the style engine only owns what it set itself.
        """
        given: 'A panel with a minimum size that was set on the component directly:'
            var applyStyle = false
            var panel =
                    UI.panel("fill")
                    .add(UI.label("Content"))
                    .withSize(200, 100)
                    .withStyle( it -> applyStyle ? it.minHeight(300) : it )
                    .peek( c -> c.setMinimumSize(new Dimension(42, 84)) )
                    .get(JPanel)
        expect: 'The directly defined minimum size is in place:'
            panel.isMinimumSizeSet()
            panel.getMinimumSize() == new Dimension(42, 84)

        when: 'The style overrides the minimum height and the component paints:'
            applyStyle = true
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The styled minimum height takes effect:'
            panel.getMinimumSize().height > 84

        when: 'The style is removed again and the component paints:'
            applyStyle = false
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: "The component's own minimum size is faithfully restored:"
            panel.isMinimumSizeSet()
            panel.getMinimumSize() == new Dimension(42, 84)
    }

    def 'A temporarily clamped minimum height does not permanently pin a component. (fold animation regression)'()
    {
        reportInfo """
            This is a regression test for a subtle but nasty bug.

            Transitional and animated styles, like the fold animation used by collapsible
            panels, temporarily clamp a component's height by setting `minHeight` and
            `maxHeight` on every animation frame. When such an animation completes and the
            style returns to its natural, unclamped form, the style engine **must** release
            the clamped minimum and maximum sizes again.

            Previously it did not: the stale minimum size stuck to the component, so a panel
            that had been folded open could no longer shrink to fit its content (for example
            after hiding some of its rows). A layout manager never sizes a component below its
            minimum, so the panel stayed stubbornly tall. Notably, no amount of `revalidate()`
            could fix that, because `revalidate()` does not touch the minimum size.

            Here we simulate the tail end of such an animation: a clamping style is applied
            and then removed. We verify that the component is no longer pinned afterwards.
        """
        given: 'A content panel which a "fold" style clamps to a fixed height while active:'
            var folding = true
            var panel =
                    UI.panel("fill")
                    .add(UI.label("Row A"))
                    .add(UI.label("Row B"))
                    .withSize(200, 100)
                    .withStyle( it -> folding ? it.minHeight(50).maxHeight(50) : it )
                    .get(JPanel)

        when: 'The clamping (fold) style is active and the panel paints:'
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The panel is clamped: both a minimum and a maximum height are pinned:'
            panel.isMinimumSizeSet()
            panel.isMaximumSizeSet()

        when: 'The animation completes, so the clamping style is gone, and the panel paints again:'
            folding = false
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The clamp is fully released, so the panel is free to size to its content again:'
            !panel.isMinimumSizeSet()
            !panel.isMaximumSizeSet()
    }

    def 'A minimum size is uninstalled even when other, non-size, style properties remain active.'(
        String remaining, Styler<JPanel> otherStyle
    ){
        reportInfo """
            This is the important "messy" case.

            A component very often keeps *some* styling while only its size clamp comes
            and goes. The fold container in a real application, for example, keeps a
            (transparent) background while the fold animation clamps and unclamps its height.
            This means the component never becomes fully "un-styled" in between, so the
            release of the size must happen on the still-styled code path, not only on the
            "no style at all" path.

            Crucially, this must hold no matter *what* the remaining styling is. Different
            kinds of style take different installation routes through the engine: a shadow or
            background gradient may install a custom UI, a border installs a custom border,
            and so on. So we exercise this with a variety of leftover styles. Here the
            remaining style is a **$remaining**.

            If this ever regresses, a panel that still has e.g. a shadow or a border would
            stay stuck at its clamped minimum size and refuse to shrink to fit its content.
        """
        given: 'A panel that is always styled (with some non-size style) but only sometimes clamped:'
            var clamp = false
            var panel =
                    UI.panel("fill")
                    .add(UI.label("Content"))
                    .withSize(200, 100)
                    .withStyle( it -> clamp ? otherStyle(it).minHeight(140) : otherStyle(it) )
                    .get(JPanel)
        and: 'We remember the natural minimum size:'
            var naturalMinimum = panel.getMinimumSize()
        and : 'The leftover style on its own is a real (non-empty) style:'
            !clamp
            ComponentExtension.from(panel).getStyle() != StyleConf.none()

        when: 'We apply the clamp and paint:'
            clamp = true
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The minimum height is pinned:'
            panel.isMinimumSizeSet()
            panel.getMinimumSize().height > naturalMinimum.height

        when: 'We drop only the clamp, so the other styling stays, and paint:'
            clamp = false
            panel.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The minimum size is restored to natural, even though the component is still styled:'
            !panel.isMinimumSizeSet()
            panel.getMinimumSize() == naturalMinimum
        and: 'The component is indeed still styled (it was never fully reset to "no style"):'
            ComponentExtension.from(panel).getStyle() != StyleConf.none()

        where : 'The leftover, non-size styling takes various installation routes through the engine:'
            remaining            | otherStyle
            'background colour'  | { it.backgroundColor(Color.LIGHT_GRAY) }
            'foundation colour'  | { it.foundationColor(Color.GREEN) }
            'background shadow'  | { it.shadow(UI.Layer.BACKGROUND, "s", c->c.color("black").blurRadius(5).spreadRadius(3)) }
            'foreground shadow'  | { it.shadow(UI.Layer.FOREGROUND, "s", c->c.color("blue").offset(2,2).blurRadius(4)) }
            'a line border'      | { it.border(2, Color.BLACK) }
            'a rounded border'   | { it.borderRadius(12).border(1, Color.DARK_GRAY) }
            'a background gradient' | { it.gradient(UI.Layer.BACKGROUND, "g", c->c.colors(Color.RED, Color.BLUE)) }
            'a border gradient'  | { it.gradient(UI.Layer.BORDER, "g", c->c.colors(Color.RED, Color.BLUE)) }
            'a margin'           | { it.margin(7) }
            'shadow and border'  | { it.border(2, Color.BLACK).shadow(UI.Layer.BACKGROUND, "s", c->c.color("black").blurRadius(6)) }
    }

}
