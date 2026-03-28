package swingtree;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import swingtree.api.Configurator;
import swingtree.layout.*;

/**
 *  Essentially just a namespace for static layout constants for
 *  the {@link net.miginfocom.swing.MigLayout} {@link java.awt.LayoutManager} type. <br>
 *  This class is not intended to be instantiated! <br>
 *  <br>
 *  The constants as well as static factory methods in this class
 *  are intended to be used like this:
 *  <pre>{@code
 * import static swingtree.UI.*;
 * //...
 * panel(FILL.and(WRAP(2)))
 * .withPrefSize(500, 300)
 * .add(GROW,
 *   panel(FILL_X.and(WRAP(2)),"[shrink][grow]")
 *   .add(label("Username"))
 *   .add(GROW_X,
 *     textField(vm.username())
 *   )
 *   .add(SHRINK_X, label("Password"))
 *   .add(GROW_X,
 *     passwordField(vm.password())
 *   )
 * )
 * .add(GROW,
 *   panel(FILL_X.and(WRAP(2)),"[shrink][grow]")
 *   .add(label("Email"))
 *   .add(GROW_X,
 *     textField(vm.email())
 *   )
 *   .add(SHRINK_X, label("Gender"))
 *   .add(GROW_X,
 *     comboBox(vm.gender())
 *   )
 * )
 * .add(GROW_X,
 *   panel(FILL_X.and(WRAP(1)))
 *   .add(GROW_X,
 *     checkBox("I accept!", vm.termsAccepted())
 *   )
 *   .add(GROW_X,
 *     button("Register")
 *     .onClick( it -> vm.register() )
 *   )
 * )
 * .add(GROW_X,
 *   panel(FILL_X.and(WRAP(1)))
 *   .withBorderTitled("Feedback")
 *   .add(GROW_X,
 *     boldLabel(
 *       vm.feedback()
 *     )
 *     .withForeground(vm.feedbackColor())
 *   )
 * )
 * .add(GROW_X.and(SPAN), button("RESET").onClick( it -> vm.reset() ));
 * }</pre>
 *
 *  In this little example form we can see how the constants are used to
 *  create a form with a grid layout. <br>
 *  The {@link #FILL} constant is used to make the panels fill the entire
 *  width of the parent panel. <br>
 *  The {@link #WRAP(int)} constant is used to make the panels wrap after
 *  n components have been added to them. <br>
 *  The {@link #GROW} constant is used to make the panels grow vertically
 *  to fill the entire height of the parent panel. <br>
 *  ... and so on. <br>
 */
public abstract class UILayoutConstants
{
    UILayoutConstants() { throw new UnsupportedOperationException(); }

    // Common Mig layout constants:

    /**
     *  Makes the layout claim all available space in the container
     *  for <b>both columns and rows</b>.
     *  Think of it as the layout stretching out like a rubber sheet
     *  to cover the entire container — but individual components
     *  will only actually expand if they themselves also have a {@link #GROW}
     *  constraint applied to them.
     *  <p>
     *  This is a <b>layout-level</b> constraint, meaning it is passed
     *  to the panel factory method (e.g. {@code UI.panel(FILL)})
     *  rather than to {@code .add(..)} calls.
     */
    public static LayoutConstraint FILL   = LayoutConstraint.of("fill");

    /**
     *  Makes the layout claim all available <b>horizontal</b> space
     *  in the container for its columns.
     *  Imagine the columns stretching sideways to fill the container's width,
     *  while the rows remain only as tall as their content needs.
     *  Components still need their own {@link #GROW_X} constraint
     *  to actually widen along with the column.
     *  <p>
     *  This is a <b>layout-level</b> constraint, meaning it is passed
     *  to the panel factory method (e.g. {@code UI.panel(FILL_X)})
     *  rather than to {@code .add(..)} calls.
     */
    public static LayoutConstraint FILL_X = LayoutConstraint.of("fillx");

    /**
     *  Makes the layout claim all available <b>vertical</b> space
     *  in the container for its rows.
     *  Imagine the rows stretching downward to fill the container's height,
     *  while the columns remain only as wide as their content needs.
     *  Components still need their own {@link #GROW_Y} constraint
     *  to actually grow taller along with the row.
     *  <p>
     *  This is a <b>layout-level</b> constraint, meaning it is passed
     *  to the panel factory method (e.g. {@code UI.panel(FILL_Y)})
     *  rather than to {@code .add(..)} calls.
     */
    public static LayoutConstraint FILL_Y = LayoutConstraint.of("filly");
    /**
     *  Shorthand for {@link #INSETS(int)}.
     *  Sets the same padding on all four sides of the container.
     *
     * @param insets The padding size in pixels, applied equally to top, left, bottom, and right.
     * @return A layout constraint that adds uniform inner padding around the container's content.
     */
    public static LayoutConstraint INS( int insets) { return INSETS(insets); }

