package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.Painter
import swingtree.api.Styler
import swingtree.api.laf.SwingTreeStyledComponentUI
import swingtree.layout.Size
import swingtree.style.ComponentBackend
import swingtree.style.ComponentStyleDelegate
import swingtree.style.StyleConf

import sprouts.Var
import swingtree.threading.EventProcessor
import utility.ConstructionCountingPanel
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.*
import javax.swing.plaf.basic.BasicButtonUI
import javax.swing.plaf.basic.BasicPanelUI
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
    def setupSpec() {
        SwingTree.initializeUsing(SwingTreeTestConfigurator.get())
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
    }

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
                ComponentBackend.powering(component).paintBackground(g2d, g2d2->super.paint(g2d2, component));
            else
                super.paint(g2d, component);
        }
    }


    static class ForwardingPanelUI extends BasicPanelUI implements SwingTreeStyledComponentUI<JPanel> {
        private final Painter lookAndFeelPainting

        ForwardingPanelUI(Painter lookAndFeelPainting) {
            this.lookAndFeelPainting = lookAndFeelPainting
        }

        @Override
        ComponentStyleDelegate<JPanel> style(ComponentStyleDelegate<JPanel> delegate) throws Exception {
            return delegate
        }

        @Override
        boolean canForwardPaintingToSwingTree() {
            return true
        }

        @Override
        void paint(Graphics g, JComponent component) {
            ComponentBackend.powering(component).paintBackground(g, lookAndFeelPainting)
        }

        @Override
        void update(Graphics g, JComponent component) {
            paint(g, component)
        }
    }

    def 'Styles may lead to the installation of a custom UI depending on how well a particular LaF supports SwingTree.'(
        boolean isStyled, boolean overridden, Styler<JButton> styler
    ){
        reportInfo """
            This is a data driven test verifying how a partially `SwingTree` compatible
            component UI can be integrated with `SwingTree`s style engine on a regular component.
            It takes a `Styler` which will be applied to a `JButton` 
            by passing it to the `withStyle(Styler)` method.
            Then we build the component and check if the custom UI was
            overridden or not. An override can take place if a look and
            feel is not fully compatible with `SwingTree`
            
            If you develop your own look and feel, then you can
            make it compatible with `SwingTree` by having your `ComponentUI`
            extensions implement the `SwingTreeStyledComponentUI` interface.
            The `SwingTree` library will detect the UI and ask the interface
            if it can cooperate with the `SwingTree` style backend.
            This cooperation has two parts:
            
            1. Supplying style information for the `SwingTree` style engine.
            2. Delegating the `paint` call to `SwingTree`s `ComponentBackend`. 
            
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
            (ComponentBackend.powering(button).getStyle() != StyleConf.none()) == isStyled
        and : 'The custom `MyButtonUI` may or may not be overridden by `SwingTree`:'
            !(button.getUI() instanceof MyButtonUI) == overridden

        when : """
            The style is deactivated, then we expect the original UI to be reinstalled.
            We test this by deactivating the style and then simulating a repaint of the button.
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
        then : 'The style must be reset to being `StyleConf.none()`:'
        ComponentBackend.powering(button).gatherStyle() == StyleConf.none()
        and : 'The original custom UI should be installed because the component is no longer styled'
            (button.getUI() instanceof MyButtonUI)

        where :
            isStyled | overridden | styler
            false    | false      | { it }
            true     | false      | { it.backgroundColor(Color.BLACK) }
            true     | false      | { it.foregroundColor(Color.BLUE) }
            true     | false      | { it.foundationColor(Color.GREEN) }
            true     | false      | { it.cursor(UI.Cursor.HAND) }
            true     | false      | { it.margin(5) }
            true     | false      | { it.padding(5).margin(5) }
            true     | false      | { it.border(2, "black") }
            true     | false      | { it.margin(5).border(3, "red").cursor(UI.Cursor.CROSS) }

            true     | false      | { it.shadowColor("green") }
            true     | false      | { it.shadowColor("blue").shadowBlurRadius(5) }
            true     | false      | { it.shadowColor("pink").shadowBlurRadius(2).shadowSpreadRadius(7) }
            true     | false      | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | false      | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | false      | { it.shadow(UI.Layer.CONTENT, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | false      | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | false      | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            true     | false      | { it.shadow(UI.Layer.BORDER, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            true     | false      | { it.shadow(UI.Layer.FOREGROUND, "myShadow", conf->conf.color("red").spreadRadius(1).blurRadius(5)) }
            false    | false      | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).offset(1,2).blurRadius(5)) }
            false    | false      | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).isOutset(true)) }
            true     | true       | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("black").offset(1,2).blurRadius(5)) }
            true     | true       | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color("red").spreadRadius(7).isOutset(true)) }
            false    | false      | { it.shadow(UI.Layer.BACKGROUND, "myShadow", conf->conf.color(UI.Color.UNDEFINED).spreadRadius(7).blurRadius(5).isOutset(true)) }

            true     | true       | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            false    | false      | { it.gradient(UI.Layer.BACKGROUND, "myGradient", conf->conf.colors([] as Color[])) }
            true     | false      | { it.gradient(UI.Layer.FOREGROUND, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | false      | { it.gradient(UI.Layer.CONTENT, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | false      | { it.gradient(UI.Layer.BORDER, "myGradient", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | true       | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.scale(1,2).colors(Color.RED, Color.BLUE)) }
            true     | true       | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors(Color.GREEN, Color.RED)) }
            false    | false      | { it.noise(UI.Layer.BACKGROUND, "myNoise", conf->conf.colors([] as Color[])) }
            true     | false      | { it.noise(UI.Layer.FOREGROUND, "myNoise", conf->conf.rotation(102).colors(Color.RED, Color.BLUE)) }
            true     | false      | { it.noise(UI.Layer.CONTENT, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }
            true     | false      | { it.noise(UI.Layer.BORDER, "myNoise", conf->conf.colors(Color.RED, Color.BLUE)) }

            true     | true       | { it.painter(UI.Layer.BACKGROUND, "myPainter", g2d -> {}) }
            true     | false      | { it.painter(UI.Layer.FOREGROUND, "myPainter", g2d -> {}) }
            true     | false      | { it.painter(UI.Layer.CONTENT, "myPainter", g2d -> {}) }
            true     | false      | { it.painter(UI.Layer.BORDER, "myPainter", g2d -> {}) }

            true     | true       | { it.painter(UI.Layer.BACKGROUND, UI.ComponentArea.EXTERIOR, "myPainter", g2d -> {}) }
            true     | false      | { it.painter(UI.Layer.FOREGROUND, UI.ComponentArea.INTERIOR, "myPainter", g2d -> {}) }
            true     | false      | { it.painter(UI.Layer.CONTENT, UI.ComponentArea.BORDER, "myPainter", g2d -> {}) }
            true     | false      | { it.painter(UI.Layer.BORDER, UI.ComponentArea.BODY, "myPainter", g2d -> {}) }

            true     | false      | { it.parentFilter( conf -> conf.blur(1) ) }
            true     | false      | { it.parentFilter( conf -> conf.blur(0.75) ) }
            false    | false      | { it.parentFilter( conf -> conf.blur(0.0) ) }
            true     | false      | { it.parentFilter( conf -> conf.kernel(Size.of(2, 1), 1,0) ) }
    }

    def 'The `SwingTree` style engine will never replace a `SwingTree` compatible `ComponentUI`.'(
        Styler<JButton> styler
    ){
        reportInfo """
            If you develop your own look and feel, then you can
            make it compatible with `SwingTree` by having your `ComponentUI`
            extensions implement the `SwingTreeStyledComponentUI` interface.
            The `SwingTree` library will detect the UI and ask the interface
            if it can cooperate with the `SwingTree` style backend.
            This cooperation has two parts:
            
            1. Supplying style information for the `SwingTree` style engine.
            2. Delegating the `paint` call to `SwingTree`s `ComponentBackend`. 
            
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

    def 'A `ComponentUI` with nothing of its own to paint passes `Painter.none()` and paints the same pixels.'(
        Styler<JPanel> styler
    ){
        reportInfo """
            A `SwingTreeStyledComponentUI` forwards its `paint` call to
            `ComponentBackend.paintBackground(Graphics, Painter)`. The `Painter` it passes is the
            painting it inherits from its Swing look and feel. SwingTree paints the style first,
            then sets the component's rounded outline as the clip of the `Graphics` and runs that
            painter inside it, so that nothing the look and feel draws spills over the round
            corners.
            
            Many inherited delegates draw nothing at all: `BasicPanelUI`, `BasicViewportUI` and
            `BasicToolBarUI` do not even override `paint`. For such a `ComponentUI` the right
            painter to pass is `Painter.none()`, and a panel painted that way must look exactly
            like a panel whose painter does nothing.
            
            You may wonder why a scenario about pixels talks so much about clips. The reason is
            a small optimization inside SwingTree. When it is handed `Painter.none()`, it paints
            the style and then simply stops, without setting up the clip at all. It does this
            only for speed. A clip in the shape of a rounded outline is not free, because Java2D
            turns that shape into a clip region one row of pixels at a time. A card 1,200 pixels
            tall would pay for 1,200 rows on every repaint, all of it to run a painter that
            draws nothing.
            
            A shortcut like that is easy to get subtly wrong. If it stopped a little too early,
            it would skip the style as well, and the panel would be painted without its
            background colour, its gradient or its shadow. So we render the same style twice,
            once through a painter that does nothing and once through `Painter.none()`, and we
            expect the two images to agree down to the last channel of the last pixel.
            
            To do that, we need a way to hand SwingTree a painter of our own choosing. The
            shortcut sits inside `ComponentBackend.paintBackground(Graphics, Painter)`, and
            SwingTree never calls that method by itself. A look and feel calls it, from the
            `paint` method of its `ComponentUI`. A plain `JPanel` comes with a plain
            `BasicPanelUI`, whose `paint` method is empty, so painting a plain panel would never
            reach the shortcut.
            
            In this scenario we are therefore using a simple custom `ComponentUI` extension
            called `ForwardingPanelUI`, in order to play the part of a look and feel and pass
            SwingTree exactly the painter we want to test. It extends `BasicPanelUI` and
            implements `SwingTreeStyledComponentUI`, which is how SwingTree recognises a
            `ComponentUI` that cooperates with its styles. Its `style(..)` method hands the style
            back unchanged, so everything we see in the images comes from the panel's own
            `withStyle(..)`. Its `canForwardPaintingToSwingTree()` method answers `true`, which
            tells SwingTree that the `paint` method will call `paintBackground` itself; a
            `ComponentUI` that answered `false` could be replaced by one of SwingTree's own once
            the panel is styled, and then our painter would never run. Its `paint` method does
            nothing but call `ComponentBackend.powering(panel).paintBackground(g, painter)` with
            the painter it was constructed with, which lets us build one panel around a painter
            that does nothing and another around `Painter.none()`. Finally, its `update` method
            calls `paint` directly, because the `update` inherited from `ComponentUI` would first
            fill an opaque panel with its background colour, and those pixels would not come
            from SwingTree.
            
            The painter that "does nothing" is not entirely idle, though. It draws nothing, but
            it writes down the clip of the `Graphics` it is handed. That is the one clip we look
            at, and it turns out not to be a rectangle: a painter which does run really is placed
            inside the rounded outline. That is exactly the work the shortcut saves. On the
            `Painter.none()` side there is nothing comparable to inspect, since no painter runs
            there and the clip is taken off the `Graphics` again before `paintBackground`
            returns. The painted panel is what a user sees, and so the pixels are what we
            compare.
        """
        given : 'Two panels under the same style, one handing SwingTree a painter which does nothing...'
            var clipsSeenByThePainter = []
            var withEmptyPainter = new JPanel()
            withEmptyPainter.setUI(new ForwardingPanelUI({ Graphics2D g2d -> clipsSeenByThePainter << g2d.getClip() } as Painter))
            withEmptyPainter = UI.of(withEmptyPainter).withStyle(styler).get(JPanel)
            withEmptyPainter.setSize(90, 60)
        and : '...and the other handing it `Painter.none()`.'
            var withNoPainter = new JPanel()
            withNoPainter.setUI(new ForwardingPanelUI(Painter.none()))
            withNoPainter = UI.of(withNoPainter).withStyle(styler).get(JPanel)
            withNoPainter.setSize(90, 60)

        when : 'We render both of them.'
            var paintedWithEmptyPainter = Utility.renderSingleComponent(withEmptyPainter)
            var paintedWithNoPainter    = Utility.renderSingleComponent(withNoPainter)
        then : 'The painter that does nothing was still run, inside a clip of the rounded body.'
            clipsSeenByThePainter.size() == 1
            !(clipsSeenByThePainter[0] instanceof Rectangle)
        and : 'Both panels are painted identically, down to the last channel of the last pixel.'
            Utility.worstChannelDelta(paintedWithEmptyPainter, paintedWithNoPainter) == 0

        where :
            styler << [
                { it.backgroundColor(Color.CYAN).borderRadius(18) },
                { it.borderRadius(12).border(3, Color.BLUE).backgroundColor(Color.ORANGE) },
                { it.margin(4).borderRadius(20).gradient( g -> g.colors(Color.RED, Color.BLUE) ) },
                { it.borderRadiusAt(UI.Corner.TOP_LEFT, 16).backgroundColor(Color.MAGENTA).padding(6) },
                { it.borderRadius(25).shadowColor(Color.BLACK).shadowBlurRadius(4).backgroundColor(Color.WHITE) }
            ]
    }

    def 'Styling a component does not run the constructor of the application class it belongs to.'()
    {
        reportInfo """
            Before anything else styles a component, the style engine has to know what
            background the component would have had without it, so that it can put that
            background back when no style asks to paint one. It used to find out by
            constructing a second instance of the component's *own* class.
            
            For a library type that is harmless. For an application's own view class it runs
            that view's constructor, with every side effect the constructor has. One such
            constructor calls `FlatLightLaf.setup()`, and styling a view that contained it
            silently replaced the look and feel which had just been installed - so a whole
            application was drawn by the wrong delegates, with nothing logged.
            
            Styling reads state. It must not create application objects to do it.
        """
        given : 'A panel class of our own, which counts how often it has been constructed.'
            ConstructionCountingPanel.CONSTRUCTIONS.set(0)
        and : '''
            A style whose background SwingTree has to paint itself. This is what puts the
            component's background into the undefined state the default lookup answers from:
            a gradient cannot be expressed as an AWT background colour, so the colour is
            replaced by a sentinel until something needs a real one back.
        '''
            var paintsItsOwnBackground = Var.of(true)
            var panel =
                    UI.of(new ConstructionCountingPanel())
                    .withStyle( it -> paintsItsOwnBackground.get()
                        ? it.gradient( g -> g.colors(Color.RED, Color.BLUE) )
                        : it
                    )
                    .get(ConstructionCountingPanel)
            panel.setSize(60, 40)

        when : 'We paint it, which is what makes the style engine apply the style.'
            Utility.renderSingleComponent(panel)
        then : 'The panel has been constructed exactly once - by this test.'
            ConstructionCountingPanel.CONSTRUCTIONS.get() == 1
        and : 'Its background is the sentinel, so the next paint has to look a real one up.'
            panel.getBackground() === UI.Color.UNDEFINED

        when : '''
            The gradient goes away and we paint again. The engine now has to restore a real
            background colour, which is the lookup this scenario is about.
        '''
            paintsItsOwnBackground.set(false)
            Utility.renderSingleComponent(panel)
        then : 'It is still exactly one construction: the style engine created nothing.'
            ConstructionCountingPanel.CONSTRUCTIONS.get() == 1
    }

}
