package com.globaltcad.swingtree;

import com.alexandriasoftware.swing.JSplitButton;
import com.globaltcad.swingtree.api.MenuBuilder;
import com.globaltcad.swingtree.api.SwingBuilder;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 *  This class is a static API for exposing swing tree builder types wrapping different {@link JComponent} types.
 *  Instances of these builder type expose an API based on chained methods
 *  designed around functional interfaces to enable building UI tree structures for Swing
 *  in an HTML-like nested fashion while also enabling the possibility to inject additional
 *  action and settings to new components via lambdas.
 *  Swing tree works especially well alongside {@link MigLayout}s,
 *  which is why this general purpose {@link LayoutManager} is integrated into this library.
 *  Simply pass {@link String} constraints to the {@link UIForAbstractSwing#withLayout(String, String)}
 *  and any given {@link UIForAbstractSwing#add(String, UIForAbstractSwing[])} method
 *  or variant of, to make use of mig layouts.
 */
public final class UI
{
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

    public enum Scroll {
        NEVER, AS_NEEDED, ALWAYS
    }

    public enum Position {
        TOP, LEFT, BOTTOM, RIGHT;
        private int forTabbedPane() {
            switch ( this ) {
                case TOP  : return JTabbedPane.TOP  ;
                case LEFT : return JTabbedPane.LEFT ;
                case BOTTOM: return JTabbedPane.BOTTOM;
                case RIGHT: return JTabbedPane.RIGHT;
            }
            throw new RuntimeException();
        }
    }

    public enum OverflowPolicy {
        WRAP, SCROLL;

        private int forTabbedPane() {
            switch ( this ) {
                case WRAP:   return JTabbedPane.WRAP_TAB_LAYOUT  ;
                case SCROLL: return JTabbedPane.SCROLL_TAB_LAYOUT ;
            }
            throw new RuntimeException();
        }
    }

    public enum Align {
        HORIZONTAL, VERTICAL;

        private int forSplitPane() {
            switch ( this )
            {
                case HORIZONTAL: return JSplitPane.HORIZONTAL_SPLIT;
                case VERTICAL: return JSplitPane.VERTICAL_SPLIT;
            }
            throw new RuntimeException();
        }

    }

    public enum VerticalAlign {
        TOP, CENTER, BOTTOM;

        int forSwing() {
            switch ( this ) {
                case TOP:    return SwingConstants.TOP  ;
                case CENTER: return SwingConstants.CENTER ;
                case BOTTOM: return SwingConstants.BOTTOM ;
            }
            throw new RuntimeException();
        }
    }

    public enum HorizontalAlign {
        LEFT, CENTER, RIGHT;

        public final int forSwing() {
            switch ( this ) {
                case LEFT:   return SwingConstants.LEFT   ;
                case CENTER: return SwingConstants.CENTER ;
                case RIGHT:  return SwingConstants.RIGHT  ;
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
    public static <T extends JComponent> UIForSwing<T> of(T component)
    {
        LogUtil.nullArgCheck(component, "component", JComponent.class);
        return new UIForSwing<>(component);
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
    public static <T extends JComponent> UIForSwing<T> of(SwingBuilder<T> builder)
    {
        LogUtil.nullArgCheck(builder, "builder", SwingBuilder.class);
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
    public static <M extends JMenuItem> UIForMenuItem of(MenuBuilder<M> builder)
    {
        LogUtil.nullArgCheck(builder, "builder", MenuBuilder.class);
        return new UIForMenuItem(builder.build());
    }

    /**
     *  Use this to create a swing tree builder for the {@link JPopupMenu} UI component.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static UIForPopup of(JPopupMenu popup)
    {
        LogUtil.nullArgCheck(popup, "popup", JPopupMenu.class);
        return new UIForPopup(popup);
    }

    /**
     *  Use this to create a swing tree builder for the {@link JPopupMenu} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPopupMenu())}.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static UIForPopup popupMenu() { return of(new JPopupMenu()); }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *
     * @param separator The new {@link JSeparator} instance which ought to be part of the Swing UI.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator of(JSeparator separator)
    {
        LogUtil.nullArgCheck(separator, "separator", JSeparator.class);
        return new UIForSeparator(separator);
    }

    /**
     *  This returns a {@link JButton} swing tree builder.
     *
     * @param component The button component which ought to be wrapped by the swing tree UI builder.
     * @return A basic UI {@link JButton} builder instance.
     */
    public static <T extends AbstractButton> UIForButton<T> of(T component)
    {
        LogUtil.nullArgCheck(component, "component", AbstractButton.class);
        return new UIForButton<>(component);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component without any text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton())}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button() { return of(new JButton()); }

    /**
     *  Use this to create a builder for the {@link JButton} UI component with the provided text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton(String text))}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button(String text) { return of(new JButton(text)); }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).make( it -> it.setIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button(Icon icon) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().make( it -> it.setIcon(icon) );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button(Icon icon, Icon onHover) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        LogUtil.nullArgCheck(onHover, "onHover", Icon.class);
        return button(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top
     *  which should both be scaled to the provided dimensions.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button(int width, int height, ImageIcon icon, ImageIcon onHover) {
        onHover = new ImageIcon(onHover.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
        icon = new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
        return button(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default, an on-hover and an on-press icon displayed on top.
     *  This is in essence a convenience method for:
     *  <pre>{@code 
     *      UI.of(new JButton()).make( it -> {
     *          it.setIcon(icon);
     *          it.setRolloverIcon(onHover);
     *          it.setPressedIcon(onPress);
     *      })
     *  }</pre>
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button(Icon icon, Icon onHover, Icon onPress) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        LogUtil.nullArgCheck(onHover, "onHover", Icon.class);
        LogUtil.nullArgCheck(onPress, "onPress", Icon.class);
        return button().make( it -> it.setIcon(icon) )
                .make( it -> it.setRolloverIcon(onHover) )
                .make( it -> it.setPressedIcon(onPress) );
    }

    public static <B extends JSplitButton> UIForSplitButton<B> of(B splitButton) {
        LogUtil.nullArgCheck(splitButton, "splitButton", JSplitButton.class);
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
    public static UIForSplitButton<JSplitButton> splitButton(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return new UIForSplitButton<>(new JSplitButton(text));
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
    public static SplitItem<JMenuItem> splitItem(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
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
    public static SplitItem<JRadioButtonMenuItem> splitRadioItem(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return SplitItem.of(new JRadioButtonMenuItem(text));
    }

    public static UIForTabbedPane of(JTabbedPane pane) {
        return new UIForTabbedPane(pane);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTabbedPane())}.
     *
     * @return A builder instance for a new {@link JTabbedPane}, which enables fluent method chaining.
     */
    public static UIForTabbedPane tabbedPane() { return of(new JTabbedPane()); }

    public static UIForTabbedPane tabbedPane(Position position) {
        return of(new JTabbedPane(position.forTabbedPane()));
    }

    public static UIForTabbedPane tabbedPane(Position position, OverflowPolicy policy) {
        return of(new JTabbedPane(position.forTabbedPane(), policy.forTabbedPane()));
    }

    public static UIForTabbedPane tabbedPane(OverflowPolicy policy) {
        return of(new JTabbedPane(Position.TOP.forTabbedPane(), policy.forTabbedPane()));
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(..).add(UI.button("click me")))
     *  }</pre>
     *
     * @param title The text displayed on the tab button.
     * @return A {@link Tab} instance containing everything needed to be added to a {@link JTabbedPane}.
     */
    public static Tab tab(String title) {
        return new Tab(null, title, null, null);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenu} instance.
     *
     * @return A builder instance for the provided {@link JMenu}, which enables fluent method chaining.
     */
    public static UIForMenu of(JMenu component) {
        LogUtil.nullArgCheck(component, "component", JMenu.class);
        return new UIForMenu(component);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenuItem} instance.
     *
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem of(JMenuItem component) {
        LogUtil.nullArgCheck(component, "component", JMenuItem.class);
        return new UIForMenuItem(component);
    }

    public static UIForMenuItem menuItem(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return new UIForMenuItem(new JMenuItem(text));
    }

    /**
     *  Use this to create a builder for the provided {@link JPanel} instance.
     *
     * @return A builder instance for the provided {@link JPanel}, which enables fluent method chaining.
     */
    public static <P extends JPanel> UIForPanel<P> of(P component) {
        LogUtil.nullArgCheck(component, "component", JPanel.class);
        return new UIForPanel<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel())}.
     *
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel() { return of(new JPanel()); }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr, layout)}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel(String attr, String colConstraints) {
        return of(new JPanel()).withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr, layout)}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel(String attr) { return of(new JPanel()).withLayout(attr); }

    public static UIForScrollPane of(JScrollPane component) {
        LogUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPane(component);
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPane())}.
     *
     * @return A builder instance for a new {@link JScrollPane}, which enables fluent method chaining.
     */
    public static UIForScrollPane scrollPane() { return of(new JScrollPane()); }

    /**
     *  Use this to create a builder for the provided {@link JSplitPane} instance.
     *
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     */
    public static UIForSplitPane of(JSplitPane component) {
        LogUtil.nullArgCheck(component, "component", JSplitPane.class);
        return new UIForSplitPane(component);
    }


    /**
     *  Use this to create a builder for a new {@link JSplitPane} instance
     *  based on tbe provided split alignment.
     *
     * @param align The alignment determining if the {@link JSplitPane} splits vertically or horizontally.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     */
    public static UIForSplitPane splitPane(Align align) { return of(new JSplitPane(align.forSplitPane())); }

    /**
     *  Use this to create a builder for the provided {@link JEditorPane} instance.
     *
     * @return A builder instance for the provided {@link JEditorPane}, which enables fluent method chaining.
     */
    public static UIForEditorPane of(JEditorPane component) {
        LogUtil.nullArgCheck(component, "component", JEditorPane.class);
        return new UIForEditorPane(component);
    }

    /**
     *  Use this to create a builder for a new {@link JEditorPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JEditorPane())}.
     *
     * @return A builder instance for a new {@link JEditorPane}, which enables fluent method chaining.
     */
    public static UIForEditorPane editorPane() { return of(new JEditorPane()); }

    /**
     *  Use this to create a builder for the provided {@link JTextPane} instance.
     *
     * @return A builder instance for the provided {@link JTextPane}, which enables fluent method chaining.
     */
    public static UIForTextPane of(JTextPane component) {
        LogUtil.nullArgCheck(component, "component", JTextPane.class);
        return new UIForTextPane(component);
    }

    /**
     *  Use this to create a builder for a new {@link JTextPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextPane())}.
     *
     * @return A builder instance for a new {@link JTextPane}, which enables fluent method chaining.
     */
    public static UIForTextPane textPane() { return of(new JTextPane()); }

    /**
     *  Use this to create a builder for the provided {@link JSlider} instance.
     *
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     */
    public static UIForSlider of(JSlider component) {
        LogUtil.nullArgCheck(component, "component", JSlider.class);
        return new UIForSlider(component);
    }

    /**
     *  Use this to create a builder for the provided {@link JComboBox} instance.
     *
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     */
    public static <E> UIForCombo<E> of(JComboBox<E> component) {
        LogUtil.nullArgCheck(component, "component", JComboBox.class);
        return new UIForCombo<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JComboBox} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JComboBox())}.
     *
     * @return A builder instance for a new {@link JComboBox}, which enables fluent method chaining.
     */
    public static UIForCombo<Object> comboBox() { return of(new JComboBox<>()); }

    /**
     *  Use this to create a builder for the provided {@link JSpinner} instance.
     *
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner of(JSpinner spinner) {
        LogUtil.nullArgCheck(spinner, "spinner", JSpinner.class);
        return new UIForSpinner(spinner);
    }

    /**
     *  Use this to create a builder for a new {@link JSpinner} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSpinner())}.
     *
     * @return A builder instance for a new {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner spinner() { return of(new JSpinner()); }

    /**
     *  Use this to create a builder for the provided {@link JLabel} instance.
     *
     * @return A builder instance for the provided {@link JLabel}, which enables fluent method chaining.
     */
    public static UIForLabel of(JLabel component) {
        LogUtil.nullArgCheck(component, "component", JLabel.class);
        return new UIForLabel(component);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(String text)}.
     *
     * @param text The text which should be displayed on the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel label(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JLabel(text));
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel label(Icon icon) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        return of(new JLabel()).withIcon(icon);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param width The width of the icon when displayed on the label.
     * @param height The height of the icon when displayed on the label.
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel label(int width, int height, ImageIcon icon) {
        LogUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        return of(new JLabel())
                .withIcon(
                    new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT))
                );
    }

    public static UIForCheckBox checkBox(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JCheckBox(text));
    }

    /**
     *  Use this to create a builder for the provided {@link JCheckBox} instance.
     *
     * @return A builder instance for the provided {@link JCheckBox}, which enables fluent method chaining.
     */
    public static UIForCheckBox of(JCheckBox component) {
        LogUtil.nullArgCheck(component, "component", JCheckBox.class);
        return new UIForCheckBox(component);
    }

    public static UIForRadioButton radioButton(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JRadioButton(text));
    }

    /**
     *  Use this to create a builder for the provided {@link JRadioButton} instance.
     *
     * @return A builder instance for the provided {@link JRadioButton}, which enables fluent method chaining.
     */
    public static UIForRadioButton of(JRadioButton component) {
        LogUtil.nullArgCheck(component, "component", JRadioButton.class);
        return new UIForRadioButton(component);
    }

    /**
     *  Use this to create a builder for the provided {@link JTextField} instance.
     *
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField of(JTextField component) {
        LogUtil.nullArgCheck(component, "component", JTextComponent.class);
        return new UIForTextField(component);
    }

    public static UIForTextField textField(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JTextField(text));
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextField())}.
     *
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField textField() { return of(new JTextField()); }

    /**
     *  Use this to create a builder for the provided {@link JFormattedTextField} instance.
     *
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField of(JFormattedTextField component) {
        LogUtil.nullArgCheck(component, "component", JFormattedTextField.class);
        return new UIForFormattedTextField(component);
    }

    public static UIForFormattedTextField formattedTextField(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JFormattedTextField(text));
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
    public static UIForPasswordField of(JPasswordField component) {
        LogUtil.nullArgCheck(component, "component", JPasswordField.class);
        return new UIForPasswordField(component);
    }

    public static UIForPasswordField passwordField(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JPasswordField(text));
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPasswordField())}.
     *
     * @return A builder instance for a new {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField passwordField() { return of(new JPasswordField()); }

    /**
     *  Use this to create a builder for the provided {@link JTextArea} instance.
     *
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea of(JTextArea area) {
        LogUtil.nullArgCheck(area, "area", JTextArea.class);
        return new UIForTextArea(area);
    }

    public static UIForTextArea textArea(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JTextArea(text));
    }

    /**
     *  Use this to create a builder for a new {@link JTextArea} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextArea())}.
     *
     * @return A builder instance for a new {@link JTextArea}, which enables fluent method chaining.
     */
    public static UIForTextArea textArea() { return of(new JTextArea()); }

    /**
     *  Use this to create a builder for anything.
     *
     * @return A builder instance for the provided object, which enables fluent method chaining.
     */
    public static <T> UIForAnything<T> of(T component) {
        LogUtil.nullArgCheck(component, "component", Object.class);
        return new UIForAnything<>(component);
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