    /**
     *  Sets <b>uniform inner padding</b> around the entire container content.
     *  Picture a picture frame: the insets are the space between
     *  the frame edge and the picture itself. All four sides get the same padding.
     *  <p>
     *  This replaces the gap before/after the first/last column and row
     *  with the specified value, similar to setting an {@code EmptyBorder}
     *  but without removing any border that is already on the container.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     *
     * @param insets The padding size in pixels, applied equally to top, left, bottom, and right.
     * @return A layout constraint that adds uniform inner padding around the container's content.
     */
    public static LayoutConstraint INSETS( int insets) { return LayoutConstraint.of("insets " + insets); }

    /**
     *  Shorthand for {@link #INSETS(int, int, int, int)}.
     *  Sets individual padding for each side of the container.
     *
     * @param top    Padding above the content, in pixels.
     * @param left   Padding to the left of the content, in pixels.
     * @param bottom Padding below the content, in pixels.
     * @param right  Padding to the right of the content, in pixels.
     * @return A layout constraint that adds the specified inner padding around the container's content.
     */
    public static LayoutConstraint INS( int top, int left, int bottom, int right) { return INSETS(top, left, bottom, right); }

    /**
     *  Sets <b>individual inner padding</b> for each side of the container.
     *  Like a picture frame with different border widths —
     *  you can have more space at the top than the bottom, for example.
     *  <p>
     *  This replaces the gap before/after the first/last column and row
     *  with the specified values, similar to setting an {@code EmptyBorder}
     *  but without removing any border that is already on the container.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     *
     * @param top    Padding above the content, in pixels.
     * @param left   Padding to the left of the content, in pixels.
     * @param bottom Padding below the content, in pixels.
     * @param right  Padding to the right of the content, in pixels.
     * @return A layout constraint that adds the specified inner padding around the container's content.
     */
    public static LayoutConstraint INSETS( int top, int left, int bottom, int right) { return LayoutConstraint.of("insets " + top + " " + left + " " + bottom + " " + right); }
    /**
     *  Turns on <b>auto-wrapping</b> so that the layout grid moves to a new row
     *  after every {@code times} components.
     *  Think of it like a typewriter carriage return: after placing {@code times}
     *  components side by side, the next component drops down to a fresh row.
     *  <p>
     *  Without this constraint, all components flow into a single row
     *  unless individual components carry a {@link #WRAP} add-constraint.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     *
     * @param times The number of components per row before wrapping to the next row.
     * @return A layout constraint that enables automatic row wrapping.
     */
    public static LayoutConstraint WRAP( int times) { return LayoutConstraint.of( "wrap " + times ); }

    /**
     *  Sets the <b>default gap</b> between grid cells to a platform-relative size.
     *  The {@code size} value is interpreted as a relative unit, meaning
     *  the actual pixel spacing adapts to the platform's look-and-feel conventions.
     *  Use this when you want spacing that "feels right" on any operating system
     *  rather than a fixed pixel count.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     *
     * @param size The relative gap size.
     * @return A layout constraint that sets platform-relative spacing between cells.
     */
    public static LayoutConstraint GAP_REL( int size) { return LayoutConstraint.of( "gap rel " + size ); }

    /**
     *  Sets the layout's flow direction to <b>horizontal</b> (left to right).
     *  This is the default, so you typically only need this to explicitly
     *  override a parent's vertical flow or to make your intention clear in code.
     *  Components are placed side by side, filling columns first.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     */
    public static LayoutConstraint FLOW_X   = LayoutConstraint.of("flowx");

    /**
     *  Sets the layout's flow direction to <b>vertical</b> (top to bottom).
     *  Instead of placing components side by side in a row,
     *  each new component is placed <b>below</b> the previous one,
     *  filling rows first — like a vertical stack.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     */
    public static LayoutConstraint FLOW_Y   = LayoutConstraint.of("flowy");

    /**
     *  Disables the grid entirely and puts the layout into <b>flow-only mode</b>.
     *  All components are placed in a single row (or column, if vertical flow),
     *  one after another, without any column/row alignment.
     *  Think of it like a simple {@link java.awt.FlowLayout} —
     *  components just stream in sequence without snapping to a grid.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     */
    public static LayoutConstraint NO_GRID  = LayoutConstraint.of("nogrid");

