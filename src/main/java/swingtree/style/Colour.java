package swingtree.style;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.util.Map;

/**
 * The {@code Colour} class is a refined and more complete/modernized
 * implementation of the {@link Color} class which models colors in the default
 * sRGB color space or colors in arbitrary color spaces identified by a
 * {@link ColorSpace}.
 * <br>
 * The original {@link Color} class is an immutable and value based class
 * (it overrides {@link Object#equals(Object) equals} and {@link Object#hashCode() hashCode})
 * but it is missing so called with-methods, which are a modern way to create
 * updated copies of an object without having to call the full constructor
 * with all the parameters.
 * <p>
 * Another big shortcoming addressed by this class is that it corrects the spelling
 * of {@link Color} as most english speaking countries agree on the word being spelled "colour"
 * (Great Britain, Canada, Australia, New Zealand, South africa, India, Ireland, ...).
 * Let's not kid ourselves, "Colour" has much more class than "Color".
 * <p>
 * But all jokes aside, <b>we are well aware that the convention in Java is to use
 * the American spelling of the word, and because we value convention
 * this is also the preferred spelling in the rest of the SwingTree library. <br>
 * Nonetheless there is a need to cleanly bundle the set of color related features
 * required for developing modern applications in Java in one API,
 * as the alternative so far has been countless utility classes
 * within the codebase of every project. <br>
 * And since Java will probably never get extension methods,
 * this is the most elegant solution to the problem.</b>
 * Here a list of the most useful features additionally provided by this class:
 * <p>
 *     <b>With-Methods</b>
 *     <ul>
 *          <li>{@link #withRed(double)}, {@link #withGreen(double)}, {@link #withBlue(double)}</li>
 *          <li>{@link #withOpacity(double)}, {@link #withAlpha(int)}</li>
 *          <li>{@link #withHue(double)}, {@link #withSaturation(double)}, {@link #withBrightness(double)}</li>
 *          <li>{@link #brighterBy(double)}, {@link #darkerBy(double)}</li>
 *          <li>{@link #saturate()}, {@link #saturateBy(double)}
 *          <li>{@link #desaturate()}, {@link #desaturateBy(double)}</li>
 *          <li>{@link #grayscale()}</li>
 *          <li>{@link #invert()}</li>
 *          <li>...</li>
 *      </ul>
 *      <b>Various Color Constants</b>
 *      <ul>
 *          <li>{@link #TRANSPARENT}</li>
 *          <li>{@link #ALICEBLUE}</li>
 *          <li>{@link #ANTIQUEWHITE}</li>
 *          <li>{@link #AQUA}</li>
 *          <li>{@link #AQUAMARINE}</li>
 *          <li>...</li>
 *      </ul>
 *      Also note that this class overrides and fixes the {@link Color#darker()}
 *      and {@link Color#brighter()} methods. Not only do they now return
 *      a {@code Colour} type, but also use an implementation which updates the
 *      brightness/darkness in terms of the HSB color space.<br>
 *      (The original implementation considers colors like
 *      {@code Color.BLUE}, {@code Color.RED} and {@code Color.GREEN}
 *      to be the brightest possible colors, which is not true in terms
 *      of the much more useful HSB color space modelling.)
 * </p>
 * <p>
 * Besides the RGB values every
 * fully opaque {@code Colour} also has an implicit alpha value of 1.0.
 * But you may also construct a {@code Colour} with an explicit alpha value
 * by using the {@link #Colour(float, float, float, float)} constructor for example.
 * The alpha value defines the transparency of a colour and can be represented by
 * a float value in the range 0.0&nbsp;-&nbsp;1.0 or 0&nbsp;-&nbsp;255.
 * An alpha value of 1.0 or 255 means that the colour is completely
 * opaque and an alpha value of 0 or 0.0 means that the colour is
 * completely transparent.
 * When constructing a {@code Colour} with an explicit alpha or
 * getting the colour/alpha components of a {@code Colour}, the colour
 * components are never premultiplied by the alpha component.
 * <p>
 * The default colour space for the Java 2D(tm) API is sRGB, a proposed
 * standard RGB colour space.  For further information on sRGB,
 * see <A href="http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html">
 * http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html
 * </A>.
 *
 * @version     22 March 2024
 * @author      Daniel Nepp
 * @see         ColorSpace
 * @see         java.awt.AlphaComposite
 */
public final class Colour extends Color
{
    /**
     * A fully transparent color with an ARGB value of #00000000.
     */
    public static final Colour TRANSPARENT = new Colour(0f, 0f, 0f, 0f);

