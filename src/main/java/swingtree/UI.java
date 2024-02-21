package swingtree;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Event;
import sprouts.*;
import swingtree.animation.Animator;
import swingtree.animation.LifeTime;
import swingtree.animation.Stride;
import swingtree.api.*;
import swingtree.api.model.BasicTableModel;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;
import swingtree.style.StylableComponent;
import swingtree.components.JBox;
import swingtree.components.JIcon;
import swingtree.components.JScrollPanels;
import swingtree.components.JSplitButton;
import swingtree.components.listener.NestedJScrollPanelScrollCorrection;
import swingtree.dialogs.ConfirmAnswer;
import swingtree.dialogs.ConfirmDialog;
import swingtree.dialogs.MessageDialog;
import swingtree.dialogs.OptionsDialog;
import swingtree.layout.LayoutConstraint;
import swingtree.style.*;
import swingtree.threading.EventProcessor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
public final class UI extends UINamespaceUtilities
{
    private static final Logger log = LoggerFactory.getLogger(UI.class);
    /**
     *  An enum set of all the available swing cursors which
     *  map to the cursor type id.
     *  This exists simply because swing was created before enums were added to Java.
     */
    public enum Cursor implements UIEnum<Cursor>
    {
        DEFAULT(java.awt.Cursor.DEFAULT_CURSOR),
        CROSS(java.awt.Cursor.CROSSHAIR_CURSOR),
        TEXT(java.awt.Cursor.TEXT_CURSOR),
        WAIT(java.awt.Cursor.WAIT_CURSOR),
        RESIZE_SOUTH_WEST(java.awt.Cursor.SW_RESIZE_CURSOR),
        RESIZE_SOUTH_EAST(java.awt.Cursor.SE_RESIZE_CURSOR),
        RESIZE_NORTH_WEST(java.awt.Cursor.NW_RESIZE_CURSOR),
        RESIZE_NORTH_EAST(java.awt.Cursor.NE_RESIZE_CURSOR),
        RESIZE_NORTH(java.awt.Cursor.N_RESIZE_CURSOR),
        RESIZE_SOUTH(java.awt.Cursor.S_RESIZE_CURSOR),
        RESIZE_WEST(java.awt.Cursor.W_RESIZE_CURSOR),
        RESIZE_EAST(java.awt.Cursor.E_RESIZE_CURSOR),
        HAND(java.awt.Cursor.HAND_CURSOR),
        MOVE(java.awt.Cursor.MOVE_CURSOR);


        final int type;


        Cursor( int type ) { this.type = type; }

        public java.awt.Cursor toAWTCursor() { return java.awt.Cursor.getPredefinedCursor(type); }
    }

    /**
     *  A general purpose enum describing if something is never, always or sometimes active.
     *  This is mostly used to configure the scroll bar policy for UI components with scroll behaviour.
     */
    public enum Active implements UIEnum<Active>{
        NEVER, AS_NEEDED, ALWAYS
    }

    /**
     *  All UI components are at their core rectangular, meaning they
     *  always have exactly 4 uniquely identifiable sides.
     *  This enum is used to target specific sides of a {@link JComponent}
     *  in various API methods like for example {@link UIForTabbedPane#withTabPlacementAt(Side)}
     *  or the tapped pane factory method {@link UI#tabbedPane(Side)}.
     */
    public enum Side implements UIEnum<Side>
    {
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
    public enum OverflowPolicy implements UIEnum<OverflowPolicy>
    {
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
    public enum Align implements UIEnum<Align>
    {
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
    public enum VerticalAlignment implements UIEnum<VerticalAlignment>{
        TOP, CENTER, BOTTOM;

