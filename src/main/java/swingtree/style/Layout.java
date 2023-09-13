package swingtree.style;

import net.miginfocom.swing.MigLayout;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;
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
public interface Layout
{
    /**
     * @return A hash code value for this layout.
     */
    int hashCode();

    /**
     * @param o The object to compare this layout to.
     * @return {@code true} if the supplied object is a layout
     *         that is equal to this layout, {@code false} otherwise.
     */
    boolean equals( Object o );

    /**
     * Installs this layout for the supplied component.
     *
     * @param component The component to install this layout for.
     */
    void installFor( JComponent component );

    /**
     * @return A layout that does nothing, i.e. it does not install any layout for a component.
     */
    static Layout unspecific() { return StyleUtility.UNSPECIFIC_LAYOUT_CONSTANT; }

    /**
     * @return A layout that removes any existing layout from a component.
     */
    static Layout none() { return StyleUtility.NONE_LAYOUT_CONSTANT; }

    /**
     * @param constr The layout constraints for the layout.
     * @param rowConstr The row constraints for the layout.
     * @param colConstr The column constraints for the layout.
     * @return A layout that uses the MigLayout.
     */
    static Layout mig(
        String constr,
        String colConstr,
        String rowConstr
    ) {
        return new ForMigLayout( constr, colConstr, rowConstr );
    }

    /**
     * @param constr The layout constraints for the layout.
     * @param rowConstr The row constraints for the layout.
     * @return A layout that uses the MigLayout.
     */
    static Layout mig(
        String constr,
        String rowConstr
    ) {
        return new ForMigLayout( constr, "", rowConstr );
    }

    /**
     * @param constr The layout constraints for the layout.
     * @return A layout that uses the MigLayout.
     */
    static Layout mig( String constr ) {
        return new ForMigLayout( constr, "", "" );
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

        @Override
        public void installFor( JComponent component ) {
            // Contrary to the 'Unspecific' layout, this layout
            // will remove any existing layout from the component:
            component.setLayout(null);
        }
    }

    /**
     *  The {@link ForMigLayout} layout is a layout that represents
     *  a {@link MigLayout} layout configuration for a component. <br>
     *  Whenever this layout configuration changes,
     *  it will create and re-install a new {@link MigLayout} onto the component
     *  based on the new configuration,
     *  which are the constraints, column constraints and row constraints.
     */
    final class ForMigLayout implements Layout
    {
        private final String _constr;
        private final String _colConstr;
        private final String _rowConstr;


        ForMigLayout( String constr, String colConstr, String rowConstr ) {
            _constr    = Objects.requireNonNull(constr);
            _colConstr = Objects.requireNonNull(colConstr);
            _rowConstr = Objects.requireNonNull(rowConstr);
        }

        ForMigLayout withConstraint( String constr ) { return new ForMigLayout( constr, _colConstr, _rowConstr ); }

        ForMigLayout withRowConstraint( String rowConstr ) { return new ForMigLayout( _constr, _colConstr, rowConstr ); }

        ForMigLayout withColumnConstraint( String colConstr ) { return new ForMigLayout( _constr, colConstr, _rowConstr ); }

        ForMigLayout withComponentConstraint( String componentConstr ) { return new ForMigLayout( _constr, _colConstr, _rowConstr ); }

        @Override public int hashCode() { return Objects.hash(_constr, _rowConstr, _colConstr); }

        @Override
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForMigLayout other = (ForMigLayout) o;
            return _constr.equals( other._constr) &&
                   _rowConstr.equals( other._rowConstr) &&
                   _colConstr.equals( other._colConstr);
        }

