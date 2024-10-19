package swingtree.layout;

import com.google.errorprone.annotations.Immutable;
import net.miginfocom.swing.MigLayout;
import swingtree.UI;

import javax.swing.JComponent;
import java.awt.*;
import java.util.Optional;

@Immutable
public final class HorizontalGrid
{
    @SuppressWarnings("Immutable")
    private final AutoCellSpanPolicy[] _autoSpans;
    private final int _numberOfColumns;

    public HorizontalGrid(int numberOfColumns, AutoCellSpanPolicy[] autoSpans) {
        _autoSpans = autoSpans.clone();
        _numberOfColumns = numberOfColumns;
    }

    public void style(Container parent, JComponent child) throws Exception {

        LayoutManager layout = parent.getLayout();
        Insets insets = parent.getInsets();
        int unusableWidth = insets.left + insets.right;
        if ( layout instanceof ResponsiveGridFlowLayout) {
            ResponsiveGridFlowLayout flowLayout = (ResponsiveGridFlowLayout) layout;
            unusableWidth += flowLayout.getHgap() * (parent.getComponentCount() - 1);
        } else if ( layout instanceof FlowLayout ) {
            FlowLayout flowLayout = (FlowLayout) layout;
            unusableWidth += flowLayout.getHgap() * (parent.getComponentCount() - 1);
        } else if ( layout instanceof MigLayout ) {
            MigLayout migLayout = (MigLayout) layout;
            Object rowConstr = migLayout.getRowConstraints();
            System.out.println(rowConstr);
        }
        int parentPrefWidth = parent.getPreferredSize().width - unusableWidth;
        int parentWidth = parent.getWidth() - unusableWidth;


        if ( parentPrefWidth <= 0 ) {
            return;
            // The responsive layout is based on the preferred width of the parent. So it has to be known.
        }
        // How much preferred width the parent actually fills:
        double howFull = parentWidth / (double) parentPrefWidth;
        howFull = Math.max(0, howFull);

        UI.ParentSize currentParentSizeCategory = UI.ParentSize.NONE;
        if ( howFull <= 0 ) {
            currentParentSizeCategory = UI.ParentSize.NONE;
        } else if (howFull < 1/5d) {
            currentParentSizeCategory = UI.ParentSize.VERY_SMALL;
        } else if (howFull < 2/5d) {
            currentParentSizeCategory = UI.ParentSize.SMALL;
        } else if (howFull < 3/5d) {
            currentParentSizeCategory = UI.ParentSize.MEDIUM;
        } else if (howFull < 4/5d) {
            currentParentSizeCategory = UI.ParentSize.LARGE;
        } else if (howFull <= 1) {
            currentParentSizeCategory = UI.ParentSize.VERY_LARGE;
        } else {
            currentParentSizeCategory = UI.ParentSize.OVERSIZE;
        }

        Optional<AutoCellSpanPolicy> autoSpan = _findNextBestAutoSpan(currentParentSizeCategory);
        if (!autoSpan.isPresent()) {
            return;
        }

        int cellsToFill = autoSpan.get().cellsToFill();
        int width = (parentWidth * cellsToFill) / _numberOfColumns;

        Dimension newSize = new Dimension(width, child.getPreferredSize().height);
        child.setPreferredSize( newSize );
    }

    private Optional<AutoCellSpanPolicy> _findNextBestAutoSpan(UI.ParentSize targetSize ) {
        for ( AutoCellSpanPolicy autoSpan : _autoSpans ) {
            if ( autoSpan.parentSize() == targetSize ) {
                return Optional.of(autoSpan);
            }
        }
        // We did not find the exact match. Let's try to find the closest match.

        UI.ParentSize[] values = UI.ParentSize.values();
        int targetOrdinal = targetSize.ordinal();
        /*
            We want to find the enum value which is closed to the target ordinal.
         */
        int sign = ( targetSize.ordinal() > values.length/2 ? -1 : 1 );
        for ( int offset = 1; offset < values.length; offset++ ) {
            int nextOrdinal = targetOrdinal + offset * sign;
            if ( nextOrdinal > 0 && nextOrdinal < values.length ) {
                UI.ParentSize next = values[nextOrdinal];
                for ( AutoCellSpanPolicy autoSpan : _autoSpans ) {
                    if ( autoSpan.parentSize() == next ) {
                        return Optional.of(autoSpan);
                    }
                }
            }
            sign = -sign;
        }
        return Optional.empty();
    }


}