        public int forSwing() {
            switch ( this ) {
                case TOP:    return SwingConstants.TOP;
                case CENTER: return SwingConstants.CENTER;
                case BOTTOM: return SwingConstants.BOTTOM;
            }
            throw new RuntimeException();
        }
        public float forY() {
            switch ( this ) {
                case TOP:    return 0f;
                case CENTER: return 0.5f;
                case BOTTOM: return 1f;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Different positions along a horizontally aligned UI component.
     */
    public enum HorizontalAlignment implements UIEnum<HorizontalAlignment>
    {
        LEFT, CENTER, RIGHT, LEADING, TRAILING;

        public final int forSwing() {
            switch ( this ) {
                case LEFT:     return SwingConstants.LEFT;
                case CENTER:   return SwingConstants.CENTER;
                case RIGHT:    return SwingConstants.RIGHT;
                case LEADING:  return SwingConstants.LEADING;
                case TRAILING: return SwingConstants.TRAILING;
            }
            throw new RuntimeException();
        }
        public final float forX() {
            switch ( this ) {
                case LEFT: case LEADING:
                    return 0f;
                case CENTER:
                    return 0.5f;
                case RIGHT: case TRAILING:
                    return 1f;
            }
            throw new RuntimeException();
        }

        public final int forFlowLayout() {
            switch ( this ) {
                case LEFT:     return FlowLayout.LEFT;
                case CENTER:   return FlowLayout.CENTER;
                case RIGHT:    return FlowLayout.RIGHT;
                case LEADING:  return FlowLayout.LEADING;
                case TRAILING: return FlowLayout.TRAILING;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  The logical combination of a vertical and horizontal alignment.
     */
    public enum Alignment implements UIEnum<Alignment>
    {
        TOP_LEFT,    TOP_CENTER, TOP_RIGHT, TOP_LEADING, TOP_TRAILING,
        CENTER_LEFT, CENTER, CENTER_RIGHT, CENTER_LEADING, CENTER_TRAILING,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, BOTTOM_LEADING, BOTTOM_TRAILING;

        public VerticalAlignment getVertical() {
            switch ( this ) {
                case TOP_LEFT: case TOP_CENTER: case TOP_RIGHT: case TOP_LEADING: case TOP_TRAILING:
                    return VerticalAlignment.TOP;
                case CENTER_LEFT: case CENTER: case CENTER_RIGHT: case CENTER_LEADING: case CENTER_TRAILING:
                    return VerticalAlignment.CENTER;
                case BOTTOM_LEFT: case BOTTOM_CENTER: case BOTTOM_RIGHT: case BOTTOM_LEADING: case BOTTOM_TRAILING:
                    return VerticalAlignment.BOTTOM;
            }
            throw new RuntimeException();
        }

        public HorizontalAlignment getHorizontal() {
            switch ( this ) {
                case TOP_LEFT: case CENTER_LEFT: case BOTTOM_LEFT:
                    return HorizontalAlignment.LEFT;
                case TOP_CENTER: case CENTER: case BOTTOM_CENTER:
                    return HorizontalAlignment.CENTER;
                case TOP_RIGHT: case CENTER_RIGHT: case BOTTOM_RIGHT:
                    return HorizontalAlignment.RIGHT;
                case TOP_LEADING: case CENTER_LEADING: case BOTTOM_LEADING:
                    return HorizontalAlignment.LEADING;
                case TOP_TRAILING: case CENTER_TRAILING: case BOTTOM_TRAILING:
                    return HorizontalAlignment.TRAILING;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Defines whether the list based data model of a {@link JTable} is row or column major
     *  and whether it is editable or not.
     *  See {@link UI#table(ListData, TableListDataSource)}  or {@link UIForTable#withModel(ListData, TableListDataSource)}
     *  for more information about the usage of this enum.
     */
    public enum ListData implements UIEnum<ListData>
    {
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

    /**
     *  Defines whether the data model of a {@link JTable} should be editable or not.
     *  See {@link UI#table(MapData, TableMapDataSource)} or {@link UIForTable#withModel(MapData, TableMapDataSource)}
     *  for more information about the usage of this enum.
     */
    public enum MapData implements UIEnum<MapData>
    {
        EDITABLE, READ_ONLY;

        final boolean isEditable() {
            switch ( this ) {
                case EDITABLE :  return true;
                case READ_ONLY : return false;
            }
            throw new RuntimeException();
        }
    }
    /**
     *  Use this to target specific edges of a {@link JComponent} and apply
     *  custom {@link StyleConf} properties to them.
     *  <br>
     *  See {@link ComponentStyleDelegate#borderWidthAt(Edge, double)}
     */
    public enum Edge implements UIEnum<Edge>
    {
        EVERY,
        TOP,    RIGHT,
        BOTTOM, LEFT
    }

    /**
     *  Instances of this enum are used to configure onto which
     *  layer a particular style configuration should be applied.
     */
    public enum Layer implements UIEnum<Layer>
    {
        /**
         *  This layer is applied through the {@link StylableComponent#paintBackground(Graphics, Runnable)} method.
         *  When using custom components, please make sure your component implements this interface!
         */
        BACKGROUND,
        /**
         *  This layer is rendered right after the background layer through the {@link Border} of a component.
         *  Every component supports this layer.
         */
        CONTENT,
        /**
         *  This layer is rendered right after the content layer through the {@link Border} of a component.
         *  Every component supports this layer.
         */
        BORDER,
        /**
         *  The foreground is painted through the {@link StylableComponent#paintForeground(Graphics, Runnable)} method.
         *  When using custom components, please make sure your component implements this interface!
         */
        FOREGROUND
    }

    /**
     *  Use these enum instances to specify the gradient type for various sub styles,
     *  like for example the gradient style API exposed by {@link ComponentStyleDelegate#gradient(Layer, String, Function)}
     *  or {@link ComponentStyleDelegate#gradient(Function)} methods (see {@link UIForAnySwing#withStyle(Styler)}).
     *  <p>
     *  {@link GradientConf#type(GradientType)} method exposed by methods like
     *  {@link ComponentStyleDelegate#gradient(String, Function)} or {@link ComponentStyleDelegate#gradient(Layer, String, Function)}.
     */
    public enum GradientType implements UIEnum<GradientType>
    {
        /**
         *  A linear gradient is a gradient that follows a straight line.
         */
        LINEAR,
        /**
         *  A radial gradient is a gradient that follows a circular pattern by growing from a central point outwards.
         */
        RADIAL,
        /**
         *  A conic gradient paints the color transition like the hands of a clock move around its center.
         */
        CONIC
    }

    /**
     *  Defines the different types of noise functions that can be used to render
     *  a {@link NoiseConf} style. <br>
     *  Pass instances of this to {@link NoiseConf#function(NoiseFunction)} to configure the noise behaviour
     *  as part of the style API (see {@link UIForAnySwing#withStyle(Styler)}).
     */
    public enum NoiseType implements UIEnum<NoiseType>, NoiseFunction
    {
        STOCHASTIC(NoiseFunctions::stochastic),
        SMOOTH_TOPOLOGY(NoiseFunctions::smoothTopology),
        HARD_TOPOLOGY(NoiseFunctions::hardTopology),
        SMOOTH_SPOTS(NoiseFunctions::smoothSpots),
        HARD_SPOTS(NoiseFunctions::hardSpots),
        GRAINY(NoiseFunctions::grainy),
        TILES(NoiseFunctions::tiles),
        FIBERS(NoiseFunctions::fibery);


        private final NoiseFunction function;

        NoiseType( NoiseFunction function ) {
            this.function = function;
        }

        @Override
        public float getFractionAt(float x, float y) {
            return function.getFractionAt(x, y);
        }
    }

    /**
     *  Use these enum instances to specify the gradient alignment for various sub styles,
     *  like for example the gradient style API exposed by {@link ComponentStyleDelegate#gradient(Function)}
     *  or {@link ComponentStyleDelegate#gradient(Function)} methods (see {@link UIForAnySwing#withStyle(Styler)}).
     * <p>
     *  {@link GradientConf#span(Span)} method exposed by methods like
     *  {@link ComponentStyleDelegate#gradient(String, Function)} or {@link ComponentStyleDelegate#gradient(Layer, String, Function)}.
     */
    public enum Span implements UIEnum<Span>
    {
        TOP_LEFT_TO_BOTTOM_RIGHT, BOTTOM_LEFT_TO_TOP_RIGHT,
        TOP_RIGHT_TO_BOTTOM_LEFT, BOTTOM_RIGHT_TO_TOP_LEFT,

        TOP_TO_BOTTOM, LEFT_TO_RIGHT,
        BOTTOM_TO_TOP, RIGHT_TO_LEFT;

        /**
         * @return {@code true} if this alignment is diagonal, {@code false} otherwise.
         */
        public boolean isDiagonal() {
            return this == TOP_LEFT_TO_BOTTOM_RIGHT || this == BOTTOM_LEFT_TO_TOP_RIGHT ||
                   this == TOP_RIGHT_TO_BOTTOM_LEFT || this == BOTTOM_RIGHT_TO_TOP_LEFT;
        }
    }

    /**
     *  Used to specify the cycle method for a gradient conf in the style API.
     *  See {@link UIForAnySwing#withStyle(Styler)} and {@link ComponentStyleDelegate#gradient(Function)}.
     *  <br>
     *  The following list describes what each enum instance represents:
     *  <ul>
     *      <li>{@link Cycle#NONE} -
     *          The gradient is only rendered once, without repeating.
     *          The last color is used to fill the remaining area.
     *      </li>
     *      <li>{@link Cycle#REFLECT} -
     *          The gradient is rendered once and then reflected.,
     *          which means that the gradient is rendered again in reverse order
     *          starting from the last color and ending with the first color.
     *          After that, the gradient is rendered again in the original order,
     *          starting from the first color and ending with the last color and so on.
     *      </li>
     *      <li>{@link Cycle#REPEAT} -
     *          The gradient is rendered repeatedly, which means that it
     *          is rendered again and again in the original order, starting from the first color
     *          and ending with the last color.
     *      </li>
     *  </ul>
     */
    public enum Cycle implements UIEnum<Cycle>
    {
        NONE,
        REFLECT,
        REPEAT
    }

    /**
     *  Use this in the style API (see {@link UIForAnySwing#withStyle(Styler)})
     *  to target specific corners of a {@link JComponent} and apply
     *  custom {@link StyleConf} properties to them.
     *  <br>
     *  See {@link ComponentStyleDelegate#borderRadiusAt(Corner, double, double)}.
     */
    public enum Corner implements UIEnum<Corner>
    {
        EVERY,
        TOP_LEFT,    TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_RIGHT
    }

    /**
     *  Use this to specify the placement of an image as part of the {@link ImageConf} through
     *  the {@link ImageConf#placement(Placement)} method exposed by the style API (see {@link UIForAnySwing#withStyle(Styler)}).
     */
    public enum Placement implements UIEnum<Placement>
    {
        UNDEFINED,
        TOP, LEFT, BOTTOM, RIGHT,
        TOP_LEFT, TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_RIGHT,
        CENTER
    }

    /**
     *  Defines the areas of a component, which is used
     *  to by the {@link ImageConf} to determine if and how an image should be clipped.
     *  Pass instances of this to {@link ImageConf#clipTo(ComponentArea)} to configure the clipping behaviour
     *  as part of the style API (see {@link UIForAnySwing#withStyle(Styler)}). <br>
     *  The following list describes what each enum instance represents:
     *  <ul>
     *      <li>{@link swingtree.UI.ComponentArea#ALL} -
     *      The entire component, which is the union of all other clip
     *      areas ({@code INTERIOR + EXTERIOR + BORDER + CONTENT}).
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#INTERIOR} -
     *      The inner component area, which is defined as {@code ALL - EXTERIOR - BORDER}.
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#EXTERIOR} -
     *      The outer component area, which can be expressed as {@code ALL - INTERIOR - BORDER},
     *      or {@code ALL - CONTENT}.
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#BORDER} -
     *      The border of the component, which is the area between the inner and outer component area
     *      and which can be expressed as {@code ALL - INTERIOR - EXTERIOR}.
     *      </li>
     *      <li>{@link swingtree.UI.ComponentArea#BODY} -
     *      The body of the component is the inner component area including the border area.
     *      It can be expressed as {@code ALL - EXTERIOR}, or {@code INTERIOR + BORDER}.
     *      </li>
     *  </ul>
     */
    public enum ComponentArea implements UIEnum<ComponentArea>
    {
        ALL, EXTERIOR, BORDER, INTERIOR, BODY
    }

    /**
     * Enum representing the different boundaries of a UI component.
     * Here's a brief explanation of each enum entry:
     * <ul>
     *     <li>{@link ComponentBoundary#OUTER_TO_EXTERIOR} -
     *     The outermost boundary of the entire component, including any margin that might be applied.
     *     </li>
     *     <li>{@link ComponentBoundary#EXTERIOR_TO_BORDER} -
     *     The boundary located after the margin but before the border.
     *     This tightly wraps the entire {@link ComponentArea#BODY}.
     *     </li>
     *     <li>{@link ComponentBoundary#BORDER_TO_INTERIOR} -
     *     The boundary located after the border but before the padding.
     *     It represents the edge of the component's interior.
     *     </li>
     *     <li>{@link ComponentBoundary#INTERIOR_TO_CONTENT} -
     *     The boundary located after the padding.
     *     It represents the innermost boundary of the component, where the actual content of the component begins,
     *     like for example the contents of a {@link JPanel} or {@link JScrollPane}.
     *     </li>
     * </ul>
     */
    public enum ComponentBoundary {
        /**
         * The outermost boundary of the component, including any margin that might be applied.
         */
        OUTER_TO_EXTERIOR, // The outermost boundary of the component.
        /**
         * The boundary located after the margin but before the border. This wraps the {@link ComponentArea#BODY}.
         */
        EXTERIOR_TO_BORDER, // After the margin, before the border.
        /**
         * The boundary located after the border but before the padding. It represents the edge of the component's interior.
         */
        BORDER_TO_INTERIOR, // After the border, before the padding.
        /**
         * The boundary located after the padding.
         * It represents the innermost boundary of the component, where the actual content of the component begins,
         * like for example the contents of a {@link JPanel} or {@link JScrollPane}.
         */
        INTERIOR_TO_CONTENT, // After the padding, before the content.
        CENTER_TO_CONTENT, // The center of the component.
    }

    /**
     *  Use this to specify the orientation of a component.
     *  This is especially important for components that display text.
     *  <br>
     *  See {@link UIForAnySwing#withStyle(Styler)} and {@link ComponentStyleDelegate#orientation(ComponentOrientation)}.
     */
    public enum ComponentOrientation implements UIEnum<ComponentOrientation>
    {
        UNKNOWN, LEFT_TO_RIGHT, RIGHT_TO_LEFT
    }

    /**
     *  Used to define how a layout manager (typically the {@link BoxLayout})
     *  will lay out components along the given axis. <br>
     *  Create a simple box layout for your components
     *  by calling the {@link UIForAnySwing#withBoxLayout(Axis)} method,
     *  or use {@link Layout#box(Axis)} factory method returning a {@link Layout} config
     *  object which can be passed to the style API (see {@link UIForAnySwing#withStyle(Styler)}
     *  and {@link ComponentStyleDelegate#layout(Layout)}).
     */
    public enum Axis implements UIEnum<Axis>
    {
        /**
         * Specifies that something is laid out left to right.
         */
        X,
        /**
         * Specifies that something is laid out top to bottom.
         */
        Y,
        /**
         * Specifies that something is laid out in the direction of
         * a line of text as determined by the target container's
         * {@code ComponentOrientation} property.
         */
        LINE,
        /**
         * Specifies that something is laid out in the direction that
         * lines flow across a page as determined by the target container's
         * {@code ComponentOrientation} property.
         */
        PAGE;

        public int forBoxLayout() {
            switch ( this ) {
                case X:    return BoxLayout.X_AXIS;
                case Y:    return BoxLayout.Y_AXIS;
                case LINE: return BoxLayout.LINE_AXIS;
                case PAGE: return BoxLayout.PAGE_AXIS;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Set of enum instances defining common types of Swing look and feels.
     *  Use {@link UI#currentLookAndFeel()} to check which look and feel is currently active.
     */
    public enum LookAndFeel implements UIEnum<LookAndFeel> {
        OTHER,
        METAL,
        FLAT_LAF,
        NIMBUS;
    }

    /**
     * @return One of
     *            <ul>
     *                <li>{@link LookAndFeel#FLAT_LAF}</li>
     *                <li>{@link LookAndFeel#NIMBUS}</li>
     *                <li>{@link LookAndFeel#METAL}</li>
     *            </ul>
     *            or {@link LookAndFeel#OTHER} if none of the above
     *            was recognized.
     *
     */
    public static LookAndFeel currentLookAndFeel() {
        try {
            String laf = UIManager.getLookAndFeel().getClass().getName();
            if ( laf.contains("FlatLaf") ) return LookAndFeel.FLAT_LAF;
            if ( laf.contains("Nimbus")  ) return LookAndFeel.NIMBUS;
            if ( laf.contains("Metal")   ) return LookAndFeel.METAL;
        }
        catch (Exception ignored) {}

        return LookAndFeel.OTHER;
    }

    /**
     * @return The current UI scale factor, which is used for DPI aware painting and layouts.
     */
    public static float scale() { return SwingTree.get().getUiScaleFactor(); }

    /**
     * Multiplies the given float value by the user scale factor.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     *
     * @param value The float value to scale.
     * @return The scaled float value.
     */
    public static float scale( float value ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1 ? value : (value * scaleFactor) );
    }

    /**
     * Multiplies the given double value by the user scale factor.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     *
     * @param value The double value to scale.
     * @return The scaled double value.
     */
    public static double scale( double value ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1 ? value : (value * scaleFactor) );
    }

    /**
     * Multiplies the given int value by the user scale factor and rounds the result.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     * @param value The int value to scale.
     * @return The scaled int value.
     */
    public static int scale( int value ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1 ? value : Math.round( value * scaleFactor ) );
    }

    /**
     * Similar as {@link UI#scale(int)} but always "rounds down".
     * <p>
     * For use in special cases. {@link UI#scale(int)} is the preferred method.
     *
     * @param value The value to scale and then round down if the scaled result is not a whole number.
     * @return The scaled and rounded down value.
     */
    public int scaleRoundedDown( int value ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1 ? value : (int) (value * scaleFactor) );
    }

    /**
     * Divides the given float value by the user scale factor.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     *
     * @param value The float value to unscale.
     * @return The unscaled float value.
     */
    public static float unscale( float value ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1f ? value : (value / scaleFactor) );
    }

    /**
     * Divides the given int value by the user scale factor and rounds the result.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     * @param value The int value to unscale.
     * @return The unscaled int value.
     */
    public static int unscale( int value ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1f ? value : Math.round( value / scaleFactor ) );
    }

    /**
     * If user scale factor is not 1, scale the given graphics context by invoking
     * {@link Graphics2D#scale(double, double)} with user scale factor.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     *
     * @param g The graphics context to scale.
     */
    public static void scale( Graphics2D g ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        if ( scaleFactor != 1f )
            g.scale( scaleFactor, scaleFactor );
    }

    /**
     * Scales the given dimension with the user scale factor.
     * <p>
     * If user scale factor is 1, then the given dimension is simply returned.
     * Otherwise, a new instance of {@link Dimension} or {@link javax.swing.plaf.DimensionUIResource}
     * is returned, depending on whether the passed dimension implements {@link javax.swing.plaf.UIResource}.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     *
     * @param dimension The dimension to scale.
     * @return The scaled dimension.
     */
    public static Dimension scale( Dimension dimension ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return (dimension == null || scaleFactor == 1f)
                ? dimension
                : (dimension instanceof UIResource
                    ? new DimensionUIResource( UI.scale( dimension.width ), UI.scale( dimension.height ) )
                    : new Dimension          ( UI.scale( dimension.width ), UI.scale( dimension.height ) ));
    }

    /**
     * Returns a rectangle from the given rectangle with the user scale factor applied.
     * <p>
     * If user scale factor is 1, then the given rectangle is simply returned.
     * Otherwise, a new instance of {@link Rectangle} or {@link javax.swing.plaf.UIResource} is returned.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     * @param rectangle The rectangle to scale.
     * @return The scaled rectangle.
     */
    public static Rectangle scale( Rectangle rectangle ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return (rectangle == null || scaleFactor == 1f)
                ? rectangle
                : new Rectangle(
                        UI.scale( rectangle.x ),     UI.scale( rectangle.y ),
                        UI.scale( rectangle.width ), UI.scale( rectangle.height )
                    );
    }

    /**
     * Returns a rectangle from the given rectangle with the user scale factor applied.
     * <p>
     * If user scale factor is 1, then the given rectangle is simply returned.
     * Otherwise, a new instance of {@link Rectangle} or {@link javax.swing.plaf.UIResource} is returned.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     *
     * @param rectangle The rectangle to scale.
     * @return The scaled rectangle.
     */
    public static RoundRectangle2D scale( RoundRectangle2D rectangle ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        if ( rectangle == null || scaleFactor == 1f ) return rectangle;
        if ( rectangle instanceof RoundRectangle2D.Float )
            return new RoundRectangle2D.Float(
                    (float) UI.scale( rectangle.getX() ),        (float) UI.scale( rectangle.getY() ),
                    (float) UI.scale( rectangle.getWidth() ),    (float) UI.scale( rectangle.getHeight() ),
                    (float) UI.scale( rectangle.getArcWidth() ), (float) UI.scale( rectangle.getArcHeight() )
                );
        else
            return new RoundRectangle2D.Double(
                    UI.scale( rectangle.getX() ),        UI.scale( rectangle.getY() ),
                    UI.scale( rectangle.getWidth() ),    UI.scale( rectangle.getHeight() ),
                    UI.scale( rectangle.getArcWidth() ), UI.scale( rectangle.getArcHeight() )
                );
    }

    /**
     * Scales the given insets with the user scale factor.
     * <p>
     * If user scale factor is 1, then the given insets is simply returned.
     * Otherwise, a new instance of {@link Insets} or {@link javax.swing.plaf.InsetsUIResource}
     * is returned, depending on whether the passed dimension implements {@link javax.swing.plaf.UIResource}.
     *
     * @param insets The insets to scale.
     * @return The scaled insets.
     */
    public static Insets scale( Insets insets ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return (insets == null || scaleFactor == 1f)
                ? insets
                : (insets instanceof UIResource
                    ? new InsetsUIResource( UI.scale( insets.top ), UI.scale( insets.left ), UI.scale( insets.bottom ), UI.scale( insets.right ) )
                    : new Insets          ( UI.scale( insets.top ), UI.scale( insets.left ), UI.scale( insets.bottom ), UI.scale( insets.right ) ));
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
     * @param styleSheet The style sheet to be used for all subsequent UI building operations.
     * @param scope A lambda scope in which the style sheet is active for all subsequent UI building operations.
     * @param <T> The type of the result of the given scope.
     * @return the result of the given scope, usually a {@link JComponent} or SwingTree UI.
     */
    public static <T> T use( StyleSheet styleSheet, Supplier<T> scope ) {
        if ( !UI.thisIsUIThread() )
            return runAndGet( ()-> use(styleSheet, scope) );

        SwingTree swingTreeContext = SwingTree.get();
        StyleSheet oldStyleSheet = swingTreeContext.getStyleSheet();
        swingTreeContext.setStyleSheet(styleSheet);
        try {
            T result = scope.get();
            if ( result instanceof JComponent )
                ComponentExtension.from((JComponent) result).gatherApplyAndInstallStyle(true);
            if ( result instanceof UIForAnySwing )
                ComponentExtension.from(((UIForAnySwing<?,?>) result).getComponent()).gatherApplyAndInstallStyle(true);

            return result;
        } finally {
            swingTreeContext.setStyleSheet(oldStyleSheet);
        }
    }

    /**
     *  Sets the {@link EventProcessor} to be used for all subsequent UI building operations.
     *  This method allows to switch between different event processing strategies.
     *  In particular, the {@link EventProcessor#DECOUPLED} is recommended to be used for
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
            return runAndGet(()-> use(processor, scope));

        SwingTree swingTreeContext = SwingTree.get();
        EventProcessor oldProcessor = swingTreeContext.getEventProcessor();
        swingTreeContext.setEventProcessor(processor);
        try {
            return scope.get();
        } finally {
            swingTreeContext.setEventProcessor(oldProcessor);
        }
    }

    /**
     * Loads an {@link ImageIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Note that this method will also return {@link SvgIcon} instances, if the icon is an SVG image.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param path The path to the icon. It can be a classpath resource or a file path.
     * @return An optional containing the icon if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public static Optional<ImageIcon> findIcon( String path ) {
        return findIcon(IconDeclaration.of(path));
    }

    /**
     * Loads an {@link ImageIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Note that this method will also return {@link SvgIcon} instances, if the icon is an SVG image.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param declaration The icon declaration, a value object defining the path to the icon.
     * @return An optional containing the icon if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code declaration} is {@code null}.
     */
    public static Optional<ImageIcon> findIcon( IconDeclaration declaration ) {
        Objects.requireNonNull(declaration, "declaration");
        Map<IconDeclaration, ImageIcon> cache = SwingTree.get().getIconCache();
        ImageIcon icon = cache.get(declaration);
        if ( icon == null ) {
            icon = _tryLoadIcon(declaration);
            if ( icon != null )
                cache.put(declaration, icon);
        }
        return Optional.ofNullable(icon);
    }

    /**
     * Loads an {@link SvgIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param path The path to the icon. It can be a classpath resource or a file path.
     * @return An optional containing the {@link SvgIcon} if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public static Optional<SvgIcon> findSvgIcon( String path ) {
        Objects.requireNonNull(path, "path");
        return findSvgIcon(IconDeclaration.of(path));
    }

    /**
     * Loads an {@link SvgIcon} from the resource folder, the classpath, a local file
     * or from cache if it has already been loaded.
     * If no icon could be found, an empty optional is returned.
     * <br><br>
     * Also, checkout {@link SwingTree#getIconCache()} to see where the icons are cached.
     *
     * @param declaration The icon declaration, a value object defining the path to the icon.
     * @return An optional containing the {@link SvgIcon} if it could be found, an empty optional otherwise.
     * @throws NullPointerException if {@code declaration} is {@code null}.
     */
    public static Optional<SvgIcon> findSvgIcon( IconDeclaration declaration ) {
        Objects.requireNonNull(declaration, "declaration");
        if ( !declaration.path().endsWith(".svg") )
            return Optional.empty();

        Map<IconDeclaration, ImageIcon> cache = SwingTree.get().getIconCache();
        ImageIcon icon = cache.get(declaration);
        if ( icon == null ) {
            icon = _tryLoadIcon(declaration);
            if ( icon != null )
                cache.put(declaration, icon);
        }
        if ( !(icon instanceof SvgIcon) )
            return Optional.empty();
        else
            return Optional.of(icon).map(SvgIcon.class::cast);
    }

    /**
     * Loads an icon from the classpath or from a file.
     * @param declaration The icon declaration, a value object defining the path to the icon.
     *          The path can be a classpath resource or a file path.
     * @return The icon.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    private static ImageIcon _tryLoadIcon( IconDeclaration declaration )
    {
        ImageIcon icon = null;
        try {
            icon = _loadIcon(declaration);
        } catch (Exception e) {
            log.error("Failed to load icon from declaration: " + declaration, e);
        }
        return icon;
    }

    /**
     * Loads an icon from the classpath or from a file.
     * @param declaration The icon declaration, a value object defining the path to the icon.
     *          The path can be a classpath resource or a file path.
     * @return The icon.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    private static ImageIcon _loadIcon( IconDeclaration declaration )
    {
        Objects.requireNonNull(declaration, "declaration");
        String path = declaration.path();
        Objects.requireNonNull(path, "path");
        path = path.trim();
        if ( path.isEmpty() )
            return null;
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
        Optional<Integer> width  = declaration.size().width().map(Number::intValue);
        Optional<Integer> height = declaration.size().height().map(Number::intValue);
        if ( path.endsWith(".svg") ) {
            SVGDocument tempSVGDocument = null;
            try {
                SVGLoader loader = new SVGLoader();
                tempSVGDocument = Objects.requireNonNull(loader.load(url));
            } catch (Exception e) {
                log.error("Failed to load SVG document from URL: " + url, e);
                return null;
            }
            SvgIcon icon = new SvgIcon(tempSVGDocument).withIconSize(declaration.size());
            if ( width.isPresent() && height.isPresent() )
                return icon.withIconSize(width.get(), height.get());
            if ( width.isPresent() )
                return icon.withIconSizeFromWidth(width.get());
            if ( height.isPresent() )
                return icon.withIconSizeFromHeight(height.get());
            return icon;
        } else {
        /*
            Not that we explicitly use the "createImage" method of the toolkit here.
            This is because otherwise the image might get cached inside the toolkit,
            which is in the way of our own caching mechanism.
            (The internal caching of the toolkit is somewhat limited and we have no control over it,
            which is why we use our own cache.)
        */
            ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(url), url.toExternalForm());
            double ratio = (double) icon.getIconWidth() / (double) icon.getIconHeight();
            if ( width.isPresent() && height.isPresent() )
                return new ImageIcon(icon.getImage().getScaledInstance(width.get(), height.get(), Image.SCALE_SMOOTH));
            if ( width.isPresent() )
                return new ImageIcon(icon.getImage().getScaledInstance(width.get(), (int) (width.get() / ratio), Image.SCALE_SMOOTH));
            if ( height.isPresent() )
                return new ImageIcon(icon.getImage().getScaledInstance((int) (height.get() * ratio), height.get(), Image.SCALE_SMOOTH));
            return icon;
        }
    }

    private UI(){ super(); } // This is a static API

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
        return new UIForSwing<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for the provided {@link JPanel} type. <br>
     *  This method is typically used to enable declarative UI design for custom
     *  {@link JPanel} based components either within the constructor of a custom
     *  subclass, like so: <br>
     *  <pre>{@code
     *  class MyCustomPanel extends JPanel {
     *      public MyCustomPanel() {
     *          UI.of(this)
     *          .add(UI.label("Hello Swing!"))
     *          .add(UI.button("Click Me"))
     *          .add(UI.button("Or Me") );
     *      }
     *  }
     *  }</pre>
     *  <br>
     *  ... or as part of a UI declaration, where the custom {@link JPanel} type
     *  is added to the components tree, like so: <br>
     *  <pre>{@code
     *  UI.show(
     *      UI.panel()
     *      .add(
     *          new MyCustomPanel()
     *      )
     *      .add(..more stuff..)
     *  );
     *  }</pre>
     *  <br>
     *
     * @param component The {@link JPanel} instance to be wrapped by a swing tree UI builder for panel components.
     * @return A builder instance for the provided {@link JPanel}, which enables fluent method chaining.
     * @param <P> The type parameter of the concrete {@link JPanel} type to be wrapped.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JPanel> UIForPanel<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JPanel.class);
        return new UIForPanel<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager.
     *  This is in essence a convenience method for {@code UI.of(new JPanel(new MigLayout()))}.
     *
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     */
    public static UIForPanel<JPanel> panel() { 
        return _panel().withLayout(new MigLayout("hidemode 2")); 
    }
    
    private static UIForPanel<JPanel> _panel() {
        return new UIForPanel<>(new BuilderState<>(Panel.class, Panel::new));
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
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( String attr ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        return _panel().withLayout(attr);
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
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( String attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return _panel().withLayout(attr, colConstraints);
    }

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
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( String attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return _panel().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LayoutConstraint attr ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        return panel(attr.toString());
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
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LayoutConstraint attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return _panel().withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, colConstraints, rowConstraints)))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LayoutConstraint attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return _panel().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}. <br>
     *  This method is typiclly used alongside the {@link UI#LC()} factory
     *  method to create a layout attributes/constraints builder, like so: <br>
     *  <pre>{@code
     *      UI.panel(
     *          UI.LC()
     *          .insets("10 10 10 10")
     *          .debug()
     *      )
     *      .add(..)
     *      .add(..)
     *  }</pre>
     *
     * @param attr The constraint attributes concerning the entire {@link MigLayout}
     *             in the form of a {@link LC} instance.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LC attr ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        return _panel().withLayout( attr );
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(new MigLayout(attr, ConstraintParser.parseColumnConstraints(colConstraints))))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LC} constants.
     * @param colConstraints The column constraints in the form of a {@link String} instance.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LC attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return _panel().withLayout( attr, colConstraints );
    }

    /**
     *  Use this to create a builder for a new {@link JPanel} UI component
     *  with a {@link MigLayout} as its layout manager and the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JPanel(
     *          new MigLayout(
     *              attr,
     *              ConstraintParser.parseColumnConstraints(colConstraints),
     *              ConstraintParser.parseRowConstraints(rowConstraints)
     *          )
     *      ))
     *  }</pre>
     *  <br>
     * @param attr The layout attributes in the form of a {@link LC} instance.
     * @param colConstraints The column constraints in the form of a {@link String} instance.
     * @param rowConstraints The row constraints in the form of a {@link String} instance.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( LC attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return _panel().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for the {@link JPanel} UI component with a
     *  dynamically updated set of {@link MigLayout} constraints/attributes.
     *  This is in essence a convenience method for {@code UI.of(new JPanel()).withLayout(attr)}.
     *
     * @param attr The layout attributes property which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JPanel}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForPanel<JPanel> panel( Val<LayoutConstraint> attr ) {
        NullUtil.nullArgCheck(attr, "attr", Val.class);
        NullUtil.nullPropertyCheck(attr, "attr", "Null is not a valid layout attribute.");
        return panel(attr.get().toString()).withLayout(attr);
    }

    /**
     *  Use this to create a builder for the provided {@link JBox} instance,
     *  which is a general purpose component wrapper type with the following properties:
     *  <ul>
     *      <li>
     *          It is transparent, meaning that it does not paint its background.
     *      </li>
     *      <li>
     *          The default layout manager is a {@link MigLayout}.
     *      </li>
     *      <li>
     *          The insets (the space between the wrapped component and the box's border)
     *          are set to zero.
     *      </li>
     *      <li>
     *          There the gap size between the components added to the box is set to zero.
     *          So they will be tightly packed.
     *      </li>
     *  </ul>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     *  This method is typically used to enable declarative UI design for custom
     *  {@link JBox} based components either within the constructor of a custom
     *  subclass, like so: <br>
     *  <pre>{@code
     *  class MyCustomBox extends JBox {
     *      public MyCustomBox() {
     *          UI.of(this)
     *          .add(UI.label("Hello Swing!"))
     *          .add(UI.button("Click Me"))
     *          .add(UI.button("Or Me") );
     *      }
     *  }
     *  }</pre>
     *  <br>
     *  ... or as part of a UI declaration, where the custom {@link JBox} type
     *  is added to the components tree, like so: <br>
     *  <pre>{@code
     *  UI.show(
     *      UI.panel()
     *      .add(
     *          new MyCustomBox()
     *      )
     *      .add(..more stuff..)
     *  );
     *  }</pre>
     *  <br>
     *
     * @param component The box component type for which a builder should be created.
     * @param <B> THe type parameter defining the concrete {@link JBox} type.
     * @return A builder for the provided box component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <B extends JBox> UIForBox<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JPanel.class);
        return new UIForBox<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a {@link JBox} instance,
     *  which is a general purpose component wrapper type with the following properties:
     *  <ul>
     *      <li>
     *          It is transparent, meaning that it does not paint its background.
     *      </li>
     *      <li>
     *          The default layout manager is a {@link MigLayout}.
     *      </li>
     *      <li>
     *          The insets (the spaces between the wrapped component and the box's border)
     *          are all set to zero.
     *      </li>
     *      <li>
     *          The gap sizes between the components added to the box is set to zero.
     *          So the children of this component will be tightly packed.
     *      </li>
     *  </ul>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     *  This factory method is especially useful for when you simply want to nest components
     *  tightly without having to worry about the layout manager or the background
     *  color covering the background of the parent component.
     *  <br>
     *  Note that you can also emulate the {@link JBox} type with a {@link JPanel} using
     *  {@code UI.panel("ins 0, gap 0").makeNonOpaque()}.
     *
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     */
    public static UIForBox<JBox> box() {
        return new UIForBox<>(new BuilderState<>(Box.class, Box::new));
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  <br>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( String attr ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        if ( attr.isEmpty() ) attr = "ins 0";
        else if ( !attr.contains("ins") ) attr += ", ins 0";
        return box().withLayout(attr);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( String attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        if (attr.isEmpty()) attr = "ins 0";
        else if (!attr.contains("ins")) attr += ", ins 0";
        return box().withLayout(attr, colConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This factory method is especially useful for when you simply want to nest components
     *  tightly without having to worry about the layout manager or the background
     *  color covering the background of the parent component.
     *  <br>
     *  Note that you can also emulate the {@link JBox} type with a {@link JPanel} using
     *  <pre>{@code
     *      UI.panel(attr, colConstraints, rowConstraints).makeNonOpaque()
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     * @param attr The layout attributes.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( String attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", String.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        if (attr.isEmpty()) attr = "ins 0";
        else if (!attr.contains("ins")) attr += ", ins 0";
        return box().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints. <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     *  <br>
     *  This method allows you to pass a {@link LayoutConstraint} constants as the layout attributes,
     *  which is an instance typically chosen from the {@link UI} class constants
     *  like for example {@link UI#FILL}, {@link UI#FILL_X}, {@link UI#FILL_Y}... <br>
     *  A typical usage example would be: <br>
     *  <pre>{@code
     *      UI.box(UI.FILL_X.and(UI.WRAP(2)))
     *      .add(..)
     *      .add(..)
     *  }</pre>
     *  In this code snippet the creates a {@link JBox} with a {@link MigLayout} as its layout manager
     *  where the box will fill the parent component horizontally and
     *  the components added to the box will be wrapped after every two components.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LayoutConstraint attr ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        return box(attr.toString());
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints. <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     *  This method allows you to pass a {@link LayoutConstraint} constants as the layout attributes,
     *  which is an instance typically chosen from the {@link UI} class constants
     *  like for example {@link UI#FILL}, {@link UI#FILL_X}, {@link UI#FILL_Y}... <br>
     *  A typical usage example would be: <br>
     *  <pre>{@code
     *      UI.box(UI.FILL, "[shrink]6[grow]")
     *      .add(..)
     *      .add(..)
     *  }</pre>
     *  In this code snippet the creates a {@link JBox} with a {@link MigLayout} as its layout manager
     *  where the box will fill the parent component horizontally and vertically
     *  and the first column of components will be shrunk to their preferred size
     *  and the second column will grow to fill the available space.
     *  Both columns will have a gap of 6 pixels between them.
     *
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @param colConstraints The layout which will be passed to the {@link MigLayout} constructor as second argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LayoutConstraint attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return box(attr.toString(), colConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This is essentially a convenience method for the following: <br>
     *  <pre>{@code
     *      UI.of(new JBox(new MigLayout(attr, colConstraints, rowConstraints)))
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LayoutConstraint attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LayoutConstraint.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return box(attr.toString(), colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     * @param attr The layout attributes which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LC attr ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        return box().withLayout(attr);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This is essentially a convenience method which may also be expressed as: <br>
     *  <pre>{@code
     *      UI.of(new JBox(new MigLayout(attr, colConstraints)))
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     *
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LC attr, String colConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        return box().withLayout( attr, colConstraints )           ;
    }

    /**
     *  Use this to create a builder for a {@link JBox}, conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This is essentially a convenience method which may also be expressed as: <br>
     *  <pre>{@code
     *      UI.of(new JBox())
     *      .peek( box -> {
     *          box.setLayout(
     *              new MigLayout(
     *                  attr,
     *                  ConstraintParser.parseColumnConstraints(colConstraints),
     *                  ConstraintParser.parseRowConstraints(rowConstraints)
     *              )
     *          )
     *      })
     *  }</pre>
     *  <br>
     *  <b>Please note that the {@link JBox} type is in no way related to the {@link BoxLayout}!
     *  The term <i>box</i> is referring to the purpose of this component, which
     *  is to tightly store and wrap other sub-components seamlessly...</b>
     *  <p>
     * @param attr The layout attributes in the form of a {@link LayoutConstraint} constants.
     * @param colConstraints The column constraints.
     * @param rowConstraints The row constraints.
     * @return A builder instance for a transparent {@link JBox}, which enables fluent method chaining.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( LC attr, String colConstraints, String rowConstraints ) {
        NullUtil.nullArgCheck(attr, "attr", LC.class);
        NullUtil.nullArgCheck(colConstraints, "colConstraints", String.class);
        NullUtil.nullArgCheck(rowConstraints, "rowConstraints", String.class);
        return box().withLayout(attr, colConstraints, rowConstraints);
    }

    /**
     *  Use this to create a builder for a {@link JBox}, a generic component wrapper type
     *  which is transparent and without any insets as well as with a {@link MigLayout}
     *  as its layout manager.
     *  This is conceptually the same as a
     *  transparent {@link JPanel} without any insets
     *  and a {@link MigLayout} constructed using the provided constraints.
     *  This method allows you to dynamically determine the {@link LayoutConstraint} constants
     *  of the {@link MigLayout} instance, by passing a {@link Val} property which
     *  will be observed and its value passed to the {@link MigLayout} constructor
     *  whenever it changes.
     *  This is in essence a convenience method for:
     *  {@code UI.box().withLayout(attr.viewAsString( it -> it+", ins 0"))}.
     *
     * @param attr The layout attributes property which will be passed to the {@link MigLayout} constructor as first argument.
     * @return A builder instance for a new {@link JBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code attr} is {@code null}.
     * @see <a href="http://www.miglayout.com/QuickStart.pdf">Quick Start Guide</a>
     */
    public static UIForBox<JBox> box( Val<LayoutConstraint> attr ) {
        NullUtil.nullArgCheck(attr, "attr", Val.class);
        NullUtil.nullPropertyCheck(attr, "attr", "Null is not a valid layout attribute.");
        return box().withLayout(attr.view( it -> it.and("ins 0")));
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
        return new UIForSwing<>(new BuilderState<>((Class<T>) JComponent.class, ()->builder.build()));
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
        return new UIForMenuItem<>(new BuilderState<>(builder.build()));
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
        return new UIForPopup<>(new BuilderState<>(popup));
    }
    
    /**
     *  Use this to create a swing tree builder node for the {@link JPopupMenu} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPopupMenu())}.
     *
     * @return A builder instance for a {@link JPopupMenu}, which enables fluent method chaining.
     */
    public static UIForPopup<JPopupMenu> popupMenu() {
        return new UIForPopup<>(new BuilderState<>(PopupMenu.class, ()->new PopupMenu()));
    }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *
     * @param separator The new {@link JSeparator} instance which ought to be part of the Swing UI.
     * @param <S> The concrete type of this new component.
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static <S extends JSeparator> UIForSeparator<S> of( S separator )
    {
        NullUtil.nullArgCheck(separator, "separator", JSeparator.class);
        return new UIForSeparator<>(new BuilderState<>(separator));
    }

    /**
     *  This returns an instance of a {@link UIForSeparator} builder
     *  responsible for building a {@link JSeparator} by exposing helpful utility methods for it.
     *  This is in essence a convenience method for {@code UI.of(new JSeparator())}.
     *
     * @return A {@link UIForSeparator} UI builder instance which wraps the {@link JSeparator} and exposes helpful methods.
     */
    public static UIForSeparator<JSeparator> separator() { 
        return new UIForSeparator<>(new BuilderState<>(JSeparator.class, ()->new JSeparator()));
    }

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
        return separator().withOrientation(align);
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
        return separator().withOrientation(align);
    }

    /**
     *  This returns a {@link JButton} swing tree builder.
     *
     * @param component The button component which ought to be wrapped by the swing tree UI builder.
     * @param <T> The concrete type of this new component.
     * @return A basic UI {@link JButton} builder instance.
     */
    public static <T extends AbstractButton> UIForButton<T> of( T component )
    {
        NullUtil.nullArgCheck(component, "component", AbstractButton.class);
        return new UIForButton<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component without any text displayed on top.
     *  This is in essence a convenience method for {@code UI.of(new JButton())}.
     *
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button() {
        return new UIForButton<>(new BuilderState<>(Button.class, ()->new Button()));
    }

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
     * @param text The text property to be displayed on top of the button.
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
     * @param icon The icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().withIcon(icon);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *
     * @param icon The icon to be displayed on top of the button.
     * @param fit The fit mode of the icon.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( ImageIcon icon, FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(fit, "fit", FitComponent.class);
        return button().withIcon(icon, fit);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *
     * @param icon The desired icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map(UI::button).orElseGet(UI::button);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *
     * @param icon The desired icon to be displayed on top of the button.
     * @param fit The fit mode of the icon.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon, FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(fit, "fit", FitComponent.class);
        return icon.find().map( it -> button(it, fit) ).orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top which should be scaled to the provided dimensions.
     *  This is in essence a convenience method for {@code UI.of(new JButton()).peek( it -> it.setIcon(icon) )}.
     *
     * @param width The width the icon should be scaled to.
     * @param height The height the icon should be scaled to.
     * @param icon The icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, ImageIcon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return button().withIcon(width, height, icon);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with an icon displayed on top which should be scaled to the provided dimensions.
     *  The icon is determined based on the provided {@link IconDeclaration}
     *  instance which is conceptually merely a resource path to the icon.
     *
     * @param width The width the icon should be scaled to.
     * @param height The height the icon should be scaled to.
     * @param icon The desired icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map( it -> button(width, height, it) ).orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a dynamically displayed icon on top.
     *  <p>
     *  Note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The icon property whose value ought to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> buttonWithIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon");
        return button().withIcon(icon);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top.
     *
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( Icon icon, Icon onHover ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(onHover, "onHover", Icon.class);
        return button(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top.
     *  The icons are determined based on the provided {@link IconDeclaration}
     *  instances which is conceptually merely a resource paths to the icons.
     *
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon, IconDeclaration onHover ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(onHover, "onHover", IconDeclaration.class);
        return icon.find()
                   .flatMap( it -> onHover.find().map( it2 -> button(it, it2) ) )
                   .orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top
     *  which should both be scaled to the provided dimensions.
     *
     * @param width The width the icons should be scaled to.
     * @param height The height the icons should be scaled to.
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, ImageIcon icon, ImageIcon onHover ) {
        NullUtil.nullArgCheck(icon, "icon", ImageIcon.class);
        NullUtil.nullArgCheck(onHover, "onHover", ImageIcon.class);
        float scale = UI.scale();

        int scaleHint = Image.SCALE_SMOOTH;
        if ( scale > 1.5f )
            scaleHint = Image.SCALE_FAST;

        width  = (int) (width * scale);
        height = (int) (height * scale);

        onHover = new ImageIcon(onHover.getImage().getScaledInstance(width, height, scaleHint));
        icon = new ImageIcon(icon.getImage().getScaledInstance(width, height, scaleHint));
        return button(icon, onHover, onHover);
    }

    /**
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default icon as well as a hover icon displayed on top
     *  which should both be scaled to the provided dimensions.
     *  The icons are determined based on the provided {@link IconDeclaration}
     *  instances which is conceptually merely a resource paths to the icons.
     *
     * @param width The width the icons should be scaled to.
     * @param height The height the icons should be scaled to.
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( int width, int height, IconDeclaration icon, IconDeclaration onHover ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(onHover, "onHover", IconDeclaration.class);
        return icon.find()
                   .flatMap( it -> onHover.find().map( it2 -> button(width, height, it, it2) ) )
                   .orElseGet( UI::button );
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
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @param onPress The pressed icon to be displayed on top of the button.
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
     *  Use this to create a builder for the {@link JButton} UI component
     *  with a default, an on-hover and an on-press icon displayed on top.
     *  The icons are determined based on the provided {@link IconDeclaration}
     *  instances which is conceptually merely a resource paths to the icons.
     *
     * @param icon The default icon to be displayed on top of the button.
     * @param onHover The hover icon to be displayed on top of the button.
     * @param onPress The pressed icon to be displayed on top of the button.
     * @return A builder instance for a {@link JButton}, which enables fluent method chaining.
     */
    public static UIForButton<JButton> button( IconDeclaration icon, IconDeclaration onHover, IconDeclaration onPress ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(onHover, "onHover", IconDeclaration.class);
        NullUtil.nullArgCheck(onPress, "onPress", IconDeclaration.class);
        return icon.find()
                   .flatMap( it -> onHover.find().flatMap( it2 -> onPress.find().map( it3 -> button(it, it2, it3) ) ) )
                   .orElseGet( UI::button );
    }

    /**
     *  Use this to create a builder for the {@link JSplitButton} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSplitButton())}.
     *
     * @param splitButton The split button component which ought to be wrapped by the swing tree UI builder.
     * @param <B> The concrete type of this new component.
     * @return A builder instance for a {@link JSplitButton}, which enables fluent method chaining.
     */
    public static <B extends JSplitButton> UIForSplitButton<B> of( B splitButton ) {
        NullUtil.nullArgCheck(splitButton, "splitButton", JSplitButton.class);
        return new UIForSplitButton<>(new BuilderState<>(splitButton));
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
        return new UIForSplitButton<>(new BuilderState<>(JSplitButton.class, SplitButton::new))
                    .withText(text);
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
     * @param <E> The type of the {@link Enum} representing the selectable options.
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
     * @param <E> The type of the {@link Enum} representing the selectable options.
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
     * @param text The text property to dynamically display text on the {@link JMenuItem} exposed by the {@link swingtree.components.JSplitButton}s {@link JPopupMenu}.
     * @return A new {@link SplitItem} wrapping a simple {@link JMenuItem}.
     */
    public static SplitItem<JMenuItem> splitItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return SplitItem.of(text);
    }

    /**
     *  Use this to add radio item entries to the {@link swingtree.components.JSplitButton} by
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
     * @param text The text displayed on the {@link JRadioButtonMenuItem} exposed by the {@link swingtree.components.JSplitButton}s {@link JPopupMenu}.
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
        return new UIForTabbedPane<>(new BuilderState<>(pane));
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
     *      .add(UI.tab("three").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     *
     * @return A builder instance for a new {@link JTabbedPane}, which enables fluent method chaining.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane() {
        return new UIForTabbedPane<>(new BuilderState<>(JTabbedPane.class, TabbedPane::new));
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link Side} applied to the tab buttons
     *  (see {@link JTabbedPane#setTabLayoutPolicy(int)}).
     *  In order to add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *
     *  <pre>{@code
     *      UI.tabbedPane(Position.RIGHT)
     *      .add(UI.tab("first").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param tabsSide The position of the tab buttons which may be {@link Side#TOP}, {@link Side#RIGHT}, {@link Side#BOTTOM}, {@link Side#LEFT}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPosition} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( Side tabsSide) {
        NullUtil.nullArgCheck(tabsSide, "tabsPosition", Side.class);
        return tabbedPane().withTabPlacementAt(tabsSide);
    }

    /**
     *  Use this to create a builder for a new {@link JTabbedPane} UI component
     *  with the provided {@link OverflowPolicy} and {@link Side} applied to the tab buttons
     *  (see {@link JTabbedPane#setTabLayoutPolicy(int)} and {@link JTabbedPane#setTabPlacement(int)}).
     *  In order to add tabs to this builder use the tab object returned by {@link #tab(String)}
     *  like so:
     *  <pre>{@code
     *      UI.tabbedPane(Position.LEFT, OverflowPolicy.WRAP)
     *      .add(UI.tab("First").add(UI.panel().add(..)))
     *      .add(UI.tab("second").withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *
     * @param tabsSide The position of the tab buttons which may be {@link Side#TOP}, {@link Side#RIGHT}, {@link Side#BOTTOM}, {@link Side#LEFT}.
     * @param tabsPolicy The overflow policy of the tab buttons which can either be {@link OverflowPolicy#SCROLL} or {@link OverflowPolicy#WRAP}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPosition} or {@code tabsPolicy} are {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane(Side tabsSide, OverflowPolicy tabsPolicy ) {
        NullUtil.nullArgCheck(tabsSide, "tabsPosition", Side.class);
        NullUtil.nullArgCheck(tabsPolicy, "tabsPolicy", OverflowPolicy.class);
        return tabbedPane().withTabPlacementAt(tabsSide).withOverflowPolicy(tabsPolicy);
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
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
     *  }</pre>
     *  
     * @param tabsPolicy The overflow policy of the tab button which can either be {@link OverflowPolicy#SCROLL} or {@link OverflowPolicy#WRAP}.
     * @return A builder instance wrapping a new {@link JTabbedPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code tabsPolicy} is {@code null}.
     */
    public static UIForTabbedPane<JTabbedPane> tabbedPane( OverflowPolicy tabsPolicy ) {
        NullUtil.nullArgCheck(tabsPolicy, "tabsPolicy", OverflowPolicy.class);
        return tabbedPane().withTabPlacementAt(Side.TOP).withOverflowPolicy(tabsPolicy);
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
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
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
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
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
     *      .add(UI.tab("third").withIcon(someIcon).add(UI.button("click me")))
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
     *      .add(UI.tab(property3).withIcon(someIcon).add(UI.button("click me")))
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
        NullUtil.nullArgCheck(component, "component", JComponent.class);
        return new Tab(null, component, null, null, null, null, null, null, null);
    }

    /**
     *  Use this to add tabs to a {@link JTabbedPane} by
     *  passing {@link Tab} instances to {@link UIForTabbedPane} builder like so: <br>
     *  <pre>{@code
     *      UI.tabbedPane()
     *      .add(UI.tab(UI.button("X")).add(UI.panel().add(..)))
     *      .add(UI.tab(UI.label("Hi!")).withTip("I give info!").add(UI.label("read me")))
     *      .add(UI.tab(UI.of(...)).withIcon(someIcon).add(UI.button("click me")))
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
     * @param component The {@link JMenu} component which should be wrapped by the swing tree UI builder designed for menus.
     * @return A builder instance for the provided {@link JMenu}, which enables fluent method chaining.
     * @param <M> The concrete type of the menu.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenu> UIForMenu<M> of( M component ) {
        NullUtil.nullArgCheck(component, "component", JMenu.class);
        return new UIForMenu<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for the provided {@link JMenuItem} instance.
     *
     * @param component The {@link JMenuItem} component which should be wrapped by the swing tree UI builder designed for menu items.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     * @param <M> The type parameter of the concrete menu item component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <M extends JMenuItem> UIForMenuItem<M> of( M component ) {
        NullUtil.nullArgCheck(component, "component", JMenuItem.class);
        return new UIForMenuItem<>(new BuilderState<>(component));
    }

    /**
     * @return A SwingTree builder node for the {@link JMenuItem} type.
     */
    public static UIForMenuItem<JMenuItem> menuItem() {
        return new UIForMenuItem<>(new BuilderState<>(JMenuItem.class, MenuItem::new));
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
        return menuItem().withText(text);
    }

    /**
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        return menuItem().withText(text);
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
        return menuItem()
                    .withText(text)
                    .withIcon(icon);
    }

    /**
     *  Use this factory method to create a {@link JMenuItem} with the
     *  provided text and default icon based on the provided {@link IconDeclaration}. <br>
     *  Here an example demonstrating the usage of this method: <br>
     *  <pre>{@code
     *    UI.menuItem("Hello", Icons.MY_ICON)
     *    .withTip("I give info!")
     *    .onClick( it -> {...} )
     *  }</pre>
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text, IconDeclaration icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return menuItem()
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
        return menuItem()
                    .withText(text)
                    .withIcon(icon);
    }

    /**
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text, IconDeclaration icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return menuItem()
                    .withText(text)
                    .withIcon(icon);
    }

    /**
     *  Allows you to create a menu item with an icon property bound to it.
     *  So when the property state changes to a different icon, then so does the
     *  icon displayed on top of the menu item.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param text The text which should be displayed on the wrapped {@link JMenuItem}.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( String text, Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        return menuItem().withText(text).withIcon(icon);
    }

    /**
     *  Allows you to create a menu item with a text property and
     *  an icon property bound to it.
     *  So when the text or con property state changes to a different text or icon, then so does the
     *  text and/or icon displayed on top of the menu item.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param text The text property which should be displayed on the wrapped {@link JMenuItem} dynamically.
     * @param icon The icon which should be displayed on the wrapped {@link JMenuItem}.
     * @return A builder instance for the provided {@link JMenuItem}, which enables fluent method chaining.
     */
    public static UIForMenuItem<JMenuItem> menuItem( Val<String> text, Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        return menuItem().withText(text).withIcon(icon);
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
        return new UIForRadioButtonMenuItem<>(new BuilderState<>(radioMenuItem));
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
        return new UIForRadioButtonMenuItem<>(new BuilderState<>(JRadioButtonMenuItem.class, RadioButtonMenuItem::new));
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
     * @param <E> The type of the enum.
     * @throws IllegalArgumentException if {@code state} or {@code property} are {@code null}.
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
        return new UIForCheckBoxMenuItem<>(new BuilderState<>(checkBoxMenuItem));
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
        return new UIForCheckBoxMenuItem<>(new BuilderState<>(JCheckBoxMenuItem.class, CheckBoxMenuItem::new));
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
        return new UIForToolBar<>(new BuilderState<>(component));
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
        return new UIForToolBar<>(new BuilderState<>(JToolBar.class, ToolBar::new));
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
        return toolBar().withOrientation(align);
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
        return toolBar().withOrientation(align);
    }

    /**
     *  Use this to create a builder for the provided {@link JScrollPane} component.
     *
     * @param component The {@link JScrollPane} component which should be represented by the returned builder.
     * @param <P> The type parameter defining the concrete scroll pane type.
     * @return A {@link UIForScrollPane} builder representing the provided component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JScrollPane> UIForScrollPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPane<>(new BuilderState<>(component));
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
    public static UIForScrollPane<JScrollPane> scrollPane() {
        return new UIForScrollPane<>(new BuilderState(ScrollPane.class, ScrollPane::new));
    }

    /**
     *  Use this to create a builder for the provided {@link JScrollPanels} component.
     *
     * @param component The {@link JScrollPanels} component which should be represented by the returned builder.
     * @param <P> The type parameter defining the concrete scroll panels type.
     * @return A {@link UIForScrollPanels} builder representing the provided component.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JScrollPanels> UIForScrollPanels<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JScrollPane.class);
        return new UIForScrollPanels<>(new BuilderState<>(component));
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
        return new UIForScrollPanels<>(new BuilderState<>(JScrollPanels.class, ()->JScrollPanels.of(Align.VERTICAL, new Dimension(100,100))));
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
        return new UIForScrollPanels<>(new BuilderState<>(JScrollPanels.class, ()->JScrollPanels.of(align, null)));
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
        return new UIForScrollPanels<>(new BuilderState<>(JScrollPanels.class, ()->JScrollPanels.of(align, size)));
    }

    /**
     *  Use this to create a builder for the provided {@link JSplitPane} instance.
     *
     * @param component The {@link JSplitPane} instance to create a builder for.
     * @param <P> The type of the {@link JSplitPane} instance.
     * @return A builder instance for the provided {@link JSplitPane}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JSplitPane> UIForSplitPane<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JSplitPane.class);
        return new UIForSplitPane<>(new BuilderState<>(component));
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
        return new UIForSplitPane<>(new BuilderState<>(JSplitPane.class, ()->new SplitPane(align)))
                    .withOrientation(align);
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
        return new UIForSplitPane<>(new BuilderState<>(JSplitPane.class, ()->new SplitPane(align.get())))
                .withOrientation(align);
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
        return new UIForEditorPane<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JEditorPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JEditorPane())}.
     *
     * @return A builder instance for a new {@link JEditorPane}, which enables fluent method chaining.
     */
    public static UIForEditorPane<JEditorPane> editorPane() {
        return new UIForEditorPane<>(new BuilderState<>(JEditorPane.class, EditorPane::new));
    }

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
        return new UIForTextPane<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a new {@link JTextPane} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextPane())}.
     *
     * @return A builder instance for a new {@link JTextPane}, which enables fluent method chaining.
     */
    public static UIForTextPane<JTextPane> textPane() {
        return new UIForTextPane<>(new BuilderState<>(TextPane.class, ()->new TextPane()));
    }

    /**
     *  Use this to create a builder for the provided {@link JSlider} instance.
     *
     * @param component The {@link JSlider} instance to create a builder for.
     * @param <S> The type of the {@link JSlider} instance.
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <S extends JSlider> UIForSlider<S> of( S component ) {
        NullUtil.nullArgCheck(component, "component", JSlider.class);
        return new UIForSlider<>(new BuilderState<>(component));
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
        return new UIForSlider<>(new BuilderState<>(JSlider.class, Slider::new))
                    .withOrientation(align);
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
        return new UIForSlider<>(new BuilderState<>(JSlider.class, Slider::new))
                .withOrientation(align);
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
        return new UIForSlider<>(new BuilderState<>(JSlider.class, Slider::new))
                    .withOrientation(align)
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
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
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
        return new UIForSlider<>(new BuilderState<>(JSlider.class, Slider::new))
                .withOrientation(align)
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
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
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
        return new UIForSlider<>(new BuilderState<>(JSlider.class, Slider::new))
                .withOrientation(align)
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
     * @return A builder instance for the provided {@link JSlider}, which enables fluent method chaining.
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
        return new UIForSlider<>(new BuilderState<>(JSlider.class, Slider::new))
                .withOrientation(align)
                .withMin(min)
                .withMax(max)
                .withValue(value);
    }

    /**
     *  Use this to create a builder for the provided {@link JComboBox} instance.
     *
     * @param component The {@link JComboBox} instance to create a builder for.
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @param <C> The type of the {@link JComboBox} instance.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     */
    public static <E, C extends JComboBox<E>> UIForCombo<E,C> of( C component ) {
        NullUtil.nullArgCheck(component, "component", JComboBox.class);
        return new UIForCombo<>(new BuilderState<>(component));
    }

    /**
     *  Use this to declare a builder for a new {@link JComboBox} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JComboBox())}.
     *
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @return A builder instance for a new {@link JComboBox}, which enables fluent method chaining.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox() {
        return new UIForCombo<>(new BuilderState<>(JComboBox.class, ComboBox::new));
    }

    /**
     *  Use this to declare a UI builder for the {@link JComboBox} component type
     *  with the provided array of elements as selectable items. <br>
     *  Note that the user may modify the items in the provided array
     *  (if the combo box is editable), if you do not want that,
     *  consider using {@link UI#comboBoxWithUnmodifiable(Object[])}
     *  or {@link UI#comboBoxWithUnmodifiable(java.util.List)}.
     *
     * @param items The array of elements to be selectable in the {@link JComboBox}.
     * @param <E> The type of the elements in the {@link JComboBox}.
     * @return A builder instance for the new {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    @SafeVarargs
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( E... items ) {
        NullUtil.nullArgCheck(items, "items", Object[].class);
        return ((UIForCombo)comboBox()).withModel(new ArrayBasedComboModel<>(items));
    }

    /**
     *  Use this to declare a UI builder for the {@link JComboBox} type
     *  with the provided array of elements as selectable items which
     *  may not be modified by the user.
     *
     * @param items The unmodifiable array of elements to be selectable in the {@link JList}.
     * @param <E> The type of the elements in the {@link JComboBox}.
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
        NullUtil.nullArgCheck(selectedItem, "selectedItem", Var.class);
        // We get an array of possible enum states from the enum class
        return comboBox(selectedItem.type().getEnumConstants()).withSelectedItem(selectedItem);
    }

    /**
     *  Use this to declare a builder for a new {@link JComboBox} instance
     *  with the provided list of elements as selectable items.
     *
     * @param items The list of elements to be selectable in the {@link JComboBox}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( java.util.List<E> items ) {
        NullUtil.nullArgCheck(items, "items", List.class);
        return ((UIForCombo)comboBox()).withItems(items);
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
        return ((UIForCombo)comboBox()).withItems(selection, items);
    }

    /**
     *  Use this to create a builder for a new  {@link JComboBox} instance
     *  with the provided properties list object as selectable (and mutable) items.
     *
     * @param items The {@link Vars} properties of elements to be selectable in the {@link JComboBox}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for the provided {@link JComboBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <E> UIForCombo<E,JComboBox<E>> comboBox( Vars<E> items ) {
        NullUtil.nullArgCheck(items, "items", Vars.class);
        return ((UIForCombo)comboBox()).withItems(items);
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
        return ((UIForCombo)comboBox()).withItems(items);
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
        return ((UIForCombo)comboBox()).withItems(selection, items);
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
        return ((UIForCombo)comboBox()).withItems(selection, items);
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
        return ((UIForCombo)comboBox()).withItems(var, items);
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
        return ((UIForCombo)comboBox()).withItems(var, items);
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
        return ((UIForCombo)comboBox()).withItems(selectedItem, items);
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
        return (UIForCombo)comboBox().peek( c -> ((JComboBox)c).setModel(model) );
    }

    /**
     *  Use this to create a builder for the provided {@link JSpinner} instance.
     *
     * @param spinner The {@link JSpinner} instance to create a builder for.
     *                The provided {@link JSpinner} instance must not be {@code null}.
     * @param <S> The type parameter of the concrete {@link JSpinner} subclass to be used by the builder.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code spinner} is {@code null}.
     */
    public static <S extends JSpinner> UIForSpinner<S> of( S spinner ) {
        NullUtil.nullArgCheck(spinner, "spinner", JSpinner.class);
        return new UIForSpinner<>(new BuilderState<>(spinner));
    }

    /**
     *  Use this to create a builder for a new {@link JSpinner} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JSpinner())}.
     *
     * @return A builder instance for a new {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<JSpinner> spinner() {
        return new UIForSpinner<>(new BuilderState<>(Spinner.class, ()->new Spinner()));
    }

    /**
     * Use this to create a builder for the provided {@link JSpinner} instance
     * with the provided {@link SpinnerModel} as the model.
     *
     * @param model The {@link SpinnerModel} to be used by the {@link JSpinner}.
     * @return A builder instance for the provided {@link JSpinner}, which enables fluent method chaining.
     */
    public static UIForSpinner<javax.swing.JSpinner> spinner( SpinnerModel model ) {
        NullUtil.nullArgCheck(model, "model", SpinnerModel.class);
        return new UIForSpinner<>(new BuilderState<JSpinner>(Spinner.class, Spinner::new))
                .peek( s -> s.setModel(model) );
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
        return new UIForSpinner<>(new BuilderState<JSpinner>(Spinner.class, Spinner::new))
                .peek( s -> s.setModel(new SpinnerNumberModel(value, min, max, step)) );
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
        return new UIForSpinner<>(new BuilderState<JSpinner>(Spinner.class, Spinner::new))
                .peek( s -> s.setModel(new SpinnerNumberModel(value, min, max, 1)) );
    }

    /**
     *  Use this to create a builder for the provided {@link JLabel} instance.
     *
     * @param label The {@link JLabel} instance to be used by the builder.
     * @param <L> The type parameter of the concrete {@link JLabel} subclass to be used by the builder.
     * @return A builder instance for the provided {@link JLabel}, which enables fluent method chaining.
     */
    public static <L extends JLabel> UIForLabel<L> of( L label ) {
        NullUtil.nullArgCheck(label, "component", JLabel.class);
        return new UIForLabel<>(new BuilderState<>(label));
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
        return _label().withText(text);
    }

    private static UIForLabel<JLabel> _label() {
        return new UIForLabel<>(new BuilderState<>(Label.class, Label::new));
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(text, alignment))}.
     *
     * @param text The text which should be displayed on the label.
     * @param alignment The horizontal alignment of the text.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( String text, HorizontalAlignment alignment ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(alignment, "alignment", HorizontalAlignment.class);
        return _label().withText(text).withHorizontalAlignment( alignment );
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *
     * @param text The text which should be displayed on the label.
     * @param alignment The vertical and horizontal alignment of the text.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( String text, Alignment alignment ) {
        NullUtil.nullArgCheck(text, "text", String.class);
        NullUtil.nullArgCheck(alignment, "alignment", Alignment.class);
        return _label().withText(text).withAlignment( alignment );
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
        return _label()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for the {@link JLabel} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JLabel(Val<String> text, alignment)}.
     *
     * @param text The text property which should be bound to the label.
     * @param alignment The horizontal alignment of the text.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( Val<String> text, HorizontalAlignment alignment ) {
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        NullUtil.nullArgCheck(alignment, "alignment", HorizontalAlignment.class);
        return _label()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text)
                .withHorizontalAlignment( alignment );
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return _label().withIcon(icon);
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *  The icon is specified by a {@link IconDeclaration} which
     *  is essentially just a path to an icon resource.
     *  If the icon cannot be found, the label will be empty.
     *  Note that loaded icons are cached, so if you load the same icon multiple times,
     *  the same icon instance will be used (see {@link SwingTree#getIconCache()}).
     *
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map( UI::label ).orElseGet( () -> label("") );
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon dynamically.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The icon property which should dynamically provide a desired icon for the {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> labelWithIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "Null icons are not allowed!");
        return _label().withIcon(icon);
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
        float scale = UI.scale();

        int scaleHint = Image.SCALE_SMOOTH;
        if ( scale > 1.5f )
            scaleHint = Image.SCALE_FAST;

        width  = (int) (width * scale);
        height = (int) (height * scale);

        Image scaled = icon.getImage().getScaledInstance(width, height, scaleHint);
        return _label()
                .withIcon(new ImageIcon(scaled));
    }

    /**
     *  Use this to create a UI builder for a text-less label containing and displaying an icon.
     *  The icon is specified by a {@link IconDeclaration} which
     *  is essentially just a path to an icon resource.
     *  If the icon cannot be found, the label will be empty.
     *  Note that loaded icons are cached, so if you load the same icon multiple times,
     *  the same icon instance will be used (see {@link SwingTree#getIconCache()}).
     *
     * @param width The width of the icon when displayed on the label.
     * @param height The height of the icon when displayed on the label.
     * @param icon The icon which should be placed into a {@link JLabel}.
     * @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> label( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map( i -> label(width, height, i) ).orElseGet( () -> label("") );
    }

    /**
     *  Use this to create a UI builder for a {@link JLabel} with bold font.
     *  This is in essence a convenience method for {@code UI.label(String text).makeBold()}.
     *  @param text The text which should be displayed on the label.
     *  @return A builder instance for the label, which enables fluent method chaining.
     */
    public static UIForLabel<JLabel> boldLabel( String text ) {
        return _label().withText(text).makeBold();
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
        return _label().withText(text).makeBold();
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
        return _label().withText("<html>" + text + "</html>");
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
        return _label()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text.view( it -> "<html>" + it + "</html>"));
    }

    /**
     *  Use this to create a builder for the provided {@link swingtree.components.JIcon} instance.
     *
     * @param icon The {@link swingtree.components.JIcon} instance to be used by the builder.
     * @param <I> The type of the {@link swingtree.components.JIcon} instance.
     * @return A builder instance for the provided {@link swingtree.components.JIcon}, which enables fluent method chaining.
     */
    public static <I extends JIcon> UIForIcon<I> of( I icon ) {
        NullUtil.nullArgCheck(icon, "icon", JIcon.class);
        return new UIForIcon<>(new BuilderState<>(icon));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the provided
     *  icon displayed on it.
     *
     * @param icon The icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(icon)));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the
     *  path provided by the supplied {@link IconDeclaration} displayed on it.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param icon The icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(icon)));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the
     *  provided icon scaled to the provided width and height.
     *
     * @param width The width of the icon when displayed on the {@link JIcon}.
     * @param height The height of the icon when displayed on the {@link JIcon}.
     * @param icon The icon which should be placed into a {@link JIcon} for display.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( int width, int height, Icon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        float scale = UI.scale();

        int scaleHint = Image.SCALE_SMOOTH;
        if ( scale > 1.5f )
            scaleHint = Image.SCALE_FAST;

        width  = (int) (width * scale);
        height = (int) (height * scale);

        if ( icon instanceof SvgIcon ) {
            SvgIcon svgIcon = (SvgIcon) icon;
            svgIcon = svgIcon.withIconSize(width, height);
            return UI.icon(svgIcon);
        } else {
            Image scaled = ((ImageIcon) icon).getImage().getScaledInstance(width, height, scaleHint);
            return UI.icon(new ImageIcon(scaled));
        }
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the
     *  path defined by the supplied {@link IconDeclaration} displayed on it and scaled to the
     *  provided width and height.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param width The width of the icon when displayed on the {@link JIcon}.
     * @param height The height of the icon when displayed on the {@link JIcon}.
     * @param icon The icon which should be placed into a {@link JIcon} for display.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon is null.
     */
    public static UIForIcon<JIcon> icon( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return icon.find().map( i -> icon(width, height, i) ).orElseGet( () -> icon("") );
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the provided
     *  path displayed on it and scaled to the provided width and height.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param width The width of the icon when displayed on the {@link JIcon}.
     * @param height The height of the icon when displayed on the {@link JIcon}.
     * @param iconPath The path to the icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon path is null.
     */
    public static UIForIcon<JIcon> icon( int width, int height, String iconPath ) {
        NullUtil.nullArgCheck(iconPath, "iconPath", String.class);
        return icon(width, height, findIcon(iconPath).orElse(null));
    }

    /**
     *  Creates a builder node wrapping a new {@link JIcon} instance with the icon found at the provided
     *  path displayed on it.
     *  Note that the icon will be cached by the {@link JIcon} instance, so that it will not be reloaded.
     *
     * @param iconPath The path to the icon which should be displayed on the {@link JIcon}.
     * @return A builder instance for the icon, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided icon path is null.
     */
    public static UIForIcon<JIcon> icon( String iconPath ) {
        NullUtil.nullArgCheck(iconPath, "iconPath", String.class);
        return new UIForIcon<>(new BuilderState<>(JIcon.class, ()->new JIcon(iconPath)));
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
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(CheckBox.class, CheckBox::new))
                .withText(text);
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
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(CheckBox.class, CheckBox::new))
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
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(CheckBox.class, CheckBox::new))
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
        return new UIForCheckBox<>(new BuilderState<JCheckBox>(CheckBox.class, CheckBox::new))
                .applyIf(!isChecked.hasNoID(), it -> it.id(isChecked.id()))
                .withText(text)
                .isSelectedIf(isChecked);
    }

    /**
     *  Use this to create a builder for the provided {@link JCheckBox} instance.
     *
     * @param component The {@link JCheckBox} instance to be used by the builder.
     * @param <B> The type parameter of the concrete {@link JCheckBox} subclass to be used by the builder.
     * @return A builder instance for the provided {@link JCheckBox}, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided checkbox is null.
     */
    public static <B extends JCheckBox> UIForCheckBox<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JCheckBox.class);
        return new UIForCheckBox<>(new BuilderState<>(component));
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
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(RadioButton.class, RadioButton::new))
                .withText(text);
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
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(RadioButton.class, RadioButton::new))
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
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(RadioButton.class, RadioButton::new))
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
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(RadioButton.class, RadioButton::new))
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
     * @param <E> The type of the enum which this {@link JToggleButton} should represent.
     * @return A builder instance for the radio button, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code selected} is {@code null}.
     */
    public static <E extends Enum<E>> UIForRadioButton<JRadioButton> radioButton( E state, Var<E> selection ) {
        NullUtil.nullArgCheck(state, "state", Enum.class);
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullPropertyCheck(selection, "selection", "The selection state of a radio button may not be modelled using null!");
        return new UIForRadioButton<>(new BuilderState<JRadioButton>(RadioButton.class, RadioButton::new))
                .applyIf(!selection.hasNoID(), it -> it.id(selection.id()))
                .isSelectedIf( state, selection );
    }

    /**
     *  Use this to create a builder for the provided {@link JRadioButton} instance.
     *
     * @param component The {@link JRadioButton} instance which should be wrapped by the builder.
     * @param <R> The type of the {@link JRadioButton} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JRadioButton}, which enables fluent method chaining.
     */
    public static <R extends JRadioButton> UIForRadioButton<R> of( R component ) {
        NullUtil.nullArgCheck(component, "component", JRadioButton.class);
        return new UIForRadioButton<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for a {@link JToggleButton} instance.
     *
     * @return A builder instance for a new {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton() {
        return new UIForToggleButton<>(new BuilderState<JToggleButton>(ToggleButton.class, ToggleButton::new));
    }

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
     *  <p>
     *  Note that the provided text property may not be null,
     *  and it is also not permitted to contain null values,
     *  instead use an empty string instead of null.
     *
     * @param text The text property which should be bound to the toggle button.
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

    public static UIForToggleButton<JToggleButton> toggleButton( ImageIcon icon, FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(fit, "fit", FitComponent.class);
        return toggleButton().withIcon(icon, fit);
    }

    /**
     *  Use this to create a builder for the {@link JToggleButton} UI component
     *  with an icon displayed on it scaled according to the provided width and height.
     *
     * @param width The width the icon should be scaled to.
     * @param height The height the icon should be scaled to.
     * @param icon The icon to be displayed on top of the button.
     * @return A builder instance for a {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( int width, int height, ImageIcon icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return toggleButton().withIcon(width, height, icon);
    }

    /**
     *  Use this to create a builder for the {@link JToggleButton} UI component
     *  with an icon displayed on it scaled according to the provided width and height.
     *
     * @param width The width the icon should be scaled to.
     * @param height The height the icon should be scaled to.
     * @param icon The {@link IconDeclaration} whose icon ought to be displayed on top of the button.
     * @return A builder instance for a {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( int width, int height, IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        return toggleButton().withIcon(width, height, icon);
    }

    /**
     * @param width The width the icon should be scaled to.
     * @param height The height the icon should be scaled to.
     * @param icon The {@link IconDeclaration} whose icon ought to be displayed on top of the button.
     * @param fit The {@link FitComponent} which determines how the icon should be fitted into the button.
     * @return A builder instance for a {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( int width, int height, IconDeclaration icon, FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", Icon.class);
        NullUtil.nullArgCheck(fit, "fit", FitComponent.class);
        return toggleButton().withIcon(width, height, icon, fit);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance with
     *  the icon found at the path provided by the supplied {@link IconDeclaration} displayed on top of it.
     *  Note that the icon will be cached by the {@link JToggleButton} instance, so that it will not be reloaded.
     *
     * @param icon The icon which should be displayed on the toggle button.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButton( IconDeclaration icon ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        return toggleButton().withIcon(icon);
    }

    public static UIForToggleButton<JToggleButton> toggleButton( IconDeclaration icon, FitComponent fit ) {
        NullUtil.nullArgCheck(icon, "icon", IconDeclaration.class);
        NullUtil.nullArgCheck(fit, "fit", FitComponent.class);
        return toggleButton().withIcon(icon, fit);
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
     *  the provided {@link IconDeclaration} based property dynamically
     *  displays the targeted image on the toggle button.
     *  <p>
     *  Note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     *
     * @param icon The icon property which should be bound to the toggle button.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButtonWithIcon( Val<IconDeclaration> icon ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "The icon of a toggle button may not be modelled using null!");
        return new UIForToggleButton<>(new BuilderState<>(ToggleButton.class, ()->new JToggleButton()))
                .applyIf(!icon.hasNoID(), it -> it.id(icon.id()))
                .withIcon(icon);
    }

    /**
     *  Use this to create a builder for a new {@link JToggleButton} instance where
     *  the provided {@link IconDeclaration} property dynamically displays its targeted icon on the toggle button
     *  and the provided boolean property dynamically determines whether the toggle button is selected or not.
     *  <p>
     *  But note that you may not use the {@link Icon} or {@link ImageIcon} classes directly,
     *  instead <b>you must use implementations of the {@link IconDeclaration} interface</b>,
     *  which merely models the resource location of the icon, but does not load
     *  the whole icon itself.
     *  <p>
     *  The reason for this distinction is the fact that traditional Swing icons
     *  are heavy objects whose loading may or may not succeed, and so they are
     *  not suitable for direct use in a property as part of your view model.
     *  Instead, you should use the {@link IconDeclaration} interface, which is a
     *  lightweight value object that merely models the resource location of the icon
     *  even if it is not yet loaded or even does not exist at all.
     *  <p>
     *  This is especially useful in case of unit tests for you view model,
     *  where the icon may not be available at all, but you still want to test
     *  the behaviour of your view model.
     *
     * @param icon The icon property which should be bound to the toggle button.
     * @param isToggled The boolean property which should be bound to the toggle button and determines whether it is selected or not.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static UIForToggleButton<JToggleButton> toggleButtonWithIcon( Val<IconDeclaration> icon, Var<Boolean> isToggled ) {
        NullUtil.nullArgCheck(icon, "icon", Val.class);
        NullUtil.nullPropertyCheck(icon, "icon", "The icon of a toggle button may not be modelled using null!");
        NullUtil.nullPropertyCheck(isToggled, "isToggled", "The selection state of a toggle button may not be modelled using null!");
        return new UIForToggleButton<>(new BuilderState<>(ToggleButton.class, ()->new JToggleButton()))
                .applyIf(!icon.hasNoID(), it -> it.id(icon.id()))
                .applyIf(!isToggled.hasNoID(), it -> it.id(isToggled.id()))
                .withIcon(icon)
                .isSelectedIf(isToggled);
    }

    /**
     *  Use this to create a builder for the provided {@link JToggleButton} instance.
     *
     * @param component The {@link JToggleButton} instance which should be wrapped by the builder.
     * @param <B> The type of the {@link JToggleButton} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JToggleButton}, which enables fluent method chaining.
     */
    public static <B extends JToggleButton> UIForToggleButton<B> of( B component ) {
        NullUtil.nullArgCheck(component, "component", JToggleButton.class);
        return new UIForToggleButton<>(new BuilderState<>(component));
    }

    /**
     *  Use this to create a builder for the provided {@link JTextField} instance.
     *
     * @param component The {@link JTextField} instance which should be wrapped by the builder.
     * @param <F> The type of the {@link JTextField} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException If the provided text field is null.
     */
    public static <F extends JTextField> UIForTextField<F> of( F component ) {
        NullUtil.nullArgCheck(component, "component", JTextComponent.class);
        return new UIForTextField<>(new BuilderState<>(component));
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
        return textField().withText(text);
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
        return textField()
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
        return textField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JTextField())}.
     *
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField() {
        return new UIForTextField<>(new BuilderState<JTextField>(TextField.class, TextField::new));
    }

    /**
     *  A convenience method for creating a builder for a {@link JTextField} with a certain text alignment.
     *  This is a shortcut version for the following code:
     *  <pre>{@code
     *      UI.textField()
     *          .withTextOrientation(UI.HorizontalDirection.RIGHT);
     *  }</pre>
     *
     * @param direction The text orientation type which should be used.
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( HorizontalAlignment direction ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalAlignment.class);
        return textField().withTextOrientation(direction);
    }

    /**
     *  A convenience method for creating a builder for a {@link JTextField} with a certain text and text alignment.
     *  This is a shortcut version for the following code:
     *  <pre>{@code
     *      UI.textField()
     *          .withTextOrientation(UI.HorizontalDirection.LEFT)
     *          .withText(text);
     *  }</pre>
     *
     * @param direction The text orientation type which should be used.
     * @param text The new text to be set for the wrapped text component type.
     * @return A builder instance for a new {@link JTextField}, which enables fluent method chaining.
     */
    public static UIForTextField<JTextField> textField( HorizontalAlignment direction, String text ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalAlignment.class);
        return textField().withTextOrientation(direction).withText(text);
    }

    public static UIForTextField<JTextField> textField( HorizontalAlignment direction, Val<String> text ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalAlignment.class);
        NullUtil.nullArgCheck(text, "text", Val.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return textField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withTextOrientation(direction)
                .withText(text);
    }

    public static UIForTextField<JTextField> textField( HorizontalAlignment direction, Var<String> text ) {
        NullUtil.nullArgCheck(direction, "direction", HorizontalAlignment.class);
        NullUtil.nullArgCheck(text, "text", Var.class);
        NullUtil.nullPropertyCheck(text, "text", "Please use an empty string instead of null!");
        return textField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withTextOrientation(direction)
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field.
     *  The property is a {@link Var}, meaning that it can be modified by the user.
     *  <p>
     *  The number property will only receive values if the text field contains a valid number.
     *  <p>
     *  Also note that the provided property is not allowed to contain {@code null} values,
     *  as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field
     *  and a function which will be used to format the number as a string.
     *  <p>
     *  The number property will only receive values if the text in the text field can be parsed as a number,
     *  in which case the provided formatter function will be used to convert the number to a string.
     *  <p>
     *  Note that the provided property is not allowed to contain {@code null} values,
     *  as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param formatter The function which will be used to format the number as a string.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number, Function<N,String> formatter ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullArgCheck(formatter, "formatter", Function.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number, formatter);
    }


    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field
     *  and a boolean property which will be set to {@code true} if the text field contains a valid number,
     *  and {@code false} otherwise.
     *  <p>
     *  The number property will only receive values if the text in the text field can be parsed as a number,
     *  in which case the provided {@link Var} will be set to {@code true}, otherwise it will be set to {@code false}.
     *  <p>
     *  Note that the two provided properties are not permitted to
     *  contain {@code null} values, as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param isValid A {@link Var} which will be set to {@code true} if the text field contains a valid number,
     *                and {@code false} otherwise.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code number} is {@code null}.
     * @throws IllegalArgumentException if {@code isValid} is {@code null}.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number, Var<Boolean> isValid ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        NullUtil.nullArgCheck(isValid, "isValid", Var.class);
        NullUtil.nullPropertyCheck(isValid, "isValid", "Please use false instead of null!");
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number, isValid);
    }

