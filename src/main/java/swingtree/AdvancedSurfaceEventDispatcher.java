package swingtree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

final class AdvancedSurfaceEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AdvancedSurfaceEventDispatcher.class);
    private static final AdvancedSurfaceEventDispatcher eventDispatcher = new AdvancedSurfaceEventDispatcher();

    static void addMouseEnterListener(Component component, AdvancedSurfaceListener listener) {
        List<AdvancedSurfaceListener> listeners = eventDispatcher.enterListeners.computeIfAbsent(component, k -> new ArrayList<>());
        listeners.add(listener);
    }

    static void addMouseExitListener(Component component, AdvancedSurfaceListener listener) {
        List<AdvancedSurfaceListener> listeners = eventDispatcher.exitListeners.computeIfAbsent(component, k -> new ArrayList<>());
        listeners.add(listener);
    }

    private final Map<Component, List<AdvancedSurfaceListener>> enterListeners;
    private final Map<Component, List<AdvancedSurfaceListener>> exitListeners;

    private AdvancedSurfaceEventDispatcher() {
        enterListeners = new WeakHashMap<>();
        exitListeners = new WeakHashMap<>();

        Toolkit.getDefaultToolkit().addAWTEventListener(this::onMouseEvent, AWTEvent.MOUSE_EVENT_MASK);
    }

    public void onMouseEvent(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent mouseEvent = (MouseEvent) event;

            switch (mouseEvent.getID()) {
                case MouseEvent.MOUSE_ENTERED:
                    onMouseEnter(mouseEvent);
                    break;
                case MouseEvent.MOUSE_EXITED:
                    onMouseExit(mouseEvent);
                    break;
            }

        }
    }

    private void onMouseEnter(MouseEvent mouseEvent) {
        if (enterListeners.isEmpty())
            return;

        Component component = mouseEvent.getComponent().getParent();

        while (component != null) {
            List<AdvancedSurfaceListener> listeners = enterListeners.get(component);

            if (listeners != null) {
                Object surface = mouseEvent.getSource();
                MouseEvent localMouseEvent = withNewSource(mouseEvent, component);
                for (AdvancedSurfaceListener listener : listeners) {
                    try {
                        listener.mouseEntered(surface, localMouseEvent);
                    } catch (Exception e) {
                        log.error("Failed to process mouseEntered event {}. Error: {}", localMouseEvent, e.getMessage(), e);
                    }
                }
            }

            component = component.getParent();
        }
    }

    private void onMouseExit(MouseEvent mouseEvent) {
        if (exitListeners.isEmpty())
            return;

        Component component = mouseEvent.getComponent().getParent();

        while (component != null) {
            List<AdvancedSurfaceListener> listeners = exitListeners.get(component);

            if (listeners != null) {
                Object surface = mouseEvent.getSource();
                MouseEvent localMouseEvent = withNewSource(mouseEvent, component);
                for (AdvancedSurfaceListener listener : listeners) {
                    try {
                        listener.mouseExited(surface, localMouseEvent);
                    } catch (Exception e) {
                        log.error("Failed to process mouseExited event {}. Error: {}", localMouseEvent, e.getMessage(), e);
                    }
                }
            }

            component = component.getParent();
        }
    }

    private MouseEvent withNewSource(MouseEvent event, Component newSource) {
        return new MouseEvent(
                newSource,
                event.getID(),
                event.getWhen(),
                event.getModifiersEx(),
                event.getX(),
                event.getY(),
                event.getClickCount(),
                event.isPopupTrigger(),
                event.getButton()
        );
    }

}