    /**
     * The color alice blue with an RGB value of #F0F8FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0F8FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour ALICEBLUE = new Colour(0.9411765f, 0.972549f, 1.0f);

    /**
     * The color antique white with an RGB value of #FAEBD7
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAEBD7;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour ANTIQUEWHITE = new Colour(0.98039216f, 0.92156863f, 0.84313726f);

    /**
     * The color aqua with an RGB value of #00FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour AQUA = new Colour(0.0f, 1.0f, 1.0f);

    /**
     * The color aquamarine with an RGB value of #7FFFD4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFFD4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour AQUAMARINE = new Colour(0.49803922f, 1.0f, 0.83137256f);

    /**
     * The color azure with an RGB value of #F0FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour AZURE = new Colour(0.9411765f, 1.0f, 1.0f);

    /**
     * The color beige with an RGB value of #F5F5DC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5DC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BEIGE = new Colour(0.9607843f, 0.9607843f, 0.8627451f);

    /**
     * The color bisque with an RGB value of #FFE4C4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4C4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BISQUE = new Colour(1.0f, 0.89411765f, 0.76862746f);

    /**
     * The color black with an RGB value of #000000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#000000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BLACK = new Colour(0.0f, 0.0f, 0.0f);

    /**
     * The color blanched almond with an RGB value of #FFEBCD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEBCD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BLANCHEDALMOND = new Colour(1.0f, 0.92156863f, 0.8039216f);

    /**
     * The color blue with an RGB value of #0000FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BLUE = new Colour(0.0f, 0.0f, 1.0f);

    /**
     * The color blue violet with an RGB value of #8A2BE2
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8A2BE2;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BLUEVIOLET = new Colour(0.5411765f, 0.16862746f, 0.8862745f);

    /**
     * The color brown with an RGB value of #A52A2A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A52A2A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BROWN = new Colour(0.64705884f, 0.16470589f, 0.16470589f);

    /**
     * The color burly wood with an RGB value of #DEB887
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DEB887;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour BURLYWOOD = new Colour(0.87058824f, 0.72156864f, 0.5294118f);

    /**
     * The color cadet blue with an RGB value of #5F9EA0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#5F9EA0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CADETBLUE = new Colour(0.37254903f, 0.61960787f, 0.627451f);

    /**
     * The color chartreuse with an RGB value of #7FFF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFF00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CHARTREUSE = new Colour(0.49803922f, 1.0f, 0.0f);

    /**
     * The color chocolate with an RGB value of #D2691E
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2691E;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CHOCOLATE = new Colour(0.8235294f, 0.4117647f, 0.11764706f);

    /**
     * The color coral with an RGB value of #FF7F50
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF7F50;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CORAL = new Colour(1.0f, 0.49803922f, 0.3137255f);

    /**
     * The color cornflower blue with an RGB value of #6495ED
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6495ED;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CORNFLOWERBLUE = new Colour(0.39215687f, 0.58431375f, 0.92941177f);

    /**
     * The color cornsilk with an RGB value of #FFF8DC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF8DC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CORNSILK = new Colour(1.0f, 0.972549f, 0.8627451f);

    /**
     * The color crimson with an RGB value of #DC143C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DC143C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CRIMSON = new Colour(0.8627451f, 0.078431375f, 0.23529412f);

    /**
     * The color cyan with an RGB value of #00FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour CYAN = new Colour(0.0f, 1.0f, 1.0f);

    /**
     * The color dark blue with an RGB value of #00008B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00008B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKBLUE = new Colour(0.0f, 0.0f, 0.54509807f);

    /**
     * The color dark cyan with an RGB value of #008B8B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008B8B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKCYAN = new Colour(0.0f, 0.54509807f, 0.54509807f);

    /**
     * The color dark goldenrod with an RGB value of #B8860B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B8860B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKGOLDENROD = new Colour(0.72156864f, 0.5254902f, 0.043137256f);

    /**
     * The color dark gray with an RGB value of #A9A9A9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKGRAY = new Colour(0.6627451f, 0.6627451f, 0.6627451f);

    /**
     * The color dark green with an RGB value of #006400
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#006400;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKGREEN = new Colour(0.0f, 0.39215687f, 0.0f);

    /**
     * The color dark grey with an RGB value of #A9A9A9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKGREY             = DARKGRAY;

    /**
     * The color dark khaki with an RGB value of #BDB76B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BDB76B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKKHAKI = new Colour(0.7411765f, 0.7176471f, 0.41960785f);

    /**
     * The color dark magenta with an RGB value of #8B008B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B008B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKMAGENTA = new Colour(0.54509807f, 0.0f, 0.54509807f);

    /**
     * The color dark olive green with an RGB value of #556B2F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#556B2F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKOLIVEGREEN = new Colour(0.33333334f, 0.41960785f, 0.18431373f);

    /**
     * The color dark orange with an RGB value of #FF8C00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF8C00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKORANGE = new Colour(1.0f, 0.54901963f, 0.0f);

    /**
     * The color dark orchid with an RGB value of #9932CC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9932CC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKORCHID = new Colour(0.6f, 0.19607843f, 0.8f);

    /**
     * The color dark red with an RGB value of #8B0000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B0000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKRED = new Colour(0.54509807f, 0.0f, 0.0f);

    /**
     * The color dark salmon with an RGB value of #E9967A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E9967A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKSALMON = new Colour(0.9137255f, 0.5882353f, 0.47843137f);

    /**
     * The color dark sea green with an RGB value of #8FBC8F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8FBC8F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKSEAGREEN = new Colour(0.56078434f, 0.7372549f, 0.56078434f);

    /**
     * The color dark slate blue with an RGB value of #483D8B
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#483D8B;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKSLATEBLUE = new Colour(0.28235295f, 0.23921569f, 0.54509807f);

    /**
     * The color dark slate gray with an RGB value of #2F4F4F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKSLATEGRAY = new Colour(0.18431373f, 0.30980393f, 0.30980393f);

    /**
     * The color dark slate grey with an RGB value of #2F4F4F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKSLATEGREY        = DARKSLATEGRAY;

    /**
     * The color dark turquoise with an RGB value of #00CED1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00CED1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKTURQUOISE = new Colour(0.0f, 0.80784315f, 0.81960785f);

    /**
     * The color dark violet with an RGB value of #9400D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9400D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DARKVIOLET = new Colour(0.5803922f, 0.0f, 0.827451f);

    /**
     * The color deep pink with an RGB value of #FF1493
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF1493;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DEEPPINK = new Colour(1.0f, 0.078431375f, 0.5764706f);

    /**
     * The color deep sky blue with an RGB value of #00BFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00BFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DEEPSKYBLUE = new Colour(0.0f, 0.7490196f, 1.0f);

    /**
     * The color dim gray with an RGB value of #696969
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DIMGRAY = new Colour(0.4117647f, 0.4117647f, 0.4117647f);

    /**
     * The color dim grey with an RGB value of #696969
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DIMGREY              = DIMGRAY;

    /**
     * The color dodger blue with an RGB value of #1E90FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#1E90FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour DODGERBLUE = new Colour(0.11764706f, 0.5647059f, 1.0f);

    /**
     * The color firebrick with an RGB value of #B22222
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B22222;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour FIREBRICK = new Colour(0.69803923f, 0.13333334f, 0.13333334f);

    /**
     * The color floral white with an RGB value of #FFFAF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAF0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour FLORALWHITE = new Colour(1.0f, 0.98039216f, 0.9411765f);

    /**
     * The color forest green with an RGB value of #228B22
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#228B22;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour FORESTGREEN = new Colour(0.13333334f, 0.54509807f, 0.13333334f);

    /**
     * The color fuchsia with an RGB value of #FF00FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour FUCHSIA = new Colour(1.0f, 0.0f, 1.0f);

    /**
     * The color gainsboro with an RGB value of #DCDCDC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DCDCDC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GAINSBORO = new Colour(0.8627451f, 0.8627451f, 0.8627451f);

    /**
     * The color ghost white with an RGB value of #F8F8FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F8F8FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GHOSTWHITE = new Colour(0.972549f, 0.972549f, 1.0f);

    /**
     * The color gold with an RGB value of #FFD700
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFD700;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GOLD = new Colour(1.0f, 0.84313726f, 0.0f);

    /**
     * The color goldenrod with an RGB value of #DAA520
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DAA520;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GOLDENROD = new Colour(0.85490197f, 0.64705884f, 0.1254902f);

    /**
     * The color gray with an RGB value of #808080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GRAY = new Colour(0.5019608f, 0.5019608f, 0.5019608f);

    /**
     * The color green with an RGB value of #008000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GREEN = new Colour(0.0f, 0.5019608f, 0.0f);

    /**
     * The color green yellow with an RGB value of #ADFF2F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADFF2F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GREENYELLOW = new Colour(0.6784314f, 1.0f, 0.18431373f);

    /**
     * The color grey with an RGB value of #808080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour GREY                 = GRAY;

    /**
     * The color honeydew with an RGB value of #F0FFF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFF0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour HONEYDEW = new Colour(0.9411765f, 1.0f, 0.9411765f);

    /**
     * The color hot pink with an RGB value of #FF69B4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF69B4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour HOTPINK = new Colour(1.0f, 0.4117647f, 0.7058824f);

    /**
     * The color indian red with an RGB value of #CD5C5C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD5C5C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour INDIANRED = new Colour(0.8039216f, 0.36078432f, 0.36078432f);

    /**
     * The color indigo with an RGB value of #4B0082
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4B0082;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour INDIGO = new Colour(0.29411766f, 0.0f, 0.50980395f);

    /**
     * The color ivory with an RGB value of #FFFFF0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFF0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour IVORY = new Colour(1.0f, 1.0f, 0.9411765f);

    /**
     * The color khaki with an RGB value of #F0E68C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0E68C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour KHAKI = new Colour(0.9411765f, 0.9019608f, 0.54901963f);

    /**
     * The color lavender with an RGB value of #E6E6FA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E6E6FA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LAVENDER = new Colour(0.9019608f, 0.9019608f, 0.98039216f);

    /**
     * The color lavender blush with an RGB value of #FFF0F5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF0F5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LAVENDERBLUSH = new Colour(1.0f, 0.9411765f, 0.9607843f);

    /**
     * The color lawn green with an RGB value of #7CFC00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7CFC00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LAWNGREEN = new Colour(0.4862745f, 0.9882353f, 0.0f);

    /**
     * The color lemon chiffon with an RGB value of #FFFACD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFACD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LEMONCHIFFON = new Colour(1.0f, 0.98039216f, 0.8039216f);

    /**
     * The color light blue with an RGB value of #ADD8E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADD8E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTBLUE = new Colour(0.6784314f, 0.84705883f, 0.9019608f);

    /**
     * The color light coral with an RGB value of #F08080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F08080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTCORAL = new Colour(0.9411765f, 0.5019608f, 0.5019608f);

    /**
     * The color light cyan with an RGB value of #E0FFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#E0FFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTCYAN = new Colour(0.8784314f, 1.0f, 1.0f);

    /**
     * The color light goldenrod yellow with an RGB value of #FAFAD2
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAFAD2;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTGOLDENRODYELLOW = new Colour(0.98039216f, 0.98039216f, 0.8235294f);

    /**
     * The color light gray with an RGB value of #D3D3D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTGRAY = new Colour(0.827451f, 0.827451f, 0.827451f);

    /**
     * The color light green with an RGB value of #90EE90
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#90EE90;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTGREEN = new Colour(0.5647059f, 0.93333334f, 0.5647059f);

    /**
     * The color light grey with an RGB value of #D3D3D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTGREY            = LIGHTGRAY;

    /**
     * The color light pink with an RGB value of #FFB6C1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFB6C1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTPINK = new Colour(1.0f, 0.7137255f, 0.75686276f);

    /**
     * The color light salmon with an RGB value of #FFA07A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA07A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTSALMON = new Colour(1.0f, 0.627451f, 0.47843137f);

    /**
     * The color light sea green with an RGB value of #20B2AA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#20B2AA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTSEAGREEN = new Colour(0.1254902f, 0.69803923f, 0.6666667f);

    /**
     * The color light sky blue with an RGB value of #87CEFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEFA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTSKYBLUE = new Colour(0.5294118f, 0.80784315f, 0.98039216f);

    /**
     * The color light slate gray with an RGB value of #778899
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTSLATEGRAY = new Colour(0.46666667f, 0.53333336f, 0.6f);

    /**
     * The color light slate grey with an RGB value of #778899
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTSLATEGREY       = LIGHTSLATEGRAY;

    /**
     * The color light steel blue with an RGB value of #B0C4DE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0C4DE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTSTEELBLUE = new Colour(0.6901961f, 0.76862746f, 0.87058824f);

    /**
     * The color light yellow with an RGB value of #FFFFE0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFE0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIGHTYELLOW = new Colour(1.0f, 1.0f, 0.8784314f);

    /**
     * The color lime with an RGB value of #00FF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIME = new Colour(0.0f, 1.0f, 0.0f);

    /**
     * The color lime green with an RGB value of #32CD32
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#32CD32;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LIMEGREEN = new Colour(0.19607843f, 0.8039216f, 0.19607843f);

    /**
     * The color linen with an RGB value of #FAF0E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAF0E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour LINEN = new Colour(0.98039216f, 0.9411765f, 0.9019608f);

    /**
     * The color magenta with an RGB value of #FF00FF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MAGENTA = new Colour(1.0f, 0.0f, 1.0f);

    /**
     * The color maroon with an RGB value of #800000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#800000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MAROON = new Colour(0.5019608f, 0.0f, 0.0f);

    /**
     * The color medium aquamarine with an RGB value of #66CDAA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#66CDAA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMAQUAMARINE = new Colour(0.4f, 0.8039216f, 0.6666667f);

    /**
     * The color medium blue with an RGB value of #0000CD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000CD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMBLUE = new Colour(0.0f, 0.0f, 0.8039216f);

    /**
     * The color medium orchid with an RGB value of #BA55D3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BA55D3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMORCHID = new Colour(0.7294118f, 0.33333334f, 0.827451f);

    /**
     * The color medium purple with an RGB value of #9370DB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9370DB;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMPURPLE = new Colour(0.5764706f, 0.4392157f, 0.85882354f);

    /**
     * The color medium sea green with an RGB value of #3CB371
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#3CB371;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMSEAGREEN = new Colour(0.23529412f, 0.7019608f, 0.44313726f);

    /**
     * The color medium slate blue with an RGB value of #7B68EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#7B68EE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMSLATEBLUE = new Colour(0.48235294f, 0.40784314f, 0.93333334f);

    /**
     * The color medium spring green with an RGB value of #00FA9A
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FA9A;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMSPRINGGREEN = new Colour(0.0f, 0.98039216f, 0.6039216f);

    /**
     * The color medium turquoise with an RGB value of #48D1CC
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#48D1CC;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMTURQUOISE = new Colour(0.28235295f, 0.81960785f, 0.8f);

    /**
     * The color medium violet red with an RGB value of #C71585
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#C71585;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MEDIUMVIOLETRED = new Colour(0.78039217f, 0.08235294f, 0.52156866f);

    /**
     * The color midnight blue with an RGB value of #191970
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#191970;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MIDNIGHTBLUE = new Colour(0.09803922f, 0.09803922f, 0.4392157f);

    /**
     * The color mint cream with an RGB value of #F5FFFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5FFFA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MINTCREAM = new Colour(0.9607843f, 1.0f, 0.98039216f);

    /**
     * The color misty rose with an RGB value of #FFE4E1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4E1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MISTYROSE = new Colour(1.0f, 0.89411765f, 0.88235295f);

    /**
     * The color moccasin with an RGB value of #FFE4B5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4B5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour MOCCASIN = new Colour(1.0f, 0.89411765f, 0.70980394f);

    /**
     * The color navajo white with an RGB value of #FFDEAD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDEAD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour NAVAJOWHITE = new Colour(1.0f, 0.87058824f, 0.6784314f);

    /**
     * The color navy with an RGB value of #000080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#000080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour NAVY = new Colour(0.0f, 0.0f, 0.5019608f);

    /**
     * The color old lace with an RGB value of #FDF5E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FDF5E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour OLDLACE = new Colour(0.99215686f, 0.9607843f, 0.9019608f);

    /**
     * The color olive with an RGB value of #808000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#808000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour OLIVE = new Colour(0.5019608f, 0.5019608f, 0.0f);

    /**
     * The color olive drab with an RGB value of #6B8E23
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6B8E23;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour OLIVEDRAB = new Colour(0.41960785f, 0.5568628f, 0.13725491f);

    /**
     * The color orange with an RGB value of #FFA500
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA500;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour ORANGE = new Colour(1.0f, 0.64705884f, 0.0f);

    /**
     * The color orange red with an RGB value of #FF4500
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF4500;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour ORANGERED = new Colour(1.0f, 0.27058825f, 0.0f);

    /**
     * The color orchid with an RGB value of #DA70D6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DA70D6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour ORCHID = new Colour(0.85490197f, 0.4392157f, 0.8392157f);

    /**
     * The color pale goldenrod with an RGB value of #EEE8AA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#EEE8AA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PALEGOLDENROD = new Colour(0.93333334f, 0.9098039f, 0.6666667f);

    /**
     * The color pale green with an RGB value of #98FB98
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#98FB98;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PALEGREEN = new Colour(0.59607846f, 0.9843137f, 0.59607846f);

    /**
     * The color pale turquoise with an RGB value of #AFEEEE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#AFEEEE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PALETURQUOISE = new Colour(0.6862745f, 0.93333334f, 0.93333334f);

    /**
     * The color pale violet red with an RGB value of #DB7093
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DB7093;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PALEVIOLETRED = new Colour(0.85882354f, 0.4392157f, 0.5764706f);

    /**
     * The color papaya whip with an RGB value of #FFEFD5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEFD5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PAPAYAWHIP = new Colour(1.0f, 0.9372549f, 0.8352941f);

    /**
     * The color peach puff with an RGB value of #FFDAB9
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDAB9;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PEACHPUFF = new Colour(1.0f, 0.85490197f, 0.7254902f);

    /**
     * The color peru with an RGB value of #CD853F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD853F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PERU = new Colour(0.8039216f, 0.52156866f, 0.24705882f);

    /**
     * The color pink with an RGB value of #FFC0CB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFC0CB;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PINK = new Colour(1.0f, 0.7529412f, 0.79607844f);

    /**
     * The color plum with an RGB value of #DDA0DD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#DDA0DD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PLUM = new Colour(0.8666667f, 0.627451f, 0.8666667f);

    /**
     * The color powder blue with an RGB value of #B0E0E6
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0E0E6;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour POWDERBLUE = new Colour(0.6901961f, 0.8784314f, 0.9019608f);

    /**
     * The color purple with an RGB value of #800080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#800080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour PURPLE = new Colour(0.5019608f, 0.0f, 0.5019608f);

    /**
     * The color red with an RGB value of #FF0000
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF0000;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour RED = new Colour(1.0f, 0.0f, 0.0f);

    /**
     * The color rosy brown with an RGB value of #BC8F8F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#BC8F8F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour ROSYBROWN = new Colour(0.7372549f, 0.56078434f, 0.56078434f);

    /**
     * The color royal blue with an RGB value of #4169E1
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4169E1;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour ROYALBLUE = new Colour(0.25490198f, 0.4117647f, 0.88235295f);

    /**
     * The color saddle brown with an RGB value of #8B4513
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B4513;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SADDLEBROWN = new Colour(0.54509807f, 0.27058825f, 0.07450981f);

    /**
     * The color salmon with an RGB value of #FA8072
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FA8072;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SALMON = new Colour(0.98039216f, 0.5019608f, 0.44705883f);

    /**
     * The color sandy brown with an RGB value of #F4A460
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F4A460;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SANDYBROWN = new Colour(0.95686275f, 0.6431373f, 0.3764706f);

    /**
     * The color sea green with an RGB value of #2E8B57
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#2E8B57;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SEAGREEN = new Colour(0.18039216f, 0.54509807f, 0.34117648f);

    /**
     * The color sea shell with an RGB value of #FFF5EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF5EE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SEASHELL = new Colour(1.0f, 0.9607843f, 0.93333334f);

    /**
     * The color sienna with an RGB value of #A0522D
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#A0522D;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SIENNA = new Colour(0.627451f, 0.32156864f, 0.1764706f);

    /**
     * The color silver with an RGB value of #C0C0C0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#C0C0C0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SILVER = new Colour(0.7529412f, 0.7529412f, 0.7529412f);

    /**
     * The color sky blue with an RGB value of #87CEEB
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEEB;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SKYBLUE = new Colour(0.5294118f, 0.80784315f, 0.92156863f);

    /**
     * The color slate blue with an RGB value of #6A5ACD
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#6A5ACD;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SLATEBLUE = new Colour(0.41568628f, 0.3529412f, 0.8039216f);

    /**
     * The color slate gray with an RGB value of #708090
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SLATEGRAY = new Colour(0.4392157f, 0.5019608f, 0.5647059f);

    /**
     * The color slate grey with an RGB value of #708090
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SLATEGREY            = SLATEGRAY;

    /**
     * The color snow with an RGB value of #FFFAFA
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAFA;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SNOW = new Colour(1.0f, 0.98039216f, 0.98039216f);

    /**
     * The color spring green with an RGB value of #00FF7F
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF7F;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour SPRINGGREEN = new Colour(0.0f, 1.0f, 0.49803922f);

    /**
     * The color steel blue with an RGB value of #4682B4
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#4682B4;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour STEELBLUE = new Colour(0.27450982f, 0.50980395f, 0.7058824f);

    /**
     * The color tan with an RGB value of #D2B48C
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2B48C;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour TAN = new Colour(0.8235294f, 0.7058824f, 0.54901963f);

    /**
     * The color teal with an RGB value of #008080
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#008080;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour TEAL = new Colour(0.0f, 0.5019608f, 0.5019608f);

    /**
     * The color thistle with an RGB value of #D8BFD8
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#D8BFD8;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour THISTLE = new Colour(0.84705883f, 0.7490196f, 0.84705883f);

    /**
     * The color tomato with an RGB value of #FF6347
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF6347;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour TOMATO = new Colour(1.0f, 0.3882353f, 0.2784314f);

    /**
     * The color turquoise with an RGB value of #40E0D0
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#40E0D0;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour TURQUOISE = new Colour(0.2509804f, 0.8784314f, 0.8156863f);

    /**
     * The color violet with an RGB value of #EE82EE
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#EE82EE;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour VIOLET = new Colour(0.93333334f, 0.50980395f, 0.93333334f);

    /**
     * The color wheat with an RGB value of #F5DEB3
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5DEB3;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour WHEAT = new Colour(0.9607843f, 0.87058824f, 0.7019608f);

    /**
     * The color white with an RGB value of #FFFFFF
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFFF;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour WHITE = new Colour(1.0f, 1.0f, 1.0f);

    /**
     * The color white smoke with an RGB value of #F5F5F5
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5F5;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour WHITESMOKE = new Colour(0.9607843f, 0.9607843f, 0.9607843f);

    /**
     * The color yellow with an RGB value of #FFFF00
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFF00;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour YELLOW = new Colour(1.0f, 1.0f, 0.0f);

    /**
     * The color yellow green with an RGB value of #9ACD32
     * <div style="border:1px solid black;width:40px;height:20px;background-color:#9ACD32;float:right;margin: 0 10px 0 0"></div>
     */
    public static final Colour YELLOWGREEN = new Colour(0.6039216f, 0.8039216f, 0.19607843f);