    /**
     *  Use this to create a builder for a new {@link JTextField} instance with
     *  the provided number property dynamically displaying its value on the text field
     *  and a boolean property which will be set to {@code true} if the text field contains a valid number,
     *  and {@code false} otherwise.
     *  <p>
     *  The number property will only receive values if the text in the text field can be parsed as a number,
     *  in which case the provided {@link Var} will be set to {@code true}, otherwise it will be set to {@code false}.
     *  <p>
     *  Note that the two provided properties are not permitted to
     *  contain {@code null} values, as this would lead to a {@link NullPointerException} being thrown.
     *
     * @param number The number property which should be bound to the text field.
     * @param isValid A {@link Var} which will be set to {@code true} if the text field contains a valid number,
     *                and {@code false} otherwise.
     * @param formatter The function which will be used to format the number as a string.
     * @param <N> The type of the number property which should be bound to the text field.
     * @return A builder instance for the provided {@link JTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code number} is {@code null}.
     * @throws IllegalArgumentException if {@code isValid} is {@code null}.
     */
    public static <N extends Number> UIForTextField<JTextField> numericTextField( Var<N> number, Var<Boolean> isValid, Function<N,String> formatter ) {
        NullUtil.nullArgCheck(number, "number", Var.class);
        NullUtil.nullPropertyCheck(number, "number", "Please use 0 instead of null!");
        NullUtil.nullArgCheck(isValid, "isValid", Var.class);
        NullUtil.nullPropertyCheck(isValid, "isValid", "Please use false instead of null!");
        NullUtil.nullArgCheck(formatter, "formatter", Function.class);
        return textField()
                .applyIf( !number.hasNoID(), it -> it.id(number.id()) )
                .withNumber(number, isValid, formatter);
    }