    /**
     *  Tells the layout engine to <b>skip its internal caching</b>.
     *  Normally the layout caches calculated sizes for performance,
     *  but if you use percentage-based sizing ({@code "%"}) or experience
     *  revalidation glitches, disabling the cache forces a fresh
     *  calculation on every layout pass.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     *  Only use it when you actually observe layout anomalies.
     */
    public static LayoutConstraint NO_CACHE = LayoutConstraint.of("nocache");

    /**
     *  Turns on <b>visual debug painting</b> for the container.
     *  When active, the layout draws coloured outlines around every grid cell
     *  and component boundary, making it easy to see why things
     *  ended up where they did. The overlay repaints once per second.
     *  <p>
     *  This is a <b>layout-level</b> constraint.
     *  Remove it before shipping — it is purely a development aid.
     */
    public static LayoutConstraint DEBUG    = LayoutConstraint.of("debug");

    /**
     *  A <b>component-level</b> constraint that tells the layout to
     *  <b>break to a new row after this component</b>.
     *  It is the per-component equivalent of pressing "Enter" —
     *  whatever comes next starts on a fresh row beneath this one.
     *  <p>
     *  Use this when you don't want global auto-wrapping via {@link #WRAP(int)}
     *  but need a line break at a specific point.
     */
    public static MigAddConstraint WRAP = MigAddConstraint.of("wrap");

    /**
     *  Makes this component <b>span across all remaining columns</b> in the current row.
     *  The component stretches from its current cell all the way to the right edge
     *  of the grid, merging every cell it crosses into one wide cell.
     *  <p>
     *  This is a <b>component-level</b> constraint, passed to {@code .add(SPAN, ...)}.
     */
    public static MigAddConstraint SPAN = MigAddConstraint.of("SPAN");

    /**
     *  Makes this component <b>span across {@code times} columns</b> in the current row.
     *  The component merges that many consecutive cells into one wider cell,
     *  giving it more horizontal room.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param times The number of columns to merge into a single cell.
     * @return A constraint that widens this component's cell by the given column count.
     */
    public static MigAddConstraint SPAN(int times ) { return MigAddConstraint.of( "span " + times ); }

    /**
     *  Makes this component <b>span across {@code xTimes} columns and {@code yTimes} rows</b>,
     *  merging a rectangular block of grid cells into one large cell.
     *  Useful for components that need to occupy a multi-cell area,
     *  like a large text area in a form.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param xTimes The number of columns to span.
     * @param yTimes The number of rows to span.
     * @return A constraint that merges columns and rows into a single cell for this component.
     */
    public static MigAddConstraint SPAN(int xTimes, int yTimes ) { return MigAddConstraint.of( "span " + xTimes + " " + yTimes ); }

    /**
     *  Makes this component <b>span across {@code times} columns</b> only
     *  (horizontal direction), without affecting the row span.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param times The number of columns to merge.
     * @return A constraint that widens the cell horizontally by the given column count.
     */
    public static MigAddConstraint SPAN_X(int times ) { return MigAddConstraint.of( "spanx " + times ); }

    /**
     *  Makes this component <b>span across {@code times} rows</b> only
     *  (vertical direction), without affecting the column span.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param times The number of rows to merge.
     * @return A constraint that extends the cell vertically by the given row count.
     */
    public static MigAddConstraint SPAN_Y(int times ) { return MigAddConstraint.of( "spany " + times ); }
    /**
     *  Tells this component to <b>eagerly expand in both directions</b>
     *  to fill any extra space in its cell.
     *  Without this, a component just sits at its preferred size
     *  even if the cell around it is larger.
     *  With {@code GROW}, it stretches like a balloon to use all available room.
     *  <p>
     *  Works hand in hand with layout-level {@link #FILL} or {@link #FILL_X}/{@link #FILL_Y},
     *  which make the cells themselves expand to fill the container.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint GROW   = MigAddConstraint.of("grow");

    /**
     *  Tells this component to <b>eagerly expand horizontally</b>
     *  to fill any extra width in its cell.
     *  The component stretches sideways but keeps its natural height.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint GROW_X = MigAddConstraint.of("growx");

    /**
     *  Tells this component to <b>eagerly expand vertically</b>
     *  to fill any extra height in its cell.
     *  The component stretches downward but keeps its natural width.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint GROW_Y = MigAddConstraint.of("growy");

    /**
     *  Tells this component to grow in both directions with a specific <b>weight</b>.
     *  The weight is a relative number: a component with weight 200 gets
     *  twice as much extra space as one with weight 100.
     *  Use this when multiple growing components compete for the same space
     *  and you want to control how the surplus is divided.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative grow weight (default is 100 when using {@link #GROW}).
     * @return A constraint that makes the component grow with the specified weight.
     */
    public static MigAddConstraint GROW(int weight ) { return MigAddConstraint.of( "grow " + weight ); }

