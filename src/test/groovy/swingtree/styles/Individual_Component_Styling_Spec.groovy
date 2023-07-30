package swingtree.styles

import com.formdev.flatlaf.FlatLightLaf
import net.miginfocom.swing.MigLayout
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title
import swingtree.SwingTreeContext
import swingtree.components.JBox
import swingtree.threading.EventProcessor
import swingtree.UI

import utility.Utility

import javax.swing.*
import javax.swing.border.TitledBorder
import java.awt.*
import java.awt.image.BufferedImage

@Title("Styling Components")
@Narrative('''
    This specification demonstrates how you can use the styling
    API to style Swing components in a functional and declarative fashion.
''')
class Individual_Component_Styling_Spec extends Specification
{
    def setupSpec() {
        SwingTreeContext.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
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

    def 'Styling components is based on a functional styler lambda.'()
    {
        reportInfo """
            Fun-Fact: 
            Styling in SwingTree is fully functional, which means 
            that the `Style` settings objects are all immutable. 
            They are not modified in place, but instead transformed
            by so called "styler" lambdas.
            Not only does this architecture make it easy to compose, reuse and share
            styles, but it also makes it possible to have a extensive style
            hierarchy without the need for very complex code.
            In practice, this means that your styler lambdas become part
            of a compositional tree of styler all the other lambdas, which is then applied to
            the components of the component tree in every repaint.
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
            panel.component.font.toString().contains("family=Dialog,name=Papyrus,style=plain,size=42")
    }

    def 'The margins defined in the style API will be applied to the layout manager through the border insets.'()
    {
        reportInfo """
            Swing does not have a concept of margins.
            Without a proper layout manager it does not even support the configuration of insets.
            However, SwingTree fixes this
            through a custom `Border` implementation and a default layout manager (`MigLayout`)
            which models the margins (and paddings) of a component.
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
            Utility.similarityBetween(image, "components/rounded-green-JLabel.png", 99.95) > 99.95
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
                            .align(UI.Transition.BOTTOM_RIGHT_TO_TOP_LEFT)
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
            Utility.similarityBetween(image, "components/shaded-border-JPanel.png", 99.95) > 99.95
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
                           .align(UI.Transition.TOP_LEFT_TO_BOTTOM_RIGHT)
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
                           .align(UI.Transition.LEFT_TO_RIGHT)
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

        when : 'We render the text area into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/sunken-JTextArea.png", 99.95) > 99.95
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
            Utility.similarityBetween(image, "components/rounded-metal-JButton.png", 99.95) > 99.95
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
            Utility.similarityBetween(image, "components/banner-JPanel.png", 99.95) > 99.95
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
                        .size(140, 80)
                        .font("Goudy Old Style", 13)
                        .fontBold(true)
                        .fontItalic(true)
                        .fontColor("#FF0000") // Red
                    )

        when : 'We render the text area into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/custom-font-JTextArea.png", 99.95) > 99.95
    }

    def 'For full styling freedom, we can add custom painters to a component on various layers.'()
    {
        reportInfo """
            If you want to have full control over how a component is painted,
            you can add custom painters to a component on various layers. <br>
            <br>
            Here you can see an example of a panel with a custom painter on the background layer. <br>
            ${Utility.linkSnapshot('components/custom-painter-JLabel.png')}

            This little example demonstrates very nicely how the painters are layered on top of each other
            and at which layer the text of the component is painted over by your custom painters.
        """
        given : 'A label UI with a custom styler lambda.'
            var ui =
                    UI.label("I am a label")
                    .withStyle( it -> it
                        .size(120, 50)
                        .padding(6)
                        .margin(10)
                        .painter(UI.Layer.BACKGROUND, g -> {
                            g.setColor(Color.RED);
                            g.fillRoundRect(10,15,20,20,5,5);
                        })
                        .painter(UI.Layer.CONTENT, g -> {
                             g.setColor(Color.ORANGE);
                             g.fillRoundRect(25,15,20,20,5,5);
                        })
                        .painter(UI.Layer.BORDER, g -> {
                             g.setColor(Color.BLUE);
                             g.fillRoundRect(40,15,20,20,5,5);
                        })
                        .painter(UI.Layer.FOREGROUND, g -> {
                             g.setColor(Color.MAGENTA);
                             g.fillRoundRect(55,15,20,20,5,5);
                        })

                    )

        when : 'We render the label into a BufferedImage.'
            var image = Utility.renderSingleComponent(ui.getComponent())

        then : 'The image is as expected.'
            Utility.similarityBetween(image, "components/custom-painter-JLabel.png", 99.95) > 99.95
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

}
