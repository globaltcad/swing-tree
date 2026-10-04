package swingtree;

import swingtree.style.ComponentBackend;

import javax.swing.JComponent;

final class SilentSet
{
    private boolean _isOngoing = false;

    static void run( JComponent component, Runnable set ) {
        SilentSet state = ComponentBackend.powering(component).getOrSet(SilentSet.class, SilentSet::new);
        boolean wasOngoing = state._isOngoing;
        state._isOngoing = true;
        try {
            set.run();
        } finally {
            state._isOngoing = wasOngoing;
        }
    }

    static boolean isOngoingFor( JComponent component ) {
        return ComponentBackend.powering(component)
                                .get(SilentSet.class)
                                .map(state -> state._isOngoing)
                                .orElse(false);
    }
}
