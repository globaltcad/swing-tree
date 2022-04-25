package com.globaltcad.swingtree;

import com.alexandriasoftware.swing.JSplitButton;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  This class is a UI tree builder utility class wrapping {@link JComponent}s or
 *  other specialized {@link UI} instances.
 *  Instances of this builder expose an API based on chained methods
 *  designed around functional interfaces to enable building UI tree structures for Swing
 *  in an HTML-like nested fashion while also enabling the possibility to inject additional
 *  functionalities to new components via lambdas.
 *  This class works especially well alongside {@link MigLayout}s.
 *  Simply pass {@link String} constraints to this API to make use of mig layouts.
 *
 * @author dnepp
 */
public class UI
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UI.class);
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
     *  This static factory method returns an instance of a generic swing tree builder
     *  for anything extending the {@link JComponent} class.
     *  <br><br>
     *
     * @param component The new component instance which ought to be part of the Swing UI.
     * @param <T> The concrete type of this new component.
     * @return A basic UI builder instance.
     */
    public static <T extends JComponent> ForSwing<ForSwing, T> of(T component)
    {
        LogUtil.nullArgCheck(component, "component", JComponent.class);
        return new ForSwing<>(component);
    }

    public static <T extends JComponent> ForSwing<ForSwing, T> of(SwingBuilder<T> builder)
    {
        LogUtil.nullArgCheck(builder, "builder", SwingBuilder.class);
        return of(builder.build());
    }

    public static <M extends JMenuItem> ForMenuItem of(MenuBuilder<M> builder)
    {
        LogUtil.nullArgCheck(builder, "builder", MenuBuilder.class);
        return new ForMenuItem(builder.build());
    }

    public static ForPopup of(JPopupMenu popup)
    {
        LogUtil.nullArgCheck(popup, "popup", JPopupMenu.class);
        return new ForPopup(popup);
    }

    /**
     *  Use this to create a builder for the {@link JPopupMenu} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPopupMenu())}.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables builder-style method chaining.
     */
    public static ForPopup popupMenu() { return of(new JPopupMenu()); }

    /**
     *  The following static factory method returns an instance of a {@link ForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *
     * @param separator The new {@link JSeparator} instance which ought to be part of the Swing UI.
     * @return A {@link ForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static ForSeparator of(JSeparator separator)
    {
        LogUtil.nullArgCheck(separator, "separator", JSeparator.class);
        return new ForSeparator(separator);
    }

    /**
     *  The following static factory method returns an instance of a more specialized builder.
     *  Namely, a "ForButton" instance, which extends the "AbstractBuilder" class and provides additional
     *  builder features associated with the more specialized "AbstractButton" component type
     *  wrapped by "ForButton".
     *  <br><br>
     *
     * @param component The new component instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance.
     */
    public static <T extends AbstractButton> ForButton<T> of(T component)
    {
        LogUtil.nullArgCheck(component, "component", AbstractButton.class);
        return new ForButton<>(component);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component without any text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton())}.
     *
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static ForButton<JButton> button() { return of(new JButton()); }

    /**
     *  Use this to create a builder for the {@link JButton} UI component with the provided text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton(String text))}.
     *
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static ForButton<JButton> button(String text) { return of(new JButton(text)); }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).make( it -> it.setIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static ForButton<JButton> buttonWithIcon(Icon icon) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().make( it -> it.setIcon(icon) );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default and on-hover icon displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).make( it -> it.setIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static ForButton<JButton> buttonWithIcon(Icon icon, Icon onHover) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        LogUtil.nullArgCheck(onHover, "onHover", Icon.class);
        return buttonWithIcon(icon, onHover, onHover);
    }

    public static ForButton<JButton> buttonWithIcon(int width, int height, ImageIcon icon, ImageIcon onHover) {
        onHover = new ImageIcon(onHover.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
        icon = new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
        return buttonWithIcon(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default, an on-hover and an on-press icon displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).make( it -> it.setIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static ForButton<JButton> buttonWithIcon(Icon icon, Icon onHover, Icon onPress) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        LogUtil.nullArgCheck(onHover, "onHover", Icon.class);
        LogUtil.nullArgCheck(onPress, "onPress", Icon.class);
        return button().make( it -> it.setIcon(icon) )
                .make( it -> it.setRolloverIcon(onHover) )
                .make( it -> it.setPressedIcon(onPress) );
    }

    public static <B extends JSplitButton> ForSplitButton<B> of(B splitButton) {
        LogUtil.nullArgCheck(splitButton, "splitButton", JSplitButton.class);
        return new ForSplitButton<>(splitButton);
    }

    public static ForSplitButton<JSplitButton> splitButton(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return new ForSplitButton<>(new JSplitButton(text));
    }

    public static ForMenu of(JMenu component) {
        LogUtil.nullArgCheck(component, "component", JMenu.class);
        return new ForMenu(component);
    }

    public static ForMenuItem of(JMenuItem component) {
        LogUtil.nullArgCheck(component, "component", JMenuItem.class);
        return new ForMenuItem(component);
    }

    public static ForMenuItem menuItemSaying(String text) {
        return new ForMenuItem(new JMenuItem(text));
    }

    public static <P extends JPanel> ForPanel<P> of(P component) {
        LogUtil.nullArgCheck(component, "component", JPanel.class);
        return new ForPanel<>(component);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel())}.
     *
     * @return A builder instance for the panel, which enables builder-style method chaining.
     */
    public static ForPanel<JPanel> panel() { return of(new JPanel()); }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr, layout)}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param layout The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for the a panel, which enables builder-style method chaining.
     */
    public static ForPanel<JPanel> panelWithLayout(String attr, String layout) {
        return of(new JPanel()).withLayout(attr, layout);
    }

    public static ForSlider of(JSlider component) {
        LogUtil.nullArgCheck(component, "component", JSlider.class);
        return new ForSlider(component);
    }

    public static ForCombo of(JComboBox component) {
        LogUtil.nullArgCheck(component, "component", JComboBox.class);
        return new ForCombo(component);
    }

    public static ForLabel of(JLabel component) {
        LogUtil.nullArgCheck(component, "component", JLabel.class);
        return new ForLabel(component);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(String text)}.
     *
     * @param text The text which should be displayed on the label.
     * @return A builder instance for the label, which enables builder-style method chaining.
     */
    public static ForLabel label(String text) {
        return of(new JLabel(text));
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables builder-style method chaining.
     */
    public static ForLabel labelWithIcon(Icon icon) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        return of(new JLabel()).make( it -> it.setIcon(icon) );
    }

    public static ForLabel labelWithIcon(int width, int height, ImageIcon icon) {
        LogUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        return of(new JLabel())
                .make(it -> it.setIcon(
                    new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT))
                ));
    }

    public static ForCheckBox of(JCheckBox component) {
        LogUtil.nullArgCheck(component, "component", JCheckBox.class);
        return new ForCheckBox(component);
    }

    public static ForRadioButton of(JRadioButton component) {
        LogUtil.nullArgCheck(component, "component", JRadioButton.class);
        return new ForRadioButton(component);
    }

    public static ForTextComponent of(JTextComponent component) {
        LogUtil.nullArgCheck(component, "component", JTextComponent.class);
        return new ForTextComponent(component);
    }

    public static ForTextComponent input(String text) { return of(new JTextField(text)); }

    public static ForTextComponent input() { return of(new JTextField()); }

    public static <T> ForAnything<T> of(T component)
    {
        return new ForAnything<>(component);
    }

    public static class ForAnything<T> extends AbstractBuilder<ForAnything<T>, T>
    {
        /**
         * Instances of the ForAnything builder do not support the
         * "add" method as defined inside the AbstractNestedBuilder.
         *
         * @param component The component type which will be wrapped by this builder node.
         */
        public ForAnything(T component) { super(component); }
    }

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
    public static class ForSwing<I, C extends JComponent> extends AbstractNestedBuilder<I, C>
    {
        private boolean idAlreadySet = false;
        private boolean migAlreadySet = false;

        /**
         *  Instances of the BasicBuilder as well as its sub types always wrap
         *  a single component for which they are responsible.
         *
         * @param component The JComponent type which will be wrapped by this builder node.
         */
        public ForSwing(C component) { super(component); }

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
         * @param type The {@link Cursor} type defined by a simple enum exposed by this API.
         * @return This very instance, which enables builder-style method chaining.
         */
        public final I withCursor(Cursor type) {
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
        public final I onMouseClick(Consumer<ActionContext<C, MouseEvent>> onClick) {
            LogUtil.nullArgCheck(onClick, "onClick", Consumer.class);
            this.component.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onClick.accept(new ActionContext<>(component, e)); }
            });
            return (I) this;
        }

        /**
         *  Use this to register and catch generic {@link MouseListener} based mouse click events on this UI component.
         *  This method adds the provided UI component consumer lambda to
         *  a {@link MouseListener} instance to the wrapped
         *  button component.
         *  <br><br>
         *
         * @param onClick The lambda instance which will be passed to the button component as {@link MouseListener}.
         * @return This very instance, which enables builder-style method chaining.
         */
        public final I onMouseClickComponent(Consumer<C> onClick) {
            LogUtil.nullArgCheck(onClick, "onClick", Consumer.class);
            return this.onMouseClick( it -> onClick.accept(it.component) );
        }

        /**
         *  Use this to register and catch generic {@link MouseListener} based mouse click events on this UI component.
         *  This method adds the provided {@link MouseEvent} consumer lambda to
         *  a {@link MouseListener} instance to the wrapped
         *  button component.
         *  <br><br>
         *
         * @param onClick The lambda instance which will be passed to the button component as {@link MouseListener}.
         * @return This very instance, which enables builder-style method chaining.
         */
        public final I onMouseClickEvent(Consumer<MouseEvent> onClick) {
            LogUtil.nullArgCheck(onClick, "onClick", Consumer.class);
            return this.onMouseClick( it -> onClick.accept(it.event) );
        }


        /**
         * The provided lambda will be invoked when the component's size changes.
         */
        public final I onResize(Consumer<ActionContext<C, ComponentEvent>> action) {
            LogUtil.nullArgCheck(action, "onClick", Consumer.class);
            this.component.addComponentListener( new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    action.accept(new ActionContext<>(component, e));
                }
            });
            return (I) this;
        }

        /**
         * The provided lambda will be invoked when the component's size changes.
         */
        public final I onResizeComponent(Consumer<C> action) {
            LogUtil.nullArgCheck(action, "onClick", Consumer.class);
            return this.onResize( it -> action.accept(it.component) );
        }

        /**
         * The provided lambda will be invoked when the component's size changes.
         */
        public final I onResizeEvent(Consumer<ComponentEvent> action) {
            LogUtil.nullArgCheck(action, "onClick", Consumer.class);
            return this.onResize( it -> action.accept(it.event) );
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
        public final <T extends JComponent> I add(ForSwing<?, T>... builders) {
            if ( builders == null )
                throw new IllegalArgumentException("Swing tree builders may not be null!");
            for( ForSwing<?, T> b : builders )
                b.siblings.addAll(
                        Stream.of(builders).filter(builder -> builder != b )
                                .map( builder -> builder.component )
                                .collect(Collectors.toList())
                );
            for( ForSwing<?, T> b : builders )
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
        public final <T extends JComponent> I add(String constraints, ForSwing<?, T> builder) {
            return (I) this.add(constraints, new ForSwing[]{builder});
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
        public final <T extends JComponent, B extends ForSwing<?, T>> I add(String conf, B... builders) {
            LayoutManager layout = this.component.getLayout();
            if ( isBorderLayout(conf) && !(layout instanceof BorderLayout) ) {
                if ( layout instanceof MigLayout )
                    log.warn("Layout ambiguity detected! Border layout constraint cannot be added to 'MigLayout'.");
                this.component.setLayout(new BorderLayout()); // The UI Maker tries to fill in the blanks!
            }
            for( ForSwing<?, T> b : builders )
                b.siblings.addAll(
                        Stream.of(builders).filter(builder -> builder != b )
                                .map( builder -> builder.component )
                                .collect(Collectors.toList())
                );
            for( ForSwing<?, T> b : builders ) this.component.add(b.getResulting(b.type), conf);
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

    /**
     *  A UI maker for {@link JPopupMenu} instances.
     */
    public static class ForPopup extends ForSwing<ForPopup, JPopupMenu> {

        /**
         * Instances of the BasicBuilder as well as its sub types always wrap
         * a single component for which they are responsible.
         *
         * @param component The JComponent type which will be wrapped by this builder node.
         */
        protected ForPopup(JPopupMenu component) {
            super(component);
        }

        public ForPopup isBorderPaintedIf(boolean borderPainted) {
            this.component.setBorderPainted(borderPainted);
            return this;
        }

        public ForPopup add(JMenuItem item) { return this.add(UI.of(item)); }

        public ForPopup add(JSeparator separator) { return this.add(UI.of(separator)); }

        public ForPopup add(JPanel panel) { return this.add(UI.of(panel)); }
    }

    /**
     *  The following is a specialized builder implementations for {@link JSeparator} swing components.
     */
    public static class ForSeparator extends ForSwing<ForSeparator, JSeparator>
    {
        /**
         * Instances of ths {@link ForSeparator} always wrap
         * a single {@link JSeparator} for which they are responsible and expose helpful utility methods.
         *
         * @param component The JComponent type which will be wrapped by this builder node.
         */
        protected ForSeparator(JSeparator component) {
            super(component);
        }

        /**
         * @param separatorLength The length of the separation line.
         * @return This very builder to allow for method chaining.
         */
        public ForSeparator withLength(int separatorLength) {
            Dimension d = component.getPreferredSize();
            if ( component.getOrientation() == JSeparator.VERTICAL ) d.height = separatorLength;
            else if ( component.getOrientation() == JSeparator.HORIZONTAL ) d.width = separatorLength;
            component.setPreferredSize(d);
            return this;
        }
    }

    public static class ForButton<B extends AbstractButton>
            extends ForAbstractButton<ForButton<B>, B>
    {
        protected ForButton(B component) {
            super(component);
        }
    }

    /**
     *  The following is a more specialized type of builder which extends the "BasicBuilder" class
     *  and provides additional features associated with the more specialized
     *  "AbstractButton" Swing component type.
     *  One of which is the "onClick" method allowing for a more readable way of adding
     *  ActionListener instances to buttons types...
     *  <br><br>
     *
     * @param <B> The type parameter for the component wrapped by an instance of this class.
     */
    public static
    abstract class ForAbstractButton<I, B extends AbstractButton>
    extends ForSwing<I, B>
    {
        protected ForAbstractButton(B component) { super(component); }

        public final I saying(String text) {
            this.component.setText(text);
            return (I) this;
        }

        /**
         *  Effectively removes the native style of this button.
         *  Without an icon or text, one will not be able to recognize the button.
         *  Use this for buttons with a custom icon or clickable text!
         *
         * @return This very instance, which enables builder-style method chaining.
         */
        public I makePlain() {
            make( it -> {
                it.setBorderPainted(false);
                it.setContentAreaFilled(false);
                it.setOpaque(false);
                it.setFocusPainted(false);
                it.setMargin(new Insets(0,0,0,0));
            });
            return (I) this;
        }

        /**
         *  This method adds the provided
         *  {@link ItemListener} instance to the wrapped button component.
         *  <br><br>
         *
         * @param action The change action lambda which will be passed to the button component.
         * @return This very instance, which enables builder-style method chaining.
         */
        public I onChange(Consumer<ActionContext<B, ItemEvent>> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            this.component.addItemListener(e -> action.accept(new ActionContext<>(this.component, e)));
            return (I) this;
        }

        /**
         *  This method enables a more readable way of adding
         *  {@link ActionListener} instances to button types.
         *  Additionally, to the other "onClick" method this method enables the involvement of the
         *  {@link JComponent} itself into the action supplied to it.
         *  This is very useful for changing the state of the JComponent when the action is being triggered.
         *  <br><br>
         *
         * @param action A {@link Consumer} instance which will be wrapped by an {@link ActionListener} and passed to the button component.
         * @return This very instance, which enables builder-style method chaining.
         */
        public I onClick(Consumer<ActionContext<B, ActionEvent>> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            this.component.addActionListener( e -> action.accept(new ActionContext<>(this.component, e)) );
            return (I) this;
        }

        public final I onClickComponent(Consumer<B> action) {
            LogUtil.nullArgCheck(action, "onClick", Consumer.class);
            return this.onClick( it -> action.accept(it.component) );
        }

        public final I onClickEvent(Consumer<ActionEvent> action) {
            LogUtil.nullArgCheck(action, "onClick", Consumer.class);
            return this.onClick( it -> action.accept(it.event) );
        }

        /**
         *  This method enables a more readable way of adding
         *  {@link ActionListener} instances to button types.
         *  Additionally, to the other "onClick" method this method enables the involvement of the
         *  sibling components of this {@link JComponent} in the supplied lambda action.
         *  This is very useful for changing the state of related {@link JComponent}s.
         *  <br><br>
         *
         * @param action A {@link Consumer} instance which will be wrapped by an {@link ActionListener} and passed to the button component.
         * @return This very instance, which enables builder-style method chaining.
         */
        public I onClickForSiblings(Consumer<ActionContext<List<B>, ActionEvent>> action) {
            this.component.addActionListener( e -> action.accept(new ActionContext<>(this.siblings, e)) );
            return (I) this;
        }

    }

    public static class ForSplitButton<B extends JSplitButton>
    extends ForAbstractButton<ForSplitButton<B>, B>
    {
        private final JPopupMenu popupMenu = new JPopupMenu();
        private final Map<JMenuItem, Runnable> options = new HashMap<>();

        protected ForSplitButton(B component) {
            super(component);
            this.component.setPopupMenu(popupMenu);
            this.component.addButtonClickedActionListener( e -> {
                for ( JMenuItem item : options.keySet() ) {
                    if ( item.getText().equals(component.getText()) ) {
                        Runnable action = options.get(item);
                        if ( action != null ) action.run();
                        break;
                    }
                }
            });
        }

        public ForSplitButton<B> onSplitClick( Consumer<ActionContext<B, ActionEvent>> action ) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            this.component.addSplitButtonClickedActionListener(
                    e -> action.accept(new ActionContext<>(this.component,e))
            );
            return this;
        }

        @Override
        public ForSplitButton<B> onClick(Consumer<ActionContext<B, ActionEvent>> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            this.component.addButtonClickedActionListener( e -> action.accept(new ActionContext<>(this.component, e)) );
            return this;
        }

        public ForSplitButton<B> add(ForMenuItem forItem) {
            LogUtil.nullArgCheck(forItem, "forItem", ForMenuItem.class);
            return this.add(forItem.component);
        }

        public ForSplitButton<B> add(JMenuItem item) {
            LogUtil.nullArgCheck(item, "item", JMenuItem.class);
            popupMenu.add(item);
            return this;
        }

        public ForSplitButton<B> addOption(String option, Runnable action) {
            LogUtil.nullArgCheck(option, "option", String.class);
            LogUtil.nullArgCheck(action, "action", Runnable.class);
            JMenuItem item = new JMenuItem(option);
            popupMenu.add(item);
            options.put(item, action);
            item.addActionListener( e -> component.setText(item.getText()) );
            return this;
        }

        public ForSplitButton<B> addOption(ForMenuItem option, Runnable action) {
            LogUtil.nullArgCheck(option, "option", ForMenuItem.class);
            LogUtil.nullArgCheck(action, "action", Runnable.class);
            JMenuItem item = option.component;
            popupMenu.add(item);
            options.put(item, action);
            item.addActionListener( e -> component.setText(item.getText()) );
            return this;
        }

    }

    /**
     *  A UI make for {@link JMenu} instances.
     */
    public static class ForMenu extends ForAbstractButton<ForMenu, JMenu>
    {
        protected ForMenu(JMenu component) {
            super(component);
        }
    }

    /**
     *  A UI make for {@link JMenuItem} instances.
     */
    public static class ForMenuItem extends ForAbstractButton<ForMenuItem, JMenuItem>
    {
        protected ForMenuItem(JMenuItem component) {
            super(component);
        }

        public ForMenuItem withKeyStroke(KeyStroke keyStroke) {
            this.component.setAccelerator(keyStroke);
            return this;
        }
    }

    /**
     *  A UI make for {@link JPanel} instances.
     */
    public static class ForPanel<P extends JPanel> extends ForSwing<ForPanel<P>, P>
    {
        protected ForPanel(P component) { super(component); }
    }

    /**
     *  A UI make for {@link JSlider} instances.
     */
    public static class ForSlider extends ForSwing<ForSlider, JSlider>
    {
        protected ForSlider(JSlider component) { super(component); }

        public ForSlider onChange(Consumer<ActionContext<JSlider, ChangeEvent>> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            this.component.addChangeListener( e -> action.accept(new ActionContext<>(this.component, e)) );
            return this;
        }

        public ForSlider onChangeComponent(Consumer<JSlider> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            return this.onChange( it -> action.accept(it.component) );
        }

        public ForSlider onChangeEvent(Consumer<ChangeEvent> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            return this.onChange( it -> action.accept(it.event) );
        }
    }

    /**
     *  A UI maker for {@link JComboBox} instances.
     */
    public static class ForCombo extends ForSwing<ForCombo, JComboBox>
    {
        protected ForCombo(JComboBox component) {
            super(component);
        }

        public ForCombo onChange(Consumer<ActionContext<JComboBox, ActionEvent>> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            this.component.addActionListener( e -> action.accept(new ActionContext<>(this.component, e)) );
            return this;
        }

        public ForCombo onChangeEvent(Consumer<ActionEvent> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            return this.onChange( it -> action.accept(it.getEvent()) );
        }

        public ForCombo onChangeComponent(Consumer<JComboBox> action) {
            LogUtil.nullArgCheck(action, "action", Consumer.class);
            return this.onChange( it -> action.accept(it.getComponent()) );
        }
    }

    /**
     *  A UI maker for {@link JLabel} instances.
     */
    public static class ForLabel extends ForSwing<ForLabel, JLabel>
    {
        protected ForLabel(JLabel component) { super(component); }

        /**
         *  Makes the wrapped {@link JLabel} font bold (!plain).
         *
         * @return This very builder to allow for method chaining.
         */
        public ForLabel makeBold() {
            this.make( label -> {
                Font f = label.getFont();
                label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
            });
            return this;
        }

        public ForLabel makeLinkTo(String href) {
            LazyRef<String> text = LazyRef.of(component::getText);
            if ( !href.startsWith("http") ) href = "https://" + href;
            String finalHref = href;
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(finalHref));
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }
                @Override  public void mouseExited(MouseEvent e) { component.setText(text.get()); }
                @Override
                public void mouseEntered(MouseEvent e) {
                    component.setText("<html><a href=''>" + text.get() + "</a></html>");
                }
            });
            return this;
        }

        /**
         *  Set the color of the labelText
         *
         * @param fontColor The color of the label text.
         * @return This very builder to allow for method chaining.
         */
        public ForLabel withColor(Color fontColor) {
            this.component.setForeground(fontColor);
            return this;
        }

        /**
         *  Makes the wrapped {@link JLabel} font plain (!bold).
         *
         * @return This very builder to allow for method chaining.
         */
        public ForLabel makePlain() {
            this.make( label -> {
                Font f = label.getFont();
                label.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
            });
            return this;
        }

        /**
         *  Makes the wrapped {@link JLabel} font bold if it is plain
         *  and plain if it is bold...
         *
         * @return This very builder to allow for method chaining.
         */
        public ForLabel toggleBold() {
            this.make( label -> {
                Font f = label.getFont();
                label.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
            });
            return this;
        }
    }

    /**
     *  A UI maker for {@link JCheckBox} instances.
     */
    public static class ForCheckBox extends ForAbstractButton<ForCheckBox,JCheckBox>
    {
        protected ForCheckBox(JCheckBox component) {
            super(component);
        }
    }

    /**
     *  A UI maker for {@link JRadioButton} instances.
     */
    public static class ForRadioButton extends ForAbstractButton<ForRadioButton,JRadioButton>
    {
        protected ForRadioButton(JRadioButton component) { super(component); }
    }

    public static class ForTextComponent extends ForSwing<ForTextComponent, JTextComponent>
    {
        public interface Remove {void remove( JTextComponent textComp, DocumentFilter.FilterBypass fb, int offset, int length);}
        public interface Insert {void insert( JTextComponent textComp, DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr);}
        public interface Replace{void replace(JTextComponent textComp, DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs);}

        private Remove remove;
        private Insert insert;
        private Replace replace;

        /**
         *  A custom document filter which is simply a lambda-rization wrapper which ought to make
         *  the implementation of custom callbacks more convenient, because the user does not have to implement
         *  all the methods provided by the {@link DocumentFilter}, but can simply pass a lambda for either one
         *  of them.
         */
        private final DocumentFilter filter = new DocumentFilter()
        {
            /**
             * See documentation in {@link DocumentFilter}!
             */
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if ( remove != null ) remove.remove( component, fb, offset, length );
                else fb.remove(offset, length);
            }

            /**
             * See documentation in {@link DocumentFilter}!
             */
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if ( insert != null ) insert.insert( component, fb, offset, string, attr );
                else fb.insertString(offset, string, attr);
            }

            /**
             * See documentation in {@link DocumentFilter}!
             */
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if ( replace != null ) replace.replace( component, fb, offset, length, text, attrs );
                else fb.replace(offset, length, text, attrs);
            }

        };

        protected ForTextComponent(JTextComponent component) { super(component); }

        /**
         * @param action An action which will be executed when the text in the underlying {@link JTextComponent} changes.
         * @return This very builder to allow for method chaining.
         */
        public ForTextComponent onTextChange(Consumer<ActionContext<JTextComponent, DocumentEvent>> action) {
            this.component.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) {action.accept(new ActionContext<>(component, e));}
                @Override public void removeUpdate(DocumentEvent e) {action.accept(new ActionContext<>(component, e));}
                @Override public void changedUpdate(DocumentEvent e) {action.accept(new ActionContext<>(component, e));}
            });
            return this;
        }

        public ForTextComponent onTextChangeComponent(Consumer<JTextComponent> action) {
            return this.onTextChange(it -> action.accept(it.component));
        }

        public ForTextComponent onTextChangeEvent(Consumer<DocumentEvent> action) {
            return this.onTextChange(it -> action.accept(it.event));
        }

        /**
         * @param action An action which will be executed in case the underlying
         *               component supports text filtering (The underlying document is an {@link AbstractDocument}).
         */
        private void ifFilterable(Runnable action) {
            if ( this.component.getDocument() instanceof AbstractDocument ) {
                action.run();
                AbstractDocument doc = (AbstractDocument)this.component.getDocument();
                doc.setDocumentFilter(filter);
            }
        }

        /**
         * @param action A {@link Remove} lambda which will be called when parts (or all) of the text in
         *               the underlying text component gets removed.
         *
         * @return This very builder to allow for method chaining.
         */
        public ForTextComponent onTextRemove(Remove action) {
            ifFilterable( () -> this.remove = action );
            return this;
        }

        /**
         * @param action A {@link Insert} lambda which will be called when new text gets inserted
         *               into the underlying text component.
         *
         * @return This very builder to allow for method chaining.
         */
        public ForTextComponent onTextInsert(Insert action) {
            ifFilterable( () -> this.insert = action );
            return this;
        }

        /**
         * @param action A {@link Replace} lambda which will be called when the text in
         *               the underlying text component gets replaced.
         *
         * @return This very builder to allow for method chaining.
         */
        public ForTextComponent onTextReplace(Replace action) {
            ifFilterable( () -> this.replace = action );
            return this;
        }
    }

    /**
     *  Instances of this are passed to action lambdas for UI components
     *  to give an action more context information.
     *
     * @param <C> The UI component type parameter stored by this.
     * @param <E> The event type parameter of the event stored by this.
     */
    public static class ActionContext<C,E>
    {
        private final C component;
        private final E event;

        private ActionContext(C component, E event) { this.component = component; this.event = event; }

        public C getComponent() { return component; }

        public E getEvent() { return event; }

    }

    /**
     *  Use this to quickly create and inspect a tes window for a UI component.
     */
    public static class TestWindow {

        private final JFrame frame;
        private final Component component;

        public TestWindow(Supplier<JFrame> frameSupplier,Component component) {
            this.frame = frameSupplier.get();
            this.component = component;
            frame.add(component);
            frame.setSize(1000, 1000);
            frame.setVisible(true);
        }

        public JFrame getFrame() { return this.frame; }

        public Component getComponent() { return this.component; }
    }

}
