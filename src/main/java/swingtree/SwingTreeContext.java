package swingtree;

import swingtree.style.StyleSheet;
import swingtree.threading.EventProcessor;

import javax.swing.*;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 *  A {@link SwingTreeContext} is a singleton that holds global configuration for the SwingTree library.
 *  This includes the {@link EventProcessor} that is used to process events, as well as the
 *  {@link StyleSheet} that is used to style components.
 *
 * @author Daniel Nepp
 */
public final class SwingTreeContext
{
	private static SwingTreeContext _INSTANCES = null;

	/**
	 * Returns the singleton instance of the {@link SwingTreeContext}.
	 * Note that this method will create the singleton if it does not exist.
	 * @return the singleton instance of the {@link SwingTreeContext}.
	 */
	public static SwingTreeContext get() {
		// We make sure this method is thread-safe by using the double-checked locking idiom.
		if ( _INSTANCES == null ) {
			synchronized ( SwingTreeContext.class ) {
				if ( _INSTANCES == null ) {
					_INSTANCES = new SwingTreeContext();
				}
			}
		}
		return _INSTANCES;
	}

    public static void reset() {
        _INSTANCES = null;
    }


	private EventProcessor _eventProcessor = EventProcessor.COUPLED_STRICT;
	private StyleSheet _styleSheet = null;

    private final UIScale uiScale;


	private SwingTreeContext() {
        this.uiScale = new UIScale();
    }

    public UIScale getUIScale() { return uiScale; }

	/**
	 * @return The currently configured {@link EventProcessor} that is used to process
	 *         GUI and application events.
	 */
	public EventProcessor getEventProcessor() {
		return _eventProcessor;
	}

	/**
	 * Sets the {@link EventProcessor} that is used to process GUI and application events.
	 * @param eventProcessor the {@link EventProcessor} that is used to process GUI and application events.
	 */
	public void setEventProcessor( EventProcessor eventProcessor ) {
		_eventProcessor = Objects.requireNonNull(eventProcessor);
	}

	/**
	 * @return The currently configured {@link StyleSheet} that is used to style components.
	 */
	public Optional<StyleSheet> getStyleSheet() {
		return Optional.ofNullable(_styleSheet);
	}

	/**
	 * Sets the {@link StyleSheet} that is used to style components.
	 * @param styleSheet the {@link StyleSheet} that is used to style components.
	 */
	public void setStyleSheet(StyleSheet styleSheet) {
		_styleSheet = styleSheet;
	}

    /**
     * This class handles scaling in Swing UIs.
     * It computes user scaling factor based on font size and
     * provides methods to scale integer, float, {@link Dimension} and {@link Insets}.
     * This class is look and feel independent.
     * <p>
     * Two scaling modes are supported by SwingTree for HiDPI displays:
     *
     * <h2>1) system scaling mode</h2>
     *
     * This mode is supported since Java 9 on all platforms and in some Java 8 VMs
     * (e.g. Apple and JetBrains). The JRE determines the scale factor per-display and
     * adds a scaling transformation to the graphics object.
     * E.g. invokes {@code java.awt.Graphics2D.scale( 1.5, 1.5 )} for 150%.
     * So the JRE does the scaling itself.
     * E.g. when you draw a 10px line, a 15px line is drawn on screen.
     * The scale factor may be different for each connected display.
     * The scale factor may change for a window when moving the window from one display to another one.
     *
     * <h2>2) user scaling mode</h2>
     *
     * This mode is mainly for Java 8 compatibility, but is also used on Linux
     * or if the default font is changed.
     * The user scale factor is computed based on the used font.
     * The JRE does not scale anything.
     * So we have to invoke {@link #scale(float)} where necessary.
     * There is only one user scale factor for all displays.
     * The user scale factor may change if the active LaF, "defaultFont" or "Label.font" has changed.
     * If system scaling mode is available the user scale factor is usually 1,
     * but may be larger on Linux or if the default font is changed.
     *
     * @author Daniel Nepp, but a derivative work originally from Karl Tauber (com.formdev.flatlaf.util.UIScale)
     */
    public static final class UIScale
    {
        private final boolean DEBUG = false;

        private PropertyChangeSupport changeSupport;

        private Boolean jreHiDPI;

        private float scaleFactor = 1;
        private boolean initialized;


        private UIScale() {} // prevent instantiation

        public void addPropertyChangeListener( PropertyChangeListener listener ) {
            if( changeSupport == null )
                changeSupport = new PropertyChangeSupport( UIScale.class );
            changeSupport.addPropertyChangeListener( listener );
        }

