package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.Styler
import swingtree.layout.Size

import javax.swing.JButton
import javax.swing.plaf.metal.MetalButtonUI
import java.awt.Color
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
            button.getUI() instanceof MetalButtonUI

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
            false    | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
            true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
            false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
            false    | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            false    | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }
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
            SwingTree to evaluate if it is necessary to overrde the look and feel.
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
}
