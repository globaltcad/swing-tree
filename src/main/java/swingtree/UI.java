package swingtree;

import com.alexandriasoftware.swing.JSplitButton;
import net.miginfocom.swing.MigLayout;
import sprouts.Event;
import sprouts.*;
import swingtree.animation.Animate;
import swingtree.animation.LifeTime;
import swingtree.api.Buildable;
import swingtree.api.MenuBuilder;
import swingtree.api.SwingBuilder;
import swingtree.api.model.BasicTableModel;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;
import swingtree.layout.CompAttr;
import swingtree.layout.LayoutAttr;
import swingtree.style.StyleSheet;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  This class is a static API for exposing swing tree builder types for wrapping
 *  and assembling various {@link JComponent} types to form a UI tree.
 *  Instances of these builder type expose an API based on chained methods
 *  designed around functional interfaces to enable building UI tree structures for Swing
 *  in an HTML-like nested fashion while also keeping a high degree of control and transparency
 *  by peeking into the underlying swing components or registering user actions through lambdas.
 *  Swing tree works especially well alongside {@link MigLayout}s,
 *  which is why this general purpose {@link LayoutManager} is integrated into this library.
 *  Simply pass {@link String} constraints to the {@link UIForAnySwing#withLayout(String, String)}
 *  and any given {@link UIForAnySwing#add(String, UIForAnySwing[])} method
 *  or variant of, to make use of mig layouts.
 * 	<p>
 * 	<b>Please take a look at the <a href="https://globaltcad.github.io/swing-tree/">living swing-tree documentation</a>
 * 	where you can browse a large collection of examples demonstrating how to use the API of this class.</b>
 */
public final class UI
{
    private static final ThreadLocal<Settings> _SETTINGS = new ThreadLocal<>();

    static Settings SETTINGS() {
        Settings settings = _SETTINGS.get();
        if ( settings == null ) {
            settings = new Settings();
            _SETTINGS.set(settings);
        }
        return settings;
    }

    /**
     *  Sets a {@link StyleSheet} which will be applied to all SwingTree UIs defined in the subsequent lambda scope.
     *  This method allows to switch between different style sheets.
     *  <p>
     * 	You can switch to a style sheet like so: <br>
     * 	<pre>{@code
     * 	use(new MyCustomStyeSheet(), ()->
     *      UI.panel("fill")
     *      .add( "shrink", UI.label( "Username:" ) )
     *      .add( "grow, pushx", UI.textField("User1234..42") )
     *      .add( label( "Password:" ) )
     *      .add( "grow, pushx", UI.passwordField("child-birthday") )
     *      .add( "span",
     *          UI.button("Login!").onClick( it -> {...} )
     *      )
     *  );
     *  }</pre>
     *
     * @return the result of the given scope, usually a {@link JComponent} or SwingTree UI.
     */
    public static <T> T use( StyleSheet styleSheet, Supplier<T> scope ) {
        if ( !UI.thisIsUIThread() )
            try {
                return runAndGet(()-> use(styleSheet, scope));
            } catch (InvocationTargetException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        Settings settings = SETTINGS();
        StyleSheet oldStyleSheet = settings.getStyleSheet().orElse(null);
        settings.setStyleSheet(styleSheet);
        try {
            T result = scope.get();
            if ( result instanceof JComponent )
                ComponentExtension.from((JComponent) result).establishStyle();
            if ( result instanceof UIForAnySwing )
                ComponentExtension.from(((UIForAnySwing<?,?>) result).getComponent()).establishStyle();

            return result;
        } finally {
            settings.setStyleSheet(oldStyleSheet);
        }
    }

    /**
     *  Sets the {@link EventProcessor} to be used for all subsequent UI building operations.
     *  This method allows to switch between different event processing strategies.
     *  In particular, the {@link DecoupledEventProcessor} is recommended to be used for
     *  proper decoupling of the UI thread from the application logic.
     *  <p>
     * 	You can switch to the decoupled event processor like so: <br>
     * 	<pre>{@code
     * 	use(EventProcessor.DECOUPLED, ()->
     *      UI.panel("fill")
     *      .add( "shrink", UI.label( "Username:" ) )
     *      .add( "grow, pushx", UI.textField("User1234..42") )
     *      .add( label( "Password:" ) )
     *      .add( "grow, pushx", UI.passwordField("child-birthday") )
     *      .add( "span",
     *          UI.button("Login!").onClick( it -> {...} )
     *      )
     *  );
     *  }</pre>
     *
     * @param processor The event processor to be used for all subsequent UI building operations
     * @param scope The scope of the event processor to be used for all subsequent UI building operations.
     *              The value returned by the given scope is returned by this method.
     * @return The value returned by the given scope.
     * @param <T> The type of the value returned by the given scope.
     */
    public static <T> T use( EventProcessor processor, Supplier<T> scope )
    {
        if ( !UI.thisIsUIThread() )
            try {
                return runAndGet(()-> use(processor, scope));
            } catch (InvocationTargetException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        Settings settings = SETTINGS();
        EventProcessor oldProcessor = settings.getEventProcessor();
        settings.setEventProcessor(processor);
        try {
            return scope.get();
        } finally {
            settings.setEventProcessor(oldProcessor);
        }
    }

    /**
     *  A fully blocking call to the decoupled thread event processor
     *  causing this thread to join its event queue
     *  so that it can continuously process events produced by the UI.
     *  <p>
     *  This method wither be called by the main thread of the application
     *  after the UI has been built and shown to the user, or alternatively
     *  a new thread dedicated to processing events. (things like button clicks, etc.)
     *  @throws IllegalStateException If this method is called from the UI thread.
     */
    public static void joinDecoupledEventProcessor() {
        if ( thisIsUIThread() )
            throw new IllegalStateException("This method must not be called from the UI thread.");
        DecoupledEventProcessor.INSTANCE().join();
    }

    /**
     *  A fully blocking call to the decoupled thread event processor
     *  causing this thread to join its event queue
     *  so that it can continuously process events produced by the UI.
     *  <p>
     *  This method should be called by the main thread of the application
     *  after the UI has been built and shown to the user, or alternatively
     *  a new thread dedicated to processing events. (things like button clicks, etc.)
     *  <p>
     *  This method will block until an exception is thrown by the event processor.
     *  This is useful for debugging purposes.
     *  @throws InterruptedException If the thread is interrupted while waiting for the event processor to join.
     */
    public static void joinDecoupledEventProcessorUntilException() throws InterruptedException {
        DecoupledEventProcessor.INSTANCE().joinUntilException();
    }

    /**
     *  A fully blocking call to the decoupled thread event processor
     *  causing this thread to join its event queue
     *  so that it can process the given number of events produced by the UI.
     *  <p>
     *  This method should be called by the main thread of the application
     *  after the UI has been built and shown to the user, or alternatively
     *  a new thread dedicated to processing events. (things like button clicks, etc.)
     *  <p>
     *  This method will block until the given number of events have been processed.
     *  @param numberOfEvents The number of events to wait for.
     */
    public static void joinDecoupledEventProcessorFor(long numberOfEvents) {
        DecoupledEventProcessor.INSTANCE().joinFor(numberOfEvents);
    }

    /**
     *  A temporarily blocking call to the decoupled thread event processor
     *  causing this thread to join its event queue
     *  so that it can process the given number of events produced by the UI.
     *  <p>
     *  This method should be called by the main thread of the application
     *  after the UI has been built and shown to the user, or alternatively
     *  a new thread dedicated to processing events. (things like button clicks, etc.)
     *  <p>
     *  This method will block until the given number of events have been processed
     *  or an exception is thrown by the event processor.
     *  @param numberOfEvents The number of events to wait for.
     *  @throws InterruptedException If the thread is interrupted while waiting for the event processor to join.
     */
    public static void joinDecoupledEventProcessorUntilExceptionFor(long numberOfEvents) throws InterruptedException {
        DecoupledEventProcessor.INSTANCE().joinUntilExceptionFor(numberOfEvents);
    }

    /**
     *  A temporarily blocking call to the decoupled thread event processor
     *  causing this thread to join its event queue
     *  so that it can continuously process events produced by the UI
     *  until all events have been processed or an exception is thrown by the event processor.
     *  <p>
     *  This method should be called by the main thread of the application
     *  after the UI has been built and shown to the user, or alternatively
     *  a new thread dedicated to processing events. (things like button clicks, etc.)
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public static void joinDecoupledEventProcessorUntilDoneOrException() throws InterruptedException {
        DecoupledEventProcessor.INSTANCE().joinUntilDoneOrException();
    }

    // Common Mig layout constants:
    public static LayoutAttr FILL     = LayoutAttr.of("fill");
    public static LayoutAttr FILL_X     = LayoutAttr.of("fillx");
    public static LayoutAttr FILL_Y     = LayoutAttr.of("filly");
    public static LayoutAttr INS(int insets) { return LayoutAttr.of("ins " + insets); }
    public static LayoutAttr INSETS(int insets) { return LayoutAttr.of("insets " + insets); }
    public static LayoutAttr INS(int top, int left, int bottom, int right) { return LayoutAttr.of("insets " + top + " " + left + " " + bottom + " " + right); }
    public static LayoutAttr INSETS(int top, int left, int bottom, int right) { return LayoutAttr.of("insets " + top + " " + left + " " + bottom + " " + right); }
    public static LayoutAttr WRAP(int times) { return LayoutAttr.of( "wrap " + times ); }
    public static LayoutAttr GAP_REL(int size) { return LayoutAttr.of( "gap rel " + size ); }
    public static LayoutAttr FLOW_X   = LayoutAttr.of("flowx");
    public static LayoutAttr FLOW_Y   = LayoutAttr.of("flowy");
    public static LayoutAttr NO_GRID  = LayoutAttr.of("nogrid");
    public static LayoutAttr NO_CACHE = LayoutAttr.of("nocache");
    public static LayoutAttr DEBUG    = LayoutAttr.of("debug");

    public static CompAttr WRAP     = CompAttr.of("wrap");
    public static CompAttr SPAN     = CompAttr.of("SPAN");
    public static CompAttr SPAN( int times ) { return CompAttr.of( "span " + times ); }
    public static CompAttr SPAN( int xTimes, int yTimes ) { return CompAttr.of( "span " + xTimes + " " + yTimes ); }
    public static CompAttr SPAN_X( int times ) { return CompAttr.of( "spanx " + times ); }
    public static CompAttr SPAN_Y( int times ) { return CompAttr.of( "spany " + times ); }
    public static CompAttr GROW     = CompAttr.of("grow");
    public static CompAttr GROW_X   = CompAttr.of("growx");
    public static CompAttr GROW_Y   = CompAttr.of("growy");
    public static CompAttr GROW( int weight ) { return CompAttr.of( "grow " + weight ); }
    public static CompAttr GROW_X( int weight ) { return CompAttr.of( "growx " + weight ); }
    public static CompAttr GROW_Y( int weight ) { return CompAttr.of( "growy " + weight ); }
    public static CompAttr SHRINK   = CompAttr.of("shrink");
    public static CompAttr SHRINK_X = CompAttr.of("shrinkx");
    public static CompAttr SHRINK_Y = CompAttr.of("shrinky");
    public static CompAttr SHRINK( int weight )  { return CompAttr.of("shrink "+weight); }
    public static CompAttr SHRINK_X( int weight )  { return CompAttr.of("shrinkx "+weight); }
    public static CompAttr SHRINK_Y( int weight )  { return CompAttr.of("shrinky "+weight); }
    public static CompAttr SHRINK_PRIO( int priority )  { return CompAttr.of("shrinkprio "+priority); }
    public static CompAttr PUSH     = CompAttr.of("push");
    public static CompAttr PUSH_X   = CompAttr.of("pushx");
    public static CompAttr PUSH_Y   = CompAttr.of("pushy");
    public static CompAttr PUSH( int weight )  { return CompAttr.of("push "+weight); }
    public static CompAttr PUSH_X( int weight ) { return CompAttr.of("pushx "+weight); }
    public static CompAttr PUSH_Y( int weight ) { return CompAttr.of("pushy "+weight); }
    public static CompAttr SKIP( int cells ) { return CompAttr.of("skip "+cells); }
    public static CompAttr SPLIT( int cells ) { return CompAttr.of("split "+cells); }
    public static CompAttr WIDTH( int min, int pref, int max ) { return CompAttr.of("width "+min+":"+pref+":"+max); }
    public static CompAttr HEIGHT( int min, int pref, int max ) { return CompAttr.of("height "+min+":"+pref+":"+max); }
    public static CompAttr PAD( int size ) { return PAD(size, size, size, size); }
    public static CompAttr PAD( int top, int left, int bottom, int right ) { return CompAttr.of("pad "+top+" "+left+" "+bottom+" "+right); }
    public static CompAttr ALIGN_CENTER = CompAttr.of("align center");
    public static CompAttr ALIGN_LEFT = CompAttr.of("align left");
    public static CompAttr ALIGN_RIGHT = CompAttr.of("align right");
    public static CompAttr ALIGN_X_CENTER = CompAttr.of("alignx center");
    public static CompAttr ALIGN_X_LEFT = CompAttr.of("alignx left");
    public static CompAttr ALIGN_X_RIGHT = CompAttr.of("alignx right");
    public static CompAttr ALIGN_Y_CENTER = CompAttr.of("aligny center");
    public static CompAttr ALIGN_Y_BOTTOM = CompAttr.of("aligny bottom");
    public static CompAttr ALIGN_Y_TOP = CompAttr.of("aligny top");
    public static CompAttr ALIGN( Position pos ) { return CompAttr.of(pos.toMigAlign()); }
    public static CompAttr TOP = CompAttr.of("top");
    public static CompAttr BOTTOM = CompAttr.of("bottom");
    public static CompAttr LEFT = CompAttr.of("left");
    public static CompAttr RIGHT = CompAttr.of("right");
    public static CompAttr GAP_LEFT_PUSH = CompAttr.of("gapleft push");
    public static CompAttr GAP_RIGHT_PUSH = CompAttr.of("gapright push");
    public static CompAttr GAP_TOP_PUSH = CompAttr.of("gaptop push");
    public static CompAttr GAP_BOTTOM_PUSH = CompAttr.of("gapbottom push");
    public static CompAttr DOCK_NORTH = CompAttr.of("dock north");
    public static CompAttr DOCK_SOUTH = CompAttr.of("dock south");
    public static CompAttr DOCK_EAST  = CompAttr.of("dock east");
    public static CompAttr DOCK_WEST  = CompAttr.of("dock west");
    public static CompAttr DOCK( Position pos ) { return CompAttr.of("dock " + pos.toDirectionString()); }

    /**
     * Loads an icon from the classpath or from a file.
     * @param path The path to the icon. It can be a classpath resource or a file path.
     * @return The icon.
     */
    public static ImageIcon icon( String path ) {
        // First we make the path platform independent:
        path = path.replace('\\', '/');
        // Then we try to load the icon url from the classpath:
        URL url = UI.class.getResource(path);
        // We check if the url is null:
        if ( url == null ) {
            // It is, let's do some troubleshooting:
            if ( !path.startsWith("/") )
                url = UI.class.getResource("/" + path);

            if ( url == null ) // Still null? Let's try to load it as a file:
                try {
                    url = new File(path).toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
        }
        return new ImageIcon(url);
    }

    private UI(){} // This is a static API

    /**
     *  An enum set of all the available swing cursors which
     *  map to the cursor type id.
     *  This exists simply because swing was created before enums were added to Java.
     */
    public enum Cursor
    {
        HAND(java.awt.Cursor.HAND_CURSOR),
        MOVE(java.awt.Cursor.MOVE_CURSOR),
        CROSS(java.awt.Cursor.CROSSHAIR_CURSOR),
        DEFAULT(java.awt.Cursor.DEFAULT_CURSOR),
        WAIT(java.awt.Cursor.WAIT_CURSOR),
        TEXT(java.awt.Cursor.TEXT_CURSOR),
        RESIZE_EAST(java.awt.Cursor.E_RESIZE_CURSOR),
        RESIZE_WEST(java.awt.Cursor.W_RESIZE_CURSOR),
        RESIZE_SOUTH(java.awt.Cursor.S_RESIZE_CURSOR),
        RESIZE_NORTH(java.awt.Cursor.N_RESIZE_CURSOR),
        RESIZE_NORTH_WEST(java.awt.Cursor.NW_RESIZE_CURSOR),
        RESIZE_NORTH_EAST(java.awt.Cursor.NE_RESIZE_CURSOR),
        RESIZE_SOUTH_WEST(java.awt.Cursor.SE_RESIZE_CURSOR),
        RESIZE_SOUTH_EAST(java.awt.Cursor.SE_RESIZE_CURSOR);

        final int type;

        Cursor( int type ) { this.type = type; }
    }

    /**
     *  The scroll policy for UI components with scroll behaviour.
     */
    public enum ScrollBarPolicy { NEVER, AS_NEEDED, ALWAYS }

    /**
     *  The position of a UI component in terms of directions.
     */
    public enum Position {
        TOP, LEFT, BOTTOM, RIGHT;
        int forTabbedPane() {
            switch ( this ) {
                case TOP   : return JTabbedPane.TOP;
                case LEFT  : return JTabbedPane.LEFT;
                case BOTTOM: return JTabbedPane.BOTTOM;
                case RIGHT : return JTabbedPane.RIGHT;
            }
            throw new RuntimeException();
        }

        String toDirectionString() {
            switch ( this ) {
                case TOP   : return "north";
                case LEFT  : return "west";
                case BOTTOM: return "south";
                case RIGHT : return "east";
            }
            throw new RuntimeException();
        }

        String toMigAlign() {
            switch ( this ) {
                case TOP   : return "top";
                case LEFT  : return "left";
                case BOTTOM: return "bottom";
                case RIGHT : return "right";
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Overflow policy of UI components.
     */
    public enum OverflowPolicy {
        WRAP, SCROLL;

        int forTabbedPane() {
            switch ( this ) {
                case WRAP  : return JTabbedPane.WRAP_TAB_LAYOUT;
                case SCROLL: return JTabbedPane.SCROLL_TAB_LAYOUT;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Vertical or horizontal alignment.
     */
    public enum Align {
        HORIZONTAL, VERTICAL;

        int forSlider() {
            switch ( this ) {
                case HORIZONTAL: return JSlider.HORIZONTAL;
                case VERTICAL  : return JSlider.VERTICAL;
            }
            throw new RuntimeException();
        }
        int forProgressBar() {
            switch ( this ) {
                case HORIZONTAL: return JProgressBar.HORIZONTAL;
                case VERTICAL  : return JProgressBar.VERTICAL;
            }
            throw new RuntimeException();
        }
        int forSeparator() {
            switch ( this ) {
                case HORIZONTAL: return JSeparator.HORIZONTAL;
                case VERTICAL  : return JSeparator.VERTICAL;
            }
            throw new RuntimeException();
        }
        int forSplitPane() {
            switch ( this ) {
                case HORIZONTAL: return JSplitPane.VERTICAL_SPLIT;
                case VERTICAL:   return JSplitPane.HORIZONTAL_SPLIT;
            }
            throw new RuntimeException();
        }
        int forToolBar() {
            switch ( this ) {
                case HORIZONTAL: return JToolBar.HORIZONTAL;
                case VERTICAL  : return JToolBar.VERTICAL;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Different positions along a vertically aligned UI component.
     */
    public enum VerticalAlignment {
        TOP, CENTER, BOTTOM;

        int forSwing() {
            switch ( this ) {
                case TOP:    return SwingConstants.TOP;
                case CENTER: return SwingConstants.CENTER;
                case BOTTOM: return SwingConstants.BOTTOM;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Different positions along a horizontally aligned UI component.
     */
    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT;

        public final int forSwing() {
            switch ( this ) {
                case LEFT:   return SwingConstants.LEFT;
                case CENTER: return SwingConstants.CENTER;
                case RIGHT:  return SwingConstants.RIGHT;
            }
            throw new RuntimeException();
        }
    }

    public enum HorizontalDirection {
        LEFT_TO_RIGHT, RIGHT_TO_LEFT;

        public final ComponentOrientation forTextOrientation() {
            switch ( this ) {
                case LEFT_TO_RIGHT: return ComponentOrientation.LEFT_TO_RIGHT;
                case RIGHT_TO_LEFT: return ComponentOrientation.RIGHT_TO_LEFT;
            }
            throw new RuntimeException();
        }
    }

    public enum ListData {
        COLUMN_MAJOR,
        ROW_MAJOR,
        COLUMN_MAJOR_EDITABLE,
        ROW_MAJOR_EDITABLE;

        final boolean isEditable() {
            switch ( this ) {
                case COLUMN_MAJOR:
                case ROW_MAJOR:
                    return false;
                case COLUMN_MAJOR_EDITABLE:
                case ROW_MAJOR_EDITABLE:
                    return true;
            }
            throw new RuntimeException();
        }

        final boolean isRowMajor() {
            switch ( this ) {
                case COLUMN_MAJOR:
                case COLUMN_MAJOR_EDITABLE:
                    return false;
                case ROW_MAJOR:
                case ROW_MAJOR_EDITABLE:
                    return true;
            }
            throw new RuntimeException();
        }
    }

    public enum MapData {
        EDITABLE, READ_ONLY;

        final boolean isEditable() {
            switch ( this ) {
                case EDITABLE: return true;
                case READ_ONLY: return false;
            }
            throw new RuntimeException();
        }
    }


    /**
     *  This returns an instance of a generic swing tree builder
     *  for anything extending the {@link JComponent} class.
     *  <br><br>
     *
     * @param component The new component instance which ought to be part of the Swing UI.
     * @param <T> The concrete type of this new component.
     * @return A basic UI builder instance wrapping any {@link JComponent}.
     */
    public static <T extends JComponent> UIForSwing<T> of( T component )
    {
        NullUtil.nullArgCheck(component, "component", JComponent.class);
        return new UIForSwing<>(component);
    }

    /**
     *  This returns an instance of a Swing-Tree builder for a {@link JFrame} type.
     * @param frame The new frame instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     * @param <F> The concrete type of this new frame.
     */
    public static <F extends JFrame> UIForJFrame<F> of( F frame ) {
        return new UIForJFrame<>(frame);
    }

    /**
     *  Use this to create a builder for the supplied {@link JFrame}. <br>
     *  This is in essence a convenience method for {@code UI.of(new JFrame()) )}.
     *
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     */
    public static UIForJFrame<JFrame> frame() {
        return new UIForJFrame<>(new JFrame());
    }

    /**
     *  Use this to create a builder for the supplied {@link JFrame} with the supplied title. <br>
     * @param title The title for the new frame.
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     */
    public static UIForJFrame<JFrame> frame( String title ) {
        return new UIForJFrame<>(new JFrame()).withTitle(title);
    }

    /**
     *  This returns an instance of a Swing-Tree builder for a {@link JDialog} type.
     * @param dialog The new dialog instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     * @param <D> The concrete type of this new dialog.
     */
    public static <D extends JDialog> UIForJDialog<D> of( D dialog ) {
        return new UIForJDialog<>(dialog);
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog}. <br>
     *  This is in essence a convenience method for {@code UI.of(new JDialog()) )}.
     *
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog() {
        return new UIForJDialog<>(new JDialog());
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied owner. <br>
     * @param owner The owner for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( Window owner ) {
        return new UIForJDialog<>(new JDialog(owner));
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied title. <br>
     * @param title The title for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( String title ) {
        return new UIForJDialog<>(new JDialog()).withTitle(title);
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied owner and title. <br>
     * @param owner The owner for the new dialog.
     * @param title The title for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( Window owner, String title ) {
        return new UIForJDialog<>(new JDialog(owner)).withTitle(title);
    }

    /**
     *  If you are using builders for your custom {@link JComponent},
     *  implement this to allow the {@link UI} API to call the {@link SwingBuilder#build()}
     *  method for you.
     *
     * @param builder A builder for custom {@link JComponent} types.
     * @param <T> The UI component type built by implementations of the provided builder.
     * @return A basic UI builder instance wrapping any {@link JComponent}.
     */
    public static <T extends JComponent> UIForSwing<T> of( SwingBuilder<T> builder )
    {
        NullUtil.nullArgCheck(builder, "builder", SwingBuilder.class);
        return of(builder.build());
    }

    /**
     *  If you are using builders for custom {@link JMenuItem} components,
     *  implement this to allow the {@link UI} API to call the {@link SwingBuilder#build()}
     *  method for you.
     *
     * @param builder A builder for custom {@link JMenuItem} types.
     * @param <M> The {@link JMenuItem} type built by implementations of the provided builder.
     * @return A builder instance for a {@link JMenuItem}, which enables fluent method chaining.
     */
    public static <M extends JMenuItem> UIForMenuItem<M> of( MenuBuilder<M> builder )
    {
        NullUtil.nullArgCheck(builder, "builder", MenuBuilder.class);
        return new UIForMenuItem<>(builder.build());
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JPopupMenu} UI component.
     *
     * @param popup The new {@link JPopupMenu} instance which ought to be part of the Swing UI.
     * @param <P> The concrete type of this new component.
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static <P extends JPopupMenu> UIForPopup<P> of( P popup )
    {
        NullUtil.nullArgCheck(popup, "popup", JPopupMenu.class);
        return new UIForPopup<>(popup);
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JPopupMenu} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPopupMenu())}.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static UIForPopup<JPopupMenu> popupMenu() { return of(new PopupMenu()); }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *
     * @param separator The new {@link JSeparator} instance which ought to be part of the Swing UI.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static <S extends JSeparator> UIForSeparator<S> of( S separator )
    {
        NullUtil.nullArgCheck(separator, "separator", JSeparator.class);
        return new UIForSeparator<>(separator);
    }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *  This is in essence a convenience method for {@code UI.of(new JSeparator())}.
     *
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator<JSeparator> separator() { return of(new JSeparator()); }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *  This is in essence a convenience method for {@code UI.of(new JSeparator(JSeparator.VERTICAL))}.
     *
     * @param align The alignment of the separator which may either be horizontal or vertical.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator<JSeparator> separator( Align align ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return separator().with(align);
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JSeparator} whose
     *  alignment is dynamically determined based on a provided property.
     *
     * @param align The alignment property of the separator which may either be horizontal or vertical.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator<JSeparator> separator( Val<Align> align ) {
        NullUtil.nullArgCheck(align, "align", Val.class);
        return separator().withAlignment(align);
    }

    /**
     *  This returns a {@link JButton} swing tree builder.
     *
     * @param component The button component which ought to be wrapped by the swing tree UI builder.
     * @return A basic UI {@link JButton} builder instance.
     */
    public static <T extends AbstractButton> UIForButton<T> of( T component )
    {
        NullUtil.nullArgCheck(component, "component", AbstractButton.class);
        return new UIForButton<>(component);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component without any text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton())}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button() { return of(new Button()); }

    /**
     *  Use this to create a builder for the {@link JButton} UI component with the provided text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton(String text))}.
     *
     * @param text The text to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( String text ) { return button().withText(text); }

    /**
     *  Create a builder for the {@link JButton} UI component where the text of the provided
     *  property is dynamically displayed on top.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Val<String> text ) {
        NullUtil.nullArgCheck( text, "text", Val.class );
        return button().withText(text);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).peek( it -> it.setIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().peek( it -> it.setIcon(icon) );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a dynamically displayed icon on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).withIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> buttonWithIcon( Val<Icon> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullPropertyCheck(icon, "icon");
        return button().withIcon(icon);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon, Icon onHover ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(onHover, "onHover", Icon.class);
        return button(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top
     *  which should both be scaled to the provided dimensions.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, ImageIcon icon, ImageIcon onHover ) {
        NullUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        NullUtil.nullArgCheck(onHover, "onHover", ImageIcon.class);
        onHover = new ImageIcon(onHover.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
        icon = new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
        return button(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default, an on-hover and an on-press icon displayed on top.
     *  This is in essence a convenience method for:
     *  <pre>{@code 
     *      UI.of(new JButton()).peek( it -> {
     *          it.setIcon(icon);
     *          it.setRolloverIcon(onHover);
     *          it.setPressedIcon(onPress);
     *      })
     *  }</pre>
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon, Icon onHover, Icon onPress ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(onHover, "onHover", Icon.class);
        NullUtil.nullArgCheck(onPress, "onPress", Icon.class);
        return button().peek(it -> it.setIcon(icon) )
                .peek(it -> it.setRolloverIcon(onHover) )
                .peek(it -> it.setPressedIcon(onPress) );
    }

    /**
     *  Use this to create a builder for the {@link JSplitButton} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSplitButton())}.
     *
     * @return A builder instance for a {@link JSplitButton}, which enables fluent method chaining.
     */
    public static <B extends JSplitButton> UIForSplitButton<B> of( B splitButton ) {
        NullUtil.nullArgCheck(splitButton, "splitButton", JSplitButton.class);
        return new UIForSplitButton<>(splitButton);
    }

    /**
     *  Use this to build {@link JSplitButton}s with custom text displayed ont top.
     *  The {@link JSplitButton} wrapped by the returned builder can be populated
     *  with {@link JMenuItem}s like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Displayed on button!")
     *      .add(UI.splitItem("first"))
     *      .add(UI.splitItem("second").onButtonClick( it -> ... ))
     *      .add(UI.splitItem("third"))
     *  }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JSplitButton}
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     */
    public static UIForSplitButton<JSplitButton> splitButton( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForSplitButton<>(new JSplitButton(text));
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type, and the click event is
     *  handles by an {@link Event} instance. <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *      private Event clickEvent = Event.of(()->{ ... }
     *
     *      public Var<Size> selection() { return selection; }
     *      public Event clickEvent() { return clickEvent; }
     *
     *      // In your view:
     *      UI.splitButton(vm.selection(), vm.clickEvent())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      For the text displayed on the split button, the selected enum state
     *      will be converted to strings based on the {@link Object#toString()}
     *      method. If you want to customize how they are displayed
     *      (So that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      simply override the {@link Object#toString()} method in your enum. </i>
     *
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @param clickEvent The {@link Event} which will be fired when the user clicks on the button.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     */
    public static <E extends Enum<E>> UIForSplitButton<JSplitButton> splitButton( Var<E> selection, Event clickEvent ) {
        return splitButton("").withSelection(selection, clickEvent);
    }

    /**
     *  Use this to build {@link JSplitButton}s where the selectable options
     *  are represented by an {@link Enum} type. <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.splitButton(vm.selection())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      The text displayed on the button is based on the {@link Object#toString()}
     *      method of the enum instances. If you want to customize how they are displayed
     *      (So that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      simply override the {@link Object#toString()} method in your enum. </i>
     *
     * @param selection The {@link Var} which holds the currently selected {@link Enum} value.
     *                  This will be updated when the user selects a new value.
     * @return A UI builder instance wrapping a {@link JSplitButton}.
     */
    public static <E extends Enum<E>> UIForSplitButton<JSplitButton> splitButton( Var<E> selection ) {
        return splitButton("").withSelection(selection);
    }

    /**
     *  Use this to add entries to the {@link JSplitButton} by
     *  passing {@link SplitItem} instances to {@link UIForSplitButton} builder like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Button")
     *      .add(UI.splitItem("first"))
     *      .add(UI.splitItem("second"))
     *      .add(UI.splitItem("third"))
     *  }</pre>
     *  You can also use the {@link SplitItem} wrapper class to wrap
     *  useful action lambdas for the split item.
     *
     * @param text The text displayed on the {@link JMenuItem} exposed by the {@link JSplitButton}s {@link JPopupMenu}.
     * @return A new {@link SplitItem} wrapping a simple {@link JMenuItem}.
     */
    public static SplitItem<JMenuItem> splitItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return SplitItem.of(text);
    }

    /**
     *  Use this to add property bound entries to the {@link JSplitButton} by
     *  passing {@link SplitItem} instances to {@link UIForSplitButton} builder like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Button")
     *      .add(UI.splitItem(viewModel.getFirstButtonName()))
     *      .add(UI.splitItem(viewModel.getSecondButtonName()))
     *      .add(UI.splitItem(viewModel.getThirdButtonName()))
     *  }</pre>
     *  You can also use the {@link SplitItem} wrapper class to wrap
     *  useful action lambdas for the split item.
     *
     * @param text The text property to dynamically display text on the {@link JMenuItem} exposed by the {@link JSplitButton}s {@link JPopupMenu}.
     * @return A new {@link SplitItem} wrapping a simple {@link JMenuItem}.
     */
    public static SplitItem<JMenuItem> splitItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return SplitItem.of(text);
    }

    /**
     *  Use this to add radio item entries to the {@link JSplitButton} by
     *  passing {@link SplitItem} instances to {@link UIForSplitButton} builder like so: <br>
     *  <pre>{@code
     *      UI.splitButton("Button")
     *      .add(UI.splitRadioItem("first"))
     *      .add(UI.splitRadioItem("second"))
     *      .add(UI.splitRadioItem("third"))
     *  }</pre>
     *  You can also use the {@link SplitItem} wrapper class to wrap
     *  useful action lambdas for the split item.
     *
     * @param text The text displayed on the {@link JRadioButtonMenuItem} exposed by the {@link JSplitButton}s {@link JPopupMenu}.
     * @return A new {@link SplitItem} wrapping a simple {@link JRadioButtonMenuItem}.
     */
    public static SplitItem<JRadioButtonMenuItem> splitRadioItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return SplitItem.of(new JRadioButtonMenuItem(text));
    }

    /**
     *  Creates a UI builder for a custom {@link JTabbedPane} type.
     *
     * @param pane The {@link JTabbedPane} type which should be used wrapped.
     * @return This instance, to allow for method chaining.
     * @param <P> The pane type parameter.
     */
    public static <P extends JTabbedPane> UIForTabbedPane<P> of( P pane ) {
        NullUtil.nullArgCheck(pane, "pane", JTabbedPane.class);
        return new UIForTabbedPane<>(pane);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTabbedPane())}.
     *  In order to add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab("one").add(UI.panel().add(..)))
     *      .add(UI.tab("two").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("three").with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     *
     * @return A builder instance for a new {@link JTabbedPane}, which enables fluent method chaining.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane() { return of(new TabbedPane()); }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link Position} applied to the tab buttons
     *  (see {@link JTabbedPane#setTabLayoutPolicy(int)}).
     *  In order to add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *
     *  <pre>{@code
     *      UI.tabbedPane(Position.RIGHT)
     *      .add(UI.tab("first").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param tabsPosition The position of the tab buttons which may be {@link Position#TOP}, {@link Position#RIGHT}, {@link Position#BOTTOM}, {@link Position#LEFT}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPosition} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( Position tabsPosition ) {
        NullUtil.nullArgCheck(tabsPosition, "tabsPosition", Position.class);
        return tabbedPane().with(tabsPosition);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link OverflowPolicy} and {@link Position} applied to the tab buttons 
     *  (see {@link JTabbedPane#setTabLayoutPolicy(int)} and {@link JTabbedPane#setTabPlacement(int)}).
     *  In order to add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(Position.LEFT, OverflowPolicy.WRAP)
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param tabsPosition The position of the tab buttons which may be {@link Position#TOP}, {@link Position#RIGHT}, {@link Position#BOTTOM}, {@link Position#LEFT}.
     * @param tabsPolicy The overflow policy of the tab buttons which can either be {@link OverflowPolicy#SCROLL} or {@link OverflowPolicy#WRAP}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPosition} or {@code tabsPolicy} are {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( Position tabsPosition, OverflowPolicy tabsPolicy ) {
        NullUtil.nullArgCheck(tabsPosition, "tabsPosition", Position.class);
        NullUtil.nullArgCheck(tabsPolicy, "tabsPolicy", OverflowPolicy.class);
        return tabbedPane().with(tabsPosition).with(tabsPolicy);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link OverflowPolicy} applied to the tab buttons (see {@link JTabbedPane#setTabLayoutPolicy(int)}).
     *  In order to add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(OverflowPolicy.SCROLL)
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *  
     * @param tabsPolicy The overflow policy of the tab button which can either be {@link OverflowPolicy#SCROLL} or {@link OverflowPolicy#WRAP}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPolicy} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( OverflowPolicy tabsPolicy ) {
        NullUtil.nullArgCheck(tabsPolicy, "tabsPolicy", OverflowPolicy.class);
        return tabbedPane().with(Position.TOP).with(tabsPolicy);
    }


    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@code selectionIndex} property which should be determine the
     *  tab selection of the {@link JTabbedPane} dynamically.
     *  To add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(vm.getSelectionIndex())
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *  Note that contrary to method {@link #tabbedPane(Var)}, this method receives a {@link Val}
     *  property which may not be changed by the GUI user. If you want to allow the user to change
     *  the selection index property state, use {@link #tabbedPane(Var)} instead.
     *
     * @param selectedIndex The index of the tab to select.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code selectedIndex} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( Val<Integer> selectedIndex ) {
        return tabbedPane().withSelectedIndex(selectedIndex);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@code selectionIndex} property which should be determine the
     *  tab selection of the {@link JTabbedPane} dynamically.
     *  To add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(vm.getSelectionIndex())
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param selectedIndex The index of the tab to select.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code selectedIndex} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( Var<Integer> selectedIndex ) {
        return tabbedPane().withSelectedIndex(selectedIndex);
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param title The text displayed on the tab button.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code title} is {@code null}.
     */
    public static Tab tab( String title ) {
        NullUtil.nullArgCheck(title, "title", String.class);
        return new Tab(null, null, Val.of(title), null, null, null, null, null, null);
    }

    /**
     *  A factory method producing a {@link Tab} instance with the provided {@code title} property
     *  which can dynamically change the title of the tab button.
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab(property1).add(UI.panel().add(..)))
     *      .add(UI.tab(property2).withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab(property3).with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param title The text property dynamically changing the title of the tab button when the property changes.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code title} is {@code null}.
     */
    public static Tab tab( Val<String> title ) {
        NullUtil.nullArgCheck(title, "title", Val.class);
        return new Tab(null, null, title, null, null, null, null, null, null);
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab(new JButton("X")).add(UI.panel().add(..)))
     *      .add(UI.tab(new JLabel("Hi!")).withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab(new JPanel()).add(UI.button("click me")))
     *  }</pre>
     *
     * @param component The component displayed on the tab button.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static Tab tab( JComponent component ) {
        NullUtil.nullArgCheck(component, "component", Component.class);
        return new Tab(null, component, null, null, null, null, null, null, null);
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab(UI.button("X")).add(UI.panel().add(..)))
     *      .add(UI.tab(UI.label("Hi!")).withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab(UI.of(...)).with(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param builder The builder wrapping the component displayed on the tab button.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     * @throws IllegalArgumentException if {@code builder} is {@code null}.
     */
    public static Tab tab( UIForAnySwing<?, ?> builder ) {
        NullUtil.nullArgCheck(builder, "builder", UIForAnySwing.class);
        return new Tab(null, builder.getComponent(), null, null, null, null, null, null, null);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenu} instance.
     *
     * @return A builder instance for the provided {@link JMenu}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenu> UIForMenu<M> of( M component ) {
        NullUtil.nullArgCheck(component, "component", JMenu.class);
        return new UIForMenu<>(component);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenuItem} instance.
     *
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenuItem> UIForMenuItem<M> of( M component ) {
        NullUtil.nullArgCheck(component, "component", JMenuItem.class);
        return new UIForMenuItem<>(component);
    }

    /**
     *  This factory method creates a {@link JMenu} with the provided text
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.menuItem("Delete").onClick( it -> {..} ))
     *    .add(UI.menuItem("Edit").onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return new UIForMenuItem<>((JMenuItem) new MenuItem()).withText(text);
    }

    /**
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return new UIForMenuItem<>((JMenuItem) new MenuItem()).withText(text);
    }

    /**
     *  Use this factory method to create a {@link JMenuItem} with the
     *  provided text and default icon. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.menuItem("Hello", UI.icon("path/to/icon.png"))
     *    .withTip("I give info!")
     *    .onClick( it -> {...} )
     *  }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return new UIForMenuItem<>((JMenuItem) new MenuItem())
                    .withText(text)
                    .withIcon(icon);
    }

    /**
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return new UIForMenuItem<>((JMenuItem) new MenuItem())
                    .withText(text)
                    .withIcon(icon);
    }

    /**
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text, Val<Icon> icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        return new UIForMenuItem<>((JMenuItem) new MenuItem()).withText(text).withIcon(icon);
    }

    /**
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text, Val<Icon> icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        return new UIForMenuItem<>((JMenuItem) new MenuItem()).withText(text).withIcon(icon);
    }

    /**
     *  A factory method to wrap the provided {@link JRadioButtonMenuItem} instance in a SwingTree UI builder.
     *
     * @param radioMenuItem The {@link JRadioButtonMenuItem} instance to be wrapped.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     * @param <M> The type of the {@link JRadioButtonMenuItem} instance to be wrapped.
     */
    public static <M extends JRadioButtonMenuItem> UIForRadioButtonMenuItem<M> of( M radioMenuItem ) {
        NullUtil.nullArgCheck(radioMenuItem, "component", JRadioButtonMenuItem.class);
        return new UIForRadioButtonMenuItem<>(radioMenuItem);
    }

    /**
     *  A factory method to create a plain {@link JRadioButtonMenuItem} instance. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem().onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem().onClick( it -> {..} ))
     *  }</pre>
     *
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem() {
        return new UIForRadioButtonMenuItem<>((JRadioButtonMenuItem) new RadioButtonMenuItem());
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with the provided text
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem("Delete").onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem("Edit").onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return radioButtonMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Val.of("Delete")).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem(Val.of("Edit")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JRadioButtonMenuItem} dynamically.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return radioButtonMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with the provided text
     *  displayed on the menu button and the provided icon displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem("Delete", UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem("Edit", UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( String text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return radioButtonMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically and the provided icon
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Val.of("Delete"), UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem(Val.of("Edit"), UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JRadioButtonMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */

    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( Val<String> text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return radioButtonMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} bound to a fixed enum value
     *  and a variable enum property which will dynamically select the menu item based on the
     *  equality of the fixed enum value and the variable enum property value. <br>
     *  Consider the following example code:
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Unit.SECONDS, myViewModel.unitProperty()))
     *    .add(UI.radioButtonMenuItem(Unit.MINUTES, myViewModel.unitProperty()))
     *    .add(UI.radioButtonMenuItem(Unit.HOURS,   myViewModel.unitProperty()))
     *  }</pre>
     *  In this example the {@code myViewModel.unitProperty()} is a {@link Var} property of
     *  example type {@code Unit}.
     *  A given menu item will be selected if the value of the {@code myViewModel.unitProperty()}
     *  is equal to the first enum value passed to the factory method.
     *  This first enum will also be used as the text of the menu item through the {@code toString()}.
     *
     * @param state The fixed enum value which will be used as the text of the menu item and
     * @param property The variable enum property which will be used to select the menu item.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static <E extends Enum<E>> UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( E state, Var<E> property ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(property, "property", Var.class);
        return radioButtonMenuItem(state.toString()).isSelectedIf(state, property);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with some custom text and a boolean property,
     *  dynamically determining whether the radio button based menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected1 = Var.of(false);
     *    Var<Boolean> isSelected2 = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem("Make Coffee", isSelected1).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem("Make Tea", isSelected2).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the boolean property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getIsSelectedProperty()}).
     *
     * @param text The text which should be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determines whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( String text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return radioButtonMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  A factory method to create a {@link JRadioButtonMenuItem} with some custom text and a boolean property,
     *  dynamically determining whether the radio button based menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected1 = Var.of(false);
     *    Var<Boolean> isSelected2 = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.radioButtonMenuItem(Val.of("Make Coffee"), isSelected1).onClick( it -> {..} ))
     *    .add(UI.radioButtonMenuItem(Val.of("Make Tea"), isSelected2).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the {@code String} and {@code boolean}
     * properties from a view model object through
     * plain old getter methods
     * (e.g. {@code myViewModel.getTextProperty()} and {@code myViewModel.getIsSelectedProperty()}).
     *
     * @param text The text property whose text should dynamically be displayed on the wrapped {@link JRadioButtonMenuItem}.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determines whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JRadioButtonMenuItem}, which enables fluent method chaining.
     */
    public static UIForRadioButtonMenuItem<JRadioButtonMenuItem> radioButtonMenuItem( Val<String> text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return radioButtonMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  A factory method to wrap the provided {@link JCheckBoxMenuItem} instance in a SwingTree UI builder.
     *
     * @param checkBoxMenuItem The {@link JCheckBoxMenuItem} instance to be wrapped.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     * @param <M> The type of the {@link JCheckBoxMenuItem} instance to be wrapped.
     */
    public static <M extends JCheckBoxMenuItem> UIForCheckBoxMenuItem<M> of( M checkBoxMenuItem ) {
        NullUtil.nullArgCheck(checkBoxMenuItem, "component", JCheckBoxMenuItem.class);
        return new UIForCheckBoxMenuItem<>(checkBoxMenuItem);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} without text
     *  displayed on top of the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem().onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem().onClick( it -> {..} ))
     * }</pre>
     *
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem() {
        return of(new CheckBoxMenuItem());
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} with the provided text
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem("Delete").onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem("Edit").onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return checkBoxMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem(Val.of("Delete")).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem(Val.of("Edit")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JCheckBoxMenuItem} dynamically.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return checkBoxMenuItem().withText(text);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} with the provided text
     *  displayed on the menu button and the provided icon displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem("Delete", UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem("Edit", UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( String text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return checkBoxMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} with some custom text and a boolean property,
     *  dynamically determining whether the menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem("Delete", isSelected).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem("Edit", isSelected).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the boolean property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getIsSelectedProperty()}).
     *
     * @param text The text which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determines whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( String text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return checkBoxMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically and the provided icon
     *  displayed on the menu button. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem(Val.of("Delete"), UI.icon("delete.png")).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem(Val.of("Edit"), UI.icon("edit.png")).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JCheckBoxMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JCheckBoxMenuItem}.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( Val<String> text, Icon icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return checkBoxMenuItem().withText(text).withIcon(icon);
    }

    /**
     *  A factory method to create a {@link JCheckBoxMenuItem} bound to the provided text
     *  property, whose value will be displayed on the menu button dynamically and the provided boolean property,
     *  dynamically determining whether the menu item is selected or not. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    // inside your view model class:
     *    Var<Boolean> isSelected = Var.of(false);
     *    // inside your view class:
     *    UI.popupMenu()
     *    .add(UI.checkBoxMenuItem(Val.of("Delete"), isSelected).onClick( it -> {..} ))
     *    .add(UI.checkBoxMenuItem(Val.of("Edit"), isSelected).onClick( it -> {..} ))
     * }</pre>
     * Note that in a real application you would take the text property from a model object through
     * a plain old getter method (e.g. {@code myViewModel.getTextProperty()}).
     *
     * @param text The text property which should be displayed on the wrapped {@link JCheckBoxMenuItem} dynamically.
     * @param isSelected The boolean property which will be bound to the menu item to dynamically
     *                   determines whether the menu item is selected or not.
     * @return A builder instance for the provided {@link JCheckBoxMenuItem}, which enables fluent method chaining.
     */
    public static UIForCheckBoxMenuItem<JCheckBoxMenuItem> checkBoxMenuItem( Val<String> text, Var<Boolean> isSelected ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isSelected, "isSelected", Var.class);
        return checkBoxMenuItem().withText(text).isSelectedIf(isSelected);
    }

    /**
     *  Use this to create a builder for the provided {@link JToolBar} instance.
     *  Using method chaining you can populate the {@link JToolBar} by like so: <br>
     *  <pre>{@code
     *    UI.of(myToolBar)
     *    .add(UI.button("X"))
     *    .add(UI.button("Y"))
     *    .add(UI.button("Z"))
     *    .addSeparator()
     *    .add(UI.button("A"))
     *  }</pre>
     *  <br>
     * @param component The {@link JToolBar} instance to be wrapped.
     * @param <T> The type of the {@link JToolBar} instance to be wrapped.
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <T extends JToolBar> UIForToolBar<T> of( T component ) {
        NullUtil.nullArgCheck(component, "component", JToolBar.class);
        return new UIForToolBar<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JToolBar} instance.
     *  Use method chaining to add buttons or other components to a {@link JToolBar} by
     *  passing them to {@link UIForToolBar} builder like so: <br>
     *  <pre>{@code
     *    UI.toolBar()
     *    .add(UI.button("X"))
     *    .add(UI.button("Y"))
     *    .add(UI.button("Z"))
     *    .addSeparator()
     *    .add(UI.button("A"))
     *  }</pre>
     *  <br>
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static UIForToolBar<JToolBar> toolBar() {
        return new UIForToolBar<>(new ToolBar());
    }

    /**
     *  A factory method for creating a {@link JToolBar} instance where
     *  the provided {@link Align} enum defines the orientation of the {@link JToolBar}.
     *
     * @param align The {@link Align} enum which defines the orientation of the {@link JToolBar}.
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForToolBar<JToolBar> toolBar( Align align ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return new UIForToolBar<>((JToolBar) new ToolBar()).with(align);
    }

    /**
     *  A factory method for creating a {@link JToolBar} instance where
     *  the provided {@link Val} property dynamically defines
     *  the orientation of the {@link JToolBar}
     *
     * @param align The {@link Val} property which dynamically defines the orientation of the {@link JToolBar}.
     * @return A builder instance for the provided {@link JToolBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForToolBar<JToolBar> toolBar( Val<Align> align ) {
        NullUtil.nullArgCheck(align, "align", Val.class);
        return new UIForToolBar<>((JToolBar) new ToolBar()).withAlignment(align);
    }

    /**
     *  Use this to create a builder for the provided {@link JPanel} instance.
     *
     * @return A builder instance for the provided {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JPanel> UIForPanel<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JPanel.class);
        return new UIForPanel<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager.
     *  This is in essence a convenience method for {@code UI.of(new JPanel(new MigLayout()))}.
     *
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel() { return of((JPanel) new Panel()).withLayout(new MigLayout("hidemode 2")); }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints, rowConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel( String attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return of((JPanel) new Panel()).withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints, rowConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LayoutAttr} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel( LayoutAttr attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutAttr.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return of((JPanel) new Panel()).withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel( String attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return of((JPanel) new Panel()).withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel( LayoutAttr attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutAttr.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return of((JPanel)new Panel()).withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public static UIForPanel<JPanel> panel( String attr ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        return of((JPanel) new Panel()).withLayout(attr);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public static UIForPanel<JPanel> panel( LayoutAttr attr ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutAttr.class);
        return panel(attr.toString());
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component with a
     *  dynamically updated set of {@link MigLayout} attributes.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}.
     *
     * @param attr The layout attributes property which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public static UIForPanel<JPanel> panel( Val<LayoutAttr> attr ) {
        NullUtil.nullArgCheck(attr, "attr", Val.class);
        NullUtil.nullPropertyCheck(attr, "attr", "Null is not a valid layout attribute.");
        return panel(attr.get().toString()).withLayout(attr);
    }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets
     *  based on a {@link MigLayout} as its layout manager.
     *  This factory method is especially useful for when you simply want to nest components
     *  in a {@link JPanel} without having to worry about the layout manager or the background
     *  color of the panel as it will be transparent and thus show the background of its parent.
     *  This is in essence a convenience method for {@code UI.panel("ins 0").makeNonOpaque()}.
     *
     * @return A builder instance for a transparent {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> box() { return panel().withLayout(new MigLayout("ins 0, hidemode 2")).makeNonOpaque(); }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} based on the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints, rowConstraints))).makeNonOpaque()
     *  }</pre>
     *  <br>
     * @param attr The layout attributes.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a transparent {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> box( String attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        if (attr.isEmpty()) attr = "ins 0";
        else if (!attr.contains("ins")) attr += ", ins 0";
        return panel(attr, colConstraints, rowConstraints).makeNonOpaque();
    }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints, rowConstraints))).makeNonOpaque()
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LayoutAttr} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a transparent {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> box( LayoutAttr attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutAttr.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return box(attr.toString(), colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr+", ins 0", colConstraints))).makeNonOpaque()
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a transparent {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> box( String attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        if (attr.isEmpty()) attr = "ins 0";
        else if (!attr.contains("ins")) attr += ", ins 0";
        return panel(attr, colConstraints).makeNonOpaque();
    }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr+", ins 0", colConstraints))).makeNonOpaque()
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a transparent {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> box( LayoutAttr attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutAttr.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return box(attr.toString(), colConstraints);
    }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr+", ins 0"))).makeNonOpaque()
     *  }</pre>
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public static UIForPanel<JPanel> box( String attr ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        if (attr.isEmpty()) attr = "ins 0";
        else if (!attr.contains("ins")) attr += ", ins 0";
        return panel(attr).makeNonOpaque();
    }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets.
     *  This is in essence a convenience method for {@code UI.box(attr.toString()+", ins 0").makeNonOpaque()}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a transparent {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public static UIForPanel<JPanel> box( LayoutAttr attr ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutAttr.class);
        return box(attr.toString());
    }

    /**
     *  Use this to create a builder for a transparent {@link JPanel} without any insets and a
     *  dynamically updated set of {@link MigLayout} attributes.
     *  This is in essence a convenience method for
     *  {@code UI.of(new JPanel()).withLayout(attr.viewAsString( it -> it+", ins 0"))}.
     *
     * @param attr The layout attributes property which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public static UIForPanel<JPanel> box( Val<LayoutAttr> attr ) {
        NullUtil.nullArgCheck(attr, "attr", Val.class);
        NullUtil.nullPropertyCheck(attr, "attr", "Null is not a valid layout attribute.");
        return box().withLayout(attr.view( it -> it.and("ins 0")));
    }

    /**
     *  Use this to create a builder for the provided {@link JScrollPane} component.
     *
     * @param component The {@link JScrollPane} component which should be represented by the returned builder.
     * @return A {@link UIForScrollPane} builder representing the provided component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JScrollPane> UIForScrollPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPane<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPane())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPane()
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *  }</pre>
     *
     * @return A builder instance for a new {@link JScrollPane}, which enables fluent method chaining.
     */
    public static UIForScrollPane<JScrollPane> scrollPane() { return of(new ScrollPane()); }

    /**
     *  Use this to create a builder for the provided {@link JScrollPanels} component.
     *
     * @param component The {@link JScrollPanels} component which should be represented by the returned builder.
     * @return A {@link UIForScrollPanels} builder representing the provided component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JScrollPanels> UIForScrollPanels<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPanels<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPanels} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPanels())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPanels()
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *      .add(UI.label("I am a label!"))
     *      .add(UI.button("I am a button! Click me!"))
     *  }</pre>
     *
     * @return A builder instance for a new {@link JScrollPanels}, which enables fluent method chaining.
     */
    public static UIForScrollPanels<JScrollPanels> scrollPanels() {
        return of(JScrollPanels.of(Align.VERTICAL, new Dimension(100,100)));
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPanels} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPanels())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPanels(Align.HORIZONTAL)
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *      .add(UI.label("I am a label!"))
     *      .add(UI.button("I am a button! Click me!"))
     *  }</pre>
     *
     * @param align The alignment of the scroll panels.
     * @return A builder instance for a new {@link JScrollPanels}, which enables fluent method chaining.
     */
    public static UIForScrollPanels<JScrollPanels> scrollPanels(Align align) {
        return of(JScrollPanels.of(align, null));
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPanels} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPanels())}. <br>
     *  Here is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPanels(Align.HORIZONTAL, new Dimension(100,100))
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *      .add(UI.label("I am a label!"))
     *      .add(UI.button("I am a button! Click me!"))
     *  }</pre>
     *
     * @param align The alignment of the scroll panels.
     * @param size The size of the scroll panels.
     * @return A builder instance for a new {@link JScrollPanels}, which enables fluent method chaining.
     */
    public static UIForScrollPanels<JScrollPanels> scrollPanels(Align align, Dimension size) {
        return of(JScrollPanels.of(align, size));
    }

    /**
     *  Use this to create a builder for the provided {@link JSplitPane} instance.
     *
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JSplitPane> UIForSplitPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JSplitPane.class);
        return new UIForSplitPane<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JSplitPane} instance
     *  based on the provided alignment enum determining how
     *  the split itself should be aligned. <br>
     *  You can create a simple split pane based UI like so: <br>
     *  <pre>{@code
     *      UI.splitPane(UI.Align.HORIZONTAL) // The split bar will be horizontal
     *      .withDividerAt(50)
     *      .add(UI.panel().add(...)) // top
     *      .add(UI.scrollPane().add(...)) // bottom
     *  }</pre>
     *
     * @param align The alignment determining if the {@link JSplitPane} split bar is aligned vertically or horizontally.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForSplitPane<JSplitPane> splitPane( Align align ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return of(new JSplitPane(align.forSplitPane()));
    }

    /**
     *  Use this to create a builder for a new {@link JSplitPane} instance
     *  based on the provided alignment property determining how
     *  the split itself should be aligned. <br>
     *  You can create a simple split pane based UI like so: <br>
     *  <pre>{@code
     *    UI.splitPane(viewModel.getAlignment())
     *    .withDividerAt(50)
     *    .add(UI.panel().add(...)) // top
     *    .add(UI.scrollPane().add(...)) // bottom
     *  }</pre>
     *  <br>
     *  The split pane will be updated whenever the provided property changes.
     *  <br>
     *  <b>Note:</b> The provided property must not be {@code null}!
     *  Otherwise, an {@link IllegalArgumentException} will be thrown.
     *  <br>
     * @param align The alignment determining if the {@link JSplitPane} split bar is aligned vertically or horizontally.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForSplitPane<JSplitPane> splitPane( Val<Align> align ) {
        NullUtil.nullArgCheck(align, "align", Val.class);
        NullUtil.nullPropertyCheck(align, "align", "Null is not a valid alignment.");
        return of(new JSplitPane(align.get().forSplitPane()))
                .withAlignment(align);
    }

    /**
     *  Use this to create a builder for the provided {@link JEditorPane} instance.
     *
     * @param component The {@link JEditorPane} instance to create a builder for.
     * @param <P> The type of the {@link JEditorPane} instance.
     * @return A builder instance for the provided {@link JEditorPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JEditorPane> UIForEditorPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JEditorPane.class);
        return new UIForEditorPane<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JEditorPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JEditorPane())}.
     *
     * @return A builder instance for a new {@link JEditorPane}, which enables fluent method chaining.
     */
    public static UIForEditorPane<JEditorPane> editorPane() { return of(new EditorPane()); }

    /**
     *  Use this to create a builder for the provided {@link JTextPane} instance.
     *
     * @param component The {@link JTextPane} instance to create a builder for.
     * @param <P> The type of the {@link JTextPane} instance.
     * @return A builder instance for the provided {@link JTextPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JTextPane> UIForTextPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JTextPane.class);
        return new UIForTextPane<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JTextPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextPane())}.
     *
     * @return A builder instance for a new {@link JTextPane}, which enables fluent method chaining.
     */
    public static UIForTextPane<JTextPane> textPane() { return of(new TextPane()); }

    /**
     *  Use this to create a builder for the provided {@link JSlider} instance.
     *
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <S extends JSlider> UIForSlider<S> of( S component ) {
        NullUtil.nullArgCheck(component, "component", JSlider.class);
        return new UIForSlider<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JSlider} instance
     *  based on tbe provided alignment type determining if
     *  the slider will be aligned vertically or horizontally.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     */
    public static UIForSlider<JSlider> slider( Align align ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return of((JSlider) new Slider()).with(align);
    }

    /**
     *  Use this to create a builder for a new {@link JSlider} instance
     *  based on tbe provided alignment property which dynamically
     *  determines if the property is aligned vertically or horizontally.
     *
     * @param align The alignment property determining if the {@link JSlider} aligns vertically or horizontally.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if the {@code align} property is {@code null}.
     *
     * @see JSlider#setOrientation
     */
    public static UIForSlider<JSlider> slider( Val<Align> align ) {
        NullUtil.nullArgCheck( align, "align", Val.class );
        return of((JSlider) new Slider()).withAlignment(align);
    }

    /**
     *  Use this to create a builder for a new {@link JSlider} instance
     *  based on tbe provided alignment type, min slider value and max slider value.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     */
    public static UIForSlider<JSlider> slider( Align align, int min, int max ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return of((JSlider) new Slider())
                    .with(align)
                    .withMin(min)
                    .withMax(max)
                    .withValue((min + max) / 2);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and initial values.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @param value  the initial value of the slider
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static UIForSlider<JSlider> slider( Align align, int min, int max, int value ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return of((JSlider) new Slider())
                .with(align)
                .withMin(min)
                .withMax(max)
                .withValue(value);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and dynamic value.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @param value The property holding the value of the slider
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static UIForSlider<JSlider> slider( Align align, int min, int max, Val<Integer> value ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        NullUtil.nullPropertyCheck(value, "value", "The state of the slider should not be null!");
        return of((JSlider) new Slider())
                .with(align)
                .withMin(min)
                .withMax(max)
                .withValue(value);
    }

    /**
     * Creates a slider with the specified alignment and the
     * specified minimum, maximum, and dynamic value.
     *
     * @param align The alignment determining if the {@link JSlider} aligns vertically or horizontally.
     * @param min The minimum possible value of the slider.
     * @param max The maximum possible value of the slider.
     * @param value The property holding the value of the slider
     *
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     *
     * @see JSlider#setOrientation
     * @see JSlider#setMinimum
     * @see JSlider#setMaximum
     * @see JSlider#setValue
     */
    public static UIForSlider<JSlider> slider( Align align, int min, int max, Var<Integer> value ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        NullUtil.nullPropertyCheck(value, "value", "The state of the slider should not be null!");
        return of((JSlider) new Slider())
                .with(align)
                .withMin(min)
                .withMax(max)
                .withValue(value);
    }

    /**
     *  Use this to create a builder for the provided {@link JComboBox} instance.
     *
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     */
    public static <E, C extends JComboBox<E>> UIForCombo<E,C> of( C component ) {
        NullUtil.nullArgCheck(component, "component", JComboBox.class);
        return new UIForCombo<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JComboBox} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JComboBox())}.
     *
     * @return A builder instance for a new {@link JComboBox}, which enables fluent method chaining.
     */
    public static UIForCombo<Object,JComboBox<Object>> comboBox() { return of(new ComboBox<>()); }

    /**
     *  Use this to create a builder for a new {@link JComboBox} instance
     *  with the provided array of elements as selectable items.
     *
     * @param items The array of elements to be selectable in the {@link JComboBox}.
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    @SafeVarargs
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( E... items ) {
        NullUtil.nullArgCheck(items, "items", Object[].class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ArrayBasedComboModel<>(items));
    }

    /**
     *  Use this to create a builder for a new {@link JComboBox} instance
     *  with the provided array of elements as selectable items which
     *  may not be modified by the user.
     *
     * @param items The unmodifiable array of elements to be selectable in the {@link JList}.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    @SafeVarargs
    public static <E> UIForCombo<E,JComboBox<E>> comboBoxWithUnmodifiable( E... items ) {
        NullUtil.nullArgCheck(items, "items", Object[].class); // Unmodifiable
        java.util.List<E> unmodifiableList = Collections.unmodifiableList(java.util.Arrays.asList(items));
        return comboBox(unmodifiableList);
    }

    /**
     *  Use this to create a builder for a new {@link JComboBox} instance
     *  where the provided enum based property dynamically models the selected item
     *  as well as all possible options (all the enum states).
     *  The property will be updated whenever the user
     *  selects a new item in the {@link JComboBox} and the {@link JComboBox}
     *  will be updated whenever the property changes in your code (see {@link Var#set(Object)}).
     *  <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.comboBox(vm.selection())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      The text displayed on the combo box is based on the {@link Object#toString()}
     *      method of the enum instances. If you want to customize how they are displayed
     *      (So that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      simply override the {@link Object#toString()} method in your enum. </i>
     *
     * @param selectedItem A property modelling the selected item in the combo box.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E extends Enum<E>> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selectedItem ) {
        NullUtil.nullArgCheck(selectedItem, "var", Var.class);
        // We get an array of possible enum states from the enum class
        return comboBox(selectedItem.type().getEnumConstants()).withSelectedItem(selectedItem);
    }

    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided list of elements as selectable items.
     *
     * @param items The list of elements to be selectable in the {@link JComboBox}.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( java.util.List<E> items ) {
        NullUtil.nullArgCheck(items, "items", List.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ListBasedComboModel<>(items));
    }

    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided list of elements as selectable items which
     *  may not be modified by the user.
     *
     * @param items The list of elements to be selectable in the {@link JComboBox}.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBoxWithUnmodifiable( java.util.List<E> items ) {
        NullUtil.nullArgCheck(items, "items", List.class);
        java.util.List<E> unmodifiableList = Collections.unmodifiableList(items);
        return comboBox(unmodifiableList);
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a list of items as a dynamically sized model for the
     *  selectable items.
     *  <p>
     *  Note that the provided list may be mutated by the combo box UI component
     *
     * @param selection The property holding the current selection.
     * @param items The list of selectable items.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
     public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selection, java.util.List<E> items ) {
        NullUtil.nullArgCheck(items, "items", List.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ListBasedComboModel<>(selection, items));
    }

    //___

    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided properties list object as selectable (and mutable) items.
     *
     * @param items The {@link Vars} properties of elements to be selectable in the {@link JComboBox}.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Vars<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vars.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new VarsBasedComboModel<>(items));
    }

    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided properties list object as selectable (and immutable) items which
     *  may not be modified by the user.
     *
     * @param items The {@link sprouts.Vals} properties of elements to be selectable in the {@link JComboBox}.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Vals<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vals.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ValsBasedComboModel<>(items));
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a list of items as a dynamically sized model for the
     *  selectable items.
     *  <p>
     *  Note that the provided list may be mutated by the combo box UI component
     *
     * @param selection The property holding the current selection.
     * @param items The list of selectable items.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
     public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selection, Vars<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vars.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new VarsBasedComboModel<>(selection, items));
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and a property list of items as a dynamically sized model for the
     *  selectable items which may not be modified by the user.
     *
     * @param selection The property holding the current selection.
     * @param items The list of selectable items which may not be modified by the user.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the list.
     */
     public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selection, Vals<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vals.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ValsBasedComboModel<>(selection, items));
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and an array of items as a fixed-size model for the
     *  selectable items.
     *  <p>
     *  Note that the provided array may be mutated by the combo box UI component
     *
     * @param var The property holding the current selection.
     * @param items The array of selectable items.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> var, E... items ) {
        NullUtil.nullArgCheck(items, "items", List.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ArrayBasedComboModel<>(var, items));
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and an array property of items as a selectable items model
     *  of variable length.
     *  <p>
     *  Note that the provided array may be mutated by the combo box UI component
     *
     * @param var The property holding the current selection.
     * @param items The property holding an array of selectable items which can be mutated by the combo box.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> var, Var<E[]> items ) {
        NullUtil.nullArgCheck(items, "items", List.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ArrayPropertyComboModel<>(var, items));
    }

    /**
     *  Creates a combo box UI builder node with a {@link Var} property as the model
     *  for the current selection and an array property of items as a selectable items model
     *  of variable length.
     *  <p>
     *  Note that the provided array may be mutated by the combo box UI component
     *
     * @param selectedItem The property holding the current selection.
     * @param items The property holding an array of selectable items which may not be modified by the user.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Var<E> selectedItem, Val<E[]> items ) {
        NullUtil.nullArgCheck(items, "items", List.class);
        NullUtil.nullArgCheck(selectedItem, "selectedItem", Var.class);
        return of((JComboBox<E>) new ComboBox<E>()).withModel(new ArrayPropertyComboModel<>(selectedItem, items));
    }

    /**
     *  Created a combo box UI builder node with the provided {@link ComboBoxModel}.
     *
     * @param model The model to be used by the combo box.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @param <E> The type of the elements in the combo box.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( ComboBoxModel<E> model ) {
        NullUtil.nullArgCheck(model, "model", ComboBoxModel.class);
        JComboBox<E> c = new ComboBox<E>();
        c.setModel(model);
        return of(c);
    }

    /**
     *  Use this to create a builder for the provided {@link JSpinner} instance.
     *
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static <S extends JSpinner> UIForSpinner<S> of( S spinner ) {
        NullUtil.nullArgCheck(spinner, "spinner", JSpinner.class);
        return new UIForSpinner<>(spinner);
    }

    /**
     *  Use this to create a builder for a new {@link JSpinner} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSpinner())}.
     *
     * @return A builder instance for a new {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<JSpinner> spinner() { return of(new Spinner()); }

    /**
     * Use this to create a builder for the provided {@link JSpinner} instance
     * with the provided {@link SpinnerModel} as the model.
     *
     * @param model The {@link SpinnerModel} to be used by the {@link JSpinner}.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( SpinnerModel model ) {
        NullUtil.nullArgCheck(model, "model", SpinnerModel.class);
        return of((JSpinner) new Spinner()).peek( s -> s.setModel(model) );
    }

    /**
     *  Use this factory method to create a {@link JSpinner} bound to a property of any type.
     *  The property will be updated when the user modifies its value.
     *
     * @param value A property of any type which should be bound to this spinner.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( Var<?> value ) {
        NullUtil.nullArgCheck(value, "value", Var.class);
        NullUtil.nullPropertyCheck(value, "value", "The state of the spinner should not be null!");
        return spinner().withValue(value);
    }

    /**
     * Use this to create a builder for the provided {@link JSpinner} instance
     * with the provided {@code min}, {@code max}, default {@code value} and {@code step} as the model.
     *
     * @param value The default value of the {@link JSpinner}.
     * @param min The minimum possible value of the {@link JSpinner}.
     * @param max The maximum possible value of the {@link JSpinner}.
     * @param step The step size of the {@link JSpinner}.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( int value, int min, int max, int step ) {
        return of((JSpinner) new Spinner()).peek( s -> s.setModel(new SpinnerNumberModel(value, min, max, step)) );
    }

    /**
     * Use this to create a builder for the provided {@link JSpinner} instance
     * with the provided {@code min}, {@code max} and default {@code value} as the model.
     *
     * @param value The default value of the {@link JSpinner}.
     * @param min The minimum possible value of the {@link JSpinner}.
     * @param max The maximum possible value of the {@link JSpinner}.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( int value, int min, int max ) {
        return of((JSpinner) new Spinner()).peek( s -> s.setModel(new SpinnerNumberModel(value, min, max, 1)) );
    }

    /**
     *  Use this to create a builder for the provided {@link JLabel} instance.
     *
     * @param component The {@link JLabel} instance to be used by the builder.
     * @return A builder instance for the provided {@link JLabel}, which enables fluent method chaining.
     */
    public static <L extends JLabel> UIForLabel<L> of( L component ) {
        NullUtil.nullArgCheck(component, "component", JLabel.class);
        return new UIForLabel<>(component);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(text)}.
     *
     * @param text The text which should be displayed on the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of((JLabel) new Label()).withText(text);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(Val<String> text)}.
     *
     * @param text The text property which should be bound to the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JLabel) new Label())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return of((JLabel) new Label()).with(icon);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon dynamically.
     *
     * @param icon The icon property which should dynamically provide a desired icon for the {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> labelWithIcon( Val<Icon> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "Null icons are not allowed!");
        return of((JLabel) new Label()).withIcon(icon);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param width The width of the icon when displayed on the label.
     * @param height The height of the icon when displayed on the label.
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( int width, int height, ImageIcon icon ) {
        NullUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        return of((JLabel) new Label())
                .with(new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT)));
    }

    /**
     *  Use this to create a UI builder for a {@link JLabel} with bold font.
     *  This is in essence a convenience method for {@code UI.label(String text).makeBold()}.
     *  @param text The text which should be displayed on the label.
     *  @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> boldLabel( String text ) {
        return of((JLabel) new Label()).withText(text).makeBold();
    }

    /**
     *  Use this to create a UI builder for a bound {@link JLabel} with bold font.
     *  This is in essence a convenience method for {@code UI.label(Val<String> text).makeBold()}.
     *  @param text The text property which should be displayed on the label dynamically.
     *  @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> boldLabel( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JLabel) new Label()).withText(text).makeBold();
    }

    /**
     *  Use this to create a builder for a {@link JLabel} displaying HTML.
     *  This is in essence a convenience method for {@code UI.of(new JLabel("<html>" + text + "</html>"))}.
     *
     * @param text The html text which should be displayed on the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> html( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of((JLabel) new Label()).withText("<html>" + text + "</html>");
    }

    /**
     *  Use this to create a builder for a {@link JLabel} displaying HTML.
     *  This is in essence a convenience method for {@code UI.of(new JLabel("<html>" + text + "</html>"))}.
     *
     * @param text The html text property which should be bound to the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> html( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JLabel) new Label())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text.view( it -> "<html>" + it + "</html>"));
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance with the provided
     *  text displayed on it.
     *
     * @param text The text which should be displayed on the checkbox.
     * @return A builder instance for the checkbox, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of((JCheckBox) new CheckBox()).withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance where the provided
     *  text property dynamically displays its value on the checkbox.
     *
     * @param text The text property which should be bound to the checkbox.
     * @return A builder instance for the checkbox, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text property is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JCheckBox) new CheckBox())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance
     *  where the provided text property dynamically displays its value on the checkbox
     *  and the provided selection property dynamically determines whether the checkbox
     *  is selected or not.
     *
     * @param text The text property which should be bound to the checkbox.
     *             This is the text which is displayed on the checkbox.
     * @param isChecked The selection property which should be bound to the checkbox and determines whether it is selected or not.
     * @return A builder instance for the checkbox, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text property is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( Val<String> text, Var<Boolean> isChecked ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isChecked, "isChecked", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        NullUtil.nullPropertyCheck(isChecked, "isChecked", "The selection state of a check box may not be modelled using null!");
        return of((JCheckBox) new CheckBox())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .applyIf(!isChecked.hasNoID(), it -> it.id(isChecked.id()))
                .withText(text)
                .isSelectedIf(isChecked);
    }

    /**
     *  Creates a builder node wrapping a new {@link JCheckBox} instance
     *  with the provided text displayed on it and the provided selection property
     *  dynamically determining whether the checkbox is selected or not.
     *  @param text The text which should be displayed on the checkbox.
     *  @param isChecked The selection property which should be bound to the checkbox and determines whether it is selected or not.
     *  @return A builder instance for the checkbox, which enables fluent method chaining.
     *  @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForCheckBox<JCheckBox> checkBox( String text, Var<Boolean> isChecked ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(isChecked, "isChecked", Var.class);
        NullUtil.nullPropertyCheck(isChecked, "isChecked", "The selection state of a check box may not be modelled using null!");
        return of((JCheckBox) new CheckBox())
                .applyIf(!isChecked.hasNoID(), it -> it.id(isChecked.id()))
                .withText(text)
                .isSelectedIf(isChecked);
    }

    /**
     *  Use this to create a builder for the provided {@link JCheckBox} instance.
     *
     * @return A builder instance for the provided {@link JCheckBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided checkbox is null.
     */
    public static <B extends JCheckBox> UIForCheckBox<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JCheckBox.class);
        return new UIForCheckBox<>(component);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance with the provided
     *  text displayed on it.
     *
     * @param text The text which should be displayed on the radio button.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForRadioButton<JRadioButton> radioButton( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of((JRadioButton) new RadioButton()).withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance where the provided
     *  text property dynamically displays its value on the radio button.
     *
     * @param text The text property which should be bound to the radio button.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     */
    public static UIForRadioButton<JRadioButton> radioButton( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JRadioButton) new RadioButton())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance
     *  where the provided text property dynamically displays its value on the radio button
     *  and the provided selection property dynamically determines whether the radio button
     *  is selected or not.
     *
     * @param text The text property which should be bound to the radio button.
     *             This is the text which is displayed on the radio button.
     * @param selected The selection property which should be bound to the radio button and determines whether it is selected or not.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text property is null.
     */
    public static UIForRadioButton<JRadioButton> radioButton( Val<String> text, Var<Boolean> selected ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(text, "selected", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        NullUtil.nullPropertyCheck(selected, "selected", "The selection state of a radio button may not be modelled using null!");
        return of((JRadioButton) new RadioButton())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .applyIf(!selected.hasNoID(), it -> it.id(selected.id()))
                .withText(text)
                .isSelectedIf(selected);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance
     *  with the provided text displayed on it and the provided selection property
     *  dynamically determining whether the radio button is selected or not.
     *  @param text The text which should be displayed on the radio button.
     *  @param selected The selection property which should be bound to the radio button and determines whether it is selected or not.
     *  @return A builder instance for the radio button, which enables fluent method chaining.
     *  @throws IllegalArgumentException If the provided text is null.
     */
    public static UIForRadioButton<JRadioButton> radioButton( String text, Var<Boolean> selected ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(text, "selected", Var.class);
        NullUtil.nullPropertyCheck(selected, "selected", "The selection state of a radio button may not be modelled using null!");
        return of((JRadioButton) new RadioButton())
                .withText(text)
                .isSelectedIf(selected);
    }

    /**
     *  Creates a builder node wrapping a new {@link JRadioButton} instance
     *  dynamically bound to an enum based {@link sprouts.Var}
     *  instance which will be used to dynamically model the selection state of the
     *  wrapped {@link JToggleButton} type by checking
     *  weather the property matches the provided enum or not.
     *  <br>
     *  Here's an example of how to use this method: <br>
     *  <pre>{@code
     *      // In your view model:
     *      enum Size { SMALL, MEDIUM, LARGE }
     *      private Var<Size> selection = Var.of(Size.SMALL);
     *
     *      public Var<Size> selection() { return selection; }
     *
     *      // In your view:
     *      UI.panel()
     *      .add(UI.radioButton(Size.SMALL,  vm.selection())
     *      .add(UI.radioButton(Size.MEDIUM, vm.selection())
     *      .add(UI.radioButton(Size.LARGE,  vm.selection())
     * }</pre>
     * <p>
     * <b>Tip:</b><i>
     *      For the text displayed on the radio buttons, the enums will be converted
     *      to strings using {@link Object#toString()} method.
     *      If you want to customize how they are displayed
     *      (So that 'Size.LARGE' is displayed as 'Large' instead of 'LARGE')
     *      simply override the {@link Object#toString()} method in your enum. </i>
     *
     *
     * @param state The reference {@link Enum} which this {@link JToggleButton} should represent.
     * @param selection The {@link sprouts.Var} instance which will be used
     *                  to dynamically model the selection state of the wrapped {@link JToggleButton} type.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public static <E extends Enum<E>> UIForRadioButton<JRadioButton> radioButton( E state, Var<E> selection ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullPropertyCheck(selection, "selection", "The selection state of a radio button may not be modelled using null!");
        return of((JRadioButton) new RadioButton())
                .applyIf(!selection.hasNoID(), it -> it.id(selection.id()))
                .isSelectedIf( state, selection );
    }

    /**
     *  Use this to create a builder for the provided {@link JRadioButton} instance.
     *
     * @param component The {@link JRadioButton} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JRadioButton}, which enables fluent method chaining.
     */
    public static <R extends JRadioButton> UIForRadioButton<R> of( R component ) {
        NullUtil.nullArgCheck(component, "component", JRadioButton.class);
        return new UIForRadioButton<>(component);
    }

    /**
     *  Use this to create a builder for a {@link JToggleButton} instance.
     *
     * @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton() { return of((JToggleButton) new ToggleButton()); }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  with the provided text displayed on it.
     *
     * @param text The text which should be displayed on the toggle button.
     * @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return toggleButton().withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  where the provided text property dynamically displays its value on the toggle button.
     *
     * @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return toggleButton()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  where the provided boolean property dynamically determines whether the toggle button is selected or not.
     *  @param  isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     *  @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Var<Boolean> isToggled ) {
        NullUtil.nullPropertyCheck(isToggled, "isToggled");
        return toggleButton()
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  with the provided text displayed on it and the provided boolean property
     *  dynamically determining whether the toggle button is selected or not.
     *  @param text The text which should be displayed on the toggle button.
     *  @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     *  @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( String text, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullPropertyCheck(isToggled, "isToggled");
        return toggleButton()
                .withText(text)
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance
     *  where the provided text property dynamically displays its value on the toggle button
     *  and the provided boolean property dynamically determines whether the toggle button is selected or not.
     *  @param text The text property which should be bound to the toggle button.
     *             This is the text which is displayed on the toggle button.
     *  @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     *  @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Val<String> text, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(isToggled, "isToggled", Var.class);
        NullUtil.nullPropertyCheck(isToggled, "isToggled", "The selection state of a toggle button may not be modelled using null!");
        return toggleButton()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .withText(text)
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance with
     *  the provided {@link Icon} displayed on it.
     *
     * @param icon The icon which should be displayed on the toggle button.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return toggleButton().withIcon(icon);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance with
     *  the provided {@link Icon} displayed on it and the provided boolean property
     *  dynamically determining whether the toggle button is selected or not.
     *
     * @param icon The icon which should be displayed on the toggle button.
     * @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( Icon icon, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullPropertyCheck(isToggled, "isToggled", "The selection state of a toggle button may not be modelled using null!");
        return toggleButton(icon)
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance where
     *  the provided {@link Icon} property dynamically displays its value on the toggle button.
     *
     * @param icon The icon property which should be bound to the toggle button.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButtonWithIcon( Val<Icon> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "The icon of a toggle button may not be modelled using null!");
        return of(new JToggleButton())
                .applyIf(!icon.hasNoID(), it -> it.id(icon.id()))
                .withIcon(icon);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance where
     *  the provided {@link Icon} property dynamically displays its value on the toggle button
     *  and the provided boolean property dynamically determines whether the toggle button is selected or not.
     *
     * @param icon The icon property which should be bound to the toggle button.
     * @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButtonWithIcon( Val<Icon> icon, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "The icon of a toggle button may not be modelled using null!");
        NullUtil.nullPropertyCheck(isToggled, "isToggled", "The selection state of a toggle button may not be modelled using null!");
        return of(new JToggleButton())
                .applyIf(!icon.hasNoID(), it -> it.id(icon.id()))
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .withIcon(icon)
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for the provided {@link JToggleButton} instance.
     *
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static <B extends JToggleButton> UIForToggleButton<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JToggleButton.class);
        return new UIForToggleButton<>(component);
    }

    /**
     *  Use this to create a builder for the provided {@link JTextField} instance.
     *
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <F extends JTextField> UIForTextField<F> of( F component ) {
        NullUtil.nullArgCheck(component, "component", JTextComponent.class);
        return new UIForTextField<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided text displayed on it.
     *
     * @param text The text which should be displayed on the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of((JTextField) new TextField()).withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided text property dynamically displaying its value on the text field.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the text field.
     *
     * @param text The text property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JTextField) new TextField())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided text property dynamically displaying its value on the text field.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JTextField) new TextField())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextField())}.
     *
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField() { return of((JTextField) new TextField()); }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field.
     *  The property is a {@link Var}, meaning that it can be modified by the user.
     *  <p>
     *  The number property will only receive values if the text field contains a valid number.
     *
     * @param number The number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        return of((JTextField) new TextField())
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field
     *  and a boolean property which will be set to {@code true} if the text field contains a valid number,
     *  and {@code false} otherwise.
     *  <p>
     *  The number property will only receive values if the text in the text field can be parsed as a number,
     *  in which case the provided {@link Var} will be set to {@code true}, otherwise it will be set to {@code false}.
     *
     * @param number The number property which should be bound to the text field.
     * @param isValid A {@link Var} which will be set to {@code true} if the text field contains a valid number,
     *                and {@code false} otherwise.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number, Var<Boolean> isValid ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        return of((JTextField) new TextField())
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number, isValid);
    }


    /**
     *  Use this to create a builder for the provided {@link JFormattedTextField} instance.
     *
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField of( JFormattedTextField component ) {
        NullUtil.nullArgCheck(component, "component", JFormattedTextField.class);
        return new UIForFormattedTextField(component);
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} instance with
     *  the provided text displayed on it.
     *
     * @param text The text which should be displayed on the text field.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of(new JFormattedTextField(text));
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} instance with
     *  the provided text property dynamically displaying its value in the text field.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the text field.
     *
     * @param text The text property which should be bound to the text field.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of(new JFormattedTextField())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} instance with
     *  the provided text property dynamically displaying its value in the formatted text field.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the formatted text field.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of(new JFormattedTextField())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JFormattedTextField())}.
     *
     * @return A builder instance for a new {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField() { return of(new JFormattedTextField()); }

    /**
     *  Use this to create a builder for the provided {@link JPasswordField} instance.
     *
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     */
    public static <F extends JPasswordField> UIForPasswordField<F> of( F component ) {
        NullUtil.nullArgCheck(component, "component", JPasswordField.class);
        return new UIForPasswordField<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} instance with
     *  the provided text as the initial password.
     *
     * @param text The initial password which should be displayed on the password field.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of((JPasswordField) new PasswordField()).withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} instance with
     *  the provided text property dynamically displaying its value in the password field.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the password field.
     *
     * @param text The text property which should be bound to the password field.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of(new JPasswordField())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} instance with
     *  the provided text property dynamically displaying its value in the password field.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the password field.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of(new JPasswordField())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPasswordField())}.
     *
     * @return A builder instance for a new {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField() { return of(new JPasswordField()); }

    /**
     *  Use this to create a builder for the provided {@link JProgressBar} instance.
     *
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static <P extends JProgressBar> UIForProgressBar<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JProgressBar.class);
        return new UIForProgressBar<>(component);
    }

    /**
     *  A factory method for creating a progress bar builder with a default {@link JProgressBar} implementation.
     *
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar() { return of(new ProgressBar()); }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided minimum and maximum values.
     *
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( int min, int max ) {
        return progressBar().withMin(min).withMax(max);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided minimum, maximum and current value.
     *
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( int min, int max, int value ) {
        return progressBar().withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided minimum, maximum and current value property dynamically bound to the progress bar.
     *
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value property of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( int min, int max, Val<Integer> value ) {
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid value for the value property of a progress bar.");
        return progressBar().withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided alignment, minimum and maximum values.
     *  The alignment is a {@link Align} value, which may be either {@link Align#HORIZONTAL}
     *  or {@link Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( Align align, int min, int max ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return progressBar().with(align).withMin(min).withMax(max);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided alignment, minimum, maximum and current value.
     *  The alignment is a {@link Align} value, which may be either {@link Align#HORIZONTAL}
     *  or {@link Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( Align align, int min, int max, int value ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return progressBar().with(align).withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with
     *  the provided alignment, minimum, maximum and current value property dynamically bound to the progress bar.
     *  The alignment is a {@link Align} value, which may be either {@link Align#HORIZONTAL}
     *  or {@link Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param min The minimum value of the progress bar.
     * @param max The maximum value of the progress bar.
     * @param value The current value property of the progress bar.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( Align align, int min, int max, Val<Integer> value ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        NullUtil.nullArgCheck(value, "value", Val.class);
        NullUtil.nullPropertyCheck(value, "value", "Null is not a valid value for the value property of a progress bar.");
        return progressBar().with(align).withMin(min).withMax(max).withValue(value);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with a default minimum and maximum value
     *  of 0 and 100 and the provided alignment and double based progress property (a property wrapping a double value between 0 and 1)
     *  dynamically bound to the progress bar.
     *  The alignment is a {@link Align} value, which may be either {@link Align#HORIZONTAL}
     *  or {@link Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param progress The current progress property of the progress bar, a property wrapping a double value between 0 and 1.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( Align align, Val<Double> progress ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        NullUtil.nullArgCheck(progress, "progress", Val.class);
        NullUtil.nullPropertyCheck(progress, "progress", "Null is not a valid value for the progress property of a progress bar.");
        return progressBar().with(align).withMin(0).withMax(100).withProgress(progress);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with a default minimum and maximum value
     *  of 0 and 100 and the provided alignment and double based progress property (a property wrapping a double value between 0 and 1)
     *  dynamically bound to the progress bar.
     *  The alignment is a {@link Align} value, which may be either {@link Align#HORIZONTAL}
     *  or {@link Align#VERTICAL}.
     *
     * @param align The alignment of the progress bar.
     * @param progress The current progress property of the progress bar, a property wrapping a double value between 0 and 1.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( Align align, double progress ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        return progressBar().with(align).withMin(0).withMax(100).withProgress(progress);
    }

    /**
     *  Use this to create a builder for a new {@link JProgressBar} instance with a default minimum and maximum value
     *  of 0 and 100 and the provided alignment property and double based progress
     *  property (a property wrapping a double value between 0 and 1)
     *  dynamically bound to the progress bar.
     *  The alignment property wraps a {@link Align} value, which may be either {@link Align#HORIZONTAL}
     *  or {@link Align#VERTICAL}.
     *  When any of the two properties change in your view model, the progress bar will be updated accordingly.
     *
     * @param align The alignment of the progress bar.
     * @param progress The current progress property of the progress bar, a property wrapping a double value between 0 and 1.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar( Val<Align> align, Val<Double> progress ) {
        NullUtil.nullArgCheck(align, "align", Align.class);
        NullUtil.nullArgCheck(progress, "progress", Val.class);
        NullUtil.nullPropertyCheck(progress, "progress", "Null is not a valid value for the progress property of a progress bar.");
        return progressBar().withAlignment(align).withMin(0).withMax(100).withProgress(progress);
    }

    /**
     *  Use this to create a builder for the provided {@link JTextArea} instance.
     *
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static <A extends JTextArea> UIForTextArea<A> of( A area ) {
        NullUtil.nullArgCheck(area, "area", JTextArea.class);
        return new UIForTextArea<>(area);
    }

    /**
     *  Use this to create a builder for a new {@link JTextArea} instance with
     *  the provided text as the initial text.
     *
     * @param text The initial text which should be displayed on the text area.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( String text ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        return of((JTextArea) new TextArea()).withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextArea} instance with
     *  the provided text property dynamically displaying its value in the text area.
     *  The property is a {@link Val}, meaning that it is read-only and may not be changed
     *  by the text area.
     *
     * @param text The text property which should be bound to the text area.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JTextArea) new TextArea())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextArea} instance with
     *  the provided text property dynamically displaying its value in the text area.
     *  The property may also be modified by the user.
     *
     * @param text The text property which should be bound to the text area.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( Var<String> text ) {
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JTextArea) new TextArea())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  A convenience method for creating a builder for a {@link JTextArea} with a certain text alignment.
     *  This is a shortcut version for the following code:
     *  <pre>{@code
     *      UI.textArea()
     *          .withTextOrientation(UI.HorizontalDirection.RIGHT_TO_LEFT);
     *  }</pre>
     * The provided {@link UI.HorizontalDirection} translates to {@link ComponentOrientation}
     * instances which are used to align the elements or text within the wrapped {@link JTextComponent}.
     * {@link LayoutManager} and {@link Component}
     * subclasses will use this property to
     * determine how to lay out and draw components.
     *
     * @param direction The text orientation type which should be used.
     * @return A builder instance for a new {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( UI.HorizontalDirection direction ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalDirection.class);
        return of((JTextArea) new TextArea()).withTextOrientation(direction);
    }

    /**
     *  A convenience method for creating a builder for a {@link JTextArea} with a certain text and text alignment.
     *  This is a shortcut version for the following code:
     *  <pre>{@code
     *      UI.textArea()
     *          .withTextOrientation(UI.HorizontalDirection.RIGHT_TO_LEFT)
     *          .withText(text);
     *  }</pre>
     * The provided {@link UI.HorizontalDirection} translates to {@link ComponentOrientation}
     * instances which are used to align the elements or text within the wrapped {@link JTextComponent}.
     * {@link LayoutManager} and {@link Component}
     * subclasses will use this property to
     * determine how to lay out and draw components.
     *
     * @param direction The text orientation type which should be used.
     * @param text The new text to be set for the wrapped text component type.
     * @return A builder instance for a new {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea<JTextArea> textArea( UI.HorizontalDirection direction, String text ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalDirection.class);
        return of((JTextArea) new TextArea()).withTextOrientation(direction).withText(text);
    }

    public static UIForTextArea<JTextArea> textArea( UI.HorizontalDirection direction, Val<String> text ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalDirection.class);
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JTextArea) new TextArea())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withTextOrientation(direction)
                .withText(text);
    }

    public static UIForTextArea<JTextArea> textArea( UI.HorizontalDirection direction, Var<String> text ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalDirection.class);
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return of((JTextArea) new TextArea())
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withTextOrientation(direction)
                .withText(text);
    }

    /**
     * @param <E> The type of the elements in the list.
     * @return A builder instance for the provided {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> of( JList<E> list ) {
        NullUtil.nullArgCheck(list, "list", JList.class);
        return new UIForList<>(list);
    }

    /**
     * @return A builder instance for a new {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> list() { return of(new List<>()); }

    /**
     * @param model The model which should be used for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> list( ListModel<E> model ) {
        NullUtil.nullArgCheck(model, "model", ListModel.class);
        JList<E> list = new List<>();
        list.setModel(model);
        return of(list);
    }

    /**
     *  Creates a new {@link JList} instance with the provided array
     *  as data model.
     *  This is functionally equivalent to {@link #listOf(Object...)}.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList} with the provided array as data model.
     */
    @SafeVarargs
    public static <E> UIForList<E, JList<E>> list( E... elements ) {
        NullUtil.nullArgCheck(elements, "elements", Object[].class);
        return of(new List<E>()).withEntries( elements );
    }

    public static <E> UIForList<E, JList<E>> list( Vals<E> elements ) {
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return of(new List<E>()).withEntries( elements );
    }

    public static <E> UIForList<E, JList<E>> list( Var<E> selection, Vals<E> elements ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return of(new List<E>()).withEntries( elements ).withSelection( selection );
    }

    public static <E> UIForList<E, JList<E>> list( Val<E> selection, Vals<E> elements ) {
        NullUtil.nullArgCheck(selection, "selection", Val.class);
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return of(new List<E>()).withEntries( elements ).withSelection( selection );
    }

    /**
     *  Creates a new {@link JList} instance with the provided array
     *  as data model.
     *  This is functionally equivalent to {@link #list(Object...)}.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList} with the provided array as data model.
     */
    @SafeVarargs
    public static <E> UIForList<E, JList<E>> listOf( E... elements ) { return list( elements ); }

    /**
     *  Creates a new {@link JList} instance with the provided {@link List}
     *  as data model.
     *  This is functionally equivalent to {@link #listOf(java.util.List)}.
     *
     * @return A builder instance for a new {@link JList} with the provided {@link List} as data model.
     */
    public static <E> UIForList<E, JList<E>> list( java.util.List<E> entries ) {
        return of(new List<E>()).withEntries( entries );
    }

    /**
     *  Creates a new {@link JList} instance with the provided {@link List}
     *  as data model.
     *  This is functionally equivalent to {@link #list(java.util.List)}.
     *
     * @param entries The elements which should be used as model data for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList} with the provided {@link List} as data model.
     */
    public static <E> UIForList<E, JList<E>> listOf( java.util.List<E> entries ) { return list( entries ); }

    /**
     * @param table The table which should be wrapped by the builder.
     * @param <T> The {@link JTable} type.
     * @return A builder instance for a new {@link JTable}.
     */
    public static <T extends JTable> UIForTable<T> of( T table ) {
        NullUtil.nullArgCheck(table, "table", JTable.class);
        return new UIForTable<>(table);
    }

    public static UIForTable<JTable> table() { return of(new Table()); }

    /**
     *  Use this to create a new {@link JTable} with a table model whose data can be represented based
     *  on a list of lists of entries.  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *  <p>
     *      <b>Please note that when the data of the provided data source changes (i.e. when the data source
     *      is a {@link java.util.List} which gets modified), the table model will not be updated automatically!
     *      Use {@link UIForTable#updateTableOn(sprouts.Event)} to bind an update {@link Event} to the table model.</b>
     *
     * @param dataFormat An enum which configures the modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableMapDataSource} returning a column major map based matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public static <E> UIForTable<JTable> table( ListData dataFormat, TableListDataSource<E> dataSource ) {
        NullUtil.nullArgCheck(dataFormat, "dataFormat", ListData.class);
        NullUtil.nullArgCheck(dataSource, "dataSource", TableListDataSource.class);
        return of((JTable) new Table()).with(dataFormat, dataSource);
    }

    /**
     *  Use this to create a new {@link JTable} with a table model whose data can be represented based
     *  on a map of column names to lists of table entries (basically a column major matrix).  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *  <p>
     *      <b>Please note that when the data of the provided data source changes (i.e. when the data source
     *      is a {@link Map} which gets modified), the table model will not be updated automatically!
     *      Use {@link UIForTable#updateTableOn(sprouts.Event)} to bind an update {@link Event} to the table model.</b>
     *
     * @param dataFormat An enum which configures the modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableMapDataSource} returning a column major map based matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public static <E> UIForTable<JTable> table( MapData dataFormat, TableMapDataSource<E> dataSource ) {
        NullUtil.nullArgCheck(dataFormat, "dataFormat", ListData.class);
        NullUtil.nullArgCheck(dataSource, "dataSource", TableMapDataSource.class);
        return of((JTable) new Table()).with(dataFormat, dataSource);
    }

    public static UIForTable<JTable> table( Buildable<BasicTableModel> tableModelBuildable ) {
        return of((JTable) new Table()).withModel(tableModelBuildable);
    }

    /**
     * @return A functional API for building a {@link javax.swing.table.TableModel}.
     */
    public static BasicTableModel.Builder tableModel() { return new BasicTableModel.Builder(); }

    public static Render.Builder<JTable, Object> renderTable() {
        return Render.forTable(Object.class, null).when(Object.class).asText(cell->cell.valueAsString().orElse(""));
    }

    /**
     *  Use this to build a list cell renderer for various item types without
     *  a meaningful common super-type (see {@link #renderList(Class)}).
     *  You would typically want to use this method to render generic types where the only
     *  common type is {@link Object}, yet you want to render the item
     *  in a specific way depending on their actual type.
     *  This is done like so:
     *  <pre>{@code
     *  UI.list(new Object[]{":-)", 42L, '§'})
     *  .withRenderer(
     *      UI.renderList()
     *      .when(String.class).asText( cell -> "String: "+cell.getValue() )
     *      .when(Character.class).asText( cell -> "Char: "+cell.getValue() )
     *      .when(Number.class).asText( cell -> "Number: "+cell.getValue() )
     *  );
     *  }</pre>
     *
     * @return A render builder exposing an API that allows you to
     *          configure how he passed item types should be rendered.
     */
    public static Render.Builder<JList<Object>, Object> renderList() {
        return Render.forList(Object.class, null).when(Object.class).asText(cell->cell.valueAsString().orElse(""));
    }

    /**
     *  Use this to build a list cell renderer for a specific item type and its subtype.
     *  You would typically want to use this method to render generic types like {@link Object}
     *  where you want to render the item in a specific way depending on its actual type.
     *  This is done like so:
     *  <pre>{@code
     *  UI.list(new Number[]{1f, 42L, 4.20d})
     *  .withRenderer(
     *      UI.renderList(Number.class)
     *      .when(Integer.class).asText( cell -> "Integer: "+cell.getValue() )
     *      .when(Long.class).asText( cell -> "Long: "+cell.getValue() )
     *      .when(Float.class).asText( cell -> "Float: "+cell.getValue() )
     *  );
     *  }</pre>
     *
     * @param commonType The common type of the items which should be rendered using a custom renderer.
     * @return A render builder exposing an API that allows you to
     *          configure how he passed item types should be rendered.
     * @param <T> The common super-type type of the items which should be rendered.
     */
    public static <T> Render.Builder<JList<T>, T> renderList( Class<T> commonType ) {
        return Render.forList(commonType, null).when(commonType).asText(cell->cell.valueAsString().orElse(""));
    }

    /**
     *  Use this to build a list cell renderer for a specific item type.
     *  What you would typically want to do is customize the text that should be displayed
     *  for a specific item type. <br>
     *  This is done like so:
     *  <pre>{@code
     *  UI.list("A", "B", "C")
     *  .withRenderer(
     *      UI.renderListItem(String.class)
     *      .asText(cell -> cell.getValue().toLowerCase())
     *  );
     *  }</pre>
     *
     * @param itemType The type of the items which should be rendered using a custom renderer.
     * @return A render builder exposing an API that allows you to
     *          configure how he passed item type should be rendered.
     * @param <T> The type of the items which should be rendered.
     */
    public static <T> Render.As<JList<T>, T, T> renderListItem( Class<T> itemType ) {
        return Render.forList(itemType, null).when(itemType);
    }

    /**
     *  Use this to create a generic combo box renderer for various item types without
     *  a meaningful common super-type (see {@link #renderCombo(Class)}).
     *  You would typically want to use this method to render generic types where the only
     *  common type is {@link Object}, yet you want to render the item
     *  in a specific way depending on their actual type.
     *  This is done like so:
     *  <pre>{@code
     *  UI.comboBox(new Object[]{":-)", 42L, '§'})
     *  .withRenderer(
     *      UI.renderCombo()
     *      .when(String.class).asText( cell -> "String: "+cell.getValue() )
     *      .when(Character.class).asText( cell -> "Char: "+cell.getValue() )
     *      .when(Number.class).asText( cell -> "Number: "+cell.getValue() )
     *  );
     *  }</pre>
     *
     * @return A render builder exposing an API that allows you to configure how he passed item types should be rendered.
     */
    public static Render.Builder<JComboBox<Object>, Object> renderCombo() {
        return Render.forCombo(Object.class, null).when(Object.class).asText(cell->cell.valueAsString().orElse(""));
    }

    /**
     *  Use this to create a combo box renderer for a specific item type and its subtype.
     *  You would typically want to use this method to render generic types like {@link Object}
     *  where you want to render the item in a specific way depending on its actual type.
     *  This is done like so:
     *  <pre>{@code
     *  UI.comboBox(new Number[]{1f, 42L, 4.20d})
     *  .withRenderer(
     *      UI.renderCombo(Number.class)
     *      .when(Integer.class).asText( cell -> "Integer: "+cell.getValue() )
     *      .when(Long.class).asText( cell -> "Long: "+cell.getValue() )
     *      .when(Float.class).asText( cell -> "Float: "+cell.getValue() )
     *  );
     *  }</pre>
     *
     * @param commonType The common type of the items which should be rendered using a custom renderer.
     * @return A render builder exposing an API that allows you to configure how he passed item types should be rendered.
     * @param <T> The common super-type type of the items which should be rendered.
     */
    public static <T> Render.Builder<JComboBox<T>, T> renderCombo( Class<T> commonType ) {
        return Render.forCombo(commonType, null).when(commonType).asText(cell->cell.valueAsString().orElse(""));
    }

    /**
     *  Use this to build a combo box cell renderer for a specific item type.
     *  What you would typically want to do is customize the text that should be displayed
     *  for a specific item type. <br>
     *  This is done like so:
     *  <pre>{@code
     *  UI.comboBox(Size.LARGE, Size.MEDIUM, Size.SMALL)
     *  .withRenderer(
     *      UI.renderComboItem(Size.class)
     *      .asText(cell -> cell.getValue().name().toLowerCase())
     *  );
     *  }</pre>
     *
     * @param itemType The type of the items which should be rendered using a custom renderer.
     * @return A render builder exposing an API that allows you to
     *          configure how he passed item type should be rendered.
     * @param <T> The type of the items which should be rendered.
     */
    public static <T> Render.As<JComboBox<T>, T, T> renderComboItem( Class<T> itemType ) {
        return Render.forCombo(itemType, null).when(itemType);
    }

    /**
     * @param borderSupplier A lambda which provides a border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JTable, Object> renderTableWithBorder( Supplier<Border> borderSupplier ) {
        return Render.forTable(Object.class, borderSupplier).when(Object.class).as(cell->{});
    }

    /**
     * @param borderSupplier A lambda which provides a border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JList<Object>, Object> renderListWithBorder( Supplier<Border> borderSupplier ) {
        return Render.forList(Object.class, borderSupplier).when(Object.class).as(cell->{});
    }

    /**
     * @param borderSupplier A lambda which provides a border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JComboBox<Object>, Object> renderComboWithBorder( Supplier<Border> borderSupplier ) {
        return Render.forCombo(Object.class, borderSupplier).when(Object.class).as(cell->{});
    }

    /**
     * @param border A border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JTable, Object> renderTableWithBorder( Border border ) {
        return renderTableWithBorder(()->border);
    }

    /**
     * @param border A property holding a {@link Border} used for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JTable, Object> renderTableWithBorder( Val<Border> border ) {
        return renderTableWithBorder(border::orElseThrow);
    }

    /**
     * @param border A border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JList<Object>, Object> renderListWithBorder( Border border ) {
        return renderListWithBorder(()->border);
    }

    /**
     * @param border A property holding a {@link Border} used for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JList<Object>, Object> renderListWithBorder( Var<Border> border ) {
        return renderListWithBorder(border::orElseThrow);
    }

    /**
     * @param border A border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JComboBox<Object>, Object> renderComboWithBorder( Border border ) {
        return renderComboWithBorder(()->border);
    }

    /**
     * @param border A property holding a {@link Border} used for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JComboBox<Object>, Object> renderComboWithBorder( Val<Border> border ) {
        NullUtil.nullPropertyCheck(border, "border", "Null is not a valid border.");
        return renderComboWithBorder(border::orElseThrow);
    }

    /**
     *  Use this to create a builder for anything.
     *
     * @return A builder instance for the provided object, which enables fluent method chaining.
     */
    public static <T extends Component> UIForAnything<T> of( T component ) {
        NullUtil.nullArgCheck(component, "component", Component.class);
        return new UIForAnything<>(component);
    }

    /**
     * A convenience method for
     * <pre>{@code
     *      if ( !UI.thisIsUIThread() )
     *          SwingUtilities.invokeLater(runnable);
     *      else
     *          runnable.run();
     * }</pre>,
     * which causes <i>runnable.run()</i> to be executed asynchronously on the
     * AWT event dispatching thread if this current thread is not already
     * the AWT thread.
     * The 'invokeLater' execution will happen after all pending AWT events have been processed.
     * This method should be used when an application thread needs to update the GUI.
     *
     * @param runnable the instance of {@code Runnable}
     * @see #runNow
     */
    public static void run( Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        if ( !UI.thisIsUIThread() )
            SwingUtilities.invokeLater(runnable);
        else
            runnable.run();
    }

    /**
     * A convenience method for {@link SwingUtilities#invokeLater(Runnable)},
     * which causes <i>doRun.run()</i> to be executed asynchronously on the
     * AWT event dispatching thread.  This will happen after all
     * pending AWT events have been processed.  This method should
     * be used when an application thread needs to update the GUI.
     * In the following example the <code>invokeLater</code> call queues
     * the <code>Runnable</code> object <code>doHelloWorld</code>
     * on the event dispatching thread and
     * then prints a message.
     * <pre>{@code
     *  UI.run( () -> System.out.println("Hello World on " + Thread.currentThread()) );
     *  System.out.println("This might well be displayed before the other message.");
     * }</pre>
     * If invokeLater is called from the event dispatching thread --
     * for example, from a JButton's ActionListener -- the <i>doRun.run()</i> will
     * still be deferred until all pending events have been processed.
     * Note that if the <i>doRun.run()</i> throws an uncaught exception
     * the event dispatching thread will unwind (not the current thread).
     *
     * @param runnable the instance of {@code Runnable}
     * @see #runNow
     */
    public static void runLater( Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * Returns true if the current thread is an AWT event dispatching thread.
     * <p>
     * This method is just a cover for
     * <code>javax.swing.SwingUtilities.isEventDispatchThread()</code>
     * and indirectly also for
     * <code>java.awt.EventQueue.isDispatchThread()</code>.
     *
     * @return true if the current thread is an AWT event dispatching thread
     */
    public static boolean thisIsUIThread() { return SwingUtilities.isEventDispatchThread(); }

    /**
     * A convenience method for {@link SwingUtilities#invokeAndWait(Runnable)},
     * causes <code>doRun.run()</code> to be executed synchronously on the
     * AWT event dispatching thread.  This call blocks until
     * all pending AWT events have been processed and (then)
     * <code>doRun.run()</code> returns. This method should
     * be used when an application thread needs to update the GUI.
     * It shouldn't be called from the event dispatching thread.
     * Here's an example that creates a new application thread
     * that uses <code>invokeAndWait</code> to print a string from the event
     * dispatching thread and then, when that's finished, print
     * a string from the application thread.
     * <pre>{@code
     *     var appThread = new Thread(() -> {
     *             try {
     *                 UI.runNow(() -> {
     *                    System.out.println("Hello World on " + Thread.currentThread());
     *                 });
     *             }
     *             catch (Exception e) {
     *                 e.printStackTrace();
     *             }
     *             System.out.println("Finished on " + Thread.currentThread());
     *         });
     *
     *     appThread.start();
     * }</pre>
     * Note that if the <code>Runnable.run</code> method throws an
     * uncaught exception
     * (on the event dispatching thread) it's caught and rethrown, as
     * an <code>InvocationTargetException</code>, on the caller's thread.
     *
     * @param runnable the instance of {@code Runnable}
     * @exception  InterruptedException if we're interrupted while waiting for
     *             the event dispatching thread to finish executing
     *             <code>doRun.run()</code>
     * @exception  InvocationTargetException  if an exception is thrown
     *             while running <code>doRun</code>
     *
     * @see #run
     */
    public static void runNow( Runnable runnable ) throws InterruptedException, InvocationTargetException {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        SwingUtilities.invokeAndWait(runnable);
    }

    /**
     * A convenience method for {@link SwingUtilities#invokeAndWait(Runnable)},
     * where the runnable is a lambda expression that has a return value.
     * This causes the {@link Supplier} to be executed synchronously on the
     * AWT event dispatching thread.  This call blocks until
     * all pending AWT events have been processed and (then)
     * the {@link Supplier} returns. This method should
     * be used when an application thread needs to update the GUI a
     * get a return value from the GUI.
     * It shouldn't be called from the event dispatching thread.
     * Here's an example that creates a new application thread
     * that uses <code>runAndGet(..)</code> to access the state of a
     * {@link javax.swing.JCheckBox} from the event dispatching thread
     * and then, when that's finished, print the state from the application thread.
     * <pre>{@code
     *     JCheckBox checkBox = new JCheckBox("Hello World");
     *     var appThread = new Thread(()->{
     *            try {
     *                boolean state = UI.runAndGet(() -> checkBox.isSelected());
     *                System.out.println("CheckBox state is " + state);
     *            }
     *            catch (Exception e) {
     *                e.printStackTrace();
     *            }
     *            System.out.println("Finished on " + Thread.currentThread());
     *        });
     *     appThread.start();
     * }</pre>
     *
     */
    public static <T> T runAndGet( Supplier<T> supplier ) throws InterruptedException, InvocationTargetException {
        NullUtil.nullArgCheck(supplier, "callable", Supplier.class);
        T[] ref = (T[]) new Object[1];
        runNow( () -> ref[0] = supplier.get() );
        return ref[0];
    }

    /**
     *  Use this to synchronize with the UI thread from a non-UI thread.
     *  After calling this method, the current thread will be blocked
     *  until the UI thread has finished executing all of its pending events.
     *  This method should only be called from the application thread
     *  and not from the UI thread.
     *
     * @throws InterruptedException if the current thread is interrupted
     * @throws InvocationTargetException if the UI thread throws an exception
     */
    public static void sync() throws InterruptedException, InvocationTargetException {
        runNow( () -> {/*
            This is a no-op, but it forces the event dispatching thread to
            process all pending events before returning.
            So when we reach this point, we know that all pending events
            have been processed.
        */});
    }

    /**
     *  Exposes an API for scheduling periodic animation updates.
     *  This is a convenience method for {@link Animate#on(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.schedule( 100, TimeUnit.MILLISECONDS )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setValue( it.progress() );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     */
    public static Animate schedule( long duration, TimeUnit unit ) {
        Objects.requireNonNull(unit, "unit");
        return Animate.on( LifeTime.of(duration, unit) );
    }

    /**
     *  Exposes an API for scheduling periodic animation updates.
     *  This is a convenience method for {@link Animate#on(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.schedule( 0.1, TimeUnit.MINUTES )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     */
    public static Animate schedule( double duration, TimeUnit unit ) {
        return Animate.on( LifeTime.of(duration, unit) );
    }

    /**
     *  Shows a conformation dialog with the given message.
     * @param message the message to show
     * @return true if the user clicked "Yes", false otherwise
     */
    public static boolean confirm( String message ) { return confirm("Confirm", message); }

    /**
     * Shows a conformation dialog with the given message.
     *
     * @param title   the title of the dialog
     * @param message the message to show
     * @return true if the user clicked "Yes", false otherwise
     */
    public static boolean confirm( String title, String message ) { return confirm(title, message, null ); }

    /**
     * Shows a conformation dialog with the given message.
     *
     * @param title   the title of the dialog
     * @param message the message to show
     * @param icon    the icon to show
     * @return true if the user clicked "Yes", false otherwise
     */
    public static boolean confirm( String title, String message, Icon icon ) {
        Objects.requireNonNull( message );
        Objects.requireNonNull( title );
        return JOptionPane.showConfirmDialog( null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon ) == JOptionPane.YES_OPTION;
    }

    /**
     *  Shows an error dialog with the given message.
     * @param message The error message to show in the dialog.
     */
    public static void error( String message ) { error("Error", message); }

    /**
     * Shows an error dialog with the given message and dialog title.
     *
     * @param title   The title of the dialog.
     * @param message The error message to show in the dialog.
     */
    public static void error( String title, String message ) { error(title, message, null ); }

    /**
     * Shows an error dialog with the given message, dialog title and icon.
     *
     * @param title   The title of the dialog.
     * @param message The error message to show in the dialog.
     * @param icon    The icon to show in the dialog.
     */
    public static void error( String title, String message, Icon icon ) {
        Objects.requireNonNull( message );
        Objects.requireNonNull( title );
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE, icon );
    }

    /**
     *  Shows an info dialog with the given message.
     * @param message The message to show in the dialog.
     */
    public static void info( String message ) { info("Info", message); }

    /**
     * Shows an info dialog with the given message and dialog title.
     *
     * @param title   The title of the dialog.
     * @param message The message to show in the dialog.
     */
    public static void info( String title, String message ) {
        Objects.requireNonNull( message );
        Objects.requireNonNull( title );
        info(title, message, null );
    }

    /**
     * Shows an info dialog with the given message, dialog title and icon.
     *
     * @param title   The title of the dialog.
     * @param message The message to show in the dialog.
     * @param icon    The icon to show in the dialog.
     */
    public static void info( String title, String message, Icon icon ) {
        Objects.requireNonNull( message );
        Objects.requireNonNull( title );
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.INFORMATION_MESSAGE, icon );
    }

    /**
     *  Shows a warning dialog with the given message.
     * @param message The warning message to show in the dialog.
     */
     public static void warn( String message ) { warn("Warning", message); }

    /**
     * Shows a warning dialog with the given message and dialog title.
     *
     * @param title   The title of the dialog.
     * @param message The warning message to show in the dialog.
     */
    public static void warn( String title, String message ) { warn(title, message, null ); }

    /**
     * Shows a warning dialog with the given message, dialog title and icon.
     *
     * @param title   The title of the dialog.
     * @param message The warning message to show in the dialog.
     * @param icon    The icon to show in the dialog.
     */
    public static void warn( String title, String message, Icon icon ) {
        Objects.requireNonNull( message );
        Objects.requireNonNull( title );
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.WARNING_MESSAGE, icon );
    }

    /**
     *  Shows a dialog where the user can select a value from a list of options
     *  based on the enum type implicitly defined by the given enum based property.
     *  The selected value will be stored in said property after the user has
     *  selected a value.
     *
     * @param message The message to show in the dialog.
     * @param selected The enum based property to store the selected value in.
     */
    public static <E extends Enum<E>> void select( String message, Var<E> selected ) {
        select("Select", message, selected );
    }

    /**
     * Shows a dialog where the user can select a value from a list of options
     * based on the enum type implicitly defined by the given enum based property.
     * The selected value will be stored in said property after the user has
     * selected a value.
     *
     * @param title    The title of the dialog.
     * @param message  The message to show in the dialog.
     * @param selected The enum based property to store the selected value in.
     */
    public static <E extends Enum<E>> void select( String title, String message, Var<E> selected ) {
        select(title, message, null, selected );
    }

    /**
     * Shows a dialog where the user can select a value from a list of options
     * based on the enum type implicitly defined by the given enum based property.
     * The selected value will be stored in said property after the user has
     * selected a value.
     *
     * @param title    The title of the dialog.
     * @param message  The message to show in the dialog.
     * @param icon     The icon to show in the dialog.
     * @param selected The enum based property to store the selected value in.
     */
    public static <E extends Enum<E>> void select( String title, String message, Icon icon, Var<E> selected ) {
        Objects.requireNonNull( message );
        Objects.requireNonNull( title );
        Objects.requireNonNull( selected );
        E[] options = selected.type().getEnumConstants();
        String[] asStr = new String[options.length];
        for ( int i = 0; i < options.length; i++ )
            asStr[i] = options[i].toString();

        int selectedIdx = JOptionPane.showOptionDialog( null, message, title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, icon, asStr, asStr[0] );
        selected.act( options[selectedIdx] );
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen.
     *
     * @param component The component to show in the window.
     */
    public static void show( Component component ) {
        Objects.requireNonNull( component );
        new UI.TestWindow( "", f -> component );
    }

    /**
     *  Use this to quickly launch a UI component in a titled {@link JFrame} window
     *  at the center of the screen.
     *
     * @param title The title of the window.
     * @param component The component to show in the window.
     */
    public static void show( String title, Component component ) {
        Objects.requireNonNull( component );
        new UI.TestWindow( title, f -> component );
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen.
     *
     * @param ui The Swing-Tree UI to show in the window.
     * @param <C> The type of the component to show in the window.
     */
    public static <C extends JComponent> void show( UIForAnySwing<?, C> ui ) {
        new UI.TestWindow( "", f -> ui.getComponent() );
    }

    /**
     *  Use this to quickly launch a UI component in a titled {@link JFrame} window
     *  at the center of the screen.
     *
     * @param title The title of the window.
     * @param ui The Swing-Tree UI to show in the window.
     * @param <C> The type of the component to show in the window.
     */
    public static <C extends JComponent> void show( String title, UIForAnySwing<?, C> ui ) {
        new UI.TestWindow( title, f -> ui.getComponent() );
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen using a function receiving the {@link JFrame}
     *  and returning the component to be shown.
     *
     * @param uiSupplier The component supplier which receives the current {@link JFrame}
     *                   and returns the component to be shown.
     */
    public static void show( Function<JFrame, Component> uiSupplier ) {
        Objects.requireNonNull( uiSupplier );
        new UI.TestWindow( "", frame -> uiSupplier.apply(frame) );
    }

    /**
     *  Use this to quickly launch a UI component in a titled {@link JFrame} window
     *  at the center of the screen using a function receiving the {@link JFrame}
     *  and returning the component to be shown.
     *
     * @param title The title of the window.
     * @param uiSupplier The component supplier which receives the current {@link JFrame}
     *                   and returns the component to be shown.
     */
    public static void show( String title, Function<JFrame, Component> uiSupplier ) {
        Objects.requireNonNull( uiSupplier );
        new UI.TestWindow( title, frame -> uiSupplier.apply(frame) );
    }

    /**
     *  Use this to quickly launch a UI component with a custom event processor
     *  in {@link JFrame} window at the center of the screen.
     *
     * @param eventProcessor the event processor to use for the UI built inside the {@link Supplier} lambda.
     * @param uiSupplier The component supplier which builds the UI and supplies the component to be shown.
     */
    public static void showUsing( EventProcessor eventProcessor, Function<JFrame, Component> uiSupplier ) {
        Objects.requireNonNull( eventProcessor );
        Objects.requireNonNull( uiSupplier );
        show(frame -> use(eventProcessor, () -> uiSupplier.apply(frame)));
    }

    /**
     *  Use this to quickly launch a UI component with a custom event processor
     *  in a titled {@link JFrame} window at the center of the screen.
     *
     * @param eventProcessor the event processor to use for the UI built inside the {@link Supplier} lambda.
     * @param title The title of the window.
     * @param uiSupplier The component supplier which builds the UI and supplies the component to be shown.
     */
    public static void showUsing( EventProcessor eventProcessor, String title, Function<JFrame, Component> uiSupplier ) {
        Objects.requireNonNull( eventProcessor );
        Objects.requireNonNull( uiSupplier );
        show(title, frame -> use(eventProcessor, () -> uiSupplier.apply(frame)));
    }

    /**
     *  Use this to quickly create and inspect a test window for a UI component.
     */
    private static class TestWindow
    {
        private final JFrame frame;
        private final Component component;

        private TestWindow( String title, Function<JFrame, Component> uiSupplier ) {
            Objects.requireNonNull( title );
            Objects.requireNonNull( uiSupplier );
            this.frame = new JFrame();
            if ( !title.isEmpty() ) this.frame.setTitle(title);
            frame.setLocationRelativeTo(null); // Initial centering!
            this.component = uiSupplier.apply(frame);
            frame.add(component);
            frame.pack(); // Otherwise some components resize strangely or are not shown at all...
            // Make sure that the window is centered on the screen again but with the component:
            frame.setLocationRelativeTo(null);
            // We set the size to fit the component:
            _determineSize();
            frame.setVisible(true);
        }
        private void _determineSize() {
            Dimension size = frame.getSize();
            if ( size == null ) // The frame has no size! It is best to set the size to the preferred size of the component:
                size = component.getPreferredSize();

            if ( size == null ) // The component has no preferred size! It is best to set the size to the minimum size of the component:
                size = component.getMinimumSize();

            if ( size == null ) // The component has no minimum size! Let's just look up the size of the component:
                size = component.getSize();

            frame.setSize(size);
        }
    }


    /*
        The following method and subsequent classes are used to smoothly render
        custom graphics on top of Swing components without requiring
        the user to override the paint method of the component.
        This is especially important to allow for declarative UI.
    */

    private static <C extends JComponent> void _renderComponent( C comp, Graphics g ) {
        ComponentExtension.from(comp).renderBaseStyle( g );
    }
    private static <C extends JComponent> void _renderForeground( C comp, Graphics g ) {
        ComponentExtension.from(comp).renderForegroundStyle( (Graphics2D) g );
    }

    /** {inheritDoc} */
    public static class Panel extends JPanel {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class Label extends JLabel {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class TextField extends JTextField {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class TextArea extends JTextArea {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class CheckBox extends JCheckBox {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class Button extends JButton {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class ToggleButton extends JToggleButton {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class RadioButton extends JRadioButton {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class ComboBox<E> extends JComboBox<E> {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class List<E> extends JList<E> {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class Table extends JTable {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class Slider extends JSlider {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class PopupMenu extends JPopupMenu {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class MenuItem extends JMenuItem {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class RadioButtonMenuItem extends JRadioButtonMenuItem {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
     public static class CheckBoxMenuItem extends JCheckBoxMenuItem {
         @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
         @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
     }
    /** {inheritDoc} */
    public static class Menu extends JMenu {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class MenuBar extends JMenuBar {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class ScrollPane extends JScrollPane {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class TabbedPane extends JTabbedPane {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class ToolBar extends JToolBar {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class ToolTip extends JToolTip {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class Tree extends JTree {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class Spinner extends JSpinner {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class SplitPane extends JSplitPane {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class PasswordField extends JPasswordField {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class ProgressBar extends JProgressBar {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class TextPane extends JTextPane {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
    /** {inheritDoc} */
    public static class EditorPane extends JEditorPane {
        @Override public void paint(Graphics g){ _renderComponent(this, g); super.paint(g); }
        @Override public void paintChildren(Graphics g) { super.paintChildren(g); _renderForeground(this, g); }
    }
}