    /**
     *  Tells this component to <b>grow horizontally</b> with a specific weight.
     *  A higher weight means this component claims a larger share
     *  of the available extra width compared to other growing components.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative horizontal grow weight.
     * @return A constraint that makes the component grow horizontally with the specified weight.
     */
    public static MigAddConstraint GROW_X(int weight ) { return MigAddConstraint.of( "growx " + weight ); }

    /**
     *  Tells this component to <b>grow vertically</b> with a specific weight.
     *  A higher weight means this component claims a larger share
     *  of the available extra height compared to other growing components.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative vertical grow weight.
     * @return A constraint that makes the component grow vertically with the specified weight.
     */
    public static MigAddConstraint GROW_Y(int weight ) { return MigAddConstraint.of( "growy " + weight ); }
    /**
     *  Allows this component to <b>shrink in both directions</b>
     *  when the container is too small to fit everything at preferred size.
     *  By default, all components already have a shrink weight of 100,
     *  so they can shrink down to their minimum size.
     *  Use this constant explicitly when combining it with other constraints
     *  or to make your intent clear.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint SHRINK   = MigAddConstraint.of("shrink");

    /**
     *  Allows this component to <b>shrink horizontally</b>
     *  when the container is too narrow to fit everything at preferred width.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint SHRINK_X = MigAddConstraint.of("shrinkx");

    /**
     *  Allows this component to <b>shrink vertically</b>
     *  when the container is too short to fit everything at preferred height.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint SHRINK_Y = MigAddConstraint.of("shrinky");

    /**
     *  Sets how <b>willingly</b> this component shrinks in both directions
     *  relative to its siblings.
     *  A component with weight 200 will give up twice as many pixels
     *  as one with weight 100 when space gets tight.
     *  Think of it as "eagerness to sacrifice space."
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative shrink weight (default is 100).
     * @return A constraint that controls how much this component shrinks relative to others.
     */
    public static MigAddConstraint SHRINK(int weight )  { return MigAddConstraint.of("shrink "+weight); }

    /**
     *  Sets how willingly this component <b>shrinks horizontally</b>
     *  relative to its siblings when the container is too narrow.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative horizontal shrink weight.
     * @return A constraint controlling horizontal shrink eagerness.
     */
    public static MigAddConstraint SHRINK_X(int weight )  { return MigAddConstraint.of("shrinkx "+weight); }

    /**
     *  Sets how willingly this component <b>shrinks vertically</b>
     *  relative to its siblings when the container is too short.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative vertical shrink weight.
     * @return A constraint controlling vertical shrink eagerness.
     */
    public static MigAddConstraint SHRINK_Y(int weight )  { return MigAddConstraint.of("shrinky "+weight); }

    /**
     *  Sets the <b>shrink priority</b> for this component.
     *  When space is scarce, components with <b>higher</b> priority values
     *  are shrunk to their minimum size <b>first</b>, before any lower-priority
     *  component is touched. The default priority is 100.
     *  <p>
     *  Use this to protect important components from shrinking
     *  by giving them a low priority, while expendable components
     *  get a high priority and absorb the squeeze first.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param priority The shrink priority (higher = shrinks sooner).
     * @return A constraint that sets the component's shrink priority.
     */
    public static MigAddConstraint SHRINK_PRIO( int priority )  { return MigAddConstraint.of("shrinkprio "+priority); }
    /**
     *  Makes the <b>row and column</b> that this component sits in
     *  <b>greedily absorb</b> all leftover space in the container.
     *  While {@link #GROW} tells the <i>component</i> to fill its cell,
     *  {@code PUSH} tells the <i>cell's row and column</i> to expand
     *  and claim any unclaimed container space.
     *  The effect is that surrounding components get pushed
     *  toward the container edges.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint PUSH     = MigAddConstraint.of("push");

    /**
     *  Makes the <b>column</b> that this component sits in
     *  <b>greedily absorb</b> all leftover horizontal space.
     *  Other columns are pushed toward the left and right edges.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint PUSH_X   = MigAddConstraint.of("pushx");

    /**
     *  Makes the <b>row</b> that this component sits in
     *  <b>greedily absorb</b> all leftover vertical space.
     *  Other rows are pushed toward the top and bottom edges.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint PUSH_Y   = MigAddConstraint.of("pushy");

    /**
     *  Makes the row and column absorb leftover space with a specific <b>weight</b>.
     *  When multiple cells push, the weight determines how the leftover space
     *  is divided between them — higher weight means a bigger share.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative push weight.
     * @return A constraint that makes the component's row and column push with the given weight.
     */
    public static MigAddConstraint PUSH( int weight )  { return MigAddConstraint.of("push "+weight); }

