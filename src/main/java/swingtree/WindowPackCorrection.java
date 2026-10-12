package swingtree;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

final class WindowPackCorrection
{
    private static final int MAX_EXTRA_PACKS = 3;

    private WindowPackCorrection() {}

    static void pack( Window window ) {
        @Nullable JRootPane rootPane = window instanceof RootPaneContainer ? ((RootPaneContainer) window).getRootPane() : null;
        if ( rootPane == null ) {
            window.pack();
            return;
        }
        List<JComponent> componentsWithNewTextHeight = new ArrayList<>();
        rootPane.putClientProperty(ScrollPaneLayoutCorrection.COMPONENTS_WITH_NEW_TEXT_HEIGHT, componentsWithNewTextHeight);
        try {
            window.pack();
            for ( int pass = 0; pass < MAX_EXTRA_PACKS && !componentsWithNewTextHeight.isEmpty(); pass++ ) {
                List<JComponent> measuredAgain = new ArrayList<>(componentsWithNewTextHeight);
                componentsWithNewTextHeight.clear();
                measuredAgain.forEach(Component::invalidate);
                window.pack();
            }
        } finally {
            rootPane.putClientProperty(ScrollPaneLayoutCorrection.COMPONENTS_WITH_NEW_TEXT_HEIGHT, null);
        }
        componentsWithNewTextHeight.forEach(Component::revalidate);
    }
}
