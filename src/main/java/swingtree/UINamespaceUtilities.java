package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.style.Colour;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Objects;

/**
 *  A namespace for useful factory methods like
 *  {@link #color(String)} and {@link #font(String)},
 *  and layout constants (see {@link UILayoutConstants}).
 *  <br>
 *  <b>
 *      This class is intended to be used
 *      by the {@link UI} namespace class ONLY!
 *      <br>
 *      Please do not inherit or import this class
 *      in your own code, as it is not intended to be
 *      used outside of the {@link UI} namespace.
 *  </b>
 */
public abstract class UINamespaceUtilities extends UILayoutConstants
{
    private static final Logger log = LoggerFactory.getLogger(UINamespaceUtilities.class);

    /**
     *  This constant is a {@link Colour} object with all of its rgba values set to 0.
     *  Its identity is used to represent the absence of a color being specified,
     *  and it is used as a safe replacement for null,
     *  meaning that when the style engine of a component encounters it, it will pass it onto
     *  the {@link java.awt.Component#setBackground(Color)} and
     *  {@link java.awt.Component#setForeground(Color)} methods as null.
     *  Passing null to these methods means that the look and feel determines the coloring.
     */
    public static final Colour COLOR_UNDEFINED = Colour.UNDEFINED;

    /**
     * A fully transparent color with an ARGB value of #00000000.
     */
    public static final Colour COLOR_TRANSPARENT = Colour.TRANSPARENT;