    /**
     *  Makes the column absorb leftover horizontal space with a specific <b>weight</b>.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative horizontal push weight.
     * @return A constraint that makes the column push horizontally with the given weight.
     */
    public static MigAddConstraint PUSH_X( int weight ) { return MigAddConstraint.of("pushx "+weight); }

    /**
     *  Makes the row absorb leftover vertical space with a specific <b>weight</b>.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param weight The relative vertical push weight.
     * @return A constraint that makes the row push vertically with the given weight.
     */
    public static MigAddConstraint PUSH_Y( int weight ) { return MigAddConstraint.of("pushy "+weight); }
    /**
     *  <b>Skips</b> over a number of cells in the grid flow
     *  before placing this component.
     *  It is like leaving blank cells in a spreadsheet —
     *  the component lands {@code cells} positions further along than it normally would.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param cells The number of cells to jump over.
     * @return A constraint that skips the specified number of grid cells.
     */
    public static MigAddConstraint SKIP( int cells ) { return MigAddConstraint.of("skip "+cells); }

    /**
     *  <b>Splits</b> the current cell into {@code cells} sub-slots,
     *  so that the next {@code cells} components all share the same grid cell
     *  and are placed side by side within it (without gaps between them).
     *  Think of it as subdividing a single table cell into smaller compartments.
     *  <p>
     *  Only the <i>first</i> component placed into a cell may declare the split;
     *  subsequent split keywords in the same cell are ignored.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param cells The number of sub-slots to create in this cell.
     * @return A constraint that splits the cell for multiple components.
     */
    public static MigAddConstraint SPLIT( int cells ) { return MigAddConstraint.of("split "+cells); }

    /**
     *  Overrides this component's <b>width</b> with explicit minimum, preferred, and maximum values (in pixels).
     *  This takes precedence over whatever size the component would normally report.
     *  <p>
     *  For example, {@code WIDTH(80, 120, 200)} means the component will be
     *  at least 80 px wide, ideally 120 px, and never wider than 200 px.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param min  The minimum width in pixels.
     * @param pref The preferred width in pixels.
     * @param max  The maximum width in pixels.
     * @return A constraint that overrides the component's width bounds.
     */
    public static MigAddConstraint WIDTH( int min, int pref, int max ) { return MigAddConstraint.of("width "+min+":"+pref+":"+max); }

    /**
     *  Overrides this component's <b>height</b> with explicit minimum, preferred, and maximum values (in pixels).
     *  This takes precedence over whatever size the component would normally report.
     *  <p>
     *  For example, {@code HEIGHT(20, 30, 50)} means the component will be
     *  at least 20 px tall, ideally 30 px, and never taller than 50 px.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param min  The minimum height in pixels.
     * @param pref The preferred height in pixels.
     * @param max  The maximum height in pixels.
     * @return A constraint that overrides the component's height bounds.
     */
    public static MigAddConstraint HEIGHT( int min, int pref, int max ) { return MigAddConstraint.of("height "+min+":"+pref+":"+max); }

    /**
     *  Applies <b>uniform padding</b> around this component in absolute pixels.
     *  Padding nudges the component's painted bounds outward (or inward, if negative)
     *  <b>after</b> the layout has been computed. It does not affect other components'
     *  positions or the cell size — it is purely a last-step visual adjustment.
     *  <p>
     *  For example, {@code PAD(5)} makes the component 10 px wider and 10 px taller
     *  (5 px on each side).
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param size The padding in pixels, applied equally to all four sides.
     * @return A constraint that pads the component's bounds.
     */
    public static MigAddConstraint PAD( int size ) { return PAD(size, size, size, size); }

