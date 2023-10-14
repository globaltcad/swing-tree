package swingtree.styles

import com.formdev.flatlaf.FlatLightLaf
import examples.stylish.MyTabsView
import examples.stylish.MyTabsViewModel
import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.SwingTree
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import swingtree.UI
import utility.SwingTreeTestConfigurator
import utility.Utility

import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

@Title("Styling Components")
@Narrative('''
    This specification demonstrates how you can use the styling
    API to style Swing components in a functional and declarative fashion.
''')
class Individual_Component_Styling_Spec extends Specification
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

    def 'Styling components is based on a functional styler lambda.'( int uiScale )
    {
        reportInfo """
            Fun-Fact: 
            Styling in SwingTree is fully functional, which means 
            that the `Style` settings objects are all immutable. 
            They are not modified in place, but instead transformed
            by so called "`Styler` lambdas".
            Not only does this architecture make it easy to compose, reuse and share
            styles, but it also makes it possible to have an extensive hierarchy of
            styles without the need for complicated code at all.
            In practice, this means that your styles become part
            of a compositional tree of `Styler` lambdas.
            The fact that they are lambdas makes it possible to
            evaluate the styles every repaint so that they can then applied to
            the components of the component tree completely dynamically.
            How cool is that? :)
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'We create a panel with some custom styling!'
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
            panel.component.border.getBorderInsets(panel.component) == new Insets(5 * uiScale, 5 * uiScale, 5 * uiScale, 5 * uiScale)
        and : 'The font of the panel will be set to Papyrus with a size of 42 * uiScale.'
            panel.component.font.toString().contains("family=Dialog,name=Papyrus,style=plain,size=" + 42 * uiScale)
        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """
            uiScale << [1, 2, 3]
    }

    def 'The margins defined in the style API will be applied to the layout manager through the border insets.'(
        int uiScale
    ) {
        reportInfo """
            Swing does not have a concept of margins.
            Without a proper layout manager it does not even support the configuration of insets.
            However, SwingTree fixes this
            through a custom `Border` implementation and a default layout manager (`MigLayout`)
            which models the margins (and paddings) of a component.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'We create a panel with some custom styling!'
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
            panel.border.getBorderInsets(panel) == new Insets(0, 64 * uiScale, 0, 42 * uiScale)
        and :
            panel.layout != null
            panel.layout instanceof MigLayout
        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """
            uiScale << [3, 2, 1]
    }


    def 'The insets of the layout manager are based on the sum of the margin and padding for a given edge of the component bounds.'(
        int uiScale
    ) {
        reportInfo """
            Swing does not have a concept of padding and margin.
            Without a proper layout manager it does not even support the configuration of insets.
            However, through a custom `Border` implementation and a default layout manager (MigLayout)
            we can model the padding and margin of a component
            and also render a fancy border and shadow around it (if specified).
            Internally the layout manager will indirectly know about the margins and paddings
            of your component through the `Border::getBorderInsets(Component)` method.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'We create a panel with some custom styling!'
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
            panel.border.getBorderInsets(panel) == new Insets(11 * uiScale, 84 * uiScale, 30 * uiScale, 52 * uiScale)
        and :
            panel.layout != null
            panel.layout instanceof MigLayout
        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """
            uiScale << [3, 2, 1]
    }

    def 'The Styling API will make sure that the layout manager accounts for the border width!'( int uiScale )
    {
        reportInfo """
            A border is a very common feature of Swing components and when it comes to styling
            your UI elements should not overlap with the border.
            This is why the styling API will make sure that the layout manager accounts for the border width
            you specify in your style.
            Internally the layout manager will indirectly know about the margins and paddings
            of your component through the `Border::getBorderInsets(Component)` method.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'We create a panel with some custom styling!'
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
            panel.border.getBorderInsets(panel) == new Insets(12 * uiScale, 19 * uiScale, 5 * uiScale, 7 * uiScale)
        and : 'We also expect there to be the mig layout manager by default.'
            panel.layout != null
            panel.layout instanceof MigLayout

        where : """
            We use the following integer scaling factors simulating different high DPI scenarios.
            Note that usually the UI is scaled by 1, 1.5 an 2 (for 4k screens for example).
            A scaling factor of 3 is rather unusual, however it is possible to scale it by 3 nonetheless.
        """
            uiScale << [1, 2, 3]
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
            Utility.similarityBetween(image, "components/rounded-green-JLabel.png", 99.5) > 99.5
    }

    def 'This is how you can create a JPanel with a shaded border.'()
    {
        reportInfo """
            It is really simple to create a JPanel with a shaded border,
            all you need to do is pass the style configuration to the style API.
            SwingTree will dynamically install or uninstall a custom border on a component
            depending on whether or not there are any style configurations defined for the component. <br>
            Note that in this example we make the border extra wide so that you can see the difference.
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
                        .borderGradient(s -> s
                            .transition(UI.Transition.BOTTOM_RIGHT_TO_TOP_LEFT)
                            .colors(Color.YELLOW, new Color(255,255,255,0))
                        )
                        .shadowColor(new Color(0,0,0,100))
                        .shadowSpreadRadius(1)
                        .shadowBlurRadius(2)
                        .font(new Font("Arial", Font.BOLD, 20))
                    )

        when : 'We render the panel into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/shaded-border-JPanel.png", 99.5) > 99.5
    }

    def 'You can style a toggle button to have a custom selection shading.'()
    {
        reportInfo """
            You can style a toggle button to have a custom selection shading.
            Please note that this will actually override some parts of the original 
            look and feel of the button simply because SwingTree needs to cut some
            corners to install custom styles for you. <br>
            So in this example you may notice that the regular Metal Lool and Feel
            is replaced by the our shading.
            ${Utility.linkSnapshot('components/shaded-JToggleButton.png')}
            And when toggled it would like this:
            ${Utility.linkSnapshot('components/selection-shaded-JToggleButton.png')}
            
            Note that this example very nicely demonstrates how the style of a JToggleButton 
            will update immediately depending on component changes.
        """
        given : 'We create a toggle button with some shading gradients.'
            var ui =
                    UI.toggleButton("I am a toggle button")
                    .withStyle( it -> it
                        .size(205, 60)
                        .gradient(shade -> shade
                           .transition(UI.Transition.TOP_LEFT_TO_BOTTOM_RIGHT)
                           .colors(
                               it.component().getModel().isSelected()
                                   ? new Color[]{ Color.YELLOW, Color.CYAN   }
                                   : new Color[]{ Color.CYAN,   Color.ORANGE }
                           )
                        )
                    )

        when : 'We render the toggle button into a BufferedImage.'
            var image1 = Utility.renderSingleComponent(ui.getComponent())
            ui.getComponent().setSelected(true)
            var image2 = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image1, "components/shaded-JToggleButton.png", 99.95) > 99.95
            Utility.similarityBetween(image2, "components/selection-shaded-JToggleButton.png", 99.95) > 99.95
    }

    def 'A text area background can be shaded from left to right with any number of colors.'()
    {
        reportInfo """
            A component can be shaded from left to right with any number of colors.
            ${Utility.linkSnapshot('components/left-to-right-shaded-JTextArea.png')}
            
            In this example we are painting a rainbow shading effect.
            This kind of shading looks really nice on text areas.
        """
        given : 'A text area UI with a custom styler lambda.'
            var ui =
                    UI.textArea("""Ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.""")
                    .peek( it -> it.setLineWrap(true) )
                    .withStyle( it -> it
                        .size(200, 240)
                        .fontBold(true)
                        .border(2, Color.DARK_GRAY)
                        .backgroundColor(Color.CYAN)
                        .padding(12)
                        .margin(6)
                        .gradient(shade -> shade
                           .transition(UI.Transition.LEFT_TO_RIGHT)
                           .colors(
                              new Color(255,0,0,64),
                              new Color(0,255,0,64),
                              new Color(0,0,255,64),
                              new Color(255,255,0,64),
                              new Color(255,0,255,64),
                              new Color(0,255,255,64)
                           )
                           .layer(UI.Layer.BACKGROUND)
                        )
                    )

        when : 'We render the text area into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/left-to-right-shaded-JTextArea.png", 99.95) > 99.95
    }

    def 'Make a text area look like it is sunken in the background using a shadow going inwards.'()
    {
        reportInfo """
            Make a text area look like it is sunken in the background using a shadow going inwards.
            We achieve this by configuring the default shadow (with the name "default").
            ${Utility.linkSnapshot('components/sunken-JTextArea.png')}
            
            This kind of shadow effect looks really nice on text areas.
        """
        given : 'A text area UI with a custom styler lambda.'
            var ui =
                    UI.textArea("""Ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.""")
                    .peek( it -> it.setLineWrap(true) )
                    .withStyle( it -> it
                        .size(240, 200)
                        .backgroundColor(new Color(120,255,240))
                        .foundationColor(new Color(50,100,200))
                        .margin(12)
                        .padding(6, 7, 6, 7)
                        .borderRadius(16)
                        .foregroundColor(Color.DARK_GRAY)
                        .shadowColor(new Color(0,0,0))
                        .shadowBlurRadius(4)
                        .shadowIsInset(true)
                        .font(new Font("Dancing Script", Font.PLAIN, 20))
                    )

        when : 'We render the text area into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/sunken-JTextArea.png", 99.9) > 99.9
    }

    def 'Create a soft UI slider that sinks into the background if you wish.'()
    {
        reportInfo """
            Specifying multiple (named) shadows, allows you to create soft UI,
            like for example a slider that sinks into the background. <br>
            The reason why we use named shadows is because we don't want to override
            the default shadow of the slider, but rather add a new one. <br>

            ${Utility.linkSnapshot('components/soft-JSlider.png')}
            
            The concept of naming exists to make any number of sub-styles possible.
            This concept of sub-styles is not exclusive to shadows.
            You can also name shadesa and custom foreground or background painters.
        """
        given : 'Before we create the styled slider, we first setup up FlatLaF as a basis.'
            FlatLightLaf.setup()
        and : 'Now a slider UI with a custom styler lambda.'
            var ui =
                    UI.slider(UI.Align.HORIZONTAL, 0, 100, 50)
                    .withStyle( it -> it
                        .size(280, 38)
                        .prefSize(280, 38)
                        .borderRadius(20)
                        .backgroundColor(new Color(0.4f, 0.85f, 1))
                        .foundationColor(new Color(0.4f, 0.85f, 1))
                        .shadow("bright", s -> s
                            .color(new Color(0.7f, 0.95f, 1f, 0.35f))
                            .offset(-11)
                        )
                        .shadow("dark", s -> s
                            .color(new Color(0, 0.1f, 0.2f, 0.20f))
                            .offset(+4)
                        )
                        .shadowBlurRadius(4)
                        .shadowSpreadRadius(-2)
                        .shadowIsInset(true)
                        .shadowLayer(UI.Layer.BACKGROUND) // So that the slider track is not affected by the shadow.
                        .padding(6)
                        .margin(10)
                    )

        when : 'We render the slider into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/soft-JSlider.png", 99.8) > 99.8
    }

    def 'The look of a component, like a button for example, will be preserved if possible, when doing custom styling.'()
    {
        reportInfo """
            In order to make it possible for SwingTree to apply the styles you specify,
            it will have to override some parts of the original look and feel of the component.
            However, SwingTree will try to preserve as much of the original look and feel as possible. <br>
            So for example, if you specify simple style attributes like padding, margin or border radius,
            the component will still look like a regular button, 
            but with the specified padding, margin or border radius. <br>
            
            ${Utility.linkSnapshot('components/rounded-metal-JButton.png')}

            In this example we have a button with a custom style that specifies a border radius of 20 pixels
            as well as a little bit of padding and margin. <br>
        """
        given : 'A button UI with a custom styler lambda.'
            var ui =
                    UI.button("I am a button")
                    .withStyle( it -> it
                        .size(200, 60)
                        .border(1, Color.BLACK)
                        .borderRadius(20)
                        .padding(6)
                        .margin(10)
                    )

        when : 'We render the button into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/rounded-metal-JButton.png", 99.9) > 99.9
    }

    def 'Turn a panel into a card like looking panel by giving it a round border, background color and some margins.'()
    {
        reportInfo """
            If you want certain content to be grouped and highlighted by a custom background color
            and a rounded border so that it looks like a card, you can do that
            very easily in a `Styler` lambda. <br>
            Here you can see an example of a panel with a black border of 3 pixels,
            a margin of 10 pixels and a border radius of 36 pixels. <br>
            ${Utility.linkSnapshot('components/banner-JPanel.png')}
        """
        given : 'A panel UI with a custom styler lambda.'
            var ui =
                    UI.panel()
                    .withStyle( it -> it
                        .size(160, 120)
                        .border(3, Color.BLACK)
                        .borderRadius(36)
                        .backgroundColor(Color.CYAN)
                        .padding(6)
                        .margin(10)
                    )

        when : 'We render the panel into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/banner-JPanel.png", 99) > 99
    }

    def 'Adjust how text is styled through the API exposed in your `Styler` lambdas'()
    {
        reportInfo """
                The `Styler` lambda exposes a lot of API to adjust how text is styled. <br>
                More specifically, you can adjust the style of the font, e.g. the font family, size and, well, style. <br>
                <br>
                Here you can see an example of a text area with a custom font family, size and style. <br>
                ${Utility.linkSnapshot('components/custom-font-JTextArea.png')}
            """

        given : 'A text area UI with a custom styler lambda.'
            var ui =
                    UI.textArea("I am a text area, \nhow are you today :) ?")
                    .peek( it -> it.setLineWrap(true) )
                    .withStyle( it -> it
                        .size(185, 60)
                        .font("Buggie", 13)
                        .fontBold(true)
                        .fontItalic(true)
                        .fontColor("#FF0000") // Red
                        .fontSpacing(0.077)
                    )

        when : 'We render the text area into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/custom-font-JTextArea.png", 99.95) > 99.95
    }

    def 'For full styling freedom, we can add custom painters to a component on various layers.'(
        float uiScale
    ) {
        reportInfo """
            If you want to have full control over how a component is painted,
            you can add custom painters to a component on various layers. <br>
            <br>
            Here you can see an example of a panel with a custom painter on the background layer. <br>
            ${Utility.linkSnapshot('components/custom-painter-JLabel.png')}

            This little example demonstrates very nicely how the painters are layered on top of each other
            and at which layer the text of the component is painted over by your custom painters.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'A label UI with a custom styler lambda.'
            var ui =
                    UI.label("I am a label")
                    .withStyle( it -> it
                        .fontSize(12)
                        .size(120, 50)
                        .padding(6)
                        .margin(10)
                        .painter(UI.Layer.BACKGROUND, g -> {
                            g.setColor(Color.RED);
                            g.fill(UI.scale(new RoundRectangle2D.Double(10,15,20,20,5,5)));
                        })
                        .painter(UI.Layer.CONTENT, g -> {
                            g.setColor(Color.ORANGE);
                            g.fill(UI.scale(new RoundRectangle2D.Double(25,15,20,20,5,5)));
                        })
                        .painter(UI.Layer.BORDER, g -> {
                            g.setColor(Color.BLUE);
                            g.fill(UI.scale(new RoundRectangle2D.Double(40,15,20,20,5,5)));
                        })
                        .painter(UI.Layer.FOREGROUND, g -> {
                            g.setColor(Color.MAGENTA);
                            g.fill(UI.scale(new RoundRectangle2D.Double(55,15,20,20,5,5)));
                        })
                    )

        when : 'We render the label into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/custom-painter-JLabel.png", 99.5) > 99.5

        where :
            uiScale << [1, 2, 3]
    }

    def 'The cursor of any component can be configured through the style API.'(
        JComponent component, UI.Cursor cursor
    ) {
        reportInfo """
            The cursor of any component can be configured through the style API
            exposed by the `withStyle` method. 
        """
        given : 'We create a SwingTree builder node using the `of`method and apply the cursor in the style API.'
            var ui = UI.of(component).withStyle( it -> it.cursor(cursor) )

        expect : 'The component indeed has the specified cursor!'
            ui.component.cursor == cursor.toAWTCursor()

        where :
            component        | cursor
            new JButton()    | UI.Cursor.HAND
            new JTextArea()  | UI.Cursor.CROSS
            new JTextField() | UI.Cursor.DEFAULT
            new JBox()       | UI.Cursor.RESIZE_EAST
            new JSlider()    | UI.Cursor.RESIZE_NORTH
            new JSpinner()   | UI.Cursor.RESIZE_NORTH_EAST
    }

    def 'The background color of any component can be configured through the style API.'(
        JComponent component, Color color
    ) {
        reportInfo """
            The background color of any component can be configured through the style API
            exposed by the `withStyle` method. 
        """
        given : 'We create a SwingTree builder node using the `of`method and apply the background color in the style API.'
            var ui = UI.of(component).withStyle( it -> it.backgroundColor(color) )

        expect : 'The component indeed has the specified background color!'
            ui.component.background == color

        where :
            component        | color
            new JButton()    | Color.RED
            new JTextArea()  | Color.GREEN
            new JTextField() | Color.BLUE
            new JBox()       | Color.YELLOW
            new JSlider()    | Color.CYAN
            new JSpinner()   | Color.MAGENTA
    }

    def 'The foreground color of any component can be configured through the style API.'(
        JComponent component, Color color
    ) {
        reportInfo """
            The foreground color of any component can be configured through the style API
            exposed by the `withStyle` method. 
        """
        given : 'We create a SwingTree builder node using the `of`method and apply the foreground color in the style API.'
            var ui = UI.of(component).withStyle( it -> it.foregroundColor(color) )

        expect : 'The component indeed has the specified foreground color!'
            ui.component.foreground == color

        where :
            component        | color
            new JButton()    | Color.RED
            new JTextArea()  | Color.GREEN
            new JTextField() | Color.BLUE
            new JBox()       | Color.YELLOW
            new JSlider()    | Color.CYAN
            new JSpinner()   | Color.MAGENTA
    }

    def 'The horizontal text alignment of many text based components can be configured through the style API.'(
        JComponent component, UI.HorizontalAlignment alignment
    ) {
        reportInfo """
            The horizontal text alignment of many text based components can be configured through the style API
            exposed by the `withStyle` method. 
        """
        given : 'We create a SwingTree builder node using the `of`method and apply the horizontal text alignment in the style API.'
            var ui = UI.of(component).withStyle( it -> it.fontAlignment(alignment) )

        expect : 'The component indeed has the specified horizontal text alignment!'
            ui.component.horizontalAlignment == alignment.forSwing()

        where :
            component               | alignment
            new JButton()           | UI.HorizontalAlignment.LEFT
            new JTextField()        | UI.HorizontalAlignment.RIGHT
            new JPasswordField()    | UI.HorizontalAlignment.CENTER
            new JMenuItem()         | UI.HorizontalAlignment.LEADING
            new JCheckBoxMenuItem() | UI.HorizontalAlignment.TRAILING
    }

    def 'Paint images as component background through the style API.'()
    {

        reportInfo """
                Inside your `Styler` lambdas you may access another sub style
                for configuring the grounding of a component, which
                is a sort of background for every style layer.
                <br>
                Here you can see an example of a panel with a background image.
                
                ${Utility.linkSnapshot('components/image-panels-collage.png')}
            """

        given : 'We create various different UIs with different grounding styles.'
            var img = Utility.loadImage("img/swing.png")
            var ui1 =
                        UI.label("Top Left").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.TOP_LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
           var ui2 =
                        UI.label("Top Right").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.TOP_RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui3 =
                        UI.label("Bottom Left").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.BOTTOM_LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui4 =
                        UI.label("Bottom Right").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.BOTTOM_RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui5 =
                        UI.label("Center").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.CENTER)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui6 =
                        UI.label("Top").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.TOP)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui7 =
                        UI.label("Bottom").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.BOTTOM)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui8 =
                        UI.label("Left").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui9 =
                        UI.label("Right").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui10 =
                        UI.label("Stretch").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img).autoFit(true)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui11 =
                        UI.label("Only Color").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .primer(new Color(200,240,230, 200))
                            )
                        )
            var ui12 =
                        UI.label("Only Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                            )
                        )


        when : 'We render the UIs into BufferedImage instances.'
            var image1 = Utility.renderSingleComponent(ui1.getComponent())
            var image2 = Utility.renderSingleComponent(ui2.getComponent())
            var image3 = Utility.renderSingleComponent(ui3.getComponent())
            var image4 = Utility.renderSingleComponent(ui4.getComponent())
            var image5 = Utility.renderSingleComponent(ui5.getComponent())
            var image6 = Utility.renderSingleComponent(ui6.getComponent())
            var image7 = Utility.renderSingleComponent(ui7.getComponent())
            var image8 = Utility.renderSingleComponent(ui8.getComponent())
            var image9 = Utility.renderSingleComponent(ui9.getComponent())
            var image10 = Utility.renderSingleComponent(ui10.getComponent())
            var image11 = Utility.renderSingleComponent(ui11.getComponent())
            var image12 = Utility.renderSingleComponent(ui12.getComponent())
            var images = new BufferedImage[] {image1, image2, image3, image4, image5, image6, image7, image8, image9, image10, image11, image12}


        then : 'The image is as expected.'
            Utility.similarityBetween(images, "components/image-panels-collage.png", 99) > 99

        where : 'We test this UI using the following scaling values:'
            scale << [1f, 1.25f, 1.75f, 2f]
    }


    def 'Paint SVG based images as a component background through the style API.'()
    {
        reportInfo """
                Inside your `Styler` lambdas you may access another sub style
                for configuring the grounding of a component, which
                is a sort of background for every style layer.
                <br>
                Here you can see an example of a panel with an SVG based background image
                rendered according to the specified placement policies, dimesnions and opacity.
                
                ${Utility.linkSnapshot('components/svg-image-panels-collage.png')}
            """

        given : 'We create various different UIs with different grounding styles.'
            var img = UI.findIcon("img/hopper.svg").get()
            var ui1 =
                        UI.label("Top Left").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.TOP_LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
           var ui2 =
                        UI.label("Top Right").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.GREEN)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                                .placement(UI.Placement.TOP_RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
           var ui3 =
                       UI.label("Bottom Left").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                               .placement(UI.Placement.BOTTOM_LEFT)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui4 =
                       UI.label("Bottom Right").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                               .placement(UI.Placement.BOTTOM_RIGHT)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui5 =
                       UI.label("Center").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                               .placement(UI.Placement.CENTER)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui6 =
                       UI.label("Top").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                               .placement(UI.Placement.TOP)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui7 =
                       UI.label("Bottom").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                               .placement(UI.Placement.BOTTOM)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui8 =
                       UI.label("Left").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                               .placement(UI.Placement.LEFT)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui9 =
                       UI.label("Right").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                               .placement(UI.Placement.RIGHT)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui10 =
                       UI.label("Stretch").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img).autoFit(true)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                           )
                       )
           var ui11 =
                       UI.label("Only Color").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .primer(new Color(200,240,230, 200))
                           )
                       )
           var ui12 =
                       UI.label("Only Image").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .size(60, 60)
                           )
                       )
           var ui13 =
                       UI.label("Center X Stretch").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .height(60)
                               .placement(UI.Placement.CENTER)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                               .fitMode(UI.FitComponent.WIDTH)
                           )
                       )
           var ui14 =
                       UI.label("Center Y Stretch").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .width(60)
                               .placement(UI.Placement.CENTER)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                               .fitMode(UI.FitComponent.HEIGHT)
                           )
                       )
           var ui15 =
                       UI.label("Center XY Stretch").withStyle( it -> it
                           .fontAlignment(UI.HorizontalAlignment.CENTER)
                           .border(2, Color.GREEN)
                           .size(120, 120)
                           .image(ground -> ground
                               .image(img)
                               .placement(UI.Placement.CENTER)
                               .opacity(0.5f)
                               .primer(new Color(100,200,230, 100))
                               .fitMode(UI.FitComponent.WIDTH_AND_HEIGHT)
                           )
                       )


        when : 'We render the UIs into BufferedImage instances.'
            var image1 = Utility.renderSingleComponent(ui1.getComponent())
            var image2 = Utility.renderSingleComponent(ui2.getComponent())
            var image3 = Utility.renderSingleComponent(ui3.getComponent())
            var image4 = Utility.renderSingleComponent(ui4.getComponent())
            var image5 = Utility.renderSingleComponent(ui5.getComponent())
            var image6 = Utility.renderSingleComponent(ui6.getComponent())
            var image7 = Utility.renderSingleComponent(ui7.getComponent())
            var image8 = Utility.renderSingleComponent(ui8.getComponent())
            var image9 = Utility.renderSingleComponent(ui9.getComponent())
            var image10 = Utility.renderSingleComponent(ui10.getComponent())
            var image11 = Utility.renderSingleComponent(ui11.getComponent())
            var image12 = Utility.renderSingleComponent(ui12.getComponent())
            var image13 = Utility.renderSingleComponent(ui13.getComponent())
            var image14 = Utility.renderSingleComponent(ui14.getComponent())
            var image15 = Utility.renderSingleComponent(ui15.getComponent())
            var images = new BufferedImage[] {image1, image2, image3, image4, image5, image6, image7, image8, image9, image10, image11, image12, image13, image14, image15}

        then : 'The image is as expected.'
            Utility.similarityBetween(images, "components/svg-image-panels-collage.png", 99.5) > 99.5

        where : 'We test this UI using the following scaling values:'
            scale << [1f, 1.25f, 1.75f, 2f]
    }


    def 'Paint automatically stretched images as component background through the style API.'()
    {

        reportInfo """
                Inside your `Styler` lambdas you may access another sub style
                for configuring the image style of a component, which
                is a image background that may be configured for every style layer.
                These images styles allows for rendering images as background
                for the component which may be stretched to fit the component size, 
                which is demonstrated in this example.
                <br>
                Note that a image dimension will only be stretched to fir the component
                if no size was specified for a particular dimension (width or height).
                Also note that here we actually configure 2 images for every component
                which will both be rendered on top of the component.
                Here you can see an example of a panel with a background image.
                
                ${Utility.linkSnapshot('components/stretched-image-panels-collage.png')}
            """

        given : 'We create various different UIs with different grounding styles.'
            var img = Utility.loadImage("img/trees.png")
            var ui1 =
                        UI.label("Top Left").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.TOP_LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.TOP_LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui2 =
                        UI.label("Top Right").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1",  ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.TOP_RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2",  ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.TOP_RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui3 =
                        UI.label("Bottom Left").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.BOTTOM_LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.BOTTOM_LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui4 =
                        UI.label("Bottom Right").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.BOTTOM_RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.BOTTOM_RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui5 =
                        UI.label("Center").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.CENTER)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.CENTER)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui6 =
                        UI.label("Top").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.TOP)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.TOP)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )

                        )
            var ui7 =
                        UI.label("Bottom").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.BOTTOM)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.BOTTOM)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui8 =
                        UI.label("Left").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.LEFT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui9 =
                        UI.label("Right").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .placement(UI.Placement.RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .placement(UI.Placement.RIGHT)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui10 =
                        UI.label("Stretch").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image("Image 1", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .width(40)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                            .image("Image 2", ground -> ground
                                .image(img)
                                .autoFit(true)
                                .height(40)
                                .opacity(0.5f)
                                .primer(new Color(100,200,230, 100))
                            )
                        )
            var ui11 =
                        UI.label("Only Color").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image(ground -> ground
                                .primer(new Color(200,240,230, 200))
                            )
                        )
            var ui12 =
                        UI.label("Only Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLUE)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(60, 60)
                            )
                        )


        when : 'We paint the UIs into BufferedImage instances.'
            var image1  = Utility.renderSingleComponent(ui1.getComponent())
            var image2  = Utility.renderSingleComponent(ui2.getComponent())
            var image3  = Utility.renderSingleComponent(ui3.getComponent())
            var image4  = Utility.renderSingleComponent(ui4.getComponent())
            var image5  = Utility.renderSingleComponent(ui5.getComponent())
            var image6  = Utility.renderSingleComponent(ui6.getComponent())
            var image7  = Utility.renderSingleComponent(ui7.getComponent())
            var image8  = Utility.renderSingleComponent(ui8.getComponent())
            var image9  = Utility.renderSingleComponent(ui9.getComponent())
            var image10 = Utility.renderSingleComponent(ui10.getComponent())
            var image11 = Utility.renderSingleComponent(ui11.getComponent())
            var image12 = Utility.renderSingleComponent(ui12.getComponent())
            var images = new BufferedImage[] {image1, image2, image3, image4, image5, image6, image7, image8, image9, image10, image11, image12}


        then : 'The image is as expected.'
            Utility.similarityBetween(images, "components/stretched-image-panels-collage.png", 98) > 98
    }

    def 'A single image can be painted repeatedly in a panel.'()
    {
        reportInfo """
            The image sub-style can be used to paint a single image repeatedly in a panel
            by setting the `repeat` flag to true.
            Here you can see various examples of this
            where the image is painted in the center, top, bottom, left, right and stretched.
        """
        given : 'A UI with a single image painted repeatedly in a panel.'
            var img = Utility.loadImage("img/trees.png")
            var ui1 =
                        UI.label("Repeated Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLACK).borderRadius(10)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(30, 30)
                                .repeat(true)
                            )
                        )
            var ui2 =
                        UI.label("Repeated Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLACK).borderRadius(10)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .size(30, 30)
                                .repeat(true)
                                .placement(UI.Placement.TOP_LEFT)
                            )
                        )
            var ui3 =
                        UI.label("Repeated Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLACK).borderRadius(10)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img).autoFit(true)
                                .width(40)
                                .repeat(true)
                            )
                        )
            var ui4 =
                        UI.label("Repeated Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLACK).borderRadius(10)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img).autoFit(true)
                                .height(40)
                                .repeat(true)
                                .placement(UI.Placement.TOP_RIGHT)
                            )
                        )
            var ui5 =
                        UI.label("Repeated Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLACK).borderRadius(10)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .width(40)
                                .repeat(true)
                                .placement(UI.Placement.RIGHT)
                            )
                        )
            var ui6 =
                        UI.label("Repeated Image").withStyle( it -> it
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                            .border(2, Color.BLACK).borderRadius(10)
                            .size(120, 120)
                            .image(ground -> ground
                                .image(img)
                                .height(40)
                                .repeat(true)
                                .placement(UI.Placement.LEFT)
                            )
                        )


        when : 'We paint the UIs into a BufferedImage instance.'
            var image1 = Utility.renderSingleComponent(ui1.getComponent())
            var image2 = Utility.renderSingleComponent(ui2.getComponent())
            var image3 = Utility.renderSingleComponent(ui3.getComponent())
            var image4 = Utility.renderSingleComponent(ui4.getComponent())
            var image5 = Utility.renderSingleComponent(ui5.getComponent())
            var image6 = Utility.renderSingleComponent(ui6.getComponent())
            var images = new BufferedImage[] {image1, image2, image3, image4, image5, image6}

        then : 'The image is as expected.'
            Utility.similarityBetween(images, "components/repeated-image-panel.png", 99.95) > 99.95
    }

    def 'Create fancy text fields with custom icons and a button.'(
        float uiScale
    ) {
        reportInfo """
            Creating heavily customized components in a way which prefers composition over inheritance
            is one of the main goals of SwingTree and this little example demonstrates this very nicely.
            <br>
            Here you can see an example of a text field with a custom icon and a button:
            ${Utility.linkSnapshot('components/heavily-customized-text-field.png')}

            As you can see, the resulting text field looks nothing like the default text field
            and we did not need to extend any Swing class to achieve this.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'Now we create a text field UI with a custom styler lambda and a button.'
            var seed = Utility.loadImage("img/seed.png")
            var trees = Utility.loadImage("img/trees.png")
            var ui =
                    UI.textField("I am fancy! :)").withLayout("fill, ins 0").withPrefSize(190, 25)
                    .withStyle( it -> it
                        .fontSize(16)
                        .padding(0, 0, 0, 26)
                        .marginRight(25)
                        .paddingRight(-20)
                        .image(image -> image
                            .layer(UI.Layer.BORDER)
                            .image(seed)
                            .placement(UI.Placement.LEFT)
                            .width(30).autoFit(true)
                            .padding(3)
                        )
                    )
                    .add("right",
                        UI.button(19, 19, new ImageIcon(trees))
                        .withStyle( it -> it
                            .margin(0)
                            .cursor(UI.Cursor.HAND)
                            .painter(UI.Layer.BACKGROUND, g2d -> {
                                boolean isHovered = it.component().getModel().isRollover();
                                boolean isPressed = it.component().getModel().isPressed();
                                if ( isPressed ) {
                                    g2d.setColor(new Color(0,100,200));
                                    g2d.fillRoundRect(0, 0, it.component().getWidth(), it.component().getHeight(), 5, 5);
                                }
                                else if ( isHovered ) {
                                    g2d.setColor(new Color(120,220,100));
                                    g2d.fillRoundRect(0, 0, it.component().getWidth(), it.component().getHeight(), 5, 5);
                                }
                            })
                        )
                        .makePlain()
                    );

        expect : 'The image is as expected.'
            Utility.similarityBetween(ui.getComponent(), "components/heavily-customized-text-field.png", 99.5) > 99.5

        where :
            uiScale << [3]
    }

    def 'Create a button with a SVG icon based toggle mode.'(
        float uiScale
    ) {
        reportInfo """
            Creating heavily customized components in a way which prefers composition over inheritance
            is one of the main goals of SwingTree and this little example demonstrates this very 
            nicely using 2 buttons, a regular button and a toggle button nested inside.
            <br>
            Here you can see an example of a text field with a custom icon and a button:
            ${Utility.linkSnapshot('components/nested-buttons.png')}

            As you can see, the resulting button looks nothing like the default button
            and we did not need to extend any Swing class to achieve this.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and : 'Now we create a button UI with a custom styler lambda and a button.'
            var hopper = UI.findIcon("img/hopper.svg")
            var ui =
                    UI.button("Click").withLayout("fill, ins 0")
                    .withPrefSize(160, 80)
                    .withSize(160, 80)
                    .withStyle( it -> it
                        .fontSize(16)
                        .fontAlignment(UI.HorizontalAlignment.LEFT)
                        .paddingLeft(10)
                        .margin(15)
                        .border(3, "orange")
                    )
                    .add("right",
                        UI.toggleButton(hopper.get())
                        .withStyle( it -> it
                            .prefWidth((int)it.parent().map(Container::getSize).map(d -> d.height).orElse(80)-40)
                            .prefHeight((int)it.parent().map(Container::getSize).map(d -> d.height).orElse(80)-40)
                            .margin(5)
                            .padding(5)
                            .cursor(UI.Cursor.HAND)
                            .border(2, "gray")
                            .borderRadius(15)
                        )
                    );

        expect : 'The image is as expected.'
            Utility.similarityBetween(ui.getComponent(), "components/nested-buttons.png", 99.5) > 99.5

        where :
            uiScale << [1]
    }

    def 'Use the style API to design custom tabbed panes from scratch.'(
        float uiScale
    ) {
        reportInfo """
            Creating heavily customized components in a way which prefers composition over inheritance
            is one of the main goals of SwingTree and this little example demonstrates 
            how this principle is also applicable for
            more sophisticated components like tabbed panes.
            <br>
            Here you can see an example of a custom SwingTree based tabbed pane:
            ${Utility.linkSnapshot('components/my-tabbed-pane.png')}

            As you can see, the resulting tabbed pane looks nothing like the default tabbed pane
            and we did not need to extend any Swing class to achieve this.
        """
        given : """
            We first set a scaling factor to simulate a platform with higher DPI.
            So when your screen has a higher pixel density then this factor
            is used by SwingTree to ensure that the UI is upscaled accordingly! 
            Please note that the line below only exists for testing purposes, 
            SwingTree will determine a suitable 
            scaling factor for the current system automatically for you,
            so you do not have to specify this factor manually. 
        """
            SwingTree.get().getUIScale().setUserScaleFactor(uiScale)
        and :
            MyTabsViewModel.TabModel tab1 = new MyTabsView.DummyTab("Tab 1", "").getModel();
            MyTabsViewModel.TabModel tab2 = new MyTabsView.DummyTab("Tab 2", "img/two-16th-notes.svg").getModel();
            MyTabsViewModel.TabModel tab3 = new MyTabsView.DummyTab("Tab 3", "img/hopper.svg").getModel();

            MyTabsViewModel vm = new MyTabsViewModel();
            vm.getTabs().add(tab1);
            vm.getTabs().add(tab2);
            vm.getTabs().add(tab3);

            vm.getCurrentTab().set(tab2);
            var ui = new MyTabsView(vm)
            ui.setPreferredSize(new Dimension(UI.scale(220), UI.scale(80)))

        expect : 'The image is as expected.'
            Utility.similarityBetween(ui, "components/my-tabbed-pane.png", 97.5) > 97.5

        where :
            uiScale << [1, 2, 3]
    }

    def 'You can use the style API to configure client properties for components.'()
    {
        reportInfo """
            The style API allows you to configure client properties for components.
            Client properties are a way to attach custom data to a component.
            Usually the purpose of client properties is to give a particular look and feel
            implementation a way to configure how a particular component should be painted.
        """
        given : 'We create a simple toggle button with a few abitrary properties.'
            var ui =
                    UI.toggleButton("Toggle Me").withStyle( it -> it
                        .property("my.custom.property.1", "Hello World!")
                        .property("my.custom.property.2", "42")
                        .property("my.custom.property.3", "true")
                    )
        expect : 'The component indeed has the specified client properties!'
            ui.component.getClientProperty("my.custom.property.1") == "Hello World!"
            ui.component.getClientProperty("my.custom.property.2") == "42"
            ui.component.getClientProperty("my.custom.property.3") == "true"
    }

}