    /**
     *  Use this to create a builder for the provided {@link JFormattedTextField} instance.
     *
     * @param component The {@link JFormattedTextField} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JFormattedTextField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static UIForFormattedTextField of( JFormattedTextField component ) {
        NullUtil.nullArgCheck(component, "component", JFormattedTextField.class);
        return new UIForFormattedTextField(new BuilderState<>(component));
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
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->{
            JFormattedTextField tf = new FormattedTextField();
            tf.setText(text);
            return tf;
        }));
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
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->new FormattedTextField()))
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
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->new FormattedTextField()))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JFormattedTextField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JFormattedTextField())}.
     *
     * @return A builder instance for a new {@link JFormattedTextField}, which enables fluent method chaining.
     */
    public static UIForFormattedTextField formattedTextField() {
        return new UIForFormattedTextField(new BuilderState<>(JFormattedTextField.class, ()->new FormattedTextField()));
    }

    /**
     *  Use this to create a builder for the provided {@link JPasswordField} instance.
     *
     * @param component The {@link JPasswordField} instance which should be wrapped by the builder.
     * @param <F> The type of the {@link JPasswordField} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JPasswordField}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <F extends JPasswordField> UIForPasswordField<F> of( F component ) {
        NullUtil.nullArgCheck(component, "component", JPasswordField.class);
        return new UIForPasswordField<>(new BuilderState<>(component));
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
        return new UIForPasswordField<>(new BuilderState<JPasswordField>(PasswordField.class, PasswordField::new))
                .withText(text);
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
        return new UIForPasswordField<>(new BuilderState<>(JPasswordField.class, PasswordField::new))
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
        return passwordField()
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     *  Use this to create a builder for a new {@link JPasswordField} UI component.
     *  This is in essence a convenience method for {@code UI.of(new JPasswordField())}.
     *
     * @return A builder instance for a new {@link JPasswordField}, which enables fluent method chaining.
     */
    public static UIForPasswordField<JPasswordField> passwordField() {
        return new UIForPasswordField<>(new BuilderState<>(JPasswordField.class, ()->new PasswordField()));
    }

