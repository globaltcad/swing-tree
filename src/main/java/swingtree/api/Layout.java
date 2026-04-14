package swingtree.api;

import com.google.errorprone.annotations.Immutable;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.layout.AddConstraint;
import swingtree.layout.FlowCell;
import swingtree.layout.FlowCellConf;
import swingtree.layout.LayoutConstraint;
import swingtree.layout.MigAddConstraint;
import swingtree.layout.ResponsiveGridFlowLayout;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.StyleConf;

import sprouts.Association;
import sprouts.Pair;
import swingtree.layout.Bounds;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.Objects;

/**
 *    An abstract representation of an immutable layout configuration for a specific component,
 *    for which layout manager specific implementations can be instantiated through
 *    various factory methods like {@link Layout#border()}, {@link Layout#flow()}, {@link Layout#grid(int, int)}...
 *    and then supplied to the style API through {@link ComponentStyleDelegate#layout(Layout)}
 *    so that the layout can then be installed onto a component dynamically.
 *    <p>
 *    The various layout types hold necessary information
 *    and implementation logic required for installing the layout onto a component
 *    through the {@link #installFor(JComponent)} method,
 *    which will be used by the style engine of SwingTree
 *    every time the layout object state changes compared to the previous state
 *    effectively making the layout mechanics of a component fully dynamic.
 *    <p>
 *    You may implement this interface to create custom layout configurations
 *    for other kinds of {@link LayoutManager} implementations.
 *    <p>
 *    This interface also contains various implementations
 *    for supporting the most common types of {@link LayoutManager}s.
 *
 * @see swingtree.UIForAnySwing#withLayout(sprouts.Val) For a common practical usecase, see this method.
 */
@Immutable
public interface Layout
{
    /**
     * @return A hash code value for this layout.
     */
    @Override int hashCode();

    /**
     * @param o The object to compare this layout to.
     * @return {@code true} if the supplied object is a layout
     *         that is equal to this layout, {@code false} otherwise.
     */
    @Override boolean equals( Object o );

    /**
     * Installs this layout for the supplied component.
     *
     * @param component The component to install this layout for.
     */
    void installFor( JComponent component );

    /**
     *  A factory method for creating a layout that does nothing
     *  (i.e. it does not install any layout for a component).
     *  This is a no-op layout that can be used to represent the lack of a specific layout
     *  being set for a component without having to set the layout to {@code null}.
     *
     * @return A layout that does nothing, i.e. it does not install any layout for a component.
     */
    static Layout unspecific() { return Constants.UNSPECIFIC_LAYOUT_CONSTANT; }

    /**
     *  Returns a {@link None} layout that removes any existing {@link LayoutManager}
     *  from a component (sets it to {@code null}), enabling manual positioning of
     *  child components via their {@link Component#setBounds(int, int, int, int)} method.
     *  <p>
     *  To also specify the initial bounds of child components declaratively,
     *  chain {@link None#withChildBounds(Bounds...)} on the returned instance, or
     *  use the {@link #none(Bounds...)} shorthand factory.
     *
     * @return A {@link None} layout that removes any existing layout manager from a component.
     */
    static None none() { return (None) Constants.NONE_LAYOUT_CONSTANT; }

    /**
     *  A convenience factory that creates a {@link None} layout pre-loaded with
     *  per-child {@link Bounds}.  This is a shorthand for:
     *  <pre>{@code
     *      Layout.none().withChildBounds(childBounds)
     *  }</pre>
     *  When {@code installFor} is called, the layout manager is first removed from
     *  the component (enabling absolute positioning), and then each supplied
     *  {@link Bounds} entry is applied to the corresponding child by index via
     *  {@link Component#setBounds(int, int, int, int)}.
     *  Because the underlying storage is a sparse {@link Association}, you only
     *  need to supply bounds for the children you actually want to position;
     *  children without an entry are left untouched.
     *
     * @param childBounds The {@link Bounds} to apply to the component's children,
     *                    in child-index order.
     * @return A {@link None} layout that removes any layout manager and applies
     *         the given bounds to the corresponding children.
     */
    static None none( Bounds... childBounds ) {
        return ((None) Constants.NONE_LAYOUT_CONSTANT).withChildBounds(childBounds);
    }

    /**
     *  The preferred factory method for creating a {@link MigLayout}-based layout configuration
     *  from type-safe {@link LayoutConstraint} objects.
     *  <p>
     *  {@link LayoutConstraint} is a composable, type-safe wrapper around MigLayout constraint
     *  strings. The recommended way to use it is through the constants and factory methods
     *  available via {@code import static swingtree.UI.*}, which can then be combined
     *  with {@link LayoutConstraint#and(LayoutConstraint)}:
     *  <pre>{@code
     *      import static swingtree.UI.*;
     *      // ...
     *      Layout.mig( FILL.and(WRAP(2)), "[shrink][grow]", "[]8[]" )
     *  }</pre>
     *  Using {@link LayoutConstraint} instead of raw strings catches typos at call-site and
     *  makes constraint composition explicit and refactor-friendly.
     *  See <a href="http://www.miglayout.com/whitepaper.html">the MigLayout whitepaper</a>
     *  for full constraint documentation.
     *  <p>
     *  The returned {@link ForMigLayout} instance supports fluent chaining to specify
     *  per-child component constraints via the various
     *  {@link ForMigLayout#withChildConstraints(MigAddConstraint...)} overloads.
     *
     * @param constr The general layout constraints for the {@link MigLayout}
     *               (e.g. {@code FILL.and(WRAP(2))}).
     * @param colConstr The column constraints for the {@link MigLayout}
     *                  (e.g. {@code LayoutConstraint.of("[shrink][grow]")}).
     * @param rowConstr The row constraints for the {@link MigLayout}
     *                  (e.g. {@code LayoutConstraint.of("[]8[]")}).
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig(
        LayoutConstraint constr,
        LayoutConstraint colConstr,
        LayoutConstraint rowConstr
    ) {
        return new ForMigLayout( constr, colConstr, rowConstr );
    }

    /**
     *  A factory method for creating a {@link MigLayout}-based layout configuration
     *  from type-safe {@link LayoutConstraint} objects, without column constraints.
     *  This is the preferred approach over the plain-{@link String} overloads.
     *  <p>
     *  See {@link #mig(LayoutConstraint, LayoutConstraint, LayoutConstraint)} for
     *  full details on the {@link LayoutConstraint} API and chaining child constraints.
     *
     * @param constr The general layout constraints for the {@link MigLayout}.
     * @param rowConstr The row constraints for the {@link MigLayout}.
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig(
        LayoutConstraint constr,
        LayoutConstraint rowConstr
    ) {
        return new ForMigLayout( constr, LayoutConstraint.of(""), rowConstr );
    }

    /**
     *  A factory method for creating a {@link MigLayout}-based layout configuration
     *  from a single type-safe {@link LayoutConstraint}, with no column or row constraints.
     *  This is the preferred approach over the plain-{@link String} overload.
     *  <p>
     *  See {@link #mig(LayoutConstraint, LayoutConstraint, LayoutConstraint)} for
     *  full details on the {@link LayoutConstraint} API and chaining child constraints.
     *
     * @param constr The general layout constraints for the {@link MigLayout}.
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig( LayoutConstraint constr ) {
        return new ForMigLayout( constr, LayoutConstraint.of(""), LayoutConstraint.of("") );
    }

    /**
     *  A convenience factory method that creates a {@link MigLayout}-based layout configuration
     *  from a single type-safe {@link LayoutConstraint} together with per-child
     *  {@link MigAddConstraint}s for the component's direct children.
     *  <p>
     *  The {@code childConstraints} are mapped positionally: the first entry is applied to the
     *  first child, the second to the second child, and so on.
     *  Excess entries (more constraints than children) are silently ignored;
     *  children without a matching entry keep whatever constraint the parent
     *  {@link MigLayout} already has for them.
     *  <p>
     *  This is a shorthand for:
     *  <pre>{@code
     *      Layout.mig(constr).withChildConstraints(childConstraints)
     *  }</pre>
     *  See {@link #mig(LayoutConstraint, LayoutConstraint, LayoutConstraint)} for full details
     *  on the {@link LayoutConstraint} API.
     *
     * @param constr The general layout constraints for the {@link MigLayout}.
     * @param childConstraints The {@link MigAddConstraint}s to apply to the component's children,
     *                         in child-index order.
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig( LayoutConstraint constr, MigAddConstraint... childConstraints ) {
        return mig(constr).withChildConstraints(childConstraints);
    }

    /**
     *  A convenience overload of {@link #mig(LayoutConstraint, LayoutConstraint, LayoutConstraint)}
     *  that accepts plain constraint strings instead of {@link LayoutConstraint} objects.
     *  Each string is wrapped via {@link LayoutConstraint#of(String...)} before being forwarded.
     *  <p>
     *  Prefer the {@link LayoutConstraint}-based overloads for new code, as they are
     *  composable and less error-prone than raw strings.
     *  Click <a href="http://www.miglayout.com/">here</a> for more information about MigLayout.
     *
     * @param constr The general layout constraints string for the {@link MigLayout}.
     * @param colConstr The column constraints string for the {@link MigLayout}.
     * @param rowConstr The row constraints string for the {@link MigLayout}.
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig(
        String constr,
        String colConstr,
        String rowConstr
    ) {
        return mig( LayoutConstraint.of(constr), LayoutConstraint.of(colConstr), LayoutConstraint.of(rowConstr) );
    }

    /**
     *  A convenience overload of {@link #mig(LayoutConstraint, LayoutConstraint)}
     *  that accepts plain constraint strings instead of {@link LayoutConstraint} objects.
     *  Each string is wrapped via {@link LayoutConstraint#of(String...)} before being forwarded.
     *  <p>
     *  Prefer the {@link LayoutConstraint}-based overloads for new code, as they are
     *  composable and less error-prone than raw strings.
     *  Click <a href="http://www.miglayout.com/">here</a> for more information about MigLayout.
     *
     * @param constr The general layout constraints string for the {@link MigLayout}.
     * @param rowConstr The row constraints string for the {@link MigLayout}.
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig(
        String constr,
        String rowConstr
    ) {
        return mig( LayoutConstraint.of(constr), LayoutConstraint.of(rowConstr) );
    }

    /**
     *  A convenience overload of {@link #mig(LayoutConstraint)}
     *  that accepts a plain constraint string instead of a {@link LayoutConstraint} object.
     *  The string is wrapped via {@link LayoutConstraint#of(String...)} before being forwarded.
     *  <p>
     *  Prefer the {@link LayoutConstraint}-based overloads for new code, as they are
     *  composable and less error-prone than raw strings.
     *  In case you are not familiar with the MigLayout constraints, you can find more information
     *  about them <a href="http://www.miglayout.com/whitepaper.html">here</a>.
     *
     * @param constr The general layout constraints string for the {@link MigLayout}.
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig( String constr ) {
        return mig( LayoutConstraint.of(constr) );
    }

    /**
     *  A convenience factory method that creates a {@link MigLayout}-based layout configuration
     *  from a plain constraint string together with per-child {@link MigAddConstraint}s
     *  for the component's direct children.
     *  <p>
     *  The {@code childConstraints} are mapped positionally: the first entry is applied to the
     *  first child, the second to the second child, and so on.
     *  This is a shorthand for:
     *  <pre>{@code
     *      Layout.mig(constr).withChildConstraints(childConstraints)
     *  }</pre>
     *  Prefer the {@link LayoutConstraint}-based overload
     *  {@link #mig(LayoutConstraint, MigAddConstraint...)} for new code.
     *
     * @param constr The general layout constraints string for the {@link MigLayout}.
     * @param childConstraints The {@link MigAddConstraint}s to apply to the component's children,
     *                         in child-index order.
     * @return A {@link ForMigLayout} configured with the supplied constraints.
     */
    static ForMigLayout mig( String constr, MigAddConstraint... childConstraints ) {
        return mig( LayoutConstraint.of(constr) ).withChildConstraints(childConstraints);
    }

