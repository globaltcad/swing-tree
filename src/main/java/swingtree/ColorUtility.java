package swingtree;

import org.jspecify.annotations.Nullable;

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
        hue = normalizedHue/360;

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


    static UI.Color parseColor(final String colorAsString)
    {
        // First some cleanup
        final String colorString = colorAsString.trim();

        if ( colorAsString.isEmpty() )
            return UI.Color.UNDEFINED;

        if ( colorString.startsWith("#") )
            return UI.Color.of(java.awt.Color.decode(colorString));

        if ( colorString.startsWith("0x") )
            return UI.Color.of(java.awt.Color.decode(colorString));

        if ( colorString.startsWith("rgb") ) {
            // We have an rgb() or rgba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if ( start < 0 || end < 0 || end < start ) {
                log.error("Invalid rgb() or rgba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            String[] parts = colorString.substring(start + 1, end).split(",");
            if ( parts.length < 3 || parts.length > 4 ) {
                log.error("Invalid rgb() or rgba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            for ( int i = 0; i < parts.length; i++ )
                parts[i] = parts[i].trim();

            int[] values = new int[parts.length];

            for ( int i = 0; i < parts.length; i++ ) {
                String part = parts[i];
                if ( part.endsWith("%") ) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Integer.parseInt(part);
                    if ( values[i] < 0 || values[i] > 100 ) {
                        log.error("Invalid rgb() or rgba() color: " + colorString, new Throwable());
                        return UI.Color.UNDEFINED;
                    }
                    values[i] = (int) Math.ceil(values[i] * 2.55);
                }
                else if ( part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])") )
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

        if ( colorString.startsWith("hsb") ) {
            // We have an hsb() or hsba() color
            int start = colorString.indexOf('(');
            int end = colorString.indexOf(')');
            if ( start < 0 || end < 0 || end < start ) {
                log.error("Invalid hsb() or hsba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            String[] parts = colorString.substring(start + 1, end).split(",");
            if ( parts.length < 3 || parts.length > 4 ) {
                log.error("Invalid hsb() or hsba() color: " + colorString, new Throwable());
                return UI.Color.UNDEFINED;
            }

            for ( int i = 0; i < parts.length; i++ )
                parts[i] = parts[i].trim();

            float[] values = new float[parts.length];

            for ( int i = 0; i < parts.length; i++ ) {
                String part = parts[i];
                if ( part.endsWith("%") ) {
                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if ( values[i] < 0 || values[i] > 100 ) {
                        log.error(
                                "Invalid hsb() or hsba() string '" + colorString + "', " +
                                "value '" + part + "' out of range.",
                                new Throwable()
                            );
                        return UI.Color.UNDEFINED;
                    }
                    values[i] = values[i] / 100.0f;
                } else if ( part.endsWith("°") ) {
                    if ( i > 0 ) {
                        log.error(
                            "Invalid hsb() or hsba() string '" + colorString + "', " +
                            "unexpected degree symbol in '" + part + "' (only allowed for hue)",
                            new Throwable()
                        );
                        return UI.Color.UNDEFINED;
                    }

                    part = part.substring(0, part.length() - 1);
                    values[i] = Float.parseFloat(part);
                    if ( values[i] < 0 || values[i] > 360 ) {
                        log.error(
                            "Invalid hsb() or hsba() string '" + colorString + "', " +
                            "hue value '" + part + "' out of range.",
                            new Throwable()
                        );
                        return UI.Color.UNDEFINED;
                    }
                    values[i] = values[i] / 360.0f;
                } else if ( part.matches("[0-9]+((\\.[0-9]+[fF]?)|[fF])") )
                    values[i] = Float.parseFloat(part);
                else
                    values[i] = Integer.parseInt(part);
            }

            float h = values[0];
            float s = values[1];
            float b = values[2];
            float a = values.length == 4 ? values[3] : 1.0f;
            java.awt.Color c = java.awt.Color.getHSBColor(h, s, b);
            return UI.Color.ofRgba(c.getRed(), c.getGreen(), c.getBlue(), (int)(a * 255));
        }

        {
            String maybeWord = colorString.toLowerCase();
            boolean transparent = false;

            if ( maybeWord.startsWith("transparent") ) {
                transparent = true;
                maybeWord = maybeWord.substring(11).trim();
            }

            // Let's try a few common color names
            UI.Color color = _tryFromName(maybeWord);
            if ( color == null && maybeWord.startsWith("darker") ) {
                color = _tryFromName(maybeWord.substring(6).trim());
                if ( color != null )
                    color = color.darker();
            }
            if ( color == null && maybeWord.startsWith("dark") ) {
                color = _tryFromName(maybeWord.substring(4).trim());
                if ( color != null )
                    color = color.darker();
            }
            if ( color == null && maybeWord.startsWith("lighter") ) {
                color = _tryFromName(maybeWord.substring(7).trim());
                if ( color != null )
                    color = color.brighter();
            }
            if ( color == null && maybeWord.startsWith("light") ) {
                color = _tryFromName(maybeWord.substring(5).trim());
                if ( color != null )
                    color = color.brighter();
            }
            if ( color == null && maybeWord.startsWith("brighter") ) {
                color = _tryFromName(maybeWord.substring(8).trim());
                if ( color != null )
                    color = color.brighter();
            }
            if ( color == null && maybeWord.startsWith("bright") ) {
                color = _tryFromName(maybeWord.substring(6).trim());
                if ( color != null )
                    color = color.brighter();
            }

            if ( color != null ) {
                if ( transparent )
                    return UI.Color.ofRgba(color.getRed(), color.getGreen(), color.getBlue(), 255/2);
                else
                    return color;
            }
            else if ( transparent )
                return UI.Color.TRANSPARENT;
        }

        // Let's try to find it as a system property
        UI.Color foundInSystemProperties = null;
        try {
            java.awt.Color found = java.awt.Color.getColor(colorString);
            if ( found != null && !(found instanceof UI.Color) )
                foundInSystemProperties = UI.Color.of(found);
        } catch ( IllegalArgumentException e ) {
            // Ignore
        }
        if ( foundInSystemProperties != null )
            return foundInSystemProperties;

        return UI.Color.UNDEFINED;
    }

    private static UI.@Nullable Color _tryFromName(String maybeColorName ) {
        try {
            String lowerCaseName = maybeColorName.toLowerCase();
            return ColorUtility.get(lowerCaseName);
        } catch ( IllegalArgumentException e ) {
            return null;
        }
    }


    static UI.@Nullable Color get(String name) {
        return NAMED_COLOURS.get(name);
    }

    private static final Map<String, UI.Color> NAMED_COLOURS = Map.ofEntries(
            Map.entry("aliceblue", UI.Color.ALICEBLUE),
            Map.entry("antiquewhite", UI.Color.ANTIQUEWHITE),
            Map.entry("aqua", UI.Color.AQUA),
            Map.entry("aquamarine", UI.Color.AQUAMARINE),
            Map.entry("azure", UI.Color.AZURE),
            Map.entry("beige", UI.Color.BEIGE),
            Map.entry("bisque", UI.Color.BISQUE),
            Map.entry("black", UI.Color.BLACK),
            Map.entry("blanchedalmond", UI.Color.BLANCHEDALMOND),
            Map.entry("blue", UI.Color.BLUE),
            Map.entry("blueviolet", UI.Color.BLUEVIOLET),
            Map.entry("brown", UI.Color.BROWN),
            Map.entry("burlywood", UI.Color.BURLYWOOD),
            Map.entry("cadetblue", UI.Color.CADETBLUE),
            Map.entry("chartreuse", UI.Color.CHARTREUSE),
            Map.entry("chocolate", UI.Color.CHOCOLATE),
            Map.entry("coral", UI.Color.CORAL),
            Map.entry("cornflowerblue", UI.Color.CORNFLOWERBLUE),
            Map.entry("cornsilk", UI.Color.CORNSILK),
            Map.entry("crimson", UI.Color.CRIMSON),
            Map.entry("cyan", UI.Color.CYAN),
            Map.entry("darkblue", UI.Color.DARKBLUE),
            Map.entry("darkcyan", UI.Color.DARKCYAN),
            Map.entry("darkgoldenrod", UI.Color.DARKGOLDENROD),
            Map.entry("darkgray", UI.Color.DARKGRAY),
            Map.entry("darkgreen", UI.Color.DARKGREEN),
            Map.entry("darkgrey", UI.Color.DARKGREY),
            Map.entry("darkkhaki", UI.Color.DARKKHAKI),
            Map.entry("darkmagenta", UI.Color.DARKMAGENTA),
            Map.entry("darkolivegreen", UI.Color.DARKOLIVEGREEN),
            Map.entry("darkorange", UI.Color.DARKORANGE),
            Map.entry("darkorchid", UI.Color.DARKORCHID),
            Map.entry("darkred", UI.Color.DARKRED),
            Map.entry("darksalmon", UI.Color.DARKSALMON),
            Map.entry("darkseagreen", UI.Color.DARKSEAGREEN),
            Map.entry("darkslateblue", UI.Color.DARKSLATEBLUE),
            Map.entry("darkslategray", UI.Color.DARKSLATEGRAY),
            Map.entry("darkslategrey", UI.Color.DARKSLATEGREY),
            Map.entry("darkturquoise", UI.Color.DARKTURQUOISE),
            Map.entry("darkviolet", UI.Color.DARKVIOLET),
            Map.entry("deeppink", UI.Color.DEEPPINK),
            Map.entry("deepskyblue", UI.Color.DEEPSKYBLUE),
            Map.entry("dimgray", UI.Color.DIMGRAY),
            Map.entry("dimgrey", UI.Color.DIMGREY),
            Map.entry("dodgerblue", UI.Color.DODGERBLUE),
            Map.entry("firebrick", UI.Color.FIREBRICK),
            Map.entry("floralwhite", UI.Color.FLORALWHITE),
            Map.entry("forestgreen", UI.Color.FORESTGREEN),
            Map.entry("fuchsia", UI.Color.FUCHSIA),
            Map.entry("gainsboro", UI.Color.GAINSBORO),
            Map.entry("ghostwhite", UI.Color.GHOSTWHITE),
            Map.entry("gold", UI.Color.GOLD),
            Map.entry("goldenrod", UI.Color.GOLDENROD),
            Map.entry("gray", UI.Color.GRAY),
            Map.entry("green", UI.Color.GREEN),
            Map.entry("greenyellow", UI.Color.GREENYELLOW),
            Map.entry("grey", UI.Color.GREY),
            Map.entry("honeydew", UI.Color.HONEYDEW),
            Map.entry("hotpink", UI.Color.HOTPINK),
            Map.entry("indianred", UI.Color.INDIANRED),
            Map.entry("indigo", UI.Color.INDIGO),
            Map.entry("ivory", UI.Color.IVORY),
            Map.entry("khaki", UI.Color.KHAKI),
            Map.entry("lavender", UI.Color.LAVENDER),
            Map.entry("lavenderblush", UI.Color.LAVENDERBLUSH),
            Map.entry("lawngreen", UI.Color.LAWNGREEN),
            Map.entry("lemonchiffon", UI.Color.LEMONCHIFFON),
            Map.entry("lightblue", UI.Color.LIGHTBLUE),
            Map.entry("lightcoral", UI.Color.LIGHTCORAL),
            Map.entry("lightcyan", UI.Color.LIGHTCYAN),
            Map.entry("lightgoldenrodyellow", UI.Color.LIGHTGOLDENRODYELLOW),
            Map.entry("lightgray", UI.Color.LIGHTGRAY),
            Map.entry("lightgreen", UI.Color.LIGHTGREEN),
            Map.entry("lightgrey", UI.Color.LIGHTGREY),
            Map.entry("lightpink", UI.Color.LIGHTPINK),
            Map.entry("lightsalmon", UI.Color.LIGHTSALMON),
            Map.entry("lightseagreen", UI.Color.LIGHTSEAGREEN),
            Map.entry("lightskyblue", UI.Color.LIGHTSKYBLUE),
            Map.entry("lightslategray", UI.Color.LIGHTSLATEGRAY),
            Map.entry("lightslategrey", UI.Color.LIGHTSLATEGREY),
            Map.entry("lightsteelblue", UI.Color.LIGHTSTEELBLUE),
            Map.entry("lightyellow", UI.Color.LIGHTYELLOW),
            Map.entry("lime", UI.Color.LIME),
            Map.entry("limegreen", UI.Color.LIMEGREEN),
            Map.entry("linen", UI.Color.LINEN),
            Map.entry("magenta", UI.Color.MAGENTA),
            Map.entry("maroon", UI.Color.MAROON),
            Map.entry("mediumaquamarine", UI.Color.MEDIUMAQUAMARINE),
            Map.entry("mediumblue", UI.Color.MEDIUMBLUE),
            Map.entry("mediumorchid", UI.Color.MEDIUMORCHID),
            Map.entry("mediumpurple", UI.Color.MEDIUMPURPLE),
            Map.entry("mediumseagreen", UI.Color.MEDIUMSEAGREEN),
            Map.entry("mediumslateblue", UI.Color.MEDIUMSLATEBLUE),
            Map.entry("mediumspringgreen", UI.Color.MEDIUMSPRINGGREEN),
            Map.entry("mediumturquoise", UI.Color.MEDIUMTURQUOISE),
            Map.entry("mediumvioletred", UI.Color.MEDIUMVIOLETRED),
            Map.entry("midnightblue", UI.Color.MIDNIGHTBLUE),
            Map.entry("mintcream", UI.Color.MINTCREAM),
            Map.entry("mistyrose", UI.Color.MISTYROSE),
            Map.entry("moccasin", UI.Color.MOCCASIN),
            Map.entry("navajowhite", UI.Color.NAVAJOWHITE),
            Map.entry("navy", UI.Color.NAVY),
            Map.entry("oldlace", UI.Color.OLDLACE),
            Map.entry("olive", UI.Color.OLIVE),
            Map.entry("olivedrab", UI.Color.OLIVEDRAB),
            Map.entry("orange", UI.Color.ORANGE),
            Map.entry("orangered", UI.Color.ORANGERED),
            Map.entry("orchid", UI.Color.ORCHID),
            Map.entry("palegoldenrod", UI.Color.PALEGOLDENROD),
            Map.entry("palegreen", UI.Color.PALEGREEN),
            Map.entry("paleturquoise", UI.Color.PALETURQUOISE),
            Map.entry("palevioletred", UI.Color.PALEVIOLETRED),
            Map.entry("papayawhip", UI.Color.PAPAYAWHIP),
            Map.entry("peachpuff", UI.Color.PEACHPUFF),
            Map.entry("peru", UI.Color.PERU),
            Map.entry("pink", UI.Color.PINK),
            Map.entry("plum", UI.Color.PLUM),
            Map.entry("powderblue", UI.Color.POWDERBLUE),
            Map.entry("purple", UI.Color.PURPLE),
            Map.entry("red", UI.Color.RED),
            Map.entry("rosybrown", UI.Color.ROSYBROWN),
            Map.entry("royalblue", UI.Color.ROYALBLUE),
            Map.entry("saddlebrown", UI.Color.SADDLEBROWN),
            Map.entry("salmon", UI.Color.SALMON),
            Map.entry("sandybrown", UI.Color.SANDYBROWN),
            Map.entry("seagreen", UI.Color.SEAGREEN),
            Map.entry("seashell", UI.Color.SEASHELL),
            Map.entry("sienna", UI.Color.SIENNA),
            Map.entry("silver", UI.Color.SILVER),
            Map.entry("skyblue", UI.Color.SKYBLUE),
            Map.entry("slateblue", UI.Color.SLATEBLUE),
            Map.entry("slategray", UI.Color.SLATEGRAY),
            Map.entry("slategrey", UI.Color.SLATEGREY),
            Map.entry("snow", UI.Color.SNOW),
            Map.entry("springgreen", UI.Color.SPRINGGREEN),
            Map.entry("steelblue", UI.Color.STEELBLUE),
            Map.entry("tan", UI.Color.TAN),
            Map.entry("teal", UI.Color.TEAL),
            Map.entry("thistle", UI.Color.THISTLE),
            Map.entry("tomato", UI.Color.TOMATO),
            Map.entry("transparent", UI.Color.TRANSPARENT),
            Map.entry("turquoise", UI.Color.TURQUOISE),
            Map.entry("violet", UI.Color.VIOLET),
            Map.entry("wheat", UI.Color.WHEAT),
            Map.entry("white", UI.Color.WHITE),
            Map.entry("whitesmoke", UI.Color.WHITESMOKE),
            Map.entry("yellow", UI.Color.YELLOW),
            Map.entry("yellowgreen", UI.Color.YELLOWGREEN));
}
