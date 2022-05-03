package com.globaltcad.swingtree;


import com.globaltcad.swingtree.api.UIAction;
import com.globaltcad.swingtree.delegates.SimpleDelegate;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;


/**
 *  A swing tree builder for any kind {@link JComponent} instance.
 *  This is the most basic builder type and therefore super-type for almost all other builders.
 *  This builder defines nested building of anything extending the {@link JComponent} class.
 *  <br><br>
 *
 * @param <I> The most basic concrete implementation of the {@link AbstractNestedBuilder}.
 * @param <C> The type parameter for the component type wrapped by an instance of this class.
 */
public abstract class UIForAbstractSwing<I, C extends JComponent> extends AbstractNestedBuilder<I, C, JComponent>
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UI.class);

    private boolean idAlreadySet = false;
    private boolean migAlreadySet = false;

    /**
     *  Instances of the BasicBuilder as well as its sub types always wrap
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
        this.component.setName(id);
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
        this.component.setVisible( isVisible );
        return (I) this;
    }

    /**
     *  Use this to enable or disable the wrapped UI component.
     *
     * @param isEnabled The truth value determining if the UI component should be enabled or not.
     * @return This very instance, which enables builder-style method chaining.
     */
    public I isEnabledIf(boolean isEnabled) {
        this.component.setEnabled( isEnabled );
        return (I) this;
    }

    /**
     * Adds {@link String} key/value "client property" pairs to the wrapped component.
     * <p>
     * The arguments will be passed to {@link JComponent#putClientProperty(Object, Object)}
     * provide access to
     * a small per-instance hashtable. Callers can use get/putClientProperty
     * to annotate components that were created by another module.
     * For example, a
     * layout manager might store per child constraints this way.
     *
     * @param key the new client property key which may be used for styles or layout managers.
     * @param value the new client property value.
     */
    public final I withProperty(String key, String value) {
        this.component.putClientProperty(key, value);
        return (I) this;
    }

    /**
     *  Use this to attach a border to the wrapped component.
     *
     * @param border The {@link Border} which should be set for the wrapped component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBorder(Border border) {
        this.component.setBorder( border );
        return (I) this;
    }

    /**
     *  Use this to conveniently set the cursor type which should be displayed
     *  when hovering over the UI component wrapped by this builder.
     *
     * @param type The {@link UI.Cursor} type defined by a simple enum exposed by this API.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withCursor(UI.Cursor type) {
        this.component.setCursor( new java.awt.Cursor( type.type ) );
        return (I) this;
    }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param constraints A string defining the mig layout.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout(String constraints) { return withLayout(constraints, null); }

    /**
     *  This creates a {@link MigLayout} for the component wrapped by this UI builder.
     *
     * @param constraints The constraints for the layout.
     * @param colConstrains The column layout for the {@link MigLayout} instance.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withLayout(String constraints, String colConstrains) {
        return withLayout(constraints, colConstrains, null);
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
        this.component.setLayout(migLayout);
        migAlreadySet = true;
        return (I) this;
    }

    /**
     *  Use this to set a helpful tool tip text for this UI component.
     *  The tool tip text will be displayed when the mouse hovers on the
     *  UI component for some time.
     *
     * @param tooltip The tool tip text which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withTooltip(String tooltip) {
        this.component.setToolTipText(tooltip);
        return (I) this;
    }

    /**
     *  Use this to set the background color of the UI component
     *  wrapped by this builder.
     *
     * @param color The background color which should be set for the UI component.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I withBackground(Color color) {
        LogUtil.nullArgCheck(color, "color", Color.class);
        this.component.setBackground(color);
        return (I) this;
    }

    /**
     *  Set the color of this {@link JComponent}. (This is usually the font color for components displaying text)
     *
     * @param color The color of the foreground (usually text).
     * @return This very builder to allow for method chaining.
     */
    public I withForeground(Color color) {
        this.component.setForeground(color);
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
        this.component.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    /**
     * The provided lambda will be invoked when the component's size changes.
     */
    public final I onResize(UIAction<SimpleDelegate<C, ComponentEvent>> onResize) {
        LogUtil.nullArgCheck(onResize, "onResize", UIAction.class);
        this.component.addComponentListener( new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { onResize.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    public final I onMoved(UIAction<SimpleDelegate<C, ComponentEvent>> onMoved) {
        LogUtil.nullArgCheck(onMoved, "onMoved", UIAction.class);
        this.component.addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) { onMoved.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    public final I onShown(UIAction<SimpleDelegate<C, ComponentEvent>> onShown) {
        LogUtil.nullArgCheck(onShown, "onShown", UIAction.class);
        this.component.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { onShown.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    public final I onHidden(UIAction<SimpleDelegate<C, ComponentEvent>> onHidden) {
        LogUtil.nullArgCheck(onHidden, "onHidden", UIAction.class);
        this.component.addComponentListener(new ComponentAdapter() {
            @Override public void componentHidden(ComponentEvent e) { onHidden.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    public final I onFocusGained(UIAction<SimpleDelegate<C, ComponentEvent>> onFocus) {
        LogUtil.nullArgCheck(onFocus, "onFocus", UIAction.class);
        this.component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { onFocus.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    public final I onFocusLost(UIAction<SimpleDelegate<C, ComponentEvent>> onFocus) {
        LogUtil.nullArgCheck(onFocus, "onFocus", UIAction.class);
        this.component.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { onFocus.accept(new SimpleDelegate<>(component, e, ()->getSiblinghood())); }
        });
        return (I) this;
    }

    @Override
    protected void _add(JComponent component, Object conf) {
        LogUtil.nullArgCheck(component, "component", JComponent.class);
        if ( conf == null )
            this.component.add(component);
        else
            this.component.add(component, conf);
    }

    /**
     *  Swing UIs are made up of nested {@link JComponent} instances.
     *  This method enables support for nested building as well as the ability to
     *  pass additional layout information the added UI component.
     *  <br><br>
     *
     * @param configuration The additional layout information which should be passed to the UI tree.
     * @param component A {@link JComponent} instance which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add(String configuration, C component) {
        LogUtil.nullArgCheck(configuration, "configuration", Object.class);
        LogUtil.nullArgCheck(component, "component", Object.class);
        this.add(configuration, UI.of(component));
        return (I) this;
    }

    /**
     *  Swing UIs are made up of nested {@link JComponent} instances.
     *  This method enables support for nested building as well as the ability to
     *  pass additional layout information the added UI component.
     *  <br><br>
     *
     * @param constraints The additional mig-layout information which should be passed to the UI tree.
     * @param builder A builder for another {@link JComponent} instance which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final <T extends JComponent> I add(String constraints, UIForAbstractSwing<?, T> builder) {
        return (I) this.add(constraints, new UIForAbstractSwing[]{builder});
    }

    @SafeVarargs
    public final <B extends UIForAbstractSwing<?, ?>> I add(String conf, B... builders) {
        LayoutManager layout = this.component.getLayout();
        if ( isBorderLayout(conf) && !(layout instanceof BorderLayout) ) {
            if ( layout instanceof MigLayout )
                log.warn("Layout ambiguity detected! Border layout constraint cannot be added to 'MigLayout'.");
            this.component.setLayout(new BorderLayout()); // The UI Maker tries to fill in the blanks!
        }
        for( UIForAbstractSwing<?, ?> b : builders )
            _doAdd(b, conf);
        return (I) this;
    }

    private static boolean isBorderLayout(Object o) {
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