    /**
     * Brightness change factor for darker() and brighter() methods.
     */
    private static final double DARKER_BRIGHTER_FACTOR = 0.7;

    /**
     * Saturation change factor for saturate() and desaturate() methods.
     */
    private static final double SATURATE_DESATURATE_FACTOR = 0.7;

    /**
     *  Creates a {@link swingtree.style.Colour} object from a {@link java.awt.Color} object.
     * @param color The color to convert to a colour.
     * @return The colour object.
     */
    public static Colour of( Color color ) {
        return new Colour(color);
    }

    /**
     * Creates an opaque sRGB color with the specified RGB values in the range {@code 0-255}.
     *
     * @param red the red component, in the range {@code 0-255}
     * @param green the green component, in the range {@code 0-255}
     * @param blue the blue component, in the range {@code 0-255}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Colour ofRgb( int red, int green, int blue ) {
        _checkRGB(red, green, blue);
        return new Colour(red, green, blue);
    }

    /**
     * Creates an sRGB color with the specified red, green, blue, and alpha
     * values in the range (0 - 255).
     *
     * @throws IllegalArgumentException if {@code r}, {@code g},
     *        {@code b} or {@code a} are outside of the range
     *        0 to 255, inclusive
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param a the alpha component
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getAlpha
     * @see #getRGB
     */
    public static Colour ofRgba( int r, int g, int b, int a ) {
        return new Colour(r, g, b, a);
    }