        public void removePropertyChangeListener( PropertyChangeListener listener ) {
            if( changeSupport == null )
                return;
            changeSupport.removePropertyChangeListener( listener );
        }

        //---- system scaling (Java 9) --------------------------------------------

        /**
         * Returns whether system scaling is enabled.
         */
        public boolean isSystemScalingEnabled() {
            if( jreHiDPI != null )
                return jreHiDPI;

            jreHiDPI = false;

            if( SystemInfo.isJava_9_orLater ) {
                // Java 9 and later supports per-monitor scaling
                jreHiDPI = true;
            } else if( SystemInfo.isJetBrainsJVM ) {
                // IntelliJ IDEA ships its own JetBrains Java 8 JRE that may support per-monitor scaling
                // see com.intellij.ui.JreHiDpiUtil.isJreHiDPIEnabled()
                try {
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    Class<?> sunGeClass = Class.forName( "sun.java2d.SunGraphicsEnvironment" );
                    if( sunGeClass.isInstance( ge ) ) {
                        Method m = sunGeClass.getDeclaredMethod( "isUIScaleOn" );
                        jreHiDPI = (Boolean) m.invoke( ge );
                    }
                } catch( Throwable ex ) {
                    // ignore
                }
            }

            return jreHiDPI;
        }

        /**
         * Returns the system scale factor for the given graphics context.
         */
        public double getSystemScaleFactor( Graphics2D g ) {
            return isSystemScalingEnabled() ? getSystemScaleFactor( g.getDeviceConfiguration() ) : 1;
        }

        /**
         * Returns the system scale factor for the given graphics configuration.
         */
        public double getSystemScaleFactor( GraphicsConfiguration gc ) {
            return (isSystemScalingEnabled() && gc != null) ? gc.getDefaultTransform().getScaleX() : 1;
        }

        //---- user scaling (Java 8) ----------------------------------------------

        private void initialize() {
            if( initialized )
                return;
            initialized = true;

            if( !isUserScalingEnabled() )
                return;

            // listener to update scale factor if LaF changed, "defaultFont" or "Label.font" changed
            PropertyChangeListener listener = new PropertyChangeListener() {
                @Override
                public void propertyChange( PropertyChangeEvent e ) {
                    switch( e.getPropertyName() ) {
                        case "lookAndFeel":
                            // it is not necessary (and possible) to remove listener of old LaF defaults
                            if( e.getNewValue() instanceof LookAndFeel)
                                UIManager.getLookAndFeelDefaults().addPropertyChangeListener( this );
                            updateScaleFactor();
                            break;

                        case "defaultFont":
                        case "Label.font":
                            updateScaleFactor();
                            break;
                    }
                }
            };
            UIManager.addPropertyChangeListener( listener );
            UIManager.getDefaults().addPropertyChangeListener( listener );
            UIManager.getLookAndFeelDefaults().addPropertyChangeListener( listener );

            updateScaleFactor();
        }

        public void reset() {
            scaleFactor = 1;
            initialized = false;
        }

        private void updateScaleFactor() {
            if( !isUserScalingEnabled() )
                return;

            // apply custom scale factor specified in system property "style.uiScale"
            float customScaleFactor = getCustomScaleFactor();
            if( customScaleFactor > 0 ) {
                setUserScaleFactor( customScaleFactor, false );
                return;
            }

            // use font size to calculate scale factor (instead of DPI)
            // because even if we are on a HiDPI display it is not sure
            // that a larger font size is set by the current LaF
            // (e.g. can avoid large icons with small text)
            Font font = UIManager.getFont( "defaultFont" );
            if( font == null )
                font = UIManager.getFont( "Label.font" );

            setUserScaleFactor( computeFontScaleFactor( font ), true );
        }