    /**
     *  A factory method for creating a {@link ForFlowLayout} configuration that installs
     *  a {@link ResponsiveGridFlowLayout} onto a component with the given alignment and gaps.
     *  <p>
     *  The returned {@link ForFlowLayout} supports fluent chaining to specify per-child
     *  {@link FlowCell} constraints via the various
     *  {@link ForFlowLayout#withChildConstraints(FlowCell...)} overloads, enabling fully
     *  reactive responsive layouts when used together with
     *  {@link swingtree.UIForAnySwing#withLayout(sprouts.Val)}.
     *
     * @param align The alignment for the layout, which has to be one of <ul>
     *               <li>{@link UI.HorizontalAlignment#LEFT}</li>
     *               <li>{@link UI.HorizontalAlignment#CENTER}</li>
     *               <li>{@link UI.HorizontalAlignment#RIGHT}</li>
     *               <li>{@link UI.HorizontalAlignment#LEADING}</li>
     *               <li>{@link UI.HorizontalAlignment#TRAILING}</li>
     *              </ul>
     * @param hgap The horizontal gap between components, in pixels.
     * @param vgap The vertical gap between component rows, in pixels.
     * @return A {@link ForFlowLayout} configured with the supplied alignment and gaps.
     */
    static ForFlowLayout flow( UI.HorizontalAlignment align, int hgap, int vgap ) {
        return new ForFlowLayout( align, hgap, vgap );
    }

    /**
     *  A factory method for creating a {@link ForFlowLayout} configuration that installs
     *  a {@link ResponsiveGridFlowLayout} with the given alignment and default gaps of 5 pixels.
     *  <p>
     *  The returned {@link ForFlowLayout} supports fluent chaining to specify per-child
     *  {@link FlowCell} constraints.
     *  See {@link #flow(UI.HorizontalAlignment, int, int)} for full details.
     *
     * @param align The horizontal alignment for the flow of components.
     * @return A {@link ForFlowLayout} configured with the supplied alignment.
     */
    static ForFlowLayout flow( UI.HorizontalAlignment align ) {
        return new ForFlowLayout( align, 5, 5 );
    }

    /**
     *  A factory method for creating a {@link ForFlowLayout} configuration that installs
     *  a {@link ResponsiveGridFlowLayout} with centered alignment and default gaps of 5 pixels.
     *  <p>
     *  The returned {@link ForFlowLayout} supports fluent chaining to specify per-child
     *  {@link FlowCell} constraints.
     *  See {@link #flow(UI.HorizontalAlignment, int, int)} for full details.
     *
     * @return A {@link ForFlowLayout} with centered alignment and 5-pixel gaps.
     */
    static ForFlowLayout flow() {
        return new ForFlowLayout( UI.HorizontalAlignment.CENTER, 5, 5 );
    }

    /**
     *  A convenience factory method that creates a centered {@link ForFlowLayout} pre-loaded
     *  with per-child {@link FlowCell} constraints.  This is a shorthand for:
     *  <pre>{@code
     *      Layout.flow().withChildConstraints(childConstraints)
     *  }</pre>
     *  Each {@link FlowCell} is typically created via {@link swingtree.UI#AUTO_SPAN(Configurator)}:
     *  <pre>{@code
     *      import static swingtree.UI.*;
     *      // ...
     *      Var<Layout> layout = Var.of(
     *          Layout.flow(
     *              AUTO_SPAN( it -> it.small(12).medium(6) ),
     *              AUTO_SPAN( it -> it.small(12).medium(6) )
     *          )
     *      );
     *      UI.panel()
     *        .withLayout(layout)
     *        .add( label("Left") )
     *        .add( label("Right") );
     *  }</pre>
     *  Changing the {@code layout} property at runtime will reinstall the layout and re-push
     *  all child {@link FlowCell} constraints, making the span behaviour fully reactive.
     *
     * @param childConstraints The {@link FlowCell} constraints to apply to the component's
     *                         children in child-index order.
     * @return A {@link ForFlowLayout} with centered alignment and the given child constraints.
     */
    static ForFlowLayout flow( FlowCell... childConstraints ) {
        return flow().withChildConstraints(childConstraints);
    }

    /**
     *  A convenience factory method that creates a {@link ForFlowLayout} with the given
     *  alignment and per-child {@link FlowCell} constraints pre-loaded.  This is a shorthand for:
     *  <pre>{@code
     *      Layout.flow(align).withChildConstraints(childConstraints)
     *  }</pre>
     *  See {@link #flow(FlowCell...)} for a usage example and reactive design notes.
     *
     * @param align The horizontal alignment for the flow of components.
     * @param childConstraints The {@link FlowCell} constraints to apply to the component's
     *                         children in child-index order.
     * @return A {@link ForFlowLayout} with the given alignment and child constraints.
     */
    static ForFlowLayout flow( UI.HorizontalAlignment align, FlowCell... childConstraints ) {
        return flow(align).withChildConstraints(childConstraints);
    }

    /**
     * A factory method for creating a layout that installs the {@link BorderLayout}
     * onto a component based on the supplied parameters.
     *
     * @param horizontalGap The horizontal gap for the layout.
     * @param verticalGap The vertical gap for the layout.
     * @return A layout that installs the {@link BorderLayout} onto a component.
     */
    static Layout border( int horizontalGap, int verticalGap ) {
        return new BorderLayoutInstaller( horizontalGap, verticalGap );
    }

    /**
     * A factory method for creating a layout that installs the {@link BorderLayout}
     * onto a component based on the supplied parameters.
     * The installed layout will have a default gap of 0 pixels.
     *
     * @return A layout that installs the {@link BorderLayout} onto a component.
     */
    static Layout border() {
        return new BorderLayoutInstaller( 0, 0 );
    }