    /**
     * Creates an opaque sRGB color with the specified combined RGB value
     * consisting of the red component in bits 16-23, the green component
     * in bits 8-15, and the blue component in bits 0-7.  The actual color
     * used in rendering depends on finding the best match given the
     * color space available for a particular output device.  Alpha is
     * defaulted to 255.
     *
     * @param rgb the combined RGB components
     * @see java.awt.image.ColorModel#getRGBdefault
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getRGB
     */
    public static Colour ofRgb( int rgb ) {
        return new Colour(rgb);
    }

    /**
     * Creates an sRGB color with the specified combined RGBA value consisting
     * of the alpha component in bits 24-31, the red component in bits 16-23,
     * the green component in bits 8-15, and the blue component in bits 0-7.
     * If the {@code hasalpha} argument is {@code false}, alpha
     * is defaulted to 255.
     *
     * @param rgba the combined RGBA components
     * @param hasalpha {@code true} if the alpha bits are valid;
     *        {@code false} otherwise
     * @see java.awt.image.ColorModel#getRGBdefault
     * @see #getRed
     * @see #getGreen
     * @see #getBlue
     * @see #getAlpha
     * @see #getRGB
     */
    public static Colour ofRgb( int rgba, boolean hasalpha ) {
        return new Colour(rgba, hasalpha);
    }

