package com.globaltcad.swingtree;

import com.alexandriasoftware.swing.JSplitButton;
import com.globaltcad.swingtree.api.Buildable;
import com.globaltcad.swingtree.api.MenuBuilder;
import com.globaltcad.swingtree.api.SwingBuilder;
import com.globaltcad.swingtree.api.model.BasicTableModel;
import com.globaltcad.swingtree.api.model.TableListDataSource;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
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

    /**
     *  The scroll policy for UI components with scroll behaviour.
     */
    public enum ScrollBarPolicy {
        NEVER, AS_NEEDED, ALWAYS
    }

    /**
     *  The position of a UI component in terms of directions.
     */
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

    /**
     *  Overflow policy of UI components.
     */
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

    /**
     *  Vertical or horizontal split.
     */
    public enum Split {
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

    /**
     *  Vertical or horizontal alignment.
     */
    public enum Align {
        HORIZONTAL, VERTICAL;

        private int forSlider () {
            switch ( this )
            {
                case HORIZONTAL: return JSlider.HORIZONTAL;
                case VERTICAL: return JSlider.VERTICAL;
            }
            throw new RuntimeException();
        }
        private int forSeparator() {
            switch ( this )
            {
                case HORIZONTAL: return JSeparator.HORIZONTAL;
                case VERTICAL: return JSeparator.VERTICAL;
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
                case TOP:    return SwingConstants.TOP  ;
                case CENTER: return SwingConstants.CENTER ;
                case BOTTOM: return SwingConstants.BOTTOM ;
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
                case LEFT:   return SwingConstants.LEFT   ;
                case CENTER: return SwingConstants.CENTER ;
                case RIGHT:  return SwingConstants.RIGHT  ;
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

    public enum TableData {
        COLUMN_MAJOR, ROW_MAJOR,
        COLUMN_MAJOR_EDITABLE, ROW_MAJOR_EDITABLE;

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
    public static <M extends JMenuItem> UIForMenuItem<M> of(MenuBuilder<M> builder)
    {
        LogUtil.nullArgCheck(builder, "builder", MenuBuilder.class);
        return new UIForMenuItem<>(builder.build());
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JPopupMenu} UI component.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static <P extends JPopupMenu> UIForPopup<P> of(P popup)
    {
        LogUtil.nullArgCheck(popup, "popup", JPopupMenu.class);
        return new UIForPopup<>(popup);
    }

    /**
     *  Use this to create a swing tree builder node for the {@link JPopupMenu} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPopupMenu())}.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static UIForPopup<JPopupMenu> popupMenu() { return of(new JPopupMenu()); }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *
     * @param separator The new {@link JSeparator} instance which ought to be part of the Swing UI.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static <S extends JSeparator> UIForSeparator<S> of(S separator)
    {
        LogUtil.nullArgCheck(separator, "separator", JSeparator.class);
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
    public static UIForSeparator<JSeparator> separator(Align align) {
        LogUtil.nullArgCheck(align, "align", Align.class);
        return of(new JSeparator(align.forSeparator()));
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
     *  This is in essence a convenience method for {@code UI.of(new JButton()).peek( it -> it.setIcon(icon) )}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button(Icon icon) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().peek(it -> it.setIcon(icon) );
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
     *      UI.of(new JButton()).peek( it -> {
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
        return button().peek(it -> it.setIcon(icon) )
                .peek(it -> it.setRolloverIcon(onHover) )
                .peek(it -> it.setPressedIcon(onPress) );
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

    public static <P extends JTabbedPane> UIForTabbedPane<P> of(P pane) {
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
    public static UIForTabbedPane<JTabbedPane> tabbedPane() { return of(new JTabbedPane()); }

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
    public static UIForTabbedPane<JTabbedPane> tabbedPane(Position tabsPosition) {
        LogUtil.nullArgCheck(tabsPosition, "tabsPosition", Position.class);
        return of(new JTabbedPane(tabsPosition.forTabbedPane()));
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
    public static UIForTabbedPane<JTabbedPane> tabbedPane(Position tabsPosition, OverflowPolicy tabsPolicy) {
        LogUtil.nullArgCheck(tabsPosition, "tabsPosition", Position.class);
        LogUtil.nullArgCheck(tabsPolicy, "tabsPolicy", OverflowPolicy.class);
        return of(new JTabbedPane(tabsPosition.forTabbedPane(), tabsPolicy.forTabbedPane()));
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
    public static UIForTabbedPane<JTabbedPane> tabbedPane(OverflowPolicy tabsPolicy) {
        LogUtil.nullArgCheck(tabsPolicy, "tabsPolicy", OverflowPolicy.class);
        return of(new JTabbedPane(Position.TOP.forTabbedPane(), tabsPolicy.forTabbedPane()));
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
    public static Tab tab(String title) {
        LogUtil.nullArgCheck(title, "title", String.class);
        return new Tab(null, title, null, null);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenu} instance.
     *
     * @return A builder instance for the provided {@link JMenu}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenu> UIForMenu<M> of(M component) {
        LogUtil.nullArgCheck(component, "component", JMenu.class);
        return new UIForMenu<>(component);
    }

    /**
     *  Use this to create a builder for the provided {@link JMenuItem} instance.
     *
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenuItem> UIForMenuItem<M> of(M component) {
        LogUtil.nullArgCheck(component, "component", JMenuItem.class);
        return new UIForMenuItem<>(component);
    }

    /**
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return new UIForMenuItem<>(new JMenuItem(text));
    }

    /**
     *  Use this to create a builder for the provided {@link JPanel} instance.
     *
     * @return A builder instance for the provided {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
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

    public static UIForPanel<JPanel> panel(String attr, String colConstraints, String rowConstraints) {
        return of(new JPanel()).withLayout(attr, colConstraints, rowConstraints);
    }

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
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     */
    public static UIForPanel<JPanel> panel(String attr) {
        LogUtil.nullArgCheck(attr, "attr", String.class);
        return of(new JPanel()).withLayout(attr);
    }

    /**
     *  Use this to create a builder for the provided {@link JScrollPane} component.
     *
     * @param component The {@link JScrollPane} component which should be represented by the returned builder.
     * @return A {@link UIForScrollPane} builder representing the provided component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JScrollPane> UIForScrollPane<P> of(P component) {
        LogUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPane<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JScrollPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JScrollPane())}. <br>
     *  Her is an example of a simple scroll panel with a text area inside:
     *  <pre>{@code
     *      UI.scrollPane()
     *      .withScrollBarPolicy(UI.Scroll.NEVER)
     *      .add(UI.textArea("I am a text area with this text inside."))
     *  }</pre>
     *
     * @return A builder instance for a new {@link JScrollPane}, which enables fluent method chaining.
     */
    public static UIForScrollPane<JScrollPane> scrollPane() { return of(new JScrollPane()); }

    /**
     *  Use this to create a builder for the provided {@link JSplitPane} instance.
     *
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JSplitPane> UIForSplitPane<P> of(P component) {
        LogUtil.nullArgCheck(component, "component", JSplitPane.class);
        return new UIForSplitPane<>(component);
    }


    /**
     *  Use this to create a builder for a new {@link JSplitPane} instance
     *  based on the provided split alignment. <br>
     *  You can create a simple split pane based UI like so: <br>
     *  <pre>{@code
     *      UI.splitPane(UI.Split.HORIZONTAL)
     *      .withDividerAt(50)
     *      .add(UI.panel().add(...)) // top
     *      .add(UI.scrollPane().add(...)) // bottom
     *  }</pre>
     *
     * @param align The alignment determining if the {@link JSplitPane} splits vertically or horizontally.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code align} is {@code null}.
     */
    public static UIForSplitPane<JSplitPane> splitPane(Split align) {
        LogUtil.nullArgCheck(align, "align", Split.class);
        return of(new JSplitPane(align.forSplitPane()));
    }

    /**
     *  Use this to create a builder for the provided {@link JEditorPane} instance.
     *
     * @return A builder instance for the provided {@link JEditorPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JEditorPane> UIForEditorPane<P> of(P component) {
        LogUtil.nullArgCheck(component, "component", JEditorPane.class);
        return new UIForEditorPane<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JEditorPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JEditorPane())}.
     *
     * @return A builder instance for a new {@link JEditorPane}, which enables fluent method chaining.
     */
    public static UIForEditorPane<JEditorPane> editorPane() { return of(new JEditorPane()); }

    /**
     *  Use this to create a builder for the provided {@link JTextPane} instance.
     *
     * @return A builder instance for the provided {@link JTextPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JTextPane> UIForTextPane<P> of(P component) {
        LogUtil.nullArgCheck(component, "component", JTextPane.class);
        return new UIForTextPane<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JTextPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextPane())}.
     *
     * @return A builder instance for a new {@link JTextPane}, which enables fluent method chaining.
     */
    public static UIForTextPane<JTextPane> textPane() { return of(new JTextPane()); }

    /**
     *  Use this to create a builder for the provided {@link JSlider} instance.
     *
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <S extends JSlider> UIForSlider<S> of(S component) {
        LogUtil.nullArgCheck(component, "component", JSlider.class);
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
    public static UIForSlider<JSlider> slider(Align align) {
        LogUtil.nullArgCheck(align, "align", Align.class);
        return of(new JSlider(align.forSlider()));
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
    public static UIForSlider<JSlider> slider(Align align, int min, int max) {
        LogUtil.nullArgCheck(align, "align", Align.class);
        return of(new JSlider(align.forSlider(), min, max, (min + max) / 2));
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
    public static UIForSlider<JSlider> slider(Align align, int min, int max, int value) {
        LogUtil.nullArgCheck(align, "align", Align.class);
        return of(new JSlider(align.forSlider(), min, max, value));
    }

    /**
     *  Use this to create a builder for the provided {@link JComboBox} instance.
     *
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     */
    public static <E, C extends JComboBox<E>> UIForCombo<E,C> of(C component) {
        LogUtil.nullArgCheck(component, "component", JComboBox.class);
        return new UIForCombo<>(component);
    }

    /**
     *  Use this to create a builder for a new {@link JComboBox} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JComboBox())}.
     *
     * @return A builder instance for a new {@link JComboBox}, which enables fluent method chaining.
     */
    public static UIForCombo<Object,JComboBox<Object>> comboBox() { return of(new JComboBox<>()); }

    /**
     *  Use this to create a builder for the provided {@link JList} instance
     *  with the provided array of elements as selectable items.
     *
     * @param items The array of elements to be selectable in the {@link JList}.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    @SafeVarargs
    public static <E> UIForCombo<E,JComboBox<E>> comboBox(E... items) {
        LogUtil.nullArgCheck(items, "items", Object[].class);
        return of(new JComboBox<>(items));
    }

    /**
     *  Use this to create a builder for the provided {@link JList} instance
     *  with the provided list of elements as selectable items.
     *
     * @param items The list of elements to be selectable in the {@link JList}.
     * @return A builder instance for the provided {@link JList}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox(java.util.List<E> items) {
        LogUtil.nullArgCheck(items, "items", List.class);
        return of(new JComboBox<>((E[]) items.toArray()));
    }

    /**
     *  Use this to create a builder for the provided {@link JSpinner} instance.
     *
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static <S extends JSpinner> UIForSpinner<S> of(S spinner) {
        LogUtil.nullArgCheck(spinner, "spinner", JSpinner.class);
        return new UIForSpinner<>(spinner);
    }

    /**
     *  Use this to create a builder for a new {@link JSpinner} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSpinner())}.
     *
     * @return A builder instance for a new {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<JSpinner> spinner() { return of(new JSpinner()); }

    /**
     *  Use this to create a builder for the provided {@link JLabel} instance.
     *
     * @return A builder instance for the provided {@link JLabel}, which enables fluent method chaining.
     */
    public static <L extends JLabel> UIForLabel<L> of(L component) {
        LogUtil.nullArgCheck(component, "component", JLabel.class);
        return new UIForLabel<>(component);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(String text)}.
     *
     * @param text The text which should be displayed on the label.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JLabel(text));
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label(Icon icon) {
        LogUtil.nullArgCheck(icon, "icon", Icon.class);
        return of(new JLabel()).with(icon);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param width The width of the icon when displayed on the label.
     * @param height The height of the icon when displayed on the label.
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label(int width, int height, ImageIcon icon) {
        LogUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        return of(new JLabel())
                .with(
                    new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT))
                );
    }

    /**
     *  Use this to create a UI builder for a {@link JLabel} with bold font.
     *  This is in essence a convenience method for {@code UI.label(String text).makeBold()}.
     *  @param text The text which should be displayed on the label.
     *  @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> boldLabel(String text) {
        return of(new JLabel(text)).makeBold();
    }

    public static UIForCheckBox<JCheckBox> checkBox(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JCheckBox(text));
    }

    /**
     *  Use this to create a builder for the provided {@link JCheckBox} instance.
     *
     * @return A builder instance for the provided {@link JCheckBox}, which enables fluent method chaining.
     */
    public static <B extends JCheckBox> UIForCheckBox<B> of(B component) {
        LogUtil.nullArgCheck(component, "component", JCheckBox.class);
        return new UIForCheckBox<>(component);
    }

    public static UIForRadioButton<JRadioButton> radioButton(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JRadioButton(text));
    }

    /**
     *  Use this to create a builder for the provided {@link JRadioButton} instance.
     *
     * @return A builder instance for the provided {@link JRadioButton}, which enables fluent method chaining.
     */
    public static <R extends JRadioButton> UIForRadioButton<R> of(R component) {
        LogUtil.nullArgCheck(component, "component", JRadioButton.class);
        return new UIForRadioButton<>(component);
    }

    /**
     *  Use this to create a builder for the provided {@link JTextField} instance.
     *
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <F extends JTextField> UIForTextField<F> of(F component) {
        LogUtil.nullArgCheck(component, "component", JTextComponent.class);
        return new UIForTextField<>(component);
    }

    public static UIForTextField<JTextField> textField(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JTextField(text));
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextField())}.
     *
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField() { return of(new JTextField()); }

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
    public static <F extends JPasswordField> UIForPasswordField<F> of(F component) {
        LogUtil.nullArgCheck(component, "component", JPasswordField.class);
        return new UIForPasswordField<>(component);
    }

    public static UIForPasswordField<JPasswordField> passwordField(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JPasswordField(text));
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPasswordField())}.
     *
     * @return A builder instance for a new {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField() { return of(new JPasswordField()); }

    /**
     *  Use this to create a builder for the provided {@link JTextArea} instance.
     *
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static <A extends JTextArea> UIForTextArea<A> of(A area) {
        LogUtil.nullArgCheck(area, "area", JTextArea.class);
        return new UIForTextArea<>(area);
    }

    public static UIForTextArea<JTextArea> textArea(String text) {
        LogUtil.nullArgCheck(text, "text", String.class);
        return of(new JTextArea(text));
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
    public static UIForTextArea<JTextArea> textArea(UI.HorizontalDirection direction) {
        LogUtil.nullArgCheck(direction, "direction", HorizontalDirection.class);
        return of(new JTextArea()).withTextOrientation(direction);
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
    public static UIForTextArea<JTextArea> textArea(UI.HorizontalDirection direction, String text) {
        LogUtil.nullArgCheck(direction, "direction", HorizontalDirection.class);
        return of(new JTextArea()).withTextOrientation(direction).withText(text);
    }

    /**
     * @return A builder instance for the provided {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> of(JList<E> list) {
        LogUtil.nullArgCheck(list, "list", JList.class);
        return new UIForList<>(list);
    }

    /**
     * @return A builder instance for a new {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> list(ListModel<E> model) {
        LogUtil.nullArgCheck(model, "model", ListModel.class);
        return of(new JList<>(model));
    }

    /**
     * @return A builder instance for a new {@link JTable}.
     */
    public static <T extends JTable> UIForTable<T> of(T table) {
        LogUtil.nullArgCheck(table, "table", JTable.class);
        return new UIForTable<>(table);
    }

    public static UIForTable<JTable> table() { return of(new JTable()); }

    public static <E> UIForTable<JTable> table(TableData dataFormat, TableListDataSource<E> dataSource) {
        LogUtil.nullArgCheck(dataFormat, "dataFormat", TableData.class);
        LogUtil.nullArgCheck(dataSource, "dataSource", TableListDataSource.class);
        return of(new JTable()).with(dataFormat, dataSource);
    }

    public static UIForTable<JTable> table(Buildable<BasicTableModel> tableModelBuildable) {
        return of(new JTable()).withModel(tableModelBuildable);
    }

    public static BasicTableModel.Builder tableModel() { return new BasicTableModel.Builder(); }

    public static Render.Builder<JTable, Object> renderTable() {
        return new Render<>(JTable.class, Object.class, null).when(Object.class).as(cell->{});
    }

    public static Render.Builder<JList, Object> renderList() {
        return new Render<>(JList.class, Object.class, null).when(Object.class).as(cell->{});
    }

    public static Render.Builder<JComboBox, Object> renderCombo() {
        return new Render<>(JComboBox.class, Object.class, null).when(Object.class).as(cell->{});
    }

    /**
     * @param borderSupplier A lambda which provides a border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JTable, Object> renderTableWithBorder(Supplier<Border> borderSupplier) {
        return new Render<>(JTable.class, Object.class, borderSupplier).when(Object.class).as(cell->{});
    }

    /**
     * @param borderSupplier A lambda which provides a border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JList, Object> renderListWithBorder(Supplier<Border> borderSupplier) {
        return new Render<>(JList.class, Object.class, borderSupplier).when(Object.class).as(cell->{});
    }

    /**
     * @param borderSupplier A lambda which provides a border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JComboBox, Object> renderComboWithBorder(Supplier<Border> borderSupplier) {
        return new Render<>(JComboBox.class, Object.class, borderSupplier).when(Object.class).as(cell->{});
    }

    /**
     * @param border A border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JTable, Object> renderTableWithBorder(Border border) {
        return renderTableWithBorder(()->border);
    }

    /**
     * @param border A border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JList, Object> renderListWithBorder(Border border) {
        return renderListWithBorder(()->border);
    }

    /**
     * @param border A border for rendered cells.
     * @return The builder API allowing method chaining.
     */
    public static Render.Builder<JComboBox, Object> renderComboWithBorder(Border border) {
        return renderComboWithBorder(()->border);
    }

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
     * A convenience method for {@link SwingUtilities#invokeLater(Runnable)},
     * which causes <i>doRun.run()</i> to be executed asynchronously on the
     * AWT event dispatching thread.  This will happen after all
     * pending AWT events have been processed.  This method should
     * be used when an application thread needs to update the GUI.
     * In the following example the <code>invokeLater</code> call queues
     * the <code>Runnable</code> object <code>doHelloWorld</code>
     * on the event dispatching thread and
     * then prints a message.
     * <pre>
     * UI.runLater( () -> System.out.println("Hello World on " + Thread.currentThread()) );
     * System.out.println("This might well be displayed before the other message.");
     * </pre>
     * If invokeLater is called from the event dispatching thread --
     * for example, from a JButton's ActionListener -- the <i>doRun.run()</i> will
     * still be deferred until all pending events have been processed.
     * Note that if the <i>doRun.run()</i> throws an uncaught exception
     * the event dispatching thread will unwind (not the current thread).
     *
     * @param runnable the instance of {@code Runnable}
     * @see #runAndWait
     */
    public static void runLater( Runnable runnable ) {
        LogUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        SwingUtilities.invokeLater(runnable);
    }

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
     * <pre>
     * final Runnable doHelloWorld = () -> {
     *         System.out.println("Hello World on " + Thread.currentThread());
     *      };
     *
     * Thread appThread = new Thread() {
     *     public void run() {
     *         try {
     *             UI.runAndWait(doHelloWorld);
     *         }
     *         catch (Exception e) {
     *             e.printStackTrace();
     *         }
     *         System.out.println("Finished on " + Thread.currentThread());
     *     }
     * };
     * appThread.start();
     * </pre>
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
     * @see #runLater
     */
    public static void runAndWait( Runnable runnable ) throws InterruptedException, InvocationTargetException {
        LogUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        SwingUtilities.invokeAndWait(runnable);
    }

    /**
     *  Use this to quickly create and inspect a tes window for a UI component.
     */
    public static class TestWindow
    {
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
