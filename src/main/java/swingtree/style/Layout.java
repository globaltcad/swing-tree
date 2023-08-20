package swingtree.style;

import net.miginfocom.swing.MigLayout;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 *    This interface represents a layout for a component
 *    whose implementations are expected to be immutable
 *    value objects holding necessary information for
 *    installing the layout for a component.
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

    String toString();

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
        String rowConstr,
        String colConstr
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
    static Layout border(int horizontalGap, int verticalGap ) {
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


    class Unspecific implements Layout
    {
        Unspecific(){}

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
            // Do nothing.
        }
    }

    class None implements Layout
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

    class ForMigLayout implements Layout
    {
        private final String _constr;
        private final String _colConstr;
        private final String _rowConstr;


        ForMigLayout( String constr, String colConstr, String rowConstr ) {
            _constr          = Objects.requireNonNull(constr);
            _colConstr       = Objects.requireNonNull(colConstr);
            _rowConstr       = Objects.requireNonNull(rowConstr);
        }

        ForMigLayout withConstraint( String constr ) {
            return new ForMigLayout( constr, _colConstr, _rowConstr );
        }

        ForMigLayout withRowConstraint( String rowConstr ) {
            return new ForMigLayout( _constr, _colConstr, rowConstr );
        }

        ForMigLayout withColumnConstraint( String colConstr ) {
            return new ForMigLayout( _constr, colConstr, _rowConstr );
        }

        ForMigLayout withComponentConstraint( String componentConstr ) {
            return new ForMigLayout( _constr, _colConstr, _rowConstr );
        }


        @Override public int hashCode() { return Objects.hash(_constr, _rowConstr, _colConstr); }

        @Override
        public boolean equals(Object o) {
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
            Style style = extension.getCurrentStylePainter().getStyle();
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
                if ( !(currentLayout instanceof MigLayout) ) {
                    // We need to replace the current layout with a MigLayout:
                    MigLayout newLayout = new MigLayout(_constr, _colConstr, _rowConstr);
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

    class ForFlowLayout implements Layout
    {
        private final int align;
        private final int hgap;
        private final int vgap;

        ForFlowLayout( UI.HorizontalAlignment align, int hgap, int vgap ) {
            this.align = align.forFlowLayout();
            this.hgap  = hgap;
            this.vgap  = vgap;
        }

        @Override public int hashCode() { return Objects.hash( align, hgap, vgap ); }

        @Override
        public boolean equals( Object o ) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForFlowLayout other = (ForFlowLayout) o;
            return align == other.align && hgap == other.hgap && vgap == other.vgap;
        }

        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !(currentLayout instanceof FlowLayout) ) {
                // We need to replace the current layout with a FlowLayout:
                FlowLayout newLayout = new FlowLayout( align, hgap, vgap );
                component.setLayout(newLayout);
                return;
            }
            FlowLayout flowLayout = (FlowLayout) currentLayout;
            int alignment = align;
            int horizontalGap = hgap;
            int verticalGap   = vgap;

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

    class BorderLayoutInstaller implements Layout
    {
        private final int hgap;
        private final int vgap;

        BorderLayoutInstaller( int hgap, int vgap ) {
            this.hgap = hgap;
            this.vgap = vgap;
        }

        @Override public int hashCode() { return Objects.hash( hgap, vgap ); }

        @Override
        public boolean equals(Object o) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            BorderLayoutInstaller other = (BorderLayoutInstaller) o;
            return hgap == other.hgap && vgap == other.vgap;
        }

        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !(currentLayout instanceof BorderLayout) ) {
                // We need to replace the current layout with a BorderLayout:
                BorderLayout newLayout = new BorderLayout( hgap, vgap );
                component.setLayout(newLayout);
                return;
            }
            BorderLayout borderLayout = (BorderLayout) currentLayout;
            int horizontalGap = hgap;
            int verticalGap   = vgap;

            boolean horizontalGapChanged = horizontalGap != borderLayout.getHgap();
            boolean verticalGapChanged   = verticalGap   != borderLayout.getVgap();

            if ( horizontalGapChanged || verticalGapChanged ) {
                borderLayout.setHgap(horizontalGap);
                borderLayout.setVgap(verticalGap);
                component.revalidate();
            }
        }
    }

    class GridLayoutInstaller implements Layout
    {
        private final int rows;
        private final int cols;
        private final int hgap;
        private final int vgap;

        GridLayoutInstaller( int rows, int cols, int hgap, int vgap ) {
            this.rows = rows;
            this.cols = cols;
            this.hgap = hgap;
            this.vgap = vgap;
        }

        @Override public int hashCode() { return Objects.hash( rows, cols, hgap, vgap ); }

        @Override
        public boolean equals(Object o) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            GridLayoutInstaller other = (GridLayoutInstaller) o;
            return rows == other.rows && cols == other.cols && hgap == other.hgap && vgap == other.vgap;
        }

        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !(currentLayout instanceof GridLayout) ) {
                // We need to replace the current layout with a GridLayout:
                GridLayout newLayout = new GridLayout( rows, cols, hgap, vgap );
                component.setLayout(newLayout);
                return;
            }
            GridLayout gridLayout = (GridLayout) currentLayout;
            int rows = this.rows;
            int cols = this.cols;
            int horizontalGap = hgap;
            int verticalGap   = vgap;

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

    class ForBoxLayout implements Layout
    {
        private final int axis;

        ForBoxLayout( int axis ) { this.axis = axis; }

        @Override public int hashCode() { return Objects.hash( axis ); }

        @Override
        public boolean equals(Object o) {
            if ( o == null ) return false;
            if ( o == this ) return true;
            if ( o.getClass() != getClass() ) return false;
            ForBoxLayout other = (ForBoxLayout) o;
            return axis == other.axis;
        }

        @Override
        public void installFor( JComponent component ) {
            LayoutManager currentLayout = component.getLayout();
            if ( !(currentLayout instanceof BoxLayout) ) {
                // We need to replace the current layout with a BoxLayout:
                BoxLayout newLayout = new BoxLayout( component, axis );
                component.setLayout(newLayout);
                return;
            }
            BoxLayout boxLayout = (BoxLayout) currentLayout;
            int axis = this.axis;

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