    /**
     * Creates an opaque sRGB color with the specified red, green and blue values
     * in the range {@code 0.0-1.0}.
     *
     * @param red the red component, in the range {@code 0.0-1.0}
     * @param green the green component, in the range {@code 0.0-1.0}
     * @param blue the blue component, in the range {@code 0.0-1.0}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     * @see #red()
     * @see #green()
     * @see #blue()
     */
    public static Colour of( double red, double green, double blue ) {
        return Colour.of((float) red, (float) green, (float) blue);
    }

    /**
     * Creates an opaque sRGB color with the specified red, green and blue values
     * in the range {@code 0.0-1.0}.
     *
     * @param red the red component, in the range {@code 0.0-1.0}
     * @param green the green component, in the range {@code 0.0-1.0}
     * @param blue the blue component, in the range {@code 0.0-1.0}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     * @see #red()
     * @see #green()
     * @see #blue()
     */
    public static Colour of( float red, float green, float blue ) {
        return new Colour(red, green, blue);
    }

    /**
     * Creates an sRGB color with the specified RGB values in the range {@code 0-255},
     * and a given opacity.
     *
     * @param red the red component, in the range {@code 0-255}
     * @param green the green component, in the range {@code 0-255}
     * @param blue the blue component, in the range {@code 0-255}
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     * @see #red()
     * @see #green()
     * @see #blue()
     * @see #opacity()
     */
    public static Colour of( double red, double green, double blue, double opacity ) {
        return Colour.of((float) red, (float) green, (float) blue, (float) opacity);
    }

    /**
     * Creates an sRGB color with the specified RGB values in the range {@code 0-255},
     * and a given opacity.
     *
     * @param red the red component, in the range {@code 0-255}
     * @param green the green component, in the range {@code 0-255}
     * @param blue the blue component, in the range {@code 0-255}
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     * @see #red()
     * @see #green()
     * @see #blue()
     * @see #opacity()
     */
    public static Colour of( float red, float green, float blue, float opacity ) {
        return new Colour(red, green, blue, opacity);
    }

    /**
     * Creates a color in the specified {@code ColourSpace}
     * with the color components specified in the {@code float}
     * array and the specified alpha.  The number of components is
     * determined by the type of the {@code ColourSpace}.  For
     * example, RGB requires 3 components, but CMYK requires 4
     * components.
     * @param cspace the {@code ColourSpace} to be used to
     *                  interpret the components
     * @param components an arbitrary number of color components
     *                      that is compatible with the {@code ColourSpace}
     * @param alpha alpha value
     * @throws IllegalArgumentException if any of the values in the
     *         {@code components} array or {@code alpha} is
     *         outside of the range 0.0 to 1.0
     * @see #getComponents
     * @see #getColorComponents
     */
    public static Colour of( ColorSpace cspace, float[] components, float alpha ) {
        return new Colour(cspace, components, alpha);
    }

    /**
     * Creates an sRGB color with the specified RGB values in the range {@code 0-255},
     * and a given opacity.
     *
     * @param red the red component, in the range {@code 0-255}
     * @param green the green component, in the range {@code 0-255}
     * @param blue the blue component, in the range {@code 0-255}
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Colour ofRgb( int red, int green, int blue, double opacity ) {
        _checkRGB(red, green, blue);
        return Colour.of(
                red / 255.0,
                green / 255.0,
                blue / 255.0,
                opacity);
    }

    /**
     * This is a shortcut for {@code rgb(gray, gray, gray)}.
     * @param gray the gray component, in the range {@code 0-255}
     * @return the {@code Colour}
     */
    public static Colour ofGrayRgb( int gray ) {
        return ofRgb(gray, gray, gray);
    }

    /**
     * This is a shortcut for {@code rgb(gray, gray, gray, opacity)}.
     * @param gray the gray component, in the range {@code 0-255}
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Colour}
     */
    public static Colour ofGrayRgb( int gray, double opacity ) {
        return ofRgb(gray, gray, gray, opacity);
    }

    /**
     * Creates a grey color.
     * @param gray color on gray scale in the range
     *             {@code 0.0} (black) - {@code 1.0} (white).
     * @param opacity the opacity component, in the range {@code 0.0-1.0}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Colour ofGray( double gray, double opacity ) {
        return Colour.of(gray, gray, gray, opacity);
    }

    /**
     * Creates an opaque grey color.
     * @param gray color on gray scale in the range
     *             {@code 0.0} (black) - {@code 1.0} (white).
     * @return the {@code Colour}
     * @throws IllegalArgumentException if any value is out of range
     */
    public static Colour ofGray( double gray ) {
        return ofGray(gray, 1.0);
    }

    /**
     * Creates a {@code Colour} based on the specified values in the HSB color model,
     * and a given opacity.
     *
     * @param hue the hue, in degrees
     * @param saturation the saturation, {@code 0.0 to 1.0}
     * @param brightness the brightness, {@code 0.0 to 1.0}
     * @param opacity the opacity, {@code 0.0 to 1.0}
     * @return the {@code Colour}
     * @throws IllegalArgumentException if {@code saturation}, {@code brightness} or
     *         {@code opacity} are out of range
     */
    public static Colour ofHsb(double hue, double saturation, double brightness, double opacity) {
        _checkSB(saturation, brightness);
        double[] rgb = HSBtoRGB(hue, saturation, brightness);
        return Colour.of(rgb[0], rgb[1], rgb[2], opacity);
    }

    private static void _checkRGB( int red, int green, int blue ) {
        if (red < 0 || red > 255) {
            throw new IllegalArgumentException("Colour.rgb's red parameter (" + red + ") expects color values 0-255");
        }
        if (green < 0 || green > 255) {
            throw new IllegalArgumentException("Colour.rgb's green parameter (" + green + ") expects color values 0-255");
        }
        if (blue < 0 || blue > 255) {
            throw new IllegalArgumentException("Colour.rgb's blue parameter (" + blue + ") expects color values 0-255");
        }
    }