    /**
     *  Applies <b>individual padding</b> around this component in absolute pixels.
     *  Each side can have a different value. Positive values push the edge outward;
     *  negative values pull it inward.
     *  This adjustment happens <b>after</b> layout, so it does not move
     *  neighbouring components or change cell sizes.
     *  <p>
     *  This is a <b>component-level</b> constraint.
     *
     * @param top    Padding above the component, in pixels.
     * @param left   Padding to the left, in pixels.
     * @param bottom Padding below the component, in pixels.
     * @param right  Padding to the right, in pixels.
     * @return A constraint that pads the component's bounds on each side.
     */
    public static MigAddConstraint PAD( int top, int left, int bottom, int right ) { return MigAddConstraint.of("pad "+top+" "+left+" "+bottom+" "+right); }
    /**
     *  Centres this component <b>both horizontally and vertically</b> within its cell.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_CENTER = MigAddConstraint.of("align center");

    /**
     *  Pushes this component to the <b>left edge</b> of its cell.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_LEFT = MigAddConstraint.of("align left");

    /**
     *  Pushes this component to the <b>right edge</b> of its cell.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_RIGHT = MigAddConstraint.of("align right");

    /**
     *  Centres this component <b>horizontally</b> within its cell,
     *  without affecting its vertical position.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_X_CENTER = MigAddConstraint.of("alignx center");

    /**
     *  Pushes this component to the <b>left edge</b> of its cell
     *  (horizontal axis only).
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_X_LEFT = MigAddConstraint.of("alignx left");

    /**
     *  Pushes this component to the <b>right edge</b> of its cell
     *  (horizontal axis only).
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_X_RIGHT = MigAddConstraint.of("alignx right");

    /**
     *  Centres this component <b>vertically</b> within its cell,
     *  without affecting its horizontal position.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_Y_CENTER = MigAddConstraint.of("aligny center");

    /**
     *  Pushes this component to the <b>bottom edge</b> of its cell
     *  (vertical axis only).
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_Y_BOTTOM = MigAddConstraint.of("aligny bottom");

    /**
     *  Pushes this component to the <b>top edge</b> of its cell
     *  (vertical axis only).
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint ALIGN_Y_TOP = MigAddConstraint.of("aligny top");

    /**
     *  Aligns this component within its cell according to the given {@link UI.Side}.
     *  <p>This is a <b>component-level</b> constraint.
     *
     * @param pos The side/direction to align toward.
     * @return A constraint that aligns the component as specified.
     */
    public static MigAddConstraint ALIGN( UI.Side pos ) { return MigAddConstraint.of(pos.toMigAlign()); }
    /**
     *  Shorthand to align this component at the <b>top</b> of its cell.
     *  Equivalent to {@link #ALIGN_Y_TOP}.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint TOP = MigAddConstraint.of("top");

    /**
     *  Shorthand to align this component at the <b>right</b> of its cell.
     *  Equivalent to {@link #ALIGN_X_RIGHT}.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint RIGHT = MigAddConstraint.of("right");

    /**
     *  Shorthand to align this component at the <b>bottom</b> of its cell.
     *  Equivalent to {@link #ALIGN_Y_BOTTOM}.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint BOTTOM = MigAddConstraint.of("bottom");

    /**
     *  Shorthand to align this component at the <b>left</b> of its cell.
     *  Equivalent to {@link #ALIGN_X_LEFT}.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint LEFT = MigAddConstraint.of("left");

    /**
     *  Shorthand to <b>centre</b> this component in its cell (both axes).
     *  Equivalent to {@link #ALIGN_CENTER}.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint CENTER = MigAddConstraint.of("center");
    /**
     *  Adds a fixed-size gap to the <b>left</b> of this component.
     *  The gap is empty space measured in pixels that separates this component
     *  from whatever is to its left (another component or the cell edge).
     *  <p>This is a <b>component-level</b> constraint.
     *
     * @param size Gap size in pixels.
     * @return A constraint adding a left-side gap.
     */
    public static MigAddConstraint GAP_LEFT(int size) { return MigAddConstraint.of("gapleft "+size); }

    /**
     *  Adds a fixed-size gap to the <b>right</b> of this component.
     *  <p>This is a <b>component-level</b> constraint.
     *
     * @param size Gap size in pixels.
     * @return A constraint adding a right-side gap.
     */
    public static MigAddConstraint GAP_RIGHT(int size) { return MigAddConstraint.of("gapright "+size); }

    /**
     *  Adds a fixed-size gap <b>above</b> this component.
     *  <p>This is a <b>component-level</b> constraint.
     *
     * @param size Gap size in pixels.
     * @return A constraint adding a top-side gap.
     */
    public static MigAddConstraint GAP_TOP(int size) { return MigAddConstraint.of("gaptop "+size); }