        /**
         * For internal use only.
         *
         * @since 2
         */
        public float computeFontScaleFactor( Font font ) {
            if( SystemInfo.isWindows ) {
                // Special handling for Windows to be compatible with OS scaling,
                // which distinguish between "screen scaling" and "text scaling".
                //  - Windows "screen scaling" scales everything (text, icon, gaps, etc)
                //    and may have different scaling factors for each screen.
                //  - Windows "text scaling" increases only the font size, but on all screens.
                //
                // Both can be changed by the user in the Windows 10 Settings:
                //  - Settings > Display > Scale and layout
                //  - Settings > Ease of Access > Display > Make text bigger (100% - 225%)
                if( font instanceof UIResource) {
                    Font uiFont = (Font) Toolkit.getDefaultToolkit().getDesktopProperty( "win.messagebox.font" );
                    if( uiFont == null || uiFont.getSize() == font.getSize() ) {
                        if( isSystemScalingEnabled() ) {
                            // Do not apply own scaling if the JRE scales using Windows screen scale factor.
                            // If user increases font size in Windows 10 settings, desktop property
                            // "win.messagebox.font" is changed and FlatLaf uses the larger font.
                            return 1;
                        } else {
                            // If the JRE does not scale (Java 8), the size of the UI font
                            // (usually from desktop property "win.messagebox.font")
                            // combines the Windows screen and text scale factors.
                            // But the font in desktop property "win.defaultGUI.font" is only
                            // scaled with the Windows screen scale factor. So use it to compute
                            // our scale factor that is equal to Windows screen scale factor.
                            Font winFont = (Font) Toolkit.getDefaultToolkit().getDesktopProperty( "win.defaultGUI.font" );
                            return computeScaleFactor( (winFont != null) ? winFont : font );
                        }
                    }
                }

                // If font was explicitly set from outside (is not a UIResource),
                // or was set in FlatLaf properties files (is a UIResource),
                // use it to compute scale factor. This allows applications to
                // use custom fonts (e.g. that the user can change in UI) and
                // get scaling if a larger font size is used.
                // E.g. FlatLaf Demo supports increasing font size in "Font" menu and UI scales.
            }

            return computeScaleFactor( font );
        }

        private static float computeScaleFactor( Font font ) {
            // default font size
            float fontSizeDivider = 12f;

            if( SystemInfo.isWindows ) {
                // Windows LaF uses Tahoma font rather than the actual Windows system font (Segoe UI),
                // and its size is always ca. 10% smaller than the actual system font size.
                // Tahoma 11 is used at 100%
                if( "Tahoma".equals( font.getFamily() ) )
                    fontSizeDivider = 11f;
            } else if( SystemInfo.isMacOS ) {
                // default font size on macOS is 13
                fontSizeDivider = 13f;
            } else if( SystemInfo.isLinux ) {
                // default font size for Unity and Gnome is 15 and for KDE it is 13
                fontSizeDivider = SystemInfo.isKDE ? 13f : 15f;
            }

            return font.getSize() / fontSizeDivider;
        }

        private static boolean isUserScalingEnabled() {
            return SystemProperties.getBoolean( SystemProperties.UI_SCALE_ENABLED, true );
        }

        /**
         * Applies a custom scale factor given in system property "style.uiScale"
         * to the given font.
         */
        public FontUIResource applyCustomScaleFactor(FontUIResource font ) {
            if( !isUserScalingEnabled() )
                return font;

            float scaleFactor = getCustomScaleFactor();
            if( scaleFactor <= 0 )
                return font;

            float fontScaleFactor = computeScaleFactor( font );
            if( scaleFactor == fontScaleFactor )
                return font;

            int newFontSize = Math.max( Math.round( (font.getSize() / fontScaleFactor) * scaleFactor ), 1 );
            return new FontUIResource( font.deriveFont( (float) newFontSize ) );
        }

        /**
         * Get custom scale factor specified in system property "style.uiScale".
         */
        private static float getCustomScaleFactor() {
            return parseScaleFactor( System.getProperty( SystemProperties.UI_SCALE ) );
        }

        /**
         * Similar to sun.java2d.SunGraphicsEnvironment.getScaleFactor(String)
         */
        private static float parseScaleFactor( String s ) {
            if( s == null )
                return -1;

            float units = 1;
            if( s.endsWith( "x" ) )
                s = s.substring( 0, s.length() - 1 );
            else if( s.endsWith( "dpi" ) ) {
                units = 96;
                s = s.substring( 0, s.length() - 3 );
            } else if( s.endsWith( "%" ) ) {
                units = 100;
                s = s.substring( 0, s.length() - 1 );
            }

            try {
                float scale = Float.parseFloat( s );
                return scale > 0 ? scale / units : -1;
            } catch( NumberFormatException ex ) {
                return -1;
            }
        }

        /**
         * Returns the user scale factor.
         */
        public float getUserScaleFactor() {
            initialize();
            return scaleFactor;
        }

        public void setUserScaleFactor( float scaleFactor ) {
            initialize();
            setUserScaleFactor( scaleFactor, true );
        }