    /**
     * The color alice blue with an RGB value of #F0F8FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0F8FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_ALICEBLUE = Colour.ALICEBLUE;

    /**
     * The color antique white with an RGB value of #FAEBD7
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAEBD7;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_ANTIQUEWHITE = Colour.ANTIQUEWHITE;

    /**
     * The color aqua with an RGB value of #00FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_AQUA = Colour.AQUA;

    /**
     * The color aquamarine with an RGB value of #7FFFD4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFFD4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_AQUAMARINE = Colour.AQUAMARINE;

    /**
     * The color azure with an RGB value of #F0FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_AZURE = Colour.AZURE;

    /**
     * The color beige with an RGB value of #F5F5DC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5DC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BEIGE = Colour.BEIGE;

    /**
     * The color bisque with an RGB value of #FFE4C4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4C4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BISQUE = Colour.BISQUE;

    /**
     * The color black with an RGB value of #000000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#000000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BLACK = Colour.BLACK;

    /**
     * The color blanched almond with an RGB value of #FFEBCD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEBCD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BLANCHEDALMOND = Colour.BLANCHEDALMOND;

    /**
     * The color blue with an RGB value of #0000FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BLUE = Colour.BLUE;

    /**
     * The color blue violet with an RGB value of #8A2BE2
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8A2BE2;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BLUEVIOLET = Colour.BLUEVIOLET;

    /**
     * The color brown with an RGB value of #A52A2A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A52A2A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BROWN = Colour.BROWN;

    /**
     * The color burly wood with an RGB value of #DEB887
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DEB887;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_BURLYWOOD = Colour.BURLYWOOD;

    /**
     * The color cadet blue with an RGB value of #5F9EA0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#5F9EA0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CADETBLUE = Colour.CADETBLUE;

    /**
     * The color chartreuse with an RGB value of #7FFF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFF00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CHARTREUSE = Colour.CHARTREUSE;

    /**
     * The color chocolate with an RGB value of #D2691E
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2691E;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CHOCOLATE = Colour.CHOCOLATE;

    /**
     * The color coral with an RGB value of #FF7F50
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF7F50;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CORAL = Colour.CORAL;

    /**
     * The color cornflower blue with an RGB value of #6495ED
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6495ED;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CORNFLOWERBLUE = Colour.CORNFLOWERBLUE;

    /**
     * The color cornsilk with an RGB value of #FFF8DC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF8DC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CORNSILK = Colour.CORNSILK;

    /**
     * The color crimson with an RGB value of #DC143C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DC143C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CRIMSON = Colour.CRIMSON;

    /**
     * The color cyan with an RGB value of #00FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_CYAN = Colour.CYAN;

    /**
     * The color dark blue with an RGB value of #00008B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00008B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKBLUE = Colour.DARKBLUE;

    /**
     * The color dark cyan with an RGB value of #008B8B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008B8B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKCYAN = Colour.DARKCYAN;

    /**
     * The color dark goldenrod with an RGB value of #B8860B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B8860B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKGOLDENROD = Colour.DARKGOLDENROD;

    /**
     * The color dark gray with an RGB value of #A9A9A9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKGRAY = Colour.DARKGRAY;

    /**
     * The color dark green with an RGB value of #006400
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#006400;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKGREEN = Colour.DARKGREEN;

    /**
     * The color dark grey with an RGB value of #A9A9A9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKGREY = COLOR_DARKGRAY;

    /**
     * The color dark khaki with an RGB value of #BDB76B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BDB76B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKKHAKI = Colour.DARKKHAKI;

    /**
     * The color dark magenta with an RGB value of #8B008B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B008B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKMAGENTA = Colour.DARKMAGENTA;

    /**
     * The color dark olive green with an RGB value of #556B2F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#556B2F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKOLIVEGREEN = Colour.DARKOLIVEGREEN;

    /**
     * The color dark orange with an RGB value of #FF8C00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF8C00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKORANGE = Colour.DARKORANGE;

    /**
     * The color dark orchid with an RGB value of #9932CC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9932CC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKORCHID = Colour.DARKORCHID;

    /**
     * The color dark red with an RGB value of #8B0000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B0000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKRED = Colour.DARKRED;

    /**
     * The color dark salmon with an RGB value of #E9967A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E9967A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKSALMON = Colour.DARKSALMON;

    /**
     * The color dark sea green with an RGB value of #8FBC8F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8FBC8F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKSEAGREEN = Colour.DARKSEAGREEN;

    /**
     * The color dark slate blue with an RGB value of #483D8B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#483D8B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKSLATEBLUE = Colour.DARKSLATEBLUE;

    /**
     * The color dark slate gray with an RGB value of #2F4F4F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKSLATEGRAY = Colour.DARKSLATEGRAY;

    /**
     * The color dark slate grey with an RGB value of #2F4F4F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKSLATEGREY        = COLOR_DARKSLATEGRAY;

    /**
     * The color dark turquoise with an RGB value of #00CED1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00CED1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKTURQUOISE = Colour.DARKTURQUOISE;

    /**
     * The color dark violet with an RGB value of #9400D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9400D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DARKVIOLET = Colour.DARKVIOLET;

    /**
     * The color deep pink with an RGB value of #FF1493
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF1493;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DEEPPINK = Colour.DEEPPINK;

    /**
     * The color deep sky blue with an RGB value of #00BFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00BFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DEEPSKYBLUE = Colour.DEEPSKYBLUE;

    /**
     * The color dim gray with an RGB value of #696969
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DIMGRAY = Colour.DIMGRAY;

    /**
     * The color dim grey with an RGB value of #696969
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DIMGREY = COLOR_DIMGRAY;

    /**
     * The color dodger blue with an RGB value of #1E90FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#1E90FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_DODGERBLUE = Colour.DODGERBLUE;

    /**
     * The color firebrick with an RGB value of #B22222
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B22222;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_FIREBRICK = Colour.FIREBRICK;

    /**
     * The color floral white with an RGB value of #FFFAF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAF0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_FLORALWHITE = Colour.FLORALWHITE;

    /**
     * The color forest green with an RGB value of #228B22
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#228B22;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_FORESTGREEN = Colour.FORESTGREEN;

    /**
     * The color fuchsia with an RGB value of #FF00FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_FUCHSIA = Colour.FUCHSIA;

    /**
     * The color gainsboro with an RGB value of #DCDCDC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DCDCDC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GAINSBORO = Colour.GAINSBORO;

    /**
     * The color ghost white with an RGB value of #F8F8FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F8F8FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GHOSTWHITE = Colour.GHOSTWHITE;

    /**
     * The color gold with an RGB value of #FFD700
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFD700;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GOLD = Colour.GOLD;

    /**
     * The color goldenrod with an RGB value of #DAA520
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DAA520;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GOLDENROD = Colour.GOLDENROD;

    /**
     * The color gray with an RGB value of #808080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GRAY = Colour.GRAY;

    /**
     * The color green with an RGB value of #008000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GREEN = Colour.GREEN;

    /**
     * The color green yellow with an RGB value of #ADFF2F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADFF2F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GREENYELLOW = Colour.GREENYELLOW;

    /**
     * The color grey with an RGB value of #808080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_GREY                 = COLOR_GRAY;

    /**
     * The color honeydew with an RGB value of #F0FFF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFF0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_HONEYDEW = Colour.HONEYDEW;

    /**
     * The color hot pink with an RGB value of #FF69B4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF69B4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_HOTPINK = Colour.HOTPINK;

    /**
     * The color indian red with an RGB value of #CD5C5C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD5C5C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_INDIANRED = Colour.INDIANRED;

    /**
     * The color indigo with an RGB value of #4B0082
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4B0082;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_INDIGO = Colour.INDIGO;

    /**
     * The color ivory with an RGB value of #FFFFF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFF0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_IVORY = Colour.IVORY;

    /**
     * The color khaki with an RGB value of #F0E68C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0E68C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_KHAKI = Colour.KHAKI;

    /**
     * The color lavender with an RGB value of #E6E6FA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E6E6FA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LAVENDER = Colour.LAVENDER;

    /**
     * The color lavender blush with an RGB value of #FFF0F5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF0F5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LAVENDERBLUSH = Colour.LAVENDERBLUSH;

    /**
     * The color lawn green with an RGB value of #7CFC00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7CFC00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LAWNGREEN = Colour.LAWNGREEN;

    /**
     * The color lemon chiffon with an RGB value of #FFFACD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFACD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LEMONCHIFFON = Colour.LEMONCHIFFON;

    /**
     * The color light blue with an RGB value of #ADD8E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADD8E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTBLUE = Colour.LIGHTBLUE;

    /**
     * The color light coral with an RGB value of #F08080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F08080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTCORAL = Colour.LIGHTCORAL;

    /**
     * The color light cyan with an RGB value of #E0FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E0FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTCYAN = Colour.LIGHTCYAN;

    /**
     * The color light goldenrod yellow with an RGB value of #FAFAD2
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAFAD2;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTGOLDENRODYELLOW = Colour.LIGHTGOLDENRODYELLOW;

    /**
     * The color light gray with an RGB value of #D3D3D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTGRAY = Colour.LIGHTGRAY;

    /**
     * The color light green with an RGB value of #90EE90
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#90EE90;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTGREEN = Colour.LIGHTGREEN;

    /**
     * The color light grey with an RGB value of #D3D3D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTGREY            = COLOR_LIGHTGRAY;

    /**
     * The color light pink with an RGB value of #FFB6C1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFB6C1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTPINK = Colour.LIGHTPINK;

    /**
     * The color light salmon with an RGB value of #FFA07A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA07A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTSALMON = Colour.LIGHTSALMON;

    /**
     * The color light sea green with an RGB value of #20B2AA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#20B2AA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTSEAGREEN = Colour.LIGHTSEAGREEN;

    /**
     * The color light sky blue with an RGB value of #87CEFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEFA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTSKYBLUE = Colour.LIGHTSKYBLUE;

    /**
     * The color light slate gray with an RGB value of #778899
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTSLATEGRAY = Colour.LIGHTSLATEGRAY;

    /**
     * The color light slate grey with an RGB value of #778899
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTSLATEGREY       = COLOR_LIGHTSLATEGRAY;

    /**
     * The color light steel blue with an RGB value of #B0C4DE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0C4DE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTSTEELBLUE = Colour.LIGHTSTEELBLUE;

    /**
     * The color light yellow with an RGB value of #FFFFE0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFE0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIGHTYELLOW = Colour.LIGHTYELLOW;

    /**
     * The color lime with an RGB value of #00FF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIME = Colour.LIME;

    /**
     * The color lime green with an RGB value of #32CD32
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#32CD32;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LIMEGREEN = Colour.LIMEGREEN;

    /**
     * The color linen with an RGB value of #FAF0E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAF0E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_LINEN = Colour.LINEN;

    /**
     * The color magenta with an RGB value of #FF00FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MAGENTA = Colour.MAGENTA;

    /**
     * The color maroon with an RGB value of #800000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#800000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MAROON = Colour.MAROON;

    /**
     * The color medium aquamarine with an RGB value of #66CDAA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#66CDAA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMAQUAMARINE = Colour.MEDIUMAQUAMARINE;

    /**
     * The color medium blue with an RGB value of #0000CD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000CD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMBLUE = Colour.MEDIUMBLUE;

    /**
     * The color medium orchid with an RGB value of #BA55D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BA55D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMORCHID = Colour.MEDIUMORCHID;

    /**
     * The color medium purple with an RGB value of #9370DB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9370DB;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMPURPLE = Colour.MEDIUMPURPLE;

    /**
     * The color medium sea green with an RGB value of #3CB371
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#3CB371;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMSEAGREEN = Colour.MEDIUMSEAGREEN;

    /**
     * The color medium slate blue with an RGB value of #7B68EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7B68EE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMSLATEBLUE = Colour.MEDIUMSLATEBLUE;

    /**
     * The color medium spring green with an RGB value of #00FA9A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FA9A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMSPRINGGREEN = Colour.MEDIUMSPRINGGREEN;

    /**
     * The color medium turquoise with an RGB value of #48D1CC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#48D1CC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMTURQUOISE = Colour.MEDIUMTURQUOISE;

    /**
     * The color medium violet red with an RGB value of #C71585
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#C71585;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MEDIUMVIOLETRED = Colour.MEDIUMVIOLETRED;

    /**
     * The color midnight blue with an RGB value of #191970
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#191970;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MIDNIGHTBLUE = Colour.MIDNIGHTBLUE;

    /**
     * The color mint cream with an RGB value of #F5FFFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5FFFA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MINTCREAM = Colour.MINTCREAM;

    /**
     * The color misty rose with an RGB value of #FFE4E1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4E1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MISTYROSE = Colour.MISTYROSE;

    /**
     * The color moccasin with an RGB value of #FFE4B5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4B5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_MOCCASIN = Colour.MOCCASIN;

    /**
     * The color navajo white with an RGB value of #FFDEAD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDEAD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_NAVAJOWHITE = Colour.NAVAJOWHITE;

    /**
     * The color navy with an RGB value of #000080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#000080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_NAVY = Colour.NAVY;

    /**
     * The color old lace with an RGB value of #FDF5E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FDF5E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_OLDLACE = Colour.OLDLACE;

    /**
     * The color olive with an RGB value of #808000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_OLIVE = Colour.OLIVE;

    /**
     * The color olive drab with an RGB value of #6B8E23
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6B8E23;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_OLIVEDRAB = Colour.OLIVEDRAB;

    /**
     * The color orange with an RGB value of #FFA500
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA500;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_ORANGE = Colour.ORANGE;

    /**
     * The color orange red with an RGB value of #FF4500
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF4500;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_ORANGERED = Colour.ORANGERED;

    /**
     * The color orchid with an RGB value of #DA70D6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DA70D6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_ORCHID = Colour.ORCHID;

    /**
     * The color pale goldenrod with an RGB value of #EEE8AA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#EEE8AA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PALEGOLDENROD = Colour.PALEGOLDENROD;

    /**
     * The color pale green with an RGB value of #98FB98
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#98FB98;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PALEGREEN = Colour.PALEGREEN;

    /**
     * The color pale turquoise with an RGB value of #AFEEEE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#AFEEEE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PALETURQUOISE = Colour.PALETURQUOISE;

    /**
     * The color pale violet red with an RGB value of #DB7093
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DB7093;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PALEVIOLETRED = Colour.PALEVIOLETRED;

    /**
     * The color papaya whip with an RGB value of #FFEFD5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEFD5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PAPAYAWHIP = Colour.PAPAYAWHIP;

    /**
     * The color peach puff with an RGB value of #FFDAB9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDAB9;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PEACHPUFF = Colour.PEACHPUFF;

    /**
     * The color peru with an RGB value of #CD853F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD853F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PERU = Colour.PERU;

    /**
     * The color pink with an RGB value of #FFC0CB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFC0CB;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PINK = Colour.PINK;

    /**
     * The color plum with an RGB value of #DDA0DD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DDA0DD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PLUM = Colour.PLUM;

    /**
     * The color powder blue with an RGB value of #B0E0E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0E0E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_POWDERBLUE = Colour.POWDERBLUE;

    /**
     * The color purple with an RGB value of #800080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#800080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_PURPLE = Colour.PURPLE;

    /**
     * The color red with an RGB value of #FF0000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF0000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_RED = Colour.RED;

    /**
     * The color rosy brown with an RGB value of #BC8F8F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BC8F8F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_ROSYBROWN = Colour.ROSYBROWN;

    /**
     * The color royal blue with an RGB value of #4169E1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4169E1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_ROYALBLUE = Colour.ROYALBLUE;

    /**
     * The color saddle brown with an RGB value of #8B4513
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B4513;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SADDLEBROWN = Colour.SADDLEBROWN;

    /**
     * The color salmon with an RGB value of #FA8072
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FA8072;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SALMON = Colour.SALMON;

    /**
     * The color sandy brown with an RGB value of #F4A460
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F4A460;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SANDYBROWN = Colour.SANDYBROWN;

    /**
     * The color sea green with an RGB value of #2E8B57
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2E8B57;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SEAGREEN = Colour.SEAGREEN;

    /**
     * The color sea shell with an RGB value of #FFF5EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF5EE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SEASHELL = Colour.SEASHELL;

    /**
     * The color sienna with an RGB value of #A0522D
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A0522D;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SIENNA = Colour.SIENNA;

    /**
     * The color silver with an RGB value of #C0C0C0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#C0C0C0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SILVER = Colour.SILVER;
    /**
     * The color sky blue with an RGB value of #87CEEB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEEB;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SKYBLUE = Colour.SKYBLUE;

    /**
     * The color slate blue with an RGB value of #6A5ACD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6A5ACD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SLATEBLUE = Colour.SLATEBLUE;

    /**
     * The color slate gray with an RGB value of #708090
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SLATEGRAY = Colour.SLATEGRAY;

    /**
     * The color slate grey with an RGB value of #708090
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SLATEGREY            = COLOR_SLATEGRAY;

    /**
     * The color snow with an RGB value of #FFFAFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAFA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SNOW = Colour.SNOW;

    /**
     * The color spring green with an RGB value of #00FF7F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF7F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_SPRINGGREEN = Colour.SPRINGGREEN;

    /**
     * The color steel blue with an RGB value of #4682B4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4682B4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_STEELBLUE = Colour.STEELBLUE;

    /**
     * The color tan with an RGB value of #D2B48C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2B48C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_TAN = Colour.TAN;

    /**
     * The color teal with an RGB value of #008080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_TEAL = Colour.TEAL;

    /**
     * The color thistle with an RGB value of #D8BFD8
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D8BFD8;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_THISTLE = Colour.THISTLE;

    /**
     * The color tomato with an RGB value of #FF6347
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF6347;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_TOMATO = Colour.TOMATO;

    /**
     * The color turquoise with an RGB value of #40E0D0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#40E0D0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_TURQUOISE = Colour.TURQUOISE;

    /**
     * The color violet with an RGB value of #EE82EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#EE82EE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_VIOLET = Colour.VIOLET;

    /**
     * The color wheat with an RGB value of #F5DEB3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5DEB3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_WHEAT = Colour.WHEAT;

    /**
     * The color white with an RGB value of #FFFFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_WHITE = Colour.WHITE;

    /**
     * The color white smoke with an RGB value of #F5F5F5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5F5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_WHITESMOKE = Colour.WHITESMOKE;

    /**
     * The color yellow with an RGB value of #FFFF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFF00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_YELLOW = Colour.YELLOW;

    /**
     * The color yellow green with an RGB value of #9ACD32
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9ACD32;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour COLOR_YELLOWGREEN = Colour.YELLOWGREEN;

    /**
     *  This constant is a {@link java.awt.Font} object with a font name of "" (empty string),
     *  a font style of -1 (undefined) and a font size of 0.
     *  Its identity is used to represent the absence of a font being specified,
     *  and it is used as a safe replacement for null,
     *  meaning that when the style engine of a component encounters it, it will pass it onto
     *  the {@link java.awt.Component#setFont(Font)} method as null.
     *  Passing null to this method means that the look and feel determines the font.
     */
    public static final Font FONT_UNDEFINED = new Font("", -1, 0);

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green and blue values.
     *
     * @param r The red value (0-255).
     * @param g The green value (0-255).
     * @param b The blue value (0-255).
     * @return The new color.
     */
    public static Color color( int r, int g, int b ) {
        return new Color(r, g, b);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green, blue and alpha values.
     *
     * @param r The red value (0-255).
     * @param g The green value (0-255).
     * @param b The blue value (0-255).
     * @param a The alpha value (0-255).
     * @return The new color.
     */
    public static Color color( int r, int g, int b, int a ) {
        return new Color(r, g, b, a);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green and blue values.
     *
     * @param r The red value (0.0-1.0).
     * @param g The green value (0.0-1.0).
     * @param b The blue value (0.0-1.0).
     * @return The new color.
     */
    public static Color color( double r, double g, double b ) {
        return new Color((float) r, (float) g, (float) b);
    }

    /**
     *  Creates a new {@link Color} object from the specified
     *  red, green, blue and alpha values.
     *
     * @param r The red value (0.0-1.0).
     * @param g The green value (0.0-1.0).
     * @param b The blue value (0.0-1.0).
     * @param a The alpha value (0.0-1.0).
     * @return The new color.
     */
    public static Color color( double r, double g, double b, double a ) {
        return new Color((float) r, (float) g, (float) b, (float) a);
    }

    /**
     *  Tries to parse the supplied string as a color value
     *  based on various formats.
     *
     * @param colorAsString The string to parse.
     * @return The parsed color.
     * @throws IllegalArgumentException If the string could not be parsed.
     * @throws NullPointerException If the string is null.
     */
    public static Color color( final String colorAsString )
    {
        Objects.requireNonNull(colorAsString);
        return Colour.of(colorAsString);
    }

    /**
     * Returns the {@code Font} that the {@code fontString}
     * argument describes.
     * To ensure that this method returns the desired Font,
     * format the {@code fontString} parameter in
     * one of these ways
     *
     * <ul>
     * <li><em>fontname-style-pointsize</em>
     * <li><em>fontname-pointsize</em>
     * <li><em>fontname-style</em>
     * <li><em>fontname</em>
     * <li><em>fontname style pointsize</em>
     * <li><em>fontname pointsize</em>
     * <li><em>fontname style</em>
     * <li><em>fontname</em>
     * </ul>
     * in which <i>style</i> is one of the four
     * case-insensitive strings:
     * {@code "PLAIN"}, {@code "BOLD"}, {@code "BOLDITALIC"}, or
     * {@code "ITALIC"}, and pointsize is a positive decimal integer
     * representation of the point size.
     * For example, if you want a font that is Arial, bold, with
     * a point size of 18, you would call this method with:
     * "Arial-BOLD-18".
     * This is equivalent to calling the Font constructor :
     * {@code new Font("Arial", Font.BOLD, 18);}
     * and the values are interpreted as specified by that constructor.
     * <p>
     * A valid trailing decimal field is always interpreted as the pointsize.
     * Therefore a fontname containing a trailing decimal value should not
     * be used in the fontname only form.
     * <p>
     * If a style name field is not one of the valid style strings, it is
     * interpreted as part of the font name, and the default style is used.
     * <p>
     * Only one of ' ' or '-' may be used to separate fields in the input.
     * The identified separator is the one closest to the end of the string
     * which separates a valid pointsize, or a valid style name from
     * the rest of the string.
     * Null (empty) pointsize and style fields are treated
     * as valid fields with the default value for that field.
     *<p>
     * Some font names may include the separator characters ' ' or '-'.
     * If {@code fontString} is not formed with 3 components, e.g. such that
     * {@code style} or {@code pointsize} fields are not present in
     * {@code fontString}, and {@code fontname} also contains a
     * character determined to be the separator character
     * then these characters where they appear as intended to be part of
     * {@code fontname} may instead be interpreted as separators
     * so the font name may not be properly recognised.
     *
     * <p>
     * The default size is 12 and the default style is PLAIN.
     * If {@code str} does not specify a valid size, the returned
     * {@code Font} has a size of 12.  If {@code fontString} does not
     * specify a valid style, the returned Font has a style of PLAIN.
     * If you do not specify a valid font name in
     * the {@code fontString} argument, this method will return
     * a font with the family name "Dialog".
     * To determine what font family names are available on
     * your system, use the
     * {@link GraphicsEnvironment#getAvailableFontFamilyNames()} method.
     * If {@code fontString} is {@code null}, a new {@code Font}
     * is returned with the family name "Dialog", a size of 12 and a
     * PLAIN style.
     * @param fontString the name of the font, or {@code null}
     * @return the {@code Font} object that {@code fontString} describes.
     * @throws NullPointerException if {@code fontString} is {@code null}
     */
    public static Font font( String fontString ) {
        Objects.requireNonNull(fontString);
        Exception potentialProblem1 = null;
        Exception potentialProblem2 = null;
        String mayBeProperty = System.getProperty(fontString);
        Font font = null;
        try {
            if ( mayBeProperty == null )
                font = Font.decode(fontString);
        } catch( Exception e ) {
            potentialProblem1 = e;
        }
        try {
            if ( mayBeProperty != null )
                font = Font.decode(mayBeProperty);
        } catch( Exception e ) {
            potentialProblem2 = e;
        }
        if ( font == null ) {
            if ( potentialProblem1 != null )
                log.error("Could not parse font string '" + fontString + "' using 'Font.decode(String)'.", potentialProblem1);
            if ( potentialProblem2 != null )
                log.error("Could not parse font string '" + fontString + "' from 'System.getProperty(String)'.", potentialProblem2);

            log.error("Could not parse font string '" + fontString + "' using 'Font.decode(String)' or 'System.getProperty(String)'.", new Throwable());

            try {
                return new Font(fontString, Font.PLAIN, UI.scale(12));
            } catch (Exception e) {
                log.error("Could not create font with name '" + fontString + "' and size 12.", e);
                return new Font(Font.DIALOG, Font.PLAIN, UI.scale(12));
            }
        }
        return font;
    }

}
