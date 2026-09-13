package swingtree

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.api.Styler
import utility.Utility

import javax.swing.*
import javax.swing.border.Border
import java.awt.*

@Title("The SwingTree Border Insets")
@Narrative('''
 
    SwingTree uses a custom `Border` implementation to support the styling
    of components. This includes the definition of the border, margin, padding
    dimensions which together form the whole border insets of a component.
    
    The border insets are then used by Swing to determine the size of the component
    and its children.
    
    In this specification we will test the border insets of components
    that are heavily styled through the SwingTree styling API.
    
''')
class Styled_Component_Border_Inset_Spec extends Specification
{
    def cleanup() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
    }


    def 'A heavily styled text field will have the correct border insets!'(
        float uiScale
    ) {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'Now we create a text field UI with a custom styler lambda and a button.'
            var seed = Utility.loadImage("img/seed.png")
            var trees = Utility.loadImage("img/trees.png")
            var ui =
                    UI.textField("I am fancy! :)").withLayout("fill, ins 0").withPrefSize(190, 25)
                    .withStyle( it -> it
                        .fontSize(16)
                        .paddingLeft(26)
                        .marginRight(25)
                        .paddingRight(-20)
                        .image(UI.Layer.BORDER, image -> image
                            .image(seed)
                            .placement(UI.Placement.LEFT)
                            .width(30).autoFit(true)
                            .padding(3)
                        )
                    )
                    .add("right",
                        UI.button(19, 19, new ImageIcon(trees))
                        .withStyle( it -> it
                            .padding(0)
                            .cursor(UI.Cursor.HAND)
                        )
                        .makePlain()
                    );
        and : 'We unpack the component and its border:'
            var component = ui.get(JTextField)
            var border = component.getBorder()


        expect : 'The insets are as expected.'
            border.getBorderInsets(component) == new Insets(2, (int)(26*uiScale), 2, (int)(25*uiScale - 20*uiScale))
            border.getMarginInsets() == new Insets(0,0,0,(int)(25*uiScale))
            border.getPaddingInsets() == new Insets(2, (int)(26*uiScale), 2, (int)(-20*uiScale))
            border.getFullPaddingInsets() == new Insets(2, (int)(26*uiScale), 2, (int)(25*uiScale - 20*uiScale))

        where :
            uiScale << [1, 2, 3]
    }

    def 'A heavily styled slider will have the correct border insets!'(
        float uiScale
    ) {
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'Now we create a slider UI with a custom styler lambda:'
            var ui =
                    UI.slider(UI.Axis.HORIZONTAL, 0, 100, 50)
                    .withStyle( it -> it
                        .size(280, 38)
                        .prefSize(280, 38)
                        .borderRadius(20)
                        .backgroundColor(new Color(0.4f, 0.85f, 1))
                        .foundationColor(new Color(0.4f, 0.85f, 1))
                        .shadow(UI.Layer.BACKGROUND, "bright", s -> s
                            .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                            .offset(-11)
                        )
                        .shadow(UI.Layer.BACKGROUND, "dark", s -> s
                            .color(new Color(0, 0.1f, 0.2f, 0.20f))
                            .offset(+4)
                        )
                        .shadowBlurRadius(4)
                        .shadowSpreadRadius(-2)
                        .shadowIsInset(true)
                        .padding(6)
                        .margin(10)
                    );

        and : 'We unpack the component and its border:'
            var component = ui.get(JSlider)
            var border = component.getBorder()


        expect : 'The insets are as expected.'
            border.getBorderInsets(component) == new Insets((int)(16*uiScale), (int)(16*uiScale), (int)(16*uiScale), (int)(16*uiScale))
            border.getMarginInsets() == new Insets((int)(10*uiScale),(int)(10*uiScale),(int)(10*uiScale),(int)(10*uiScale))
            border.getPaddingInsets() == new Insets((int)(6*uiScale), (int)(6*uiScale), (int)(6*uiScale), (int)(6*uiScale))
            border.getFullPaddingInsets() == new Insets((int)(16*uiScale), (int)(16*uiScale), (int)(16*uiScale), (int)(16*uiScale))

        where :
            uiScale << [1, 2, 3]
    }

    def 'A `JMenuItem` will not loose its default Nimbus border insets when styled!'(
        float uiScale, Styler<JMenuItem> styler, Class<? extends Border> borderType
    ) {
        reportInfo """
            Many components have default borders with default insets. 
            When using the nimbus look and feel, then the menu item 
            will have a default border with default insets.
            When you then style the menu item 
            (which SwingTree largely does through the border), 
            then you should not loose these default insets provided by the nimbus look and feel.
        """
        given : 'We first switch to the nimbus look and feel'
            Utility.setLaF(Utility.LaF.NIMBUS)
        and : """
            Then we create a menu item saying "SwingTree" in japanese and 
            then we style it with a custom styler lambda.
        """
            var ui =
                    UI.menuItem("ブランコツリー")
                    .withStyle(styler);

        and : 'We unpack the component and its border:'
            var component = ui.get(JMenuItem)
            var border = component.getBorder()
        expect : 'The insets are as expected.'
            border.getBorderInsets(component) == new Insets(1, 12, 2, 13)
        and : 'It has the expected border type:'
            borderType.isAssignableFrom(border.getClass())

        where :
            uiScale | styler                                             || borderType

            1       | { it -> it }                                       || javax.swing.plaf.synth.SynthBorder
            2       | { it -> it }                                       || javax.swing.plaf.synth.SynthBorder
            3       | { it -> it }                                       || javax.swing.plaf.synth.SynthBorder

            1       | { it -> it.foregroundColor(Color.BLUE) }           || javax.swing.plaf.synth.SynthBorder
            2       | { it -> it.foregroundColor(Color.BLUE) }           || javax.swing.plaf.synth.SynthBorder
            3       | { it -> it.foregroundColor(Color.BLUE) }           || javax.swing.plaf.synth.SynthBorder

            1       | { it -> it.cursor(UI.Cursor.HAND) }                || javax.swing.plaf.synth.SynthBorder
            2       | { it -> it.cursor(UI.Cursor.HAND) }                || javax.swing.plaf.synth.SynthBorder
            3       | { it -> it.cursor(UI.Cursor.HAND) }                || javax.swing.plaf.synth.SynthBorder

            1       | { it -> it.foundationColor(Color.BLUE) }           || javax.swing.plaf.synth.SynthBorder
            2       | { it -> it.foundationColor(Color.BLUE) }           || javax.swing.plaf.synth.SynthBorder
            3       | { it -> it.foundationColor(Color.BLUE) }           || javax.swing.plaf.synth.SynthBorder

            1       | { it -> it.painter(UI.Layer.BORDER,     g2d->{}) } || swingtree.style.StyleAndAnimationBorder
            2       | { it -> it.painter(UI.Layer.CONTENT,    g2d->{}) } || swingtree.style.StyleAndAnimationBorder
            //3       | { it -> it.painter(UI.Layer.BACKGROUND, g2d->{}) } || javax.swing.plaf.synth.SynthBorder
    }

    def 'Fractional `padding`, `border` and `margin` widths are rounded up to whole pixel insets by growing the margin.'(
        float uiScale, double padding, double borderWidth, double margin, int insets
    ) {
        reportInfo """
            Swing measures the border of a component in whole pixels, because
            `Border.getBorderInsets(Component)` returns an `Insets` object, and `Insets`
            holds `int` values. The padding, border width and margin of a SwingTree style
            are `double` values, and the UI scale factor multiplies all three of them.
            So on one side of a component they can add up to a number like 9.75.
            When that happens, SwingTree adds the missing fraction of a pixel to the
            margin on that side, so that padding, border width and margin add up
            to a whole number of pixels, and Swing and the style agree on where
            the content of the component starts.

            Take a padding of 6.25, a border width of 1.5 and a margin of 2 at a
            UI scale factor of 1. Together they are 6.25 + 1.5 + 2 = 9.75 pixels.
            The missing fraction is 1 - 0.75 = 0.25, so the margin becomes 2.25,
            and the insets are 6.25 + 1.5 + 2.25 = 10 pixels on every side.
            Without that correction, Swing would cut 9.75 down to 9 and lay the
            content out 9 pixels in from the edge, while the style paints its
            padding all the way to 9.75 pixels in.

            The UI scale factor is applied before the fraction is measured.
            At a factor of 1.25, a padding of 5, a border width of 1 and a margin of 1
            become 6.25, 1.25 and 1.25, which add up to 8.75, so the margin grows by
            0.25 and the insets are 9 pixels.

            When the widths already add up to a whole number, nothing is added.
            A padding of 6.5, a border width of 1 and a margin of 2.5 are exactly
            10 pixels, and the insets stay at 10, not 11.
        """
        given : 'We set the UI scale factor that the widths are multiplied by:'
            SwingTree.get().setUiScaleFactor(uiScale)
        and : 'We create a panel whose padding, border width and margin are fractional:'
            var ui =
                    UI.panel()
                    .withStyle( it -> it
                        .padding(padding)
                        .border(borderWidth, Color.BLACK)
                        .margin(margin)
                    )
        and : 'We unpack the panel:'
            var component = ui.get(JPanel)

        expect : 'The insets are the rounded up sum of padding, border width and margin on every side.'
            component.getInsets() == new Insets(insets, insets, insets, insets)

        where :
            uiScale | padding | borderWidth | margin || insets
            1       | 6.25    | 1.5         | 2      || 10
            1       | 6.5     | 1           | 2.5    || 10
            1.25    | 5       | 1           | 1      || 9
            2       | 3.125   | 0.5         | 1      || 10
    }

}
