package swingtree;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/*
 * Named colors moved to nested class to initialize them only when they
 * are needed.
 */
final class ColorUtility {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ColorUtility.class);

    static UI.Color deriveColor(
            final double hueShift,
            final double saturationFactor,
            final double brightnessFactor,
            final double opacityFactor,
            final double red,
            final double green,
            final double blue,
            final double opacity
    ) {
        double[] hsb = ColorUtility.RGBtoHSB(red, green, blue);

        /* Allow brightness increase of black color */
        double b = hsb[2];
        if (b == 0 && brightnessFactor > 1.0) {
            b = 0.05;
        }

        /* the tail "+ 360) % 360" solves shifts into negative numbers */
        double h = (((hsb[0] + hueShift) % 360) + 360) % 360;
        double s = Math.max(Math.min(hsb[1] * saturationFactor, 1.0), 0.0);
        b = Math.max(Math.min(b * brightnessFactor, 1.0), 0.0);
        double a = Math.max(Math.min(opacity * opacityFactor, 1.0), 0.0);
        return UI.Color.ofHsb(h, s, b, a);
    }

    static double[] HSBtoRGB(double hue, double saturation, double brightness) {
        // normalize the hue
        double normalizedHue = ((hue % 360) + 360) % 360;
        hue = normalizedHue / 360;

        double r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = brightness;
        } else {
            double h = (hue - Math.floor(hue)) * 6.0;
            double f = h - Math.floor(h);
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

    static double[] RGBtoHSB(double r, double g, double b) {
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


    static UI.Color parseColor(final String colorAsString) {
        // First some cleanup
        final String colorString = colorAsString.trim();

        if (colorAsString.isEmpty())
            return UI.Color.UNDEFINED;

        if (colorString.startsWith("#"))
            return UI.Color.of(java.awt.Color.decode(colorString));

        if (colorString.startsWith("0x"))
            return UI.Color.of(java.awt.Color.decode(colorString));

        if (colorString.startsWith("rgb")) {
            // We have an rgb() or rgba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if (start < 0 || end < 0 || end < start) {
                log.error(SwingTree.get().loggingMarker(), "Invalid rgb() or rgba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            String[] parts = colorString.substring(start + 1, end).split(",", -1);
            if (parts.length < 3 || parts.length > 4) {
                log.error(SwingTree.get().loggingMarker(), "Invalid rgb() or rgba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            for (int i = 0; i < parts.length; i++)
                parts[i] = parts[i].trim();

            int[] values = new int[parts.length];

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.endsWith("%")) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Integer.parseInt(part);
                    if (values[i] < 0 || values[i] > 100) {
                        log.error(SwingTree.get().loggingMarker(), "Invalid rgb() or rgba() color: " + colorString, new Throwable());
                        return UI.Color.UNDEFINED;
                    }
                    values[i] = (int) Math.ceil(values[i] * 2.55);
                } else if (part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])"))
                    values[i] = (int) (Float.parseFloat(part) * 255);
                else
                    values[i] = Integer.parseInt(part);
            }
            int r = values[0];
            int g = values[1];
            int b = values[2];
            int a = values.length == 4 ? values[3] : 255;
            return UI.Color.ofRgba(r, g, b, a);
        }

        if (colorString.startsWith("hsb")) {
            // We have an hsb() or hsba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if (start < 0 || end < 0 || end < start) {
                log.error(SwingTree.get().loggingMarker(), "Invalid hsb() or hsba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            String[] parts = colorString.substring(start + 1, end).split(",", -1);
            if (parts.length < 3 || parts.length > 4) {
                log.error(SwingTree.get().loggingMarker(), "Invalid hsb() or hsba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            for (int i = 0; i < parts.length; i++)
                parts[i] = parts[i].trim();

            float[] values = new float[parts.length];

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.endsWith("%")) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if (values[i] < 0 || values[i] > 100) {
                        log.error(
                                "Invalid hsb() or hsba() string '" + colorString + "', " +
                                        "value '" + part + "' out of range.",
                                new Throwable()
                        );
                        return UI.Color.UNDEFINED;
                    }
                    values[i] = values[i] / 100.0f;
                } else if (part.endsWith("°")) {
                    if (i > 0) {
                        log.error(
                                "Invalid hsb() or hsba() string '" + colorString + "', " +
                                        "unexpected degree symbol in '" + part + "' (only allowed for hue)",
                                new Throwable()
                        );
                        return UI.Color.UNDEFINED;
                    }

                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if (values[i] < 0 || values[i] > 360) {
                        log.error(
                                "Invalid hsb() or hsba() string '" + colorString + "', " +
                                        "hue value '" + part + "' out of range.",
                                new Throwable()
                        );
                        return UI.Color.UNDEFINED;
                    }
                    values[i] = values[i] / 360.0f;
                } else if (part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])"))
                    values[i] = Float.parseFloat(part);
                else
                    values[i] = Integer.parseInt(part);
            }

            float h = values[0];
            float s = values[1];
            float b = values[2];
            float a = values.length == 4 ? values[3] : 1.0f;
            java.awt.Color c = java.awt.Color.getHSBColor(h, s, b);
            return UI.Color.ofRgba(c.getRed(), c.getGreen(), c.getBlue(), (int) (a * 255));
        }

        {
            String maybeWord = colorString.toLowerCase(Locale.ENGLISH);
            boolean transparent = false;

            if (maybeWord.startsWith("transparent")) {
                transparent = true;
                maybeWord = maybeWord.substring(11).trim();
            }

            // Let's try a few common color names
            UI.Color color = _tryFromName(maybeWord);
            if (color == null && maybeWord.startsWith("darker")) {
                color = _tryFromName(maybeWord.substring(6).trim());
                if (color != null)
                    color = color.darker();
            }
            if (color == null && maybeWord.startsWith("dark")) {
                color = _tryFromName(maybeWord.substring(4).trim());
                if (color != null)
                    color = color.darker();
            }
            if (color == null && maybeWord.startsWith("lighter")) {
                color = _tryFromName(maybeWord.substring(7).trim());
                if (color != null)
                    color = color.brighter();
            }
            if (color == null && maybeWord.startsWith("light")) {
                color = _tryFromName(maybeWord.substring(5).trim());
                if (color != null)
                    color = color.brighter();
            }
            if (color == null && maybeWord.startsWith("brighter")) {
                color = _tryFromName(maybeWord.substring(8).trim());
                if (color != null)
                    color = color.brighter();
            }
            if (color == null && maybeWord.startsWith("bright")) {
                color = _tryFromName(maybeWord.substring(6).trim());
                if (color != null)
                    color = color.brighter();
            }

            if (color != null) {
                if (transparent)
                    return UI.Color.ofRgba(color.getRed(), color.getGreen(), color.getBlue(), 255 / 2);
                else
                    return color;
            } else if (transparent)
                return UI.Color.TRANSPARENT;
        }

        // Let's try to find it as a system property
        UI.Color foundInSystemProperties = null;
        try {
            java.awt.Color found = java.awt.Color.getColor(colorString);
            if (found != null && !(found instanceof UI.Color))
                foundInSystemProperties = UI.Color.of(found);
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        if (foundInSystemProperties != null)
            return foundInSystemProperties;

        return UI.Color.UNDEFINED;
    }

    private static UI.@Nullable Color _tryFromName(String maybeColorName) {
        try {
            String lowerCaseName = maybeColorName.toLowerCase(Locale.ENGLISH);
            return ColorUtility.get(lowerCaseName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    static UI.@Nullable Color get(String name) {
        return NAMED_COLOURS.get(name);
    }

    private static final Map<String, UI.Color> NAMED_COLOURS = new HashMap<>();
    static {
        NAMED_COLOURS.put("aliceblue", UI.Color.ALICEBLUE);
        NAMED_COLOURS.put("antiquewhite", UI.Color.ANTIQUEWHITE);
        NAMED_COLOURS.put("aqua", UI.Color.AQUA);
        NAMED_COLOURS.put("aquamarine", UI.Color.AQUAMARINE);
        NAMED_COLOURS.put("azure", UI.Color.AZURE);
        NAMED_COLOURS.put("beige", UI.Color.BEIGE);
        NAMED_COLOURS.put("bisque", UI.Color.BISQUE);
        NAMED_COLOURS.put("black", UI.Color.BLACK);
        NAMED_COLOURS.put("blanchedalmond", UI.Color.BLANCHEDALMOND);
        NAMED_COLOURS.put("blue", UI.Color.BLUE);
        NAMED_COLOURS.put("blueviolet", UI.Color.BLUEVIOLET);
        NAMED_COLOURS.put("brown", UI.Color.BROWN);
        NAMED_COLOURS.put("burlywood", UI.Color.BURLYWOOD);
        NAMED_COLOURS.put("cadetblue", UI.Color.CADETBLUE);
        NAMED_COLOURS.put("chartreuse", UI.Color.CHARTREUSE);
        NAMED_COLOURS.put("chocolate", UI.Color.CHOCOLATE);
        NAMED_COLOURS.put("coral", UI.Color.CORAL);
        NAMED_COLOURS.put("cornflowerblue", UI.Color.CORNFLOWERBLUE);
        NAMED_COLOURS.put("cornsilk", UI.Color.CORNSILK);
        NAMED_COLOURS.put("crimson", UI.Color.CRIMSON);
        NAMED_COLOURS.put("cyan", UI.Color.CYAN);
        NAMED_COLOURS.put("darkblue", UI.Color.DARKBLUE);
        NAMED_COLOURS.put("darkcyan", UI.Color.DARKCYAN);
        NAMED_COLOURS.put("darkgoldenrod", UI.Color.DARKGOLDENROD);
        NAMED_COLOURS.put("darkgray", UI.Color.DARKGRAY);
        NAMED_COLOURS.put("darkgreen", UI.Color.DARKGREEN);
        NAMED_COLOURS.put("darkgrey", UI.Color.DARKGREY);
        NAMED_COLOURS.put("darkkhaki", UI.Color.DARKKHAKI);
        NAMED_COLOURS.put("darkmagenta", UI.Color.DARKMAGENTA);
        NAMED_COLOURS.put("darkolivegreen", UI.Color.DARKOLIVEGREEN);
        NAMED_COLOURS.put("darkorange", UI.Color.DARKORANGE);
        NAMED_COLOURS.put("darkorchid", UI.Color.DARKORCHID);
        NAMED_COLOURS.put("darkred", UI.Color.DARKRED);
        NAMED_COLOURS.put("darksalmon", UI.Color.DARKSALMON);
        NAMED_COLOURS.put("darkseagreen", UI.Color.DARKSEAGREEN);
        NAMED_COLOURS.put("darkslateblue", UI.Color.DARKSLATEBLUE);
        NAMED_COLOURS.put("darkslategray", UI.Color.DARKSLATEGRAY);
        NAMED_COLOURS.put("darkslategrey", UI.Color.DARKSLATEGREY);
        NAMED_COLOURS.put("darkturquoise", UI.Color.DARKTURQUOISE);
        NAMED_COLOURS.put("darkviolet", UI.Color.DARKVIOLET);
        NAMED_COLOURS.put("deeppink", UI.Color.DEEPPINK);
        NAMED_COLOURS.put("deepskyblue", UI.Color.DEEPSKYBLUE);
        NAMED_COLOURS.put("dimgray", UI.Color.DIMGRAY);
        NAMED_COLOURS.put("dimgrey", UI.Color.DIMGREY);
        NAMED_COLOURS.put("dodgerblue", UI.Color.DODGERBLUE);
        NAMED_COLOURS.put("firebrick", UI.Color.FIREBRICK);
        NAMED_COLOURS.put("floralwhite", UI.Color.FLORALWHITE);
        NAMED_COLOURS.put("forestgreen", UI.Color.FORESTGREEN);
        NAMED_COLOURS.put("fuchsia", UI.Color.FUCHSIA);
        NAMED_COLOURS.put("gainsboro", UI.Color.GAINSBORO);
        NAMED_COLOURS.put("ghostwhite", UI.Color.GHOSTWHITE);
        NAMED_COLOURS.put("gold", UI.Color.GOLD);
        NAMED_COLOURS.put("goldenrod", UI.Color.GOLDENROD);
        NAMED_COLOURS.put("gray", UI.Color.GRAY);
        NAMED_COLOURS.put("green", UI.Color.GREEN);
        NAMED_COLOURS.put("greenyellow", UI.Color.GREENYELLOW);
        NAMED_COLOURS.put("grey", UI.Color.GREY);
        NAMED_COLOURS.put("honeydew", UI.Color.HONEYDEW);
        NAMED_COLOURS.put("hotpink", UI.Color.HOTPINK);
        NAMED_COLOURS.put("indianred", UI.Color.INDIANRED);
        NAMED_COLOURS.put("indigo", UI.Color.INDIGO);
        NAMED_COLOURS.put("ivory", UI.Color.IVORY);
        NAMED_COLOURS.put("khaki", UI.Color.KHAKI);
        NAMED_COLOURS.put("lavender", UI.Color.LAVENDER);
        NAMED_COLOURS.put("lavenderblush", UI.Color.LAVENDERBLUSH);
        NAMED_COLOURS.put("lawngreen", UI.Color.LAWNGREEN);
        NAMED_COLOURS.put("lemonchiffon", UI.Color.LEMONCHIFFON);
        NAMED_COLOURS.put("lightblue", UI.Color.LIGHTBLUE);
        NAMED_COLOURS.put("lightcoral", UI.Color.LIGHTCORAL);
        NAMED_COLOURS.put("lightcyan", UI.Color.LIGHTCYAN);
        NAMED_COLOURS.put("lightgoldenrodyellow", UI.Color.LIGHTGOLDENRODYELLOW);
        NAMED_COLOURS.put("lightgray", UI.Color.LIGHTGRAY);
        NAMED_COLOURS.put("lightgreen", UI.Color.LIGHTGREEN);
        NAMED_COLOURS.put("lightgrey", UI.Color.LIGHTGREY);
        NAMED_COLOURS.put("lightpink", UI.Color.LIGHTPINK);
        NAMED_COLOURS.put("lightsalmon", UI.Color.LIGHTSALMON);
        NAMED_COLOURS.put("lightseagreen", UI.Color.LIGHTSEAGREEN);
        NAMED_COLOURS.put("lightskyblue", UI.Color.LIGHTSKYBLUE);
        NAMED_COLOURS.put("lightslategray", UI.Color.LIGHTSLATEGRAY);
        NAMED_COLOURS.put("lightslategrey", UI.Color.LIGHTSLATEGREY);
        NAMED_COLOURS.put("lightsteelblue", UI.Color.LIGHTSTEELBLUE);
        NAMED_COLOURS.put("lightyellow", UI.Color.LIGHTYELLOW);
        NAMED_COLOURS.put("lime", UI.Color.LIME);
        NAMED_COLOURS.put("limegreen", UI.Color.LIMEGREEN);
        NAMED_COLOURS.put("linen", UI.Color.LINEN);
        NAMED_COLOURS.put("magenta", UI.Color.MAGENTA);
        NAMED_COLOURS.put("maroon", UI.Color.MAROON);
        NAMED_COLOURS.put("mediumaquamarine", UI.Color.MEDIUMAQUAMARINE);
        NAMED_COLOURS.put("mediumblue", UI.Color.MEDIUMBLUE);
        NAMED_COLOURS.put("mediumorchid", UI.Color.MEDIUMORCHID);
        NAMED_COLOURS.put("mediumpurple", UI.Color.MEDIUMPURPLE);
        NAMED_COLOURS.put("mediumseagreen", UI.Color.MEDIUMSEAGREEN);
        NAMED_COLOURS.put("mediumslateblue", UI.Color.MEDIUMSLATEBLUE);
        NAMED_COLOURS.put("mediumspringgreen", UI.Color.MEDIUMSPRINGGREEN);
        NAMED_COLOURS.put("mediumturquoise", UI.Color.MEDIUMTURQUOISE);
        NAMED_COLOURS.put("mediumvioletred", UI.Color.MEDIUMVIOLETRED);
        NAMED_COLOURS.put("midnightblue", UI.Color.MIDNIGHTBLUE);
        NAMED_COLOURS.put("mintcream", UI.Color.MINTCREAM);
        NAMED_COLOURS.put("mistyrose", UI.Color.MISTYROSE);
        NAMED_COLOURS.put("moccasin", UI.Color.MOCCASIN);
        NAMED_COLOURS.put("navajowhite", UI.Color.NAVAJOWHITE);
        NAMED_COLOURS.put("navy", UI.Color.NAVY);
        NAMED_COLOURS.put("oak", UI.Color.OAK);
        NAMED_COLOURS.put("oldlace", UI.Color.OLDLACE);
        NAMED_COLOURS.put("olive", UI.Color.OLIVE);
        NAMED_COLOURS.put("olivedrab", UI.Color.OLIVEDRAB);
        NAMED_COLOURS.put("orange", UI.Color.ORANGE);
        NAMED_COLOURS.put("orangered", UI.Color.ORANGERED);
        NAMED_COLOURS.put("orchid", UI.Color.ORCHID);
        NAMED_COLOURS.put("palegoldenrod", UI.Color.PALEGOLDENROD);
        NAMED_COLOURS.put("palegreen", UI.Color.PALEGREEN);
        NAMED_COLOURS.put("paleturquoise", UI.Color.PALETURQUOISE);
        NAMED_COLOURS.put("palevioletred", UI.Color.PALEVIOLETRED);
        NAMED_COLOURS.put("papayawhip", UI.Color.PAPAYAWHIP);
        NAMED_COLOURS.put("peachpuff", UI.Color.PEACHPUFF);
        NAMED_COLOURS.put("peru", UI.Color.PERU);
        NAMED_COLOURS.put("pink", UI.Color.PINK);
        NAMED_COLOURS.put("plum", UI.Color.PLUM);
        NAMED_COLOURS.put("powderblue", UI.Color.POWDERBLUE);
        NAMED_COLOURS.put("purple", UI.Color.PURPLE);
        NAMED_COLOURS.put("red", UI.Color.RED);
        NAMED_COLOURS.put("rosybrown", UI.Color.ROSYBROWN);
        NAMED_COLOURS.put("royalblue", UI.Color.ROYALBLUE);
        NAMED_COLOURS.put("saddlebrown", UI.Color.SADDLEBROWN);
        NAMED_COLOURS.put("salmon", UI.Color.SALMON);
        NAMED_COLOURS.put("sandybrown", UI.Color.SANDYBROWN);
        NAMED_COLOURS.put("seagreen", UI.Color.SEAGREEN);
        NAMED_COLOURS.put("seashell", UI.Color.SEASHELL);
        NAMED_COLOURS.put("sienna", UI.Color.SIENNA);
        NAMED_COLOURS.put("silver", UI.Color.SILVER);
        NAMED_COLOURS.put("skyblue", UI.Color.SKYBLUE);
        NAMED_COLOURS.put("slateblue", UI.Color.SLATEBLUE);
        NAMED_COLOURS.put("slategray", UI.Color.SLATEGRAY);
        NAMED_COLOURS.put("slategrey", UI.Color.SLATEGREY);
        NAMED_COLOURS.put("snow", UI.Color.SNOW);
        NAMED_COLOURS.put("springgreen", UI.Color.SPRINGGREEN);
        NAMED_COLOURS.put("steelblue", UI.Color.STEELBLUE);
        NAMED_COLOURS.put("tan", UI.Color.TAN);
        NAMED_COLOURS.put("teal", UI.Color.TEAL);
        NAMED_COLOURS.put("thistle", UI.Color.THISTLE);
        NAMED_COLOURS.put("tomato", UI.Color.TOMATO);
        NAMED_COLOURS.put("transparent", UI.Color.TRANSPARENT);
        NAMED_COLOURS.put("turquoise", UI.Color.TURQUOISE);
        NAMED_COLOURS.put("violet", UI.Color.VIOLET);
        NAMED_COLOURS.put("wheat", UI.Color.WHEAT);
        NAMED_COLOURS.put("white", UI.Color.WHITE);
        NAMED_COLOURS.put("whitesmoke", UI.Color.WHITESMOKE);
        NAMED_COLOURS.put("yellow", UI.Color.YELLOW);
        NAMED_COLOURS.put("yellowgreen", UI.Color.YELLOWGREEN);
    }
}
