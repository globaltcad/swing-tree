package swingtree.api;

import com.google.errorprone.annotations.Immutable;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;
import swingtree.UI;
import swingtree.layout.LayoutConstraint;
import swingtree.layout.MigAddConstraint;
import swingtree.layout.ResponsiveGridFlowLayout;
import swingtree.style.ComponentExtension;
import swingtree.style.ComponentStyleDelegate;
import swingtree.style.StyleConf;

import sprouts.Tuple;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
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
     *  If you don't want to assign any layout to a component style, but you also
     *  don't want to pass null to the {@link ComponentStyleDelegate#layout(Layout)}
     *  method, you can use the no-op instance returned by this method.
     *
     * @return A layout that removes any existing layout from a component.
     */
    static Layout none() { return Constants.NONE_LAYOUT_CONSTANT; }

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
     * A factory method for creating a layout that installs the {@link FlowLayout}
     * onto a component based on the supplied parameters.
     *
     * @param align The alignment for the layout, which has to be one of <ul>
     *               <li>{@link UI.HorizontalAlignment#LEFT}</li>
     *               <li>{@link UI.HorizontalAlignment#CENTER}</li>
     *               <li>{@link UI.HorizontalAlignment#RIGHT}</li>
     *               <li>{@link UI.HorizontalAlignment#LEADING}</li>
     *               <li>{@link UI.HorizontalAlignment#TRAILING}</li>
     *              </ul>
     *
     * @param hgap The horizontal gap for the layout.
     * @param vgap The vertical gap for the layout.
     * @return A layout that installs the {@link FlowLayout} onto a component.
     */
    static Layout flow( UI.HorizontalAlignment align, int hgap, int vgap ) {
        return new ForFlowLayout( align, hgap, vgap );
    }

    /**
     * A factory method for creating a layout that installs the {@link FlowLayout}
     * onto a component based on the supplied parameters.
     *
     * @param align The alignment for the layout, which has to be one of <ul>
     *               <li>{@link UI.HorizontalAlignment#LEFT}</li>
     *               <li>{@link UI.HorizontalAlignment#CENTER}</li>
     *               <li>{@link UI.HorizontalAlignment#RIGHT}</li>
     *               <li>{@link UI.HorizontalAlignment#LEADING}</li>
     *               <li>{@link UI.HorizontalAlignment#TRAILING}</li>
     *              </ul>
     *
     * @return A layout that installs the {@link FlowLayout} onto a component.
     */
    static Layout flow( UI.HorizontalAlignment align ) {
        return new ForFlowLayout( align, 5, 5 );
    }

    /**
     *  Creates a layout that installs the {@link FlowLayout}
     *  with a default alignment of {@link UI.HorizontalAlignment#CENTER}
     *  and a default gap of 5 pixels.
     *
     * @return A layout that installs the {@link FlowLayout} onto a component.
     */
    static Layout flow() {
        return new ForFlowLayout( UI.HorizontalAlignment.CENTER, 5, 5 );
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
     *  The {@link None} layout is a layout that represents the absence
     *  of a {@link LayoutManager} for a component.
     *  This means that whatever layout is currently installed for a component
     *  will be removed, and {@code null} will be set as the layout for the component.
     *  <p>
     *  Note that this is different from the {@link Unspecific} layout,
     *  which does not represent the absence of a {@link LayoutManager}
     *  for a component, but rather the absence of it being specified.
     */
    @Immutable
    final class None implements Layout
    {
        None(){}

        @Override public int hashCode() { return 0; }

        @Override
        public boolean equals(Object o) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            return o.getClass() == getClass();
        }

        @Override public String toString() { return getClass().getSimpleName() + "[]"; }

        /**
         *  Removes any existing {@link LayoutManager} from the supplied component
         *  by setting it to {@code null}, effectively leaving the component without
         *  a layout manager.
         *
         * @param component The component whose layout manager will be removed.
         */
        @Override
        public void installFor( JComponent component ) {
            // Contrary to the 'Unspecific' layout, this layout
            // will remove any existing layout from the component:
            component.setLayout(null);
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
     *        positional {@link Tuple} of {@link MigAddConstraint}s that are applied to
     *        the component's direct children: the first entry goes to the first child,
     *        the second to the second child, and so on.
     *        Children without a matching entry keep whatever constraint the
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
    final class ForMigLayout implements Layout
    {
        private final LayoutConstraint          _constr;
        private final LayoutConstraint          _colConstr;
        private final LayoutConstraint          _rowConstr;
        private final Tuple<MigAddConstraint>   _childConstraints;

        ForMigLayout( LayoutConstraint constr, LayoutConstraint colConstr, LayoutConstraint rowConstr ) {
            this( constr, colConstr, rowConstr, Tuple.of(MigAddConstraint.class) );
        }

        ForMigLayout(
            LayoutConstraint        constr,
            LayoutConstraint        colConstr,
            LayoutConstraint        rowConstr,
            Tuple<MigAddConstraint> childConstraints
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
         *  are replaced by the supplied {@link Tuple}.
         *  <p>
         *  The {@code childConstraints} tuple maps positionally to the component's children:
         *  index&nbsp;0 maps to the first child, index&nbsp;1 to the second, and so on.
         *  Children at indices beyond the tuple size are left untouched.
         *  An empty tuple clears all previously stored per-child constraints.
         *
         * @param childConstraints The positional {@link MigAddConstraint}s for the children.
         * @return A new {@link ForMigLayout} with the updated child constraints.
         */
        public ForMigLayout withChildConstraints( Tuple<MigAddConstraint> childConstraints ) {
            return new ForMigLayout( _constr, _colConstr, _rowConstr, Objects.requireNonNull(childConstraints) );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance whose per-child component constraints
         *  are replaced by the supplied varargs array of {@link MigAddConstraint}s.
         *  <p>
         *  The constraints are mapped positionally to the component's children:
         *  the first argument applies to the first child, the second to the second, and so on.
         *  Children at indices beyond the supplied array length are left untouched.
         *  Passing an empty array clears all previously stored per-child constraints.
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
            return withChildConstraints( Tuple.of(MigAddConstraint.class, childConstraints) );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the {@link MigAddConstraint} at the
         *  given child index replaced by the supplied value.
         *  All other child constraints and all other properties are copied unchanged.
         *  <p>
         *  If the supplied {@code index} is beyond the current size of the child-constraint
         *  tuple, the tuple is padded with empty {@link MigAddConstraint}s
         *  ({@code MigAddConstraint.of()}) up to and including that index.
         *
         * @param index The zero-based index of the child whose constraint to update.
         *              The first child has index&nbsp;0.
         * @param childConstraint The new {@link MigAddConstraint} for the child at {@code index}.
         * @return A new {@link ForMigLayout} with the updated child constraint at {@code index}.
         * @throws IndexOutOfBoundsException if {@code index} is negative.
         */
        public ForMigLayout withChildConstraint( int index, MigAddConstraint childConstraint ) {
            Objects.requireNonNull(childConstraint);
            Tuple<MigAddConstraint> padded = _childConstraints;
            // Pad with empty constraints if needed so we can set at 'index':
            while ( padded.size() <= index )
                padded = padded.add( MigAddConstraint.of() );
            return withChildConstraints( padded.setAt(index, childConstraint) );
        }

        /**
         *  Returns a new {@link ForMigLayout} instance with the {@link MigAddConstraint} at the
         *  given child index replaced by a constraint wrapping the supplied string.
         *  The string is converted via {@link MigAddConstraint#of(String...)} and then forwarded
         *  to {@link #withChildConstraint(int, MigAddConstraint)}.
         *  <p>
         *  If the supplied {@code index} is beyond the current size of the child-constraint
         *  tuple, the tuple is padded with empty {@link MigAddConstraint}s up to that index.
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
            return withChildConstraints( _childConstraints.add(Objects.requireNonNull(childConstraint)) );
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
                    int count               = Math.min(children.length, _childConstraints.size());
                    boolean childrenChanged = false;
                    for ( int i = 0; i < count; i++ ) {
                        Object desired  = _childConstraints.get(i).toConstraintForLayoutManager();
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
     *  The {@link ForFlowLayout} layout is a layout that represents
     *  a {@link FlowLayout} layout configuration for a component. <br>
     *  Whenever this layout configuration changes,
     *  it will create and re-install a new {@link FlowLayout} onto the component
     *  based on the new configuration,
     *  which are the alignment, horizontal gap and vertical gap.
     */
    @Immutable
    final class ForFlowLayout implements Layout
    {
        private final UI.HorizontalAlignment _align;
        private final int                    _horizontalGapSize;
        private final int                    _verticalGapSize;

        ForFlowLayout( UI.HorizontalAlignment align, int hgap, int vgap ) {
            _align             = Objects.requireNonNull(align);
            _horizontalGapSize = hgap;
            _verticalGapSize   = vgap;
        }

        @Override public int hashCode() { return Objects.hash( _align, _horizontalGapSize, _verticalGapSize); }

        @Override
        public boolean equals( @Nullable Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForFlowLayout other = (ForFlowLayout) o;
            return _align == other._align && _horizontalGapSize == other._horizontalGapSize && _verticalGapSize == other._verticalGapSize;
        }

        /**
         *  Installs a {@link FlowLayout} (backed by {@link ResponsiveGridFlowLayout})
         *  onto the supplied component using the alignment and gap settings
         *  stored in this configuration. If a compatible layout is already installed,
         *  only the properties that have changed are updated and
         *  {@link JComponent#revalidate()} is called to trigger a layout refresh.
         *
         * @param component The component to install the {@link FlowLayout} for.
         */
        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !( currentLayout instanceof ResponsiveGridFlowLayout) ) {
                // We need to replace the current layout with a FlowLayout:
                ResponsiveGridFlowLayout newLayout = new ResponsiveGridFlowLayout(_align, _horizontalGapSize, _verticalGapSize);
                component.setLayout(newLayout);
                return;
            }
            ResponsiveGridFlowLayout flowLayout = (ResponsiveGridFlowLayout) currentLayout;
            UI.HorizontalAlignment alignment = _align;
            int horizontalGap                = _horizontalGapSize;
            int verticalGap                  = _verticalGapSize;

            boolean alignmentChanged     = alignment != flowLayout.getAlignment();
            boolean horizontalGapChanged = horizontalGap != flowLayout.horizontalGapSize();
            boolean verticalGapChanged   = verticalGap   != flowLayout.verticalGapSize();

            if ( alignmentChanged || horizontalGapChanged || verticalGapChanged ) {
                flowLayout.setAlignment(alignment);
                flowLayout.setHorizontalGapSize(horizontalGap);
                flowLayout.setVerticalGapSize(verticalGap);
                component.revalidate();
            }
        }

        @Override public String toString() {
            return getClass().getSimpleName() + "[" +
                        "align=" + _align + ", " +
                        "hgap=" + _horizontalGapSize + ", " +
                        "vgap=" + _verticalGapSize +
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
