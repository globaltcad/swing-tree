package swingtree;

import swingtree.style.StyleSheet;
import swingtree.threading.EventProcessor;

import java.awt.Font;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 *  An immutable configuration object for the {@link SwingTree} library,
 *  which can be configured using a functional {@link SwingTreeConfigurator} lambda
 *  passed to {@link SwingTree#initialiseUsing(SwingTreeConfigurator)}.
 *  <p>
 *  It allows for the configuration of the default
 *  font, font installation, scaling, event processing and
 *  application wide {@link StyleSheet}.
 */
public final class SwingTreeInitConfig
{
    /**
     *  Defines how the {@link Font}, specified through {@link SwingTreeInitConfig#defaultFont(Font)},
     *  is installed in the {@link javax.swing.UIManager}.
     */
    public enum FontInstallation
    {
        /**
         *  A soft installation will only install the font as the "defaultFont" property
         *  of the {@link javax.swing.UIManager}.
         */
        SOFT,

        /**
         *  A hard installation will install the font as the "defaultFont" property
         *  of the {@link javax.swing.UIManager} and as the default font of all
         *  {@link javax.swing.UIManager} components.
         */
        HARD,

        /**
         *  No installation will be performed.
         */
        NONE
    }

    /**
     *  Defines how the scaling factor for the UI should be determined.
     */
    public enum Scaling
    {
        /**
         *  No scaling will be performed.
         */
        NONE,

        /**
         *  The scaling will be derived from the supplied scaling factor.
         *  (see {@link SwingTreeInitConfig#uiScaleFactor(float)} as well as
         *  system property {@code "swingtree.uiScale"}).
         */
        FROM_SCALING_FACTOR,

        /**
         *  The scaling will be derived from the default font size,
         *  if a default font was specified. (see {@link SwingTreeInitConfig#defaultFont(Font)})
         */
        FROM_DEFAULT_FONT,

        /**
         *  The scaling will be derived from the system font size.
         *  This scaling policy is chosen if no default font was specified,
         *  no scaling factor was specified and the scaling mode is enabled
         *  (see {@link SwingTreeInitConfig#isUiScaleFactorEnabled()}).
         */
        FROM_SYSTEM_FONT
    }

    public static SwingTreeInitConfig standard() {
        return new SwingTreeInitConfig(
                        null,
                        FontInstallation.SOFT,
                        EventProcessor.COUPLED_STRICT,
                        StyleSheet.none(),
                        SystemProperties.getFloat(SystemProperties.UI_SCALE,                 -1    ),
                        SystemProperties.getBool(SystemProperties.UI_SCALE_ENABLED,          true  ),
                        SystemProperties.getBool(SystemProperties.UI_SCALE_ALLOW_SCALE_DOWN, false ),
                        SystemProperties.getLong(SystemProperties.ANIMATION_INTERVAL,        16    )
                    );
                    /*
                        Nte that we want the refresh rate to be as high as possible so that the animation
                        looks smooth, but we don't want to use 100% of the CPU.
                        The ideal refresh rate is 60 fps which is 16.6 ms per frame.
                        So we set the timer to 16 ms.
                        This does of course not account for the time it takes to run the animation
                        code, but that should be negligible, and in the worst case
                        the animation will be a bit slower than 60 fps.
                    */
    }


    private final Font             _defaultFont; // may be null
    private final FontInstallation _fontInstallation;
    private final EventProcessor   _eventProcessor;
    private final StyleSheet       _styleSheet;
    private final float            _uiScale;
    private final boolean          _uiScaleEnabled;
    private final boolean          _uiScaleAllowScaleDown;
    private final long             _defaultAnimationInterval;


    private SwingTreeInitConfig(
        Font             defaultFont,
        FontInstallation fontInstallation,
        EventProcessor   eventProcessor,
        StyleSheet       styleSheet,
        float            uiScale,
        boolean          uiScaleEnabled,
        boolean          uiScaleAllowScaleDown,
        long             defaultAnimationInterval
    ) {
        _defaultFont              = defaultFont;
        _fontInstallation         = Objects.requireNonNull(fontInstallation);
        _eventProcessor           = Objects.requireNonNull(eventProcessor);
        _styleSheet               = Objects.requireNonNull(styleSheet);
        _uiScale                  = uiScale;
        _uiScaleEnabled           = uiScaleEnabled;
        _uiScaleAllowScaleDown    = uiScaleAllowScaleDown;
        _defaultAnimationInterval = defaultAnimationInterval;
    }

    /**
     *  Returns an {@link Optional} containing the default font
     *  or an empty {@link Optional} if no default font is set.
     */
    Optional<Font> defaultFont() {
        return Optional.ofNullable(_defaultFont);
    }

    /**
     *  Returns the {@link FontInstallation} mode.
     */
    FontInstallation fontInstallation() {
        return _fontInstallation;
    }

    /**
     *  Returns the {@link Scaling} mode
     *  which is determined based on the configuration.
     *  <ul>
     *      <li>
     *          If {@code false} was passed to {@link #isUiScaleFactorEnabled(boolean)},
     *          then {@link Scaling#NONE} is returned.
     *      </li>
     *      <li>
     *          Otherwise, if a scaling factor greater than 0 was passed to {@link #uiScaleFactor(float)},
     *          then {@link Scaling#FROM_SCALING_FACTOR} is returned.
     *      </li>
     *      <li>
     *          Otherwise, if a default font was passed to {@link #defaultFont(Font)}, <br>
     *          then {@link Scaling#FROM_DEFAULT_FONT} is returned. <br>
     *      </li>
     *  </ul>
     *  <p>
     *  If none of the above applies, then {@link Scaling#FROM_SYSTEM_FONT} is returned. <br>
     */
    Scaling scalingStrategy() {
        if ( !_uiScaleEnabled )
            return Scaling.NONE;
        if ( _uiScale > 0 )
            return Scaling.FROM_SCALING_FACTOR;
        if ( _defaultFont != null )
            return Scaling.FROM_DEFAULT_FONT;

        return Scaling.FROM_SYSTEM_FONT;
    }

    /**
     *  Returns the {@link EventProcessor},
     *  which is used to process both UI and application events.
     */
    EventProcessor eventProcessor() {
        return _eventProcessor;
    }

    /**
     *  Returns an {@link Optional} containing the {@link StyleSheet}
     *  or an empty {@link Optional} if no {@link StyleSheet} is set.
     */
    StyleSheet styleSheet() {
        return _styleSheet;
    }

    /**
     *  Returns the UI scaling factor as is specified by the system property {@code swingtree.uiScale}.
     */
    float uiScaleFactor() {
        return _uiScale;
    }

    /**
     *  Returns whether the UI scaling mode is enabled as is specified by the system property {@code swingtree.uiScale.enabled}.
     */
    boolean isUiScaleFactorEnabled() {
        return _uiScaleEnabled;
    }

    /**
     *  Returns whether values smaller than 100% are allowed for the user scale factor
     *  as is specified by the system property {@code swingtree.uiScale.allowScaleDown}.
     */
    boolean isUiScaleDownAllowed() {
        return _uiScaleAllowScaleDown;
    }

    /**
     *  Returns the default animation interval in milliseconds,
     *  which is a property that
     *  determines the delay between two consecutive animation steps.
     *  You can think of it as the time between the heartbeats of the animation.
     *  The smaller the interval, the higher the refresh rate and
     *  the smoother the animation will look.
     *  However, the smaller the interval, the more CPU time will be used.
     *  The default interval is 16 ms which corresponds to 60 fps.
     *  See {@link #standard()}, returning an instance of this config with the default value. <br>
     *  This property is used as default value by the {@link swingtree.animation.LifeTime}
     *  object which is used to define the duration of an {@link swingtree.animation.Animation}.
     */
    long defaultAnimationInterval() {
        return _defaultAnimationInterval;
    }

    /**
     *  Used to configure the default font, which may be used by the {@link SwingTree}
     *  to derive the UI scaling factor and or to install the font in the {@link javax.swing.UIManager}
     *  depending on the {@link FontInstallation} mode (see {@link #defaultFont(Font, FontInstallation)}).
     * @param newDefaultFont The new default font, or {@code null} to unset the default font.
     * @return A new {@link SwingTreeInitConfig} instance with the new default font.
     */
    public SwingTreeInitConfig defaultFont( Font newDefaultFont ) {
        return new SwingTreeInitConfig(newDefaultFont, _fontInstallation, _eventProcessor, _styleSheet, _uiScale, _uiScaleEnabled, _uiScaleAllowScaleDown, _defaultAnimationInterval);
    }

    /**
     *  Used to configure both the default {@link Font} and the {@link FontInstallation} mode.
     *  <p>
     *  The {@link FontInstallation} mode determines how the {@link Font} is installed
     *  in the {@link javax.swing.UIManager} (see {@link FontInstallation}).
     *  <p>
     *  If the {@link FontInstallation} mode is {@link FontInstallation#SOFT},
     *  then the {@link Font} will only be installed as the "defaultFont" property
     *  in the {@link javax.swing.UIManager}. If on the other hand the {@link FontInstallation}
     *  mode is {@link FontInstallation#HARD}, then the {@link Font} will be installed
     *  by replacing all {@link Font}s in the {@link javax.swing.UIManager}.
     *
     * @param newDefaultFont The new default font, or {@code null} to unset the default font.
     * @param newFontInstallation The new {@link FontInstallation} mode.
     * @return A new {@link SwingTreeInitConfig} instance with the new default font and {@link FontInstallation} mode.
     */
    public SwingTreeInitConfig defaultFont( Font newDefaultFont, FontInstallation newFontInstallation ) {
        return new SwingTreeInitConfig(newDefaultFont, newFontInstallation, _eventProcessor, _styleSheet, _uiScale, _uiScaleEnabled, _uiScaleAllowScaleDown, _defaultAnimationInterval);
    }

    /**
     *  Used to configure the {@link EventProcessor}, which is used to process both
     *  UI and application events.
     *  You may create your own {@link EventProcessor} implementation or use one of the
     *  predefined ones like {@link EventProcessor#COUPLED_STRICT}, {@link EventProcessor#COUPLED}
     *  or {@link EventProcessor#DECOUPLED}.
     *
     * @param newEventProcessor The new {@link EventProcessor}.
     * @return A new {@link SwingTreeInitConfig} instance with the new {@link EventProcessor}.
     */
    public SwingTreeInitConfig eventProcessor( EventProcessor newEventProcessor ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, newEventProcessor, _styleSheet, _uiScale, _uiScaleEnabled, _uiScaleAllowScaleDown, _defaultAnimationInterval);
    }

    /**
     *  Used to configure a global {@link StyleSheet} serving as a base for all
     *  {@link StyleSheet}s used inside your application (see {@link UI#use(swingtree.style.StyleSheet, Supplier)}).
     *
     * @param newStyleSheet The new {@link StyleSheet}, or {@code null} to unset the {@link StyleSheet}.
     * @return A new {@link SwingTreeInitConfig} instance with the new {@link StyleSheet}.
     */
    public SwingTreeInitConfig styleSheet( StyleSheet newStyleSheet ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, _eventProcessor, newStyleSheet, _uiScale, _uiScaleEnabled, _uiScaleAllowScaleDown, _defaultAnimationInterval);
    }

    /**
     *  Use this to configure the UI scaling factor.
     *  The default factor determined by the system property {@code "swingtree.uiScale"}.
     *  <p>
     *  If Java runtime scales (Java 9 or later), this scale factor is applied on top
     *  of the Java system scale factor. Java 8 does not scale and this scale factor
     *  replaces the user scale factor that SwingTree computes based on the font.
     *  To replace the Java 9+ system scale factor, use system property "sun.java2d.uiScale",
     *  which has the same syntax as this one.
     *  <p>
     *  <strong>Allowed Values</strong> e.g. {@code 1.5}, {@code 1.5x}, {@code 150%} or {@code 144dpi} (96dpi is 100%)<br>
     *
     * @param newUiScale The new UI scaling factor.
     * @return A new {@link SwingTreeInitConfig} instance with the new UI scaling factor.
     */
    public SwingTreeInitConfig uiScaleFactor( float newUiScale ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, _eventProcessor, _styleSheet, newUiScale, _uiScaleEnabled, _uiScaleAllowScaleDown, _defaultAnimationInterval);
    }

    /**
     *  Used to configure whether the UI scaling mode is enabled as is specified by the
     *  system property {@code swingtree.uiScale.enabled}.
     *  <p>
     *  <strong>Allowed Values</strong> {@code false} and {@code true}<br>
     *  <strong>Default</strong> {@code true}
     *
     * @param newUiScaleEnabled The new UI scaling mode.
     * @return A new {@link SwingTreeInitConfig} instance with the new UI scaling mode.
     */
    public SwingTreeInitConfig isUiScaleFactorEnabled( boolean newUiScaleEnabled ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, _eventProcessor, _styleSheet, _uiScale, newUiScaleEnabled, _uiScaleAllowScaleDown, _defaultAnimationInterval);
    }

    /**
     *  Used to configure whether values smaller than 100% are allowed for the user scale factor
     *  as is specified by the system property {@code swingtree.uiScale.allowScaleDown}.
     *  <p>
     *  <strong>Allowed Values</strong> {@code false} and {@code true}<br>
     *  <strong>Default</strong> {@code false}
     *
     * @param newUiScaleAllowScaleDown The new UI scaling mode.
     * @return A new {@link SwingTreeInitConfig} instance with the new UI scaling mode.
     */
    public SwingTreeInitConfig isUiScaleDownAllowed( boolean newUiScaleAllowScaleDown ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, _eventProcessor, _styleSheet, _uiScale, _uiScaleEnabled, newUiScaleAllowScaleDown, _defaultAnimationInterval);
    }

    /**
     *  Used to configure the default animation interval in milliseconds,
     *  which is a property that
     *  determines the delay between two consecutive animation steps.
     *  You can think of it as the time between the heartbeats of the animation.
     *  The smaller the interval, the higher the refresh rate and
     *  the smoother the animation will look.
     *  However, the smaller the interval, the more CPU time will be used.
     *  The default interval is 16 ms which corresponds to 60 fps.
     *  See {@link #standard()}, returning an instance of this config with the default value. <br>
     *  This property is used as default value by the {@link swingtree.animation.LifeTime}
     *  object which is used to define the duration of an {@link swingtree.animation.Animation}.
     *
     * @param newDefaultAnimationInterval The new default animation interval.
     * @return A new {@link SwingTreeInitConfig} instance with the new default animation interval.
     */
    public SwingTreeInitConfig defaultAnimationInterval( long newDefaultAnimationInterval ) {
        return new SwingTreeInitConfig(_defaultFont, _fontInstallation, _eventProcessor, _styleSheet, _uiScale, _uiScaleEnabled, _uiScaleAllowScaleDown, newDefaultAnimationInterval);
    }

    /**
     * Defines/documents own system properties used in SwingTree.
     *
     * @author Daniel Nepp, but originally a derivative work of Karl Tauber
     */
    private static interface SystemProperties
    {
        /**
         * Specifies a custom scale factor used to scale the UI.
         * <p>
         * If Java runtime scales (Java 9 or later), this scale factor is applied on top
         * of the Java system scale factor. Java 8 does not scale and this scale factor
         * replaces the user scale factor that SwingTree computes based on the font.
         * To replace the Java 9+ system scale factor, use system property "sun.java2d.uiScale",
         * which has the same syntax as this one.
         * <p>
         * <strong>Allowed Values</strong> e.g. {@code 1.5}, {@code 1.5x}, {@code 150%} or {@code 144dpi} (96dpi is 100%)<br>
         */
        String UI_SCALE = "swingtree.uiScale";

        /**
         * Specifies whether user scaling mode is enabled.
         * <p>
         * <strong>Allowed Values</strong> {@code false} and {@code true}<br>
         * <strong>Default</strong> {@code true}
         */
        String UI_SCALE_ENABLED = "swingtree.uiScale.enabled";

        /**
         * Specifies whether values smaller than 100% are allowed for the user scale factor
         * (see {@link UI#scale()}).
         * <p>
         * <strong>Allowed Values</strong> {@code false} and {@code true}<br>
         * <strong>Default</strong> {@code false}
         */
        String UI_SCALE_ALLOW_SCALE_DOWN = "swingtree.uiScale.allowScaleDown";

        /**
         * Specifies the default animation interval in milliseconds.
         * <p>
         * This property is used as default value by the {@link swingtree.animation.LifeTime}
         * object which is used to define the duration of an {@link swingtree.animation.Animation}.
         * <p>
         * <strong>Allowed Values</strong> must be a positive integer<br>
         * <strong>Default</strong> {@code 16}
         */
        String ANIMATION_INTERVAL = "swingtree.animationInterval";

        /**
         * Checks whether a system property is set and returns {@code true} if its value
         * is {@code "true"} (case-insensitive), otherwise it returns {@code false}.
         * If the system property is not set, {@code defaultValue} is returned.
         */
        static boolean getBool(String key, boolean defaultValue ) {
            String value = System.getProperty( key );
            return (value != null) ? Boolean.parseBoolean( value ) : defaultValue;
        }

        /**
         * Similar to sun.java2d.SunGraphicsEnvironment.getScaleFactor(String)
         */
        static float getFloat(String key, float defaultValue ) {
            String s = System.getProperty( key );

            if ( s == null )
                return defaultValue;

            float units = 1;
            if ( s.endsWith( "x" ) )
                s = s.substring( 0, s.length() - 1 );
            else if ( s.endsWith( "dpi" ) ) {
                units = 96;
                s = s.substring( 0, s.length() - 3 );
            } else if ( s.endsWith( "%" ) ) {
                units = 100;
                s = s.substring( 0, s.length() - 1 );
            }

            try {
                float scale = Float.parseFloat( s );
                return scale > 0 ? scale / units : -1;
            } catch( NumberFormatException ex ) {
                return defaultValue;
            }
        }

        static long getLong(String key, long defaultValue ) {
            String s = System.getProperty( key );

            if ( s == null )
                return defaultValue;

            try {
                long interval = Long.parseLong( s );
                return interval > 0 ? interval : defaultValue;
            } catch( NumberFormatException ex ) {
                return defaultValue;
            }
        }
    }
}
