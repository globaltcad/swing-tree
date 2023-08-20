package swingtree.styles

import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import swingtree.SwingTree
import swingtree.threading.EventProcessor
import swingtree.style.Style
import swingtree.style.ComponentStyleDelegate

import javax.swing.JPanel
import java.awt.*

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
                                .fontName("Times New Roman")
                                .fontSize(12)
                                .fontBold(true)
                                .fontUnderline(true)
                                .fontStrikeThrough(true)
                                .style()

        expect :
                style.toString() == "Style[" +
                                        "LayoutStyle[" +
                                            "layout=Unspecific[], " +
                                            "constraint=?, " +
                                            "alignmentX=?, " +
                                            "alignmentY=?" +
                                        "], " +
                                        "BorderStyle[" +
                                            "arcWidth=12, " +
                                            "arcHeight=18, " +
                                            "width=?, " +
                                            "margin=Outline[top=?, right=?, bottom=?, left=?], " +
                                            "padding=Outline[top=?, right=?, bottom=?, left=?], " +
                                            "color=rgba(0,0,255,255), " +
                                            "GradientStyle[alignment=TOP_TO_BOTTOM, type=LINEAR, colors=[], layer=BACKGROUND]" +
                                        "], " +
                                        "BaseStyle[" +
                                            "backgroundColor=rgba(0,255,0,255), " +
                                            "foundationColor=rgba(255,0,0,255), " +
                                            "foregroundColor=?, " +
                                            "cursor=?" +
                                        "], " +
                                        "FontStyle[" +
                                            "name=Times New Roman, size=12, posture=0.0, weight=2.0, underlined=true, " +
                                            "strikeThrough=true, color=rgba(255,0,255,255), backgroundColor=?, " +
                                            "selectionColor=rgba(0,255,255,255), transform=?, paint=?, horizontalAlignment=?, verticalAlignment=?" +
                                        "], " +
                                        "DimensionalityStyle[" +
                                            "minWidth=?, minHeight=?, " +
                                            "maxWidth=?, maxHeight=?, " +
                                            "preferredWidth=?, preferredHeight=?, " +
                                            "width=?, height=?" +
                                        "], " +
                                        "ShadowStyle[" +
                                            "horizontalOffset=0, " +
                                            "verticalOffset=0, " +
                                            "blurRadius=0, " +
                                            "spreadRadius=0, " +
                                            "color=rgba(255,255,0,255), " +
                                            "isInset=false, " +
                                            "layer=CONTENT" +
                                        "], " +
                                        "PainterStyle[" +
                                            "painter=none, " +
                                            "layer=BACKGROUND" +
                                        "], " +
                                        "GradientStyle[alignment=TOP_TO_BOTTOM, type=LINEAR, colors=[], layer=BACKGROUND], " +
                                        "ImageStyle[" +
                                            "layer=BACKGROUND, " +
                                            "primer=?, " +
                                            "image=?, " +
                                            "placement=CENTER, " +
                                            "repeat=false, " +
                                            "autoFit=false, " +
                                            "width=?, " +
                                            "height=?, " +
                                            "opacity=1.0, " +
                                            "padding=Outline[top=?, right=?, bottom=?, left=?]" +
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
                                 .fontName("Times New Roman")
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
                                 .fontName("Times New Roman")
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
}