    /**
     *  Adds a fixed-size gap <b>below</b> this component.
     *  <p>This is a <b>component-level</b> constraint.
     *
     * @param size Gap size in pixels.
     * @return A constraint adding a bottom-side gap.
     */
    public static MigAddConstraint GAP_BOTTOM(int size) { return MigAddConstraint.of("gapbottom "+size); }

    /**
     *  Adds an <b>expanding (pushing) gap</b> to the <b>left</b> of this component.
     *  A pushing gap is greedy: it absorbs all available leftover space,
     *  effectively shoving this component to the right.
     *  Think of it as an invisible spring pushing from the left.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint GAP_LEFT_PUSH = MigAddConstraint.of("gapleft push");

    /**
     *  Adds an <b>expanding (pushing) gap</b> to the <b>right</b> of this component.
     *  The gap absorbs all available leftover space, shoving this component to the left.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint GAP_RIGHT_PUSH = MigAddConstraint.of("gapright push");

    /**
     *  Adds an <b>expanding (pushing) gap</b> <b>above</b> this component.
     *  The gap absorbs all available leftover space, shoving this component downward.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint GAP_TOP_PUSH = MigAddConstraint.of("gaptop push");

    /**
     *  Adds an <b>expanding (pushing) gap</b> <b>below</b> this component.
     *  The gap absorbs all available leftover space, shoving this component upward.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint GAP_BOTTOM_PUSH = MigAddConstraint.of("gapbottom push");
    /**
     *  <b>Docks</b> this component against the <b>top edge</b> of the container.
     *  The component stretches across the full container width and
     *  "cuts off" a horizontal strip at the top. Remaining components
     *  are laid out in the space below.
     *  Works like {@link java.awt.BorderLayout#NORTH} but supports
     *  multiple docked components stacking in the order they are added.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint DOCK_NORTH = MigAddConstraint.of("dock north");

    /**
     *  <b>Docks</b> this component against the <b>bottom edge</b> of the container.
     *  The component stretches across the full container width and
     *  "cuts off" a horizontal strip at the bottom.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint DOCK_SOUTH = MigAddConstraint.of("dock south");

    /**
     *  <b>Docks</b> this component against the <b>right edge</b> of the container.
     *  The component stretches across the full container height (between any
     *  north/south docks) and "cuts off" a vertical strip on the right.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint DOCK_EAST  = MigAddConstraint.of("dock east");

    /**
     *  <b>Docks</b> this component against the <b>left edge</b> of the container.
     *  The component stretches across the full container height (between any
     *  north/south docks) and "cuts off" a vertical strip on the left.
     *  <p>This is a <b>component-level</b> constraint.
     */
    public static MigAddConstraint DOCK_WEST  = MigAddConstraint.of("dock west");

    /**
     *  <b>Docks</b> this component against the edge of the container
     *  indicated by the given {@link UI.Side}.
     *  <p>This is a <b>component-level</b> constraint.
     *
     * @param pos The side of the container to dock against.
     * @return A docking constraint for the specified side.
     */
    public static MigAddConstraint DOCK( UI.Side pos ) { return MigAddConstraint.of("dock " + pos.toDirectionString()); }

    /**
     *  A factory method for creating a {@link net.miginfocom.layout.LC} instance.
     * @return A {@link net.miginfocom.layout.LC} instance.
     */
    public static LC LC() { return new LC(); }
    /**
     *  A factory method for creating an {@link net.miginfocom.layout.AC} (Axis Constraints) instance.
     *  Use this when you need to define <b>column or row constraints</b> programmatically
     *  via the MigLayout API rather than as a constraint string.
     *  The returned object provides a fluent builder for specifying sizes, gaps,
     *  alignments, and grow/shrink behaviour for each column or row in the grid.
     *
     * @return A fresh {@link net.miginfocom.layout.AC} instance.
     */
    public static AC AC() { return new AC(); }

    /**
     *  A factory method for creating a {@link net.miginfocom.layout.CC} (Component Constraints) instance.
     *  Use this when you need fine-grained, programmatic control over a single component's
     *  placement and sizing — position, span, split, grow, shrink, alignment, gaps, and more —
     *  via the MigLayout API rather than as a constraint string.
     *
     * @return A fresh {@link net.miginfocom.layout.CC} instance.
     */
    public static CC CC() { return new CC(); }

