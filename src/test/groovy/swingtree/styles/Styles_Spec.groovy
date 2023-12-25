package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.UI
import swingtree.style.ComponentExtension
import swingtree.style.Layout
import swingtree.threading.EventProcessor
import swingtree.style.Style
import swingtree.style.ComponentStyleDelegate

import javax.swing.JPanel
import java.awt.*
import java.awt.geom.AffineTransform

@Title("Style Properties")
@Narrative('''

    This specification demonstrates how the `Style` type
    can be used to define how Swing components ought to be
    rendered.

''')
@Subject([Style])
class Styles_Spec extends Specification
{
    def setupSpec() {
        SwingTree.get().setEventProcessor(EventProcessor.COUPLED_STRICT)
        // In this specification we are using the strict event processor
        // which will throw exceptions if we try to perform UI operations in the test thread.
    }

    def 'Various kinds of String expressions can be parsed as colors by various style properties.'(
        String colorString, Color expectedColor
    ) {
        given : 'We use method chaining to build a colorful style:'
            var style = new ComponentStyleDelegate<>(new JPanel(), Style.none())
                                .foundationColor(colorString)
                                .backgroundColor(colorString)
                                .borderColor(colorString)
                                .shadowColor(colorString)
                                .fontSelectionColor(colorString)
                                .fontColor(colorString)
                                .style()
        expect :
            style.base().foundationColor().get() == expectedColor
            style.base().backgroundColor().get() == expectedColor
            style.border().color().get() == expectedColor
            style.shadow().color().get() == expectedColor
            style.font().selectionColor().get() == expectedColor
            style.font().color().get() == expectedColor

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
    }