    /**
     *  Use this to create a builder for the provided {@link JProgressBar} instance.
     *
     * @param component The {@link JProgressBar} instance which should be wrapped by the builder.
     * @param <P> The type of the {@link JProgressBar} instance which should be wrapped by the builder.
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     * @throws IllegalArgumentException if {@code component} is {@code null}.
     */
    public static <P extends JProgressBar> UIForProgressBar<P> of( P component ) {
        NullUtil.nullArgCheck(component, "component", JProgressBar.class);
        return new UIForProgressBar<>(new BuilderState<>(component));
    }

    /**
     *  A factory method for creating a progress bar builder with a default {@link JProgressBar} implementation.
     *
     * @return A builder instance for the provided {@link JProgressBar}, which enables fluent method chaining.
     */
    public static UIForProgressBar<JProgressBar> progressBar() {
        return new UIForProgressBar<>(new BuilderState<>(ProgressBar.class, ()->new ProgressBar()));
    }

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
        return progressBar().withOrientation(align).withMin(min).withMax(max);
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
        return progressBar().withOrientation(align).withMin(min).withMax(max).withValue(value);
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
        return progressBar().withOrientation(align).withMin(min).withMax(max).withValue(value);
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
        return progressBar().withOrientation(align).withMin(0).withMax(100).withProgress(progress);
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
        return progressBar().withOrientation(align).withMin(0).withMax(100).withProgress(progress);
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
        return progressBar().withOrientation(align).withMin(0).withMax(100).withProgress(progress);
    }