    /**
     *  A factory method for creating an {@link swingtree.layout.AddConstraint} of the
     *  {@link swingtree.layout.FlowCell} type, that is used to define at which parent size category
     *  how many cells the component should span as part of a {@link swingtree.layout.ResponsiveGridFlowLayout}
     *  layout configuration.<br>
     *  <p>
     *  Here is an example of how this factory method might be used
     *  as part of a larger UI declaration:
     *  <pre>{@code
     *    UI.panel().withFlowLayout()
     *    .withPrefSize(400, 300)
     *    .add(UI.AUTO_SPAN( it->it.small(12).medium(6).large(8) ),
     *         html("A red cell").withStyle(it->it
     *             .backgroundColor(UI.Color.RED)
     *         )
     *    )
     *    .add(UI.AUTO_SPAN( it->it.small(12).medium(6).large(4) ),
     *         html("a green cell").withStyle(it->it
     *             .backgroundColor(Color.GREEN)
     *         )
     *    )
     *  }</pre>
     *  In the above example, the {@code it} parameter of the {@code Configurator}
     *  is an instance of the {@link FlowCellConf} class, which defines cell span sizes
     *  for different parent size categories.<br>
     *  These parent size categories are determined by the width of the parent container
     *  compared to its preferred width.<br>
     *  So a parent is considered larger if its width is closer to its preferred width
     *  and smaller if its width is closer to 0.<br>
     *  <p>
     *  The {@link Configurator} passed to this method is called every time the {@link ResponsiveGridFlowLayout}
     *  updates the layout of the parent container.
     *  This allows it to determine the number of cells a component should span dynamically at a given size.<br>
     *  <b>If you do not perform any configuration in the supplied configurator,
     *  then the component will span 12 cells at all parent sizes.</b><br>
     *  <p>
     *  The {@link swingtree.UIForAnySwing#withFlowLayout()} creates the necessary {@link ResponsiveGridFlowLayout}
     *  and attaches it to the panel.<br>
     *  <p><b>
     *      Note that a {@link ResponsiveGridFlowLayout} is required for the {@link FlowCell} configuration
     *      to have any effect. The {@link FlowCell} configuration is not compatible with other layout managers
     *      like {@link net.miginfocom.swing.MigLayout}.
     *  </b>
     * @param configurator A {@link swingtree.api.Configurator} that configures a {@link swingtree.layout.FlowCellConf} instance.
     * @return An {@link swingtree.layout.FlowCell} instance containing the responsive cell span
     *         configuration for a component that is part of a parent component with
     *         a {@link swingtree.layout.ResponsiveGridFlowLayout} layout manager.
     */
    public static FlowCell AUTO_SPAN( Configurator<FlowCellConf> configurator ) {
        return new FlowCell(configurator);
    }

    /**
     *  A factory method for creating a {@link swingtree.layout.FlowCell} instance
     *  that will span the given number of cells for each parent size category
     *  when used in a {@link swingtree.layout.ResponsiveGridFlowLayout} layout.<br>
     *  <p>
     *  Here is an example of how this factory method might be used
     *  as part of a larger UI declaration:
     *  <pre>{@code
     *    UI.panel().withFlowLayout()
     *    .withPrefSize(400, 300)
     *    .add(UI.AUTO_SPAN(12),
     *         html("A red cell").withStyle(it->it
     *             .backgroundColor(UI.Color.RED)
     *         )
     *    )
     *    .add(UI.AUTO_SPAN(6),
     *         html("a green cell").withStyle(it->it
     *             .backgroundColor(Color.GREEN)
     *         )
     *    )
     *  }</pre>
     *  In the above example, the first cell will always span 12 cells
     *  and the second cell will always span 6 cells, regardless of the parent size.<br>
     *  <p>
     *  The {@link swingtree.UIForAnySwing#withFlowLayout()} creates the necessary {@link ResponsiveGridFlowLayout}
     *  and attaches it to the panel.<br>
     *  <p><b>
     *      Note that a {@link ResponsiveGridFlowLayout} is required for the {@link FlowCell} configuration
     *      to have any effect. The {@link FlowCell} configuration is not compatible with other layout managers
     *      like {@link net.miginfocom.swing.MigLayout}.
     *  </b>
     * @param numberOfCells The number of cells to, irrespective of parent size category.
     * @return An {@link swingtree.layout.FlowCell} instance containing the responsive cell span
     *         configuration for a component that is part of a parent component with
     *         a {@link swingtree.layout.ResponsiveGridFlowLayout} layout manager.
     */
    public static FlowCell AUTO_SPAN( int numberOfCells ) {
        return new FlowCell(it->it
                .verySmall(numberOfCells)
                .small(numberOfCells)
                .medium(numberOfCells)
                .large(numberOfCells)
                .veryLarge(numberOfCells)
                .oversize(numberOfCells)
        );
    }
}