    /**
     * A factory method for creating a layout that installs the {@link GridLayout}
     * onto a component based on the supplied parameters.
     *
     * @param rows The number of rows for the layout.
     * @param cols The number of columns for the layout.
     * @param horizontalGap The horizontal gap for the layout.
     * @param verticalGap The vertical gap for the layout.
     * @return A layout that installs the {@link GridLayout} onto a component.
     */
    static Layout grid( int rows, int cols, int horizontalGap, int verticalGap ) {
        return new GridLayoutInstaller( rows, cols, horizontalGap, verticalGap );
    }

    /**
     * A factory method for creating a layout that installs the {@link GridLayout}
     * onto a component based on the supplied parameters.
     * The installed layout will have a default gap of 0 pixels.
     *
     * @param rows The number of rows for the layout.
     * @param cols The number of columns for the layout.
     * @return A layout that installs the {@link GridLayout} onto a component.
     */
    static Layout grid( int rows, int cols ) {
        return new GridLayoutInstaller( rows, cols, 0, 0 );
    }

    /**
     *  A factory method for creating a layout that installs the {@link BoxLayout}
     *  onto a component based on the supplied {@link UI.Axis} parameter.
     *  The axis determines whether the layout will be a horizontal or vertical
     *  {@link BoxLayout}.
     *
     * @param axis The axis for the layout, which has to be one of <ul>
     *                 <li>{@link UI.Axis#X}</li>
     *                 <li>{@link UI.Axis#Y}</li>
     *                 <li>{@link UI.Axis#LINE}</li>
     *                 <li>{@link UI.Axis#PAGE}</li>
     *             </ul>
     *
     * @return A layout that installs the {@link BoxLayout} onto a component.
     */
    static Layout box( UI.Axis axis ) {
        return new ForBoxLayout( axis.forBoxLayout() );
    }

    /**
     *  A factory method for creating a layout that installs the {@link BoxLayout}
     *  onto a component with a default axis of {@link UI.Axis#X}.
     *
     * @return A layout that installs the default {@link BoxLayout} onto a component.
     */
    static Layout box() {
        return new ForBoxLayout( BoxLayout.X_AXIS );
    }

    /**
     *  The {@link Unspecific} layout is a layout that represents the lack
     *  of a specific layout being set for a component.
     *  Note that this does not represent the absence of a {@link LayoutManager}
     *  for a component, but rather the absence of it being specified.
     *  This means that whatever layout is currently installed for a component
     *  will be left as is, and no other layout will be installed for the component.
     *  <p>
     *  Note that this is different from the {@link None} layout,
     *  which represents the absence of a {@link LayoutManager}
     *  for a component (i.e. it removes any existing layout from the component and sets it to {@code null}).
     */
    @Immutable
    final class Unspecific implements Layout
    {
        Unspecific() {}

        @Override public int hashCode() { return 0; }

        @Override
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            return o.getClass() == getClass();
        }

        @Override public String toString() { return getClass().getSimpleName() + "[]"; }

