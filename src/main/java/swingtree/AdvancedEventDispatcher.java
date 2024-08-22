package swingtree;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class AdvancedEventDispatcher {

    private static final AdvancedEventDispatcher eventDispatcher = new AdvancedEventDispatcher();

    static void addMouseEnterListener(Component component, MouseListener listener) {
        List<MouseListener> listeners = eventDispatcher.enterListeners.computeIfAbsent(component, k -> new ArrayList<>());
        listeners.add(listener);
    }

    static void addMouseExitListener(Component component, MouseListener listener) {
        List<MouseListener> listeners = eventDispatcher.exitListeners.computeIfAbsent(component, k -> new ArrayList<>());
        listeners.add(listener);
    }

    private final Map<Component, List<MouseListener>> enterListeners;
    private final Map<Component, List<MouseListener>> exitListeners;

    private AdvancedEventDispatcher() {
        enterListeners = new WeakHashMap<>();
        exitListeners  = new WeakHashMap<>();

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
        Component component = mouseEvent.getComponent().getParent();

        while (component != null) {
            List<MouseListener> listeners = enterListeners.get(component);

            if (listeners != null) {
                MouseEvent localMouseEvent = withNewSource(mouseEvent, component);
                for (MouseListener listener : listeners) {
                    listener.mouseEntered(localMouseEvent);
                }
            }

            component = component.getParent();
        }
    }

    private void onMouseExit(MouseEvent mouseEvent) {
        Component component = mouseEvent.getComponent().getParent();

        while (component != null && !containsScreenLocation(component, mouseEvent.getLocationOnScreen())) {
            List<MouseListener> listeners = exitListeners.get(component);

            if (listeners != null) {
                MouseEvent localMouseEvent = withNewSource(mouseEvent, component);
                for (MouseListener listener : listeners) {
                    listener.mouseExited(localMouseEvent);
                }
            }

            component = component.getParent();
        }
    }

    private MouseEvent withNewSource( MouseEvent event, Component newSource ) {
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

    private static boolean containsScreenLocation(Component component, Point screenLocation) {
        Point compLocation = component.getLocationOnScreen();
        Dimension compSize = component.getSize();
        int relativeX = screenLocation.x - compLocation.x;
        int relativeY = screenLocation.y - compLocation.y;
        return (relativeX >= 0 && relativeX < compSize.width && relativeY >= 0 && relativeY < compSize.height);
    }
}
