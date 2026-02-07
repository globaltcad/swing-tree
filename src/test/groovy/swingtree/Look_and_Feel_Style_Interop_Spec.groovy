package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.Styler
import swingtree.api.laf.SwingTreeStyledComponentUI
import swingtree.layout.Size
import swingtree.style.ComponentExtension
import swingtree.style.ComponentStyleDelegate
import swingtree.style.StyleConf

import javax.swing.*
import javax.swing.plaf.basic.BasicButtonUI
import java.awt.*
import java.awt.image.BufferedImage

@Title("Look and Feel Interoperability")
@Narrative('''

    SwingTree ships with a rich style rendering engine and
    various API endpoints for configuring styles for components.
    This style engine and any style configuration you feed it, is similar
    to CSS, in that is so versatile that it satisfies any UX requirements.
    
    In this specification we demonstrate the main ways how the
    style of a component is computed, and more specifically, how the
    look and feel of a component can also plug into the SwingTree style engine.

''')
@Subject([SwingTreeStyledComponentUI])
class Look_and_Feel_Style_Interop_Spec extends Specification
{
    static class MyButtonUI extends BasicButtonUI implements SwingTreeStyledComponentUI<AbstractButton> {
        private final boolean supportsSwingTree;
        private final Styler<AbstractButton> styler;

        public MyButtonUI(boolean supportsSwingTree, Styler<AbstractButton> styler) {
            this.supportsSwingTree = supportsSwingTree;
            this.styler = styler;
        }

        @Override
        public ComponentStyleDelegate<AbstractButton> style(ComponentStyleDelegate<AbstractButton> delegate) throws Exception {
            return styler.style(delegate);
        }

        @Override
        public boolean canForwardPaintingToSwingTree() {
            return supportsSwingTree;
        }

        @Override
        public void paint(Graphics g2d, JComponent component) {
            if ( supportsSwingTree )
                ComponentExtension.from(component).paintBackground(g2d, g2d2->super.paint(g2d2, component));
            else
                super.paint(g2d, component);
        }
    }