        /**
         * Sets the user scale factor.
         */
        private void setUserScaleFactor( float scaleFactor, boolean normalize ) {
            if( normalize ) {
                if( scaleFactor < 1f ) {
                    scaleFactor = SystemProperties.getBoolean( SystemProperties.UI_SCALE_ALLOW_SCALE_DOWN, false )
                            ? Math.round( scaleFactor * 10f ) / 10f // round small scale factor to 1/10
                            : 1f;
                } else if( scaleFactor > 1f ) // round scale factor to 1/4
                    scaleFactor = Math.round( scaleFactor * 4f ) / 4f;
            }

            // minimum scale factor
            scaleFactor = Math.max( scaleFactor, 0.1f );

            float oldScaleFactor = this.scaleFactor;
            this.scaleFactor = scaleFactor;

            if( DEBUG )
                System.out.println( "HiDPI scale factor " + scaleFactor );

            if( changeSupport != null )
                changeSupport.firePropertyChange( "userScaleFactor", oldScaleFactor, scaleFactor );
        }

        /**
         * Multiplies the given value by the user scale factor.
         */
        public float scale( float value ) {
            initialize();
            return (scaleFactor == 1) ? value : (value * scaleFactor);
        }

        /**
         * Multiplies the given value by the user scale factor.
         */
        public double scale( double value ) {
            initialize();
            return (scaleFactor == 1) ? value : (value * scaleFactor);
        }

        /**
         * Multiplies the given value by the user scale factor and rounds the result.
         */
        public int scale( int value ) {
            initialize();
            return (scaleFactor == 1) ? value : Math.round( value * scaleFactor );
        }

        /**
         * Similar as {@link #scale(int)} but always "rounds down".
         * <p>
         * For use in special cases. {@link #scale(int)} is the preferred method.
         */
        public int scale2( int value ) {
            initialize();
            return (scaleFactor == 1) ? value : (int) (value * scaleFactor);
        }

        /**
         * Divides the given value by the user scale factor.
         */
        public float unscale( float value ) {
            initialize();
            return (scaleFactor == 1f) ? value : (value / scaleFactor);
        }

        /**
         * Divides the given value by the user scale factor and rounds the result.
         */
        public int unscale( int value ) {
            initialize();
            return (scaleFactor == 1f) ? value : Math.round( value / scaleFactor );
        }

        /**
         * If user scale factor is not 1, scale the given graphics context by invoking
         * {@link Graphics2D#scale(double, double)} with user scale factor.
         */
        public void scaleGraphics( Graphics2D g ) {
            initialize();
            if( scaleFactor != 1f )
                g.scale( scaleFactor, scaleFactor );
        }

        /**
         * Scales the given dimension with the user scale factor.
         * <p>
         * If user scale factor is 1, then the given dimension is simply returned.
         * Otherwise, a new instance of {@link Dimension} or {@link DimensionUIResource}
         * is returned, depending on whether the passed dimension implements {@link UIResource}.
         */
        public Dimension scale( Dimension dimension ) {
            initialize();
            return (dimension == null || scaleFactor == 1f)
                    ? dimension
                    : (dimension instanceof UIResource
                    ? new DimensionUIResource( scale( dimension.width ), scale( dimension.height ) )
                    : new Dimension          ( scale( dimension.width ), scale( dimension.height ) ));
        }

        /**
         * Scales the given insets with the user scale factor.
         * <p>
         * If user scale factor is 1, then the given insets is simply returned.
         * Otherwise, a new instance of {@link Insets} or {@link InsetsUIResource}
         * is returned, depending on whether the passed dimension implements {@link UIResource}.
         */
        public Insets scale( Insets insets ) {
            initialize();
            return (insets == null || scaleFactor == 1f)
                    ? insets
                    : (insets instanceof UIResource
                    ? new InsetsUIResource( scale( insets.top ), scale( insets.left ), scale( insets.bottom ), scale( insets.right ) )
                    : new Insets          ( scale( insets.top ), scale( insets.left ), scale( insets.bottom ), scale( insets.right ) ));
        }
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
         * replaces the user scale factor that FlatLaf computes based on the font.
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
         * Checks whether a system property is set and returns {@code true} if its value
         * is {@code "true"} (case-insensitive), otherwise it returns {@code false}.
         * If the system property is not set, {@code defaultValue} is returned.
         */
        static boolean getBoolean( String key, boolean defaultValue ) {
            String value = System.getProperty( key );
            return (value != null) ? Boolean.parseBoolean( value ) : defaultValue;
        }
    }

    /**
     * Provides information about the current system.
     *
     * @author Daniel Nepp, but originally a derivative work of
     *         Karl Tauber (com.formdev.flatlaf.util.SystemInfo)
     */
    static final class SystemInfo
    {
        // platforms
        public static final boolean isWindows;
        public static final boolean isMacOS;
        public static final boolean isLinux;