    /**
     *  Use this to create a builder for the provided {@link JTextArea} instance.
     *
     * @param area The {@link JTextArea} which should be wrapped by the builder.
     * @param <A> The type of the {@link JTextArea} for which the builder should be created.
     * @return A builder instance for the provided {@link JTextArea}, which enables fluent method chaining.
     */
    public static <A extends JTextArea> UIForTextArea<A> of( A area ) {
        NullUtil.nullArgCheck(area, "area", JTextArea.class);
        return new UIForTextArea<>(new BuilderState<>(area));
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
        return new UIForTextArea<>(new BuilderState<JTextArea>(TextArea.class, TextArea::new))
                .withText(text);
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
        return new UIForTextArea<>(new BuilderState<JTextArea>(TextArea.class, TextArea::new))
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
        return new UIForTextArea<>(new BuilderState<JTextArea>(TextArea.class, TextArea::new))
                .applyIf(!text.hasNoID(), it -> it.id(text.id()))
                .withText(text);
    }

    /**
     * @param list The {@link JList} which should be wrapped by the builder.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for the provided {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> of( JList<E> list ) {
        NullUtil.nullArgCheck(list, "list", JList.class);
        return new UIForList<>(new BuilderState<>(list));
    }

    /**
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> list() {
        return new UIForList<>(new BuilderState<>(List.class, List::new));
    }

    /**
     * @param model The model which should be used for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList}.
     */
    public static <E> UIForList<E, JList<E>> list( ListModel<E> model ) {
        NullUtil.nullArgCheck(model, "model", ListModel.class);
        return new UIForList<>(new BuilderState<>(List.class, ()->{
            JList<E> list = new List<>();
            list.setModel(model);
            return list;
        }));
    }

