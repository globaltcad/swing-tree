package swingtree.styles

import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.UI
import swingtree.style.ShadingStrategy
import utility.Utility

import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.*
import java.awt.image.BufferedImage

@Title("Styling Components")
@Narrative('''
    This specification demonstrates how you can use the styling
    API to style Swing components in declarative SwingTree code.
''')
class Individual_Component_Styling_Spec extends Specification
{
    def 'Styling components is based on a functional styler lambda.'()
    {
        reportInfo """
            Fun-Fact: 
            Styling in SwingTree is fully functional, which means 
            that the `Style` settings objects are all immutable. 
            They are not modified in place, but instead transformed
            by so called "styler" lambdas.
            Not only does this architecture make it easy to compose, reuse and share
            styles, but it also makes it possible to have a complex style
            inheritance hierarchy without the need for very complex code.
            In practice, this means that your styler lambdas become part
            of a compositional tree of styler lambdas, which is then applied to
            the component tree in a single pass.
            How cool is that? :)
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.foundationColor("green")
                              .backgroundColor("cyan")
                              .foregroundColor("blue")
                              .borderColor("blue")
                              .borderWidth(5)
                              .shadowColor("black")
                              .shadowSpreadRadius(10)
                              .shadowOffset(10)
                              .font("Papyrus", 42)
                        )
        expect : 'The background color of the panel will be set to cyan.'
            panel.component.background == Color.cyan
        and : 'The foreground color of the panel will be set to blue.'
            panel.component.foreground == Color.blue
        and : 'The insets of the border will be increased by the border width (because the border grows inwards).'
            panel.component.border.getBorderInsets(panel.component) == new Insets(5, 5, 5, 5)
        and : 'The font of the panel will be set to Papyrus with a size of 42.'
            panel.component.font == new Font("Papyrus", Font.PLAIN, 42)
    }

    def 'The margins defined in the style API will be applied to the layout manager through the border insets.'()
    {
        reportInfo """
            Swing does not have a concept of margins.
            Without a proper layout manager it does not even support the configuration of insets.
            However, through a custom `Border` implementation and a default layout manager (MigLayout)
            we can model the margins (and paddings) of a component.
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.marginRight(42)
                              .marginLeft(64)
                        )
                        .get(JPanel)
        expect : """
            Note that the insets of the border of the component now model the margins of the component.
            This information is used by the layout manager to position the component correctly.
        """
            panel.border.getBorderInsets(panel) == new Insets(0, 64, 0, 42)
        and :
            panel.layout != null
            panel.layout instanceof MigLayout
    }


    def 'The insets of the layout manager are based on the sum of the margin and padding for a given edge of the component bounds.'()
    {
        reportInfo """
            Swing does not have a concept of padding and margin.
            Without a proper layout manager it does not even support the configuration of insets.
            However, through a custom `Border` implementation and a default layout manager (MigLayout)
            we can model the padding and margin of a component
            and also render a fancy border and shadow around it (if specified).
            Internally the layout manager will indirectly know about the margins and paddings
            of your component through the `Border::getBorderInsets(Component)` method.
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.marginTop(11)
                              .marginRight(42)
                              .marginLeft(64)
                              .paddingRight(10)
                              .paddingLeft(20)
                              .paddingBottom(30)
                        )
                        .get(JPanel)
        expect :
            panel.border.getBorderInsets(panel) == new Insets(11, 84, 30, 52)
        and :
            panel.layout != null
            panel.layout instanceof MigLayout
    }

    def 'The Styling API will make sure that the layout manager accounts for the border width!'()
    {
        reportInfo """
            A border is a very common feature of Swing components and when it comes to styling
            your UI elements should not overlap with the border.
            This is why the styling API will make sure that the layout manager accounts for the border width
            you specify in your style.
            Internally the layout manager will indirectly know about the margins and paddings
            of your component through the `Border::getBorderInsets(Component)` method.
        """
        given : 'We create a panel with some custom styling!'
            var panel =
                        UI.panel()
                        .withStyle( it ->
                            it.marginTop(7)
                              .marginRight(2)
                              .paddingLeft(14)
                              .borderWidth(5)
                        )
                        .get(JPanel)
        expect : """
            The insets of the border not only model the padding and margin of the component,
            but also the border width.
        """
            panel.border.getBorderInsets(panel) == new Insets(12, 19, 5, 7)
        and : 'We also expect there to be the mig layout manager by default.'
            panel.layout != null
            panel.layout instanceof MigLayout
    }


    def 'SwingTree will un-install any custom border if no styles are found.'()
    {
        reportInfo """
            SwingTree will dynamically install or uninstall a custom border on a component
            depending on whether or not there are any styles defined for the component.
            Check out the following example:
        """
        given : 'We create a simple `JLabel` UI component with a style animation.'
            var doStyle = true
            var label = UI.label("Click me!")
                            .withStyle(style ->
                                 doStyle ? style.border(3, new Color(230, 238, 220)) : style
                            )

        expect : """
            There is now a custom border installed on the label.
        """
            label.component.border != null

        when : 'We disable the style and simulate a repaint.'
            doStyle = false
            label.component.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : """
            The custom border has been uninstalled.
        """
            label.component.border == null
    }


    def 'SwingTree will re-install any borders overridden by the style API.'()
    {
        reportInfo """
            If you supply a border implementation yourself, SwingTree may override it with a custom border
            if you define a style for the component.
            However, if you disable the style, SwingTree will re-install the original border.
            Smart, right?
        """
        given : 'We create a simple `JLabel` UI component with a style animation.'
            var doStyle = false
            var label = UI.label("Click me!").withBorderTitled("Original border")
                            .withStyle(style ->
                                 doStyle ? style.border(3, new Color(230, 238, 220)) : style
                            )

        expect : """
            Initially there is a custom border installed on the label.
        """
            label.component.border instanceof TitledBorder
            label.component.border.title == "Original border"

        when : 'We enable the style and simulate a repaint.'
            doStyle = true
            label.component.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : """
            The custom border has been installed.
        """
            label.component.border != null
            !(label.component.border instanceof TitledBorder)

        when : 'We disable the style and simulate a repaint.'
            doStyle = false
            label.component.paint(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics())

        then : """
            The original border has been re-installed.
        """
            label.component.border instanceof TitledBorder
            label.component.border.title == "Original border"
    }

    def 'This is how you can create a rounded green label with a border at the bottom.'()
    {
        reportInfo """
            This is how you can create a rounded green label with a border at the bottom.
            It looks like this:
            ${Utility.linkSnapshot('components/rounded-green-JLabel.png')}
            
            It demonstrates how to style a JLabel with the style API.
        """
        given : 'We create a UI with a green label.'
            var ui =
                    UI.label("I am a green label")
                    .withStyle( it -> it
                        .size(205, 60)
                        .backgroundColor(new Color(40,200,70))
                        .padding(4, 7, 4, 7)
                        .border(0, 0, 3, 0, Color.BLACK)
                        .borderRadius(13)
                        .margin(12)
                        .foregroundColor(Color.GREEN)
                        .shadowColor(new Color(0,0,0,100))
                        .shadowSpreadRadius(1)
                        .shadowBlurRadius(2)
                        .font(new Font("Arial", Font.BOLD, 20))
                    )

        when : 'We render the label into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/rounded-green-JLabel.png", 99.9) > 99.9
    }

    def 'This is how you can create a JPanel with a shaded border.'()
    {
        reportInfo """
            This is how you can create a JPanel with a shaded border.
            It looks like this:
            ${Utility.linkSnapshot('components/shaded-border-JPanel.png')}
            
            It demonstrates how to style a JPanel with the style API.
        """
        given : 'We create a UI with a green label.'
            var ui =
                    UI.panel()
                    .withStyle( it -> it
                        .size(205, 205)
                        .backgroundColor(new Color(40,180,240))
                        .foundationColor(new Color(200,200,240))
                        .padding(30)
                        .margin(22)
                        .border(15, Color.CYAN)
                        .borderShade( s -> s
                            .strategy(ShadingStrategy.BOTTOM_RIGHT_TO_TOP_LEFT)
                            .colors(Color.YELLOW, new Color(255,255,255,0))
                        )
                        .shadowColor(new Color(0,0,0,100))
                        .shadowSpreadRadius(1)
                        .shadowBlurRadius(2)
                        .font(new Font("Arial", Font.BOLD, 20))
                    )

        when : 'We render the label into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/shaded-border-JPanel.png", 99.9) > 99.9
    }

    def 'You can style a toggle button to have a custom selection shading.'()
    {
        reportInfo """
            You can style a toggle button to have a custom selection shading.
            This is what it looks like:
            ${Utility.linkSnapshot('components/shaded-JToggleButton.png')}
            And when toggled it would like this:
            ${Utility.linkSnapshot('components/selection-shaded-JToggleButton.png')}
            
            It demonstrates how to style a JToggleButton with the style API
            in a way where the style changes depending on the component state.
        """
        given : 'We create a UI with a green label.'
            var ui =
                    UI.toggleButton("I am a toggle button")
                    .withStyle( it -> it
                        .size(205, 60)
                        .backgroundShade( shade -> shade
                           .strategy(ShadingStrategy.TOP_LEFT_TO_BOTTOM_RIGHT)
                           .colors(
                               it.component().getModel().isSelected()
                                   ? new Color[]{ Color.YELLOW, Color.CYAN   }
                                   : new Color[]{ Color.CYAN,   Color.ORANGE }
                           )
                        )
                    )

        when : 'We render the label into a BufferedImage.'
            var image1 = Utility.renderSingleComponent(ui.getComponent())
            ui.getComponent().setSelected(true)
            var image2 = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image1, "components/shaded-JToggleButton.png", 99.9) > 99.9
            Utility.similarityBetween(image2, "components/selection-shaded-JToggleButton.png", 99.9) > 99.9
    }

    def 'Make a text area look like it is sunken in the background using a shadow going inwards.'()
    {
        reportInfo """
            Make a text area look like it is sunken in the background using a shadow going inwards.
            This is what it looks like:
            ${Utility.linkSnapshot('components/sunken-JTextArea.png')}
            
            It demonstrates how to style a JTextArea with the style API.
        """
        given : 'We create a UI with a green label.'
            var ui =
                    UI.textArea("""Ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.""")
                    .peek( it -> it.setLineWrap(true) )
                    .withStyle( it -> it
                        .size(200, 240)
                        .backgroundColor(new Color(120,255,240))
                        .foundationColor(new Color(50,100,200))
                        .margin(12)
                        .padding(6, 7, 6, 7)
                        .borderRadius(16)
                        .foregroundColor(Color.DARK_GRAY)
                        .shadowColor(new Color(0,0,0))
                        .shadowBlurRadius(4)
                        .shadowIsInset(true)
                        .font(new Font("Palatino", Font.PLAIN, 20))
                    )

        when : 'We render the label into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/sunken-JTextArea.png", 99.9) > 99.9
    }

}
