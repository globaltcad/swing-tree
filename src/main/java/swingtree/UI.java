package swingtree;

import com.google.errorprone.annotations.Immutable;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sprouts.Result;
import sprouts.Val;
import swingtree.api.Configurator;
import swingtree.api.Layout;
import swingtree.api.NoiseFunction;
import swingtree.api.Styler;
import swingtree.api.model.TableListDataSource;
import swingtree.api.model.TableMapDataSource;
import swingtree.components.JBox;
import swingtree.components.JSplitButton;
import swingtree.components.listener.NestedJScrollPanelScrollCorrection;
import swingtree.style.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
public final class UI extends UIFactoryMethods
{
    private static final Logger log = LoggerFactory.getLogger(UI.class);
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  An enum set of all the available swing cursors which
     *  map to the cursor type id.
     *  This exists simply because swing was created before enums were added to Java.
     */
    @Immutable
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
    @Immutable
    public enum Active implements UIEnum<Active>{
        NEVER, AS_NEEDED, ALWAYS
    }

    /**
     *  All UI components are at their core rectangular, meaning they
     *  always have exactly 4 uniquely identifiable sides.
     *  This enum is used to target specific sides of a {@link JComponent}
     *  in various API methods like for example {@link UIForTabbedPane#withTabPlacementAt(swingtree.UI.Side)}
     *  or the tapped pane factory method {@link UI#tabbedPane(swingtree.UI.Side)}.
     */
    @Immutable
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
    @Immutable
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
    @Immutable
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
    @Immutable
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
    @Immutable
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
    @Immutable
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
     *  See {@link UI#table(swingtree.UI.ListData, TableListDataSource)}
     *  or {@link UIForTable#withModel(swingtree.UI.ListData, TableListDataSource)}
     *  for more information about the usage of this enum.
     */
    @Immutable
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
     *  See {@link UI#table(swingtree.UI.MapData, TableMapDataSource)} or
     *  {@link UIForTable#withModel(swingtree.UI.MapData, TableMapDataSource)}
     *  for more information about the usage of this enum.
     */
    @Immutable
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
     *  See {@link ComponentStyleDelegate#borderWidthAt(swingtree.UI.Edge, double)}
     */
    @Immutable
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
    @Immutable
    public enum Layer implements UIEnum<Layer>
    {
        /**
         *  This layer is applied through the {@link StylableComponent#paintBackground(Graphics, Consumer)} method.
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
         *  The foreground is painted through the {@link StylableComponent#paintForeground(Graphics, Consumer)} method.
         *  When using custom components, please make sure your component implements this interface!
         */
        FOREGROUND
    }

    /**
     *  Use these enum instances to specify the gradient type for various sub styles,
     *  like for example the gradient style API exposed by {@link ComponentStyleDelegate#gradient(swingtree.UI.Layer, String, Configurator)}
     *  or {@link ComponentStyleDelegate#gradient(Configurator)} methods (see {@link UIForAnySwing#withStyle(Styler)}).
     *  <p>
     *  {@link GradientConf#type(swingtree.UI.GradientType)} method exposed by methods like
     *  {@link ComponentStyleDelegate#gradient(String, Configurator)} or {@link ComponentStyleDelegate#gradient(swingtree.UI.Layer, String, Configurator)}.
     */
    @Immutable
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
    @Immutable
    public enum NoiseType implements UIEnum<NoiseType>, NoiseFunction
    {
        CELLS(NoiseFunctions::cells),
        FABRIC(NoiseFunctions::fabric),
        GRAINY(NoiseFunctions::grainy),
        HARD_SPOTS(NoiseFunctions::hardSpots),
        HARD_TOPOLOGY(NoiseFunctions::hardTopology),
        HAZE(NoiseFunctions::haze),
        MANDELBROT(NoiseFunctions::mandelbrot),
        MOSAIC(NoiseFunctions::voronoiBasedCellMosaic),
        GEM_STONES(NoiseFunctions::voronoiBasedPolygonCell),
        RETRO(NoiseFunctions::retro),
        STOCHASTIC(NoiseFunctions::stochastic),
        SMOOTH_TOPOLOGY(NoiseFunctions::smoothTopology),
        SMOOTH_SPOTS(NoiseFunctions::smoothSpots),
        SPIRALS(NoiseFunctions::spirals),
        TILES(NoiseFunctions::tiles),
        TISSUE(NoiseFunctions::voronoiBasedCellTissue),
        POND_IN_DRIZZLE(NoiseFunctions::voronoiBasedPondInDrizzle),
        POND_IN_RAIN(NoiseFunctions::voronoiBasedPondInRain),
        POND_OF_STRINGS(NoiseFunctions::voronoiBasedPondOfStrings),
        POND_OF_TANGLED_STRINGS(NoiseFunctions::voronoiBasedPondOfTangledStrings);


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
     *  like for example the gradient style API exposed by {@link ComponentStyleDelegate#gradient(Configurator)}
     *  or {@link ComponentStyleDelegate#gradient(Configurator)} methods (see {@link UIForAnySwing#withStyle(Styler)}).
     * <p>
     *  {@link GradientConf#span(swingtree.UI.Span)} method exposed by methods like
     *  {@link ComponentStyleDelegate#gradient(String, Configurator)} or
     *  {@link ComponentStyleDelegate#gradient(swingtree.UI.Layer, String, Configurator)}.
     */
    @Immutable
    public enum Span implements UIEnum<Span>
    {
        TOP_LEFT_TO_BOTTOM_RIGHT, BOTTOM_LEFT_TO_TOP_RIGHT,
        TOP_RIGHT_TO_BOTTOM_LEFT, BOTTOM_RIGHT_TO_TOP_LEFT,

        TOP_TO_BOTTOM, LEFT_TO_RIGHT,
        BOTTOM_TO_TOP, RIGHT_TO_LEFT;

        /**
         *  Use this to check if the alignment is diagonal and not horizontal or vertical.
         * @return {@code true} if this alignment is diagonal, {@code false} otherwise.
         */
        public boolean isDiagonal() {
            return this == TOP_LEFT_TO_BOTTOM_RIGHT || this == BOTTOM_LEFT_TO_TOP_RIGHT ||
                    this == TOP_RIGHT_TO_BOTTOM_LEFT || this == BOTTOM_RIGHT_TO_TOP_LEFT;
        }
    }

    /**
     *  Used to specify the cycle method for a gradient conf in the style API.
     *  See {@link UIForAnySwing#withStyle(Styler)} and {@link ComponentStyleDelegate#gradient(Configurator)}.
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
    @Immutable
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
     *  See {@link ComponentStyleDelegate#borderRadiusAt(swingtree.UI.Corner, double, double)}.
     */
    @Immutable
    public enum Corner implements UIEnum<Corner>
    {
        EVERY,
        TOP_LEFT,    TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_RIGHT
    }

    /**
     *  Use this to specify the placement of an image as part of the {@link ImageConf} through
     *  the {@link ImageConf#placement(swingtree.UI.Placement)} method exposed by the
     *  style API (see {@link UIForAnySwing#withStyle(Styler)}).
     */
    @Immutable
    public enum Placement implements UIEnum<Placement>
    {
        UNDEFINED,
        TOP, LEFT, BOTTOM, RIGHT,
        TOP_LEFT, TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_RIGHT,
        CENTER
    }

    /**
     *  This enum is used to specify how an image or icon (usually a {@link SvgIcon})
     *  should be scaled to fit the
     *  dimensions of the component that it is being rendered into, like for example
     *  through the {@link SvgIcon#paintIcon(java.awt.Component, Graphics, int, int)} method.<br>
     *  You may want to pass constants of this enum to {@link ImageConf#fitMode(FitComponent)} as
     *  part of using the style API:<br>
     *  <pre>{@code
     *  UI.button("Click Me!")
     *  .withStyle( conf -> conf
     *      .image( img -> img
     *          .image(SvgIcon.at("my/path/to.svg"))
     *          .fitMode(UI.FitComponent.MIN_DIM)
     *      )
     *      .border(12, UI.Color.LIGHTSTEELBLUE)
     *      .borderRadius(8)
     *  )
     *  }</pre>
     * @see ImageConf#fitMode(FitComponent)
     * @see SvgIcon#withFitComponent(FitComponent)
     */
    @Immutable
    public enum FitComponent implements UIEnum<FitComponent> {
        /**
         *  How a particular image is supposed to fit a component is unknown
         *  and <b>may be overridden by another policy or default behavior</b>.
         *  Typically, this is equivalent to {@link #NO}.
         */
        UNDEFINED,
        /**
         *  Fit the image or icon to the width of a component by scaling the icon/image along the x-axis.
         *  <b>This implies that you only want to scale the width of an image/icon, but not its height,
         *  so this constant may change the inherent aspect ratio of a targeted image/icon!</b>
         */
        WIDTH,
        /**
         *  Fit the image or icon to the height of a component by scaling the icon/image along the y-axis.
         *  <b>This implies that you only want to scale the height of an image/icon, but not its width,
         *  so this constant may change the inherent aspect ratio of a targeted image/icon!</b>
         */
        HEIGHT,
        /**
         *  Fit the image or icon to both fit the width and height of the component.
         *  <b>Note that this may change the inherent aspect ratio of the image in
         *  favor of the components aspect ratio...</b>
         */
        WIDTH_AND_HEIGHT,
        /**
         *  Fit the image to the largest dimension of the component.
         */
        MAX_DIM,
        /**
         *  Fit the image to the smallest dimension of the component,
         *  while preserving the aspect ratio of the image.
         */
        MIN_DIM,
        /**
         *  Do not fit the image to the component,
         *  while preserving the aspect ratio of the image.
         */
        NO
    }

    /**
     *  Defines the areas of a component, which is used
     *  to by the {@link ImageConf} to determine if and how an image should be clipped.
     *  Pass instances of this to {@link ImageConf#clipTo(swingtree.UI.ComponentArea)} to configure the clipping behaviour
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
    @Immutable
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
    @Immutable
    public enum ComponentBoundary implements UIEnum<ComponentBoundary> {
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
        /**
         * The center point of the component.
         */
        CENTER_TO_CONTENT, // The center of the component.
    }

    /**
     *  Use this to specify the orientation of a component.
     *  This is especially important for components that display text.
     *  <br>
     *  See {@link UIForAnySwing#withStyle(Styler)} and {@link ComponentStyleDelegate#orientation(swingtree.UI.ComponentOrientation)}.
     */
    @Immutable
    public enum ComponentOrientation implements UIEnum<ComponentOrientation>
    {
        UNKNOWN, LEFT_TO_RIGHT, RIGHT_TO_LEFT
    }

    /**
     *  Defines a set of close operations for a {@link JDialog} or {@link JFrame} windows.
     *  The following list describes what each enum instance represents:
     *  <ul>
     *      <li>{@link OnWindowClose#DISPOSE} -
     *      The window is disposed when it is closed.
     *      </li>
     *      <li>{@link OnWindowClose#HIDE} -
     *      The window is hidden when it is closed.
     *      It will not be disposed and can be shown again.
     *      </li>
     *      <li>{@link OnWindowClose#DO_NOTHING} -
     *      The window does nothing when it is closed.
     *      </li>
     *  </ul>
     *  See {@link UIForAnyWindow#withOnCloseOperation(swingtree.UI.OnWindowClose)} for more
     *  information about the usage of this enum.
     */
    @Immutable
    public enum OnWindowClose implements UIEnum<OnWindowClose>
    {
        DISPOSE, HIDE, DO_NOTHING;

        public int forSwing() {
            switch ( this ) {
                case DISPOSE:     return WindowConstants.DISPOSE_ON_CLOSE;
                case HIDE:        return WindowConstants.HIDE_ON_CLOSE;
                case DO_NOTHING:  return WindowConstants.DO_NOTHING_ON_CLOSE;
            }
            throw new RuntimeException();
        }
    }

    /**
     *  Used to define how a layout manager (typically the {@link BoxLayout})
     *  will lay out components along the given axis. <br>
     *  Create a simple box layout for your components
     *  by calling the {@link UIForAnySwing#withBoxLayout(swingtree.UI.Axis)} method,
     *  or use {@link Layout#box(swingtree.UI.Axis)} factory method returning a {@link Layout} config
     *  object which can be passed to the style API (see {@link UIForAnySwing#withStyle(Styler)}
     *  and {@link ComponentStyleDelegate#layout(Layout)}).
     */
    @Immutable
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
     *  See {@link UIForAnySwing#withStyle(Styler)} and {@link ComponentStyleDelegate#fontStyle(swingtree.UI.FontStyle)}.
     */
    @Immutable
    public enum FontStyle implements UIEnum<FontStyle>
    {
        PLAIN, BOLD, ITALIC, BOLD_ITALIC;

        int toAWTFontStyle() {
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
     * This enum contains constant values representing
     * the type of action(s) to be performed by a Drag and Drop operation.
     * These constants are a direct mapping to the constants defined in the {@link TransferHandler} class
     * as well as the {@link java.awt.dnd.DnDConstants} class.
     */
    @Immutable
    public enum DragAction implements UIEnum<DragAction>
    {
        /**
         * An <code>int</code> representing no transfer action.
         */
        NONE,
        /**
         * An <code>int</code> representing a &quot;copy&quot; transfer action.
         * This value is used when data is copied to a clipboard
         * or copied elsewhere in a drag and drop operation.
         */
        COPY,
        /**
         * An <code>int</code> representing a &quot;move&quot; transfer action.
         * This value is used when data is moved to a clipboard (i.e. a cut)
         * or moved elsewhere in a drag and drop operation.
         */
        MOVE,
        /**
         * An <code>int</code> representing a source action capability of either
         * &quot;copy&quot; or &quot;move&quot;.
         */
        COPY_OR_MOVE,
        /**
         * An <code>int</code> representing a &quot;link&quot; transfer action.
         * This value is used to specify that data should be linked in a drag
         * and drop operation.
         *
         * @see java.awt.dnd.DnDConstants#ACTION_LINK
         */
        LINK;

        public int toIntCode() {
            switch ( this ) {
                case NONE:        return TransferHandler.NONE;
                case COPY:        return TransferHandler.COPY;
                case MOVE:        return TransferHandler.MOVE;
                case COPY_OR_MOVE:return TransferHandler.COPY_OR_MOVE;
                case LINK:        return TransferHandler.LINK;
            }
            throw new RuntimeException();
        }

        /**
         *  Use this to check if this enum action is a particular {@link java.awt.dnd.DnDConstants} action.
         *  @param action The action to check against.
         *  @return {@code true} if this enum action is the same as the given {@code action}, {@code false} otherwise.
         */
        public boolean is( int action ) {
            return (action & toIntCode()) != 0;
        }
    }

    /**
     *  Set of enum instances defining common types of Swing look and feels.
     *  Use {@link UI#currentLookAndFeel()} to check which look and feel is currently active.
     */
    @Immutable
    public enum LookAndFeel implements UIEnum<LookAndFeel> {
        OTHER,
        METAL,
        FLAT_LAF,
        NIMBUS;
    }

    /**
     *  SwingTree tries to be compatible with different look and feels, which is
     *  why it maintains a set of constants for the most common look and feels through
     *  the {@link swingtree.UI.LookAndFeel} enum.
     *  This method returns the current look and feel of the application
     *  or {@link swingtree.UI.LookAndFeel#OTHER} if the look and feel is not recognized.
     * @return One of
     *            <ul>
     *                <li>{@link swingtree.UI.LookAndFeel#FLAT_LAF}</li>
     *                <li>{@link swingtree.UI.LookAndFeel#NIMBUS}</li>
     *                <li>{@link swingtree.UI.LookAndFeel#METAL}</li>
     *            </ul>
     *            or {@link swingtree.UI.LookAndFeel#OTHER} if none of the above
     *            was recognized.
     */
    public static LookAndFeel currentLookAndFeel() {
        try {
            String laf = UIManager.getLookAndFeel().getClass().getName();
            if ( laf.contains("FlatLaf") ) return LookAndFeel.FLAT_LAF;
            if ( laf.contains("Nimbus")  ) return LookAndFeel.NIMBUS;
            if ( laf.contains("Metal")   ) return LookAndFeel.METAL;
        }
        catch (Exception e) {
            log.warn(SwingTree.get().logMarker(), "Failed to determine current look and feel.", e);
        }

        return LookAndFeel.OTHER;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     *  There are two types of strategies for achieving high DPI scaling in Swing.
     *  The first one is available since Java 9, and it uses the system property {@code sun.java2d.uiScale}
     *  to scale the {@link java.awt.geom.AffineTransform} of the {@link Graphics2D} graphics context.
     *  The second one is look and feel / client code dependent scaling, which is what this library uses.
     *  <p>
     *  The float based scaling factor returned by this method can be used as a multiplier in order
     *  to scale something from "developer pixels" to "component pixels" / "look and feel pixels".
     *  Conversely, by dividing a number using this factor, you convert something from "component pixels"
     *  to platform-agnostic "developer pixel".<br>
     *  The scaling factor is computed by SwingTree automatically based on the system font.
     *  Anything you do through the SwingTree API, will be scaled for you,<br>
     *  but if you write code against raw Swing, you may need to use this scale factor
     *  to ensure consistent scaling support across screens with varying DPI.
     *  Most commonly, you will need to do manual scaling when defining component dimensions
     *  or to scale custom {@link Graphics2D} based painting operations.
     *  <br>
     *  For configuring a manual scaling factor, see {@link SwingTree#setUiScaleFactor(float)}
     *  or {@link SwingTree#initialiseUsing(SwingTreeConfigurator)}.
     *
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
     * Converts a double in "developer pixel size" to "Look and Feel / component pixel size",
     * by multiplying the given double value with the user scale factor.
     * See {@link swingtree.SwingTree} for more information about how the user scale factor is determined.
     *
     * @param value The double value to scale from developer pixel size to component pixel size.
     * @return The scaled double value.
     */
    public static double scale( double value ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1 ? value : (value * scaleFactor) );
    }

    /**
     * Converts an int representing "developer pixel size" to "Look and Feel / component pixel size",
     * by multiplying the given int value with the user "scale factor" and then rounding the result.
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

    public static Dimension unscale( Dimension size ) {
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1f ? size : new Dimension(unscale(size.width), unscale(size.height)) );
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
        Objects.requireNonNull(dimension);
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1f)
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
        Objects.requireNonNull(rectangle);
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1f )
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
        Objects.requireNonNull(rectangle);
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        if ( scaleFactor == 1f )
            return rectangle;
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
     *  Takes an ellipse and scales it with the user scale factor
     *  or returns the provided ellipse if the user scale factor is 1.
     * @param ellipse The ellipse to scale to the current UI scale factor.
     * @return The scaled ellipse.
     */
    public static Ellipse2D scale( Ellipse2D ellipse ) {
        Objects.requireNonNull(ellipse);
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        if ( scaleFactor == 1f)
            return ellipse;
        if ( ellipse instanceof Ellipse2D.Float )
            return new Ellipse2D.Float(
                    (float) UI.scale( ellipse.getX() ), (float) UI.scale( ellipse.getY() ),
                    (float) UI.scale( ellipse.getWidth() ), (float) UI.scale( ellipse.getHeight() )
                );
        else
            return new Ellipse2D.Double(
                    UI.scale( ellipse.getX() ), UI.scale( ellipse.getY() ),
                    UI.scale( ellipse.getWidth() ), UI.scale( ellipse.getHeight() )
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
        Objects.requireNonNull(insets);
        float scaleFactor = SwingTree.get().getUiScaleFactor();
        return ( scaleFactor == 1f )
                ? insets
                : (insets instanceof UIResource
                    ? new InsetsUIResource( UI.scale( insets.top ), UI.scale( insets.left ), UI.scale( insets.bottom ), UI.scale( insets.right ) )
                    : new Insets          ( UI.scale( insets.top ), UI.scale( insets.left ), UI.scale( insets.bottom ), UI.scale( insets.right ) ));
    }

    private UI(){ super(); } // This is a static API

    /**
     * A convenience method which ensures that a supplied {@link Runnable}
     * is executed on the GUI thead (AWT event dispatch thread)
     * using the following condition:<br>
     * <pre>{@code
     *      if ( !UI.thisIsUIThread() )
     *          SwingUtilities.invokeLater(runnable);
     *      else
     *          runnable.run();
     * }</pre>
     * The <i>runnable.run()</i> will be executed immediately, if the invoker already
     * is the GUI thread. Otherwise, it will be queued for execution on the GUI thread.
     * The 'invokeLater' execution will happen after all pending AWT events have been processed.
     * This method should be used when an application thread needs to update the GUI.
     *
     * @param runnable the instance of {@code Runnable} which needs to be run by the GUI thread.
     * @see #runNow
     * @see #runLater(int, Runnable)
     * @see #runLater(double, TimeUnit, Runnable)
     * @see #thisIsUIThread()
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
     * which causes the supplied {@link Runnable} to be executed asynchronously on the
     * AWT event dispatching thread at a later point in time. <br>
     * This will happen after all pending AWT events have been processed.
     * This method should be used when an application thread needs to update the GUI.
     * In the following example the <code>runLater</code> call queues
     * the <code>Runnable</code> lambda on the event dispatching thread and
     * then prints a message:
     * <pre>{@code
     *  UI.run( ()-> System.out.println(
     *          "Hello World on " + Thread.currentThread()
     *      ));
     *
     *  System.out.println(
     *      "This will probably be displayed first!"
     *  );
     * }</pre>
     * If {@code runLater} is called from the event dispatching thread --
     * for example, from a JButton's ActionListener -- the <code>Runnable</code> will
     * still be deferred until all pending events have been processed.
     * Note that if the <code>Runnable</code> throws an uncaught exception
     * the event dispatching thread will unwind (not the current thread).
     *
     * @param runnable the instance of {@code Runnable} to be executed later by the GUI thread
     * @see #run
     * @see #runNow
     * @see #runLater(int, Runnable)
     * @see #runLater(double, TimeUnit, Runnable)
     * @see #thisIsUIThread()
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
     * after a particular delay.<br>
     * If the supplied delay is smaller or equal to {@code 0}, then the runnable
     * will be passed to {@link #run(Runnable)} to ensure the task is executed on the
     * GUI thread as fast as possible.<br>
     * <br>
     * In the following example the <code>invokeLater</code> call queues
     * the <code>Runnable</code> lambda containing a print statement
     * on the event dispatching thread and
     * then prints a message.
     * <pre>{@code
     *  UI.runLater( 1000, ()->System.out.println(
     *          "Hello World on " + Thread.currentThread())
     *      );
     *
     *  System.out.println(
     *      "This will probably be displayed first!"
     *  );
     * }</pre>
     * If {@code runLater} is called from the event dispatching thread --
     * for example, from a JButton's ActionListener -- the <code>Runnable</code> will
     * still be deferred until the specified delay has passed.
     * Note that if the <code>Runnable</code> throws an uncaught exception
     * the event dispatching thread will unwind (not the current thread).
     *
     * @param delay The delay in milliseconds.
     * @param runnable the instance of {@code Runnable} to be executed by the GUI thread after the desired delay.
     * @see #run
     * @see #runNow
     * @see #runLater(double, TimeUnit, Runnable)
     * @see #thisIsUIThread()
     */
    public static void runLater( int delay, Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        if ( delay <= 0 ) {
            run(runnable);
            return;
        }
        Timer timer = new Timer( delay, e -> { runnable.run(); } );
        timer.setRepeats(false); // Execute only once
        timer.setInitialDelay(delay);
        timer.start();
    }

    /**
     * A convenience method for {@link SwingUtilities#invokeLater(Runnable)},
     * which causes the supplied {@link Runnable} to be executed asynchronously on the
     * AWT event dispatching thread after the specified delay
     * has passed in the given time unit.<br>
     * If the supplied delay is smaller or equal to {@code 0}, then the runnable
     * will be passed to {@link #run(Runnable)} to ensure the task is executed on the
     * GUI thread as fast as possible.<br>
     * <br>
     * This method should be used when an application thread needs to update the GUI
     * after a particular delay.
     * In the following example the <code>invokeLater</code> call queues
     * the <code>Runnable</code> lambda containing a print statement
     * on the event dispatching thread and then prints a message.
     * <pre>{@code
     *  UI.runLater( 1000, TimeUnit.MILLISECONDS, ()->
     *          System.out.println("Hello World on " + Thread.currentThread()
     *      ));
     *
     *  System.out.println("This will certainly be displayed first!");
     * }</pre>
     * If {@code runLater} is called from the event dispatching thread --
     * for example, from a {@code JButton}'s ActionListener -- the <code>Runnable</code> will
     * still be deferred until the specified delay has passed.
     * Note that if the <code>Runnable</code> throws an uncaught exception
     * the event dispatching thread will unwind (not the current thread).
     *
     * @param delay The delay in the given time unit.
     * @param unit The time unit of the delay parameter.
     * @param runnable the instance of {@code Runnable}
     * @see #run
     * @see #runNow
     * @see #runLater(int, Runnable)
     * @see #thisIsUIThread()
     */
    public static void runLater( double delay, TimeUnit unit, Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        NullUtil.nullArgCheck(unit, "unit", TimeUnit.class);
        if ( delay <= 0 ) {
            run(runnable);
            return;
        }
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
     * @see #runLater(int, Runnable)
     * @see #runLater(double, TimeUnit, Runnable)
     */
    public static void runNow( Runnable runnable ) {
        NullUtil.nullArgCheck(runnable, "runnable", Runnable.class);
        Result.ofTry(()->{
            if ( !UI.thisIsUIThread() )
                SwingUtilities.invokeAndWait(runnable);
            else
                runnable.run();
        });
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
        AtomicReference<@Nullable T> ref = new AtomicReference<>();
        runNow( () -> ref.set(supplier.get()) );
        return Objects.requireNonNull(ref.get());
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
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Panel extends JPanel implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Label extends JLabel implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TextField extends JTextField implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TextArea extends JTextArea implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class CheckBox extends JCheckBox implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Button extends JButton implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ToggleButton extends JToggleButton implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class RadioButton extends JRadioButton implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ComboBox<E> extends JComboBox<E> implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ListView<E> extends JList<E> implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Table extends JTable implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TableHeader extends JTableHeader implements StylableComponent {
        private @Nullable Function<Integer, String> _toolTipTextSupplier;
        public TableHeader() { super(); }
        public TableHeader(TableColumnModel model) { super(model); }
        /**
         *  Use this for defining the header cell tool tips.
         *  This models the tool tip of all header cells using a function which receives the column index
         *  and returns the tool tip text for that column.
         * @param toolTipTextSupplier A function which receives the column index and returns the
         *                            tool tip text for that column.
         */
        public void setToolTipsSupplier( Function<Integer, String> toolTipTextSupplier ) {
            Objects.requireNonNull(toolTipTextSupplier);
            _toolTipTextSupplier = toolTipTextSupplier;
        }
        /**
         *  Use this for defining a fixed set of tool tip texts for the columns.
         * @param toolTips The tool tip texts for the columns.
         */
        public void setToolTips( String... toolTips ) {
            Objects.requireNonNull(toolTips);
            setToolTipsSupplier( i -> toolTips[i] );
        }
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintComponent); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
        @Override public String getToolTipText(MouseEvent e) {
            int col = columnAtPoint(e.getPoint());
            int modelCol = Optional.ofNullable(getTable())
                                    .map( t -> t.convertColumnIndexToModel(col) )
                                    .orElse(col);
            String retStr = "";
            try {
                if ( _toolTipTextSupplier != null )
                    retStr = _toolTipTextSupplier.apply(modelCol);
            }
            catch ( NullPointerException | ArrayIndexOutOfBoundsException ex ) {
                retStr = "";
            }
            return  ( retStr.isEmpty() ? super.getToolTipText(e) : retStr );
        }
    }
    /** {inheritDoc} */
    public static class Slider extends JSlider implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Separator extends JSeparator implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class PopupMenu extends JPopupMenu implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class MenuItem extends JMenuItem implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class RadioButtonMenuItem extends JRadioButtonMenuItem implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
     public static class CheckBoxMenuItem extends JCheckBoxMenuItem implements StylableComponent {
         @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
         @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
     }
    /** {inheritDoc} */
    public static class Menu extends JMenu implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class MenuBar extends JMenuBar implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ScrollPane extends JScrollPane implements StylableComponent {
        public ScrollPane() { this(null); }
        public ScrollPane(java.awt.@Nullable Component view) {
            super(view);
            addMouseWheelListener(new NestedJScrollPanelScrollCorrection(this));
        }
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TabbedPane extends JTabbedPane implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ToolBar extends JToolBar implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ToolTip extends JToolTip implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Tree extends JTree implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class TextPane extends JTextPane implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Spinner extends JSpinner implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class SplitPane extends JSplitPane implements StylableComponent {
        SplitPane( Align align ) { super(align.forSplitPane()); }
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class PasswordField extends JPasswordField implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class ProgressBar extends JProgressBar implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class EditorPane extends JEditorPane implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g) { paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {@inheritDoc} */
    public static class FormattedTextField extends JFormattedTextField implements StylableComponent {
        @Override public void paintComponent(Graphics g){ paintBackground(g, super::paintComponent); }
        @Override public void paintChildren(Graphics g){ paintForeground(g, super::paintChildren); }
        @Override public void setUISilently( ComponentUI ui ) { this.ui = ui; }
    }
    /** {inheritDoc} */
    public static class Box extends JBox {/* Already implemented */}
    /** {@inheritDoc} */
    public static class SplitButton extends JSplitButton {/* Already implemented */}

    /**
     * This {@code Color} class is a refined and more complete/modernized
     * implementation of the {@link java.awt.Color} class which models colors in the default
     * sRGB color space or colors in arbitrary color spaces identified by a
     * {@link ColorSpace}.
     * <br>
     * The original {@link java.awt.Color} class is an immutable and value based class
     * (it overrides {@link Object#equals(Object) equals} and {@link Object#hashCode() hashCode})
     * but it is missing so called with-methods, which are a modern way to create
     * updated copies of an object without having to call the full constructor
     * with all the parameters.
     * <p>
     * Here a list of the most useful features additionally provided by this class:
     * <p>
     *     <b>With-Methods</b>
     *     <ul>
     *          <li>{@link #withRed(double)}, {@link #withGreen(double)}, {@link #withBlue(double)}</li>
     *          <li>{@link #withOpacity(double)}, {@link #withAlpha(int)}</li>
     *          <li>{@link #withHue(double)}, {@link #withSaturation(double)}, {@link #withBrightness(double)}</li>
     *          <li>{@link #brighterBy(double)}, {@link #darkerBy(double)}</li>
     *          <li>{@link #saturate()}, {@link #saturateBy(double)}
     *          <li>{@link #desaturate()}, {@link #desaturateBy(double)}</li>
     *          <li>{@link #grayscale()}</li>
     *          <li>{@link #invert()}</li>
     *          <li>...</li>
     *      </ul>
     *      <b>Various Color Constants</b>
     *      <ul>
     *          <li>{@link #TRANSPARENT}</li>
     *          <li>{@link #ALICEBLUE}</li>
     *          <li>{@link #ANTIQUEWHITE}</li>
     *          <li>{@link #AQUA}</li>
     *          <li>{@link #AQUAMARINE}</li>
     *          <li>...</li>
     *      </ul>
     *      Also note that this class overrides and fixes the {@link java.awt.Color#darker()}
     *      and {@link java.awt.Color#brighter()} methods. Not only do they now return
     *      a {@code Color} type, but also use an implementation which updates the
     *      brightness/darkness in terms of the HSB color space.<br>
     *      (The original implementation considers colors like
     *      {@code Color.BLUE}, {@code Color.RED} and {@code Color.GREEN}
     *      to be the brightest possible colors, which is not true in terms
     *      of the much more useful HSB color space modelling.)
     *
     * <p>
     * Besides the RGB values every
     * fully opaque {@code Color} also has an implicit alpha value of 1.0.
     * But you may also construct a {@code Color} with an explicit alpha value
     * by using the {@link #Color(float, float, float, float)} constructor for example.
     * The alpha value defines the transparency of a color and can be represented by
     * a float value in the range 0.0&nbsp;-&nbsp;1.0 or 0&nbsp;-&nbsp;255.
     * An alpha value of 1.0 or 255 means that the color is completely
     * opaque and an alpha value of 0 or 0.0 means that the color is
     * completely transparent.
     * When constructing a {@code Color} with an explicit alpha or
     * getting the color/alpha components of a {@code Color}, the color
     * components are never premultiplied by the alpha component.
     * <p>
     * The default color space for the Java 2D(tm) API is sRGB, a proposed
     * standard RGB color space.  For further information on sRGB,
     * see <A href="http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html">
     * http://www.w3.org/pub/WWW/Graphics/Color/sRGB.html
     * </A>.
     *
     * @version     22 March 2024
     * @author      Daniel Nepp
     * @see         ColorSpace
     * @see         AlphaComposite
     */
    @Immutable
    public static final class Color extends java.awt.Color
    {
        private static final Logger log = LoggerFactory.getLogger(Color.class);

        /**
         *  This constant is a {@link Color} object with all of its rgba values set to 0.
         *  Its identity is used to represent the absence of a color being specified,
         *  and it is used as a safe replacement for null,
         *  meaning that when the style engine of a component encounters it, it will pass it onto
         *  the {@link java.awt.Component#setBackground(java.awt.Color)} and
         *  {@link java.awt.Component#setForeground(java.awt.Color)} methods as null.
         *  Passing null to these methods means that the look and feel determines the coloring.
         */
        public static final Color UNDEFINED = new Color(0f, 0f, 0f, 0f);
        /**
         * A fully transparent color with an ARGB value of #00000000.
         */
        public static final Color TRANSPARENT = new Color(0f, 0f, 0f, 0f);

        /**
         * The color alice blue with an RGB value of #F0F8FF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0F8FF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color ALICEBLUE = new Color(0.9411765f, 0.972549f, 1.0f);

        /**
         * The color antique white with an RGB value of #FAEBD7
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAEBD7;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color ANTIQUEWHITE = new Color(0.98039216f, 0.92156863f, 0.84313726f);

        /**
         * The color aqua with an RGB value of #00FFFF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color AQUA = new Color(0.0f, 1.0f, 1.0f);

        /**
         * The color aquamarine with an RGB value of #7FFFD4
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFFD4;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color AQUAMARINE = new Color(0.49803922f, 1.0f, 0.83137256f);

        /**
         * The color azure with an RGB value of #F0FFFF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFFF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color AZURE = new Color(0.9411765f, 1.0f, 1.0f);

        /**
         * The color beige with an RGB value of #F5F5DC
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5DC;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BEIGE = new Color(0.9607843f, 0.9607843f, 0.8627451f);

        /**
         * The color bisque with an RGB value of #FFE4C4
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4C4;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BISQUE = new Color(1.0f, 0.89411765f, 0.76862746f);

        /**
         * The color black with an RGB value of #000000
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#000000;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BLACK = new Color(0.0f, 0.0f, 0.0f);

        /**
         * The color blanched almond with an RGB value of #FFEBCD
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEBCD;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BLANCHEDALMOND = new Color(1.0f, 0.92156863f, 0.8039216f);

        /**
         * The color blue with an RGB value of #0000FF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000FF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BLUE = new Color(0.0f, 0.0f, 1.0f);

        /**
         * The color blue violet with an RGB value of #8A2BE2
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#8A2BE2;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BLUEVIOLET = new Color(0.5411765f, 0.16862746f, 0.8862745f);

        /**
         * The color brown with an RGB value of #A52A2A
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#A52A2A;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BROWN = new Color(0.64705884f, 0.16470589f, 0.16470589f);

        /**
         * The color burly wood with an RGB value of #DEB887
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#DEB887;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color BURLYWOOD = new Color(0.87058824f, 0.72156864f, 0.5294118f);

        /**
         * The color cadet blue with an RGB value of #5F9EA0
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#5F9EA0;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CADETBLUE = new Color(0.37254903f, 0.61960787f, 0.627451f);

        /**
         * The color chartreuse with an RGB value of #7FFF00
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#7FFF00;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CHARTREUSE = new Color(0.49803922f, 1.0f, 0.0f);

        /**
         * The color chocolate with an RGB value of #D2691E
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2691E;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CHOCOLATE = new Color(0.8235294f, 0.4117647f, 0.11764706f);

        /**
         * The color coral with an RGB value of #FF7F50
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF7F50;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CORAL = new Color(1.0f, 0.49803922f, 0.3137255f);

        /**
         * The color cornflower blue with an RGB value of #6495ED
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#6495ED;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CORNFLOWERBLUE = new Color(0.39215687f, 0.58431375f, 0.92941177f);

        /**
         * The color cornsilk with an RGB value of #FFF8DC
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF8DC;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CORNSILK = new Color(1.0f, 0.972549f, 0.8627451f);

        /**
         * The color crimson with an RGB value of #DC143C
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#DC143C;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CRIMSON = new Color(0.8627451f, 0.078431375f, 0.23529412f);

        /**
         * The color cyan with an RGB value of #00FFFF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FFFF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color CYAN = new Color(0.0f, 1.0f, 1.0f);

        /**
         * The color dark blue with an RGB value of #00008B
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00008B;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKBLUE = new Color(0.0f, 0.0f, 0.54509807f);

        /**
         * The color dark cyan with an RGB value of #008B8B
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#008B8B;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKCYAN = new Color(0.0f, 0.54509807f, 0.54509807f);

        /**
         * The color dark goldenrod with an RGB value of #B8860B
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#B8860B;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKGOLDENROD = new Color(0.72156864f, 0.5254902f, 0.043137256f);

        /**
         * The color dark gray with an RGB value of #A9A9A9
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKGRAY = new Color(0.6627451f, 0.6627451f, 0.6627451f);

        /**
         * The color dark green with an RGB value of #006400
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#006400;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKGREEN = new Color(0.0f, 0.39215687f, 0.0f);

        /**
         * The color dark grey with an RGB value of #A9A9A9
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#A9A9A9;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKGREY             = DARKGRAY;

        /**
         * The color dark khaki with an RGB value of #BDB76B
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#BDB76B;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKKHAKI = new Color(0.7411765f, 0.7176471f, 0.41960785f);

        /**
         * The color dark magenta with an RGB value of #8B008B
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B008B;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKMAGENTA = new Color(0.54509807f, 0.0f, 0.54509807f);

        /**
         * The color dark olive green with an RGB value of #556B2F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#556B2F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKOLIVEGREEN = new Color(0.33333334f, 0.41960785f, 0.18431373f);

        /**
         * The color dark orange with an RGB value of #FF8C00
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF8C00;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKORANGE = new Color(1.0f, 0.54901963f, 0.0f);

        /**
         * The color dark orchid with an RGB value of #9932CC
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#9932CC;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKORCHID = new Color(0.6f, 0.19607843f, 0.8f);

        /**
         * The color dark red with an RGB value of #8B0000
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B0000;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKRED = new Color(0.54509807f, 0.0f, 0.0f);

        /**
         * The color dark salmon with an RGB value of #E9967A
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#E9967A;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKSALMON = new Color(0.9137255f, 0.5882353f, 0.47843137f);

        /**
         * The color dark sea green with an RGB value of #8FBC8F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#8FBC8F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKSEAGREEN = new Color(0.56078434f, 0.7372549f, 0.56078434f);

        /**
         * The color dark slate blue with an RGB value of #483D8B
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#483D8B;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKSLATEBLUE = new Color(0.28235295f, 0.23921569f, 0.54509807f);

        /**
         * The color dark slate gray with an RGB value of #2F4F4F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKSLATEGRAY = new Color(0.18431373f, 0.30980393f, 0.30980393f);

        /**
         * The color dark slate grey with an RGB value of #2F4F4F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#2F4F4F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKSLATEGREY        = DARKSLATEGRAY;

        /**
         * The color dark turquoise with an RGB value of #00CED1
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00CED1;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKTURQUOISE = new Color(0.0f, 0.80784315f, 0.81960785f);

        /**
         * The color dark violet with an RGB value of #9400D3
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#9400D3;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DARKVIOLET = new Color(0.5803922f, 0.0f, 0.827451f);

        /**
         * The color deep pink with an RGB value of #FF1493
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF1493;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DEEPPINK = new Color(1.0f, 0.078431375f, 0.5764706f);

        /**
         * The color deep sky blue with an RGB value of #00BFFF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00BFFF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DEEPSKYBLUE = new Color(0.0f, 0.7490196f, 1.0f);

        /**
         * The color dim gray with an RGB value of #696969
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DIMGRAY = new Color(0.4117647f, 0.4117647f, 0.4117647f);

        /**
         * The color dim grey with an RGB value of #696969
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#696969;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DIMGREY              = DIMGRAY;

        /**
         * The color dodger blue with an RGB value of #1E90FF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#1E90FF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color DODGERBLUE = new Color(0.11764706f, 0.5647059f, 1.0f);

        /**
         * The color firebrick with an RGB value of #B22222
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#B22222;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color FIREBRICK = new Color(0.69803923f, 0.13333334f, 0.13333334f);

        /**
         * The color floral white with an RGB value of #FFFAF0
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAF0;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color FLORALWHITE = new Color(1.0f, 0.98039216f, 0.9411765f);

        /**
         * The color forest green with an RGB value of #228B22
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#228B22;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color FORESTGREEN = new Color(0.13333334f, 0.54509807f, 0.13333334f);

        /**
         * The color fuchsia with an RGB value of #FF00FF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color FUCHSIA = new Color(1.0f, 0.0f, 1.0f);

        /**
         * The color gainsboro with an RGB value of #DCDCDC
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#DCDCDC;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GAINSBORO = new Color(0.8627451f, 0.8627451f, 0.8627451f);

        /**
         * The color ghost white with an RGB value of #F8F8FF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F8F8FF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GHOSTWHITE = new Color(0.972549f, 0.972549f, 1.0f);

        /**
         * The color gold with an RGB value of #FFD700
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFD700;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GOLD = new Color(1.0f, 0.84313726f, 0.0f);

        /**
         * The color goldenrod with an RGB value of #DAA520
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#DAA520;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GOLDENROD = new Color(0.85490197f, 0.64705884f, 0.1254902f);

        /**
         * The color gray with an RGB value of #808080
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GRAY = new Color(0.5019608f, 0.5019608f, 0.5019608f);

        /**
         * The color green with an RGB value of #008000
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#008000;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GREEN = new Color(0.0f, 0.5019608f, 0.0f);

        /**
         * The color green yellow with an RGB value of #ADFF2F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADFF2F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GREENYELLOW = new Color(0.6784314f, 1.0f, 0.18431373f);

        /**
         * The color grey with an RGB value of #808080
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#808080;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color GREY                 = GRAY;

        /**
         * The color honeydew with an RGB value of #F0FFF0
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0FFF0;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color HONEYDEW = new Color(0.9411765f, 1.0f, 0.9411765f);

        /**
         * The color hot pink with an RGB value of #FF69B4
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF69B4;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color HOTPINK = new Color(1.0f, 0.4117647f, 0.7058824f);

        /**
         * The color indian red with an RGB value of #CD5C5C
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD5C5C;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color INDIANRED = new Color(0.8039216f, 0.36078432f, 0.36078432f);

        /**
         * The color indigo with an RGB value of #4B0082
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#4B0082;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color INDIGO = new Color(0.29411766f, 0.0f, 0.50980395f);

        /**
         * The color ivory with an RGB value of #FFFFF0
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFF0;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color IVORY = new Color(1.0f, 1.0f, 0.9411765f);

        /**
         * The color khaki with an RGB value of #F0E68C
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F0E68C;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color KHAKI = new Color(0.9411765f, 0.9019608f, 0.54901963f);

        /**
         * The color lavender with an RGB value of #E6E6FA
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#E6E6FA;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LAVENDER = new Color(0.9019608f, 0.9019608f, 0.98039216f);

        /**
         * The color lavender blush with an RGB value of #FFF0F5
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF0F5;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LAVENDERBLUSH = new Color(1.0f, 0.9411765f, 0.9607843f);

        /**
         * The color lawn green with an RGB value of #7CFC00
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#7CFC00;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LAWNGREEN = new Color(0.4862745f, 0.9882353f, 0.0f);

        /**
         * The color lemon chiffon with an RGB value of #FFFACD
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFACD;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LEMONCHIFFON = new Color(1.0f, 0.98039216f, 0.8039216f);

        /**
         * The color light blue with an RGB value of #ADD8E6
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#ADD8E6;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTBLUE = new Color(0.6784314f, 0.84705883f, 0.9019608f);

        /**
         * The color light coral with an RGB value of #F08080
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F08080;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTCORAL = new Color(0.9411765f, 0.5019608f, 0.5019608f);

        /**
         * The color light cyan with an RGB value of #E0FFFF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#E0FFFF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTCYAN = new Color(0.8784314f, 1.0f, 1.0f);

        /**
         * The color light goldenrod yellow with an RGB value of #FAFAD2
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAFAD2;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTGOLDENRODYELLOW = new Color(0.98039216f, 0.98039216f, 0.8235294f);

        /**
         * The color light gray with an RGB value of #D3D3D3
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTGRAY = new Color(0.827451f, 0.827451f, 0.827451f);

        /**
         * The color light green with an RGB value of #90EE90
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#90EE90;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTGREEN = new Color(0.5647059f, 0.93333334f, 0.5647059f);

        /**
         * The color light grey with an RGB value of #D3D3D3
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#D3D3D3;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTGREY            = LIGHTGRAY;

        /**
         * The color light pink with an RGB value of #FFB6C1
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFB6C1;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTPINK = new Color(1.0f, 0.7137255f, 0.75686276f);

        /**
         * The color light salmon with an RGB value of #FFA07A
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA07A;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTSALMON = new Color(1.0f, 0.627451f, 0.47843137f);

        /**
         * The color light sea green with an RGB value of #20B2AA
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#20B2AA;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTSEAGREEN = new Color(0.1254902f, 0.69803923f, 0.6666667f);

        /**
         * The color light sky blue with an RGB value of #87CEFA
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEFA;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTSKYBLUE = new Color(0.5294118f, 0.80784315f, 0.98039216f);

        /**
         * The color light slate gray with an RGB value of #778899
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTSLATEGRAY = new Color(0.46666667f, 0.53333336f, 0.6f);

        /**
         * The color light slate grey with an RGB value of #778899
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#778899;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTSLATEGREY       = LIGHTSLATEGRAY;

        /**
         * The color light steel blue with an RGB value of #B0C4DE
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0C4DE;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTSTEELBLUE = new Color(0.6901961f, 0.76862746f, 0.87058824f);

        /**
         * The color light yellow with an RGB value of #FFFFE0
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFE0;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIGHTYELLOW = new Color(1.0f, 1.0f, 0.8784314f);

        /**
         * The color lime with an RGB value of #00FF00
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF00;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIME = new Color(0.0f, 1.0f, 0.0f);

        /**
         * The color lime green with an RGB value of #32CD32
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#32CD32;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LIMEGREEN = new Color(0.19607843f, 0.8039216f, 0.19607843f);

        /**
         * The color linen with an RGB value of #FAF0E6
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FAF0E6;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color LINEN = new Color(0.98039216f, 0.9411765f, 0.9019608f);

        /**
         * The color magenta with an RGB value of #FF00FF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF00FF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MAGENTA = new Color(1.0f, 0.0f, 1.0f);

        /**
         * The color maroon with an RGB value of #800000
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#800000;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MAROON = new Color(0.5019608f, 0.0f, 0.0f);

        /**
         * The color medium aquamarine with an RGB value of #66CDAA
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#66CDAA;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMAQUAMARINE = new Color(0.4f, 0.8039216f, 0.6666667f);

        /**
         * The color medium blue with an RGB value of #0000CD
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#0000CD;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMBLUE = new Color(0.0f, 0.0f, 0.8039216f);

        /**
         * The color medium orchid with an RGB value of #BA55D3
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#BA55D3;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMORCHID = new Color(0.7294118f, 0.33333334f, 0.827451f);

        /**
         * The color medium purple with an RGB value of #9370DB
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#9370DB;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMPURPLE = new Color(0.5764706f, 0.4392157f, 0.85882354f);

        /**
         * The color medium sea green with an RGB value of #3CB371
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#3CB371;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMSEAGREEN = new Color(0.23529412f, 0.7019608f, 0.44313726f);

        /**
         * The color medium slate blue with an RGB value of #7B68EE
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#7B68EE;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMSLATEBLUE = new Color(0.48235294f, 0.40784314f, 0.93333334f);

        /**
         * The color medium spring green with an RGB value of #00FA9A
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FA9A;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMSPRINGGREEN = new Color(0.0f, 0.98039216f, 0.6039216f);

        /**
         * The color medium turquoise with an RGB value of #48D1CC
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#48D1CC;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMTURQUOISE = new Color(0.28235295f, 0.81960785f, 0.8f);

        /**
         * The color medium violet red with an RGB value of #C71585
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#C71585;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MEDIUMVIOLETRED = new Color(0.78039217f, 0.08235294f, 0.52156866f);

        /**
         * The color midnight blue with an RGB value of #191970
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#191970;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MIDNIGHTBLUE = new Color(0.09803922f, 0.09803922f, 0.4392157f);

        /**
         * The color mint cream with an RGB value of #F5FFFA
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5FFFA;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MINTCREAM = new Color(0.9607843f, 1.0f, 0.98039216f);

        /**
         * The color misty rose with an RGB value of #FFE4E1
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4E1;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MISTYROSE = new Color(1.0f, 0.89411765f, 0.88235295f);

        /**
         * The color moccasin with an RGB value of #FFE4B5
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFE4B5;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color MOCCASIN = new Color(1.0f, 0.89411765f, 0.70980394f);

        /**
         * The color navajo white with an RGB value of #FFDEAD
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDEAD;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color NAVAJOWHITE = new Color(1.0f, 0.87058824f, 0.6784314f);

        /**
         * The color navy with an RGB value of #000080
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#000080;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color NAVY = new Color(0.0f, 0.0f, 0.5019608f);

        /**
         * The color "oak".
         */
        public static final Color OAK = new Color(216/255f, 181/255f, 137/255f);

        /**
         * The color old lace with an RGB value of #FDF5E6
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FDF5E6;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color OLDLACE = new Color(0.99215686f, 0.9607843f, 0.9019608f);

        /**
         * The color olive with an RGB value of #808000
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#808000;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color OLIVE = new Color(0.5019608f, 0.5019608f, 0.0f);

        /**
         * The color olive drab with an RGB value of #6B8E23
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#6B8E23;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color OLIVEDRAB = new Color(0.41960785f, 0.5568628f, 0.13725491f);

        /**
         * The color orange with an RGB value of #FFA500
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFA500;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color ORANGE = new Color(1.0f, 0.64705884f, 0.0f);

        /**
         * The color orange red with an RGB value of #FF4500
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF4500;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color ORANGERED = new Color(1.0f, 0.27058825f, 0.0f);

        /**
         * The color orchid with an RGB value of #DA70D6
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#DA70D6;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color ORCHID = new Color(0.85490197f, 0.4392157f, 0.8392157f);

        /**
         * The color pale goldenrod with an RGB value of #EEE8AA
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#EEE8AA;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PALEGOLDENROD = new Color(0.93333334f, 0.9098039f, 0.6666667f);

        /**
         * The color pale green with an RGB value of #98FB98
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#98FB98;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PALEGREEN = new Color(0.59607846f, 0.9843137f, 0.59607846f);

        /**
         * The color pale turquoise with an RGB value of #AFEEEE
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#AFEEEE;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PALETURQUOISE = new Color(0.6862745f, 0.93333334f, 0.93333334f);

        /**
         * The color pale violet red with an RGB value of #DB7093
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#DB7093;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PALEVIOLETRED = new Color(0.85882354f, 0.4392157f, 0.5764706f);

        /**
         * The color papaya whip with an RGB value of #FFEFD5
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFEFD5;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PAPAYAWHIP = new Color(1.0f, 0.9372549f, 0.8352941f);

        /**
         * The color peach puff with an RGB value of #FFDAB9
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFDAB9;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PEACHPUFF = new Color(1.0f, 0.85490197f, 0.7254902f);

        /**
         * The color peru with an RGB value of #CD853F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#CD853F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PERU = new Color(0.8039216f, 0.52156866f, 0.24705882f);

        /**
         * The color pink with an RGB value of #FFC0CB
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFC0CB;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PINK = new Color(1.0f, 0.7529412f, 0.79607844f);

        /**
         * The color plum with an RGB value of #DDA0DD
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#DDA0DD;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PLUM = new Color(0.8666667f, 0.627451f, 0.8666667f);

        /**
         * The color powder blue with an RGB value of #B0E0E6
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#B0E0E6;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color POWDERBLUE = new Color(0.6901961f, 0.8784314f, 0.9019608f);

        /**
         * The color purple with an RGB value of #800080
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#800080;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color PURPLE = new Color(0.5019608f, 0.0f, 0.5019608f);

        /**
         * The color red with an RGB value of #FF0000
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF0000;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color RED = new Color(1.0f, 0.0f, 0.0f);

        /**
         * The color rosy brown with an RGB value of #BC8F8F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#BC8F8F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color ROSYBROWN = new Color(0.7372549f, 0.56078434f, 0.56078434f);

        /**
         * The color royal blue with an RGB value of #4169E1
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#4169E1;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color ROYALBLUE = new Color(0.25490198f, 0.4117647f, 0.88235295f);

        /**
         * The color saddle brown with an RGB value of #8B4513
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#8B4513;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SADDLEBROWN = new Color(0.54509807f, 0.27058825f, 0.07450981f);

        /**
         * The color salmon with an RGB value of #FA8072
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FA8072;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SALMON = new Color(0.98039216f, 0.5019608f, 0.44705883f);

        /**
         * The color sandy brown with an RGB value of #F4A460
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F4A460;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SANDYBROWN = new Color(0.95686275f, 0.6431373f, 0.3764706f);

        /**
         * The color sea green with an RGB value of #2E8B57
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#2E8B57;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SEAGREEN = new Color(0.18039216f, 0.54509807f, 0.34117648f);

        /**
         * The color sea shell with an RGB value of #FFF5EE
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFF5EE;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SEASHELL = new Color(1.0f, 0.9607843f, 0.93333334f);

        /**
         * The color sienna with an RGB value of #A0522D
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#A0522D;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SIENNA = new Color(0.627451f, 0.32156864f, 0.1764706f);

        /**
         * The color silver with an RGB value of #C0C0C0
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#C0C0C0;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SILVER = new Color(0.7529412f, 0.7529412f, 0.7529412f);
        /**
         * The color sky blue with an RGB value of #87CEEB
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#87CEEB;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SKYBLUE = new Color(0.5294118f, 0.80784315f, 0.92156863f);

        /**
         * The color slate blue with an RGB value of #6A5ACD
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#6A5ACD;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SLATEBLUE = new Color(0.41568628f, 0.3529412f, 0.8039216f);

        /**
         * The color slate gray with an RGB value of #708090
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SLATEGRAY = new Color(0.4392157f, 0.5019608f, 0.5647059f);

        /**
         * The color slate grey with an RGB value of #708090
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#708090;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SLATEGREY            = SLATEGRAY;

        /**
         * The color snow with an RGB value of #FFFAFA
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFAFA;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SNOW = new Color(1.0f, 0.98039216f, 0.98039216f);

        /**
         * The color spring green with an RGB value of #00FF7F
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#00FF7F;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color SPRINGGREEN = new Color(0.0f, 1.0f, 0.49803922f);

        /**
         * The color steel blue with an RGB value of #4682B4
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#4682B4;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color STEELBLUE = new Color(0.27450982f, 0.50980395f, 0.7058824f);

        /**
         * The color tan with an RGB value of #D2B48C
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#D2B48C;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color TAN = new Color(0.8235294f, 0.7058824f, 0.54901963f);

        /**
         * The color teal with an RGB value of #008080
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#008080;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color TEAL = new Color(0.0f, 0.5019608f, 0.5019608f);

        /**
         * The color thistle with an RGB value of #D8BFD8
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#D8BFD8;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color THISTLE = new Color(0.84705883f, 0.7490196f, 0.84705883f);

        /**
         * The color tomato with an RGB value of #FF6347
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FF6347;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color TOMATO = new Color(1.0f, 0.3882353f, 0.2784314f);

        /**
         * The color turquoise with an RGB value of #40E0D0
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#40E0D0;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color TURQUOISE = new Color(0.2509804f, 0.8784314f, 0.8156863f);

        /**
         * The color violet with an RGB value of #EE82EE
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#EE82EE;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color VIOLET = new Color(0.93333334f, 0.50980395f, 0.93333334f);

        /**
         * The color wheat with an RGB value of #F5DEB3
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5DEB3;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color WHEAT = new Color(0.9607843f, 0.87058824f, 0.7019608f);

        /**
         * The color white with an RGB value of #FFFFFF
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFFFF;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color WHITE = new Color(1.0f, 1.0f, 1.0f);

        /**
         * The color white smoke with an RGB value of #F5F5F5
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#F5F5F5;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color WHITESMOKE = new Color(0.9607843f, 0.9607843f, 0.9607843f);

        /**
         * The color yellow with an RGB value of #FFFF00
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#FFFF00;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color YELLOW = new Color(1.0f, 1.0f, 0.0f);

        /**
         * The color yellow green with an RGB value of #9ACD32
         * <div style="border:1px solid black;width:40px;height:20px;background-color:#9ACD32;float:right;margin: 0 10px 0 0"></div>
         */
        public static final Color YELLOWGREEN = new Color(0.6039216f, 0.8039216f, 0.19607843f);

        /**
         * Brightness change factor for darker() and brighter() methods.
         */
        private static final double DARKER_BRIGHTER_FACTOR = 0.7;

        /**
         * Saturation change factor for saturate() and desaturate() methods.
         */
        private static final double SATURATE_DESATURATE_FACTOR = 0.7;

        /**
         *  Creates a {@link Color} object from a {@link java.awt.Color} object.
         * @param color The color to convert to a color.
         * @return The color object.
         */
        public static Color of( java.awt.Color color ) {
            return new Color(color);
        }

        /**
         * Creates an opaque sRGB color with the specified RGB values in the range {@code 0-255}.
         *
         * @param red the red component, in the range {@code 0-255}
         * @param green the green component, in the range {@code 0-255}
         * @param blue the blue component, in the range {@code 0-255}
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         */
        public static Color ofRgb( int red, int green, int blue ) {
            _checkRGB(red, green, blue);
            return new Color(red, green, blue);
        }

        /**
         * Creates an sRGB color with the specified red, green, blue, and alpha
         * values in the range (0 - 255).
         *
         * @throws IllegalArgumentException if {@code r}, {@code g},
         *        {@code b} or {@code a} are outside of the range
         *        0 to 255, inclusive
         * @param r the red component
         * @param g the green component
         * @param b the blue component
         * @param a the alpha component
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getAlpha
         * @see #getRGB
         * @return the {@code Color}
         */
        public static Color ofRgba( int r, int g, int b, int a ) {
            return new Color(r, g, b, a);
        }

        /**
         * Creates an opaque sRGB color with the specified combined RGB value
         * consisting of the red component in bits 16-23, the green component
         * in bits 8-15, and the blue component in bits 0-7.  The actual color
         * used in rendering depends on finding the best match given the
         * color space available for a particular output device.  Alpha is
         * defaulted to 255.
         *
         * @param rgb the combined RGB components
         * @see java.awt.image.ColorModel#getRGBdefault
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getRGB
         * @return the {@code Color}
         */
        public static Color ofRgb( int rgb ) {
            return new Color(rgb);
        }

        /**
         * Creates an sRGB color with the specified combined RGBA value consisting
         * of the alpha component in bits 24-31, the red component in bits 16-23,
         * the green component in bits 8-15, and the blue component in bits 0-7.
         * If the {@code hasalpha} argument is {@code false}, alpha
         * is defaulted to 255.
         *
         * @param rgba the combined RGBA components
         * @param hasalpha {@code true} if the alpha bits are valid;
         *        {@code false} otherwise
         * @see java.awt.image.ColorModel#getRGBdefault
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getAlpha
         * @see #getRGB
         * @return the {@code Color} from the specified RGBA value or RGB value depending on whether the {@code hasalpha} flag is set
         */
        public static Color ofRgb( int rgba, boolean hasalpha ) {
            return new Color(rgba, hasalpha);
        }

        /**
         * Creates an opaque sRGB color with the specified red, green and blue values
         * in the range {@code 0.0-1.0}.
         *
         * @param red the red component, in the range {@code 0.0-1.0}
         * @param green the green component, in the range {@code 0.0-1.0}
         * @param blue the blue component, in the range {@code 0.0-1.0}
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         * @see #red()
         * @see #green()
         * @see #blue()
         */
        public static Color of( double red, double green, double blue ) {
            return Color.of((float) red, (float) green, (float) blue);
        }

        /**
         * Creates an opaque sRGB color with the specified red, green and blue values
         * in the range {@code 0.0-1.0}.
         *
         * @param red the red component, in the range {@code 0.0-1.0}
         * @param green the green component, in the range {@code 0.0-1.0}
         * @param blue the blue component, in the range {@code 0.0-1.0}
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         * @see #red()
         * @see #green()
         * @see #blue()
         */
        public static Color of( float red, float green, float blue ) {
            return new Color(red, green, blue);
        }

        /**
         * Creates an sRGB color with the specified RGB values in the range {@code 0-255},
         * and a given opacity.
         *
         * @param red the red component, in the range {@code 0-255}
         * @param green the green component, in the range {@code 0-255}
         * @param blue the blue component, in the range {@code 0-255}
         * @param opacity the opacity component, in the range {@code 0.0-1.0}
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         * @see #red()
         * @see #green()
         * @see #blue()
         * @see #opacity()
         */
        public static Color of( double red, double green, double blue, double opacity ) {
            return Color.of((float) red, (float) green, (float) blue, (float) opacity);
        }

        /**
         * Creates an sRGB color with the specified RGB values in the range {@code 0-255},
         * and a given opacity.
         *
         * @param red the red component, in the range {@code 0-255}
         * @param green the green component, in the range {@code 0-255}
         * @param blue the blue component, in the range {@code 0-255}
         * @param opacity the opacity component, in the range {@code 0.0-1.0}
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         * @see #red()
         * @see #green()
         * @see #blue()
         * @see #opacity()
         */
        public static Color of( float red, float green, float blue, float opacity ) {
            return new Color(red, green, blue, opacity);
        }

        /**
         * Creates a color in the specified {@code ColorSpace}
         * with the color components specified in the {@code float}
         * array and the specified alpha.  The number of components is
         * determined by the type of the {@code ColorSpace}.  For
         * example, RGB requires 3 components, but CMYK requires 4
         * components.
         * @param cspace the {@code ColorSpace} to be used to
         *                  interpret the components
         * @param components an arbitrary number of color components
         *                      that is compatible with the {@code ColorSpace}
         * @param alpha alpha value
         * @throws IllegalArgumentException if any of the values in the
         *         {@code components} array or {@code alpha} is
         *         outside of the range 0.0 to 1.0
         * @see #getComponents
         * @see #getColorComponents
         * @return the {@code Color} corresponding to the specified components and alpha.
         */
        public static Color of( ColorSpace cspace, float[] components, float alpha ) {
            return new Color(cspace, components, alpha);
        }

        /**
         * Creates an sRGB color with the specified RGB values in the range {@code 0-255},
         * and a given opacity.
         *
         * @param red the red component, in the range {@code 0-255}
         * @param green the green component, in the range {@code 0-255}
         * @param blue the blue component, in the range {@code 0-255}
         * @param opacity the opacity component, in the range {@code 0.0-1.0}
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         */
        public static Color ofRgb( int red, int green, int blue, double opacity ) {
            _checkRGB(red, green, blue);
            return Color.of(
                    red / 255.0,
                    green / 255.0,
                    blue / 255.0,
                    opacity);
        }

        /**
         * This is a shortcut for {@code rgb(gray, gray, gray)}.
         * @param gray the gray component, in the range {@code 0-255}
         * @return the {@code Color}
         */
        public static Color ofGrayRgb( int gray ) {
            return ofRgb(gray, gray, gray);
        }

        /**
         * This is a shortcut for {@code rgb(gray, gray, gray, opacity)}.
         * @param gray the gray component, in the range {@code 0-255}
         * @param opacity the opacity component, in the range {@code 0.0-1.0}
         * @return the {@code Color}
         */
        public static Color ofGrayRgb( int gray, double opacity ) {
            return ofRgb(gray, gray, gray, opacity);
        }

        /**
         * Creates a grey color.
         * @param gray color on gray scale in the range
         *             {@code 0.0} (black) - {@code 1.0} (white).
         * @param opacity the opacity component, in the range {@code 0.0-1.0}
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         */
        public static Color ofGray( double gray, double opacity ) {
            return Color.of(gray, gray, gray, opacity);
        }

        /**
         * Creates an opaque grey color.
         * @param gray color on gray scale in the range
         *             {@code 0.0} (black) - {@code 1.0} (white).
         * @return the {@code Color}
         * @throws IllegalArgumentException if any value is out of range
         */
        public static Color ofGray( double gray ) {
            return ofGray(gray, 1.0);
        }

        /**
         * Creates a {@code Color} based on the specified values in the HSB color model,
         * and a given opacity.
         *
         * @param hue the hue, in degrees
         * @param saturation the saturation, {@code 0.0 to 1.0}
         * @param brightness the brightness, {@code 0.0 to 1.0}
         * @param opacity the opacity, {@code 0.0 to 1.0}
         * @return the {@code Color}
         * @throws IllegalArgumentException if {@code saturation}, {@code brightness} or
         *         {@code opacity} are out of range
         */
        public static Color ofHsb(double hue, double saturation, double brightness, double opacity) {
            _checkSB(saturation, brightness);
            double[] rgb = ColorUtility.HSBtoRGB(hue, saturation, brightness);
            return Color.of(rgb[0], rgb[1], rgb[2], opacity);
        }

        public static Color of( String colorString ) {
            try {
                return ColorUtility.parseColor(colorString);
            } catch ( Exception e ) {
                log.error(SwingTree.get().logMarker(), "Could not parse color '" + colorString + "'.", e);
                return Color.UNDEFINED;
            }
        }

        private static void _checkRGB( int red, int green, int blue ) {
            if (red < 0 || red > 255) {
                throw new IllegalArgumentException("Color.rgb's red parameter (" + red + ") expects color values 0-255");
            }
            if (green < 0 || green > 255) {
                throw new IllegalArgumentException("Color.rgb's green parameter (" + green + ") expects color values 0-255");
            }
            if (blue < 0 || blue > 255) {
                throw new IllegalArgumentException("Color.rgb's blue parameter (" + blue + ") expects color values 0-255");
            }
        }

        private static void _checkSB( double saturation, double brightness ) {
            if (saturation < 0.0 || saturation > 1.0) {
                throw new IllegalArgumentException("Color.hsb's saturation parameter (" + saturation + ") expects values 0.0-1.0");
            }
            if (brightness < 0.0 || brightness > 1.0) {
                throw new IllegalArgumentException("Color.hsb's brightness parameter (" + brightness + ") expects values 0.0-1.0");
            }
        }

        private Color(java.awt.Color color) {
            super(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }

        private Color(int r, int g, int b) {
            super(r, g, b);
        }

        private Color(int r, int g, int b, int a) {
            super(r, g, b, a);
        }

        private Color(int rgb) {
            super(rgb);
        }

        private Color(int rgba, boolean hasalpha) {
            super(rgba, hasalpha);
        }

        private Color(float r, float g, float b) {
            super(r, g, b);
        }

        private Color(float r, float g, float b, float a) {
            super(r, g, b, a);
        }

        private Color(ColorSpace cspace, float[] components, float alpha) {
            super(cspace, components, alpha);
        }

        /**
         * The red component of the {@code Color}, in the range {@code 0.0-1.0}.
         * If you want to get the red component in the range {@code 0-255}, use the
         * {@link #getRed()} method.
         *
         * @return the red component of the {@code Color}, in the range {@code 0.0-1.0}
         * @see #getRed()
         */
        public double red() {
            return getRed() / 255.0;
        }

        /**
         * The green component of the {@code Color}, in the range {@code 0.0-1.0}.
         * If you want to get the green component in the range {@code 0-255}, use the
         * {@link #getGreen()} method.
         *
         * @return the green component of the {@code Color}, in the range {@code 0.0-1.0}
         * @see #getGreen()
         */
        public double green() {
            return getGreen() / 255.0;
        }

        /**
         * The blue component of the {@code Color}, in the range {@code 0.0-1.0}.
         * If you want to get the blue component in the range {@code 0-255}, use the
         * {@link #getBlue()} method.
         *
         * @return the blue component of the {@code Color}, in the range {@code 0.0-1.0}
         * @see #getBlue()
         */
        public double blue() {
            return getBlue() / 255.0;
        }

        /**
         * The opacity of the {@code Color}, in the range {@code 0.0-1.0}.
         * If you want to get the opacity in the form of the alpha component in the
         * range {@code 0-255}, use the {@link #getAlpha()} method.
         *
         * @return the opacity of the {@code Color}, in the range {@code 0.0-1.0}
         * @see #getAlpha()
         */
        public double opacity() {
            return getAlpha() / 255.0;
        }

        /**
         * Gets the hue component of this {@code Color}.
         * @return Hue value in the range in the range {@code 0.0-360.0}.
         */
        public double hue() {
            return ColorUtility.RGBtoHSB(red(), green(), blue())[0];
        }

        /**
         * Gets the saturation component of this {@code Color}.
         * @return Saturation value in the range in the range {@code 0.0-1.0}.
         */
        public double saturation() {
            return ColorUtility.RGBtoHSB(red(), green(), blue())[1];
        }

        /**
         * Gets the brightness component of this {@code Color}.
         * @return Brightness value in the range in the range {@code 0.0-1.0}.
         */
        public double brightness() {
            return ColorUtility.RGBtoHSB(red(), green(), blue())[2];
        }

        /**
         * Creates a new {@code Color} based on this {@code Color} with hue,
         * saturation, brightness and opacity values altered. Hue is shifted
         * about the given value and normalized into its natural range, the
         * other components' values are multiplied by the given factors and
         * clipped into their ranges.
         * <p>
         * Increasing brightness of black color is allowed by using an arbitrary,
         * very small source brightness instead of zero.
         * @param hueShift the hue shift
         * @param saturationFactor the saturation factor
         * @param brightnessFactor the brightness factor
         * @param opacityFactor the opacity factor
         * @return a {@code Color} based based on this {@code Color} with hue,
         * saturation, brightness and opacity values altered.
         */
        private Color _deriveColor(
            double hueShift,
            double saturationFactor,
            double brightnessFactor,
            double opacityFactor
        ) {
            return ColorUtility.deriveColor(
                    hueShift, saturationFactor, brightnessFactor, opacityFactor, red(), green(), blue(), opacity()
                );
        }

        /**
         * Creates a new color that is a brighter version of this color.
         * @return A color that is a brighter version of this color.
         */
        @Override
        public Color brighter() {
            return _deriveColor(0, 1.0, 1.0 / DARKER_BRIGHTER_FACTOR, 1.0);
        }

        /**
         * Creates an updated color whose brightness is increased by the specified factor.
         * @param percentage The factor by which to increase the brightness.
         * @return A color that is a brighter version of this color or the same color if the factor is 0.0.
         */
        public Color brighterBy(double percentage) {
            if ( percentage == 0.0 )
                return this;

            double factor = 1 - percentage;
            return _deriveColor(0, 1.0, 1.0 / factor, 1.0);
        }

        /**
         * Creates a new color that is a darker version of this color.
         * @return a color that is a darker version of this color
         */
        @Override
        public Color darker() {
            return _deriveColor(0, 1.0, DARKER_BRIGHTER_FACTOR, 1.0);
        }

        /**
         * Creates an updated color whose brightness is decreased by the specified
         * percentage factor.
         * @param percentage The factor by which to decrease the brightness.
         * @return A color that is a darker version of this color or the same color if the factor is 0.0.
         */
        public Color darkerBy(double percentage) {
            if ( percentage == 0.0 )
                return this;

            double factor = 1 - percentage;
            return _deriveColor(0, 1.0, factor, 1.0);
        }

        /**
         *  Provides an updated color that is a more saturated version of this color.
         *  The color will be 30% more saturated than the original color.
         *
         * @return A color that is a more saturated version of this color.
         */
        public Color saturate() {
            return _deriveColor(0, 1.0 / SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
        }

        /**
         *  Creates a color which is saturated by the specified percentage factor.
         *  So the value {@code 0.0} will return the same color and the value
         *  and the value {@code 1.0} will return a fully saturated color.
         *
         * @param percentage The percentage factor by which to increase the saturation.
         * @return A color that is saturated by the specified percentage factor or the same color if the factor is 0.0.
         */
        public Color saturateBy(double percentage) {
            if ( percentage == 0.0 )
                return this;

            double factor = 1 - percentage;
            return _deriveColor(0, 1.0 / factor, 1.0, 1.0);
        }

        /**
         * Creates a new color that is a less saturated version of this color.
         * The color will be 30% less saturated than the original color.
         *
         * @return A color that is a less saturated version of this color.
         */
        public Color desaturate() {
            return _deriveColor(0, SATURATE_DESATURATE_FACTOR, 1.0, 1.0);
        }

        /**
         *  Creates a color which is desaturated by the specified percentage factor.
         *  So the value {@code 0.0} will return the same color and the value
         *  and the value {@code 1.0} will return a fully desaturated color.
         *
         * @param percentage The percentage factor by which to decrease the saturation.
         * @return A color that is desaturated by the specified percentage factor or the same color if the factor is 0.0.
         */
        public Color desaturateBy(double percentage) {
            if ( percentage == 0.0 )
                return this;

            double factor = 1 - percentage;
            return _deriveColor(0, factor, 1.0, 1.0);
        }

        /**
         * Creates an updated color that is grayscale equivalent of this color.
         * Opacity is preserved.
         * @return A color that is grayscale equivalent of this color
         */
        public Color grayscale() {
            double gray = 0.2126 * red() + 0.7152 * green() + 0.0722 * blue();
            return Color.of(gray, gray, gray, opacity());
        }

        /**
         * Creates a new color that is inversion of this color.
         * Opacity is preserved.
         * @return A color that is inversion of this color.
         */
        public Color invert() {
            return Color.of(1.0 - red(), 1.0 - green(), 1.0 - blue(), opacity());
        }

        /**
         *  Returns an updated version of this color with the red component changed
         *  to the specified value in the range {@code 0.0-1.0}.
         *  The number {@code 0.0} represents no red, and {@code 1.0} represents
         *  full red.
         *
         *  @param red The red component, in the range {@code 0.0-1.0}.
         *  @return A new {@code Color} object with the red component changed.
         * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
         * @see #red()
         */
        public Color withRed(double red) {
            return Color.of(red, green(), blue(), opacity());
        }

        /**
         *  Returns an updated version of this color with the green component changed
         *  to the specified value in the range {@code 0.0-1.0}.
         *  A number of {@code 0.0} represents no green, and {@code 1.0} represents
         *  green to the maximum extent.
         *
         *  @param green The green component, in the range {@code 0.0-1.0}
         *  @return A new {@code Color} object with the green component changed
         * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
         * @see #green()
         */
        public Color withGreen(double green) {
            return Color.of(red(), green, blue(), opacity());
        }

        /**
         *  Returns an updated version of this color with the blue component changed
         *  to the specified value in the range {@code 0.0-1.0}.
         *  A value closer to {@code 0.0} represents no blue, and closer to {@code 1.0}
         *  represents blue to the maximum extent possible.
         *
         *  @param blue The blue component, in the range {@code 0.0-1.0}
         *  @return A new {@code Color} object with the blue component changed
         * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
         * @see #blue()
         */
        public Color withBlue(double blue) {
            return Color.of(red(), green(), blue, opacity());
        }

        /**
         *  Returns an updated version of this color with the opacity changed
         *  to the specified value in the range {@code 0.0-1.0}.
         *  A value closer to {@code 0.0} represents a fully transparent color,
         *  and closer to {@code 1.0} represents a fully opaque color.
         *
         *  @param opacity The opacity component, in the range {@code 0.0-1.0}
         *  @return A new {@code Color} object with the opacity changed
         * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
         * @see #opacity()
         */
        public Color withOpacity(double opacity) {
            return Color.of(red(), green(), blue(), opacity);
        }

        /**
         *  Creates and returns an updated version of this color with the
         *  alpha component changed to the specified value in the range {@code 0-255}.
         *  A value closer to {@code 0} represents a fully transparent color,
         *  and closer to {@code 255} represents a fully opaque color.
         *
         * @param alpha The alpha component, in the range {@code 0-255}.
         * @return A new {@code Color} object with the alpha component changed
         */
        public Color withAlpha(int alpha ) {
            return new Color(getRed(), getGreen(), getBlue(), alpha);
        }

        /**
         *  Returns an updated version of this color with the hue changed
         *  to the specified value in the range {@code 0.0-360.0}.
         *  A value closer to {@code 0.0} represents red, and closer to {@code 360.0}
         *  represents red again.
         *
         *  @param hue The hue component, in the range {@code 0.0-360.0}
         *  @return A new {@code Color} object with the hue changed
         * @throws IllegalArgumentException If the value is out of range (0.0-360.0)
         * @see #hue()
         */
        public Color withHue(double hue) {
            return Color.ofHsb(hue, saturation(), brightness(), opacity());
        }

        /**
         *  Returns an updated version of this color with the saturation changed
         *  to the specified value in the range {@code 0.0-1.0}.
         *  A value closer to {@code 0.0} represents a shade of grey, and closer to
         *  {@code 1.0} represents a fully saturated color.
         *
         *  @param saturation The saturation component, in the range {@code 0.0-1.0}
         *  @return A new {@code Color} object with the saturation changed
         * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
         * @see #saturation()
         */
        public Color withSaturation(double saturation) {
            return Color.ofHsb(hue(), saturation, brightness(), opacity());
        }

        /**
         *  Returns an updated version of this color with the brightness changed
         *  to the specified value in the range {@code 0.0-1.0}.
         *  A value closer to {@code 0.0} represents black, and closer to {@code 1.0}
         *  represents white.
         *
         *  @param brightness The brightness component, in the range {@code 0.0-1.0}
         *  @return A new {@code Color} object with the brightness changed
         * @throws IllegalArgumentException If the value is out of range (0.0-1.0)
         * @see #brightness()
         */
        public Color withBrightness( double brightness ) {
            return Color.ofHsb(hue(), saturation(), brightness, opacity());
        }
    }

    /**
     *  An immutable and value based font configuration object, which is used to configure component fonts
     *  in a UI declaration. SwingTree can dynamically generate {@link java.awt.Font}s for
     *  components using the {@link Font#toAwtFont()} method. This may be done reactively, like for
     *  example when the look and feel changes its UI scale and SwingTree scales and re-installs your fonts.<br>
     *  This is a wrapper around a {@link FontConf} object, which is also used in the {@link UIForAnySwing#withStyle(Styler)}
     *  API, and it holds all font properties needed to create {@link java.awt.Font} instances for Swing components.
     *  The appearance of a font is primarily based on the font family name which is used to find a font on the system,
     *  but there is a variety of other properties which you can configure using {@link #with(Configurator)}.<br>
     *  <b>
     *      We recommend using this class instead of classical AWT Font instances in your code,
     *      as it allows SwingTree to scale your UI more reliably.
     *  </b><br>
     * @see UIForAnySwing#withFont(Font) For the most common usecase of this class.
     * @see UIForAnySwing#withFont(Val) To configure a reactive font for a component.
     * @see UIForAnySwing#withStyle(Styler) and more specifically {@link ComponentStyleDelegate#font(UI.Font)}
     *      to configure the font of a component through the SwingTree style API.
     */
    public static final class Font
    {
        /**
         *  This constant is a {@link java.awt.Font} object with a font name of "" (empty string),
         *  a font style of -1 (undefined) and a font size of 0.
         *  Its identity is used to represent the absence of a font being specified,
         *  and it is used as a safe replacement for null,
         *  meaning that when the style engine of a component encounters it, it will pass it onto
         *  the {@link java.awt.Component#setFont(java.awt.Font)} method as null.
         *  Passing null to this method means that the look and feel determines the font.
         */
        public static final java.awt.Font UNDEFINED = new java.awt.Font("", -1, 0);

        private final FontConf conf;

        /**
         *  A factory method that creates a new {@code Font} object with the specified font name
         *  {@link FontStyle} and size.
         *  This maps directly to the constructor of {@link java.awt.Font#Font(String, int, int)}.
         * @param name The font name, which may be anything depending on what fonts are loaded on the system.
         * @param style The style of the font, which is one of the constants {@link FontStyle#PLAIN},
         * @param size The point size of the font.
         * @return A new {@code Font} object with the specified font name, style and size.
         */
        public static Font of( String name, FontStyle style, int size ) {
            return new Font(name, style.toAWTFontStyle(), size);
        }

        /**
         *  A factory method that creates a new {@code Font} object with the specified font name
         *  and size where the {@link FontStyle} defaults to {@code PLAIN}.
         *  This maps directly to the constructor of {@link java.awt.Font#Font(String, int, int)}.
         * @param name The font name, which may be anything depending on what fonts are loaded on the system.
         * @param size The point size of the font.
         * @return A new {@code Font} object with the specified font name, style and size.
         */
        public static Font of( String name, int size ) {
            return new Font(name, FontStyle.PLAIN.toAWTFontStyle(), size);
        }

        /**
         *  Creates a new {@code Font} object from a map of attributes
         *  where the key is an attribute and the value is the value of the attribute.
         *  See {@link java.awt.font.TextAttribute} for a list of common attributes.
         *  These attributes define the style of the font.
         * @param attributes A map of attributes that define the style of the font.
         * @return A new {@code Font} object with the specified attributes.
         */
        public static Font of( Map<? extends AttributedCharacterIterator.Attribute, ?> attributes ) {
            return new Font(attributes);
        }

        /**
         *  Creates a new {@link swingtree.UI.Font} object from a {@link java.awt.Font} object.
         * @param font The font to convert to a font.
         * @return The SwingTree native font object.
         */
        public static Font of( java.awt.Font font ) {
            return new Font(font);
        }

        private Font(String name, int style, int size) {
            this(new java.awt.Font(name, style, size));
        }

        private Font(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
            this(new java.awt.Font(attributes));
        }

        private Font(java.awt.Font font) {
            this(conf->conf.withPropertiesFromFont(font));
        }

        private Font(Configurator<FontConf> configurator) {
            this(Result.ofTry(FontConf.class, ()->configurator.configure(FontConf.none()))
                                    .logProblemsAsError()
                                    .orElse(FontConf.none()));
        }

        private Font(FontConf conf) {
            this.conf = conf;
        }

        /**
         *  Converts this font to a {@link java.awt.Font} object
         *  that can be used in Swing components.
         *  Note that this method creates a new {@link java.awt.Font} object
         *  which will always be scaled according to the current DPI settings
         *  of SwingTree, which you can access using {@link UI#scale()}.
         *
         *  @return The {@link java.awt.Font} object representing this font.
         */
        public java.awt.Font toAwtFont() {
            return SwingTree.get().scale(this.conf.toAwtFont());
        }

        /**
         *  Returns the configuration of this font which
         *  encapsulates all properties of the font and can
         *  be used to convert to a {@link java.awt.Font} object
         *  using the {@link FontConf#toAwtFont()} method.
         *
         *  @return The configuration of this font.
         */
        public FontConf conf() {
            return this.conf;
        }

        /**
         *  Allows configuring this font using a configurator function
         *  to a new {@code Font} object with the desired changes.
         *  @param configurator The configurator function that applies changes to the font configuration.
         *                      It receives a {@link FontConf} object representing the current configuration
         *                      of the font and should return the modified {@code FontConf} object.
         * @return A new {@code Font} object with the applied changes.
         * @throws NullPointerException If the configurator is null.
         */
        public Font with( Configurator<FontConf> configurator ) {
            Objects.requireNonNull(configurator, "The font configurator cannot be null.");
            return new Font(Result.ofTry(FontConf.class, ()-> configurator.configure(this.conf))
                    .logProblemsAsError()
                    .orElse(this.conf));
        }

        /**
         *  Returns an updated version of this font with the font (family) name changed to the specified value.
         *  @param name The font name, which may be anything depending on what fonts are loaded on the system.
         *  @return A new {@code Font} object with the font name changed.
         *  @throws NullPointerException If the font name is null.
         */
        public Font withName( String name ) {
            Objects.requireNonNull(name, "The font name cannot be null.");
            return with(conf->conf.family(name));
        }

        /**
         *  Returns an updated version of this font with the font style changed to the specified value.
         *  @param style The style of the font, which is one of the constants {@link FontStyle#PLAIN},
         *  {@link FontStyle#BOLD} or {@link FontStyle#ITALIC}.
         *  @return A new {@code Font} object with the font style changed.
         */
        public Font withStyle( FontStyle style ) {
            return with(conf->conf.style(style));
        }

        /**
         *  Returns an updated version of this font with the font size changed to the specified value.
         *  @param size The point size of the font.
         *  @return A new {@code Font} object with the font size changed.
         */
        public Font withSize( int size ) {
            return with(conf->conf.size(size));
        }

        @Override
        public int hashCode() {
            return this.conf.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Font && this.conf.equals(((Font)obj).conf);
        }

        @Override
        public String toString() {
            String confString = this.conf.toString();
            return "UI.Font[" + confString.substring(9, confString.length() - 1) + "]";
        }

    }

}
