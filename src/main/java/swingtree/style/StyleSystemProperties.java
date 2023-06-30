package swingtree.style;


/**
 * Defines/documents own system properties used in SwingTree.
 *
 * @author Daniel Nepp, but originally a derivative work of Karl Tauber
 */
public interface StyleSystemProperties
{
    /**
     * Specifies a custom scale factor used to scale the UI.
     * <p>
     * If Java runtime scales (Java 9 or later), this scale factor is applied on top
     * of the Java system scale factor. Java 8 does not scale and this scale factor
     * replaces the user scale factor that FlatLaf computes based on the font.
     * To replace the Java 9+ system scale factor, use system property "sun.java2d.uiScale",
     * which has the same syntax as this one.
     * <p>
     * <strong>Allowed Values</strong> e.g. {@code 1.5}, {@code 1.5x}, {@code 150%} or {@code 144dpi} (96dpi is 100%)<br>
     */
    String UI_SCALE = "style.uiScale";

    /**
     * Specifies whether user scaling mode is enabled.
     * <p>
     * <strong>Allowed Values</strong> {@code false} and {@code true}<br>
     * <strong>Default</strong> {@code true}
     */
    String UI_SCALE_ENABLED = "style.uiScale.enabled";

    /**
     * Specifies whether values smaller than 100% are allowed for the user scale factor
     * (see {@link UIScale#getUserScaleFactor()}).
     * <p>
     * <strong>Allowed Values</strong> {@code false} and {@code true}<br>
     * <strong>Default</strong> {@code false}
     */
    String UI_SCALE_ALLOW_SCALE_DOWN = "style.uiScale.allowScaleDown";

    /**
     * Checks whether a system property is set and returns {@code true} if its value
     * is {@code "true"} (case-insensitive), otherwise it returns {@code false}.
     * If the system property is not set, {@code defaultValue} is returned.
     */
    static boolean getBoolean( String key, boolean defaultValue ) {
        String value = System.getProperty( key );
        return (value != null) ? Boolean.parseBoolean( value ) : defaultValue;
    }
}