    /**
     *  Creates a new {@link JList} instance builder
     *  with the provided array as data model.
     *  This is functionally equivalent to {@link #listOf(Object...)}.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @param <E> The type of the elements in the list.
     * @return A builder instance for a new {@link JList} with the provided array as data model.
     */
    @SafeVarargs
    public static <E> UIForList<E, JList<E>> list( E... elements ) {
        NullUtil.nullArgCheck(elements, "elements", Object[].class);
        return new UIForList<>(new BuilderState<JList<E>>(List.class, ()->new List<E>()))
                .withEntries( elements );
    }

    /**
     *  Allows for the creation of a new {@link JList} instance with the provided
     *  observable property list (a {@link Vals} object) as data model.
     *  When the property list changes, the {@link JList} will be updated accordingly.
     *
     * @param elements The elements which should be used as model data for the new {@link JList}.
     * @return A builder instance for a new {@link JList} with the provided {@link Vals} as data model.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForList<E, JList<E>> list( Vals<E> elements ) {
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return new UIForList<>(new BuilderState<JList<E>>(List.class, ()->new List<E>()))
                .withEntries( elements );
    }

    /**
     *  Allows for the creation of a new {@link JList} instance with 2 observable
     *  collections as data model, a {@link Var} property for the selection and a {@link Vals}
     *  property list for the elements.
     *  When any of the properties change, the {@link JList} will be updated accordingly,
     *  and conversely, when the {@link JList} selection changes, the properties will be updated accordingly.
     *
     * @param selection The {@link Var} property which should be bound to the selection of the {@link JList}.
     * @param elements The {@link Vals} property which should be bound to the displayed elements of the {@link JList}.
     * @return A builder instance for a new {@link JList} with the provided arguments as data model.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForList<E, JList<E>> list( Var<E> selection, Vals<E> elements ) {
        NullUtil.nullArgCheck(selection, "selection", Var.class);
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return list( elements ).withSelection( selection );
    }

    /**
     *  Allows for the creation of a new {@link JList} instance with 2 observable
     *  collections as data model, a {@link Val} property for the selection and a {@link Vals}
     *  property list for the elements.
     *  When any of the properties change, the {@link JList} will be updated accordingly,
     *  however, due to the usage of a read only {@link Val} property for the selection,
     *  the {@link JList} selection will not be updated when the property changes.
     *  If you want a bidirectional binding, use {@link #list(Var, Vals)} instead.
     *
     * @param selection The {@link Val} property which should be bound to the selection of the {@link JList}.
     * @param elements The {@link Vals} property which should be bound to the displayed elements of the {@link JList}.
     * @return A builder instance for a new {@link JList} with the provided {@link Val} and {@link Vals} as data models.
     * @param <E> The type of the elements in the list.
     */
    public static <E> UIForList<E, JList<E>> list( Val<E> selection, Vals<E> elements ) {
        NullUtil.nullArgCheck(selection, "selection", Val.class);
        NullUtil.nullArgCheck(elements, "elements", Vals.class);
        return new UIForList<>(new BuilderState<JList<E>>(List.class, ()->new List<E>()))
                .withEntries( elements ).withSelection( selection );
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
     * @param entries The list of entries used for populating a new {@link JList} component.
     * @param <E> The type parameter defining the concrete type of the list entries.
     * @return A builder instance for a new {@link JList} with the provided {@link List} as data model.
     */
    public static <E> UIForList<E, JList<E>> list( java.util.List<E> entries ) {
        return new UIForList<>(new BuilderState<JList<E>>(List.class, ()->new List<E>()))
                .withEntries( entries );
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
        return new UIForTable<>(new BuilderState<>(table));
    }

    /**
     * @return A fluent builder instance for a new {@link JTable}.
     */
    public static UIForTable<JTable> table() {
        return new UIForTable<>(new BuilderState<>(Table.class, ()->new Table()));
    }

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
        return table().withModel(dataFormat, dataSource);
    }

    /**
     *  Use this to create a new {@link JTable} with a table model whose data can be represented based
     *  on a map of column names to lists of table entries (basically a column major matrix).  <br>
     *  This method will automatically create a {@link AbstractTableModel} instance for you.
     *  <p>
     *  <b>Please note that when the data of the provided data source changes (i.e. when the data source
     *  is a {@link Map} which gets modified), the table model will not be updated automatically!
     *  Use {@link UIForTable#updateTableOn(sprouts.Event)} to bind an update {@link Event} to the table model.</b>
     *
     * @param dataFormat An enum which configures the modifiability of the table in a readable fashion.
     * @param dataSource The {@link TableMapDataSource} returning a column major map based matrix which will be used to populate the table.
     * @return This builder node.
     * @param <E> The type of the table entry {@link Object}s.
     */
    public static <E> UIForTable<JTable> table( MapData dataFormat, TableMapDataSource<E> dataSource ) {
        NullUtil.nullArgCheck(dataFormat, "dataFormat", ListData.class);
        NullUtil.nullArgCheck(dataSource, "dataSource", TableMapDataSource.class);
        return table().withModel(dataFormat, dataSource);
    }

    /**
     *  Creates a new {@link JTable} instance builder with the provided table model
     *  buildable used for creating the table model in a declarative fashion. <br>
     *  It is expected to be used like so:
     *  <pre>{@code
     *  UI.table(
     *    UI.tableModel()
     *    .colCount( () -> data[0].size() )
     *    .rowCount( () -> data.size() )
     *    .getsEntryAt((col, row) -> data[col][row] )
     * )
     * }</pre>
     * The factory method {@link #tableModel()} is used to create a builder for the table model
     * which can be passed to this method, which will call the {@link BasicTableModel.Builder#build()}
     * method on the provided builder to create the table model for the table.
     * <br>
     * The purpose of this pattern is to remove the necessity of implementing the {@link javax.swing.table.TableModel}
     * interface manually, which is a rather tedious task.
     * Instead, you can use ths fluent API provided by the {@link BasicTableModel.Builder} to create
     * a general purpose table model for your table.
     *
     * @param tableModelBuildable The table model builder which can be created using the {@link #tableModel()} factory method.
     * @return A builder instance for a new {@link JTable}.
     */
    public static UIForTable<JTable> table( Buildable<BasicTableModel> tableModelBuildable ) {
        return table().withModel(tableModelBuildable);
    }

    /**
     * @param header The table header which should be wrapped by the builder.
     * @return A builder instance for a new {@link JTableHeader}.
     * @param <H> The type of the {@link JTableHeader} for which the builder should be created.
     */
    public static <H extends TableHeader> UIForTableHeader<H> of( H header ) {
        NullUtil.nullArgCheck(header, "header", TableHeader.class);
        return new UIForTableHeader<>(new BuilderState<>(header));
    }

    /**
     * @return A builder instance for a new {@link JTableHeader}.
     */
    public static UIForTableHeader<TableHeader> tableHeader() {
        return new UIForTableHeader<>(new BuilderState<>(TableHeader.class, ()->new TableHeader()));
    }

    /**
     * @return A functional API for building a {@link javax.swing.table.TableModel}.
     */
    public static BasicTableModel.Builder<Object> tableModel() {
        return new BasicTableModel.Builder<>(Object.class);
    }

    /**
     * @param entryType The type of the table entries.
     * @param <E> The type of the table entries.
     * @return A functional API for building a {@link javax.swing.table.TableModel}.
     */
    public static <E> BasicTableModel.Builder<E> tableModel( Class<E> entryType ) {
        return new BasicTableModel.Builder<>(entryType);
    }

