package swingtree;

import sprouts.Tuple;
import swingtree.components.JBox;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

final class InternalUtil
{
    private InternalUtil() {}

    static Optional<Tuple<StackTraceElement>> ifInDebugModeExtractUserSourceCodeTrace() {
        if ( !SwingTree.get().isRecordingDebugSourceTrace() ) {
            return Optional.empty();
        }
        StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < traceElements.length; i++) {
            StackTraceElement element = traceElements[i];
            String className = element.getClassName();
            if (!className.startsWith("java.") && !className.startsWith("swingtree.")) {
                Tuple<StackTraceElement> fullTrace = Tuple.of(StackTraceElement.class, traceElements);
                Tuple<StackTraceElement> subTrace = fullTrace.slice(i, fullTrace.size());
                return Optional.of(subTrace);
            }
        }
        return Optional.empty();
    }

    static int _actualComponentCountFrom(Container container) {
        if ( container instanceof JMenu ) {
            return ((JMenu) container).getMenuComponentCount();
        }
        return container.getComponentCount();
    }

    static Component[] _actualComponentsFrom(Container container) {
        if ( container instanceof JMenu ) {
            return ((JMenu) container).getMenuComponents();
        }
        return container.getComponents();
    }

    static Component _actualGetComponentAt(int index, Container container) {
        if ( container instanceof JMenu ) {
            return ((JMenu) container).getMenuComponent(index);
        }
        return container.getComponent(index);
    }

    static void _traverseEnable(Component c, boolean isEnabled ) {
        c.setEnabled( isEnabled );
        if ( JPanel.class.isAssignableFrom( c.getClass() ) || JBox.class.isAssignableFrom( c.getClass() ) )
            for ( Component c2 : ((JComponent)c).getComponents() )
                _traverseEnable( c2, isEnabled );
        /*
            Note:
                We use getClass() here, because we want to stop at subclasses of
                JBox or JPanel because they are likely user defined components and not dumb
                wrappers for other components.
        */
    }
}
