package com.globaltcad.swingtree;


import com.globaltcad.swingtree.api.UIAction;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  The following is one of the most basic builder implementations inside this file.
 *  It enables nested building of anything extending the JComponent class (Swing components) and
 *  it also serves as a useful super class from which more specialized
 *  builder types can inherit.
 *  <br><br>
 *
 * @param <I> The concrete implementation of the {@link AbstractBuilder}.
 * @param <C> The type parameter for the component wrapped by an instance of this class.
 */
public class UIForSwing<I, C extends JComponent> extends AbstractNestedBuilder<I, C>
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
    public UIForSwing(C component) { super(component); }

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
     *  Use this to register and catch generic {@link MouseListener} based mouse click events on this UI component.
     *  This method adds the provided consumer lambda to
     *  an an{@link MouseListener} instance to the wrapped
     *  button component.
     *  <br><br>
     *
     * @param onClick The lambda instance which will be passed to the button component as {@link MouseListener}.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I onMouseClick(UIAction<C, MouseEvent> onClick) {
        LogUtil.nullArgCheck(onClick, "onClick", UIAction.class);
        this.component.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.accept(new EventContext<>(component, e)); }
        });
        return (I) this;
    }

    /**
     * The provided lambda will be invoked when the component's size changes.
     */
    public final I onResize(UIAction<C, ComponentEvent> onResize) {
        LogUtil.nullArgCheck(onResize, "onResize", UIAction.class);
        this.component.addComponentListener( new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { onResize.accept(new EventContext<>(component, e)); }
        });
        return (I) this;
    }

    public final I onMoved(UIAction<C, ComponentEvent> onMoved) {
        LogUtil.nullArgCheck(onMoved, "onMoved", UIAction.class);
        this.component.addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) { onMoved.accept(new EventContext<>(component, e)); }
        });
        return (I) this;
    }

    public final I onShown(UIAction<C, ComponentEvent> onShown) {
        LogUtil.nullArgCheck(onShown, "onShown", UIAction.class);
        this.component.addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { onShown.accept(new EventContext<>(component, e)); }
        });
        return (I) this;
    }

    public final I onHidden(UIAction<C, ComponentEvent> onHidden) {
        LogUtil.nullArgCheck(onHidden, "onHidden", UIAction.class);
        this.component.addComponentListener(new ComponentAdapter() {
            @Override public void componentHidden(ComponentEvent e) { onHidden.accept(new EventContext<>(component, e)); }
        });
        return (I) this;
    }

    public final I onFocusGained(UIAction<C, ComponentEvent> onFocus) {
        LogUtil.nullArgCheck(onFocus, "onFocus", UIAction.class);
        this.component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { onFocus.accept(new EventContext<>(component, e)); }
        });
        return (I) this;
    }

    public final I onFocusLost(UIAction<C, ComponentEvent> onFocus) {
        LogUtil.nullArgCheck(onFocus, "onFocus", UIAction.class);
        this.component.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { onFocus.accept(new EventContext<>(component, e)); }
        });
        return (I) this;
    }

    /**
     *  Swing UIs are made up of nested {@link JComponent} instances.
     *  This method enables support for nested building.
     *  <br><br>
     *
     * @param components An array of {@link JComponent} instances which ought to be added to the wrapped component type.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    @Override
    public final I add(C... components) {
        LogUtil.nullArgCheck(components, "components", Object[].class);
        for( C c : components ) this.add(UI.of(c));
        return (I) this;
    }

    @Override
    protected I _add(C component) {
        LogUtil.nullArgCheck(component, "component", Object.class);
        this.add(UI.of(component));
        return (I) this;
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
     *  This method provides the same functionality as the other "containing" method.
     *  However, it bypasses the necessity to call the "build" method by
     *  calling it internally fo you. <br>
     *  This helps to improve readability, especially when the degree of nesting is very low.
     *
     * @param builders An array of builder instances whose JComponents ought to be added to the one wrapped by this builder.
     * @return This very instance, which enables builder-style method chaining.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final <T extends JComponent> I add(UIForSwing<?, T>... builders) {
        if ( builders == null )
            throw new IllegalArgumentException("Swing tree builders may not be null!");
        for( UIForSwing<?, T> b : builders )
            b.siblings.addAll(
                    Stream.of(builders).filter(builder -> builder != b )
                            .map( builder -> builder.component )
                            .collect(Collectors.toList())
            );
        for( UIForSwing<?, T> b : builders )
            this.component.add(b.getResulting(b.type));

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
    public final <T extends JComponent> I add(String constraints, UIForSwing<?, T> builder) {
        return (I) this.add(constraints, new UIForSwing[]{builder});
    }

    /**
     *  Use this to add any kind of AWT {@link Component} to the {@link JComponent} wrapped by this.
     *
     * @param component A generic AWT {@link Component} which ought to be added to the {@link JComponent} wrapped by this.
     * @return This very instance, which enables builder-style method chaining.
     */
    public final I add(Component component) {
        this.component.add(component);
        return (I) this;
    }

    @SafeVarargs
    public final <T extends JComponent, B extends UIForSwing<?, T>> I add(String conf, B... builders) {
        LayoutManager layout = this.component.getLayout();
        if ( isBorderLayout(conf) && !(layout instanceof BorderLayout) ) {
            if ( layout instanceof MigLayout )
                log.warn("Layout ambiguity detected! Border layout constraint cannot be added to 'MigLayout'.");
            this.component.setLayout(new BorderLayout()); // The UI Maker tries to fill in the blanks!
        }
        for( UIForSwing<?, T> b : builders )
            b.siblings.addAll(
                    Stream.of(builders).filter(builder -> builder != b )
                            .map( builder -> builder.component )
                            .collect(Collectors.toList())
            );
        for( UIForSwing<?, T> b : builders ) this.component.add(b.getResulting(b.type), conf);
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