    def 'The String representation of a Style will tell you everything about it!'()
    {
        given : 'We first create a style with various properties:'
            var style = new ComponentStyleDelegate<>(new JPanel(), Style.none())
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
                                .style()

        expect :
                style.toString() == "Style[" +
                                        "LayoutStyle[NONE], " +
                                        "BorderStyle[" +
                                            "arcWidth=12, " +
                                            "arcHeight=18, " +
                                            "width=?, " +
                                            "margin=Outline[top=?, right=?, bottom=?, left=?], " +
                                            "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                                            "color=rgba(0,0,255,255), " +
                                            "GradientStyle[NONE]" +
                                        "], " +
                                        "BaseStyle[" +
                                            "icon=?, " +
                                            "fitComponent=NO, " +
                                            "backgroundColor=rgba(0,255,0,255), " +
                                            "foundationColor=rgba(255,0,0,255), " +
                                            "foregroundColor=?, " +
                                            "cursor=?, " +
                                            "orientation=UNKNOWN" +
                                        "], " +
                                        "FontStyle[" +
                                            "family=Times New Roman, " +
                                            "size=12, " +
                                            "posture=0.0, " +
                                            "weight=2.0, " +
                                            "spacing=0.0, " +
                                            "underlined=true, " +
                                            "strikeThrough=true, " +
                                            "color=rgba(255,0,255,255), " +
                                            "backgroundColor=?, " +
                                            "selectionColor=rgba(0,255,255,255), " +
                                            "transform=?, " +
                                            "paint=?, " +
                                            "backgroundPaint=?, " +
                                            "horizontalAlignment=?, " +
                                            "verticalAlignment=?" +
                                        "], " +
                                        "DimensionalityStyle[NONE], " +
                                        "StyleLayers[" +
                                            "background=StyleLayer[EMPTY], " +
                                            "content=StyleLayer[" +
                                                "shadows=ShadowStyle[" +
                                                    "horizontalOffset=0, " +
                                                    "verticalOffset=0, " +
                                                    "blurRadius=0, " +
                                                    "spreadRadius=0, " +
                                                    "color=rgba(255,255,0,255), " +
                                                    "isInset=false" +
                                                "], " +
                                                "painters=PainterStyle[NONE], " +
                                                "gradients=GradientStyle[NONE], " +
                                                "images=ImageStyle[NONE]" +
                                            "], " +
                                            "border=StyleLayer[EMPTY], " +
                                            "foreground=StyleLayer[EMPTY]" +
                                        "], " +
                                        "properties=[]" +
                                    "]"

        when : 'We create another style with some other properties:'
            var paint1 = new GradientPaint(0, 0, Color.RED, 100, 100, Color.BLUE)
            var paint2 = new GradientPaint(0, 0, Color.BLACK, 100, 100, Color.GREEN)
            var transform = AffineTransform.getRotateInstance(0.5)
            style = new ComponentStyleDelegate<>(new JPanel(), Style.none())
                                .fontAlignment(UI.Alignment.CENTER)
                                .fontBackgroundPaint(paint1)
                                .fontPaint(paint2)
                                .fontTransform(transform)
                                .fontBackgroundColor("cyan")
                                .fontBackgroundColor(new Color(0, 42, 42, 42))
                                .image(UI.Layer.FOREGROUND, "bubbles", conf -> conf
                                    .fitMode(UI.FitComponent.WIDTH)
                                    .repeat(true)
                                    .image("/images/bubbles.svg")
                                )
                                .style()

        then :
                style.toString() == "Style[" +
                                        "LayoutStyle[NONE], " +
                                        "BorderStyle[NONE], " +
                                        "BaseStyle[NONE], " +
                                        "FontStyle[" +
                                            "family=, " +
                                            "size=0, " +
                                            "posture=0.0, " +
                                            "weight=0.0, " +
                                            "spacing=0.0, " +
                                            "underlined=?, " +
                                            "strikeThrough=?, " +
                                            "color=?, " +
                                            "backgroundColor=rgba(0,42,42,42), " +
                                            "selectionColor=?, " +
                                            "transform=$transform, " +
                                            "paint=$paint2, " +
                                            "backgroundPaint=$paint1, " +
                                            "horizontalAlignment=CENTER, " +
                                            "verticalAlignment=CENTER" +
                                        "], " +
                                        "DimensionalityStyle[NONE], " +
                                        "StyleLayers[" +
                                            "background=StyleLayer[EMPTY], " +
                                            "content=StyleLayer[EMPTY], " +
                                            "border=StyleLayer[EMPTY], " +
                                            "foreground=StyleLayer[" +
                                                "shadows=ShadowStyle[NONE], " +
                                                "painters=PainterStyle[NONE], " +
                                                "gradients=GradientStyle[NONE], " +
                                                "images=NamedStyles[" +
                                                    "default=ImageStyle[NONE], " +
                                                    "bubbles=ImageStyle[" +
                                                        "primer=?, " +
                                                        "image=SvgIcon[" +
                                                            "width=?, height=?, " +
                                                            "fitComponent=MIN_DIM, " +
                                                            "preferredPlacement=UNDEFINED, " +
                                                            "doc=?" +
                                                        "], " +
                                                        "placement=UNDEFINED, " +
                                                        "repeat=true, " +
                                                        "fitComponent=WIDTH, " +
                                                        "width=?, height=?, " +
                                                        "opacity=1.0, " +
                                                        "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                                                        "offset=Offset[x=0, y=0], " +
                                                        "clipArea=BODY" +
                                                    "]" +
                                                "]" +
                                            "]" +
                                        "], " +
                                        "properties=[]" +
                                    "]"

        when : 'We create another style with some other properties:'
            style = new ComponentStyleDelegate<>(new JPanel(), Style.none())
                                .layout(Layout.border(2, 4))
                                .size(100, 200)
                                .minSize(50, 100)
                                .maxHeight(300)
                                .prefWidth(400)
                                .gradient(UI.Layer.BORDER, "gradient", conf -> conf
                                    .colors(Color.RED, Color.GREEN, Color.BLUE)
                                    .transition(UI.Transition.BOTTOM_TO_TOP)
                                    .type(UI.GradientType.RADIAL)
                                )
                                .style()

        then :
                style.toString() == "Style[" +
                                        "LayoutStyle[" +
                                            "layout=BorderLayoutInstaller[hgap=2, vgap=4], " +
                                            "constraint=?, " +
                                            "alignmentX=?, " +
                                            "alignmentY=?" +
                                        "], " +
                                        "BorderStyle[NONE], " +
                                        "BaseStyle[NONE], " +
                                        "FontStyle[NONE], " +
                                        "DimensionalityStyle[" +
                                            "minWidth=50, " +
                                            "minHeight=100, " +
                                            "maxWidth=300, " +
                                            "maxHeight=?, " +
                                            "preferredWidth=400, " +
                                            "preferredHeight=?, " +
                                            "width=100, " +
                                            "height=200" +
                                        "], " +
                                        "StyleLayers[" +
                                            "background=StyleLayer[EMPTY], " +
                                            "content=StyleLayer[EMPTY], " +
                                            "border=StyleLayer[" +
                                                "shadows=ShadowStyle[NONE], " +
                                                "painters=PainterStyle[NONE], " +
                                                "gradients=NamedStyles[" +
                                                    "default=GradientStyle[NONE], " +
                                                    "gradient=GradientStyle[" +
                                                        "transition=BOTTOM_TO_TOP, " +
                                                        "type=RADIAL, " +
                                                        "colors=[java.awt.Color[r=255,g=0,b=0], " +
                                                        "java.awt.Color[r=0,g=255,b=0], " +
                                                        "java.awt.Color[r=0,g=0,b=255]" +
                                                    "]" +
                                                "]" +
                                            "], " +
                                            "images=ImageStyle[NONE]" +
                                        "], " +
                                        "foreground=StyleLayer[EMPTY]" +
                                    "], " +
                                    "properties=[]" +
                                "]"

    }