        @Override
        public void installFor( JComponent component ) {
            ComponentExtension<?> extension = ComponentExtension.from(component);
            Style style = extension.getStyle();
            if ( style.layout().constraint().isPresent() ) {
                // We ensure that the parent layout has the correct component constraints for the component:
                LayoutManager parentLayout = ( component.getParent() == null ? null : component.getParent().getLayout() );
                if ( parentLayout instanceof MigLayout) {
                    MigLayout migLayout = (MigLayout) parentLayout;
                    Object componentConstraints = style.layout().constraint().get();
                    Object currentComponentConstraints = migLayout.getComponentConstraints(component);
                    //     ^ can be a String or a CC object, we want to compare it to the new constraints:
                    if ( !componentConstraints.equals(currentComponentConstraints) ) {
                        migLayout.setComponentConstraints(component, componentConstraints);
                        component.getParent().revalidate();
                    }
                }
            }
            if ( !_constr.isEmpty() || !_colConstr.isEmpty() || !_rowConstr.isEmpty() ) {
                // We ensure that the parent layout has the correct layout constraints for the component:
                LayoutManager currentLayout = component.getLayout();
                if ( !( currentLayout instanceof MigLayout ) ) {
                    // We need to replace the current layout with a MigLayout:
                    MigLayout newLayout = new MigLayout( _constr, _colConstr, _rowConstr );
                    component.setLayout(newLayout);
                    return;
                }
                MigLayout migLayout = (MigLayout) currentLayout;
                String layoutConstraints = _constr;
                String columnConstraints = _colConstr;
                String rowConstraints    = _rowConstr;

                Object currentLayoutConstraints = migLayout.getLayoutConstraints();
                Object currentColumnConstraints = migLayout.getColumnConstraints();
                Object currentRowConstraints    = migLayout.getRowConstraints();

                boolean layoutConstraintsChanged = !layoutConstraints.equals(currentLayoutConstraints);
                boolean columnConstraintsChanged = !columnConstraints.equals(currentColumnConstraints);
                boolean rowConstraintsChanged    = !rowConstraints.equals(currentRowConstraints);

                if ( layoutConstraintsChanged || columnConstraintsChanged || rowConstraintsChanged ) {
                    migLayout.setLayoutConstraints(layoutConstraints);
                    migLayout.setColumnConstraints(columnConstraints);
                    migLayout.setRowConstraints(rowConstraints);
                    component.revalidate();
                }
            }
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
    final class ForFlowLayout implements Layout
    {
        private final int _align;
        private final int _hgap;
        private final int _vgap;

        ForFlowLayout( UI.HorizontalAlignment align, int hgap, int vgap ) {
            _align = align.forFlowLayout();
            _hgap  = hgap;
            _vgap  = vgap;
        }

        @Override public int hashCode() { return Objects.hash( _align, _hgap, _vgap  ); }

        @Override
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForFlowLayout other = (ForFlowLayout) o;
            return _align == other._align && _hgap == other._hgap && _vgap == other._vgap;
        }

        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !( currentLayout instanceof FlowLayout ) ) {
                // We need to replace the current layout with a FlowLayout:
                FlowLayout newLayout = new FlowLayout(_align, _hgap, _vgap);
                component.setLayout(newLayout);
                return;
            }
            FlowLayout flowLayout = (FlowLayout) currentLayout;
            int alignment     = _align;
            int horizontalGap = _hgap;
            int verticalGap   = _vgap;

            boolean alignmentChanged     = alignment != flowLayout.getAlignment();
            boolean horizontalGapChanged = horizontalGap != flowLayout.getHgap();
            boolean verticalGapChanged   = verticalGap   != flowLayout.getVgap();

            if ( alignmentChanged || horizontalGapChanged || verticalGapChanged ) {
                flowLayout.setAlignment(alignment);
                flowLayout.setHgap(horizontalGap);
                flowLayout.setVgap(verticalGap);
                component.revalidate();
            }
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
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            BorderLayoutInstaller other = (BorderLayoutInstaller) o;
            return _hgap == other._hgap && _vgap == other._vgap;
        }

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
    }

    /**
     *  The {@link GridLayoutInstaller} layout is a layout that represents
     *  a {@link GridLayout} layout configuration for a component,
     *  which consists of the number of rows, number of columns, horizontal gap and vertical gap. <br>
     *  Whenever this layout configuration changes,
     *  it will create and re-install a new {@link GridLayout} onto the component
     *  based on the new configuration.
     */
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
    }

}