        /**
         *  Does nothing.
         * @param component The component to install the layout for.
         */
        @Override public void installFor( JComponent component ) { /* Do nothing. */ }
    }

    /**
     *  The {@link None} layout removes any existing {@link LayoutManager} from a component
     *  (sets it to {@code null}), enabling fully manual positioning of child components
     *  via {@link Component#setBounds(int, int, int, int)}.
     *  <p>
     *  Beyond simply clearing the layout manager, a {@link None} instance can carry a sparse
     *  {@link Association} of per-child {@link Bounds} that are applied to the component's
     *  direct children during {@link #installFor(JComponent)}.  This lets you declare the
     *  absolute position and size of each child you care about right inside the
     *  {@link Layout} object, keeping the layout specification co-located with the
     *  component tree rather than scattered across imperative setup code:
     *  <pre>{@code
     *      import static swingtree.UI.*;
     *      import swingtree.layout.Bounds;
     *      // ...
     *      Var<Layout> layout = Var.of(
     *          Layout.none(
     *              Bounds.of(  0,   0, 120, 40),  // child 0
     *              Bounds.of(130,   0, 120, 40),  // child 1
     *              Bounds.of(  0,  50, 250, 80)   // child 2
     *          )
     *      );
     *
     *      UI.panel().withLayout(layout)
     *        .add( button("A") )
     *        .add( button("B") )
     *        .add( label("C")  );
     *  }</pre>
     *  Because the association is sparse you can also target a single child by index
     *  without touching the others:
     *  <pre>{@code
     *      layout.set( Layout.none().withChildBound(2, Bounds.of(0, 50, 250, 80)) );
     *  }</pre>
     *  <p>
     *  Note that this is different from the {@link Unspecific} layout, which does nothing
     *  at all — {@link None} actively removes the layout manager.
     */
    @Immutable
    @SuppressWarnings("Immutable")
    final class None implements Layout
    {
        private final Association<Integer, Bounds> _childBounds;

        None() {
            this( Association.betweenSorted(Integer.class, Bounds.class) );
        }

        None( Association<Integer, Bounds> childBounds ) {
            _childBounds = Objects.requireNonNull(childBounds);
        }

        // -- Per-child bounds withers --

        /**
         *  Returns a new {@link None} layout whose per-child {@link Bounds} are replaced
         *  by the supplied sorted {@link Association}.
         *  <p>
         *  Keys are child indices ({@code 0} = first child, {@code 1} = second, etc.);
         *  the association is sparse, so you only need to include entries for the children
         *  you actually want to position.  Children whose index has no entry are left
         *  untouched.  An empty association produces the plain "remove layout only" behaviour.
         *
         * @param childBounds A sorted {@link Association} mapping child indices to
         *                    the {@link Bounds} to apply.
         * @return A new {@link None} layout with the updated child bounds,
         *         or the shared {@link Layout#none()} constant when the association is empty.
         */
        public None withChildBounds( Association<Integer, Bounds> childBounds ) {
            Objects.requireNonNull(childBounds);
            if ( childBounds.isEmpty() )
                return (None) Constants.NONE_LAYOUT_CONSTANT;
            return new None(childBounds);
        }

        /**
         *  Returns a new {@link None} layout with per-child {@link Bounds} built from
         *  the supplied varargs array.  Entries are mapped positionally: index&nbsp;0
         *  applies to the first child, index&nbsp;1 to the second, and so on.
         *  Passing an empty array returns the shared {@link Layout#none()} constant.
         *
         * @param childBounds The {@link Bounds} to apply to the component's children,
         *                    in child-index order.
         * @return A new {@link None} layout with the updated child bounds.
         */
        public None withChildBounds( Bounds... childBounds ) {
            Association<Integer, Bounds> assoc = Association.betweenSorted(Integer.class, Bounds.class);
            for ( int i = 0; i < childBounds.length; i++ )
                assoc = assoc.put(i, childBounds[i]);
            return withChildBounds(assoc);
        }

        /**
         *  Returns a new {@link None} layout with the {@link Bounds} at the given child
         *  index replaced by the supplied value.  All other child bounds are copied unchanged.
         *  <p>
         *  Because the underlying storage is a sparse {@link Association}, no padding is
         *  needed: the bound is stored at exactly {@code index}, regardless of whether
         *  lower indices have entries.
         *
         * @param index The zero-based index of the child whose bounds to update.
         * @param childBound The new {@link Bounds} for the child at {@code index}.
         * @return A new {@link None} layout with the updated child bound at {@code index}.
         * @throws IndexOutOfBoundsException if {@code index} is negative.
         */
        public None withChildBound( int index, Bounds childBound ) {
            Objects.requireNonNull(childBound);
            if ( index < 0 )
                throw new IndexOutOfBoundsException("Child index must not be negative, but was: " + index);
            return withChildBounds( _childBounds.put(index, childBound) );
        }

        /**
         *  Returns a new {@link None} layout with the supplied {@link Bounds} appended
         *  as the constraint for the next child in sequence (i.e. at index = max existing
         *  key + 1, or 0 if no bounds have been set yet).
         *
         * @param childBound The {@link Bounds} to append for the next child.
         * @return A new {@link None} layout with the bound appended.
         */
        public None withAddedChildBound( Bounds childBound ) {
            Objects.requireNonNull(childBound);
            int nextIndex = 0;
            for ( Integer key : _childBounds.keySet() )
                nextIndex = key + 1;
            return withChildBounds( _childBounds.put(nextIndex, childBound) );
        }

        // -- Object contract --

        @Override public int hashCode() { return _childBounds.hashCode(); }

        @Override
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            None other = (None) o;
            return _childBounds.equals(other._childBounds);
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[childBounds=" + _childBounds + "]";
        }

        // -- Layout installation --

        /**
         *  Installs this layout for the supplied component in two phases:
         *  <ol>
         *    <li><b>Layout removal</b> — the existing {@link LayoutManager} (if any) is
         *        replaced with {@code null}, enabling absolute positioning of child
         *        components.</li>
         *    <li><b>Child bounds</b> — if any per-child {@link Bounds} were specified,
         *        each stored entry is applied to the corresponding direct child of
         *        {@code component} (by index) via
         *        {@link Component#setBounds(int, int, int, int)}.
         *        Only entries that differ from the child's current bounds are written,
         *        and {@link JComponent#repaint()} is called exactly once at the end if
         *        anything changed.</li>
         *  </ol>
         *
         * @param component The component to remove the layout manager from and
         *                  optionally apply child bounds to.
         */
        @Override
        public void installFor( JComponent component ) {
            // Phase 1: remove the layout manager:
            LayoutManager oldManager = component.getLayout();
            if ( oldManager != null ) {
                component.setLayout(null);
                component.revalidate();
            }
            // Phase 2: apply per-child bounds if specified:
            if ( _childBounds.isNotEmpty() ) {
                Component[] children = component.getComponents();
                boolean changed = false;
                for ( Pair<Integer, Bounds> entry : _childBounds ) {
                    int i = entry.first();
                    if ( i < 0 || i >= children.length ) continue;
                    java.awt.Rectangle desired  = UI.scale(entry.second().toRectangle());
                    java.awt.Rectangle existing = children[i].getBounds();
                    if ( !desired.equals(existing) ) {
                        children[i].setBounds(desired);
                        changed = true;
                    }
                }
                if ( changed )
                    component.repaint();
            }
        }
    }

    /**
     *  An immutable {@link Layout} implementation that configures and installs a
     *  {@link MigLayout} onto a component. It holds three kinds of constraints:
     *  <ul>
     *    <li><b>General layout constraints</b> ({@code constr}) — control global layout
     *        behaviour such as wrapping, filling, gaps and hiding mode.</li>
     *    <li><b>Column constraints</b> ({@code colConstr}) — define the sizing and
     *        alignment rules for each column in the grid.</li>
     *    <li><b>Row constraints</b> ({@code rowConstr}) — define the sizing and
     *        alignment rules for each row in the grid.</li>
     *    <li><b>Per-child component constraints</b> ({@code childConstraints}) — a
     *        sorted {@link Association} mapping child indices ({@link Integer}) to
     *        {@link MigAddConstraint}s applied to the component's direct children.
     *        Unlike a positional tuple, the association is sparse: you only need to
     *        include entries for the children you actually want to configure; children
     *        whose index has no entry keep whatever constraint the
     *        {@link MigLayout} already has for them.</li>
     *  </ul>
     *  <p>
     *  Instances are created via the {@link Layout#mig(LayoutConstraint)} family of
     *  factory methods and are further configured through the fluent {@code with*} wither
     *  methods, which all return a new immutable instance:
     *  <pre>{@code
     *      import static swingtree.UI.*;
     *      // ...
     *      Layout.mig( FILL.and(WRAP(2)), "[shrink][grow]", "[]8[]" )
     *            .withChildConstraints( RIGHT, GROW_X, RIGHT, GROW_X )
     *  }</pre>
     *  Whenever any property of this configuration changes (detected by the style engine
     *  via {@link #equals}/{@link #hashCode}), the {@link #installFor(JComponent)} method
     *  is called, which surgically updates only the properties that have changed and
     *  calls {@link JComponent#revalidate()} to trigger a layout refresh.
     */
    @Immutable
    @SuppressWarnings("Immutable")
    final class ForMigLayout implements Layout
    {
        private final LayoutConstraint                        _constr;
        private final LayoutConstraint                        _colConstr;
        private final LayoutConstraint                        _rowConstr;
        private final Association<Integer, MigAddConstraint>  _childConstraints;

        ForMigLayout( LayoutConstraint constr, LayoutConstraint colConstr, LayoutConstraint rowConstr ) {
            this( constr, colConstr, rowConstr, Association.betweenSorted(Integer.class, MigAddConstraint.class) );
        }

        ForMigLayout(
            LayoutConstraint                       constr,
            LayoutConstraint                       colConstr,
            LayoutConstraint                       rowConstr,
            Association<Integer, MigAddConstraint> childConstraints
        ) {
            _constr           = Objects.requireNonNull(constr);
            _colConstr        = Objects.requireNonNull(colConstr);
            _rowConstr        = Objects.requireNonNull(rowConstr);
            _childConstraints = Objects.requireNonNull(childConstraints);
        }

        // -- General layout constraint withers --

        /**
         *  Returns a new {@link ForMigLayout} instance with the supplied general layout constraints
         *  and all other properties copied from this instance.
         *  This is the preferred overload as it works with the type-safe {@link LayoutConstraint}
         *  API, which supports composition via {@link LayoutConstraint#and(LayoutConstraint)}.
         *
         * @param constr The new general layout constraints for the {@link MigLayout}.
         * @return A new {@link ForMigLayout} instance with the updated layout constraints.
         */
        public ForMigLayout withConstraint( LayoutConstraint constr ) {
            return new ForMigLayout( constr, _colConstr, _rowConstr, _childConstraints );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the supplied general layout constraints
         *  and all other properties copied from this instance.
         *  The string is wrapped via {@link LayoutConstraint#of(String...)} and forwarded
         *  to {@link #withConstraint(LayoutConstraint)}.
         *  Prefer {@link #withConstraint(LayoutConstraint)} for new code.
         *
         * @param constr The new general layout constraints string for the {@link MigLayout}.
         * @return A new {@link ForMigLayout} instance with the updated layout constraints.
         */
        public ForMigLayout withConstraint( String constr ) { return withConstraint( LayoutConstraint.of(constr) ); }

        // -- Row constraint withers --

        /**
         *  Returns a new {@link ForMigLayout} instance with the supplied row constraints
         *  and all other properties copied from this instance.
         *  This is the preferred overload as it works with the type-safe {@link LayoutConstraint}
         *  API, which supports composition via {@link LayoutConstraint#and(LayoutConstraint)}.
         *
         * @param rowConstr The new row constraints for the {@link MigLayout}.
         * @return A new {@link ForMigLayout} instance with the updated row constraints.
         */
        public ForMigLayout withRowConstraint( LayoutConstraint rowConstr ) {
            return new ForMigLayout( _constr, _colConstr, rowConstr, _childConstraints );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the supplied row constraints
         *  and all other properties copied from this instance.
         *  The string is wrapped via {@link LayoutConstraint#of(String...)} and forwarded
         *  to {@link #withRowConstraint(LayoutConstraint)}.
         *  Prefer {@link #withRowConstraint(LayoutConstraint)} for new code.
         *
         * @param rowConstr The new row constraints string for the {@link MigLayout}.
         * @return A new {@link ForMigLayout} instance with the updated row constraints.
         */
        public ForMigLayout withRowConstraint( String rowConstr ) { return withRowConstraint( LayoutConstraint.of(rowConstr) ); }

        // -- Column constraint withers --

        /**
         *  Returns a new {@link ForMigLayout} instance with the supplied column constraints
         *  and all other properties copied from this instance.
         *  This is the preferred overload as it works with the type-safe {@link LayoutConstraint}
         *  API, which supports composition via {@link LayoutConstraint#and(LayoutConstraint)}.
         *
         * @param colConstr The new column constraints for the {@link MigLayout}.
         * @return A new {@link ForMigLayout} instance with the updated column constraints.
         */
        public ForMigLayout withColumnConstraint( LayoutConstraint colConstr ) {
            return new ForMigLayout( _constr, colConstr, _rowConstr, _childConstraints );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the supplied column constraints
         *  and all other properties copied from this instance.
         *  The string is wrapped via {@link LayoutConstraint#of(String...)} and forwarded
         *  to {@link #withColumnConstraint(LayoutConstraint)}.
         *  Prefer {@link #withColumnConstraint(LayoutConstraint)} for new code.
         *
         * @param colConstr The new column constraints string for the {@link MigLayout}.
         * @return A new {@link ForMigLayout} instance with the updated column constraints.
         */
        public ForMigLayout withColumnConstraint( String colConstr ) { return withColumnConstraint( LayoutConstraint.of(colConstr) ); }

        // -- Per-child component constraint withers --

        /**
         *  Returns a new {@link ForMigLayout} instance whose per-child component constraints
         *  are replaced by the supplied sorted {@link Association}.
         *  <p>
         *  Keys are child indices ({@code 0} = first child, {@code 1} = second, etc.);
         *  the association is sparse, so you only need to include entries for children
         *  you actually want to configure.
         *  Children whose index has no entry in the association are left untouched.
         *  An empty association means no constraints are stored in this layout object;
         *  any constraints previously applied to children by an earlier {@code installFor}
         *  call remain in the {@link MigLayout} until explicitly overwritten.
         *
         * @param childConstraints A sorted {@link Association} mapping child indices to
         *                         the {@link MigAddConstraint} to apply.
         * @return A new {@link ForMigLayout} with the updated child constraints.
         */
        public ForMigLayout withChildConstraints( Association<Integer, MigAddConstraint> childConstraints ) {
            return new ForMigLayout( _constr, _colConstr, _rowConstr, Objects.requireNonNull(childConstraints) );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance whose per-child component constraints
         *  are replaced by the supplied varargs array of {@link MigAddConstraint}s.
         *  <p>
         *  The constraints are mapped positionally to the component's children:
         *  the first argument applies to the first child, the second to the second, and so on.
         *  Children at indices beyond the supplied array length are left untouched.
         *  Passing an empty array stores no constraints in this layout object;
         *  any constraints previously applied to children by an earlier {@code installFor}
         *  call remain in the {@link MigLayout} until explicitly overwritten.
         *  <p>
         *  This is the most concise way to specify per-child constraints for common cases:
         *  <pre>{@code
         *      import static swingtree.UI.*;
         *      // ...
         *      Layout.mig( FILL.and(WRAP(2)) )
         *            .withChildConstraints( RIGHT, GROW_X, RIGHT, GROW_X )
         *  }</pre>
         *
         * @param childConstraints The {@link MigAddConstraint}s to apply to the component's
         *                         children, in child-index order.
         * @return A new {@link ForMigLayout} with the updated child constraints.
         */
        public ForMigLayout withChildConstraints( MigAddConstraint... childConstraints ) {
            Association<Integer, MigAddConstraint> assoc = Association.betweenSorted(Integer.class, MigAddConstraint.class);
            for ( int i = 0; i < childConstraints.length; i++ )
                assoc = assoc.put(i, childConstraints[i]);
            return withChildConstraints(assoc);
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the {@link MigAddConstraint} at the
         *  given child index replaced by the supplied value.
         *  All other child constraints and all other properties are copied unchanged.
         *  <p>
         *  Because the underlying storage is a sparse {@link Association}, no padding
         *  is needed: the constraint is stored at exactly {@code index}, regardless of
         *  whether lower indices have entries.
         *
         * @param index The zero-based index of the child whose constraint to update.
         *              The first child has index&nbsp;0.
         * @param childConstraint The new {@link MigAddConstraint} for the child at {@code index}.
         * @return A new {@link ForMigLayout} with the updated child constraint at {@code index}.
         * @throws IndexOutOfBoundsException if {@code index} is negative.
         */
        public ForMigLayout withChildConstraint( int index, MigAddConstraint childConstraint ) {
            Objects.requireNonNull(childConstraint);
            if ( index < 0 )
                throw new IndexOutOfBoundsException("Child index must not be negative, but was: " + index);
            return withChildConstraints( _childConstraints.put(index, childConstraint) );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the {@link MigAddConstraint} at the
         *  given child index replaced by a constraint wrapping the supplied string.
         *  The string is converted via {@link MigAddConstraint#of(String...)} and then forwarded
         *  to {@link #withChildConstraint(int, MigAddConstraint)}.
         *  <p>
         *  Because the underlying storage is a sparse {@link Association}, no padding is
         *  needed: the constraint is stored at exactly {@code index}, regardless of whether
         *  lower indices have entries.
         *
         * @param index The zero-based index of the child whose constraint to update.
         * @param childConstraint The MigLayout component-constraint string for the child.
         * @return A new {@link ForMigLayout} with the updated child constraint at {@code index}.
         * @throws IndexOutOfBoundsException if {@code index} is negative.
         */
        public ForMigLayout withChildConstraint( int index, String childConstraint ) {
            return withChildConstraint( index, MigAddConstraint.of(childConstraint) );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the supplied {@link MigAddConstraint}
         *  appended to the end of the existing child-constraint tuple.
         *  This is a convenient alternative to {@link #withChildConstraints(MigAddConstraint...)}
         *  when building up constraints one at a time:
         *  <pre>{@code
         *      import static swingtree.UI.*;
         *      // ...
         *      Layout.mig( FILL.and(WRAP(2)) )
         *            .withAddedChildConstraint( RIGHT )
         *            .withAddedChildConstraint( GROW_X )
         *            .withAddedChildConstraint( RIGHT )
         *            .withAddedChildConstraint( GROW_X )
         *  }</pre>
         *
         * @param childConstraint The {@link MigAddConstraint} to append as the next child
         *                        component constraint.
         * @return A new {@link ForMigLayout} with the constraint appended.
         */
        public ForMigLayout withAddedChildConstraint( MigAddConstraint childConstraint ) {
            Objects.requireNonNull(childConstraint);
            int nextIndex = 0;
            for ( Integer key : _childConstraints.keySet() )
                nextIndex = key + 1;
            return withChildConstraints( _childConstraints.put(nextIndex, childConstraint) );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with a {@link MigAddConstraint} wrapping
         *  the supplied string appended to the end of the existing child-constraint tuple.
         *  The string is converted via {@link MigAddConstraint#of(String...)} and then forwarded
         *  to {@link #withAddedChildConstraint(MigAddConstraint)}.
         *
         * @param childConstraint The MigLayout component-constraint string to append.
         * @return A new {@link ForMigLayout} with the constraint appended.
         */
        public ForMigLayout withAddedChildConstraint( String childConstraint ) {
            return withAddedChildConstraint( MigAddConstraint.of(childConstraint) );
        }

        // -- Object contract --

        @Override public int hashCode() { return Objects.hash(_constr, _rowConstr, _colConstr, _childConstraints); }

        @Override
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForMigLayout other = (ForMigLayout) o;
            return _constr.equals(other._constr)           &&
                   _rowConstr.equals(other._rowConstr)     &&
                   _colConstr.equals(other._colConstr)     &&
                   _childConstraints.equals(other._childConstraints);
        }

        // -- Layout installation --

        /**
         *  Installs a {@link MigLayout} onto the supplied component and applies all constraints
         *  stored in this configuration.
         *  <p>
         *  The installation proceeds in three phases:
         *  <ol>
         *    <li><b>Self constraint</b> — if this component's own style holds a
         *        {@link StyleConf#layoutConstraint() layout constraint} and its parent uses a
         *        {@link MigLayout}, that constraint is pushed into the parent layout so the
         *        component is correctly positioned within the parent grid.</li>
         *    <li><b>Layout manager</b> — if none of the three constraint strings is empty, a
         *        {@link MigLayout} is installed (or updated in-place if one is already present)
         *        using the stored general, column, and row constraints.</li>
         *    <li><b>Child constraints</b> — if the child-constraint tuple is non-empty, each
         *        stored {@link MigAddConstraint} is applied to the corresponding direct child of
         *        {@code component} (by position).  Only entries that differ from what the
         *        {@link MigLayout} already has are written, and
         *        {@link JComponent#revalidate()} is called exactly once at the end if anything
         *        changed.</li>
         *  </ol>
         *
         * @param component The component to install the {@link MigLayout} for.
         */
        @Override
        public void installFor( JComponent component ) {
            ComponentExtension<?> extension = ComponentExtension.from(component);
            StyleConf styleConf = extension.getStyle();
            if ( styleConf.layoutConstraint().isPresent() ) {
                // Phase 1: push this component's own layout constraint into its parent MigLayout:
                LayoutManager parentLayout = ( component.getParent() == null ? null : component.getParent().getLayout() );
                if ( parentLayout instanceof MigLayout) {
                    MigLayout migLayout = (MigLayout) parentLayout;
                    Object componentConstraints = styleConf.layoutConstraint().get();
                    Object currentComponentConstraints = migLayout.getComponentConstraints(component);
                    //     ^ can be a String or a CC object, we compare it to the desired constraint:
                    if ( !componentConstraints.equals(currentComponentConstraints) ) {
                        migLayout.setComponentConstraints(component, componentConstraints);
                        component.getParent().revalidate();
                    }
                }
            }
            // Phase 2: install / update the MigLayout on the component itself:
            final String layoutConstraints = _constr.toString();
            final String columnConstraints = _colConstr.toString();
            final String rowConstraints    = _rowConstr.toString();
            if ( !layoutConstraints.isEmpty() || !columnConstraints.isEmpty() || !rowConstraints.isEmpty() ) {
                LayoutManager currentLayout = component.getLayout();
                if ( !( currentLayout instanceof MigLayout ) ) {
                    component.setLayout(new MigLayout( layoutConstraints, columnConstraints, rowConstraints ));
                    component.revalidate();
                } else {
                    MigLayout migLayout = (MigLayout) currentLayout;
                    boolean layoutConstraintsChanged = !layoutConstraints.equals(migLayout.getLayoutConstraints());
                    boolean columnConstraintsChanged = !columnConstraints.equals(migLayout.getColumnConstraints());
                    boolean rowConstraintsChanged    = !rowConstraints.equals(migLayout.getRowConstraints());
                    if ( layoutConstraintsChanged || columnConstraintsChanged || rowConstraintsChanged ) {
                        migLayout.setLayoutConstraints(layoutConstraints);
                        migLayout.setColumnConstraints(columnConstraints);
                        migLayout.setRowConstraints(rowConstraints);
                        component.revalidate();
                    }
                }
            }
            // Phase 3: apply per-child component constraints:
            if ( _childConstraints.isNotEmpty() ) {
                LayoutManager currentLayout = component.getLayout();
                if ( currentLayout instanceof MigLayout ) {
                    MigLayout migLayout     = (MigLayout) currentLayout;
                    Component[] children    = component.getComponents();
                    boolean childrenChanged = false;
                    for ( Pair<Integer, MigAddConstraint> entry : _childConstraints ) {
                        int i = entry.first();
                        if ( i < 0 || i >= children.length ) continue;
                        Object desired  = entry.second().toConstraintForLayoutManager();
                        Object existing = migLayout.getComponentConstraints(children[i]);
                        if ( !desired.equals(existing) ) {
                            migLayout.setComponentConstraints(children[i], desired);
                            childrenChanged = true;
                        }
                    }
                    if ( childrenChanged )
                        component.revalidate();
                }
            }
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" +
                        "constr="           + _constr           + ", " +
                        "colConstr="        + _colConstr        + ", " +
                        "rowConstr="        + _rowConstr        + ", " +
                        "childConstraints=" + _childConstraints +
                    "]";
        }
    }

    /**
     *  An immutable {@link Layout} implementation that configures and installs a
     *  {@link ResponsiveGridFlowLayout} onto a component. It holds:
     *  <ul>
     *    <li><b>Alignment</b> ({@code align}) — the horizontal alignment of components
     *        within each row of the flow (left, center, right, leading, or trailing).</li>
     *    <li><b>Horizontal gap</b> ({@code hgap}) — the pixel spacing between components
     *        in the same row.</li>
     *    <li><b>Vertical gap</b> ({@code vgap}) — the pixel spacing between rows.</li>
     *    <li><b>Per-child {@link FlowCell} constraints</b> ({@code childConstraints}) — a
     *        sorted {@link Association} mapping child indices ({@link Integer}) to
     *        {@link FlowCell}s that are pushed onto the component's direct children.
     *        Unlike a positional tuple, the association is sparse: you only need to include
     *        entries for the children you actually want to configure.
     *        Each {@link FlowCell} carries a responsive span policy (see
     *        {@link swingtree.UI#AUTO_SPAN(Configurator)}) that the
     *        {@link ResponsiveGridFlowLayout} queries on every layout pass to determine
     *        how many grid columns a child should occupy for the current parent size.</li>
     *  </ul>
     *  <p>
     *  The child-constraint tuple is the key to building <em>fully reactive</em> responsive
     *  layouts.  With the static {@code UI.AUTO_SPAN()} approach every child's span policy
     *  is fixed at the time the component is added.  When child constraints need to change
     *  at runtime (e.g. the number of columns depends on application state), wrap a
     *  {@link ForFlowLayout} in a {@link sprouts.Var} and pass it to
     *  {@link swingtree.UIForAnySwing#withLayout(sprouts.Val)}:
     *  <pre>{@code
     *      import static swingtree.UI.*;
     *      // ...
     *      Var<Layout> layout = Var.of(
     *          Layout.flow( AUTO_SPAN(it -> it.small(12).medium(6)),
     *                       AUTO_SPAN(it -> it.small(12).medium(6)) )
     *      );
     *
     *      UI.panel()
     *        .withLayout(layout)
     *        .add( label("A") )
     *        .add( label("B") );
     *
     *      // Later: swap to a single full-width column for both children:
     *      layout.set( Layout.flow( AUTO_SPAN(12), AUTO_SPAN(12) ) );
     *  }</pre>
     *  Changing the {@code layout} property triggers a style re-evaluation, which calls
     *  {@link #installFor(JComponent)}, which re-pushes the updated {@link FlowCell}s as
     *  client properties onto the children so that the next layout pass picks them up.
     *  <p>
     *  Instances are created via the {@link Layout#flow()} family of factory methods and
     *  are further configured through the fluent {@code with*} wither methods.
     */
    @Immutable
    @SuppressWarnings("Immutable")
    final class ForFlowLayout implements Layout
    {
        private final UI.HorizontalAlignment          _align;
        private final int                             _horizontalGapSize;
        private final int                             _verticalGapSize;
        private final Association<Integer, FlowCell>  _childConstraints;

        ForFlowLayout( UI.HorizontalAlignment align, int hgap, int vgap ) {
            this( align, hgap, vgap, Association.betweenSorted(Integer.class, FlowCell.class) );
        }

        ForFlowLayout(
            UI.HorizontalAlignment              align,
            int                                 hgap,
            int                                 vgap,
            Association<Integer, FlowCell>      childConstraints
        ) {
            _align             = Objects.requireNonNull(align);
            _horizontalGapSize = hgap;
            _verticalGapSize   = vgap;
            _childConstraints  = Objects.requireNonNull(childConstraints);
        }

        // -- Alignment / gap withers --

        /**
         *  Returns a new {@link ForFlowLayout} with the given horizontal alignment and
         *  all other properties copied unchanged.
         *
         * @param align The new horizontal alignment for the flow.
         * @return A new {@link ForFlowLayout} with the updated alignment.
         */
        public ForFlowLayout withAlignment( UI.HorizontalAlignment align ) {
            return new ForFlowLayout( align, _horizontalGapSize, _verticalGapSize, _childConstraints );
        }

        /**
         *  Returns a new {@link ForFlowLayout} with the given horizontal gap size and
         *  all other properties copied unchanged.
         *
         * @param hgap The new horizontal gap between components, in pixels.
         * @return A new {@link ForFlowLayout} with the updated horizontal gap.
         */
        public ForFlowLayout withHorizontalGap( int hgap ) {
            return new ForFlowLayout( _align, hgap, _verticalGapSize, _childConstraints );
        }

        /**
         *  Returns a new {@link ForFlowLayout} with the given vertical gap size and
         *  all other properties copied unchanged.
         *
         * @param vgap The new vertical gap between component rows, in pixels.
         * @return A new {@link ForFlowLayout} with the updated vertical gap.
         */
        public ForFlowLayout withVerticalGap( int vgap ) {
            return new ForFlowLayout( _align, _horizontalGapSize, vgap, _childConstraints );
        }

        // -- Per-child FlowCell constraint withers --

        /**
         *  Returns a new {@link ForFlowLayout} whose per-child {@link FlowCell} constraints
         *  are replaced by the supplied sorted {@link Association}.
         *  <p>
         *  Keys are child indices ({@code 0} = first child, {@code 1} = second, etc.);
         *  the association is sparse, so you only need to include entries for children
         *  you actually want to configure.
         *  Children whose index has no entry keep whatever {@link FlowCell} they already have.
         *  An empty association means no constraints are stored in this layout object;
         *  any {@link AddConstraint} client properties previously pushed to children
         *  by an earlier {@code installFor} call remain on those children until explicitly
         *  overwritten.<br>
         *  The intended way of creating {@link FlowCell}s is by using {@link UI#AUTO_SPAN(Configurator)}!<br>
         *  An important edge case to consider when writing a responsive flow layout:<br>
         *  <b>
         *      If a {@link FlowCell} is passed to the responsive flow layout without
         *      any span policies defined, it will always default to spanning 12 cells at all parent size categories!
         *  </b>
         *
         * @param childConstraints The positional {@link FlowCell} constraints for the children.
         * @return A new {@link ForFlowLayout} with the updated child constraints.
         */
        public ForFlowLayout withChildConstraints( Association<Integer, FlowCell> childConstraints ) {
            return new ForFlowLayout( _align, _horizontalGapSize, _verticalGapSize,
                                      Objects.requireNonNull(childConstraints) );
        }

        /**
         *  Returns a new {@link ForFlowLayout} whose per-child {@link FlowCell} constraints
         *  are replaced by the supplied varargs array.
         *  <p>
         *  The constraints are mapped positionally to the component's children:
         *  the first argument applies to the first child, the second to the second, and so on.
         *  Passing an empty array stores no constraints in this layout object;
         *  any {@link AddConstraint} client properties previously pushed to children
         *  by an earlier {@code installFor} call remain on those children until explicitly
         *  overwritten.
         *  <p>
         *  {@link FlowCell} instances are most conveniently created via
         *  {@link swingtree.UI#AUTO_SPAN(Configurator)}:
         *  <pre>{@code
         *      import static swingtree.UI.*;
         *      // ...
         *      Layout.flow()
         *            .withChildConstraints(
         *                AUTO_SPAN( it -> it.small(12).medium(6) ),
         *                AUTO_SPAN( it -> it.small(12).medium(6) )
         *            )
         *  }</pre>
         *  The intended way of creating {@link FlowCell}s is through the {@link UI#AUTO_SPAN(Configurator)} factory method!<br>
         *  An important edge case to consider when writing a responsive flow layout:<br>
         *  <b>
         *      If a {@link FlowCell} is passed to the responsive flow layout without
         *      any size specific span policies defined, it will always default to spanning 12 cells at all parent size categories!
         *  </b>
         *
         * @param childConstraints The {@link FlowCell} constraints to apply to the component's
         *                         children in child-index order.
         * @return A new {@link ForFlowLayout} with the updated child constraints.
         */
        public ForFlowLayout withChildConstraints( FlowCell... childConstraints ) {
            Association<Integer, FlowCell> assoc = Association.betweenSorted(Integer.class, FlowCell.class);
            for ( int i = 0; i < childConstraints.length; i++ )
                assoc = assoc.put(i, childConstraints[i]);
            return withChildConstraints(assoc);
        }

        /**
         *  Returns a new {@link ForFlowLayout} with the {@link FlowCell} at the given child
         *  index replaced by the supplied value.  All other child constraints and all other
         *  properties are copied unchanged.
         *  The intended way of creating {@link FlowCell}s is through the {@link UI#AUTO_SPAN(Configurator)} factory method!<br>
         *  <p>
         *  Because the underlying storage is a sparse {@link Association}, no padding
         *  is needed: the constraint is stored at exactly {@code index}, regardless of
         *  whether lower indices have entries.<br>
         *  Another important edge case to consider when writing a responsive flow layout:<br>
         *  <b>
         *      If a {@link FlowCell} is passed to the responsive flow layout without
         *      any size specific span policies defined, it will always default to spanning 12 cells at all parent size categories!
         *  </b>
         *
         * @param index The zero-based index of the child whose constraint to update.
         * @param childConstraint The new {@link FlowCell} for the child at {@code index}.
         * @return A new {@link ForFlowLayout} with the updated child constraint at {@code index}.
         * @throws IndexOutOfBoundsException if {@code index} is negative.
         */
        public ForFlowLayout withChildConstraint( int index, FlowCell childConstraint ) {
            Objects.requireNonNull(childConstraint);
            if ( index < 0 )
                throw new IndexOutOfBoundsException("Child index must not be negative, but was: " + index);
            return withChildConstraints( _childConstraints.put(index, childConstraint) );
        }

        /**
         *  Returns a new {@link ForFlowLayout} with the {@link FlowCell} at the given child
         *  index replaced by one built from the supplied {@link Configurator} lambda.
         *  The lambda receives a {@link FlowCellConf} and returns the configured version,
         *  matching exactly the API of {@link swingtree.UI#AUTO_SPAN(Configurator)}:
         *  <pre>{@code
         *      Layout.flow()
         *            .withChildConstraint(0, it -> it.small(12).medium(6).large(4))
         *            .withChildConstraint(1, it -> it.small(12).medium(6).large(8))
         *  }</pre>
         *  The intended way of creating {@link FlowCell}s is through the {@link UI#AUTO_SPAN(Configurator)} factory method!<br>
         *  An important edge case to consider when writing a responsive flow layout:<br>
         *  <b>
         *      If a {@link FlowCell} is passed to the responsive flow layout without
         *      any size specific span policies defined, it will always default to spanning 12 cells at all parent size categories!
         *  </b>
         *
         * @param index The zero-based index of the child whose constraint to update.
         * @param cellConfig A {@link Configurator} that configures the {@link FlowCellConf}
         *                   for the child's responsive span policy.
         * @return A new {@link ForFlowLayout} with the updated child constraint at {@code index}.
         * @throws IndexOutOfBoundsException if {@code index} is negative.
         */
        public ForFlowLayout withChildConstraint( int index, Configurator<FlowCellConf> cellConfig ) {
            return withChildConstraint( index, new FlowCell(Objects.requireNonNull(cellConfig)) );
        }

        /**
         *  Returns a new {@link ForFlowLayout} with the supplied {@link FlowCell} appended
         *  to the end of the existing child-constraint tuple.
         *  This is a convenient alternative to {@link #withChildConstraints(FlowCell...)}
         *  when building up constraints one at a time:
         *  <pre>{@code
         *      import static swingtree.UI.*;
         *      // ...
         *      Layout.flow()
         *            .withAddedChildConstraint( AUTO_SPAN(it -> it.small(12).medium(6)) )
         *            .withAddedChildConstraint( AUTO_SPAN(it -> it.small(12).medium(6)) )
         *  }</pre>
         *  The intended way of creating {@link FlowCell}s is through the {@link UI#AUTO_SPAN(Configurator)} factory method!<br>
         *  An important edge case to consider when writing a responsive flow layout:<br>
         *  <b>
         *      If a {@link FlowCell} is passed to the responsive flow layout without
         *      any size specific span policies defined, it will always default to spanning 12 cells at all parent size categories!
         *  </b>
         *
         * @param childConstraint The {@link FlowCell} to append as the next child constraint.
         * @return A new {@link ForFlowLayout} with the constraint appended.
         */
        public ForFlowLayout withAddedChildConstraint( FlowCell childConstraint ) {
            Objects.requireNonNull(childConstraint);
            int nextIndex = 0;
            for ( Integer key : _childConstraints.keySet() )
                nextIndex = key + 1;
            return withChildConstraints( _childConstraints.put(nextIndex, childConstraint) );
        }

        /**
         *  Returns a new {@link ForFlowLayout} with a {@link FlowCell} built from the
         *  supplied {@link Configurator} lambda appended to the end of the existing
         *  child-constraint tuple.
         *  The lambda receives a {@link FlowCellConf} and returns the configured version,
         *  matching exactly the API of {@link swingtree.UI#AUTO_SPAN(Configurator)}:
         *  <pre>{@code
         *      Layout.flow()
         *            .withAddedChildConstraint( it -> it.small(12).medium(6) )
         *            .withAddedChildConstraint( it -> it.small(12).medium(6) )
         *  }</pre>
         *  The intended way of creating {@link FlowCell}s is through the {@link UI#AUTO_SPAN(Configurator)} factory method!<br>
         *  An important edge case to consider when writing a responsive flow layout:<br>
         *  <b>
         *      If a {@link FlowCell} is passed to the responsive flow layout without
         *      any size specific span policies defined, it will always default to spanning 12 cells at all parent size categories!
         *  </b>
         *
         * @param cellConfig A {@link Configurator} that configures the {@link FlowCellConf}
         *                   for the appended child's responsive span policy.
         * @return A new {@link ForFlowLayout} with the constraint appended.
         */
        public ForFlowLayout withAddedChildConstraint( Configurator<FlowCellConf> cellConfig ) {
            return withAddedChildConstraint( new FlowCell(Objects.requireNonNull(cellConfig)) );
        }

        // -- Object contract --

        @Override public int hashCode() {
            return Objects.hash( _align, _horizontalGapSize, _verticalGapSize, _childConstraints );
        }

        @Override
        public boolean equals( @Nullable Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForFlowLayout other = (ForFlowLayout) o;
            return _align              == other._align              &&
                   _horizontalGapSize  == other._horizontalGapSize  &&
                   _verticalGapSize    == other._verticalGapSize    &&
                   _childConstraints.equals(other._childConstraints);
        }

        // -- Layout installation --

        /**
         *  Installs a {@link ResponsiveGridFlowLayout} onto the supplied component and
         *  applies all constraints stored in this configuration.
         *  <p>
         *  The installation proceeds in two phases:
         *  <ol>
         *    <li><b>Layout manager</b> — if no {@link ResponsiveGridFlowLayout} is currently
         *        installed, a new one is created with the stored alignment and gap settings.
         *        If one is already installed, only the properties that have changed are
         *        updated in-place and {@link JComponent#revalidate()} is called.</li>
         *    <li><b>Child constraints</b> — if the child-constraint tuple is non-empty,
         *        each stored {@link FlowCell} is written as a
         *        {@link AddConstraint} client property onto the corresponding direct child
         *        (using the same key that {@link ResponsiveGridFlowLayout#addLayoutComponent}
         *        uses, so the layout manager picks them up on the next layout pass).
         *        Only entries that differ from the currently stored value are written, and
         *        {@link JComponent#revalidate()} is called exactly once at the end if
         *        anything changed.</li>
         *  </ol>
         *
         * @param component The component to install the {@link ResponsiveGridFlowLayout} for.
         */
        @Override
        public void installFor( JComponent component ) {
            // Phase 1: install / update the ResponsiveGridFlowLayout:
            LayoutManager currentLayout = component.getLayout();
            if ( !( currentLayout instanceof ResponsiveGridFlowLayout ) ) {
                component.setLayout(new ResponsiveGridFlowLayout(_align, _horizontalGapSize, _verticalGapSize));
                component.revalidate();
            } else {
                ResponsiveGridFlowLayout flowLayout = (ResponsiveGridFlowLayout) currentLayout;
                boolean alignmentChanged     = _align             != flowLayout.getAlignment();
                boolean horizontalGapChanged = _horizontalGapSize != flowLayout.horizontalGapSize();
                boolean verticalGapChanged   = _verticalGapSize   != flowLayout.verticalGapSize();
                if ( alignmentChanged || horizontalGapChanged || verticalGapChanged ) {
                    flowLayout.setAlignment(_align);
                    flowLayout.setHorizontalGapSize(_horizontalGapSize);
                    flowLayout.setVerticalGapSize(_verticalGapSize);
                    component.revalidate();
                }
            }
            // Phase 2: push per-child FlowCell constraints as client properties:
            if ( _childConstraints.isNotEmpty() ) {
                Component[] children  = component.getComponents();
                boolean changed       = false;
                for ( Pair<Integer, FlowCell> entry : _childConstraints ) {
                    int i = entry.first();
                    if ( i < 0 || i >= children.length ) continue;
                    if ( !( children[i] instanceof JComponent ) )
                        continue;
                    JComponent child  = (JComponent) children[i];
                    FlowCell desired  = entry.second();
                    Object existing   = child.getClientProperty(AddConstraint.class);
                    if ( !desired.equals(existing) ) {
                        child.putClientProperty(AddConstraint.class, desired);
                        changed = true;
                    }
                }
                if ( changed )
                    component.revalidate();
            }
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" +
                        "align="            + _align             + ", " +
                        "hgap="             + _horizontalGapSize + ", " +
                        "vgap="             + _verticalGapSize   + ", " +
                        "childConstraints=" + _childConstraints  +
                    "]";
        }
    }

    /**
     *  The {@link BorderLayoutInstaller} layout is a layout that represents
     *  a {@link BorderLayout} layout configuration for a component,
     *  which consists of the horizontal gap and vertical gap. <br>
     *  Whenever this layout configuration changes,
     *  it will create and re-install a new {@link BorderLayout} onto the component
     *  based on the new configuration.
     */
    @Immutable
    final class BorderLayoutInstaller implements Layout
    {
        private final int _hgap;
        private final int _vgap;

        BorderLayoutInstaller( int hgap, int vgap ) {
            _hgap = hgap;
            _vgap = vgap;
        }

        @Override public int hashCode() { return Objects.hash(_hgap, _vgap); }

        @Override
        public boolean equals( @Nullable Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            BorderLayoutInstaller other = (BorderLayoutInstaller) o;
            return _hgap == other._hgap && _vgap == other._vgap;
        }

        /**
         *  Installs a {@link BorderLayout} onto the supplied component using the horizontal
         *  and vertical gap sizes stored in this configuration. If a {@link BorderLayout}
         *  is already installed, only the gap values that have changed are updated and
         *  {@link JComponent#revalidate()} is called to trigger a layout refresh.
         *
         * @param component The component to install the {@link BorderLayout} for.
         */
        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !(currentLayout instanceof BorderLayout) ) {
                // We need to replace the current layout with a BorderLayout:
                BorderLayout newLayout = new BorderLayout(_hgap, _vgap);
                component.setLayout(newLayout);
                component.revalidate();
                return;
            }
            BorderLayout borderLayout = (BorderLayout) currentLayout;
            int horizontalGap = _hgap;
            int verticalGap   = _vgap;

            boolean horizontalGapChanged = horizontalGap != borderLayout.getHgap();
            boolean verticalGapChanged   = verticalGap   != borderLayout.getVgap();

            if ( horizontalGapChanged || verticalGapChanged ) {
                borderLayout.setHgap(horizontalGap);
                borderLayout.setVgap(verticalGap);
                component.revalidate();
            }
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" +
                        "hgap=" + _hgap + ", " +
                        "vgap=" + _vgap +
                    "]";
        }
    }

    /**
     *  The {@link GridLayoutInstaller} layout is a layout that represents
     *  a {@link GridLayout} layout configuration for a component,
     *  which consists of the number of rows, number of columns, horizontal gap and vertical gap. <br>
     *  Whenever this layout configuration changes,
     *  it will create and re-install a new {@link GridLayout} onto the component
     *  based on the new configuration.
     */
    @Immutable
    final class GridLayoutInstaller implements Layout
    {
        private final int _rows;
        private final int _cols;
        private final int _hgap;
        private final int _vgap;

        GridLayoutInstaller( int rows, int cols, int hgap, int vgap ) {
            _rows = rows;
            _cols = cols;
            _hgap = hgap;
            _vgap = vgap;
        }

        @Override public int hashCode() { return Objects.hash(_rows, _cols, _hgap, _vgap); }

        @Override
        public boolean equals(Object o) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            GridLayoutInstaller other = (GridLayoutInstaller) o;
            return _rows == other._rows && _cols == other._cols && _hgap == other._hgap && _vgap == other._vgap;
        }

        /**
         *  Installs a {@link GridLayout} onto the supplied component using the row count,
         *  column count, and gap sizes stored in this configuration. If a {@link GridLayout}
         *  is already installed, only the properties that have changed are updated and
         *  {@link JComponent#revalidate()} is called to trigger a layout refresh.
         *
         * @param component The component to install the {@link GridLayout} for.
         */
        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !(currentLayout instanceof GridLayout) ) {
                // We need to replace the current layout with a GridLayout:
                GridLayout newLayout = new GridLayout(_rows, _cols, _hgap, _vgap);
                component.setLayout(newLayout);
                component.revalidate();
                return;
            }
            GridLayout gridLayout = (GridLayout) currentLayout;
            int rows          = _rows;
            int cols          = _cols;
            int horizontalGap = _hgap;
            int verticalGap   = _vgap;

            boolean rowsChanged = rows != gridLayout.getRows();
            boolean colsChanged = cols != gridLayout.getColumns();
            boolean horizontalGapChanged = horizontalGap != gridLayout.getHgap();
            boolean verticalGapChanged   = verticalGap   != gridLayout.getVgap();

            if ( rowsChanged || colsChanged || horizontalGapChanged || verticalGapChanged ) {
                gridLayout.setRows(rows);
                gridLayout.setColumns(cols);
                gridLayout.setHgap(horizontalGap);
                gridLayout.setVgap(verticalGap);
                component.revalidate();
            }
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" +
                        "rows=" + _rows + ", " +
                        "cols=" + _cols + ", " +
                        "hgap=" + _hgap + ", " +
                        "vgap=" + _vgap +
                    "]";
        }
    }

    /**
     *  The {@link ForBoxLayout} layout is a layout that represents
     *  a {@link BoxLayout} layout configuration for a component,
     *  which consists of the axis. <br>
     *  The axis determines whether the layout will be a horizontal or vertical
     *  {@link BoxLayout}. <br>
     *  Whenever this layout configuration object changes,
     *  it will create and re-install a new {@link BoxLayout} onto the component
     *  based on the new configuration.
     */
    @Immutable
    final class ForBoxLayout implements Layout
    {
        private final int _axis;

        ForBoxLayout( int axis ) { _axis = axis; }

        @Override public int hashCode() { return Objects.hash(_axis); }

        @Override
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForBoxLayout other = (ForBoxLayout) o;
            return _axis == other._axis;
        }

        /**
         *  Installs a {@link BoxLayout} onto the supplied component using the axis
         *  stored in this configuration. If a {@link BoxLayout} with a different axis
         *  is already installed, a new {@link BoxLayout} is created and installed
         *  (since {@link BoxLayout} does not support changing the axis after construction)
         *  and {@link JComponent#revalidate()} is called to trigger a layout refresh.
         *
         * @param component The component to install the {@link BoxLayout} for.
         */
        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !( currentLayout instanceof BoxLayout ) ) {
                // We need to replace the current layout with a BoxLayout:
                BoxLayout newLayout = new BoxLayout( component, _axis);
                component.setLayout(newLayout);
                component.revalidate();
                return;
            }
            BoxLayout boxLayout = (BoxLayout) currentLayout;
            int axis = _axis;

            boolean axisChanged = axis != boxLayout.getAxis();

            if ( axisChanged ) {
                // The BoxLayout does not have a 'setAxis' method!
                // Instead, we need to create and install a new BoxLayout with the new axis:
                BoxLayout newLayout = new BoxLayout( component, axis );
                component.setLayout(newLayout);
                component.revalidate();
            }
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" +
                        "axis=" + _axis +
                    "]";
        }
    }

}
