package com.globaltcad.swingtree;


import com.globaltcad.swingtree.api.Peeker;
import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.input.Keyboard;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;


/**
 *  A swing tree builder node for any kind {@link JComponent} instance.
 *  This is the most generic builder type and therefore abstract super-type for almost all other builders.
 *  This builder defines nested building for anything extending the {@link JComponent} class.
 *  <br><br>
 *
 * @param <I> The concrete extension of the {@link AbstractNestedBuilder}.
 * @param <C> The type parameter for the component type wrapped by an instance of this class.
 */
public abstract class UIForAbstractSwing<I, C extends JComponent> extends AbstractNestedBuilder<I, C, JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UI.class);

    private final static Map<JComponent, java.util.List<Timer>> timers = new WeakHashMap<>(); // We attach garbage collectable timers to components this way!

    private boolean idAlreadySet = false; // The id translates to the 'name' property of swing components.
    private boolean migAlreadySet = false;

    /**
     *  Extensions of the {@link  UIForAbstractSwing} always wrap
     *  a single component for which they are responsible.
     *
     * @param component The JComponent type which will be wrapped by this builder node.
     */
    public UIForAbstractSwing(C component) { super(component); }

    /**
     *  This method exposes a concise way to set an identifier for the component
     *  wrapped by this {@link UI}!
     *  In essence this is simply a delegate for the {@link JComponent#setName(String)} method
     *  to make it more expressive and widely recognized what is meant
     *  ("id" is shorter and makes more sense than "name" which could be confused with "title").
     *
     * @param id The identifier for this {@link JComponent} which will
     *           simply translate to {@link JComponent#setName(String)}
     *
     * @return The JComponent type which will be wrapped by this builder node.
     */
    public final I id(String id) {
        if ( idAlreadySet )
            throw new IllegalArgumentException("The id has already been specified for this component!");
        _component.setName(id);
        idAlreadySet = true;
        return (I) this;
    }

    /**
     *  Use this to make the wrapped UI component visible or invisible.
     *
     * @param isVisible The truth value determining if the UI component should be visible or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I isVisibleIf(boolean isVisible) {
        _component.setVisible( isVisible );
        return (I) this;
    }

    /**
     *  Use this to enable or disable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I isEnabledIf(boolean isEnabled) {
        _component.setEnabled( isEnabled );
        return (I) this;
    }

    /**
     * Adds {@link String} key/value "client property" pairs to the wrapped component.
     * <p>
     * The arguments will be passed to {@link JComponent#putClientProperty(Object, Object)}
     * which accesses
     * a small per-instance hashtable. Callers can use get/putClientProperty
     * to annotate components that were created by another module.
     * For example, a
     * layout manager might store per child constraints this way. <br>
     * This is in essence a more convenient way than the alternative usage pattern involving
     * the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     * <pre>{@code
     *     UI.button()
     *         .peek( button -> button.withProperty("key", "value") );
     * }</pre>
     *
     * @param key the new client property key which may be used for styles or layout managers.
     * @param value the new client property value.
     */
    public final I withProperty(String key, String value) {
        _component.putClientProperty(key, value);
        return (I) this;
    }

    /**
     *  Use this to attach a border to the wrapped component.
     *
     * @param border The {@link Border} which should be set for the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I with(Border border) {
        _component.setBorder( border );
        return (I) this;
    }

    /**
     *  Use this to conveniently set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder.
     *
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I with(UI.Cursor type) {
        _component.setCursor( new java.awt.Cursor( type.type ) );
        return (I) this;
    }

    /**
     *  Use this to set the {@link LayoutManager} of the component wrapped by this builder. <br>
     *  This is in essence a more convenient way than the alternative usage pattern involving
     *  the {@link #peek(Peeker)} method to peek into the builder's component like so: <br>
     *  <pre>{@code
     *      UI.panel()
     *          .peek( panel -> panel.setLayout(new FavouriteLayoutManager()) );
     *  }</pre>
     *
     * @param layout The {@link LayoutManager} which should be supplied to the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I with(LayoutManager layout) {
        if ( migAlreadySet )
            throw new IllegalArgumentException("The mig layout has already been specified for this component!");
        _component.setLayout(layout);
        return (I) this;
    }

    /**
     *  Passes the provided string to the layout manager of the wrapped component.
     *  By default, a {@link MigLayout} is used for the component wrapped by this UI builder.
     *
     * @param attr A string defining the layout (usually mig layout).
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout(String attr) { return withLayout(attr, null); }

    /**
     *  Passes the provided string to the {@link MigLayout} manager of the wrapped component.
     *
     * @param attr A string defining the mig layout.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout(Attr attr) { return withLayout(attr.toString(), null); }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param attr The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout(String attr, String colConstrains) {
        return withLayout(attr, colConstrains, null);
    }

    /**
     * @param attr The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout(Attr attr, String colConstrains) {
        return withLayout(attr.toString(), colConstrains, null);
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param constraints The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout(String constraints, String colConstrains, String rowConstraints) {
        if ( migAlreadySet )
            throw new IllegalArgumentException("The mig layout has already been specified for this component!");

        // We make sure the default hidemode is 2 instead of 3 (which sucks because it takes up too much space)
        if ( constraints == null )
            constraints = "hidemode 2";
        else if ( !constraints.contains("hidemode") )
            constraints += ", hidemode 2";

        MigLayout migLayout = new MigLayout(constraints, colConstrains, rowConstraints);
        _component.setLayout(migLayout);
        migAlreadySet = true;
        return (I) this;
    }

    /**
     *  Use this to set a helpful tool tip text for this UI component.
     *  The tool tip text will be displayed when the mouse hovers on the
     *  UI component for some time. <br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.button("Click Me")
     *          .peek( button -> button.setToolTipText("Can be clicked!") );
     *  }</pre>
     *
     * @param tooltip The tool tip text which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withTooltip(String tooltip) {
        _component.setToolTipText(tooltip);
        return (I) this;
    }

    /**
     *  Use this to set the background color of the UI component
     *  wrapped by this builder.<br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.label("Something")
     *          .peek( label -> label.setBackground(Color.CYAN) );
     *  }</pre>
     *
     *
     * @param color The background color which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackground(Color color) {
        LogUtil.nullArgCheck(color, "color", Color.class);
        _component.setBackground(color);
        return (I) this;
    }

    /**
     *  Set the color of this {@link JComponent}. (This is usually the font color for components displaying text) <br>
     *  This is in essence a convenience method, which avoid having to expose the underlying component
     *  through the {@link #peek(Peeker)} method like so: <br>
     *  <pre>{@code
     *      UI.label("Something")
     *          .peek( label -> label.setForeground(Color.GRAY) );
     *  }</pre>
     *
     * @param color The color of the foreground (usually text).
     * @return This very builder to allow for method chaining.
     */
    public I withForeground(Color color) {
        _component.setForeground(color);
        return (I) this;
    }

    /**
     *  Set the minimum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param size The minimum {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     */
    public I withMinimumSize(Dimension size) {
        _component.setMinimumSize(size);
        return (I) this;
    }

    /**
     *  Set the minimum width and heigh ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component. <br>
     * @param width The minimum width of the component.
     * @param height The minimum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public I withMinimumSize(int width, int height) {
        _component.setMinimumSize(new Dimension(width, height));
        return (I) this;
    }

    /**
     *  Use this to only set the minimum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param width The minimum width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public I withMinimumWidth(int width) {
        _component.setMinimumSize(new Dimension(width, _component.getMinimumSize().height));
        return (I) this;
    }

    /**
     *  Use this to only set the minimum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMinimumSize(Dimension)} on the underlying component for you. <br>
     * @param height The minimum height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public I withMinimumHeight(int height) {
        _component.setMinimumSize(new Dimension(_component.getMinimumSize().width, height));
        return (I) this;
    }

    /**
     *  Set the maximum {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param size The maximum {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     */
    public I withMaximumSize(Dimension size) {
        _component.setMaximumSize(size);
        return (I) this;
    }

    /**
     *  Set the maximum width and height ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component. <br>
     * @param width The maximum width of the component.
     * @param height The maximum height of the component.
     * @return This very builder to allow for method chaining.
     */
    public I withMaximumSize(int width, int height) {
        _component.setMaximumSize(new Dimension(width, height));
        return (I) this;
    }

    /**
     *  Use this to only set the maximum width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param width The maximum width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public I withMaximumWidth(int width) {
        _component.setMaximumSize(new Dimension(width, _component.getMaximumSize().height));
        return (I) this;
    }

    /**
     *  Use this to only set the maximum height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setMaximumSize(Dimension)} on the underlying component for you. <br>
     * @param height The maximum height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public I withMaximumHeight(int height) {
        _component.setMaximumSize(new Dimension(_component.getMaximumSize().width, height));
        return (I) this;
    }

    /**
     *  Set the preferred {@link Dimension} of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param size The preferred {@link Dimension} of the component.
     * @return This very builder to allow for method chaining.
     */
    public I withPreferredSize(Dimension size) {
        _component.setPreferredSize(size);
        return (I) this;
    }

    /**
     *  Set the preferred width and height ({@link Dimension}) of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component. <br>
     * @param width The preferred width of the component.
     * @param height The preferred height of the component.
     * @return This very builder to allow for method chaining.
     */
    public I withPreferredSize(int width, int height) {
        _component.setPreferredSize(new Dimension(width, height));
        return (I) this;
    }

    /**
     *  Use this to only set the preferred width of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param width The preferred width which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public I withPreferredWidth(int width) {
        _component.setPreferredSize(new Dimension(width, _component.getPreferredSize().height));
        return (I) this;
    }

    /**
     *  Use this to only set the preferred height of this {@link JComponent}. <br>
     *  This calls {@link JComponent#setPreferredSize(Dimension)} on the underlying component for you. <br>
     * @param height The preferred height which should be set for the underlying component.
     * @return This very builder to allow for method chaining.
     */
    public I withPreferredHeight(int height) {
        _component.setPreferredSize(new Dimension(_component.getPreferredSize().width, height));
        return (I) this;
    }

    /**
     *  Use this to register and catch generic {@link MouseListener} based mouse click events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onClick The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseClick(UIAction<SimpleDelegate<C, MouseEvent>> onClick) {
        LogUtil.nullArgCheck(onClick, "onClick", UIAction.class);
        _component.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     *  The provided lambda will be invoked when the component's size changes.
     *  This will internally translate to a {@link ComponentListener} implementation.
     *
     * @param onResize The resize action which will be called when the underlying component changes size.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onResize(UIAction<SimpleDelegate<C, ComponentEvent>> onResize) {
        LogUtil.nullArgCheck(onResize, "onResize", UIAction.class);
        _component.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { onResize.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     *  The provided lambda will be invoked when the component was moved.
     *  This will internally translate to a {@link ComponentListener} implementation.
     *
     * @param onMoved The action lambda which will be executed once the component was moved / its position canged.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMoved(UIAction<SimpleDelegate<C, ComponentEvent>> onMoved) {
        LogUtil.nullArgCheck(onMoved, "onMoved", UIAction.class);
        _component.addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) { onMoved.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     *  Adds the supplied {@link UIAction} wrapped in a {@link ComponentListener}
     *  to the component, to receive those component events where the wrapped component becomes visible.
     *
     * @param onShown The {@link UIAction} which gets invoked when the component has been made visible.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onShown(UIAction<SimpleDelegate<C, ComponentEvent>> onShown) {
        LogUtil.nullArgCheck(onShown, "onShown", UIAction.class);
        _component.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { onShown.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     *  Adds the supplied {@link UIAction} wrapped in a {@link ComponentListener}
     *  to the component, to receive those component events where the wrapped component becomes invisible.
     *
     * @param onHidden The {@link UIAction} which gets invoked when the component has been made invisible.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onHidden(UIAction<SimpleDelegate<C, ComponentEvent>> onHidden) {
        LogUtil.nullArgCheck(onHidden, "onHidden", UIAction.class);
        _component.addComponentListener(new ComponentAdapter() {
            @Override public void componentHidden(ComponentEvent e) { onHidden.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     * Adds the supplied {@link UIAction} wrapped in a {@link FocusListener}
     * to the component, to receive those focus events where the wrapped component gains input focus.
     *
     * @param onFocus The {@link UIAction} which should be executed once the input focus was gained on the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onFocusGained(UIAction<SimpleDelegate<C, ComponentEvent>> onFocus) {
        LogUtil.nullArgCheck(onFocus, "onFocus", UIAction.class);
        _component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { onFocus.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     * Adds the supplied {@link UIAction} wrapped in a focus listener
     * to receive those focus events where the wrapped component loses input focus.
     *
     * @param onFocus The {@link UIAction} which should be executed once the input focus was lost on the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onFocusLost(UIAction<SimpleDelegate<C, ComponentEvent>> onFocus) {
        LogUtil.nullArgCheck(onFocus, "onFocus", UIAction.class);
        _component.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { onFocus.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     * Adds the supplied {@link UIAction} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyPressed The {@link UIAction} which will be executed once the wrapped component received a key press.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onKeyPressed(UIAction<SimpleDelegate<C, KeyEvent>> onKeyPressed) {
        LogUtil.nullArgCheck(onKeyPressed, "onKeyPressed", UIAction.class);
        _component.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKeyPressed.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     * Adds the supplied {@link UIAction} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the wrapped component receives a particular
     * keyboard input matching the provided {@link com.globaltcad.swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link com.globaltcad.swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyPressed The {@link UIAction} which will be executed once the wrapped component received the targeted key press.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onPressed(Keyboard.Key key, UIAction<SimpleDelegate<C, KeyEvent>> onKeyPressed) {
        LogUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        LogUtil.nullArgCheck(onKeyPressed, "onKeyPressed", UIAction.class);
        _component.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed( KeyEvent e ) {
                if ( e.getKeyCode() == key.code )
                    onKeyPressed.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
            }
        });
        return (I) this;
    }

                             /**
     * Adds the supplied {@link UIAction} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyReleased The {@link UIAction} which will be executed once the wrapped component received a key release.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(UIAction)
     */
    public final I onKeyReleased(UIAction<SimpleDelegate<C, KeyEvent>> onKeyReleased) {
        LogUtil.nullArgCheck(onKeyReleased, "onKeyReleased", UIAction.class);
        _component.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { onKeyReleased.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     * Adds the supplied {@link UIAction} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the wrapped component receives a particular
     * keyboard input matching the provided {@link com.globaltcad.swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link com.globaltcad.swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyReleased The {@link UIAction} which will be executed once the wrapped component received the targeted key release.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(UIAction)
     * @see #onKeyReleased(UIAction)
     */
    public I onReleased(Keyboard.Key key, UIAction<SimpleDelegate<C, KeyEvent>> onKeyReleased) {
        LogUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        LogUtil.nullArgCheck(onKeyReleased, "onKeyReleased", UIAction.class);
        _component.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased( KeyEvent e ) {
                if ( e.getKeyCode() == key.code )
                    onKeyReleased.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
            }
        });
        return (I) this;
    }

    /**
     * Adds the supplied {@link UIAction} wrapped in a {@link KeyListener}
     * to the component, to receive key events triggered when the wrapped component receives keyboard input.
     * <br><br>
     * @param onKeyTyped The {@link UIAction} which will be executed once the wrapped component received a key typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(UIAction)
     * @see #onKeyReleased(UIAction)
     */
    public I onKeyTyped(UIAction<SimpleDelegate<C, KeyEvent>> onKeyTyped) {
        LogUtil.nullArgCheck(onKeyTyped, "onKeyTyped", UIAction.class);
        _component.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { onKeyTyped.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     * Adds the supplied {@link UIAction} wrapped in a {@link KeyListener} to the component,
     * to receive key events triggered when the wrapped component receives a particular
     * keyboard input matching the provided {@link com.globaltcad.swingtree.input.Keyboard.Key}.
     * <br><br>
     * @param key The {@link com.globaltcad.swingtree.input.Keyboard.Key} which should be matched to the key event.
     * @param onKeyTyped The {@link UIAction} which will be executed once the wrapped component received the targeted key typed.
     * @return This very instance, which enables builder-style method chaining.
     * @see #onKeyPressed(UIAction)
     * @see #onKeyReleased(UIAction)
     * @see #onKeyTyped(UIAction)
     */
    public I onTyped(Keyboard.Key key, UIAction<SimpleDelegate<C, KeyEvent>> onKeyTyped) {
        LogUtil.nullArgCheck(key, "key", Keyboard.Key.class);
        LogUtil.nullArgCheck(onKeyTyped, "onKeyTyped", UIAction.class);
        _component.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped( KeyEvent e ) {
                if ( e.getKeyCode() == key.code )
                    onKeyTyped.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood()));
            }
        });
        return (I) this;
    }

    /**
     *  Use this to register periodic update actions which should be called
     *  based on the provided {@code delay}! <br>
     *  The following example produces a label which will display the current date.
     *  <pre>{@code
     *      UI.label("")
     *          .doUpdates( 100, it -> it.getComponent().setText(new Date().toString()) )
     *  }</pre>
     *
     * @param delay The delay between calling the provided {@link UIAction}.
     * @param onUpdate The {@link UIAction} which should be called periodically.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I doUpdates(int delay, UIAction<SimpleDelegate<C, ActionEvent>> onUpdate) {
        LogUtil.nullArgCheck(onUpdate, "onUpdate", UIAction.class);
        Timer timer = new Timer(delay, e -> onUpdate.accept(new SimpleDelegate<>(_component, e, ()->getSiblinghood())));
        synchronized (timers) {
            timers.getOrDefault(_component, new ArrayList<>()).add(timer);
        }
        timer.start();
        return (I) this;
    }

    @Override
    protected void _add( JComponent component, Object conf ) {
        LogUtil.nullArgCheck(component, "component", JComponent.class);
        if ( conf == null )
            _component.add(component);
        else
            _component.add(component, conf);
    }

    /**
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add(UIForAbstractSwing<?, T> builder) {
        return (I) this.add(new AbstractNestedBuilder[]{builder});
    }

    /**
     *  Use this to nest builder nodes into this builder to effectively plug the wrapped {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument is expected to contain layout information for the layout manager of the wrapped {@link JComponent},
     *  through the {@link JComponent#add(Component, Object)} method.
     *  By default, the {@link MigLayout} is used.
     *  <br><br>
     *
     * @param attr The additional mig-layout information which should be passed to the UI tree.
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add(String attr, UIForAbstractSwing<?, T> builder) {
        return this.add(attr, new UIForAbstractSwing[]{builder});
    }

    /**
     *  Use this to nest builder nodes into this builder to effectively plug the wrapped {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument will be passed to the layout manager of the wrapped {@link JComponent},
     *  through the {@link JComponent#add(Component, Object)} method.
     *  By default, the {@link MigLayout} is used.
     *  <br><br>
     *
     * @param attr The mig-layout attribute.
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add(Attr attr, UIForAbstractSwing<?, T> builder) {
        return this.add(attr.toString(), new UIForAbstractSwing[]{builder});
    }

    /**
     *  Use this to nest builder types into this builder to effectively plug the wrapped {@link JComponent}s 
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument represents layout attributes/constraints which will
     *  be passed to the {@link LayoutManager} of the underlying {@link JComponent}.
     *  through the {@link JComponent#add(Component, Object)} method.
     *  <br><br>
     *
     * @param attr The additional mig-layout information which should be passed to the UI tree.
     * @param builders An array of builders for a corresponding number of {@link JComponent} 
     *                  type which ought to be added to the wrapped component type of this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <B extends UIForAbstractSwing<?, ?>> I add(String attr, B... builders) {
        LayoutManager layout = _component.getLayout();
        if ( _isBorderLayout(attr) && !(layout instanceof BorderLayout) ) {
            if ( layout instanceof MigLayout )
                log.warn("Layout ambiguity detected! Border layout constraint cannot be added to 'MigLayout'.");
            _component.setLayout(new BorderLayout()); // The UI Maker tries to fill in the blanks!
        }
        for ( UIForAbstractSwing<?, ?> b : builders ) _doAdd(b, attr);
        return (I) this;
    }

    /**
     *  Use this to nest builder types into this builder to effectively plug the wrapped {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument will be passed to the {@link LayoutManager}
     *  of the underlying {@link JComponent} to serve as layout constraints.
     *  through the {@link JComponent#add(Component, Object)} method.
     *  <br><br>
     *
     * @param attr The first mig-layout information which should be passed to the UI tree.
     * @param builders An array of builders for a corresponding number of {@link JComponent}
     *                  type which ought to be added to the wrapped component type of this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <B extends UIForAbstractSwing<?, ?>> I add(Attr attr, B... builders) {
        return this.add(attr.toString(), builders);
    }

    /**
     *  Use this to nest {@link JComponent} types into this builder to effectively plug the provided {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first argument represents layout attributes/constraints which will
     *  be applied to the subsequently provided {@link JComponent} types.
     *  <br><br>
     *
     * @param attr The additional layout information which should be passed to the UI tree.
     * @param components A {@link JComponent}s array which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <E extends JComponent> I add( String attr, E... components ) {
        LogUtil.nullArgCheck(attr, "conf", Object.class);
        LogUtil.nullArgCheck(components, "components", Object[].class);
        for( E component : components ) {
            LogUtil.nullArgCheck(component, "component", JComponent.class);
            this.add(attr, UI.of(component));
        }
        return (I) this;
    }

    /**
     *  Use this to nest {@link JComponent} types into this builder to effectively plug the provided {@link JComponent}s
     *  into the {@link JComponent} type wrapped by this builder instance.
     *  The first 2 arguments will be joined by a comma and passed to the {@link LayoutManager}
     *  of the underlying {@link JComponent} to serve as layout constraints.
     *  <br><br>
     *
     * @param attr The first layout information which should be passed to the UI tree.
     * @param components A {@link JComponent}s array which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    public final <E extends JComponent> I add( Attr attr, E... components ) {
        return this.add(attr.toString(), components);
    }

    private static boolean _isBorderLayout( Object o ) {
        return BorderLayout.CENTER.equals(o) ||
                BorderLayout.PAGE_START.equals(o) ||
                BorderLayout.PAGE_END.equals(o) ||
                BorderLayout.LINE_END.equals(o) ||
                BorderLayout.LINE_START.equals(o) ||
                BorderLayout.EAST.equals(o)  ||
                BorderLayout.WEST.equals(o)  ||
                BorderLayout.NORTH.equals(o) ||
                BorderLayout.SOUTH.equals(o);
    }
}
