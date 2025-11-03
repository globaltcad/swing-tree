package swingtree;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import sprouts.Var;
import sprouts.Viewable;
import swingtree.api.IconDeclaration;
import swingtree.api.Painter;
import swingtree.style.StyleSheet;
import swingtree.threading.EventProcessor;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *  A {@link SwingTree} is a singleton that holds global configuration context for the SwingTree library.
 *  This includes the {@link EventProcessor} that is used to process events, as well as the
 *  {@link StyleSheet} that is used to style components.
 *  <br>
 *  You may access the singleton instance of the {@link SwingTree} class through the {@link #get()} method.
 *
 * @author Daniel Nepp
 */
public final class SwingTree
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(SwingTree.class);

    private static final String _DEFAULT_FONT = "defaultFont";

	private static @Nullable LazyRef<SwingTree> _INSTANCE = new LazyRef<>(SwingTree::new);

	/**
	 * Returns the singleton instance of the {@link SwingTree}.
	 * Note that this method will create the singleton if it does not exist.
	 * @return the singleton instance of the {@link SwingTree}.
	 */
	public static SwingTree get() {
        if ( _INSTANCE == null )
            initialize();
        if ( _INSTANCE == null )
            throw new IllegalStateException("Failed to initialize SwingTree singleton!");

        return _INSTANCE.get();
    }

    /**
     *  Clears the singleton instance of the {@link SwingTree}.
     *  This is useful for testing purposes, or if you want to
     *  reconfigure your application with a different {@link SwingTreeInitConfig}.
     *  (see {@link #initialiseUsing(SwingTreeConfigurator)}).
     */
    public static void clear() { _INSTANCE = null; }

    /**
     *  A lazy initialization of the singleton instance of the {@link SwingTree} class
     *  causing it to be recreated the next time it is requested through {@link #get()}.<br>
     *  This is useful for testing purposes, also in cases where
     *  the UI scale changes (through the reference font).<br>
     *  Also see {@link #initialiseUsing(SwingTreeConfigurator)}.
     */
    public static void initialize() {
        _INSTANCE = new LazyRef<>(SwingTree::new);
    }

    /**
     *  A lazy initialization of the singleton instance of the {@link SwingTree} class
     *  causing it to be recreated the next time it is requested through {@link #get()},<br>
     *  but with a {@link SwingTreeConfigurator} that is used
     *  to configure the {@link SwingTree} instance.<br>
     *  This is useful for testing purposes, but also in cases where
     *  the UI scale must be initialized or changed manually (through the reference font).<br>
     *  Also see {@link #initialize()}.
     *
     * @param configurator the {@link SwingTreeConfigurator} that is used
     *                     to configure the {@link SwingTree} instance.
     */
    public static void initialiseUsing( SwingTreeConfigurator configurator ) {
        _INSTANCE = new LazyRef<>(()->new SwingTree(configurator));
    }

    private SwingTreeInitConfig _config;

    private final LazyRef<UiScale> _uiScale;
    private final Map<IconDeclaration, ImageIcon> _iconCache = new HashMap<>();


    private SwingTree() { this(config -> config); }

	private SwingTree( SwingTreeConfigurator configurator ) {
        _config = _resolveConfiguration(configurator);
        _uiScale = new LazyRef<>( () -> new UiScale(_config) );
        _establishMainFont(_config);
    }

    private SwingTreeInitConfig _resolveConfiguration( SwingTreeConfigurator configurator ) {
        try {
            Objects.requireNonNull(configurator);
            SwingTreeInitConfig config = configurator.configure(SwingTreeInitConfig.standard());
            Objects.requireNonNull(config);
            return config;
        } catch (Exception ex) {
            log.error("Error resolving SwingTree configuration", ex);
            ex.printStackTrace();
            return SwingTreeInitConfig.standard();
        }
    }

    private static void _establishMainFont( SwingTreeInitConfig config ) {
        try {
            if (config.fontInstallation() == SwingTreeInitConfig.FontInstallation.HARD)
                config.defaultFont().ifPresent(font -> {
                    if (font instanceof FontUIResource)
                        _installFontInUIManager((FontUIResource) font);
                    else
                        _installFontInUIManager(new FontUIResource(font));
                });
        } catch (Exception ex) {
            log.error("Error installing font in UIManager", ex);
            ex.printStackTrace();
        }
    }

    private static void _installFontInUIManager(javax.swing.plaf.FontUIResource f){
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while ( keys.hasMoreElements() ) {
            Object key = keys.nextElement();
            Object value = UIManager.get (key);
            if ( value instanceof javax.swing.plaf.FontUIResource )
                UIManager.put(key, f);
        }
    }

    /**
     *  The icon cash is a hash map that uses an {@link IconDeclaration} as a key
     *  and an {@link ImageIcon} as a value. This is used to cache icons that are loaded
     *  from the file system using convenience methods like
     *  {@link swingtree.UI#findIcon(String)} and {@link swingtree.UI#findIcon(IconDeclaration)} or
     *  {@link swingtree.UI#findSvgIcon(String)}, {@link swingtree.UI#findSvgIcon(IconDeclaration)}.<br>
     *  Note that the map returned by this method is mutable and can be used to add or remove icons
     *  from the cache. <b>You may also want to consider this as a possible source of memory leaks.</b>
     *
     * @return The icon cache of this context, which is used to cache icons
     *         that are loaded from the file system.
     */
    public Map<IconDeclaration, ImageIcon> getIconCache() { return _iconCache; }

    /**
     * Returns the user scale factor is a scaling factor is used by SwingTree's
     * style engine to scale the UI during painting.
     * Note that this is different from the system/Graphics2D scale factor, which is
     * the scale factor that the JRE uses to scale everything through the
     * {@link java.awt.geom.AffineTransform} of the {@link Graphics2D}.
     * <p>
     * Use this scaling factor for painting operations that are not performed
     * by SwingTree's style engine, e.g. custom painting
     * (see {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, Painter)}).
     * <p>
     * You can configure this scaling factor through the library initialization
     * method {@link SwingTree#initialiseUsing(SwingTreeConfigurator)},
     * or directly through the system property "swingtree.uiScale".
     *
     * @return The user scale factor.
     */
    public float getUiScaleFactor() {
        return _uiScale.get().getUserScaleFactor();
    }

    /**
     * Sets the user scale factor is a scaling factor that is used by SwingTree's
     * style engine to scale the UI during painting.
     * Note that this is different from the system/Graphics2D scale factor, which is
     * the scale factor that the JRE uses to scale everything through the
     * {@link java.awt.geom.AffineTransform} of the {@link Graphics2D}.
     * <p>
     * Use this scaling factor for painting operations that are not performed
     * by SwingTree's style engine, e.g. custom painting
     * (see {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, Painter)}).
     * <p>
     * You can configure this scaling factor through the library initialization
     * method {@link SwingTree#initialiseUsing(SwingTreeConfigurator)},
     * or directly through the system property "swingtree.uiScale".
     *
     * @param scaleFactor The user scale factor.
     */
    public void setUiScaleFactor( float scaleFactor ) {
        log.debug("Changing UI scale factor from {} to {} now.", _uiScale.get().getUserScaleFactor(), scaleFactor);
        if ( UI.thisIsUIThread() )
            _uiScale.get().setUserScaleFactor(scaleFactor);
        else
            UI.runNow(()->{
                _uiScale.get().setUserScaleFactor(scaleFactor);
            });
    }

    /**
     * Creates and returns a reactive {@link Viewable} of the library context's user scale factor
     * which will update itself and invoke all of its change listeners when the user scale factor changes,
     * through methods like {@link #setUiScaleFactor(float)}.<br>
     * If you no longer reference a reactive property view strongly in your
     * code, then it will be garbage collected alongside all of its change
     * listeners automatically for you!<br>
     * <p>
     * The user scale factor is a scaling factor that is used by SwingTree's
     * style engine to scale the UI during painting.<br>
     * <p>
     * Note that this is different from the system/Graphics2D scale factor, which is
     * the scale factor that the JRE uses to scale everything through the
     * {@link java.awt.geom.AffineTransform} of the {@link Graphics2D}.
     * <p>
     * Use this scaling factor for painting operations that are not performed
     * by SwingTree's style engine, e.g. custom painting
     * (see {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, Painter)}).
     * <p>
     * You can configure this scaling factor through the library initialization
     * method {@link SwingTree#initialiseUsing(SwingTreeConfigurator)},
     * or directly through the system property "swingtree.uiScale".
     *
     * @return A reactive property holding the current user scale factor used
     *         for scaling the UI of your application. You may hold onto such a view
     *         and register change listeners on it to ensure your components always have
     *         the correct scale!
     */
    public Viewable<Float> createAndGetUiScaleView() {
        return _uiScale.get().createScaleFactorViewable();
    }

    /**
     *  Returns whether the UI scaling mode is enabled as is specified by
     *  the system property {@code swingtree.uiScale.enabled}.
     */
    public boolean isUiScaleFactorEnabled() {
        return _config.isUiScaleFactorEnabled();
    }

    /**
     * Applies a custom scale factor given in system property "swingtree.uiScale"
     * to the given font.
     */
    public Font scale( Font font ) {
        if( !isUiScaleFactorEnabled() )
            return font;

        float scaleFactor = getUiScaleFactor();
        if( scaleFactor <= 0 || scaleFactor == 1 )
            return font;

        int newFontSize = Math.max( Math.round( font.getSize() * scaleFactor ), 1 );
        return new Font( font.deriveFont( (float) newFontSize ).getAttributes() );
    }

    /**
     * Converts the current scale factor given in system property "swingtree.uiScale"
     * to a font size and the returns a new font derived from the provided one, with that new size!
     */
    public Font applyScaleAsFontSize( Font font ) {
        if( !isUiScaleFactorEnabled() )
            return font;

        float scaleFactor = getUiScaleFactor();
        if( scaleFactor <= 0 )
            return font;

        float fontScaleFactor = UiScale._computeScaleFactorFromFontSize( font );
        if( scaleFactor == fontScaleFactor )
            return font;

        int newFontSize = Math.max( Math.round( (font.getSize() / fontScaleFactor) * scaleFactor ), 1 );
        return new Font( font.deriveFont( (float) newFontSize ).getAttributes() );
    }

    /**
     * Returns whether system scaling is enabled.
     * System scaling means that the JRE scales everything
     * through the {@link java.awt.geom.AffineTransform} of the {@link Graphics2D}.
     * If this is the case, then we do not have to do scaled painting
     * and can use the original size of icons, gaps, etc.
     * @return true if system scaling is enabled.
     */
    public boolean isSystemScalingEnabled() { return UiScale._isSystemScalingEnabled(); }

    /**
     * Returns the system scale factor for the given graphics context.
     * The system scale factor is the scale factor that the JRE uses
     * to scale everything (text, icons, gaps, etc).
     *
     * @param g The graphics context to get the system scale factor for.
     * @return The system scale factor for the given graphics context.
     */
    public double getSystemScaleFactorOf( Graphics2D g ) {
        return UiScale._getSystemScaleFactorOf(g);
    }

    /**
     * Returns the system scale factor.
     * The system scale factor is the scale factor that the JRE uses
     * to scale everything (text, icons, gaps, etc) irrespective of the
     * current look and feel, as this is the scale factor that is used
     * by the {@link java.awt.geom.AffineTransform} of the {@link Graphics2D}.
     *
     * @return The system scale factor.
     */
    public double getSystemScaleFactor() {
        return UiScale._getSystemScaleFactor();
    }

	/**
     *  The {@link EventProcessor} is a simple interface whose implementations
     *  delegate tasks to threads that are capable of processing GUI or application events.
     *  As part of this singleton, the SwingTree library maintains a global
     *  {@link EventProcessor} that is used consistently by all declarative builders.
     *
	 * @return The currently configured {@link EventProcessor} that is used to process
	 *         GUI and application events.
	 */
	public EventProcessor getEventProcessor() {
		return _config.eventProcessor();
	}

	/**
	 * Sets the {@link EventProcessor} that is used to process GUI and application events.
     * You may not pass null as an argument, because SwingTree requires an event processor to function.
     *
	 * @param eventProcessor the {@link EventProcessor} that is used to process GUI and application events.
     * @throws NullPointerException if eventProcessor is null!
	 */
	public void setEventProcessor( EventProcessor eventProcessor ) {
        try {
            _config = _config.eventProcessor(Objects.requireNonNull(eventProcessor));
        } catch (Exception ex) {
            log.error("Error setting event processor", ex);
            ex.printStackTrace();
        }
	}

	/**
     *  The {@link StyleSheet} is an abstract class whose extensions are used to declare
     *  component styles through a CSS like DSL API.
     *  As part of this singleton, the SwingTree library maintains a global
     *  {@link StyleSheet} that is used consistently by all declarative builders.
     *  Use this method to access this global style sheet.
     *
	 * @return The currently configured {@link StyleSheet} that is used to style components.
	 */
	public StyleSheet getStyleSheet() {
        return _config.styleSheet();
	}

	/**
	 * Sets the {@link StyleSheet} that is used to style components.
     * Use {@link StyleSheet#none()} instead of null to switch off global styling.
	 * @param styleSheet The {@link StyleSheet} that is used to style components.
     * @throws NullPointerException if styleSheet is null!
	 */
	public void setStyleSheet( StyleSheet styleSheet ) {
        try {
            _config = _config.styleSheet(Objects.requireNonNull(styleSheet));
        } catch ( Exception ex ) {
            log.error("Error setting style sheet", ex);
            ex.printStackTrace();
        }
	}

    /**
     *  Returns the default animation interval in milliseconds
     *  which is a property that
     *  determines the delay between two consecutive animation steps.
     *  You can think of it as the time between the heartbeats of the animation.
     *  The smaller the interval, the higher the refresh rate and
     *  the smoother the animation will look.
     *  However, the smaller the interval, the more CPU time will be used.
     *  The default interval is 16 ms which corresponds to almost 60 fps.
     *  <br>
     *  This property is used as default value by the {@link swingtree.animation.LifeTime}
     *  object which is used to define the duration of an {@link swingtree.animation.Animation}.
     *  The value returned by this is used by animations
     *  if no other interval is specified through {@link swingtree.animation.LifeTime#withInterval(long, TimeUnit)}.
     * @return The default animation interval in milliseconds.
     */
    public long getDefaultAnimationInterval() {
        return _config.defaultAnimationInterval();
    }

    /**
     *  Sets the default animation interval in milliseconds
     *  which is a property that
     *  determines the delay between two consecutive animation steps.
     *  You can think of it as the time between the heartbeats of the animation.
     *  The smaller the interval, the higher the refresh rate and
     *  the smoother the animation will look.
     *  However, the smaller the interval, the more CPU time will be used.
     *  The default interval is 16 ms which corresponds to almost 60 fps.
     *  <br>
     *  This property is used as default value by the {@link swingtree.animation.LifeTime}
     *  object which is used to define the duration of an {@link swingtree.animation.Animation}.
     *  The value returned by this is used by animations
     *  if no other interval is specified through {@link swingtree.animation.LifeTime#withInterval(long, TimeUnit)}.
     * @param defaultAnimationInterval The default animation interval in milliseconds.
     */
    public void setDefaultAnimationInterval( long defaultAnimationInterval ) {
        _config = _config.defaultAnimationInterval(defaultAnimationInterval);
    }

    /**
     *  Exposes a set of system properties in the form of a nicely formatted string.
     *  These are used by the SwingTree library to determine the system configuration
     *  and to adjust the UI accordingly.
     *
     * @return A string containing system information.
     */
    public String getSystemInfo() {
        return SystemInfo.getAsPrettyString();
    }

    /**
     * This class handles scaling in SwingTree UIs.
     * It computes user scaling factor based on font size and
     * provides methods to scale integer, float, {@link Dimension} and {@link Insets}.
     * This class is look and feel independent.
     * <p>
     * Two scaling modes are supported by SwingTree for HiDPI displays:
     * <p>
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
     * <p>
     * <h2>2) user scaling mode</h2>
     *
     * This mode is mainly for Java 8 compatibility, but is also used on Linux
     * or if the default font is changed.
     * The user scale factor is computed based on the used font.
     * The JRE does not scale anything.
     * So we have to invoke {@link UI#scale(float)} where necessary.
     * There is only one user scale factor for all displays.
     * The user scale factor may change if the active LaF, "defaultFont" or "Label.font" has changed.
     * If system scaling mode is available the user scale factor is usually 1,
     * but may be larger on Linux or if the default font is changed.
     *
     * @author Daniel Nepp, but a derivative work originally from Karl Tauber (com.formdev.flatlaf.util.UIScale)
     */
    static final class UiScale
    {
        private final SwingTreeInitConfig config;

        private static @Nullable Boolean jreHiDPI;

        private final Var<Float> scaleFactor = Var.of(1f);
        private boolean initialized;


        private UiScale( SwingTreeInitConfig config ) // private to prevent instantiation from outside
        {
            this.config = config;
            try {
                // add user scale factor to allow layout managers (e.g. MigLayout) to use it
                UIManager.put( "laf.scaleFactor", (UIDefaults.ActiveValue) t -> {
                    return this.scaleFactor.get();
                });

                if ( config.scalingStrategy() == SwingTreeInitConfig.Scaling.NONE ) {
                    this.scaleFactor.set(1f);
                    this.initialized = true;
                    return;
                }

                if ( config.scalingStrategy() == SwingTreeInitConfig.Scaling.FROM_DEFAULT_FONT ) {
                    Font uiScaleReferenceFont = config.defaultFont().orElse(null);
                    if ( uiScaleReferenceFont != null ) {
                        UIManager.getDefaults().put(_DEFAULT_FONT, uiScaleReferenceFont);
                        log.debug("Setting default font ('{}') to in UIManager to {}", _DEFAULT_FONT, uiScaleReferenceFont);
                    }
                }

                if ( config.scalingStrategy() == SwingTreeInitConfig.Scaling.FROM_SYSTEM_FONT ) {
                    float defaultScale = this.scaleFactor.get();
                    Font highDPIFont = _calculateDPIAwarePlatformFont();
                    boolean updated = _initialize( highDPIFont );
                    if ( this.scaleFactor.isNot(defaultScale) ) {
                        UIManager.getDefaults().put(_DEFAULT_FONT, highDPIFont);
                        log.debug("Setting default font ('{}') to in UIManager to {}", _DEFAULT_FONT, highDPIFont);
                    }
                    if ( updated )
                        _setScalePropertyListeners();
                }
                else
                    _initialize();

            } catch (Exception ex) {
                log.error("Error initializing "+ UiScale.class.getSimpleName(), ex);
                // Usually there should be no exception, if there is one, the library will still work, but
                // the UI may not be scaled correctly. Please report this exception to the library author.
            }
        }

        private Font _calculateDPIAwarePlatformFont()
        {
            FontUIResource dpiAwareFont = null;

            // determine UI font based on operating system
            if( SystemInfo.isWindows ) {
                Font winFont = (Font) Toolkit.getDefaultToolkit().getDesktopProperty( "win.messagebox.font" );
                if( winFont != null ) {
                    if( SystemInfo.isWinPE ) {
                        // on WinPE use "win.defaultGUI.font", which is usually Tahoma,
                        // because Segoe UI font is not available on WinPE
                        Font winPEFont = (Font) Toolkit.getDefaultToolkit().getDesktopProperty( "win.defaultGUI.font" );
                        if ( winPEFont != null )
                            dpiAwareFont = _createCompositeFont( winPEFont.getFamily(), winPEFont.getStyle(), winFont.getSize() );
                    }
                    else
                        dpiAwareFont = _createCompositeFont( winFont.getFamily(), winFont.getStyle(), winFont.getSize() );
                }

            } else if ( SystemInfo.isMacOS ) {
                String fontName;
                if( SystemInfo.isMacOS_10_15_Catalina_orLater ) {
                    if ( SystemInfo.isJetBrainsJVM_11_orLater ) {
                        // See https://youtrack.jetbrains.com/issue/JBR-1915
                        fontName = ".AppleSystemUIFont";
                    } else {
                        // use Helvetica Neue font
                        fontName = "Helvetica Neue";
                    }
                } else if( SystemInfo.isMacOS_10_11_ElCapitan_orLater ) {
                    // use San Francisco Text font
                    fontName = ".SF NS Text";
                } else {
                    // default font on older systems (see com.apple.laf.AquaFonts)
                    fontName = "Lucida Grande";
                }

                dpiAwareFont = _createCompositeFont( fontName, Font.PLAIN, 13 );

            } else if( SystemInfo.isLinux ) {
                try {
                    Font font = LinuxFontPolicy.getFont();
                    dpiAwareFont = (font instanceof FontUIResource) ? (FontUIResource) font : new FontUIResource( font );
                } catch (Exception e) {
                    log.error("Failed to find linux font for scaling!", e);
                }
            }

            // fallback
            if( dpiAwareFont == null )
                dpiAwareFont = _createCompositeFont( Font.SANS_SERIF, Font.PLAIN, 12 );

            // Todo: Look at ActiveFont in FlatLaf to see if we need to do anything with it here.
            // Comment from FlatLaf code base below:
            /*
                // get/remove "defaultFont" from defaults if set in properties files
                // (use remove() to avoid that ActiveFont.createValue() gets invoked)
                Object defaultFont = defaults.remove( _DEFAULT_FONT );
                // use font from OS as base font and derive the UI font from it
                if( defaultFont instanceof ActiveFont ) {
                    Font baseFont = uiFont;
                    uiFont = ((ActiveFont)defaultFont).derive( baseFont, fontSize -> {
                        return Math.round( fontSize * computeFontScaleFactor( baseFont ) );
                    } );
                }
            */

            // increase font size if system property "swingtree.uiScale" is set
            dpiAwareFont = _applyCustomScaleFactor( dpiAwareFont );

            return dpiAwareFont;
        }

        private static FontUIResource _createCompositeFont( String family, int style, int size ) {
            // using StyleContext.getFont() here because it uses
            // sun.font.FontUtilities.getCompositeFontUIResource()
            // and creates a composite font that is able to display all Unicode characters
            Font font = StyleContext.getDefaultStyleContext().getFont( family, style, size );
            return (font instanceof FontUIResource) ? (FontUIResource) font : new FontUIResource( font );
        }

        public Viewable<Float> createScaleFactorViewable() {
            return scaleFactor.view();
        }

        //---- system scaling (Java 9) --------------------------------------------

        /**
         * Returns whether system scaling is enabled.
         * System scaling means that the JRE scales everything
         * through the {@link java.awt.geom.AffineTransform} of the {@link Graphics2D}.
         * If this is the case, then we do not have to do scaled painting
         * and can use the original size of icons, gaps, etc.
         */
        static boolean _isSystemScalingEnabled() {
            if ( jreHiDPI != null )
                return jreHiDPI;

            jreHiDPI = false;

            if ( SystemInfo.isJava_9_orLater ) {
                // Java 9 and later supports per-monitor scaling
                jreHiDPI = true;
            } else if ( SystemInfo.isJetBrainsJVM ) {
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
         * The system scale factor is the scale factor that the JRE uses
         * to scale everything (text, icons, gaps, etc).
         *
         * @param g The graphics context to get the system scale factor for.
         * @return The system scale factor for the given graphics context.
         */
        private static double _getSystemScaleFactorOf( Graphics2D g ) {
            return _isSystemScalingEnabled() ? _getSystemScaleFactorOf( g.getDeviceConfiguration() ) : 1;
        }

        /**
         * @param gc The graphics configuration to get the system scale factor for.
         * @return The system scale factor for the given graphics configuration.
         */
        private static double _getSystemScaleFactorOf( GraphicsConfiguration gc ) {
            return (_isSystemScalingEnabled() && gc != null) ? gc.getDefaultTransform().getScaleX() : 1;
        }

        //---- user scaling (Java 8) ----------------------------------------------

        private void _initialize()
        {
            boolean updated = _initialize( _getDefaultFont() );
            if ( updated )
                _setScalePropertyListeners();
        }

        private boolean _initialize( Font uiScaleReferenceFont )
        {
            if ( initialized )
                return false;

            initialized = true;

            if ( !config.isUiScaleFactorEnabled() )
                return false;

            _setUserScaleFactor( _calculateScaleFactor( uiScaleReferenceFont ) );

            return true;
        }

        private void _setScalePropertyListeners() {
            // listener to update scale factor if LaF changed, "defaultFont" or "Label.font" changed
            PropertyChangeListener listener = new PropertyChangeListener() {
                @Override
                public void propertyChange( PropertyChangeEvent e ) {
                    switch( e.getPropertyName() ) {
                        case "lookAndFeel":
                            // it is not necessary (and possible) to remove listener of old LaF defaults
                            if( e.getNewValue() instanceof LookAndFeel)
                                UIManager.getLookAndFeelDefaults().addPropertyChangeListener( this );

                            _setUserScaleFactor( _calculateScaleFactor( _getDefaultFont() ) );
                            break;

                        case _DEFAULT_FONT:
                        case "Label.font":
                            _setUserScaleFactor( _calculateScaleFactor( _getDefaultFont() ) );
                            break;
                    }
                }
            };
            UIManager.addPropertyChangeListener( listener );
            UIManager.getDefaults().addPropertyChangeListener( listener );
            UIManager.getLookAndFeelDefaults().addPropertyChangeListener( listener );
        }

        private Font _getDefaultFont() {
            // use font size to calculate scale factor (instead of DPI)
            // because even if we are on a HiDPI display it is not sure
            // that a larger font size is set by the current LaF
            // (e.g. can avoid large icons with small text)
            Font font = UIManager.getFont( _DEFAULT_FONT );
            if ( font == null )
                font = UIManager.getFont( "Label.font" );

            return font;
        }

        /**
         * Computes the scale factor based on the given font.
         * @param font font to compute scale factor from
         * @return scale factor, normalized
         */
        private float _calculateScaleFactor( Font font ) {
            // apply custom scale factor specified in system property "swingtree.uiScale"
            float customScaleFactor = config.uiScaleFactor();
            if ( customScaleFactor > 0 ) {
                return customScaleFactor;
            }

            return _normalize(_internalComputeScaleFactorFrom( font ) );
        }

        /**
         * Computes the scale factor based on the given font.
         * @param font font to compute scale factor from
         * @return scale factor
         */
        private float _internalComputeScaleFactorFrom( Font font ) {
            if ( SystemInfo.isWindows ) {
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
                        if( _isSystemScalingEnabled() ) {
                            // Do not apply own scaling if the JRE scales using Windows screen scale factor.
                            // If user increases font size in Windows 10 settings, desktop property
                            // "win.messagebox.font" is changed and SwingTree uses the larger font.
                            return 1;
                        } else {
                            // If the JRE does not scale (Java 8), the size of the UI font
                            // (usually from desktop property "win.messagebox.font")
                            // combines the Windows screen and text scale factors.
                            // But the font in desktop property "win.defaultGUI.font" is only
                            // scaled with the Windows screen scale factor. So use it to compute
                            // our scale factor that is equal to Windows screen scale factor.
                            Font winFont = (Font) Toolkit.getDefaultToolkit().getDesktopProperty( "win.defaultGUI.font" );
                            return _computeScaleFactorFromFontSize( (winFont != null) ? winFont : font );
                        }
                    }
                }

                // If font was explicitly set from outside (is not a UIResource),
                // or was set in SwingTree properties files (is a UIResource),
                // use it to compute scale factor. This allows applications to
                // use custom fonts (e.g. that the user can change in UI) and
                // get scaling if a larger font size is used.
                // E.g. SwingTree Demo supports increasing font size in "Font" menu and UI scales.
            }

            return _computeScaleFactorFromFontSize( font );
        }

        private static float _computeScaleFactorFromFontSize(Font font ) {
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

        /**
         * Applies a custom scale factor given in system property "swingtree.uiScale"
         * to the given font.
         */
        private FontUIResource _applyCustomScaleFactor( FontUIResource font )
        {
            if ( !config.isUiScaleFactorEnabled() )
                return font;

            float scaleFactor = config.uiScaleFactor();
            if ( scaleFactor <= 0 )
                return font;

            float fontScaleFactor = _computeScaleFactorFromFontSize( font );
            if ( scaleFactor == fontScaleFactor )
                return font;

            int newFontSize = Math.max( Math.round( (font.getSize() / fontScaleFactor) * scaleFactor ), 1 );
            return new FontUIResource( font.deriveFont( (float) newFontSize ) );
        }

        /**
         * Returns the user scale factor is a scaling factor is used by SwingTree's
         * style engine to scale the UI during painting.
         * Note that this is different from the system scale factor, which is
         * the scale factor that the JRE uses to scale everything through the
         * {@link java.awt.geom.AffineTransform} of the {@link Graphics2D}.
         * <p>
         * Use this scaling factor for painting operations that are not performed
         * by SwingTree's style engine, e.g. custom painting
         * (see {@link swingtree.style.ComponentStyleDelegate#painter(UI.Layer, Painter)}).
         * <p>
         * You can configure this scaling factor through the library initialization
         * method {@link SwingTree#initialiseUsing(SwingTreeConfigurator)},
         * or through the system property "swingtree.uiScale".
         *
         * @return The user scale factor.
         */
        public float getUserScaleFactor() {
            _initialize();
            return scaleFactor.get();
        }

        public void setUserScaleFactor( float scaleFactor ) {
            _initialize();
            _setUserScaleFactor( _normalize(scaleFactor) );
        }

        private float _normalize( float scaleFactor ) {
            if ( scaleFactor < 1f ) {
                scaleFactor = config.isUiScaleDownAllowed()
                        ? Math.round( scaleFactor * 10f ) / 10f // round small scale factor to 1/10
                        : 1f;
            }
            else if ( scaleFactor > 1f ) // round scale factor to 1/4
                scaleFactor = Math.round( scaleFactor * 4f ) / 4f;

            return scaleFactor;
        }

        /**
         * Sets the user scale factor.
         */
        private void _setUserScaleFactor(float scaleFactor) {
            // minimum scale factor
            scaleFactor = Math.max( scaleFactor, 0.1f );
            this.scaleFactor.set(scaleFactor);
        }

        /**
         * Returns true if the JRE scales, which is the case if:
         *   - environment variable GDK_SCALE is set and running on Java 9 or later
         *   - running on JetBrains Runtime 11 or later and scaling is enabled in system Settings
         */
        static boolean _isSystemScaling() {
            if( GraphicsEnvironment.isHeadless() )
                return true;

            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                          .getDefaultScreenDevice()
                                                          .getDefaultConfiguration();

            return UiScale._getSystemScaleFactorOf( gc ) > 1;
        }

        static double _getSystemScaleFactor() {
            if ( GraphicsEnvironment.isHeadless() )
                return 1;

            return UiScale._getSystemScaleFactorOf(
                                GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                   .getDefaultScreenDevice()
                                                   .getDefaultConfiguration()
                            );
        }

    }

    /**
     * Provides information about the current system.
     *
     * @author Daniel Nepp, but originally a derivative work of
     *         Karl Tauber (com.formdev.flatlaf.util.SystemInfo)
     */
    private static final class SystemInfo
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
         **/
        public static final boolean isWindows_11_orLater;
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

        static String getAsPrettyString() {
            return SystemInfo.class.getSimpleName() + "[\n" +
                    "    isWindows=" + isWindows + ",\n" +
                    "    isMacOS=" + isMacOS + ",\n" +
                    "    isLinux=" + isLinux + ",\n" +
                    "    osVersion=" + osVersion + ",\n" +
                    "    isWindows_10_orLater=" + isWindows_10_orLater + ",\n" +
                    "    isWindows_11_orLater=" + isWindows_11_orLater + ",\n" +
                    "    isMacOS_10_11_ElCapitan_orLater=" + isMacOS_10_11_ElCapitan_orLater + ",\n" +
                    "    isMacOS_10_14_Mojave_orLater=" + isMacOS_10_14_Mojave_orLater + ",\n" +
                    "    isMacOS_10_15_Catalina_orLater=" + isMacOS_10_15_Catalina_orLater + ",\n" +
                    "    isX86=" + isX86 + ",\n" +
                    "    isX86_64=" + isX86_64 + ",\n" +
                    "    isAARCH64=" + isAARCH64 + ",\n" +
                    "    javaVersion=" + javaVersion + ",\n" +
                    "    isJava_9_orLater=" + isJava_9_orLater + ",\n" +
                    "    isJava_11_orLater=" + isJava_11_orLater + ",\n" +
                    "    isJava_15_orLater=" + isJava_15_orLater + ",\n" +
                    "    isJava_17_orLater=" + isJava_17_orLater + ",\n" +
                    "    isJava_18_orLater=" + isJava_18_orLater + ",\n" +
                    "    isJetBrainsJVM=" + isJetBrainsJVM + ",\n" +
                    "    isJetBrainsJVM_11_orLater=" + isJetBrainsJVM_11_orLater + ",\n" +
                    "    isKDE=" + isKDE + ",\n" +
                    "    isProjector=" + isProjector + ",\n" +
                    "    isWebswing=" + isWebswing + ",\n" +
                    "    isWinPE=" + isWinPE + "\n" +
                    "]\n";
        }
    }

    private static class LinuxFontPolicy
    {
        static Font getFont() {
            return SystemInfo.isKDE ? getKDEFont() : getGnomeFont();
        }

        /**
         * Gets the default font for Gnome.
         */
        private static Font getGnomeFont() {
            // see class com.sun.java.swing.plaf.gtk.PangoFonts background information

            Object fontName = Toolkit.getDefaultToolkit().getDesktopProperty( "gnome.Gtk/FontName" );
            if( !(fontName instanceof String) )
                fontName = "sans 10";

            String family = "";
            int style = Font.PLAIN;
            double dsize = 10;

            // parse pango font description
            // see https://developer.gnome.org/pango/1.46/pango-Fonts.html#pango-font-description-from-string
            StringTokenizer st = new StringTokenizer( (String) fontName );
            while( st.hasMoreTokens() ) {
                String word = st.nextToken();

                // remove trailing ',' (e.g. in "Ubuntu Condensed, 11" or "Ubuntu Condensed, Bold 11")
                if( word.endsWith( "," ) )
                    word = word.substring( 0, word.length() - 1 ).trim();

                String lword = word.toLowerCase(Locale.ENGLISH);
                if( lword.equals( "italic" ) || lword.equals( "oblique" ) )
                    style |= Font.ITALIC;
                else if( lword.equals( "bold" ) )
                    style |= Font.BOLD;
                else if( Character.isDigit( word.charAt( 0 ) ) ) {
                    try {
                        dsize = Double.parseDouble( word );
                    } catch( NumberFormatException ex ) {
                        // ignore
                    }
                } else {
                    // remove '-' from "Semi-Bold", "Extra-Light", etc
                    if( lword.startsWith( "semi-" ) || lword.startsWith( "demi-" ) )
                        word = word.substring( 0, 4 ) + word.substring( 5 );
                    else if( lword.startsWith( "extra-" ) || lword.startsWith( "ultra-" ) )
                        word = word.substring( 0, 5 ) + word.substring( 6 );

                    family = family.isEmpty() ? word : (family + ' ' + word);
                }
            }

            // Ubuntu font is rendered poorly (except if running in JetBrains VM)
            // --> use Liberation Sans font
            String useUbuntu = "swingtree.USE_UBUNTU_FONT";

            if( family.startsWith( "Ubuntu" ) && !SystemInfo.isJetBrainsJVM && !getBoolean( useUbuntu, false ) )
                family = "Liberation Sans";

            // scale font size
            dsize *= getGnomeFontScale();
            int size = (int) (dsize + 0.5);
            if( size < 1 )
                size = 1;

            // handle logical font names
            String logicalFamily = mapFcName( family.toLowerCase(Locale.ENGLISH) );
            if( logicalFamily != null )
                family = logicalFamily;

            return createFontEx( family, style, size, dsize );
        }

        /**
         * Checks whether a system property is set and returns {@code true} if its value
         * is {@code "true"} (case-insensitive), otherwise it returns {@code false}.
         * If the system property is not set, {@code defaultValue} is returned.
         */
        static boolean getBoolean( String key, boolean defaultValue ) {
            String value = System.getProperty( key );
            return (value != null) ? Boolean.parseBoolean( value ) : defaultValue;
        }

        /**
         * Create a font for the given family, style and size.
         * If the font family does not match any font on the system,
         * then the last word (usually a font weight) from the family name is removed and tried again.
         * E.g. family 'URW Bookman Light' is not found, but 'URW Bookman' is found.
         * If still not found, then font of family 'Dialog' is returned.
         */
        private static Font createFontEx( String family, int style, int size, double dsize ) {
            for(;;) {
                Font font = createFont( family, style, size, dsize );

                if( Font.DIALOG.equals( family ) )
                    return font;

                // if the font family does not match any font on the system, "Dialog" family is returned
                if( !Font.DIALOG.equals( font.getFamily() ) ) {
                    // check for font problems
                    // - font height much larger than expected (e.g. font Inter; Oracle Java 8)
                    // - character width is zero (e.g. font Cantarell; Fedora; Oracle Java 8)
                    FontMetrics fm = StyleContext.getDefaultStyleContext().getFontMetrics( font );
                    if( fm.getHeight() > size * 2 || fm.stringWidth( "a" ) == 0 )
                        return createFont( Font.DIALOG, style, size, dsize );

                    return font;
                }

                // find last word in family
                int index = family.lastIndexOf( ' ' );
                if( index < 0 )
                    return createFont( Font.DIALOG, style, size, dsize );

                // check whether last work contains some font weight (e.g. Ultra-Bold or Heavy)
                String lastWord = family.substring( index + 1 ).toLowerCase(Locale.ENGLISH);
                if( lastWord.contains( "bold" ) || lastWord.contains( "heavy" ) || lastWord.contains( "black" ) )
                    style |= Font.BOLD;

                // remove last word from family and try again
                family = family.substring( 0, index );
            }
        }

        private static Font createFont( String family, int style, int size, double dsize ) {
            Font font = UiScale._createCompositeFont( family, style, size );

            // set font size in floating points
            font = font.deriveFont( style, (float) dsize );

            return font;
        }

        private static double getGnomeFontScale() {
            // do not scale font here if JRE scales
            if( UiScale._isSystemScaling() )
                return 96. / 72.;

            // see class com.sun.java.swing.plaf.gtk.PangoFonts background information

            Object value = Toolkit.getDefaultToolkit().getDesktopProperty( "gnome.Xft/DPI" );
            if( value instanceof Integer ) {
                int dpi = (Integer) value / 1024;
                if( dpi == -1 )
                    dpi = 96;
                if( dpi < 50 )
                    dpi = 50;
                return dpi / 72.0;
            } else {
                return GraphicsEnvironment.getLocalGraphicsEnvironment()
                                          .getDefaultScreenDevice()
                                          .getDefaultConfiguration()
                                          .getNormalizingTransform()
                                          .getScaleY();
            }
        }

        /**
         * map GTK/fontconfig names to equivalent JDK logical font name
         */
        private static @Nullable String mapFcName( String name ) {
            switch( name ) {
                case "sans":		return "sansserif";
                case "sans-serif":	return "sansserif";
                case "serif":		return "serif";
                case "monospace":	return "monospaced";
            }
            return null;
        }

        /**
         * Gets the default font for KDE from KDE configuration files.
         * <p>
         * The Swing fonts are not updated when the user changes system font size
         * (System Settings > Fonts > Force Font DPI). A application restart is necessary.
         * This is the same behavior as in native KDE applications.
         * <p>
         * The "display scale factor" (kdeglobals: [KScreen] > ScaleFactor) is not used
         * KDE also does not use it to calculate font size. Only forceFontDPI is used by KDE.
         * If user changes "display scale factor" (System Settings > Display and Monitors >
         * Displays > Scale Display), the forceFontDPI is also changed to reflect the scale factor.
         */
        private static Font getKDEFont() {
            List<String> kdeglobals = readConfig( "kdeglobals" );
            List<String> kcmfonts   = readConfig( "kcmfonts" );

            String generalFont  = getConfigEntry( kdeglobals, "General", "font" );
            String forceFontDPI = getConfigEntry( kcmfonts, "General", "forceFontDPI" );

            String family = "sansserif";
            int style = Font.PLAIN;
            int size = 10;

            if ( generalFont != null ) {
                List<String> strs = StringUtils.split( generalFont, ',' );
                try {
                    family = strs.get( 0 );
                    size = Integer.parseInt( strs.get( 1 ) );
                    if( "75".equals( strs.get( 4 ) ) )
                        style |= Font.BOLD;
                    if( "1".equals( strs.get( 5 ) ) )
                        style |= Font.ITALIC;
                } catch ( RuntimeException ex ) {
                    log.error("Failed to parse KDE system font from String '"+generalFont+"'.", ex);
                }
            }

            // font dpi
            int dpi = 96;
            if( forceFontDPI != null && !UiScale._isSystemScaling() ) {
                try {
                    dpi = Integer.parseInt( forceFontDPI );
                    if( dpi <= 0 )
                        dpi = 96;
                    if( dpi < 50 )
                        dpi = 50;
                } catch( NumberFormatException ex ) {
                    log.error("Failed to parse DPI scale from font size String '"+forceFontDPI+"'", ex);
                }
            }

            // scale font size
            double fontScale = dpi / 72.0;
            double dsize = size * fontScale;
            size = (int) (dsize + 0.5);
            if( size < 1 )
                size = 1;

            return createFont( family, style, size, dsize );
        }

        private static List<String> readConfig( String filename ) {
            File userHome = new File( System.getProperty( "user.home" ) );

            // search for config file
            String[] configDirs = {
                    ".config", // KDE 5
                    ".kde4/share/config", // KDE 4
                    ".kde/share/config"// KDE 3
            };
            File file = null;
            for( String configDir : configDirs ) {
                file = new File( userHome, configDir + "/" + filename );
                if( file.isFile() )
                    break;
            }
            if( file == null || !file.isFile() )
                return Collections.emptyList();

            // read config file
            ArrayList<String> lines = new ArrayList<>( 200 );
            try( BufferedReader reader = Files.newBufferedReader(file.toPath(), UTF_8) ) {
                String line;
                while( (line = reader.readLine()) != null )
                    lines.add( line );
            } catch( IOException ex ) {
                log.error("Failed to read KDE font config files for determining DPI scale factor.", ex);
            }
            return Collections.unmodifiableList(lines);
        }

        private static @Nullable String getConfigEntry( List<String> config, String group, String key ) {
            int groupLength = group.length();
            int keyLength = key.length();
            boolean inGroup = false;
            for( String line : config ) {
                if( !inGroup ) {
                    if( line.length() >= groupLength + 2 &&
                            line.charAt( 0 ) == '[' &&
                            line.charAt( groupLength + 1 ) == ']' &&
                            line.indexOf( group ) == 1 )
                    {
                        inGroup = true;
                    }
                } else {
                    if( line.startsWith( "[" ) )
                        return null;

                    if( line.length() >= keyLength + 2 &&
                            line.charAt( keyLength ) == '=' &&
                            line.startsWith( key ) )
                    {
                        return line.substring( keyLength + 1 );
                    }
                }
            }
            return null;
        }

    }

    private static class LazyRef<T> {
        private final Supplier<T> supplier;
        private volatile @Nullable T value;

        LazyRef( Supplier<T> supplier ) {
            this.supplier = Objects.requireNonNull( supplier );
        }

        T get() {
            @Nullable T value = this.value;
            if( value == null ) {
                synchronized( this ) {
                    value = this.value;
                    if ( value == null )
                        this.value = value = supplier.get();
                }
            }
            return value;
        }
    }
}