    /**
     *  Exposes a fluent builder API for creating a table renderer.<br>
     *  Here an example of how this would typically be used:
     * <pre>{@code
     *     UI.table(myModel)
     *     .withRendererForColumn(0,
     *         UI.renderTable()
     *         .when(String.class)
     *         .asText( cell -> "[" + cell.valueAsString().orElse("") + "]" ) )
     *     )
     *     .withRendererForColumn(1,
     *         UI.renderTable()
     *         .when(Float.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "f)" ) )
     *         .when(Double.class)
     *         .asText( cell -> "(" + cell.valueAsString().orElse("") + "d)" ) )
     *     );
     * }</pre>
     * The above example would render the first column of the table as a string surrounded by square brackets,
     * and the second column as a float or double value surrounded by parentheses.
     * Note that the API allows you to specify how specific types of table entry values
     * should be rendered. This is done by calling the {@link Render.Builder#when(Class)} method
     * bedore calling the {@link Render.As#asText(Function)} method.
     *
     * @return A builder instance for a new {@link JTable}.
     */
    public static Render.Builder<JTable, Object> renderTable() {
        return Render.forTable(Object.class, null)
                     .when(Object.class)
                     .asText( cell -> cell.valueAsString().orElse("") );
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
     *  This returns an instance of a SwingTree builder for a {@link JFrame} type.
     * @param frame The new frame instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     * @param <F> The concrete type of this new frame.
     */
    public static <F extends JFrame> UIForJFrame<F> of( F frame ) {
        return new UIForJFrame<>(new BuilderState<>(frame));
    }

    /**
     *  Use this to create a builder for the supplied {@link JFrame}. <br>
     *  This is in essence a convenience method for {@code UI.of(new JFrame()) )}.
     *
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     */
    public static UIForJFrame<JFrame> frame() {
        return new UIForJFrame<>(new BuilderState<>(JFrame.class, ()->new JFrame()));
    }

    /**
     *  Use this to create a builder for the supplied {@link JFrame} with the supplied title. <br>
     * @param title The title for the new frame.
     * @return A basic UI builder instance wrapping a {@link JFrame}.
     */
    public static UIForJFrame<JFrame> frame( String title ) {
        return new UIForJFrame<>(new BuilderState<>(JFrame.class, ()->new JFrame()))
                    .withTitle(title);
    }

    /**
     *  This returns an instance of a SwingTree builder for a {@link JDialog} type.
     * @param dialog The new dialog instance which ought to be part of the Swing UI.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     * @param <D> The concrete type of this new dialog.
     */
    public static <D extends JDialog> UIForJDialog<D> of( D dialog ) {
        return new UIForJDialog<>(new BuilderState<>(dialog));
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog}. <br>
     *  This is in essence a convenience method for {@code UI.of(new JDialog()) )}.
     *
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog() {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()->new JDialog()));
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied owner. <br>
     * @param owner The owner for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( Window owner ) {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()->new JDialog(owner)));
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied title. <br>
     * @param title The title for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( String title ) {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()->new JDialog())).withTitle(title);
    }

    /**
     *  Use this to create a builder for the supplied {@link JDialog} with the supplied owner and title. <br>
     * @param owner The owner for the new dialog.
     * @param title The title for the new dialog.
     * @return A basic UI builder instance wrapping a {@link JDialog}.
     */
    public static UIForJDialog<JDialog> dialog( Window owner, String title ) {
        return new UIForJDialog<>(new BuilderState<>(JDialog.class, ()->new JDialog(owner)))
                    .withTitle(title);
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
     * which causes a provided {@link Runnable} to be executed asynchronously on the
     * AWT event dispatching thread.  This will happen after all
     * pending AWT events have been processed.  This method should
     * be used when an application thread needs to update the GUI.
     * In the following example the <code>runLater</code> call queues
     * the <code>Runnable</code> lambda
     * on the event dispatching thread and
     * then prints a message.
     * <pre>{@code
     *  UI.run( () -> System.out.println("Hello World on " + Thread.currentThread()) );
     *  System.out.println("This might well be displayed before the other message.");
     * }</pre>
     * If runLater is called from the event dispatching thread --
     * for example, from a JButton's ActionListener -- the <code>Runnable</code> will
     * still be deferred until all pending events have been processed.
     * Note that if the <code>Runnable</code> throws an uncaught exception
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
     * A convenience method for {@link SwingUtilities#invokeLater(Runnable)},
     * which causes {@link Runnable} to be executed asynchronously on the
     * AWT event dispatching thread after the specified delay.
     * This method should be used when an application thread needs to update the GUI
     * after a particular delay.
     * In the following example the <code>invokeLater</code> call queues
     * the <code>Runnable</code> lambda containing a print statement
     * on the event dispatching thread and
     * then prints a message.
     * <pre>{@code
     *  UI.runLater( 1000, () -> System.out.println("Hello World on " + Thread.currentThread()) );
     *  System.out.println("This might well be displayed before the other message.");
     * }</pre>
     * If runLater is called from the event dispatching thread --
     * for example, from a JButton's ActionListener -- the <code>Runnable</code> will
     * still be deferred until the specified delay has passed.
     * Note that if the <code>Runnable</code> throws an uncaught exception
     * the event dispatching thread will unwind (not the current thread).
     *
     * @param delay The delay in milliseconds.
     * @param runnable the instance of {@code Runnable}
     * @see #runNow
     */
    public static void runLater( int delay, Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        Timer timer = new Timer( delay, e -> { runnable.run(); } );
        timer.setRepeats(false); // Execute only once
        timer.setInitialDelay(delay);
        timer.start();
    }

    /**
     * A convenience method for {@link SwingUtilities#invokeLater(Runnable)},
     * which causes {@link Runnable} to be executed asynchronously on the
     * AWT event dispatching thread after the specified delay
     * has passed in the given time unit.
     * This method should be used when an application thread needs to update the GUI
     * after a particular delay.
     * In the following example the <code>invokeLater</code> call queues
     * the <code>Runnable</code> lambda containing a print statement
     * on the event dispatching thread and
     * then prints a message.
     * <pre>{@code
     *  UI.runLater( 1000, TimeUnit.MILLISECONDS, () -> System.out.println("Hello World on " + Thread.currentThread()) );
     *  System.out.println("This might well be displayed before the other message.");
     * }</pre>
     * If runLater is called from the event dispatching thread --
     * for example, from a JButton's ActionListener -- the <code>Runnable</code> will
     * still be deferred until the specified delay has passed.
     * Note that if the <code>Runnable</code> throws an uncaught exception
     * the event dispatching thread will unwind (not the current thread).
     *
     * @param delay The delay in the given time unit.
     * @param unit The time unit of the delay parameter.
     * @param runnable the instance of {@code Runnable}
     * @see #runNow
     */
    public static void runLater( double delay, TimeUnit unit,  Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        NullUtil.nullArgCheck(unit, "unit", TimeUnit.class);
        long millis = (long) (delay * unit.toMillis(1));
        long remainderMillis = (long) (delay * unit.toMillis(1) - millis);
        long convertedDelay = TimeUnit.MILLISECONDS.convert(millis + remainderMillis, TimeUnit.MILLISECONDS);
        runLater( (int) convertedDelay, runnable );
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
     * Note that contrary to the {@link SwingUtilities#invokeAndWait(Runnable)} method,
     * this method does not throw an exception if it is called from the
     * event dispatching thread. Instead, it just executes the runnable
     * immediately.
     *
     * @param runnable the instance of {@code Runnable}
     * @see #run
     */
    public static void runNow( Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        try {
            if ( !UI.thisIsUIThread() )
                SwingUtilities.invokeAndWait(runnable);
            else
                runnable.run();
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
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
     * @param supplier The supplier which should be executed on the UI thread.
     * @param <T> The return type of the result value produced by the supplier.
     * @return The result provided by the supplier.
     */
    public static <T> T runAndGet( Supplier<T> supplier ) {
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
     */
    public static void sync() {
        runNow( () -> {/*
            This is a no-op, but it forces the event dispatching thread to
            process all pending events before returning.
            So when we reach this point, we know that all pending events
            have been processed.
        */});
    }

    /**
     *  Exposes an API for scheduling periodic animation updates.
     *  This is a convenience method for {@link Animator#animateFor(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( 100, TimeUnit.MILLISECONDS )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setValue( it.progress() );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *  @param unit The time unit of the duration.
     *  @return An {@link Animator} instance which allows you to configure the animation.
     */
    public static Animator animateFor(long duration, TimeUnit unit ) {
        Objects.requireNonNull(unit, "unit");
        return Animator.animateFor( LifeTime.of(duration, unit) );
    }

    /**
     *  Exposes a builder API for creating and scheduling periodic animation updates.
     *  This is a convenience method for {@link Animator#animateFor(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( 0.1, TimeUnit.MINUTES )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *  @param unit The time unit of the duration.
     *  @return An {@link Animator} instance which allows you to configure the animation.
     */
    public static Animator animateFor( double duration, TimeUnit unit ) {
        return Animator.animateFor( LifeTime.of(duration, unit) );
    }

    /**
     *  Exposes a builder API for creating and scheduling periodic animation updates.
     *  This is a convenience method for {@link Animator#animateFor(LifeTime, Stride)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( 0.1, TimeUnit.MINUTES, Stride.REGRESSIVE )
     *       .until( it -> it.progress() < 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *  @param unit The time unit of the duration.
     *  @param stride The stride of the animation, which determines whether the animation
     *                progresses going forward or backwards.
     *  @return An {@link Animator} instance which allows you to configure the animation.
     */
    public static Animator animateFor(double duration, TimeUnit unit, Stride stride) {
        return Animator.animateFor( LifeTime.of(duration, unit), stride );
    }

    /**
     *  Exposes an API for scheduling periodic animation updates.
     *  This is a convenience method for {@link Animator#animateFor(LifeTime)}. <br>
     *  A typical usage would be:
     *  <pre>{@code
     *    UI.animateFor( LifeTime.of(0.1, TimeUnit.MINUTES) )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *          // ...
     *          someComponent.repaint();
     *       });
     *  }</pre>
     *  @param duration The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     *
     *  @return An {@link Animator} instance which allows you to configure the animation.
     */
    public static Animator animateFor( LifeTime duration ) {
        return Animator.animateFor( duration );
    }

    /**
     * Exposes an API for scheduling periodic animation updates
     * for a specific component whose {@link java.awt.Component#repaint()}
     * method should be called after every animation update.
     * This is a convenience method for {@link Animator#animateFor(LifeTime)}. <br>
     * A typical usage would be:
     * <pre>{@code
     *    UI.animateFor( UI.lifeTime(0.1, TimeUnit.MINUTES), someComponent )
     *       .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *       .go( it -> {
     *          // do something
     *          someComponent.setBackground( new Color( 0, 0, 0, (int)(it.progress()*255) ) );
     *       });
     *  }</pre>
     *
     * @param duration  The duration of the animation.
     *                  This is the time it takes for the animation to reach 100% progress.
     * @param component The component which should be repainted after every animation update.
     * @return An {@link Animator} instance which allows you to configure the animation.
     */
    public static Animator animateFor( LifeTime duration, java.awt.Component component ) {
        return Animator.animateFor( duration, component );
    }

    /**
     *  A factory method for creating a {@link LifeTime} instance
     *  with the given duration and time unit.
     *  This is a convenience method for {@link LifeTime#of(long, TimeUnit)}.
     *  The {@link LifeTime} instance is an immutable value type
     *  which is used for scheduling animations, usually through
     *  {@link Animator#animateFor(LifeTime)} or the convenience methods
     *  {@link UI#animateFor(long, TimeUnit)}, {@link UI#animateFor(double, TimeUnit)},
     *  {@link UI#animateFor(LifeTime)} or {@link UI#animateFor(LifeTime, java.awt.Component)}.
     *  A typical usage would be:
     *  <pre>{@code
     *      UI.animateFor( UI.lifeTime(0.1, TimeUnit.MINUTES) )
     *      .until( it -> it.progress() >= 0.75 && someOtherCondition() )
     *      .go( it -> {
     *          // do something
     *      });
     *  }</pre>
     *
     * @param duration The duration of the animation.
     * @param unit The time unit of the duration.
     * @return A {@link LifeTime} instance.
     */
    public static LifeTime lifeTime( long duration, TimeUnit unit ) { return LifeTime.of(duration, unit); }

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
        message(message)
                .titled(title)
                .showAsInfo();
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
    public static void warn( String title, String message ) {
        message(message)
                .titled(title)
                .showAsWarning();
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
    public static void error( String title, String message ) {
        message(message)
            .titled(title)
            .showAsError();
    }

    /**
     * @param text The text to show in the dialog.
     * @return A builder for creating an error dialog.
     */
    public static MessageDialog message( String text ) { return MessageDialog.saying(text); }

    /**
     *  Shows a conformation dialog with the given message.
     * @param message the message to show
     * @return {@code Answer.YES} if the user clicked "Yes", {@code Answer.NO} if the user clicked "No", {@code Answer.CANCEL} otherwise.
     */
    public static ConfirmAnswer confirm( String message ) { return confirm("Confirm", message); }

    /**
     * Shows a conformation dialog with the given message.
     *
     * @param title   the title of the dialog
     * @param message the message to show
     * @return {@code Answer.YES} if the user clicked "Yes", {@code Answer.NO} if the user clicked "No", {@code Answer.CANCEL} otherwise.
     */
    public static ConfirmAnswer confirm( String title, String message ) {
        return ConfirmDialog.asking(message)
                            .titled(title)
                            .showAsQuestion();
    }

    /**
     * @param toBeConfirmed The question to ask the user.
     * @return A builder for creating a confirmation dialog designed to ask a question.
     */
    public static ConfirmDialog confirmation( String toBeConfirmed ) {
        return ConfirmDialog.asking(toBeConfirmed);
    }

    /**
     *  Shows a dialog where the user can select a value from a list of options
     *  based on the enum type implicitly defined by the given enum based property.
     *  The selected value will be stored in said property after the user has
     *  selected a value and also returned as an {@link Optional}.
     *  If no value is selected, the returned {@link Optional} will be empty
     *  and the property will not be changed.
     *
     * @param question The message to show in the dialog.
     * @param selected The enum based property to store the selected value in.
     * @param <E> The enum type.
     * @return The selected enum value wrapped in an {@link Optional} or an empty optional if the user cancelled the dialog.
     */
    public static <E extends Enum<E>> Optional<E> ask( String question, Var<E> selected ) {
        return ask("Select", question, selected );
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
     * @param <E> The enum type.
     * @return The selected enum value wrapped in an {@link Optional} or an empty optional if the user cancelled the dialog.
     */
    public static <E extends Enum<E>> Optional<E> ask( String title, String message, Var<E> selected ) {
        Objects.requireNonNull( message  );
        Objects.requireNonNull( title    );
        Objects.requireNonNull( selected );
        return OptionsDialog.offering(message, selected)
                            .titled(title)
                            .showAsQuestion();
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
     * @param <E> The type parameter defining the concrete enum type.
     */
    public static <E extends Enum<E>> void ask( String title, String message, Icon icon, Var<E> selected ) {
        Objects.requireNonNull( message  );
        Objects.requireNonNull( title    );
        Objects.requireNonNull( selected );
        OptionsDialog.offering(message, selected)
                                .titled(title)
                                .icon(icon)
                                .showAsQuestion();
    }

    /**
     *  Exposes the {@link OptionsDialog} API for creating a question dialog
     *  that allows the user to select a value from an array of provided enum values.
     *
     * @param offer The message to show in the dialog.
     * @param options The array of enum values to show in the dialog.
     * @param <E> The enum type.
     * @return A builder for creating a question dialog with a set of selectable enum values
     *         based on the provided array of enum values.
     */
    @SafeVarargs
    public static <E extends Enum<E>> OptionsDialog<E> choice( String offer, E... options ) {
        return OptionsDialog.offering(offer, options);
    }

    /**
     *  Exposes the {@link OptionsDialog} API for creating a question dialog
     *  that allows the user to select and set a value from the provided enum based property.
     *
     * @param offer The message to show in the dialog.
     * @param selectable The enum based property to store the selected value in.
     * @param <E> The enum type.
     * @return A builder for creating a question dialog with a set of selectable enum values
     *         based on the provided array of enum values.
     */
    public static <E extends Enum<E>> OptionsDialog<E> choice( String offer, Var<E> selectable ) {
        return OptionsDialog.offering(offer, selectable);
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen.
     *
     * @param component The component to show in the window.
     */
    public static void show( java.awt.Component component ) {
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
    public static void show( String title, java.awt.Component component ) {
        Objects.requireNonNull( component );
        new UI.TestWindow( title, f -> component );
    }

    /**
     *  Use this to quickly launch a UI component in a {@link JFrame} window
     *  at the center of the screen.
     *
     * @param ui The SwingTree UI to show in the window.
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
     * @param ui The SwingTree UI to show in the window.
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
    public static void show( Function<JFrame, java.awt.Component> uiSupplier ) {
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
    public static void show( String title, Function<JFrame, java.awt.Component> uiSupplier ) {
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
    public static void showUsing( EventProcessor eventProcessor, Function<JFrame, java.awt.Component> uiSupplier ) {
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
    public static void showUsing(
        EventProcessor eventProcessor,
        String title,
        Function<JFrame, java.awt.Component> uiSupplier
    ) {
        Objects.requireNonNull( eventProcessor );
        Objects.requireNonNull( uiSupplier );
        show(title, frame -> use(eventProcessor, () -> uiSupplier.apply(frame)));
    }

    /**
     *  This enum is used to specify how an image or icon (usually a {@link SvgIcon})
     *  should be scaled to fit the
     *  dimensions of the component that it is being rendered into, like for example
     *  through the {@link SvgIcon#paintIcon(java.awt.Component, Graphics, int, int)} method.
     */
    public enum FitComponent {
        /**
         *  Fit the image or icon to the width of the component.
         */
        WIDTH,
        /**
         *  Fit the image or icon to the height of the component.
         */
        HEIGHT,
        /**
         *  Fit the image or icon to the width and height of the component.
         */
        WIDTH_AND_HEIGHT,
        /**
         *  Fit the image to the largest dimension of the component.
         */
        MAX_DIM,
        /**
         *  Fit the image to the smallest dimension of the component.
         */
        MIN_DIM,
        /**
         *  Do not fit the image to the component.
         */
        NO
    }

    /**
     *  Use this to quickly create and inspect a test window for a UI component.
     */
    private static class TestWindow
    {
        private final JFrame frame;
        private final java.awt.Component component;

        private TestWindow( String title, Function<JFrame, java.awt.Component> uiSupplier ) {
            Objects.requireNonNull( title );
            Objects.requireNonNull( uiSupplier );
            this.frame = new JFrame();
            if ( !title.isEmpty() ) this.frame.setTitle(title);
            frame.setLocationRelativeTo(null); // Initial centering!
            java.awt.Component c = null;
            if ( !UI.thisIsUIThread() ) {
                try {
                    c = UI.runAndGet(() -> uiSupplier.apply(frame));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
                c = uiSupplier.apply(frame);

            this.component = c;
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
        The following methods and subsequent class definitions have 2 purposes:

         1. The nested classes are all bundled into the UI class to
            avoid having to import them from different packages.

         2. Their paint methods are overridden to allow SwingTree to perform
            rendering of the style configuration of a component
            without requiring the user to override the paint methods.
    */

    /** {inheritDoc} */
    public static class Component extends JComponent implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Panel extends JPanel implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }

        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Label extends JLabel implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TextField extends JTextField implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TextArea extends JTextArea implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class CheckBox extends JCheckBox implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Button extends JButton implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ToggleButton extends JToggleButton implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class RadioButton extends JRadioButton implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ComboBox<E> extends JComboBox<E> implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class List<E> extends JList<E> implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Table extends JTable implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TableHeader extends JTableHeader implements StylableComponent {
        private Function<Integer, String> _toolTipTextSupplier;
        public TableHeader() { super(); }
        public TableHeader(TableColumnModel model) { super(model); }
        /**
         * @param toolTipTextSupplier A function which receives the column index and returns the
         *                            tool tip text for that column.
         */
        public void setToolTipsSupplier( Function<Integer, String> toolTipTextSupplier ) {
            Objects.requireNonNull(toolTipTextSupplier);
            _toolTipTextSupplier = toolTipTextSupplier;
        }
        /**
         * @param toolTips The tool tip texts for the columns.
         */
        public void setToolTips( String... toolTips ) {
            Objects.requireNonNull(toolTips);
            setToolTipsSupplier( i -> toolTips[i] );
        }
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintComponent(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
        @Override public String getToolTipText(MouseEvent e) {
            int col = columnAtPoint(e.getPoint());
            int modelCol = Optional.ofNullable(getTable())
                                    .map( t -> t.convertColumnIndexToModel(col) )
                                    .orElse(col);
            String retStr;
            try { retStr = _toolTipTextSupplier.apply(modelCol); }
            catch ( NullPointerException | ArrayIndexOutOfBoundsException ex ) {
                retStr = "";
            }
            return  ( retStr.isEmpty() ? super.getToolTipText(e) : retStr );
        }
    }
    /** {inheritDoc} */
    public static class Slider extends JSlider implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class PopupMenu extends JPopupMenu implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class MenuItem extends JMenuItem implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class RadioButtonMenuItem extends JRadioButtonMenuItem implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
     public static class CheckBoxMenuItem extends JCheckBoxMenuItem implements StylableComponent {
         @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
         @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
     }
    /** {inheritDoc} */
    public static class Menu extends JMenu implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class MenuBar extends JMenuBar implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ScrollPane extends JScrollPane implements StylableComponent {
        public ScrollPane() { this(null); }
        public ScrollPane(java.awt.Component view) {
            super(view);
            addMouseWheelListener(new NestedJScrollPanelScrollCorrection(this));
        }
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TabbedPane extends JTabbedPane implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ToolBar extends JToolBar implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ToolTip extends JToolTip implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Tree extends JTree implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TextPane extends JTextPane implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Spinner extends JSpinner implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class SplitPane extends JSplitPane implements StylableComponent {
        SplitPane( Align align ) { super(align.forSplitPane()); }
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class PasswordField extends JPasswordField implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ProgressBar extends JProgressBar implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class EditorPane extends JEditorPane implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {@inheritDoc} */
    public static class FormattedTextField extends JFormattedTextField implements StylableComponent {
        @Override public void paint(Graphics g){ paintBackground(g, ()->super.paint(g)); }
        @Override public void paintChildren(Graphics g){ paintForeground(g, ()->super.paintChildren(g)); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Box extends JBox {/* Already implemented */}
    /** {@inheritDoc} */
    public static class SplitButton extends JSplitButton {/* Already implemented */}

}
