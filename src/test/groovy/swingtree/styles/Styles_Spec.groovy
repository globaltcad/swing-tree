package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.components.JBox
import swingtree.style.ComponentExtension
import swingtree.api.Layout
import swingtree.threading.EventProcessor
import swingtree.style.StyleConf
import swingtree.style.ComponentStyleDelegate

import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JToggleButton
import java.awt.*
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D

@Title("The Style Configuration")
@Narrative('''

    This specification demonstrates what kind of style configuration is
    created by various usage patterns of the style API.
    The style configuration defines how Swing components ought to be
    placed and rendered.

''')
@Subject([StyleConf])
class Styles_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def cleanupSpec() {
        SwingTree.initialize()
    }

    def 'Various kinds of String expressions can be parsed as colors by various style properties.'(
        String colorString, Color expectedColor
    ) {
        given : 'We use method chaining within the style API to build a colorful style:'
            var style =
                            ComponentExtension.from(
                                UI.of(new JComponent(){})
                                .withStyle(conf -> conf
                                    .foundationColor(colorString)
                                    .backgroundColor(colorString)
                                    .foregroundColor(colorString)
                                    .border(1, colorString)
                                    .shadowColor(colorString)
                                    .fontSelectionColor(colorString)
                                    .fontColor(colorString)
                                )
                                .get(JComponent)
                            )
                            .getStyle()
        and : 'We unpack everything:'
            var foundationColor = style.base().foundationColor()
            var backgroundColor = style.base().backgroundColor()
            var foregroundColor = style.base().foregroundColor()
            var borderColor = style.border().color()
            var shadowColor = style.shadow().color()
            var fontSelectionColor = style.font().selectionColor()
            var fontColor = style.font().paint()
        expect :
            backgroundColor.get() == expectedColor
            foregroundColor.get() == expectedColor
        and :
            !foundationColor.isPresent() && expectedColor == UI.Color.UNDEFINED || foundationColor.get() == expectedColor
            !borderColor.isPresent() && expectedColor == UI.Color.UNDEFINED || borderColor.get() == expectedColor
            !shadowColor.isPresent() && expectedColor == UI.Color.UNDEFINED || shadowColor.get() == expectedColor
            !fontSelectionColor.isPresent() && expectedColor == UI.Color.UNDEFINED || fontSelectionColor.get() == expectedColor
            !fontColor.isPresent() && expectedColor == UI.Color.UNDEFINED || fontColor.get() == expectedColor

        where :
            colorString                    | expectedColor
            "red"                          | Color.RED
            "#ff0000"                      | Color.RED
            "0xff0000"                     | Color.RED
            "rgb(255,0,0)"                 | Color.RED
            "rgba(1.0,0,0,1.0)"            | Color.RED
            "rgba(100%,0,0, 1.0)"          | Color.RED
            "rgba(255,0,0,255)"            | Color.RED
            "rgba(100%,0,0,100%)"          | Color.RED
            "hsb(0,100%,100%)"             | Color.RED
            "hsb(0,1.0,1f)"                | Color.RED
            "magenta"                      | Color.MAGENTA
            "#ff00ff"                      | Color.MAGENTA
            "0xff00ff"                     | Color.MAGENTA
            "rgb(255,0,255)"               | Color.MAGENTA
            "rgba(1.0,0,1.0,1.0)"          | Color.MAGENTA
            "rgba(100%,0,100%,1f)"         | Color.MAGENTA
            "rgba(255,0,255,255)"          | Color.MAGENTA
            "rgba(100%,0,100%,100%)"       | Color.MAGENTA
            "hsb(0.8333333,100%,100%)"     | Color.MAGENTA
            "hsb(300°,100%,100%)"          | Color.MAGENTA
            "rgb(255,22,0)"                | new Color(255, 22, 0)
            "rgba(255,22,0, 0.5)"          | new Color(255, 22, 0, 127)
            "hsb(0.014379084,1f,1f)"       | new Color(255, 22, 0)
            "hsba(0.014379084,1f,1f,0.5)"  | new Color(255, 22, 0, 127)
            "transparent"                  | new Color(0, 0, 0, 0)
            "yellow"                       | Color.YELLOW
            "black"                        | Color.BLACK
            "indigo"                       | new Color(75, 0, 130)
            "transparent purple"           | new Color(128, 0, 128, 127)
            "transparent red"              | new Color(255, 0, 0, 127)
            "transparent green"            | new Color(0, 128, 0, 127)
            "transparent blue"             | new Color(0, 0, 255, 127)
            "light indigo"                 | new Color(107, 0, 186)
            "dark navy"                    | new Color(0, 0, 90)
            ""                             | UI.Color.UNDEFINED
            "I make no sense!"             | UI.Color.UNDEFINED
            "I make no sense! at all!"     | UI.Color.UNDEFINED
            "Hold my b(0/&/§H%,1fu3s98s"   | UI.Color.UNDEFINED
    }

    def 'The String representation of a style config will tell you everything about it!'( float uiScale )
    {
        given :
            SwingTree.initialiseUsing { it.uiScaleFactor(uiScale) }
            var scale = { it * uiScale }
            var scaledToString = { String.valueOf(scale(it)).replace(".0", "") }
            var roundScaledToString = { String.valueOf(Math.round(scale(it))).replace(".0", "") }
        and : """
            We create a simple Swing component, a JSpinner, and
            send it through the SwingTree builder API where
            we apply a style to it by updating its style configuration.
        """
            var style =
                            ComponentExtension.from(
                                UI.of(new JSpinner()).withStyle(conf->conf
                                    .foundationColor("red")
                                    .backgroundColor("green")
                                    .borderColor("blue")
                                    .borderWidths(1, 2, 3, 4)
                                    .borderRadius(12, 18)
                                    .shadowColor("yellow")
                                    .fontSelectionColor("cyan")
                                    .fontColor("magenta")
                                    .fontFamily("Times New Roman")
                                    .fontSize(12)
                                    .fontBold(true)
                                    .fontUnderline(true)
                                    .fontStrikeThrough(true)
                                )
                                .get(JSpinner)
                            )
                            .getStyle()

        expect :
                style.toString() == "StyleConf[" +
                                        "LayoutConf[NONE], " +
                                        "BorderConf[" +
                                            "arcWidth=" + scaledToString(12) + ", " +
                                            "arcHeight=" + scaledToString(18) + ", " +
                                            "topWidth=" + scaledToString(1) + ", " +
                                            "rightWidth=" + scaledToString(2) + ", " +
                                            "bottomWidth=" + scaledToString(3) + ", " +
                                            "leftWidth=" + scaledToString(4) + ", " +
                                            "margin=Outline[" +
                                                "top=${(scale(1) % 1 == 0 ? "?" : 1 - scale(1) % 1 )}, " +
                                                "right=${(scale(2) % 1 == 0 ? "?" : 1 - scale(2) % 1 )}, " +
                                                "bottom=${(scale(3) % 1 == 0 ? "?" : 1 - scale(3) % 1 )}, " +
                                                "left=${(scale(4) % 1 == 0 ? "?" : 1 - scale(4) % 1 )}" +
                                            "], " +
                                            "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                                            "color=rgba(0,0,255,255)" +
                                        "], " +
                                        "BaseConf[" +
                                            "icon=?, " +
                                            "fitComponent=NO, " +
                                            "backgroundColor=rgba(0,128,0,255), " +
                                            "foundationColor=rgba(255,0,0,255), " +
                                            "foregroundColor=?, " +
                                            "cursor=?, " +
                                            "orientation=UNKNOWN" +
                                        "], " +
                                        "FontConf[" +
                                            "family=Times New Roman, " +
                                            "size=" + Math.round(12*uiScale) + ", " +
                                            "posture=0.0, " +
                                            "weight=2.0, " +
                                            "spacing=0.0, " +
                                            "underlined=true, " +
                                            "strikeThrough=true, " +
                                            "selectionColor=rgba(0,255,255,255), " +
                                            "transform=?, " +
                                            "paint=FontPaintConf[rgba(255,0,255,255)], " +
                                            "backgroundPaint=FontPaintConf[NONE], " +
                                            "horizontalAlignment=?, " +
                                            "verticalAlignment=?" +
                                        "], " +
                                        "DimensionalityConf[NONE], " +
                                        "StyleConfLayers[" +
                                            "filter=FilterConf[NONE], " +
                                            "background=StyleConfLayer[EMPTY], " +
                                            "content=StyleConfLayer[" +
                                                "shadows=ShadowConf[" +
                                                    "horizontalOffset=0, " +
                                                    "verticalOffset=0, " +
                                                    "blurRadius=0, " +
                                                    "spreadRadius=0, " +
                                                    "color=rgba(255,255,0,255), " +
                                                    "isInset=false" +
                                                "], " +
                                                "painters=PainterConf[NONE], " +
                                                "gradients=GradientConf[NONE], " +
                                                "noises=NoiseConf[NONE], " +
                                                "images=ImageConf[NONE], " +
                                                "texts=TextConf[NONE]" +
                                            "], " +
                                            "border=StyleConfLayer[EMPTY], " +
                                            "foreground=StyleConfLayer[EMPTY]" +
                                        "], " +
                                        "properties=[]" +
                                    "]"

        when : 'We create another style with some other properties:'
            var paint1 = new GradientPaint(0, 0, Color.RED, 100, 100, Color.BLUE)
            var paint2 = new GradientPaint(0, 0, Color.BLACK, 100, 100, Color.GREEN)
            var transform = AffineTransform.getRotateInstance(0.5)
            style = ComponentExtension.from(
                                UI.of(new JSpinner()).withStyle(conf->conf
                                    .fontAlignment(UI.Alignment.CENTER)
                                    .fontBackgroundColor("cyan")
                                    .fontBackgroundColor(new Color(0, 42, 42, 42))
                                    .fontBackgroundPaint(paint1)
                                    .fontPaint(paint2)
                                    .fontTransform(transform)
                                    .image(UI.Layer.FOREGROUND, "bubbles", imgConf -> imgConf
                                        .fitMode(UI.FitComponent.WIDTH)
                                        .repeat(true)
                                        .image("/img/bubble-tree.svg")
                                    )
                                )
                                .get(JSpinner)
                            )
                            .getStyle()

        then : 'The style config has the expected string representation.'
                style.toString() == "StyleConf[" +
                                        "LayoutConf[NONE], " +
                                        "BorderConf[NONE], " +
                                        "BaseConf[NONE], " +
                                        "FontConf[" +
                                            "family=, " +
                                            "size=0, " +
                                            "posture=0.0, " +
                                            "weight=0.0, " +
                                            "spacing=0.0, " +
                                            "underlined=?, " +
                                            "strikeThrough=?, " +
                                            "selectionColor=?, " +
                                            "transform=$transform, " +
                                            "paint=FontPaintConf[$paint2], " +
                                            "backgroundPaint=FontPaintConf[$paint1], " +
                                            "horizontalAlignment=CENTER, " +
                                            "verticalAlignment=CENTER" +
                                        "], " +
                                        "DimensionalityConf[NONE], " +
                                        "StyleConfLayers[" +
                                            "filter=FilterConf[NONE], " +
                                            "background=StyleConfLayer[EMPTY], " +
                                            "content=StyleConfLayer[EMPTY], " +
                                            "border=StyleConfLayer[EMPTY], " +
                                            "foreground=StyleConfLayer[" +
                                                "shadows=ShadowConf[NONE], " +
                                                "painters=PainterConf[NONE], " +
                                                "gradients=GradientConf[NONE], " +
                                                "noises=NoiseConf[NONE], " +
                                                "images=NamedConfigs[" +
                                                    "default=ImageConf[NONE], " +
                                                    "bubbles=ImageConf[" +
                                                        "primer=?, " +
                                                        "image=${style.images(UI.Layer.FOREGROUND).get(0).image().get()}, " +
                                                        "placement=UNDEFINED, " +
                                                        "repeat=true, " +
                                                        "fitComponent=WIDTH, " +
                                                        "width=?, height=?, " +
                                                        "opacity=1.0, " +
                                                        "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                                                        "offset=Offset[x=0, y=0], " +
                                                        "clipArea=BODY" +
                                                    "]" +
                                                "], " +
                                                "texts=TextConf[NONE]" +
                                            "]" +
                                        "], " +
                                        "properties=[]" +
                                    "]"

        when : 'We create another style with some other properties:'
            style = ComponentExtension.from(
                                UI.of(new JPanel()).withStyle(conf->conf
                                    .layout(Layout.border(2, 4))
                                    .size(100, 200)
                                    .minSize(50, 100)
                                    .maxHeight(300)
                                    .prefWidth(400)
                                    .gradient(UI.Layer.BORDER, "gradient", imgConf -> imgConf
                                        .colors(Color.RED, Color.GREEN, Color.BLUE)
                                        .span(UI.Span.BOTTOM_TO_TOP)
                                        .type(UI.GradientType.RADIAL)
                                    )
                                )
                                .get(JPanel)
                            )
                            .getStyle()

        then :
                style.toString() == "StyleConf[" +
                                        "LayoutConf[" +
                                            "layout=BorderLayoutInstaller[hgap=2, vgap=4], " +
                                            "constraint=?, " +
                                            "alignmentX=?, " +
                                            "alignmentY=?" +
                                        "], " +
                                        "BorderConf[NONE], " +
                                        "BaseConf[NONE], " +
                                        "FontConf[NONE], " +
                                        "DimensionalityConf[" +
                                            "minWidth=" + roundScaledToString(50) + ", " +
                                            "minHeight=" + roundScaledToString(100) + ", " +
                                            "maxWidth=" + roundScaledToString(300) + ", " +
                                            "maxHeight=?, " +
                                            "preferredWidth=" + roundScaledToString(400) + ", " +
                                            "preferredHeight=?, " +
                                            "width=" + roundScaledToString(100) + ", " +
                                            "height=" + roundScaledToString(200) +
                                        "], " +
                                        "StyleConfLayers[" +
                                            "filter=FilterConf[NONE], " +
                                            "background=StyleConfLayer[EMPTY], " +
                                            "content=StyleConfLayer[EMPTY], " +
                                            "border=StyleConfLayer[" +
                                                "shadows=ShadowConf[NONE], " +
                                                "painters=PainterConf[NONE], " +
                                                "gradients=NamedConfigs[" +
                                                    "default=GradientConf[NONE], " +
                                                    "gradient=GradientConf[" +
                                                        "transition=BOTTOM_TO_TOP, " +
                                                        "type=RADIAL, " +
                                                        "colors=[java.awt.Color[r=255,g=0,b=0], " +
                                                        "java.awt.Color[r=0,g=255,b=0], " +
                                                        "java.awt.Color[r=0,g=0,b=255]], " +
                                                        "offset=Offset[x=0, y=0], " +
                                                        "size=-1.0, " +
                                                        "area=BODY, " +
                                                        "boundary=EXTERIOR_TO_BORDER, " +
                                                        "focus=Offset[x=0, y=0], " +
                                                        "rotation=0.0, " +
                                                        "fractions=[], " +
                                                        "cycle=NONE" +
                                                    "]" +
                                                "], " +
                                                "noises=NoiseConf[NONE], " +
                                                "images=ImageConf[NONE], " +
                                                "texts=TextConf[NONE]" +
                                            "], " +
                                            "foreground=StyleConfLayer[EMPTY]" +
                                        "], " +
                                        "properties=[]" +
                                    "]"
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f, 2.25f, 3.0f ]
    }

    def 'Style objects are value based (with respect to equality and hash code).'()
    {
        reportInfo """
            Style objects are value based, meaning that if two style objects
            have the same properties, then they are considered equal.
            So their identity is considered to be their value.
            This is important for caching purposes, as the style objects are
            used as cache keys.
        """
        given : """
            First we need the component-style delegate styler, 
            which can update the style properties of the style configuration.
        """
            var styler = s -> new ComponentStyleDelegate<>(new JPanel(), s)

        and : 'Then we create a starting style with various properties:'
            var style1 =
                            ComponentExtension.from(
                                UI.box().withStyle(conf->conf
                                    .foundationColor("red")
                                    .backgroundColor("green")
                                    .borderColor("blue")
                                    .borderRadius(12, 18)
                                    .shadowColor("yellow")
                                    .fontSelectionColor("cyan")
                                    .fontColor("magenta")
                                    .fontFamily("Times New Roman")
                                    .fontSize(12)
                                    .fontBold(true)
                                    .fontUnderline(true)
                                    .fontStrikeThrough(true)
                                    .shadowSpreadRadius(12)
                                    .shadowBlurRadius(42)
                                )
                                .get(JBox)
                            )
                            .getStyle()

        and : 'We then create a second style with the same properties:'
            var style2 =
                            ComponentExtension.from(
                                UI.box().withStyle(conf->conf
                                    .foundationColor("red")
                                    .backgroundColor("green")
                                    .borderColor("blue")
                                    .borderRadius(12, 18)
                                    .shadowColor("yellow")
                                    .fontSelectionColor("cyan")
                                    .fontColor("magenta")
                                    .fontFamily("Times New Roman")
                                    .fontSize(12)
                                    .fontBold(true)
                                    .fontUnderline(true)
                                    .fontStrikeThrough(true)
                                    .shadowSpreadRadius(12)
                                    .shadowBlurRadius(42)
                                )
                                .get(JBox)
                            )
                            .getStyle()
        expect :
                style1 == style2
                style1.hashCode() == style2.hashCode()
        and : 'Changing a property and then comparing the styles should return false:'
                style1 != styler(style2).fontBold(false).style()
                style1.hashCode() != styler(style2).fontBold(false).style().hashCode()
                style1 != styler(style2).shadowSpreadRadius(1).style()
                style1.hashCode() != styler(style2).shadowSpreadRadius(1).style().hashCode()
                style1 != styler(style2).shadowBlurRadius(1).style()
                style1.hashCode() != styler(style2).shadowBlurRadius(1).style().hashCode()
        and : 'If we transform them both the same way then they will be equal:'
                styler(style2).fontBold(false).style()                   == styler(style2).fontBold(false).style()
                styler(style2).fontBold(false).style().hashCode()        == styler(style2).fontBold(false).style().hashCode()
                styler(style2).shadowSpreadRadius(1).style()            == styler(style2).shadowSpreadRadius(1).style()
                styler(style2).shadowSpreadRadius(1).style().hashCode() == styler(style2).shadowSpreadRadius(1).style().hashCode()
                styler(style2).shadowBlurRadius(1).style()              == styler(style2).shadowBlurRadius(1).style()
                styler(style2).shadowBlurRadius(1).style().hashCode()   == styler(style2).shadowBlurRadius(1).style().hashCode()
    }

    def 'SwingTree will simplify the style configuration of a component if possible.'( float uiScale )
    {
        reportInfo """
            Simplifying a style configuration means that if the properties of a style
            a configured in such a way that they are effectively the same as the default
            in terms of how they will be rendered, then the style configuration will be
            simplified to the default style.
            
            This optimization is important to improve cache hit rates, as the immutable
            style configuration objects are used as cache keys.
            Simplifying the style configurations will ensure that style configurations
            which effectively render the same thing will also share the same cache buffer
            for rendering.
            
            It is also memory efficient, as the default style objects are global null objects.
        """
        given :
            SwingTree.initialiseUsing { it.uiScaleFactor(uiScale) }
            var scale = { it * uiScale }
            var scaledToString = { String.valueOf(scale(it)).replace(".0", "") }
            var roundScaledToString = { String.valueOf(Math.round(scale(it))).replace(".0", "") }
        and : 'We create a highly simplifiable style through the style API.'
            var button =
                        UI.panel().withStyle(conf -> conf
                            .borderColor(new Color(0,0,0,0))
                            .borderRadius(0)
                            .borderWidths(0,0,0,0)
                            .shadowColor(new Color(50,150,200,0))
                            .shadowBlurRadius(4)
                            .shadowSpreadRadius(2)
                            .image(UI.Layer.CONTENT, i -> i
                                .primer(new Color(100,100,100,0))
                                .size(100, 100)
                                .fitMode(UI.FitComponent.WIDTH)
                                .image("/images/bubbles.svg")
                                .opacity(0.0f) // You can't see me!
                            )
                            .gradient(UI.Layer.BACKGROUND, g -> g
                                .colors(new Color(100, 50, 200, 0), new Color(255, 00, 250, 0))
                                .span(UI.Span.TOP_TO_BOTTOM)
                                .type(UI.GradientType.LINEAR)
                            )
                        )
                        .get(JPanel)
        expect : 'The style config has the expected string representation.'
            ComponentExtension.from(button).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[NONE], " +
                    "BaseConf[NONE], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[NONE], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"

        when : 'We create a not so simplifiable style for a component through the style API...'
            button = UI.button("Hello World").withStyle(conf -> conf
                         .backgroundColor(Color.BLACK)
                         .foregroundColor(Color.WHITE)
                         .size(120, 80)
                         .borderRadius(40)
                     )
                     .get(JButton)
        then : 'The style config has the expected string representation.'
            ComponentExtension.from(button).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[" +
                        "radius=" + scaledToString(40) + ", " +
                        "width=?, " +
                        "margin=Outline[top=?, right=?, bottom=?, left=?], " +
                        "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                        "color=?" +
                    "], " +
                    "BaseConf[" +
                        "icon=?, " +
                        "fitComponent=NO, " +
                        "backgroundColor=rgba(0,0,0,255), " +
                        "foundationColor=?, " +
                        "foregroundColor=rgba(255,255,255,255), " +
                        "cursor=?, " +
                        "orientation=UNKNOWN" +
                    "], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[" +
                        "minWidth=?, " +
                        "minHeight=?, " +
                        "maxWidth=?, " +
                        "maxHeight=?, " +
                        "preferredWidth=?, " +
                        "preferredHeight=?, " +
                        "width=" + roundScaledToString(120) + ", " +
                        "height=" + roundScaledToString(80) +
                    "], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f, 2.25f, 3.0f ]
    }

    def 'The border style will be simplified if margin and widths are all 0.'( float uiScale )
    {
        reportInfo """
            Simplifying a style configuration means that if the properties of a style
            a configured in such a way that they are effectively the same as the default
            in terms of how they will be rendered, then the style configuration will be
            simplified to the default style.
            
            This optimization is important to improve cache hit rates, as the immutable
            style configuration objects are used as cache keys.
            Simplifying the style configurations will ensure that style configurations
            which effectively render the same thing will also share the same cache buffer
            for rendering.
            
            It is also memory efficient, as the default style objects are global null objects.
        """
        given :
            SwingTree.initialiseUsing { it.uiScaleFactor(uiScale) }
        and : 'We create a highly simplifiable style through the style API.'
            var panel =
                        UI.panel().withStyle(conf -> conf
                            .borderColor(Color.GREEN)
                            .borderWidths(0,0,0,0)
                            .margin(0,0,0,0)
                            .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 20)
                            .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 10, 0)
                        )
                        .get(JPanel)
        expect : """
            The style config has the expected string representation,
            despite the fact that a visible border color was specified.
            This is because the border width is 0, so the border is invisible
            and the border color is irrelevant.
        """
            ComponentExtension.from(panel).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[NONE], " +
                    "BaseConf[NONE], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[NONE], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"

        when : """
            We now define the exact same style, but with a visible border width
            of 1 px at the right edge.
        """
            panel = UI.panel().withStyle(conf -> conf
                        .borderColor(Color.GREEN)
                        .borderWidths(0,0,0,1)
                        .padding(0,0,0,0)
                        .borderRadiusAt(UI.Corner.TOP_LEFT, 0, 20)
                        .borderRadiusAt(UI.Corner.BOTTOM_LEFT, 10, 0)
                    )
                    .get(JPanel)
        then : """
            The style config has the expected string representation,
            despite the fact that a visible border color was specified.
            This is because the border width is 0, so the border is invisible
            and the border color is irrelevant.
            
            Also note that if the uiScale is not whole, then the left border width
            is also not whole which then leads to there being a margin on the left side.
            This margin is a correction for the size of a component being integer based.
            So in order to make the transition between styles look smooth, we need the margin
            to buffer the fractional part of the border width.
        """
            ComponentExtension.from(panel).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[" +
                        "radius=?, " +
                        "topWidth=?, " +
                        "rightWidth=?, " +
                        "bottomWidth=?, " +
                        "leftWidth=" + String.valueOf(uiScale).replace(".0", "") + ", " +
                        "margin=Outline[top=?, right=?, bottom=?, left=${( uiScale % 1 == 0 ? "?" : 1 - uiScale % 1 )}], " +
                        "padding=Outline[top=0, right=0, bottom=0, left=0], " +
                        "color=rgba(0,255,0,255)" +
                    "], " +
                    "BaseConf[" +
                        (
                            uiScale % 1 == 0 ?
                                "NONE" :
                                "icon=?, " +
                                "fitComponent=NO, " +
                                "backgroundColor=rgba(238,238,238,255), " +
                                "foundationColor=?, " +
                                "foregroundColor=?, " +
                                "cursor=?, " +
                                "orientation=UNKNOWN"
                        ) +
                    "], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[NONE], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"
        where :
            uiScale << [ 1.0f, 1.5f, 2.0f, 2.25f, 3.0f ]
    }

    def 'The `UI.Color.UNDEFINED` constant can be used as a safe shorthand for null for the background and foreground properties of the style API'()
    {
        reportInfo """
            The `UI.Color.UNDEFINED` constant is a java.awt.Color object with all of its rgba values set to 0.
            Its identity is used to represent the absence of a color being specified, 
            and is used as a safe replacement for null, meaning that when the style engine of a 
            component encounters it, it will pass it onto
            the `Component::setBackground` and `Component::setForeground` methods as null.
            Passing null to these methods means that the look and feel determines the coloring.
        """
        given : 'We have a new Swing component with a custom foreground and background color.'
            var aToggleButton = new JToggleButton("Hello World")
            aToggleButton.setBackground(Color.ORANGE)
            aToggleButton.setForeground(Color.DARK_GRAY)
        when : """
            We send the component through the SwingTree builder API and apply a style to it... 
        """
        aToggleButton = UI.of(aToggleButton).withStyle(conf -> conf
                            .backgroundColor(UI.Color.UNDEFINED)
                            .foregroundColor(UI.Color.UNDEFINED)
                            .foundationColor(UI.Color.UNDEFINED)
                        )
                        .get(JToggleButton)
        then : """
            The component will have its background and foreground color set to null,
            which will cause it to use the default colors of the Look and Feel.
        """
            aToggleButton.getBackground() == null
            aToggleButton.getForeground() == null
        and : """
            The style config has the expected string representation!
            Note that only the background and foreground colors are now set to "DEFAULT",
            whereas the foundation color is represented by a question mark.
            This is because the foundation color is not something that is used by the look and feel,
            which means there is no default value for it.
            The foundation color is used by the SwingTree style engine only, which does not impose
            any default value for it (defaults may come from `StyleSheet` objects, but that is a different story).
        """
            ComponentExtension.from(aToggleButton).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[NONE], " +
                    "BaseConf[" +
                        "icon=?, " +
                        "fitComponent=NO, " +
                        "backgroundColor=DEFAULT, " +
                        "foundationColor=?, " +
                        "foregroundColor=DEFAULT, " +
                        "cursor=?, " +
                        "orientation=UNKNOWN" +
                    "], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[NONE], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"
    }


    def 'The `UI.Color.UNDEFINED` constant can be used as a safe shorthand for null for various properties in the style API'()
    {
        reportInfo """
            The `UI.Color.UNDEFINED` constant is a java.awt.Color object with all of its rgba values set to 0.
            Its identity is used to represent the absence of a color, and is used as a safe shorthand for null,
            meaning that when the style engine of a component encounters it, it will treat it as if no
            color was specified for the property.
            This is true for the shadow color, gradient colors, image primer color, and font colors...
        """
        given : """
            We have a simple Swing component.
            Which we wrap in the SwingTree builder API and apply a style to it... 
        """
            var aComboBox =
                        UI.of(new JComboBox<String>()).withStyle( conf -> conf
                            .shadowColor(UI.Color.UNDEFINED)
                            .gradient(UI.Layer.BACKGROUND, g -> g
                                .colors(UI.Color.UNDEFINED, UI.Color.UNDEFINED, UI.Color.UNDEFINED)
                            )
                            .image(UI.Layer.FOREGROUND, i -> i
                                .primer(UI.Color.UNDEFINED)
                            )
                            .fontColor(UI.Color.UNDEFINED)
                            .fontSelectionColor(UI.Color.UNDEFINED)
                            .fontBackgroundColor(UI.Color.UNDEFINED)
                        )
                        .get(JComboBox)
        expect : """
            The style configuration of the component will be simplified heavily, to
            the point where it is effectively considered to have no style at all.
        """
            ComponentExtension.from(aComboBox).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[NONE], " +
                    "BaseConf[NONE], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[NONE], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"
    }

    def 'The `UI.Font.UNDEFINED` constant can be used as a safe shorthand for null for the font property of the style API'()
    {
        reportInfo """
            The `UI.Font.UNDEFINED` constant is a java.awt.Font object with its family set to "" and its size set to -1.
            Its identity is used to represent the absence of a font family, and is used as a safe shorthand for null,
            meaning that when the style engine of a component encounters it, it will treat it as if no
            font family was specified for the property.
        """
        given : """
            We create a simple Swing JComboBox component,
            which we send through the SwingTree builder API and apply a style to it... 
        """
            var aComboBox =
                        UI.of(new JComboBox<String>()).withStyle( conf -> conf
                            .font(UI.Font.UNDEFINED)
                        )
                        .get(JComboBox)
        expect : """
            The style configuration of the component will be simplified heavily, to
            the point where it is effectively considered to have no style at all.
        """
            ComponentExtension.from(aComboBox).getStyle().toString() == "StyleConf[" +
                    "LayoutConf[NONE], " +
                    "BorderConf[NONE], " +
                    "BaseConf[NONE], " +
                    "FontConf[NONE], " +
                    "DimensionalityConf[NONE], " +
                    "StyleConfLayers[" +
                        "filter=FilterConf[NONE], " +
                        "background=StyleConfLayer[EMPTY], " +
                        "content=StyleConfLayer[EMPTY], " +
                        "border=StyleConfLayer[EMPTY], " +
                        "foreground=StyleConfLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"
    }

    def 'The style API allows you to configure a custom paint for a component font.'( float uiScale )
    {
        reportInfo """
            Usually the font of a component is a solid color which simply fills
            out the characters of the text. But the `java.awt.Font` class also
            allows for a custom paint to be used to fill out the characters.
            
            SwingTree allows you to configure this font paint
            through the style API.
            
            In this example we will configure a linear gradient as
            the paint for the font of a `JLabel` component.
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
            SwingTree.get().setUiScaleFactor(uiScale)

        and : 'A UI declaration consisting of a `JLabel` with a custom gradient paint for the font:'
            var ui =
                        UI.label("I am a Gradient")
                        .withStyle( it -> it
                            .size(200, 50)
                            .padding(3).borderRadius(12)
                            .componentFont(f->f
                                .size(18)
                                .family("Ubuntu")
                                .gradient(grad -> grad
                                    .colors(Color.RED, Color.GREEN, Color.BLUE)
                                    .boundary(UI.ComponentBoundary.INTERIOR_TO_CONTENT)
                                    .span(UI.Span.TOP_TO_BOTTOM)
                                    .type(UI.GradientType.LINEAR)
                                )
                                .backgroundGradient( grad -> grad
                                    .colors(Color.CYAN, Color.MAGENTA)
                                    .span(UI.Span.RIGHT_TO_LEFT)
                                    .type(UI.GradientType.LINEAR)
                                    .boundary(UI.ComponentBoundary.CENTER_TO_CONTENT)
                                    .offset(-6, -6)
                                )
                            )
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                        )
        when : 'We create the declared `JLabel` component using the `get` method...'
            var label = ui.get(JLabel)
        then : 'The label will have a font with the expected size and family.'
            label.getFont().getSize() == (int)(18 * uiScale)
            label.getFont().getFamily() == "Ubuntu"
        when : 'We extract the foreground and background paint of the font...'
            var paint           = label.getFont().getAttributes().get(TextAttribute.FOREGROUND)
            var backgroundPaint = label.getFont().getAttributes().get(TextAttribute.BACKGROUND)
        then : """
            We find that both paint objects are font paints.
        """
            paint instanceof swingtree.style.FontPaint
            backgroundPaint instanceof swingtree.style.FontPaint
        when : 'We unpack their delegated paint...'
            paint           = paint.getDelegatedPaint()
            backgroundPaint = backgroundPaint.getDelegatedPaint()
        then : """
            We find tha both paint objects are gradient paints.
            But one is a linear gradient and the other is a simple 2 color based gradient.
        """
            paint instanceof LinearGradientPaint
            backgroundPaint instanceof GradientPaint
        when : 'We cast them to their respective types...'
            var grad1 = paint as LinearGradientPaint
            var grad2 = backgroundPaint as GradientPaint
        then : 'They have the expected start and end points:'
            grad1.getStartPoint() == new Point2D.Float((float)(3*uiScale), (float)(3*uiScale))
            grad1.getEndPoint() == new Point2D.Float((float)(3*uiScale), (float)(47*uiScale))
            grad2.getPoint1() == new Point2D.Float((float)(100*uiScale+94.0-100), (float)(25*uiScale-6))
            grad2.getPoint2() == new Point2D.Float((float)(3*uiScale-6), (float)(25*uiScale-6))
        and : 'They also both have the expected colors:'
            grad1.getColors() == [ Color.RED, Color.GREEN, Color.BLUE ] as Color[]
            grad2.getColor1() == Color.CYAN
            grad2.getColor2() == Color.MAGENTA

        where :
            uiScale << [ 1.0f, 2.0f, 3.0f ]
    }

    def 'The style API allows you to configure a custom noise paint for a component font.'( float uiScale )
    {
        reportInfo """
            Usually the font of a component is a solid color which simply fills
            out the characters of the text. But the `java.awt.Font` class also
            allows for a custom paint to be used to fill out the characters.
            
            SwingTree allows you to configure this font paint
            through the style API.
            
            In this example we will configure a noise gradient as
            the paint for the font of a `JLabel` component.
            The noise gradient is a custom paint that uses a noise function
            to generate values used for interpolation between colors.
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
            SwingTree.get().setUiScaleFactor(uiScale)

        and :'A UI declaration consisting of a `JLabel` with a custom noise paint for the font:'
            var ui =
                        UI.label("I am a Gradient")
                        .withStyle( it -> it
                            .size(200, 50)
                            .padding(3).borderRadius(12)
                            .componentFont(f->f
                                .size(18)
                                .family("Arial")
                                .noise(grad -> grad
                                    .colors(Color.RED, Color.GREEN, Color.BLUE)
                                    .boundary(UI.ComponentBoundary.INTERIOR_TO_CONTENT)
                                    .function(UI.NoiseType.RETRO)
                                )
                                .backgroundNoise( grad -> grad
                                    .colors(Color.CYAN, Color.MAGENTA)
                                    .boundary(UI.ComponentBoundary.CENTER_TO_CONTENT)
                                    .offset(-6, -6)
                                    .function(UI.NoiseType.MANDELBROT)
                                    .scale(3, 4)
                                )
                            )
                            .fontAlignment(UI.HorizontalAlignment.CENTER)
                        )
        when : 'We create the declared `JLabel` component using the `get` method...'
            var label = ui.get(JLabel)
        then : 'The label will have a font with the expected size and family.'
            label.getFont().getSize() == 18 * uiScale
            label.getFont().getFamily() == "Arial"
        when : 'We extract the foreground and background paint of the font...'
            var paint           = label.getFont().getAttributes().get(TextAttribute.FOREGROUND)
            var backgroundPaint = label.getFont().getAttributes().get(TextAttribute.BACKGROUND)
        then : """
            We find that both paint objects are font paints.
        """
            paint instanceof swingtree.style.FontPaint
            backgroundPaint instanceof swingtree.style.FontPaint
        when : 'We unpack their delegated paint...'
            paint           = paint.getDelegatedPaint()
            backgroundPaint = backgroundPaint.getDelegatedPaint()
        then : """
            We find that both paint objects are noise paints.
        """
            paint instanceof swingtree.style.NoiseGradientPaint
            backgroundPaint instanceof swingtree.style.NoiseGradientPaint
        and : 'They have the expected colors and noise functions:'
            paint.getColors() == [ Color.RED, Color.GREEN, Color.BLUE ]
            paint.getNoiseFunction() == UI.NoiseType.RETRO
            backgroundPaint.getColors() == [ Color.CYAN, Color.MAGENTA ]
            backgroundPaint.getNoiseFunction() == UI.NoiseType.MANDELBROT
        and : 'They also both have the expected scale:'
            paint.getScale() == new Point2D.Float(1, 1)
            backgroundPaint.getScale() == new Point2D.Float(3, 4)

        where :
            uiScale << [ 1.0f, 2.0f, 3.0f ]
    }
}
