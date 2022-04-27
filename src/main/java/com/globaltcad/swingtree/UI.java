package com.globaltcad.swingtree;

import com.alexandriasoftware.swing.JSplitButton;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.function.Supplier;

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
    public static <T extends JComponent> UIForSwing<UIForSwing, T> of(T component)
    {
        LogUtil.nullArgCheck(component, "component", JComponent.class);
        return new UIForSwing<>(component);
    }

    public static <T extends JComponent> UIForSwing<UIForSwing, T> of(SwingBuilder<T> builder)
    {
        LogUtil.nullArgCheck(builder, "builder", SwingBuilder.class);
        return of(builder.build());
    }

    public static <M extends JMenuItem> UIForMenuItem of(MenuBuilder<M> builder)
    {
        LogUtil.nullArgCheck(builder, "builder", MenuBuilder.class);
        return new UIForMenuItem(builder.build());
    }

    public static UIForPopup of(JPopupMenu popup)
    {
        LogUtil.nullArgCheck(popup, "popup", JPopupMenu.class);
        return new UIForPopup(popup);
    }

    /**
     *  Use this to create a builder for the {@link JPopupMenu} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPopupMenu())}.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables builder-style method chaining.
     */
    public static UIForPopup popupMenu() { return of(new JPopupMenu()); }

    /**
     *  The following static factory method returns an instance of a {@link UIForSeparator} builder
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
     *  The following static factory method returns an instance of a more specialized builder.
     *  Namely, a "ForButton" instance, which extends the "AbstractBuilder" class and provides additional
     *  builder features associated with the more specialized "AbstractButton" component type
     *  wrapped by "ForButton".
     *  <br><br>
     *
     * @param component The new component instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance.
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
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static UIForButton<JButton> button() { return of(new JButton()); }

    /**
     *  Use this to create a builder for the {@link JButton} UI component with the provided text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton(String text))}.
     *
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static UIForButton<JButton> button(String text) { return of(new JButton(text)); }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).make( it -> it.setIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables builder-style method chaining.
     */
    public static UIForButton<JButton> buttonWithIcon(Icon icon) {
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
    public static UIForButton<JButton> buttonWithIcon(Icon icon, Icon onHover) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        LogUtil.nullArgCheck(onHover, "onHover", Icon.class);
        return buttonWithIcon(icon, onHover, onHover);
    }

    public static UIForButton<JButton> buttonWithIcon(int width, int height, ImageIcon icon, ImageIcon onHover) {
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
    public static UIForButton<JButton> buttonWithIcon(Icon icon, Icon onHover, Icon onPress) {
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

    public static UIForSplitButton<JSplitButton> splitButton(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return new UIForSplitButton<>(new JSplitButton(text));
    }

    public static UIForMenu of(JMenu component) {
        LogUtil.nullArgCheck(component, "component", JMenu.class);
        return new UIForMenu(component);
    }

    public static UIForMenuItem of(JMenuItem component) {
        LogUtil.nullArgCheck(component, "component", JMenuItem.class);
        return new UIForMenuItem(component);
    }

    public static UIForMenuItem menuItemSaying(String text) {
        return new UIForMenuItem(new JMenuItem(text));
    }

    public static <P extends JPanel> UIForPanel<P> of(P component) {
        LogUtil.nullArgCheck(component, "component", JPanel.class);
        return new UIForPanel<>(component);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel())}.
     *
     * @return A builder instance for the panel, which enables builder-style method chaining.
     */
    public static UIForPanel<JPanel> panel() { return of(new JPanel()); }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr, layout)}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param layout The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for the a panel, which enables builder-style method chaining.
     */
    public static UIForPanel<JPanel> panelWithLayout(String attr, String layout) {
        return of(new JPanel()).withLayout(attr, layout);
    }

    public static UIForSlider of(JSlider component) {
        LogUtil.nullArgCheck(component, "component", JSlider.class);
        return new UIForSlider(component);
    }

    public static <E> UIForCombo<E> of(JComboBox<E> component) {
        LogUtil.nullArgCheck(component, "component", JComboBox.class);
        return new UIForCombo<>(component);
    }

    public static UIForCombo<Object> comboBox() { return of(new JComboBox<>()); }

    public static UIForSpinner of(JSpinner spinner) {
        LogUtil.nullArgCheck(spinner, "spinner", JSpinner.class);
        return new UIForSpinner(spinner);
    }

    public static UIForSpinner spinner() { return of(new JSpinner()); }

    public static UIForLabel of(JLabel component) {
        LogUtil.nullArgCheck(component, "component", JLabel.class);
        return new UIForLabel(component);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(String text)}.
     *
     * @param text The text which should be displayed on the label.
     * @return A builder instance for the label, which enables builder-style method chaining.
     */
    public static UIForLabel label(String text) {
        return of(new JLabel(text));
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables builder-style method chaining.
     */
    public static UIForLabel labelWithIcon(Icon icon) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        return of(new JLabel()).make( it -> it.setIcon(icon) );
    }

    public static UIForLabel labelWithIcon(int width, int height, ImageIcon icon) {
        LogUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        return of(new JLabel())
                .make(it -> it.setIcon(
                    new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT))
                ));
    }

    public static UIForCheckBox checkBox(String text) { return of(new JCheckBox(text)); }

    public static UIForCheckBox of(JCheckBox component) {
        LogUtil.nullArgCheck(component, "component", JCheckBox.class);
        return new UIForCheckBox(component);
    }

    public static UIForRadioButton radioButton(String text) {return of(new JRadioButton(text));}

    public static UIForRadioButton of(JRadioButton component) {
        LogUtil.nullArgCheck(component, "component", JRadioButton.class);
        return new UIForRadioButton(component);
    }

    public static UIForTextComponent of(JTextComponent component) {
        LogUtil.nullArgCheck(component, "component", JTextComponent.class);
        return new UIForTextComponent(component);
    }

    public static UIForTextComponent input(String text) { return of(new JTextField(text)); }

    public static UIForTextComponent input() { return of(new JTextField()); }

    public static <T> UIForAnything<T> of(T component) { return new UIForAnything<>(component); }

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