    private static void _checkSB( double saturation, double brightness ) {
        if (saturation < 0.0 || saturation > 1.0) {
            throw new IllegalArgumentException("Colour.hsb's saturation parameter (" + saturation + ") expects values 0.0-1.0");
        }
        if (brightness < 0.0 || brightness > 1.0) {
            throw new IllegalArgumentException("Colour.hsb's brightness parameter (" + brightness + ") expects values 0.0-1.0");
        }
    }

    private Colour(Color color) {
        super(color.getRGB());
    }

    private Colour(int r, int g, int b) {
        super(r, g, b);
    }

    private Colour(int r, int g, int b, int a) {
        super(r, g, b, a);
    }

    private Colour(int rgb) {
        super(rgb);
    }

    private Colour(int rgba, boolean hasalpha) {
        super(rgba, hasalpha);
    }

    private Colour(float r, float g, float b) {
        super(r, g, b);
    }

    private Colour(float r, float g, float b, float a) {
        super(r, g, b, a);
    }

    private Colour(ColorSpace cspace, float[] components, float alpha) {
        super(cspace, components, alpha);
    }

    /**
     * The red component of the {@code Colour}, in the range {@code 0.0-1.0}.
     * If you want to get the red component in the range {@code 0-255}, use the
     * {@link #getRed()} method.
     *
     * @return the red component of the {@code Colour}, in the range {@code 0.0-1.0}
     * @see #getRed()
     */
    public double red() {
        return getRed() / 255.0;
    }

    /**
     * The green component of the {@code Colour}, in the range {@code 0.0-1.0}.
     * If you want to get the green component in the range {@code 0-255}, use the
     * {@link #getGreen()} method.
     *
     * @return the green component of the {@code Colour}, in the range {@code 0.0-1.0}
     * @see #getGreen()
     */
    public double green() {
        return getGreen() / 255.0;
    }

    /**
     * The blue component of the {@code Colour}, in the range {@code 0.0-1.0}.
     * If you want to get the blue component in the range {@code 0-255}, use the
     * {@link #getBlue()} method.
     *
     * @return the blue component of the {@code Colour}, in the range {@code 0.0-1.0}
     * @see #getBlue()
     */
    public double blue() {
        return getBlue() / 255.0;
    }

    /**
     * The opacity of the {@code Colour}, in the range {@code 0.0-1.0}.
     * If you want to get the opacity in the form of the alpha component in the
     * range {@code 0-255}, use the {@link #getAlpha()} method.
     *
     * @return the opacity of the {@code Colour}, in the range {@code 0.0-1.0}
     * @see #getAlpha()
     */
    public double opacity() {
        return getAlpha() / 255.0;
    }

    /**
     * Gets the hue component of this {@code Colour}.
     * @return Hue value in the range in the range {@code 0.0-360.0}.
     */
    public double hue() {
        return RGBtoHSB(red(), green(), blue())[0];
    }

    /**
     * Gets the saturation component of this {@code Colour}.
     * @return Saturation value in the range in the range {@code 0.0-1.0}.
     */
    public double saturation() {
        return RGBtoHSB(red(), green(), blue())[1];
    }

    /**
     * Gets the brightness component of this {@code Colour}.
     * @return Brightness value in the range in the range {@code 0.0-1.0}.
     */
    public double brightness() {
        return RGBtoHSB(red(), green(), blue())[2];
    }

    /**
     * Creates a new {@code Colour} based on this {@code Colour} with hue,
     * saturation, brightness and opacity values altered. Hue is shifted
     * about the given value and normalized into its natural range, the
     * other components' values are multiplied by the given factors and
     * clipped into their ranges.
     * <p>
     * Increasing brightness of black color is allowed by using an arbitrary,
     * very small source brightness instead of zero.
     * @param hueShift the hue shift
     * @param saturationFactor the saturation factor
     * @param brightnessFactor the brightness factor
     * @param opacityFactor the opacity factor
     * @return a {@code Colour} based based on this {@code Colour} with hue,
     * saturation, brightness and opacity values altered.
     */
    private Colour _deriveColour(
        double hueShift,
        double saturationFactor,
        double brightnessFactor,
        double opacityFactor
    ) {
        double[] hsb = RGBtoHSB(red(), green(), blue());

        /* Allow brightness increase of black color */
        double b = hsb[2];
        if (b == 0 && brightnessFactor > 1.0) {
            b = 0.05;
        }

        /* the tail "+ 360) % 360" solves shifts into negative numbers */
        double h = (((hsb[0] + hueShift) % 360) + 360) % 360;
        double s = Math.max(Math.min(hsb[1] * saturationFactor, 1.0), 0.0);
        b = Math.max(Math.min(b * brightnessFactor, 1.0), 0.0);
        double a = Math.max(Math.min(opacity() * opacityFactor, 1.0), 0.0);
        return ofHsb(h, s, b, a);
    }

    /**
     * Creates a new colour that is a brighter version of this colour.
     * @return A colour that is a brighter version of this colour.
     */
    @Override
    public Colour brighter() {
        return _deriveColour(0, 1.0, 1.0 / DARKER_BRIGHTER_FACTOR, 1.0);
    }

    /**
     * Creates an updated colour whose brightness is increased by the specified factor.
     * @param factor The factor by which to increase the brightness.
     */
    public Colour brighterBy(double factor) {
        return _deriveColour(0, 1.0, 1.0 / factor, 1.0);
    }

    /**
     * Creates a new colour that is a darker version of this colour.
     * @return a colour that is a darker version of this colour
     */
    @Override
    public Colour darker() {
        return _deriveColour(0, 1.0, DARKER_BRIGHTER_FACTOR, 1.0);
    }

    /**
     * Creates an updated colour whose brightness is decreased by the specified factor.
     * @param factor The factor by which to decrease the brightness.
     */
    public Colour darkerBy(double factor) {
        return _deriveColour(0, 1.0, factor, 1.0);
    }

