package swingtree;

import swingtree.api.Layout;
import swingtree.api.NoiseFunction;
import swingtree.api.Styler;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;
import swingtree.style.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.Optional;
import java.util.function.Function;


public abstract class UIConstants extends UILayoutConstants
{
    UIConstants() { throw new UnsupportedOperationException(); }

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
        UNDEFINED, TOP, CENTER, BOTTOM;

        public Optional<Integer> forSwing() {
            switch ( this ) {
                case TOP:    return Optional.of(SwingConstants.TOP);
                case CENTER: return Optional.of(SwingConstants.CENTER);
                case BOTTOM: return Optional.of(SwingConstants.BOTTOM);
                default:
                    return Optional.empty();
            }
        }
    }

    /**
     *  Different positions along a horizontally aligned UI component.
     */
    public enum HorizontalAlignment implements UIEnum<HorizontalAlignment>
    {
        UNDEFINED,
        LEFT, CENTER, RIGHT, LEADING, TRAILING;

        public final Optional<Integer> forSwing() {
            switch ( this ) {
                case LEFT:     return Optional.of(SwingConstants.LEFT);
                case CENTER:   return Optional.of(SwingConstants.CENTER);
                case RIGHT:    return Optional.of(SwingConstants.RIGHT);
                case LEADING:  return Optional.of(SwingConstants.LEADING);
                case TRAILING: return Optional.of(SwingConstants.TRAILING);
                default:
                    return Optional.empty();
            }
        }

        public final Optional<Integer> forFlowLayout() {
            switch ( this ) {
                case LEFT:     return Optional.of(FlowLayout.LEFT);
                case CENTER:   return Optional.of(FlowLayout.CENTER);
                case RIGHT:    return Optional.of(FlowLayout.RIGHT);
                case LEADING:  return Optional.of(FlowLayout.LEADING);
                case TRAILING: return Optional.of(FlowLayout.TRAILING);
                default:
                    return Optional.empty();
            }
        }
    }

    /**
     *  The logical combination of a vertical and horizontal alignment.
     */
    public enum Alignment implements UIEnum<Alignment>
    {
        UNDEFINED,
        TOP_LEFT,    TOP_CENTER, TOP_RIGHT, TOP_LEADING, TOP_TRAILING,
        CENTER_LEFT, CENTER, CENTER_RIGHT, CENTER_LEADING, CENTER_TRAILING,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, BOTTOM_LEADING, BOTTOM_TRAILING;

        public VerticalAlignment getVertical() {
            switch ( this ) {
                case UNDEFINED : return VerticalAlignment.UNDEFINED;
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
                case UNDEFINED : return HorizontalAlignment.UNDEFINED;
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
        FIBERS(NoiseFunctions::fibery),
        RETRO(NoiseFunctions::retro),
        CELLS(NoiseFunctions::cells),
        HAZE(NoiseFunctions::haze),
        SPIRALS(NoiseFunctions::spirals),
        MANDELBROT(NoiseFunctions::mandelbrot);


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
     *  Use this to specify the font style of a component.
     *  <br>
     *  See {@link UIForAnySwing#withStyle(Styler)} and {@link ComponentStyleDelegate#fontStyle(FontStyle)}.
     */
    public enum FontStyle implements UIEnum<FontStyle>
    {
        PLAIN, BOLD, ITALIC, BOLD_ITALIC;

        public int toAWTFontStyle() {
            switch ( this ) {
                case PLAIN:        return java.awt.Font.PLAIN;
                case BOLD:         return java.awt.Font.BOLD;
                case ITALIC:       return java.awt.Font.ITALIC;
                case BOLD_ITALIC:  return java.awt.Font.BOLD + java.awt.Font.ITALIC;
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

}
