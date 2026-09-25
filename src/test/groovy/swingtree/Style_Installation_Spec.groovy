package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.api.Styler
import swingtree.layout.Size
import swingtree.style.ComponentBackend
import utility.Utility
import swingtree.style.StyleConf
import swingtree.style.StyleSheet

import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.plaf.metal.MetalButtonUI
import javax.swing.plaf.metal.MetalTextFieldUI
import javax.swing.tree.DefaultTreeCellEditor
import javax.swing.tree.DefaultTreeCellRenderer
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

    def 'A component whose border was replaced by somebody else wears the style border again after the next repaint.'()
    {
        reportInfo """
            A style is not only painted, it is also **measured**. SwingTree keeps the margin, the
            padding and the border widths of a style in a `Border` which it installs on the
            component, so that the text of a text field is laid out inside them and a layout manager
            asking the component for its size is told about them.
            
            Swing replaces borders behind your back. The editor a `JTable` uses for the cells of an
            ordinary column is a `JTextField`, and `JTable.GenericEditor` calls
            `setBorder(new LineBorder(Color.black))` on it every single time the editing of a cell
            begins. If the style engine let that stand, the two halves of the component's appearance
            would disagree with each other: the text would be laid out one pixel from the edge,
            inside the foreign border, while the style is still painted and clipped where the style
            says it goes. The first letter of the text is then cut in half by that clip, which is
            precisely the bug this scenario was written for.
            
            So a border which is no longer the one the style engine installed is a reason to install
            the style again, even when nothing about the style itself has changed since the last
            repaint. The repair happens on the next repaint, which is the moment the disagreement
            would otherwise have become visible.
            
            The component below is a panel rather than the cell editor of the story, because the
            requirement is about any component whose style both measures and paints it, and a panel
            with a fill is the smallest of those.
        """
        given : 'A scale factor of one, so that every number in this scenario is a plain pixel.'
            var formerScale = SwingTree.get().getUiScaleFactor()
            SwingTree.get().setUiScaleFactor(1)
        and : 'A component which is both measured and painted by its style, which a panel with a fill is.'
            var panel =
                    UI.panel()
                    .withSize(120, 30)
                    .withStyle( it -> it
                        .margin(6).padding(3).border(1, Color.BLACK).backgroundColor(Color.WHITE)
                    )
                    .get(JPanel)

        expect : 'The style engine has installed a border of its own, ten pixels deep on every side.'
            panel.getBorder() instanceof swingtree.style.StyleAndAnimationBorder
            panel.getInsets() == new Insets(10, 10, 10, 10)

        when : 'Somebody else - a cell editor, say - gives the component a border of its own.'
            panel.setBorder(new LineBorder(Color.BLACK))
        then : 'The component is measured by that border now, and it knows nothing about the style.'
            panel.getInsets() == new Insets(1, 1, 1, 1)

        when : 'The component is repainted, which is when the style engine looks at it again.'
            Utility.renderSingleComponent(panel)
        then : 'The style border is back, and the component is measured by the style once more.'
            panel.getBorder() instanceof swingtree.style.StyleAndAnimationBorder
            panel.getInsets() == new Insets(10, 10, 10, 10)

        cleanup:
            SwingTree.get().setUiScaleFactor(formerScale)
    }

    def 'Styling the text field Swing edits a tree cell with does not send the style engine round in circles.'()
    {
        reportInfo """
            `JComponent.setBorder(Border)` asks the new border for its insets before it returns, so
            that it can tell whether the component has to be laid out again. A SwingTree border
            answers that question by gathering and installing the style, because the insets are part
            of the style and it may be the first time anybody asked.
            
            Usually that is harmless: the component reports the new border the moment it is given
            one, so the style engine sees its own border and leaves it there. Swing has one class
            for which this is not true. `DefaultTreeCellEditor.DefaultTextField`, the text field a
            tree edits its cells with, keeps the border in a field of its own which it assigns
            *after* `super.setBorder(..)` has returned:
            
            ```java
                public void setBorder(Border border) {
                    super.setBorder(border);
                    this.border = border;      // ... only now does getBorder() agree
                }
            ```
            
            So while the style engine installs its border, that component still reports the border it
            had before, the engine concludes that it has no border of its own yet, and installs one
            from inside the installation it is already in. Left alone, that recursion ends a tree
            cell edit in a `StackOverflowError` instead of an editor.
            
            An installation which is already under way therefore never starts a second one. The
            insets of such a component are worked out on the repaint that follows instead, which is
            what the last step of this scenario checks.
        """
        given : 'A scale factor of one, so that every number in this scenario is a plain pixel.'
            var formerScale = SwingTree.get().getUiScaleFactor()
            SwingTree.get().setUiScaleFactor(1)
        and : 'Swing\'s own tree cell editor, asked for the component it edits a cell with.'
            var tree = new JTree()
            var cellEditor = new DefaultTreeCellEditor(tree, new DefaultTreeCellRenderer())
            var editorBox = cellEditor.getTreeCellEditorComponent(tree, "Loom A", true, false, true, 0)
            var field = editorBox.getComponent(0)

        expect : 'It is the text field whose `getBorder()` lags behind by one call.'
            field instanceof DefaultTreeCellEditor.DefaultTextField

        when : 'We style it the way a look and feel styles every text field of an application.'
            var styled = UI.of((JComponent) field)
                           .withStyle( it -> it.margin(4).padding(2).border(1, Color.BLACK) )
                           .get(JTextField)
        then : 'The style engine installed its border once, instead of calling itself until the stack is full.'
            styled.getBorder() instanceof swingtree.style.StyleAndAnimationBorder

        when : 'The editor is painted, the way it would be after Swing added it to the tree.'
            Utility.renderSingleComponent(styled)
        then : 'It is measured by the style: four pixels of margin, two of padding and the border.'
            styled.getInsets() == new Insets(7, 7, 7, 7)

        cleanup:
            SwingTree.get().setUiScaleFactor(formerScale)
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
            var buttonStyle = ComponentBackend.powering(button).getStyle()
            var labelStyle = ComponentBackend.powering(label).getStyle()

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
            (ComponentBackend.powering(button).getStyle() != StyleConf.none()) == hasEffect

        when : """
            We de-activate the style and check if the style was properly reset to being "none"!
        """
            applyStyle = false
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            button.paint(image.createGraphics())
        then :
            ComponentBackend.powering(button).getStyle() == StyleConf.none()

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
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The style engine has installed an explicit, larger minimum size:'
            panel.isMinimumSizeSet()
            panel.getMinimumSize().height > naturalMinimum.height

        when: 'We deactivate the style again and let the component paint:'
            applyStyle = false
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
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
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The size of this kind is now explicitly set by the style engine:'
            isSet(panel)

        when: 'The style is removed again and the component painted:'
            applyStyle = false
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
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
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The preferred size is now explicitly set by the style engine:'
            panel.isPreferredSizeSet()
        and : 'We remember the preferred size that was installed:'
            var installed = panel.getPreferredSize()

        when: 'We remove the style again and paint:'
            applyStyle = false
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
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
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The styled minimum height takes effect:'
            panel.getMinimumSize().height > 84

        when: 'The style is removed again and the component paints:'
            applyStyle = false
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
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
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The panel is clamped: both a minimum and a maximum height are pinned:'
            panel.isMinimumSizeSet()
            panel.isMaximumSizeSet()

        when: 'The animation completes, so the clamping style is gone, and the panel paints again:'
            folding = false
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
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
            ComponentBackend.powering(panel).getStyle() != StyleConf.none()

        when: 'We apply the clamp and paint:'
            clamp = true
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The minimum height is pinned:'
            panel.isMinimumSizeSet()
            panel.getMinimumSize().height > naturalMinimum.height

        when: 'We drop only the clamp, so the other styling stays, and paint:'
            clamp = false
            Utility.paintWithoutWindow(panel, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The minimum size is restored to natural, even though the component is still styled:'
            !panel.isMinimumSizeSet()
            panel.getMinimumSize() == naturalMinimum
        and: 'The component is indeed still styled (it was never fully reset to "no style"):'
            ComponentBackend.powering(panel).getStyle() != StyleConf.none()

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

    def 'A button whose border was not painted gets that flag back when the style engine removes its border.'()
    {
        reportInfo """
            To draw a styled border around a button, the style engine installs a border of its own,
            and a button only paints its border while `AbstractButton.isBorderPainted()` is `true`.
            So the engine switches that flag on for a button that had it off - a menu item, or a
            button made plain through `makePlain()`.

            It has to switch the flag off again once it takes its border away. Otherwise the button
            keeps painting whatever border it is given afterwards: a look and feel that is switched to
            at runtime, for example, would draw its own border around every menu item.
        """
        given: 'A button without a painted border, which is styled with a border only sometimes:'
            var styled = false
            var button =
                    UI.button("Plain")
                    .isBorderPaintedIf(false)
                    .withSize(120, 40)
                    .withStyle( it -> styled ? it.border(2, Color.RED) : it )
                    .get(JButton)
        expect: 'The border is not painted to begin with:'
            !button.isBorderPainted()

        when: 'We activate the border style and paint:'
            styled = true
            Utility.paintWithoutWindow(button, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The border is painted, because the style engine has to draw it:'
            button.isBorderPainted()

        when: 'We remove the style again and paint:'
            styled = false
            Utility.paintWithoutWindow(button, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The button is back to not painting its border:'
            !button.isBorderPainted()
    }

    def 'When the style of a scroll pane is removed, SwingTree keeps a viewport background color which was set while the style was active.'()
    {
        reportInfo """
            When a style gives a scroll pane a background color, SwingTree gives that color to the
            viewport inside the scroll pane as well. When the style is removed again, SwingTree puts
            the old background color of the viewport back, but only if the viewport still has the
            color of the style. If something else replaced the background of the viewport in the
            meantime, then that replacement has to stay.

            Let's first look at why SwingTree touches the viewport at all.
            The content of a `JScrollPane` is not a child of the scroll pane itself. It is a child of a
            `JViewport`, and that viewport is a child of the scroll pane. The viewport covers the area
            inside the border of the scroll pane which is not taken by scroll bars or headers, it is
            painted on top of the scroll pane, and it is opaque. So it fills that whole area with its
            own background color, which under Swing's default Metal look and feel is a light grey,
            `(238, 238, 238)`. A red background which a style gives to the scroll pane would be hidden
            under that grey. This is why SwingTree calls `viewport.setBackground(..)` with the red of
            the style too, and remembers the grey, so that it can put the grey back once the style is
            removed.

            Now suppose that, while the style is still active, something else calls
            `viewport.setBackground(..)` with a blue. That can be your application code. It can also
            be a look and feel which is switched to at runtime and installs its own default background
            color on the viewport. If SwingTree put the remembered grey back when the style is removed,
            it would throw that blue away. After a switch of the look and feel it would even bring back
            a color of the look and feel which was installed before, so the viewport would show the
            grey of the old look and feel inside a scroll pane painted by the new one.
            So SwingTree puts the grey back only while the viewport still has the red it was given.

            A look and feel wraps the colors it installs in a `ColorUIResource`, which is how Swing
            tells the colors of a look and feel apart from the colors an application sets. The
            scenario uses a `ColorUIResource` for the blue, so that it is exactly the kind of color a
            look and feel would install.

            The styler lambda reads the local variable `styled` every time SwingTree runs it. While
            `styled` is `false`, the lambda returns the style delegate `it` unchanged, which is the same
            as having no style at all. SwingTree applies a style to a component while the component is
            painted, which is why the scenario paints the scroll pane after every change of `styled`.
            And because Swing skips painting a component whose width or height is 0, the scroll pane
            gets a size of 200 by 100 pixels.
        """
        given: 'A scroll pane whose styler sets a red background only while the variable `styled` is true:'
            var styled = false
            var scrollPane =
                    UI.scrollPane()
                    .withSize(200, 100)
                    .withStyle( it -> styled ? it.backgroundColor(Color.RED) : it )
                    .get(JScrollPane)
            var viewport = scrollPane.getViewport()
        and: 'We remember the background color of the viewport from before the style, the default of the look and feel:'
            var original = viewport.getBackground()

        when: 'We turn the style on and paint the scroll pane, so that SwingTree applies the style:'
            styled = true
            Utility.paintWithoutWindow(scrollPane, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'SwingTree has given the red of the style to the viewport:'
            viewport.getBackground() == Color.RED

        when: 'While the style is still active, the viewport gets a blue background the way a look and feel would install it:'
            var replacement = new javax.swing.plaf.ColorUIResource(Color.BLUE)
            viewport.setBackground(replacement)
        and: 'We turn the style off and paint the scroll pane again, so that SwingTree removes the style:'
            styled = false
            Utility.paintWithoutWindow(scrollPane, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The viewport still has the blue background, SwingTree did not put the remembered color back over it:'
            viewport.getBackground() == replacement
            viewport.getBackground() != original
    }

    def 'When the style of a scroll pane is removed, SwingTree gives the viewport back the background color it had before the style.'()
    {
        reportInfo """
            When a style gives a scroll pane a background color, SwingTree gives that color to the
            viewport inside the scroll pane as well. When the style is removed again, the viewport has
            to get back the background color it had before the style was applied.

            To see why SwingTree touches the viewport at all, you need to know how a `JScrollPane` is
            painted. The content of a scroll pane is not a child of the scroll pane itself. It is a
            child of a `JViewport`, and that viewport is a child of the scroll pane. The viewport covers
            the area inside the border of the scroll pane which is not taken by scroll bars or headers,
            it is painted on top of the scroll pane, and it is opaque. So it fills that whole area with
            its own background color, which under Swing's default Metal look and feel is a light grey,
            `(238, 238, 238)`. A red background which a style gives to the scroll pane would be hidden
            under that grey. This is why SwingTree calls `viewport.setBackground(..)` with the red of
            the style too.

            When the style is removed, SwingTree gives the scroll pane its old background color back.
            If SwingTree did not do the same for the viewport, the viewport would stay red, and you would
            see a red area inside a grey scroll pane which no longer has any style at all.
            So SwingTree remembers the grey of the viewport when it applies the style, and calls
            `viewport.setBackground(..)` with that grey again when the style is removed.

            The scenario checks that the viewport really is red while the style is active. Without that
            check the scenario would also pass if SwingTree never gave the red to the viewport, because
            then the viewport would simply keep its grey the whole time.

            The styler lambda reads the local variable `styled` every time SwingTree runs it. While
            `styled` is `false`, the lambda returns the style delegate `it` unchanged, which is the same
            as having no style at all. SwingTree applies a style to a component while the component is
            painted, which is why the scenario paints the scroll pane after every change of `styled`.
            And because Swing skips painting a component whose width or height is 0, the scroll pane
            gets a size of 200 by 100 pixels.
        """
        given: 'A scroll pane whose styler sets a red background only while the variable `styled` is true:'
            var styled = false
            var scrollPane =
                    UI.scrollPane()
                    .withSize(200, 100)
                    .withStyle( it -> styled ? it.backgroundColor(Color.RED) : it )
                    .get(JScrollPane)
            var viewport = scrollPane.getViewport()
        and: 'We remember the background color of the viewport from before the style, the default of the look and feel:'
            var original = viewport.getBackground()

        when: 'We turn the style on and paint the scroll pane, so that SwingTree applies the style:'
            styled = true
            Utility.paintWithoutWindow(scrollPane, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'SwingTree has given the red of the style to the viewport:'
            viewport.getBackground() == Color.RED

        when: 'We turn the style off and paint the scroll pane again, so that SwingTree removes the style:'
            styled = false
            Utility.paintWithoutWindow(scrollPane, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The viewport has the background color from before the style again:'
            viewport.getBackground() == original
    }

    def 'When a style stops setting the foreground color of a component, SwingTree gives the component back the foreground color it had before the style.'(
        String remainingStyle, Styler<JLabel> otherStyle, String howTheColorIsSet, Styler<JLabel> redStyle
    ) {
        reportInfo """
            A style can set the color of the text of a component, and it can stop setting it again:
            a hover effect ends, a condition in your styler changes, or a style sheet rule no longer
            applies. When that happens, the component has to get back the foreground color it had
            before the style set one.

            Swing paints the text of a component in its foreground color, the color which
            `getForeground()` returns. A style can set that color in two ways: with
            `foregroundColor(..)`, or as the color of the font with `componentFont(f -> f.color(..))`.
            SwingTree applies both of them by calling `setForeground(..)` on the component. So when the
            style no longer sets a color, SwingTree has to call `setForeground(..)` once more, with the
            color the component had before the style.

            If SwingTree did not do that, the text would keep the red of a style which is no longer
            there. You would see a red label which no styler and no line of your code asks to be red,
            and the red would only go away if your code set a foreground color itself.

            A style does not have to disappear completely to stop setting the foreground color. It may
            keep other properties the whole time. This is why the scenario runs with each of these
            styles staying active: no style at all, a background color, a border and a larger font
            ('$remainingStyle' in this run). And it runs for both ways of setting the color
            ('$howTheColorIsSet' in this run).

            The label starts with a dark grey foreground color, which `withForeground(Color.DARK_GRAY)`
            sets. The styler lambda reads the local variable `styled` every time SwingTree runs it, and
            adds the red only while `styled` is `true`. SwingTree applies a style to a component while the
            component is painted, which is why the scenario paints the label after every change of
            `styled`. And because Swing skips painting a component whose width or height is 0, the label
            gets a size of 120 by 40 pixels.
        """
        given: 'A dark grey label whose styler adds a red text color only while the variable `styled` is true:'
            var styled = false
            var label =
                    UI.label("Text")
                    .withForeground(Color.DARK_GRAY)
                    .withSize(120, 40)
                    .withStyle( it -> styled ? redStyle(otherStyle(it)) : otherStyle(it) )
                    .get(JLabel)
        expect: 'The label starts with its dark grey foreground color:'
            label.getForeground() == Color.DARK_GRAY

        when: 'We turn the red on and paint the label, so that SwingTree applies the style:'
            styled = true
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the red foreground color of the style:'
            label.getForeground() == Color.RED

        when: 'We turn the red off and paint the label again:'
            styled = false
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has its dark grey foreground color from before the style again:'
            label.getForeground() == Color.DARK_GRAY

        where:
            remainingStyle       | otherStyle                                  | howTheColorIsSet  | redStyle
            'no style'           | { it }                                      | 'foregroundColor' | { it.foregroundColor(Color.RED) }
            'a background color' | { it.backgroundColor(Color.YELLOW) }        | 'foregroundColor' | { it.foregroundColor(Color.RED) }
            'a border'           | { it.border(2, Color.BLUE) }                | 'foregroundColor' | { it.foregroundColor(Color.RED) }
            'a larger font'      | { it.componentFont(f -> f.size(20)) }       | 'foregroundColor' | { it.foregroundColor(Color.RED) }
            'no style'           | { it }                                      | 'a font color'    | { it.componentFont(f -> f.color(Color.RED)) }
            'a background color' | { it.backgroundColor(Color.YELLOW) }        | 'a font color'    | { it.componentFont(f -> f.color(Color.RED)) }
            'a border'           | { it.border(2, Color.BLUE) }                | 'a font color'    | { it.componentFont(f -> f.color(Color.RED)) }
            'a larger font'      | { it.componentFont(f -> f.size(20)) }       | 'a font color'    | { it.componentFont(f -> f.color(Color.RED)) }
    }

    def 'When a style stops setting the foreground color of a component without a foreground color of its own, the component follows the foreground color of its parent again.'(
        String howTheColorIsSet, Styler<JLabel> redStyle
    ) {
        reportInfo """
            A component does not need a foreground color of its own. When nothing set one on it, or
            `setForeground(null)` was called, then `getForeground()` returns the foreground color of its
            parent, and the component follows the parent whenever the parent changes its color.
            When a style sets a foreground color on such a component and then stops setting it, the
            component has to go back to having no foreground color of its own, so that it follows its
            parent again.

            A style can set the foreground color in two ways: with `foregroundColor(..)`, or as the color
            of the font with `componentFont(f -> f.color(..))`. SwingTree applies both of them by calling
            `setForeground(..)` on the component, and before it does that, it remembers the color the
            component had, so that it can put that color back later. The difficulty lies in remembering
            the color. In this scenario `getForeground()` returns green, but it does not tell whether the
            green belongs to the label or to the panel around it. `isForegroundSet()` does: it returns
            `false` when the label has no foreground color of its own.

            If SwingTree remembered the green from `getForeground()` and put it back with
            `setForeground(..)`, the label would look right at first. But the green would now be a
            foreground color of the label itself, and when the panel later changed its foreground color
            to blue, the label would stay green. Your code might never have set a foreground color on
            this label, and yet the label would have stopped following its panel.

            The scenario runs for both ways of setting the color ('$howTheColorIsSet' in this run).
            The panel has a green foreground color. The label inside it has none, because
            `withForeground(UI.Color.UNDEFINED)` calls `setForeground(null)` on it. The styler lambda reads
            the local variable `styled` every time SwingTree runs it, and adds the red only while `styled`
            is `true`. SwingTree applies a style to a component while the component is painted, which is
            why the scenario paints the label after every change of `styled`. And because Swing skips
            painting a component whose width or height is 0, the label gets a size of 120 by 40 pixels.
        """
        given: 'A green panel holding a label without a foreground color, whose styler adds a red text color only while `styled` is true:'
            var styled = false
            var panel =
                    UI.panel()
                    .withForeground(Color.GREEN)
                    .add(
                        UI.label("Text")
                        .withForeground(UI.Color.UNDEFINED)
                        .withSize(120, 40)
                        .withStyle( it -> styled ? redStyle(it) : it )
                    )
                    .get(JPanel)
            var label = panel.getComponent(0) as JLabel
        expect: 'The label has no foreground color of its own and shows the green of the panel:'
            !label.isForegroundSet()
            label.getForeground() == Color.GREEN

        when: 'We turn the red on and paint the label, so that SwingTree applies the style:'
            styled = true
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the red foreground color of the style:'
            label.isForegroundSet()
            label.getForeground() == Color.RED

        when: 'We turn the red off and paint the label again:'
            styled = false
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has no foreground color of its own again and shows the green of the panel:'
            !label.isForegroundSet()
            label.getForeground() == Color.GREEN

        when: 'The panel changes its foreground color to blue:'
            panel.setForeground(Color.BLUE)
        then: 'The label follows the panel and shows the blue:'
            label.getForeground() == Color.BLUE

        where:
            howTheColorIsSet  | redStyle
            'foregroundColor' | { it.foregroundColor(Color.RED) }
            'a font color'    | { it.componentFont(f -> f.color(Color.RED)) }
    }

    def 'When a style stops setting the foreground color of a component, SwingTree keeps a foreground color which your code set while the style was active.'(
        String howTheColorIsSet, Styler<JLabel> redStyle
    ) {
        reportInfo """
            While a style sets the foreground color of a component, your code may call
            `setForeground(..)` on the component too, for example through a property of your view model
            which you bound with `withForeground(Val<Color>)`. When the style then stops setting a
            foreground color, the color which your code set has to stay. SwingTree only takes back a
            color which it set itself.

            A style can set the foreground color in two ways: with `foregroundColor(..)`, or as the color
            of the font with `componentFont(f -> f.color(..))`. SwingTree applies both of them by calling
            `setForeground(..)` on the component. Before it does that, it remembers the color the
            component had, so that it can put that color back once the style stops setting one.

            Now suppose that your code sets blue while the style sets red. If SwingTree put the
            remembered color back when the style stops setting one, it would throw your blue away, and
            the label would show the dark grey from before the style, a color your code has already
            replaced. So SwingTree first checks whether the label still has the red which it set, and
            only then puts the remembered color back.

            The scenario runs for both ways of setting the color ('$howTheColorIsSet' in this run).
            The label starts with a dark grey foreground color, which `withForeground(Color.DARK_GRAY)`
            sets. The styler lambda reads the local variable `styled` every time SwingTree runs it, and
            adds the red only while `styled` is `true`. SwingTree applies a style to a component while the
            component is painted, which is why the scenario paints the label after every change of
            `styled`. And because Swing skips painting a component whose width or height is 0, the label
            gets a size of 120 by 40 pixels.
        """
        given: 'A dark grey label whose styler adds a red text color only while the variable `styled` is true:'
            var styled = false
            var label =
                    UI.label("Text")
                    .withForeground(Color.DARK_GRAY)
                    .withSize(120, 40)
                    .withStyle( it -> styled ? redStyle(it) : it )
                    .get(JLabel)

        when: 'We turn the red on and paint the label, so that SwingTree applies the style:'
            styled = true
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the red foreground color of the style:'
            label.getForeground() == Color.RED

        when: 'While the style is still active, our code sets a blue foreground color on the label:'
            label.setForeground(Color.BLUE)
        and: 'We turn the red off and paint the label again:'
            styled = false
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label keeps the blue which our code set:'
            label.getForeground() == Color.BLUE

        where:
            howTheColorIsSet  | redStyle
            'foregroundColor' | { it.foregroundColor(Color.RED) }
            'a font color'    | { it.componentFont(f -> f.color(Color.RED)) }
    }

    def 'When a style changes its foreground color after your code set one, SwingTree gives back the color of your code once the style stops setting a foreground color.'(
        String howTheColorIsSet, Closure<?> colorStyle
    ) {
        reportInfo """
            While a style sets the foreground color of a component, your code may set a foreground color
            too, and the style may change its color afterwards, for example from a hover color to a
            pressed color. When the style finally stops setting a foreground color, the component has to
            show the color which your code set last, and not the color the component had before the style.

            A style can set the foreground color in two ways: with `foregroundColor(..)`, or as the color
            of the font with `componentFont(f -> f.color(..))`. SwingTree applies both of them by calling
            `setForeground(..)` on the component. Before it does that, it remembers the color the
            component had, so that it can put that color back once the style stops setting one.

            Let's walk through the steps of this scenario. The label starts dark grey. The style sets red,
            and SwingTree remembers the dark grey. Then our code sets blue. Then the style changes its
            color to green. At this moment the label has the blue and not the red which SwingTree set, so
            SwingTree treats the blue as a color from outside of the style: it remembers the blue in place
            of the dark grey, and then it sets the green. When the style stops setting a color, the label
            still has the green which SwingTree set, so SwingTree puts the blue back.

            If SwingTree kept remembering the dark grey, the label would end up dark grey, a color which
            our code replaced two steps earlier. And if SwingTree never put a color back, the label would
            stay green, the color of a style which is no longer there.

            The scenario runs for both ways of setting the color ('$howTheColorIsSet' in this run).
            The styler lambda reads the local variable `styleColor` every time SwingTree runs it. While
            `styleColor` is `null`, the lambda sets no foreground color at all. SwingTree applies a style to
            a component while the component is painted, which is why the scenario paints the label after
            every change of `styleColor`. And because Swing skips painting a component whose width or
            height is 0, the label gets a size of 120 by 40 pixels.
        """
        given: 'A dark grey label whose styler sets the text color stored in the variable `styleColor`, unless it is null:'
            Color styleColor = null
            var label =
                    UI.label("Text")
                    .withForeground(Color.DARK_GRAY)
                    .withSize(120, 40)
                    .withStyle( it -> styleColor == null ? it : colorStyle(it, styleColor) )
                    .get(JLabel)

        when: 'The style sets red and we paint the label, so that SwingTree applies the style:'
            styleColor = Color.RED
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the red foreground color of the style:'
            label.getForeground() == Color.RED

        when: 'Our code sets a blue foreground color on the label:'
            label.setForeground(Color.BLUE)
        and: 'The style changes its color to green and we paint the label again:'
            styleColor = Color.GREEN
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the green foreground color of the style:'
            label.getForeground() == Color.GREEN

        when: 'The style stops setting a color and we paint the label again:'
            styleColor = null
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the blue which our code set, not the dark grey from before the style:'
            label.getForeground() == Color.BLUE

        where:
            howTheColorIsSet  | colorStyle
            'foregroundColor' | { it, color -> it.foregroundColor(color) }
            'a font color'    | { it, color -> it.componentFont(f -> f.color(color)) }
    }

    def 'When a style stops setting a font color and a foreground color one after the other, the component shows the color which is left and then the foreground color from before the style.'(
        String firstGone, boolean fontColorStays, boolean foregroundColorStays, Color colorInBetween
    ) {
        reportInfo """
            A style can set the foreground color of a component in two ways at once: with
            `foregroundColor(..)`, and as the color of the font with `componentFont(f -> f.color(..))`.
            Both end up in the same place, because SwingTree applies both of them by calling
            `setForeground(..)` on the component. When a style sets both, the font color wins, because it
            is the more specific of the two. When the style stops setting them one after the other, the
            component has to show the color which is left, and once both are gone, it has to get back the
            foreground color it had before the style.

            The order in which the two colors go away must not matter, which is why the scenario runs
            once with the font color going away first and once with the foreground color going away first
            ('$firstGone' goes away first in this run).

            When the font color goes away first, the label changes from the red font color to the blue
            foreground color: SwingTree calls `setForeground(..)` with the blue. SwingTree still has to
            remember the dark grey from before the style at this step, because the blue is a color of the
            style too. If SwingTree forgot the dark grey when it replaced its own red with its own blue,
            the label would stay blue once the style stops setting both colors.

            The label starts with a dark grey foreground color, which `withForeground(Color.DARK_GRAY)`
            sets. The styler lambda reads the local variables `withForegroundColor` and `withFontColor`
            every time SwingTree runs it, and sets the blue foreground color and the red font color only
            while the matching variable is `true`. SwingTree applies a style to a component while the
            component is painted, which is why the scenario paints the label after every change of these
            variables. And because Swing skips painting a component whose width or height is 0, the label
            gets a size of 120 by 40 pixels.
        """
        given: 'A dark grey label whose styler sets a blue foreground color and a red font color, each only while its variable is true:'
            var withForegroundColor = false
            var withFontColor = false
            var label =
                    UI.label("Text")
                    .withForeground(Color.DARK_GRAY)
                    .withSize(120, 40)
                    .withStyle( it -> {
                        var style = withForegroundColor ? it.foregroundColor(Color.BLUE) : it
                        return withFontColor ? style.componentFont(f -> f.color(Color.RED)) : style
                    })
                    .get(JLabel)
        expect: 'The label starts with its dark grey foreground color:'
            label.getForeground() == Color.DARK_GRAY

        when: 'We turn both colors on and paint the label, so that SwingTree applies the style:'
            withForegroundColor = true
            withFontColor = true
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the red of the font color, which wins over the foreground color:'
            label.getForeground() == Color.RED

        when: 'We turn one of the two colors off and paint the label again:'
            withFontColor = fontColorStays
            withForegroundColor = foregroundColorStays
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has the color which is left:'
            label.getForeground() == colorInBetween

        when: 'We turn the other color off as well and paint the label again:'
            withFontColor = false
            withForegroundColor = false
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has its dark grey foreground color from before the style again:'
            label.getForeground() == Color.DARK_GRAY

        where:
            firstGone              | fontColorStays | foregroundColorStays | colorInBetween
            'the font color'       | false          | true                 | Color.BLUE
            'the foreground color' | true           | false                | Color.RED
    }

    def 'When a style stops setting the foreground color `UI.Color.UNDEFINED`, SwingTree gives the component back the foreground color it had before the style.'()
    {
        reportInfo """
            The style API does not accept `null` as a color, so a style uses the constant
            `UI.Color.UNDEFINED` in its place. When a style sets `foregroundColor(UI.Color.UNDEFINED)`,
            SwingTree calls `setForeground(null)` on the component, so the component has no foreground
            color of its own while the style is active. When the style stops setting it, the component
            has to get back the foreground color it had before the style.

            SwingTree only puts the color from before the style back if the component still has the
            color which the style gave it, because a color which your code set in the meantime has to
            stay. For `UI.Color.UNDEFINED` that check cannot compare colors with `getForeground()`.
            A component without a foreground color of its own returns the foreground color of its parent
            from `getForeground()`, which is green in this scenario, and green is not the `null` which
            SwingTree set. The check has to call `isForegroundSet()` instead, which returns `false` for a
            component without a foreground color of its own. If SwingTree compared the colors, it would
            take the green for a color which your code set, and the label would never get its dark grey
            back.

            The panel has a green foreground color, and the label inside it starts with a dark grey
            foreground color, which `withForeground(Color.DARK_GRAY)` sets. The styler lambda reads the
            local variable `styled` every time SwingTree runs it, and sets `UI.Color.UNDEFINED` only while
            `styled` is `true`. SwingTree applies a style to a component while the component is painted,
            which is why the scenario paints the label after every change of `styled`. And because Swing
            skips painting a component whose width or height is 0, the label gets a size of 120 by 40
            pixels.
        """
        given: 'A green panel holding a dark grey label, whose styler sets `UI.Color.UNDEFINED` only while `styled` is true:'
            var styled = false
            var panel =
                    UI.panel()
                    .withForeground(Color.GREEN)
                    .add(
                        UI.label("Text")
                        .withForeground(Color.DARK_GRAY)
                        .withSize(120, 40)
                        .withStyle( it -> styled ? it.foregroundColor(UI.Color.UNDEFINED) : it )
                    )
                    .get(JPanel)
            var label = panel.getComponent(0) as JLabel
        expect: 'The label starts with its dark grey foreground color:'
            label.getForeground() == Color.DARK_GRAY

        when: 'We turn the style on and paint the label, so that SwingTree applies the style:'
            styled = true
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has no foreground color of its own and shows the green of the panel:'
            !label.isForegroundSet()
            label.getForeground() == Color.GREEN

        when: 'We turn the style off and paint the label again:'
            styled = false
            Utility.paintWithoutWindow(label, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())
        then: 'The label has its dark grey foreground color from before the style again:'
            label.isForegroundSet()
            label.getForeground() == Color.DARK_GRAY
    }

}