    def 'Styles may lead to the installation of a custom UI depending on how well a particular LaF supports SwingTree.'(
        boolean isStyled, boolean isCustom, Styler<JButton> styler
    ){
        reportInfo """
            This is a data driven test verifying how a partially `SwingTree` compatible
            component UI can be integrates with `SwingTree`s style engine on a regular component.
            It takes a `Styler` which will be applied to a `JButton` 
            by passing it to the `withStyle(Styler)` method.
            Then we build the component and check if the custom UI was
            overridden or not. An override can take place if a look and
            feel is not fully compatible with `SwingTree`
            
            If your develop your own look and feel, then you can
            make it compatible with `SwingTree` by having your `ComponentUI`
            extensions implement the `SwingTreeStyledComponentUI` interface.
            The `SwingTree` library will detect the UI and ask the interface
            if it can cooperate with the `SwingTree` style backend.
            This corporation has two parts:
            
            1. Supplying style information for the `SwingTree` style engine.
            2. Delegating the `paint` call to `SwingTree`s `ComponentExtension`. 
            
            In this unit test, the `ComponentUI` under test only supports 1.
            but not 2...
            
            This specification may not be relevant to you if you are not interested
            in the details of the SwingTree library internals.
            But it demonstrates the complexity of the style installation process
            and should give you a good idea of what it took to build the SwingTree library.
        """
        given : 'A button with a custom component UI (look and feel).'
            boolean isFullyCompatible = false
            var applyStyle = true
            var buttonUI = new MyButtonUI(isFullyCompatible, {it -> applyStyle ? styler(it) : it});
            var button = new JButton()
            button.setUI(buttonUI)
        and : 'We create a button UI with the given styler'
            var ui =
                    UI.of(button)
                    .withSize(80,50)
        when : 'We build the button'
            button = ui.get(JButton)
        then : 'Depending on the applied style, there may or may not be a `StyleConf` installed:'
            (ComponentExtension.from(button).getStyle() != StyleConf.none()) == isStyled
        and : 'The custom UI also may or may not be installed:'
            !(button.getUI() instanceof MyButtonUI) == isCustom

        when : """
            The style is deactivated, then we expect the original UI to be reinstalled.
            We test this by deactivating the style and then simulating a repaint of the button.
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
        then : 'The style must be reset to being `StyleConf.none()`:'
            ComponentExtension.from(button).gatherStyle() == StyleConf.none()
        and : 'The original custom UI should be installed because the component is no longer styled'
            (button.getUI() instanceof MyButtonUI)

        where :
            isStyled | isCustom | styler
            false    | false    | { it }
            true     | false    | { it.backgroundColor(Color.BLACK) }
            true     | false    | { it.foregroundColor(Color.BLUE) }
            true     | false    | { it.foundationColor(Color.GREEN) }
            true     | false    | { it.cursor(UI.Cursor.HAND) }
            true     | false    | { it.margin(5) }
            true     | false    | { it.padding(5).margin(5) }
            true     | false    | { it.border(2, "black") }
            true     | false    | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }

            true     | false    | { it.shadowColor("green") }
            true     | false    | { it.shadowColor("blue").shadowBlurRadius(5) }
            true     | false    | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
            true     | false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | false    | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | false    | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | false    | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            false    | false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) }
            false    | false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) }
            true     | true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | true     | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | false    | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).blurRadius(5).isOutset(true)) }

            true     | true     | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | false    | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
            true     | false    | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | false    | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | false    | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
            true     | true     | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
            false    | false    | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
            true     | false    | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
            true     | false    | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | false    | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | true     | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            true     | false    | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            true     | false    | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            true     | false    | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }

            true     | true     | { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) }
            true     | false    | { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) }
            true     | false    | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
            true     | false    | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) }

            true     | false    | { it.parentFilter( conf -> conf.blur(1) ) }
            true     | false    | { it.parentFilter( conf -> conf.blur(0.75) ) }
            false    | false    | { it.parentFilter( conf -> conf.blur(0.0) ) }
            true     | false    | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
    }

    def 'The `SwingTree` style engine will never replace a `SwingTree` compatible `ComponentUI`.'(
        Styler<JButton> styler
    ){
        reportInfo """
            If your develop your own look and feel, then you can
            make it compatible with `SwingTree` by having your `ComponentUI`
            extensions implement the `SwingTreeStyledComponentUI` interface.
            The `SwingTree` library will detect the UI and ask the interface
            if it can cooperate with the `SwingTree` style backend.
            This corporation has two parts:
            
            1. Supplying style information for the `SwingTree` style engine.
            2. Delegating the `paint` call to `SwingTree`s `ComponentExtension`. 
            
            In this unit test, the `ComponentUI` under test supports 
            both 1. as well as 2. and so in this unit test we verify that `SwingTree` will never override
            such a `ComponentUI` which is fully compatibly and fully cooperative with `SwingTree` in that sense.
            
            If a particular Look and Feel is not fully compatible, `SwingTree` may decide to
            override a particular `ComponentUI` in order to ensure that the style is rendered correctly.
            But a compatible Look and Feel like in this test will make this mechanism obsolete.
        """
        given : 'A button with a custom component UI (look and feel).'
            boolean isFullyCompatible = true
            var applyStyle = true
            var buttonUI = new MyButtonUI(isFullyCompatible, { it -> applyStyle ? styler(it) : it });
            var button = new JButton()
            button.setUI(buttonUI)
        and : 'We create a button UI with the given styler'
            var ui =
                    UI.of(button)
                    .withSize(80,50)
                    .withStyle( it -> applyStyle ? styler(it) : it )
        when: 'We build the button and obtain it from the builder...'
            button = ui.get(JButton)
        then: 'The `MyButtonUI` was not overridden, it is still there:'
            (button.getUI() instanceof MyButtonUI)

        when : """
            The style is deactivated, then also expect the custom `MyButtonUI` to remain the same.
            We test this by deactivating the style and then simulating a repaint of the button.
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
        then : 'The exact same `MyButtonUI` remains:'
            (button.getUI() instanceof MyButtonUI)

        where :
            styler << [
                 { it },
                 { it.backgroundColor(Color.BLACK) },
                 { it.foregroundColor(Color.BLUE) },
                 { it.foundationColor(Color.GREEN) },
                 { it.cursor(UI.Cursor.HAND) },
                 { it.margin(5) },
                 { it.padding(5).margin(5) },
                 { it.border(2, "black") },
                 { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) },
                 { it.shadowColor("green") },
                 { it.shadowColor("blue").shadowBlurRadius(5) },
                 { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) },
                 { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) },
                 { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) },
                 { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) },
                 { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) },
                 { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) },
                 { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) },
                 { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) },
                 { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) },
                 { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) },
                 { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) },
                 { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) },
                 { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) },
                 { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) },
                 { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) },
                 { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) },
                 { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) },
                 { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) },
                 { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) },
                 { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) },
                 { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) },
                 { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) },
                 { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) },
                 { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) },
                 { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) },
                 { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) },
                 { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) },
                 { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) },
                 { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) },
                 { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) },
                 { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) },
                 { it.parentFilter( conf -> conf.blur(1) ) },
                 { it.parentFilter( conf -> conf.blur(0.75) ) },
                 { it.parentFilter( conf -> conf.blur(0.0) ) },
                 { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
            ]
    }

}