        // OS versions
        public static final long osVersion;
        public static final boolean isWindows_10_orLater;
        /** <strong>Note</strong>: This requires Java 8u321, 11.0.14, 17.0.2 or 18 (or later).
         * (see https://bugs.openjdk.java.net/browse/JDK-8274840)
         **/ public static final boolean isWindows_11_orLater;
        public static final boolean isMacOS_10_11_ElCapitan_orLater;
        public static final boolean isMacOS_10_14_Mojave_orLater;
        public static final boolean isMacOS_10_15_Catalina_orLater;

        // OS architecture
        public static final boolean isX86;
        public static final boolean isX86_64;
        public static final boolean isAARCH64;

        // Java versions
        public static final long javaVersion;
        public static final boolean isJava_9_orLater;
        public static final boolean isJava_11_orLater;
        public static final boolean isJava_15_orLater;
        public static final boolean isJava_17_orLater;
        public static final boolean isJava_18_orLater;

        // Java VMs
        public static final boolean isJetBrainsJVM;
        public static final boolean isJetBrainsJVM_11_orLater;

        // UI toolkits
        public static final boolean isKDE;

        // other
        public static final boolean isProjector;
        public static final boolean isWebswing;
        public static final boolean isWinPE;

        static {
            // platforms
            String osName = System.getProperty( "os.name" ).toLowerCase( Locale.ENGLISH );
            isWindows = osName.startsWith( "windows" );
            isMacOS = osName.startsWith( "mac" );
            isLinux = osName.startsWith( "linux" );

            // OS versions
            osVersion = scanVersion( System.getProperty( "os.version" ) );
            isWindows_10_orLater = (isWindows && osVersion >= toVersion( 10, 0, 0, 0 ));
            isWindows_11_orLater = (isWindows_10_orLater && osName.length() > "windows ".length() &&
                    scanVersion( osName.substring( "windows ".length() ) ) >= toVersion( 11, 0, 0, 0 ));
            isMacOS_10_11_ElCapitan_orLater = (isMacOS && osVersion >= toVersion( 10, 11, 0, 0 ));
            isMacOS_10_14_Mojave_orLater = (isMacOS && osVersion >= toVersion( 10, 14, 0, 0 ));
            isMacOS_10_15_Catalina_orLater = (isMacOS && osVersion >= toVersion( 10, 15, 0, 0 ));

            // OS architecture
            String osArch = System.getProperty( "os.arch" );
            isX86 = osArch.equals( "x86" );
            isX86_64 = osArch.equals( "amd64" ) || osArch.equals( "x86_64" );
            isAARCH64 = osArch.equals( "aarch64" );

            // Java versions
            javaVersion = scanVersion( System.getProperty( "java.version" ) );
            isJava_9_orLater = (javaVersion >= toVersion( 9, 0, 0, 0 ));
            isJava_11_orLater = (javaVersion >= toVersion( 11, 0, 0, 0 ));
            isJava_15_orLater = (javaVersion >= toVersion( 15, 0, 0, 0 ));
            isJava_17_orLater = (javaVersion >= toVersion( 17, 0, 0, 0 ));
            isJava_18_orLater = (javaVersion >= toVersion( 18, 0, 0, 0 ));

            // Java VMs
            isJetBrainsJVM = System.getProperty( "java.vm.vendor", "Unknown" )
                    .toLowerCase( Locale.ENGLISH ).contains( "jetbrains" );
            isJetBrainsJVM_11_orLater = isJetBrainsJVM && isJava_11_orLater;

            // UI toolkits
            isKDE = (isLinux && System.getenv( "KDE_FULL_SESSION" ) != null);

            // other
            isProjector = Boolean.getBoolean( "org.jetbrains.projector.server.enable" );
            isWebswing = (System.getProperty( "webswing.rootDir" ) != null);
            isWinPE = isWindows && "X:\\Windows\\System32".equalsIgnoreCase( System.getProperty( "user.dir" ) );
        }

        public static long scanVersion( String version ) {
            int major = 1;
            int minor = 0;
            int micro = 0;
            int patch = 0;
            try {
                StringTokenizer st = new StringTokenizer( version, "._-+" );
                major = Integer.parseInt( st.nextToken() );
                minor = Integer.parseInt( st.nextToken() );
                micro = Integer.parseInt( st.nextToken() );
                patch = Integer.parseInt( st.nextToken() );
            } catch( Exception ex ) {
                // ignore
            }

            return toVersion( major, minor, micro, patch );
        }

        public static long toVersion( int major, int minor, int micro, int patch ) {
            return ((long) major << 48) + ((long) minor << 32) + ((long) micro << 16) + patch;
        }
    }
}