    def 'Style objects are value based (with respect to equality and hash code).'()
    {
        given : 'First we need the `StyleDelegate` styler, which can apply styles to `Style` objects.'
            var styler = s -> new ComponentStyleDelegate<>(new JPanel(), s)

        and : 'Then we create a starting style with various properties:'
            var style1 = styler(Style.none())
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
                                 .style()

        and : 'We then create a second style with the same properties:'
            var style2 = styler(Style.none())
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
                                 .style()
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

    def 'SwingTree will simplify the style configuration of a component if possible.'()
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
        given : 'We create a highly simplifiable style through the style API.'
            var ui = UI.panel().withStyle(conf -> conf
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
                                    .transition(UI.Transition.TOP_TO_BOTTOM)
                                    .type(UI.GradientType.LINEAR)
                                )
                        )
                        .getComponent()
        expect : 'The style config has the expected string representation.'
            ComponentExtension.from(ui).getStyle().toString() == "Style[" +
                    "LayoutStyle[NONE], " +
                    "BorderStyle[NONE], " +
                    "BaseStyle[NONE], " +
                    "FontStyle[NONE], " +
                    "DimensionalityStyle[NONE], " +
                    "StyleLayers[" +
                        "background=StyleLayer[EMPTY], " +
                        "content=StyleLayer[EMPTY], " +
                        "border=StyleLayer[EMPTY], " +
                        "foreground=StyleLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"

        when : 'We create a not so simplifiable style for a component through the style API...'
            ui = UI.button("Hello World").withStyle(conf -> conf
                            .backgroundColor(Color.BLACK)
                            .foregroundColor(Color.WHITE)
                            .size(120, 80)
                            .borderRadius(40)
                        )
                        .getComponent()
        then : 'The style config has the expected string representation.'
            ComponentExtension.from(ui).getStyle().toString() == "Style[" +
                    "LayoutStyle[NONE], " +
                    "BorderStyle[" +
                        "radius=40, " +
                        "width=?, " +
                        "margin=Outline[top=?, right=?, bottom=?, left=?], " +
                        "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                        "color=?, " +
                        "GradientStyle[NONE]" +
                    "], " +
                    "BaseStyle[" +
                        "icon=?, " +
                        "fitComponent=NO, " +
                        "backgroundColor=rgba(0,0,0,255), " +
                        "foundationColor=?, " +
                        "foregroundColor=rgba(255,255,255,255), " +
                        "cursor=?, " +
                        "orientation=UNKNOWN" +
                    "], " +
                    "FontStyle[NONE], " +
                    "DimensionalityStyle[" +
                        "minWidth=?, " +
                        "minHeight=?, " +
                        "maxWidth=?, " +
                        "maxHeight=?, " +
                        "preferredWidth=?, " +
                        "preferredHeight=?, " +
                        "width=120, " +
                        "height=80" +
                    "], " +
                    "StyleLayers[" +
                        "background=StyleLayer[EMPTY], " +
                        "content=StyleLayer[EMPTY], " +
                        "border=StyleLayer[EMPTY], " +
                        "foreground=StyleLayer[EMPTY]" +
                    "], " +
                    "properties=[]" +
                "]"
    }
}