    /**
     *  Provides an updated colour that is a more saturated version of this colour.
     * @return A colour that is a more saturated version of this colour.
     */
    public Colour saturate() {
        return _deriveColour(0, 1.0 / SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
    }

    public Colour saturateBy(double factor) {
        return _deriveColour(0, 1.0 / factor, 1.0, 1.0);
    }

    /**
     * Creates a new colour that is a less saturated version of this colour.
     * @return A colour that is a less saturated version of this colour.
     */
    public Colour desaturate() {
        return _deriveColour(0, SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
    }

    public Colour desaturateBy(double factor) {
        return _deriveColour(0, factor, 1.0, 1.0);
    }

    /**
     * Creates an updated colour that is grayscale equivalent of this colour.
     * Opacity is preserved.
     * @return A colour that is grayscale equivalent of this colour
     */
    public Colour grayscale() {
        double gray = 0.2126 * red() + 0.7152 * green() + 0.0722 * blue();
        return Colour.of(gray, gray, gray, opacity());
    }

    /**
     * Creates a new colour that is inversion of this colour.
     * Opacity is preserved.
     * @return A colour that is inversion of this colour.
     */
    public Colour invert() {
        return Colour.of(1.0 - red(), 1.0 - green(), 1.0 - blue(), opacity());
    }

    /**
     *  Returns an updated version of this colour with the red component changed
     *  to the specified value in the range {@code 0.0-1.0}.
     *  The number {@code 0.0} represents no red, and {@code 1.0} represents
     *  full red.
     *  
     *  @param red The red component, in the range {@code 0.0-1.0}.
     *  @return A new {@code Colour} object with the red component changed.
     * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
     * @see #red()
     */
    public Colour withRed(double red) {
        return Colour.of(red, green(), blue(), opacity());
    }

    /**
     *  Returns an updated version of this colour with the green component changed
     *  to the specified value in the range {@code 0.0-1.0}.
     *  A number of {@code 0.0} represents no green, and {@code 1.0} represents
     *  green to the maximum extent.
     *  
     *  @param green The green component, in the range {@code 0.0-1.0}
     *  @return A new {@code Colour} object with the green component changed
     * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
     * @see #green()
     */
    public Colour withGreen(double green) {
        return Colour.of(red(), green, blue(), opacity());
    }

    /**
     *  Returns an updated version of this colour with the blue component changed
     *  to the specified value in the range {@code 0.0-1.0}.
     *  A value closer to {@code 0.0} represents no blue, and closer to {@code 1.0}
     *  represents blue to the maximum extent possible.
     *  
     *  @param blue The blue component, in the range {@code 0.0-1.0}
     *  @return A new {@code Colour} object with the blue component changed
     * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
     * @see #blue()
     */
    public Colour withBlue(double blue) {
        return Colour.of(red(), green(), blue, opacity());
    }

    /**
     *  Returns an updated version of this colour with the opacity changed
     *  to the specified value in the range {@code 0.0-1.0}.
     *  A value closer to {@code 0.0} represents a fully transparent colour,
     *  and closer to {@code 1.0} represents a fully opaque colour.
     *  
     *  @param opacity The opacity component, in the range {@code 0.0-1.0}
     *  @return A new {@code Colour} object with the opacity changed
     * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
     * @see #opacity()
     */
    public Colour withOpacity(double opacity) {
        return Colour.of(red(), green(), blue(), opacity);
    }

    /**
     *  Creates and returns an updated version of this colour with the 
     *  alpha component changed to the specified value in the range {@code 0-255}.
     *  A value closer to {@code 0} represents a fully transparent colour,
     *  and closer to {@code 255} represents a fully opaque colour.
     *  
     * @param alpha The alpha component, in the range {@code 0-255}.
     * @return A new {@code Colour} object with the alpha component changed
     */
    public Colour withAlpha( int alpha ) {
        return new Colour(getRed(), getGreen(), getBlue(), alpha);
    }

    /**
     *  Returns an updated version of this colour with the hue changed
     *  to the specified value in the range {@code 0.0-360.0}.
     *  A value closer to {@code 0.0} represents red, and closer to {@code 360.0}
     *  represents red again.
     *  
     *  @param hue The hue component, in the range {@code 0.0-360.0}
     *  @return A new {@code Colour} object with the hue changed
     * @throws IllegalArgumentException If the value is out of range (0.0-360.0)
     * @see #hue()
     */
    public Colour withHue(double hue) {
        return Colour.ofHsb(hue, saturation(), brightness(), opacity());
    }

    /**
     *  Returns an updated version of this colour with the saturation changed
     *  to the specified value in the range {@code 0.0-1.0}.
     *  A value closer to {@code 0.0} represents a shade of grey, and closer to
     *  {@code 1.0} represents a fully saturated colour.
     *  
     *  @param saturation The saturation component, in the range {@code 0.0-1.0}
     *  @return A new {@code Colour} object with the saturation changed
     * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
     * @see #saturation()
     */
    public Colour withSaturation(double saturation) {
        return Colour.ofHsb(hue(), saturation, brightness(), opacity());
    }

    /**
     *  Returns an updated version of this colour with the brightness changed
     *  to the specified value in the range {@code 0.0-1.0}.
     *  A value closer to {@code 0.0} represents black, and closer to {@code 1.0}
     *  represents white.
     *  
     *  @param brightness The brightness component, in the range {@code 0.0-1.0}
     *  @return A new {@code Colour} object with the brightness changed
     * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
     * @see #brightness()
     */
    public Colour withBrightness( double brightness ) {
        return Colour.ofHsb(hue(), saturation(), brightness, opacity());
    }


    /*
     * Named colors moved to nested class to initialize them only when they
     * are needed.
     */
    private static final class NamedColours {

        private static Colour get(String name) {
            return NAMED_COLOURS.get(name);
        }

        private static final Map<String, Colour> NAMED_COLOURS = Map.ofEntries(
                Map.entry("aliceblue",            ALICEBLUE),
                Map.entry("antiquewhite",         ANTIQUEWHITE),
                Map.entry("aqua",                 AQUA),
                Map.entry("aquamarine",           AQUAMARINE),
                Map.entry("azure",                AZURE),
                Map.entry("beige",                BEIGE),
                Map.entry("bisque",               BISQUE),
                Map.entry("black",                BLACK),
                Map.entry("blanchedalmond",       BLANCHEDALMOND),
                Map.entry("blue",                 BLUE),
                Map.entry("blueviolet",           BLUEVIOLET),
                Map.entry("brown",                BROWN),
                Map.entry("burlywood",            BURLYWOOD),
                Map.entry("cadetblue",            CADETBLUE),
                Map.entry("chartreuse",           CHARTREUSE),
                Map.entry("chocolate",            CHOCOLATE),
                Map.entry("coral",                CORAL),
                Map.entry("cornflowerblue",       CORNFLOWERBLUE),
                Map.entry("cornsilk",             CORNSILK),
                Map.entry("crimson",              CRIMSON),
                Map.entry("cyan",                 CYAN),
                Map.entry("darkblue",             DARKBLUE),
                Map.entry("darkcyan",             DARKCYAN),
                Map.entry("darkgoldenrod",        DARKGOLDENROD),
                Map.entry("darkgray",             DARKGRAY),
                Map.entry("darkgreen",            DARKGREEN),
                Map.entry("darkgrey",             DARKGREY),
                Map.entry("darkkhaki",            DARKKHAKI),
                Map.entry("darkmagenta",          DARKMAGENTA),
                Map.entry("darkolivegreen",       DARKOLIVEGREEN),
                Map.entry("darkorange",           DARKORANGE),
                Map.entry("darkorchid",           DARKORCHID),
                Map.entry("darkred",              DARKRED),
                Map.entry("darksalmon",           DARKSALMON),
                Map.entry("darkseagreen",         DARKSEAGREEN),
                Map.entry("darkslateblue",        DARKSLATEBLUE),
                Map.entry("darkslategray",        DARKSLATEGRAY),
                Map.entry("darkslategrey",        DARKSLATEGREY),
                Map.entry("darkturquoise",        DARKTURQUOISE),
                Map.entry("darkviolet",           DARKVIOLET),
                Map.entry("deeppink",             DEEPPINK),
                Map.entry("deepskyblue",          DEEPSKYBLUE),
                Map.entry("dimgray",              DIMGRAY),
                Map.entry("dimgrey",              DIMGREY),
                Map.entry("dodgerblue",           DODGERBLUE),
                Map.entry("firebrick",            FIREBRICK),
                Map.entry("floralwhite",          FLORALWHITE),
                Map.entry("forestgreen",          FORESTGREEN),
                Map.entry("fuchsia",              FUCHSIA),
                Map.entry("gainsboro",            GAINSBORO),
                Map.entry("ghostwhite",           GHOSTWHITE),
                Map.entry("gold",                 GOLD),
                Map.entry("goldenrod",            GOLDENROD),
                Map.entry("gray",                 GRAY),
                Map.entry("green",                GREEN),
                Map.entry("greenyellow",          GREENYELLOW),
                Map.entry("grey",                 GREY),
                Map.entry("honeydew",             HONEYDEW),
                Map.entry("hotpink",              HOTPINK),
                Map.entry("indianred",            INDIANRED),
                Map.entry("indigo",               INDIGO),
                Map.entry("ivory",                IVORY),
                Map.entry("khaki",                KHAKI),
                Map.entry("lavender",             LAVENDER),
                Map.entry("lavenderblush",        LAVENDERBLUSH),
                Map.entry("lawngreen",            LAWNGREEN),
                Map.entry("lemonchiffon",         LEMONCHIFFON),
                Map.entry("lightblue",            LIGHTBLUE),
                Map.entry("lightcoral",           LIGHTCORAL),
                Map.entry("lightcyan",            LIGHTCYAN),
                Map.entry("lightgoldenrodyellow", LIGHTGOLDENRODYELLOW),
                Map.entry("lightgray",            LIGHTGRAY),
                Map.entry("lightgreen",           LIGHTGREEN),
                Map.entry("lightgrey",            LIGHTGREY),
                Map.entry("lightpink",            LIGHTPINK),
                Map.entry("lightsalmon",          LIGHTSALMON),
                Map.entry("lightseagreen",        LIGHTSEAGREEN),
                Map.entry("lightskyblue",         LIGHTSKYBLUE),
                Map.entry("lightslategray",       LIGHTSLATEGRAY),
                Map.entry("lightslategrey",       LIGHTSLATEGREY),
                Map.entry("lightsteelblue",       LIGHTSTEELBLUE),
                Map.entry("lightyellow",          LIGHTYELLOW),
                Map.entry("lime",                 LIME),
                Map.entry("limegreen",            LIMEGREEN),
                Map.entry("linen",                LINEN),
                Map.entry("magenta",              MAGENTA),
                Map.entry("maroon",               MAROON),
                Map.entry("mediumaquamarine",     MEDIUMAQUAMARINE),
                Map.entry("mediumblue",           MEDIUMBLUE),
                Map.entry("mediumorchid",         MEDIUMORCHID),
                Map.entry("mediumpurple",         MEDIUMPURPLE),
                Map.entry("mediumseagreen",       MEDIUMSEAGREEN),
                Map.entry("mediumslateblue",      MEDIUMSLATEBLUE),
                Map.entry("mediumspringgreen",    MEDIUMSPRINGGREEN),
                Map.entry("mediumturquoise",      MEDIUMTURQUOISE),
                Map.entry("mediumvioletred",      MEDIUMVIOLETRED),
                Map.entry("midnightblue",         MIDNIGHTBLUE),
                Map.entry("mintcream",            MINTCREAM),
                Map.entry("mistyrose",            MISTYROSE),
                Map.entry("moccasin",             MOCCASIN),
                Map.entry("navajowhite",          NAVAJOWHITE),
                Map.entry("navy",                 NAVY),
                Map.entry("oldlace",              OLDLACE),
                Map.entry("olive",                OLIVE),
                Map.entry("olivedrab",            OLIVEDRAB),
                Map.entry("orange",               ORANGE),
                Map.entry("orangered",            ORANGERED),
                Map.entry("orchid",               ORCHID),
                Map.entry("palegoldenrod",        PALEGOLDENROD),
                Map.entry("palegreen",            PALEGREEN),
                Map.entry("paleturquoise",        PALETURQUOISE),
                Map.entry("palevioletred",        PALEVIOLETRED),
                Map.entry("papayawhip",           PAPAYAWHIP),
                Map.entry("peachpuff",            PEACHPUFF),
                Map.entry("peru",                 PERU),
                Map.entry("pink",                 PINK),
                Map.entry("plum",                 PLUM),
                Map.entry("powderblue",           POWDERBLUE),
                Map.entry("purple",               PURPLE),
                Map.entry("red",                  RED),
                Map.entry("rosybrown",            ROSYBROWN),
                Map.entry("royalblue",            ROYALBLUE),
                Map.entry("saddlebrown",          SADDLEBROWN),
                Map.entry("salmon",               SALMON),
                Map.entry("sandybrown",           SANDYBROWN),
                Map.entry("seagreen",             SEAGREEN),
                Map.entry("seashell",             SEASHELL),
                Map.entry("sienna",               SIENNA),
                Map.entry("silver",               SILVER),
                Map.entry("skyblue",              SKYBLUE),
                Map.entry("slateblue",            SLATEBLUE),
                Map.entry("slategray",            SLATEGRAY),
                Map.entry("slategrey",            SLATEGREY),
                Map.entry("snow",                 SNOW),
                Map.entry("springgreen",          SPRINGGREEN),
                Map.entry("steelblue",            STEELBLUE),
                Map.entry("tan",                  TAN),
                Map.entry("teal",                 TEAL),
                Map.entry("thistle",              THISTLE),
                Map.entry("tomato",               TOMATO),
                Map.entry("transparent",          TRANSPARENT),
                Map.entry("turquoise",            TURQUOISE),
                Map.entry("violet",               VIOLET),
                Map.entry("wheat",                WHEAT),
                Map.entry("white",                WHITE),
                Map.entry("whitesmoke",           WHITESMOKE),
                Map.entry("yellow",               YELLOW),
                Map.entry("yellowgreen",          YELLOWGREEN));
    }

    private static double[] HSBtoRGB(double hue, double saturation, double brightness) {
        // normalize the hue
        double normalizedHue = ((hue % 360) + 360) % 360;
        hue = normalizedHue/360;

        double r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = brightness;
        } else {
            double h = (hue - Math.floor(hue)) * 6.0;
            double f = h - java.lang.Math.floor(h);
            double p = brightness * (1.0 - saturation);
            double q = brightness * (1.0 - saturation * f);
            double t = brightness * (1.0 - (saturation * (1.0 - f)));
            switch ((int) h) {
                case 0:
                    r = brightness;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = brightness;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = brightness;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = brightness;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = brightness;
                    break;
                case 5:
                    r = brightness;
                    g = p;
                    b = q;
                    break;
            }
        }
        double[] f = new double[3];
        f[0] = r;
        f[1] = g;
        f[2] = b;
        return f;
    }

    private static double[] RGBtoHSB(double r, double g, double b) {
        double hue, saturation, brightness;
        double[] hsbvals = new double[3];
        double cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        double cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = cmax;
        if (cmax != 0)
            saturation = (cmax - cmin) / cmax;
        else
            saturation = 0;

        if (saturation == 0) {
            hue = 0;
        } else {
            double redc = (cmax - r) / (cmax - cmin);
            double greenc = (cmax - g) / (cmax - cmin);
            double bluec = (cmax - b) / (cmax - cmin);
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0 + redc - bluec;
            else
                hue = 4.0 + greenc - redc;
            hue = hue / 6.0;
            if (hue < 0)
                hue = hue + 1.0;
        }
        hsbvals[0] = hue * 360;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

}
